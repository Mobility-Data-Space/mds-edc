# Frequently Asked Questions (FAQ)

Welcome to the MDS EDC Connector FAQ! This section provides answers to common questions about deploying, configuring, and using the MDS EDC Connector.

If you can't find the answer to your question, please check our [GitHub Discussions](https://github.com/Mobility-Data-Space/mds-edc/discussions) or [open an issue](https://github.com/Mobility-Data-Space/mds-edc/issues).

## General Questions

### What is the MDS EDC Connector?

The MDS EDC Connector is an Eclipse Dataspace Components (EDC) distribution specifically tailored for the Mobility Data Space. It enables secure, sovereign data exchange between participants in the mobility ecosystem while following the Dataspace Protocol (DSP) specification.

### What is DAPS and why do I need it?

DAPS (Dynamic Attribute Provisioning Service) is an OAuth2-based authentication service required for connector-to-connector communication in production deployments.

It is responsible for the following:

- Issues signed tokens (Dynamic Attribute Tokens - DATs)
- Verifies connector identities
- Enforces participant authentication in the dataspace

### How do I secure my EDC Management API?

The Management API should be secured using:

1. **Strong API Keys**: Use long, random API keys (not `ApiKeyDefaultValue`)
2. **Network Isolation**: Restrict Management API access to internal networks.
3. **Rate Limiting**: Configure rate limits to prevent brute force attacks

## Deployment & Configuration

### Is the docker-compose setup production-ready?

**Partially.** The docker-compose configuration is designed as a **quick-start development and testing environment**. Some components require additional configuration for production use.

**Production-Ready Components:**

- PostgreSQL with persistent volumes
- Nginx with Let's Encrypt SSL
- EDC connector runtime

**Requires Production Configuration:**

- **Vault is running in dev mode** (in-memory storage, auto-unseal)
- **No automated backup strategy** included
- **No high availability** configuration
- **No monitoring/alerting** setup

For production deployments, see:

- [Production Vault Setup Guide](deployment/production_vault_setup.md)
- [Backup and Recovery Guide](deployment/backup_and_recovery.md)
- [Data Persistence Considerations](deployment/production_blueprint_nginx.md#data-persistence-and-production-considerations)

### What happens to Vault secrets when I restart containers?

This depends on which secrets and whether Vault is in dev mode or production mode.

**Current Setup (Dev Mode):**

Vault runs in **development mode** (`command: server -dev`), which stores data **in-memory only**.

For production deployments, configure Vault with persistent storage (Raft backend). See the [Production Vault Setup Guide](deployment/production_vault_setup.md).

### Why is Vault running in dev mode and what are the implications?

**Development mode** is used in the default docker-compose setup for ease of getting started, but it has significant limitations.

**Dev Mode Characteristics:**

- Stores all data **in-memory only** (ignores volume mounts)
- Auto-unseals on startup (no seal/unseal required)
- Uses fixed root token (security risk)
- No TLS encryption

**Production Mode Recommendation:**

- Persistent storage backend (Raft, Consul, etc.)
- Proper seal/unseal mechanism (manual or auto-unseal with KMS)
- TLS encryption for all communication
- Authentication methods (AppRole, OIDC, etc.)
- Audit logging enabled

See [Production Vault Setup Guide](deployment/production_vault_setup.md) for migration instructions.

### How do I store secrets in Vault for use with EDC?

**CRITICAL**: All secrets stored in HashiCorp Vault for EDC must follow a specific format. This is a common source of configuration errors.

**Required Format:**

Secrets must be stored with a `content` field wrapper:

```json
{
  "content": "your-secret-value-here"
}
```

### What happens if I delete Docker volumes?

**CRITICAL WARNING:** Deleting volumes **permanently destroys all data**.

### Can I run a connector locally and consume data from an online connector?

**No**, locally run connectors cannot exchange data with online connectors.

Contract negotiation and data transfer in EDC are asynchronous processes. The provider connector needs to send callback requests to the consumer connector's DSP endpoint. If your connector runs on `localhost`, it's not reachable from the internet, and these callbacks will fail.

For testing with online connectors, you need:

- A publicly accessible domain or IP address
- Proper DNS configuration
- HTTPS/TLS certificates
- Firewall rules allowing inbound connections on DSP ports

### Can I change the Participant ID of my connector?

**Yes**, but with important limitations:

You can always restart your connector with a different Participant ID. However:

1. **DAPS Configuration Required**: Your new Participant ID must be registered in the DAPS with the `referringConnector` claim.
2. **Existing Agreements Break**: All existing contract agreements will stop working because:
   - The Participant ID is referenced in both provider and consumer connectors
   - There's no mechanism for other connectors to know your Participant ID changed
   - Contract policies may be bound to specific Participant IDs
3. **Fresh Start**: Changing Participant ID essentially gives you a fresh start in the dataspace:
   - You'll need to renegotiate all contracts
   - Previous transfer history won't be associated with the new ID
   - Trust relationships may need to be re-established

### How do I configure the connector hostname?

The `EDC_HOSTNAME` variable must be set to the publicly accessible hostname where your connector can be reached (example `EDC_HOSTNAME=connector.example.com`)

The hostname is used to construct callback URLs for asynchronous operations. If set incorrectly, contract negotiations and transfers will fail.

### What database does MDS EDC support?

MDS EDC uses **PostgreSQL** for:

- Assets
- Policy definitions
- Contract definitions and agreements
- Transfer process state

## Data Exchange

### What data transfer methods are supported?

MDS EDC supports:

1. **HTTP Push (HttpData)**:
   - Provider pushes data to consumer's endpoint
   - Consumer provides a destination URL
   - Data is transferred directly

2. **HTTP Pull (HttpProxy)**:
   - Consumer pulls data from provider
   - Provider issues an Endpoint Data Reference (EDR)
   - Consumer uses EDR to fetch data

3. **Cloud Storage**:
   - AWS S3 (with proper extensions)
   - Azure Blob Storage (with proper extensions)

4. **Kafka** (with kafka extension):
   - Stream data via Kafka topics

### Can I use OAuth2-protected data sources?

**Yes!** The connector supports OAuth2-protected data sources.

Configure the data address with:

- `tokenUrl`: OAuth2 token endpoint
- `clientId`: OAuth2 client ID
- `clientSecretKey`: Vault key containing the client secret

The connector will automatically obtain and refresh OAuth2 tokens when accessing the data.

### What are policies and how do I use them?

Policies define **who can access your data** and **under what conditions**. They use the ODRL (Open Digital Rights Language) format.

MDS policy types:

- **Always-true policy**: Allow everyone
- **Time-based policy**: Restrict access to specific time periods
- **Connector-specific policy**: Allow only specific connectors
