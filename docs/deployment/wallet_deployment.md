# MDS Identity Wallet deployment

The MDS Identity Wallet is a per-participant runtime that combines four responsibilities into one process:

- **Identity Hub** — creation and management of *participant contexts* (the wallet's local view of "this connector").
- **Secure Token Service (STS)** — mints the short-lived signed tokens (`client_credentials`) the EDC connector presents to counterparties during Dataspace Protocol handshakes.
- **Credential Service** — receives issued Verifiable Credentials from the issuer service and stores them.
- **`did:web` publisher** — serves the wallet's DID document at `/.well-known/did.json`, referenced by the participant's DID.

It corresponds to the [`launchers/wallet`](../../launchers/wallet) module. Source lives under [`extensions/identity-hub/`](../../extensions/identity-hub) (Flyway migrations + super-user seeder). This document describes how to run it standalone and how a connector participant self-provides a wallet against it.

For the accompanying **credential issuer** service (which issues Membership / DataProcessor credentials to wallets), see the [mds-identity-issuer](https://github.com/Mobility-Data-Space/mds-identity-issuer) repository.

## When you need a wallet

You need one wallet **per connector participant** as soon as you are running the DCP (Decentralized Claims Protocol) profile. Legacy DAPS-based connectors do not need it. The DCP launchers (`connector-vault-postgresql-dcp`, `connector-inmemory-dcp`) rely on a wallet reachable at `edc.iam.sts.oauth.token.url`, so the wallet must be running and the participant context registered before the connector will complete a Dataspace Protocol handshake.

## Prerequisites

- **PostgreSQL** — dedicated database (or dedicated schema on a shared instance). The wallet owns its own tables via Flyway (`extensions/identity-hub/database-schema-migration-wallet`); do **not** share the schema with the connector.
- **HashiCorp Vault** — for private key storage. The wallet stores generated participant private keys under `<did>-privatekey-alias`, per-participant super-user api-keys, and the STS client secrets.
- **A resolvable `did:web` hostname** — the wallet publishes the DID document at `https://<wallet-host>/.well-known/did.json`; whichever DIDs your participants use (`did:web:<connector-host>`) must point at *this* wallet's `/` for resolution to work.
- **The issuer's DID resolvable** (needed later for credential requests) — either at a public URL or reachable from the wallet pod.

## Building the wallet image

The wallet launcher is not currently published to `ghcr.io`. Build it locally from source:

```bash
# from the repo root
./gradlew :launchers:wallet:build
docker build \
  --build-arg RUNTIME=wallet \
  -t mds-wallet:local \
  -f launchers/Dockerfile launchers/
```

The `Dockerfile` at `launchers/Dockerfile` is shared across all runtimes and picks the launcher via the `RUNTIME` build-arg (same pattern as `connector-vault-postgresql`). The resulting image runs `bin/wallet` as its entrypoint.

## Runtime configuration

Every setting is passed via environment variables (EDC translates dots to underscores). The wallet exposes **four** independent HTTP contexts on distinct ports.

### Web endpoints

| Context | Purpose | Env vars |
|---|---|---|
| `credentials` | DCP Credential Service — the endpoint the issuer POSTs issued VCs to. | `WEB_HTTP_CREDENTIALS_PORT`, `WEB_HTTP_CREDENTIALS_PATH` |
| `did` | Hosts `/.well-known/did.json` for every registered participant. | `WEB_HTTP_DID_PORT`, `WEB_HTTP_DID_PATH` |
| `identity` | Identity Hub admin API — used to create/update/delete participant contexts, protected by the super-user api-key. | `WEB_HTTP_IDENTITY_PORT`, `WEB_HTTP_IDENTITY_PATH` |
| `sts` | Secure Token Service — the OAuth2 token endpoint the connector calls to obtain DCP tokens. | `WEB_HTTP_STS_PORT`, `WEB_HTTP_STS_PATH` |

There is no separate management/public port. Do not multiplex these behind the same port; the EDC web framework requires four separate contexts.

A typical Kubernetes port table:

```
credentials  7080  /api/credentials
did          7083  /
identity     7081  /api/identity
sts          7086  /api/sts
```

### Vault, database, super-user

```env
# Postgres — Flyway migrations run automatically at boot.
EDC_DATASOURCE_DEFAULT_URL=jdbc:postgresql://<host>:5432/<wallet-db>?currentSchema=mds_wallet_schema
EDC_DATASOURCE_DEFAULT_USER=<user>
EDC_DATASOURCE_DEFAULT_PASSWORD=<password>
EU_DATASPACE_WALLET_POSTGRESQL_MIGRATION_SCHEMA=mds_wallet_schema
EDC_SQL_SCHEMA_AUTOCREATE=false   # required — Flyway owns the schema

# Vault (HashiCorp KV v2).
EDC_VAULT_HASHICORP_URL=http://vault:8200
EDC_VAULT_HASHICORP_TOKEN=<vault-token>

# Super-user (seeded on first boot; used for the /v1beta/participants admin API).
EDC_IDENTITYHUB_SUPERUSER_ID=super-user
EDC_IDENTITYHUB_SUPERUSER_API_KEY=<base64(super-user)>.<random>

# did:web must use HTTPS in production so counterparties can resolve DIDs.
EDC_IAM_DID_WEB_USE_HTTPS=true
```

The super-user api-key must be formatted `base64(<EDC_IDENTITYHUB_SUPERUSER_ID>).<random-string>` — a random-only value boots but every subsequent admin call returns 401 with the log line `this key appears to have an invalid format`.

The seeder writes the api-key into Vault under `secret/<super-user-id>-apikey` on first boot **only**; if the participant-context row already exists in Postgres, the vault write is skipped. If you rotate the k8s secret without wiping the DB row you will get 401s until you either delete the row + Vault entry and restart, or write the new value into Vault by hand.

## Registering a participant (self-provide a wallet)

Once the wallet is up you register **one participant context per connector** through the admin Identity API. The context is what the connector's STS calls will authenticate against.

The example below assumes:

- `WALLET_HOST=https://wallet.example.com` — the wallet's public hostname.
- `PARTICIPANT_DID=did:web:my-connector.example.com` — the DID your connector will advertise on DSP.
- `SUPER_API_KEY` — the value of `EDC_IDENTITYHUB_SUPERUSER_API_KEY`.

```bash
PARTICIPANT_CONTEXT_ID="$(uuidgen)"

curl -fsS -X POST "${WALLET_HOST}/api/identity/v1beta/participants" \
  -H "x-api-key: ${SUPER_API_KEY}" \
  -H 'content-type: application/json' \
  -d @- <<JSON
{
  "participantContextId": "${PARTICIPANT_CONTEXT_ID}",
  "did": "${PARTICIPANT_DID}",
  "active": true,
  "serviceEndpoint": {
    "id": "$(uuidgen)",
    "type": "CredentialService",
    "serviceEndpoint": "${WALLET_HOST}/api/credentials/v1/participants/${PARTICIPANT_CONTEXT_ID}"
  },
  "key": {
    "keyId": "${PARTICIPANT_DID}#key-1",
    "privateKeyAlias": "${PARTICIPANT_DID}-privatekey-alias",
    "keyGeneratorParams": { "algorithm": "EC" }
  }
}
JSON
```

Notes on the request body:

- `participantContextId` — a fresh UUID; it becomes the wallet-internal identifier and the path segment on `/v1/participants/{id}`. It is **not** the DID.
- `serviceEndpoint.type` must be `CredentialService`; this is the endpoint the issuer will POST issued VCs to, so the URL must be reachable from the issuer.
- `serviceEndpoint.serviceEndpoint` must include `/v1/participants/${PARTICIPANT_CONTEXT_ID}` — the wallet consumes this value verbatim in the DID document, and the issuer follows it as-is when delivering credentials. A bare host produces a DID document that no issuer can honour.
- `key.privateKeyAlias` — the Vault alias the wallet will use to store the generated private key. Any string is accepted; align it with your Vault path conventions.
- `key.keyGeneratorParams.algorithm` — `EC` (P-256) for interoperability with the current issuer profile. `EdDSA` / `Ed25519` also works but is not yet used across the MDS stack.

The response body includes the participant's **client id** and **client secret** — persist both:

```json
{
  "clientId": "did:web:my-connector.example.com",
  "clientSecret": "..."
}
```

The wallet will not return the client secret again. Store it in the **connector's** Vault under the alias the connector expects (`edc.iam.sts.oauth.client.secret.alias`; by convention `<did>-sts-client-secret`).

### Verify the DID document

```bash
curl -fsS "https://<connector-host>/.well-known/did.json" | jq
```

You should see a document with `id = ${PARTICIPANT_DID}`, one `verificationMethod` entry (public key), and one `service` entry of type `CredentialService`.

If the connector's hostname is different from the wallet's hostname (typical), the connector's ingress must forward `/.well-known/did.json` to the wallet's `did` context. Any reverse proxy path rewrite that hides `/.well-known/` from the wallet breaks DID resolution.

### Request credential issuance

Once the connector's participant is registered on the wallet and on the issuer, ask the wallet to fetch a Membership credential from the issuer:

```bash
HOLDER_PID="$(uuidgen)"

curl -fsS -X POST \
  "${WALLET_HOST}/api/identity/v1beta/participants/${PARTICIPANT_CONTEXT_ID}/credentials/request" \
  -H "x-api-key: ${PARTICIPANT_API_KEY}" \
  -H 'content-type: application/json' \
  -d @- <<JSON
{
  "holderPid": "${HOLDER_PID}",
  "issuerDid": "${ISSUER_DID}",
  "credentials": [
    {
      "id": "membershipCredential-id",
      "type": "MembershipCredential",
      "format": "VC1_0_JWT"
    }
  ]
}
JSON
```

`PARTICIPANT_API_KEY` is the api-key returned in the participant-registration response (or the super-user key, which also works). Poll `GET /api/identity/v1beta/participants/${PARTICIPANT_CONTEXT_ID}/credentials/request/${HOLDER_PID}` until `status == "ISSUED"`.

## Wiring the connector to the wallet

The connector participant that owns this DID needs four config values that point at the wallet:

```env
# In the connector's env (matches connector-vault-postgresql-dcp).
EDC_IAM_STS_OAUTH_CLIENT_ID=${PARTICIPANT_DID}
EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS=${PARTICIPANT_DID}-sts-client-secret
EDC_IAM_STS_OAUTH_TOKEN_URL=${WALLET_HOST}/api/sts/token
EDC_IAM_TRUSTED_ISSUER_ISSUER_ID=${ISSUER_DID}
EDC_IAM_TRUSTED_ISSUER_ISSUER_SUPPORTEDTYPES=["MembershipCredential"]
```

The connector reads `EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS` from **its own** Vault, not from the wallet's Vault — store the client secret returned by the participant-registration call at that alias in the connector's Vault before the connector boots.

## Operational notes

- **`EDC_SQL_SCHEMA_AUTOCREATE`** must be `false` in production. The Flyway bundle owns the schema; with `true`, EDC's SQL stores silently create missing tables at runtime, which masks missing migration files (this is exactly the failure mode fixed in [PR #474](https://github.com/Mobility-Data-Space/mds-edc/pull/474)).
- **`EDC_ENCRYPTION_STRICT=false`** is recommended unless you also configure `edc.encryption.aes.key.alias`. When strict encryption is enabled without an AES key, creating a participant context with non-empty `properties` throws `EncryptionAlgorithmRegistry.encrypt("aes", ...)` at runtime.
- **API version.** The wallet ships EDC IdentityHub `0.18.0`, so all admin routes are under `/v1beta`. Runtimes bumped from an older version will find `/v1alpha` returning 404.
- **The DID document is only refreshed on participant-context activation.** If you patch the DID or the service endpoint after activation, republish by cycling the participant context (deactivate, update, reactivate) — the DID doc is otherwise cached from the last activation.

## Related documentation

- [MDS connector default configuration](mds_connector_default_configuration.md)
- [Production Docker image](production_docker_image.md)
- [Production Vault setup](production_vault_setup.md)
- [mds-identity-issuer — post-deploy setup](https://github.com/Mobility-Data-Space/mds-identity-issuer/blob/main/docs/post-deploy-setup.md)
