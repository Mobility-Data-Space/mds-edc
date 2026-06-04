# Production Vault Setup Guide

This guide covers what is needed to move the MDS EDC Connector's HashiCorp Vault from development mode to a production-ready deployment. For general Vault administration, refer to the [official documentation](https://developer.hashicorp.com/vault/docs).

## Why Development Mode is Insufficient

The default docker-compose setup starts Vault with `command: server -dev`, which means:

- **Runtime secrets are lost** when the container restarts
- OAuth2 client secrets stored by EDC disappear
- Dynamically created tokens and credentials are gone
- Manual reconfiguration is needed after every restart

## Production Requirements

A production Vault deployment must address the following. Each links to the relevant official documentation:

1. **Persistent storage backend** — use Raft integrated storage or Consul ([Storage Configuration](https://developer.hashicorp.com/vault/docs/configuration/storage))
2. **TLS encryption** — enable TLS on all listeners with valid certificates ([TCP Listener](https://developer.hashicorp.com/vault/docs/configuration/listener/tcp))
3. **Auto-unseal** — use a cloud KMS to avoid manual unseal operations ([Seal/Unseal](https://developer.hashicorp.com/vault/docs/configuration/seal))
4. **Initialization** — run `vault operator init` once after first deploy ([Operator Init](https://developer.hashicorp.com/vault/docs/commands/operator/init))
5. **Audit logging** — enable at least one audit device ([Audit Devices](https://developer.hashicorp.com/vault/docs/audit))
6. **Backup strategy** — schedule periodic Raft snapshots ([Raft Snapshots](https://developer.hashicorp.com/vault/docs/commands/operator/raft/snapshot))
7. **High availability** — for critical deployments, run multiple nodes with Raft ([HA with Raft](https://developer.hashicorp.com/vault/docs/configuration/storage/raft))

For a comprehensive checklist, see the [Production Hardening Guide](https://developer.hashicorp.com/vault/tutorials/operations/production-hardening).

## Docker Compose: Dev to Production

Modify the `vault` service in `docker-compose.yml`:

```yaml
vault:
  image: hashicorp/vault:1.18.4
  cap_add:
    - IPC_LOCK
  environment:
    VAULT_ADDR: https://127.0.0.1:8200
    # Remove all VAULT_DEV_* variables
  volumes:
    - vault-data:/vault/file
    - ./resources/vault.hcl:/vault/config/vault.hcl:ro
    - ./resources/vault-tls:/vault/config/tls:ro
  command: server -config=/vault/config/vault.hcl
  healthcheck:
    test: ["CMD", "vault", "status"]
    interval: 10s
    timeout: 5s
    retries: 5
  ports:
    - "8200:8200"
```

### Key Changes from Dev Mode

1. **Remove** `VAULT_DEV_ROOT_TOKEN_ID` and `VAULT_DEV_LISTEN_ADDRESS`
2. **Add** configuration file mount (`vault.hcl`)
3. **Add** TLS certificate mount
4. **Change** command to `server -config=/vault/config/vault.hcl`
5. **Update** `VAULT_ADDR` to `https://`

For `vault.hcl` configuration examples (Raft, TLS, auto-unseal), see [Vault Server Configuration](https://developer.hashicorp.com/vault/docs/configuration).

## Authentication for EDC

The EDC HashiCorp Vault extension supports two authentication modes. For production, replace the root token with one of the following.

> **Upstream reference:** [eclipse-edc/Connector — vault-hashicorp extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/vault/vault-hashicorp)

### Option 1: Token Authentication

```yaml
edc:
  environment:
    EDC_VAULT_HASHICORP_URL: "https://vault:8200"
    EDC_VAULT_HASHICORP_TOKEN: "<scoped-token>"
    EDC_VAULT_HASHICORP_TOKEN_SCHEDULEDRENEW_ENABLED: "true"
    EDC_VAULT_HASHICORP_TOKEN_TTL: "300"
    EDC_VAULT_HASHICORP_TOKEN_RENEW_BUFFER: "30"
```

The extension automatically renews the token before expiry when scheduled renewal is enabled.

### Option 2: OAuth2 Client Credentials

```yaml
edc:
  environment:
    EDC_VAULT_HASHICORP_URL: "https://vault:8200"
    EDC_VAULT_HASHICORP_CLIENTID: "<oauth2-client-id>"
    EDC_VAULT_HASHICORP_CLIENTSECRET: "<oauth2-client-secret>"
    EDC_VAULT_HASHICORP_TOKENURL: "https://idp.example.com/oauth2/token"
```

Do not set `EDC_VAULT_HASHICORP_TOKEN` when using OAuth2 client credentials.

## References

- [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault/docs)
- [Production Hardening Guide](https://developer.hashicorp.com/vault/tutorials/operations/production-hardening)
- [Vault Server Configuration](https://developer.hashicorp.com/vault/docs/configuration)
- [EDC HashiCorp Vault Extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/vault/vault-hashicorp)
