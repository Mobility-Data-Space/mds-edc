# Production Vault Setup Guide

This guide explains how to configure HashiCorp Vault for production use with the MDS EDC Connector. The default docker-compose configuration uses Vault in **development mode**, which is not suitable for production deployments.

## Why Development Mode is Insufficient

The current docker-compose setup starts Vault with `command: server -dev`, which has limitations.

### Impact on MDS EDC

When Vault runs in dev mode:

- **Runtime secrets are lost** when the container restarts
- OAuth2 client secrets stored by EDC disappear
- Dynamically created tokens and credentials are gone
- Manual reconfiguration needed after every restart
- Not compliant with security best practices

## Production Requirements

According to [HashiCorp's production hardening guidelines](https://developer.hashicorp.com/vault/tutorials/operations/production-hardening), a production Vault deployment requires:

### 1. Persistent Storage Backend

Use **Raft Integrated Storage** (recommended) or Consul for data persistence.

#### Why Raft?

- Integrated with Vault (no external dependencies)
- Provides high availability and automatic failover
- Simplified backup and recovery
- Native clustering support

**Reference:** [Raft Storage Configuration](https://developer.hashicorp.com/vault/docs/configuration/storage/raft)

### 2. TLS/SSL Encryption

All Vault communication must be encrypted with TLS.

**Requirements:**

- Valid TLS certificates for Vault server
- TLS enabled on all listeners
- Minimum TLS 1.2 or higher
- Strong cipher suites

**Reference:** [TCP Listener Configuration](https://developer.hashicorp.com/vault/docs/configuration/listener/tcp)

### 3. Seal/Unseal Mechanism

Production Vault should use **auto-unseal** with a cloud Key Management Service (KMS).

**Supported Auto-Unseal Options:**

- AWS KMS
- Azure Key Vault
- Google Cloud KMS
- Transit (another Vault cluster)
- HSM (Hardware Security Module)

**Benefits:**

- Eliminates manual unseal operations
- Unseal keys never exist in plaintext
- Integrated with cloud security infrastructure

**Reference:** [Auto-Unseal Configuration](https://developer.hashicorp.com/vault/docs/configuration/seal)

### 4. Authentication Methods

Remove reliance on the root token and implement proper authentication.

**Recommended Auth Methods:**

- **AppRole**: For machine/application authentication (ideal for EDC)
- **Kubernetes**: If running in Kubernetes
- **LDAP/OIDC**: For human users
- **AWS/Azure/GCP**: Cloud provider authentication

**Reference:** [Authentication Methods](https://developer.hashicorp.com/vault/docs/auth)

### 5. Audit Logging

Enable audit logging to track all Vault operations.

**Reference:** [Audit Devices](https://developer.hashicorp.com/vault/docs/audit)

## Example Production Configuration

Below is an example Vault configuration file (`vault.hcl`) for production use. This example uses Raft storage and assumes you have TLS certificates.

### vault.hcl (Basic Production Configuration)

```hcl
# Vault Production Configuration for MDS EDC

# Raft Integrated Storage Backend
storage "raft" {
  path    = "/vault/file"
  node_id = "vault-node-1"

  # For HA cluster, configure additional nodes:
  # retry_join {
  #   leader_api_addr = "https://vault-node-2:8200"
  # }
  # retry_join {
  #   leader_api_addr = "https://vault-node-3:8200"
  # }
}

# TCP Listener with TLS
listener "tcp" {
  address       = "0.0.0.0:8200"
  tls_disable   = false
  tls_cert_file = "/vault/config/tls/vault.crt"
  tls_key_file  = "/vault/config/tls/vault.key"

  # Optional: Client certificate authentication
  # tls_require_and_verify_client_cert = true
  # tls_client_ca_file = "/vault/config/tls/ca.crt"
}

# Cluster address (for HA)
cluster_addr = "https://0.0.0.0:8201"

# API address (how clients reach this node)
api_addr = "https://vault.yourdomain.com:8200"

# Enable UI
ui = true

# Disable memory locking (required in containerized environments)
disable_mlock = true

# Log level
log_level = "info"

# Telemetry (optional but recommended)
telemetry {
  prometheus_retention_time = "30s"
  disable_hostname = true
}
```

### vault.hcl (With AWS KMS Auto-Unseal)

```hcl
# Vault Production Configuration with AWS KMS Auto-Unseal

storage "raft" {
  path    = "/vault/file"
  node_id = "vault-node-1"
}

# AWS KMS Auto-Unseal
seal "awskms" {
  region     = "us-east-1"
  kms_key_id = "your-kms-key-id"
  # AWS credentials should be provided via IAM role or environment variables
}

listener "tcp" {
  address       = "0.0.0.0:8200"
  tls_disable   = false
  tls_cert_file = "/vault/config/tls/vault.crt"
  tls_key_file  = "/vault/config/tls/vault.key"
}

cluster_addr = "https://0.0.0.0:8201"
api_addr = "https://vault.yourdomain.com:8200"

ui = true
disable_mlock = true
log_level = "info"
```

### vault.hcl (With Azure Key Vault Auto-Unseal)

```hcl
# Vault Production Configuration with Azure Key Vault Auto-Unseal

storage "raft" {
  path    = "/vault/file"
  node_id = "vault-node-1"
}

# Azure Key Vault Auto-Unseal
seal "azurekeyvault" {
  tenant_id      = "your-tenant-id"
  client_id      = "your-client-id"
  client_secret  = "your-client-secret"
  vault_name     = "your-keyvault-name"
  key_name       = "your-key-name"
}

listener "tcp" {
  address       = "0.0.0.0:8200"
  tls_disable   = false
  tls_cert_file = "/vault/config/tls/vault.crt"
  tls_key_file  = "/vault/config/tls/vault.key"
}

cluster_addr = "https://0.0.0.0:8201"
api_addr = "https://vault.yourdomain.com:8200"

ui = true
disable_mlock = true
log_level = "info"
```

## Docker Compose Production Configuration

To use production Vault configuration with docker-compose, modify the `vault` service:

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

1. **Remove** `VAULT_DEV_ROOT_TOKEN_ID`
2. **Remove** `VAULT_DEV_LISTEN_ADDRESS`
3. **Add** configuration file mount: `./resources/vault.hcl:/vault/config/vault.hcl:ro`
4. **Add** TLS certificate mount: `./resources/vault-tls:/vault/config/tls:ro`
5. **Change** command to: `server -config=/vault/config/vault.hcl`
6. **Update** `VAULT_ADDR` to use `https://` (if TLS enabled)

## Initialization and Unsealing

When you first start Vault in production mode, you must initialize it:

### 1. Initialize Vault

```bash
# Initialize Vault (only run once)
docker exec -it <vault-container-id> vault operator init
```

This command will output:
- **Unseal Keys** (5 by default, need 3 to unseal)
- **Initial Root Token**

**CRITICAL**: Store these securely! If lost, you cannot access Vault data.

### 2. Unseal Vault (if not using auto-unseal)

After every restart, Vault must be unsealed:

```bash
# Unseal with 3 of the 5 keys
docker exec -it <vault-container-id> vault operator unseal <key-1>
docker exec -it <vault-container-id> vault operator unseal <key-2>
docker exec -it <vault-container-id> vault operator unseal <key-3>
```

**With auto-unseal (AWS KMS, Azure, etc.)**, this step is automatic.

### 3. Login with Root Token

```bash
docker exec -it <vault-container-id> vault login <root-token>
```

## Authentication for EDC

For production, replace root token authentication with **AppRole**:

### Enable and Configure AppRole

```bash
# Enable AppRole auth method
vault auth enable approle

# Create policy for EDC
vault policy write edc-policy - <<EOF
path "secret/data/*" {
  capabilities = ["read", "list"]
}
path "secret/metadata/*" {
  capabilities = ["list"]
}
EOF

# Create AppRole for EDC
vault write auth/approle/role/edc-connector \
  token_policies="edc-policy" \
  token_ttl=1h \
  token_max_ttl=4h

# Get Role ID and Secret ID
vault read auth/approle/role/edc-connector/role-id
vault write -f auth/approle/role/edc-connector/secret-id
```

### Update EDC Configuration

Update `docker-compose.yml` to use AppRole instead of token:

```yaml
edc:
  environment:
    EDC_VAULT_HASHICORP_URL: "https://vault:8200"
    EDC_VAULT_HASHICORP_AUTH_METHOD: "approle"
    EDC_VAULT_HASHICORP_ROLE_ID: "<role-id>"
    EDC_VAULT_HASHICORP_SECRET_ID: "<secret-id>"
    # Remove EDC_VAULT_HASHICORP_TOKEN
```

## Backup and Recovery

### Backup Vault Data

```bash
# Create snapshot (Raft storage)
docker exec <vault-container> vault operator raft snapshot save /tmp/vault-snapshot.snap

# Copy snapshot out of container
docker cp <vault-container>:/tmp/vault-snapshot.snap ./backups/vault-$(date +%Y%m%d-%H%M%S).snap
```

### Restore from Snapshot

```bash
# Copy snapshot into container
docker cp ./backups/vault-snapshot.snap <vault-container>:/tmp/vault-snapshot.snap

# Restore snapshot
docker exec <vault-container> vault operator raft snapshot restore /tmp/vault-snapshot.snap
```

### Automated Backup Script

```bash
#!/bin/bash
# vault-backup.sh

BACKUP_DIR="/path/to/backups"
RETENTION_DAYS=30
VAULT_CONTAINER="<vault-container-id>"

# Create snapshot
docker exec $VAULT_CONTAINER vault operator raft snapshot save /tmp/vault-snapshot.snap

# Copy to backup directory
docker cp $VAULT_CONTAINER:/tmp/vault-snapshot.snap \
  $BACKUP_DIR/vault-$(date +%Y%m%d-%H%M%S).snap

# Clean up old backups
find $BACKUP_DIR -name "vault-*.snap" -mtime +$RETENTION_DAYS -delete
```

Schedule with cron:
```bash
# Daily backup at 2 AM
0 2 * * * /path/to/vault-backup.sh
```

## High Availability Setup

For critical production deployments, run Vault in HA mode with multiple nodes:

```yaml
# docker-compose-ha.yml example
services:
  vault-1:
    image: hashicorp/vault:1.18.4
    volumes:
      - vault-data-1:/vault/file
      - ./vault-1.hcl:/vault/config/vault.hcl:ro
    command: server -config=/vault/config/vault.hcl

  vault-2:
    image: hashicorp/vault:1.18.4
    volumes:
      - vault-data-2:/vault/file
      - ./vault-2.hcl:/vault/config/vault.hcl:ro
    command: server -config=/vault/config/vault.hcl

  vault-3:
    image: hashicorp/vault:1.18.4
    volumes:
      - vault-data-3:/vault/file
      - ./vault-3.hcl:/vault/config/vault.hcl:ro
    command: server -config=/vault/config/vault.hcl
```

Each node's configuration should include `retry_join` blocks pointing to other nodes.

## Additional Resources

- [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault/docs)
- [Production Hardening Guide](https://developer.hashicorp.com/vault/tutorials/operations/production-hardening)
- [Raft Storage Backend](https://developer.hashicorp.com/vault/docs/configuration/storage/raft)
- [Auto-Unseal Configuration](https://developer.hashicorp.com/vault/docs/configuration/seal)
- [AppRole Auth Method](https://developer.hashicorp.com/vault/docs/auth/approle)
- [Vault Operations Guide](https://developer.hashicorp.com/vault/tutorials/operations)
- [KodeKloud Vault Configuration Tutorial](https://notes.kodekloud.com/docs/HashiCorp-Certified-Vault-Associate-Certification/Learning-the-Vault-Architecture/Vault-Configuration-File)

## Support

For issues specific to MDS EDC integration with Vault, please open an issue in the [MDS EDC GitHub repository](https://github.com/Mobility-Data-Space/mds-edc/issues).

For Vault-specific questions, consult the [HashiCorp Vault Community Forum](https://discuss.hashicorp.com/c/vault).
