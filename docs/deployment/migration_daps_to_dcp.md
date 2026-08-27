# Migrating an on-premise MDS connector from DAPS to DCP

This guide covers self-hosted MDS Connector deployments moving from the DAPS identity profile to the Decentralized Claims Protocol (DCP).

Connector-as-a-Service participants are migrated by MDS and do not need this document.

## What changes

Only your connector's identity changes. Everything else — hostname, ports, reverse proxy, database, assets, policies, contract definitions — stays exactly as it is.

- **You are identified by a DID, not by your MDS participant id.** `edc.participant.id` becomes `did:web:<wallet-host>`.
- **You prove that identity with a Verifiable Credential, not a DAPS token.** Your wallet's STS mints a presentation containing a `MembershipCredential` issued by MDS, in place of the Dynamic Attribute Token you got from the DAPS realm.
- **You need a wallet.** It hosts your DID document, holds your credential, and runs the STS. Either MDS operates it for you or you run it yourself.
- **Your Vault secret changes.** The `.p12`-derived `daps-private-key` / `daps-public-key` are replaced by a single STS client secret.
- **The connector image changes.** `connector-vault-postgresql` becomes `connector-vault-postgresql-dcp`.

Because the identity changes, existing contract agreements — which reference your old participant id on both sides — stop working and must be re-negotiated after the cutover.

## Two phases

DCP is not a newer version of MDS-EDC — it is a second image built from the same release. Since v2.2.0 every release publishes both `connector-vault-postgresql` (DAPS) and `connector-vault-postgresql-dcp` (DCP) from the same tag.

That means version and identity are independent changes, and you should make them one at a time:

| | Phase A | Phase B |
|---|---|---|
| What | Upgrade to 2.6.0 or later | Switch to the DCP image |
| Identity | DAPS | DAPS → DID |
| Database | schema migrations run | untouched |
| Rollback | needs a database restore | redeploy the old image |

Running them together makes a failed cutover impossible to attribute, and forces you to undo a database migration to get back to a working state.

---

## Phase A — upgrade to 2.6.0, still on DAPS

Coming from 2.0.0, this crosses EDC 0.15.1 → 0.18.0 and three schema migrations. It is the recommended release that publishes the wallet image, and the release where Management API V4 became the active version. Any later release works too — take the newest from the [releases page](https://github.com/Mobility-Data-Space/mds-edc/releases).

1. **Back up the database.** Schema migrations are forward-only; this backup is your only way back. See [Backup and Recovery](backup_and_recovery.md).
2. **Bump the image tag**, leaving every environment variable untouched:

   ```diff
   - image: ghcr.io/mobility-data-space/mds-edc/connector-vault-postgresql:2.0.0
   + image: ghcr.io/mobility-data-space/mds-edc/connector-vault-postgresql:2.6.0
   ```

   Pin an exact tag. Never `:latest` — rollback needs a known-good version.
3. **Restart.** Flyway migrations run automatically at boot. Watch the logs until they complete.
4. **Verify against a real counterparty**: a catalog request and a contract negotiation, still over DAPS.
5. **(Optional) Migrate your Management API clients to V4.** V3 still works at 2.6.0 but is deprecated and will be removed. See [Management API](../usage/management_api/README.md) and [Upgrade to EDC 1.0](../development/upgrade_to_1.0.md).

Do not continue until this is stable in production. Note the exact tag — Phase B keeps it.

---

## Phase B — switch to DCP

### Get a wallet

**MDS-hosted (default).** MDS operates the wallet and hands over four values: your DID, the STS token endpoint, the STS client id (equal to your DID) and client secret, and the MDS issuer DID. Request these in the MDS Portal and confirm your `MembershipCredential` has been issued.

**Self-hosted (advanced).** You run the `wallet` image on its own hostname, with its own PostgreSQL schema and Vault. Follow [Identity Wallet deployment](wallet_deployment.md) end to end — deploy, register your participant context, verify the DID document, request credential issuance — then return here with the same four values. You then also own DID resolution availability: if your wallet host is unreachable, counterparties cannot verify you.

### 0. Checkpoint

- [ ] Phase A complete: 2.6.0 or later, verified in production.
- [ ] Your DID is registered with MDS and known to the issuer.
- [ ] Your `MembershipCredential` reports `status == "ISSUED"`.
- [ ] You have the STS client secret — it is shown only once, at participant registration.
- [ ] Your DID document resolves over HTTPS from the public internet.
- [ ] Check DID resolution before anything else — nothing downstream works without it:

```bash
curl -fsS "https://<wallet-host>/.well-known/did.json" | jq '{id, service: .service[0].type}'
```

Expect `id` to equal your DID and one service entry of type `CredentialService`.

### 1. Store the STS client secret in the connector's Vault

The connector reads this from **its own** Vault, not the wallet's:

```bash
vault kv put "secret/<did>-sts-client-secret" content="<client-secret>"
```

The alias is arbitrary but must match `EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS` below. The convention is `<did>-sts-client-secret`.

Leave the DAPS entries (`daps-private-key`, `daps-public-key`) in place — the DCP runtime ignores them, and keeping them makes rollback trivial. The `transfer-proxy-token-signer-*` keys are still required.

### 2. Swap the image

```diff
- image: ghcr.io/mobility-data-space/mds-edc/connector-vault-postgresql:2.6.0
+ image: ghcr.io/mobility-data-space/mds-edc/connector-vault-postgresql-dcp:2.6.0
```

**Keep the version tag identical.** Only the image name changes. If you self-host the wallet, run it on the same tag.

### 3. Apply the configuration diff

This is the whole migration. Remove nine variables, add seven, change one.

**Remove** — DAPS-only:

```
EDC_OAUTH_TOKEN_URL
EDC_OAUTH_CLIENT_ID
EDC_OAUTH_PRIVATE_KEY_ALIAS
EDC_OAUTH_CERTIFICATE_ALIAS
EDC_OAUTH_PROVIDER_JWKS_URL
EDC_OAUTH_PROVIDER_AUDIENCE
EDC_OAUTH_ENDPOINT_AUDIENCE
EDC_IAM_TOKEN_SCOPE
EDC_AGENT_IDENTITY_KEY
```

**Add and change** — the complete identity block:

```env
# changed: was your MDS participant id
EDC_PARTICIPANT_ID=did:web:wallet.example.com

# added
EDC_IAM_ISSUER_ID=did:web:wallet.example.com
EDC_IAM_STS_OAUTH_CLIENT_ID=did:web:wallet.example.com
EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS=did:web:wallet.example.com-sts-client-secret
EDC_IAM_STS_OAUTH_TOKEN_URL=https://wallet.example.com/api/sts/token
EDC_IAM_TRUSTED_ISSUER_ISSUER_ID=did:web:issuer.mobility-dataspace.eu
EDC_IAM_TRUSTED_ISSUER_ISSUER_SUPPORTEDTYPES=["MembershipCredential"]
EDC_IAM_DID_WEB_USE_HTTPS=true
```

| Variable | Value |
|---|---|
| `EDC_PARTICIPANT_ID`, `EDC_IAM_ISSUER_ID`, `EDC_IAM_STS_OAUTH_CLIENT_ID` | your DID |
| `EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS` | the Vault alias from step 1 |
| `EDC_IAM_STS_OAUTH_TOKEN_URL` | your wallet's STS endpoint |
| `EDC_IAM_TRUSTED_ISSUER_ISSUER_ID` | the MDS issuer DID |
| `EDC_IAM_TRUSTED_ISSUER_ISSUER_SUPPORTEDTYPES` | `["MembershipCredential"]` |
| `EDC_IAM_DID_WEB_USE_HTTPS` | `true` — required in production |

### 4. Restart

```bash
docker compose up -d edc          # or: kubectl rollout restart deployment/<connector>
```

No migration runs — the version is unchanged, so the schema is already at the right level. Flyway activity in the logs here means the version tag changed too; revert to the Phase A tag.

## Verify

**1. The runtime started with the DCP identity.** The boot log shows your DID as participant id, and no `oauth2` extension lines.

**2. STS mints a token.** From the connector's network namespace:

```bash
curl -fsS -X POST "https://<wallet-host>/api/sts/token" \
  -d "grant_type=client_credentials" \
  -d "client_id=<did>" \
  -d "client_secret=<client-secret>" | jq '.access_token != null'
```

`true` confirms the Vault secret matches what the wallet expects.

**3. The credential is present.** On the wallet:

```bash
curl -fsS "https://<wallet-host>/api/identity/v1beta/participants/<url-encoded-did>/credentials" \
  -H "x-api-key: <participant-api-key>" | jq '.[].credential.type'
```

Expect `MembershipCredential`.

**4. A catalog request against a migrated counterparty.** The real end-to-end check — DID resolution, STS, credential presentation and verification in one call. Note that `counterPartyId` is now the counterparty's **DID**:

```bash
curl -fsS -X POST "https://<edc-hostname>/api/management/v4/catalog/request" \
  -H "x-api-key: <management-api-key>" \
  -H 'content-type: application/json' \
  -d '{
    "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
    "@type": "CatalogRequest",
    "counterPartyAddress": "https://<counterparty-host>/api/dsp/2025-1",
    "counterPartyId": "did:web:<counterparty-wallet-host>",
    "protocol": "dataspace-protocol-http:2025-1"
  }' | jq '."dcat:dataset" | length'
```

A populated catalog means the migration succeeded. Then re-negotiate your contracts.

## Further reading

- [Identity Wallet deployment](wallet_deployment.md) — self-hosted wallet, participant registration, credential issuance
- [Backup and Recovery](backup_and_recovery.md) — required before Phase A
- [MDS connector default configuration](mds_connector_default_configuration.md)
- [Production Docker image](production_docker_image.md) — ports, reverse proxy, TLS
- [Production Vault setup](production_vault_setup.md)
- [mds-identity-issuer](https://github.com/Mobility-Data-Space/mds-identity-issuer) — the credential issuer service
- [EDC IdentityHub documentation](https://eclipse-edc.github.io/documentation/for-adopters/identity-hub/) — upstream reference for the wallet components
