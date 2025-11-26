# Azure Blob Storage

## Provider: Data Source Configuration

The provider's data must be stored in **Azure Blob Storage**.

### Data Address Properties

The provider configures the asset's `dataAddress` with:

- `type`: "AzureStorage" (required)
- `account`: Azure Storage account name (required)
- `container`: Container name (required)
- `blobName`: Specific blob identifier (optional, for single blob)
- `blobPrefix`: Pattern prefix for multiple blobs (optional, takes precedence over `blobName`)
- `keyName`: Vault reference for storage account access key (optional)

### Selection Modes

- **Single blob**: Use `blobName` to transfer one specific blob
- **Multiple blobs**: Use `blobPrefix` to transfer all blobs matching the prefix pattern
- **Full container**: Omit both `blobName` and `blobPrefix` to transfer entire container

Note: When both `blobPrefix` and `blobName` are specified, `blobPrefix` takes precedence.

### Supported Blob Types

- **Block blobs** - Recommended for most scenarios
- **Append blobs** - For append operations
- **Page blobs** - For random read/write operations

### Provider Azure Configuration

- Asset data must be stored in an Azure Blob Storage container
- Connector must have vault-stored access key with read access to the **source** storage account
- Connector must also have vault-stored access key with write access to the **consumer's destination** storage account
- Both source and destination storage accounts must allow public network access from the provider connector's IP range
- Default block size: 4MB (configurable via `edc.azure.block.size.mb`)
- Maximum file size with default settings: 200GB (due to Azure SDK's 50,000-block limit)

**Authentication:**

- Provider connector requires access keys for **both** provider and consumer storage accounts
- Source account: Provider uses its own vault-stored access key (referenced by `keyName`)
- Destination account: Provider receives SAS token from consumer connector
- If vault entry is unavailable, falls back to environment or system variables
- Access key must match the storage account name for proper resolution

### Provider Configuration Examples

#### Single Blob Transfer

```json
{
  "dataAddress": {
    "@type": "DataAddress",
    "type": "AzureStorage",
    "account": "providerStorageAccount",
    "container": "datasets",
    "blobName": "data.csv",
    "keyName": "vault-stored-access-key"
  }
}
```

#### Multiple Blobs Transfer

```json
{
  "dataAddress": {
    "@type": "DataAddress",
    "type": "AzureStorage",
    "account": "providerStorageAccount",
    "container": "datasets",
    "blobPrefix": "monthly-reports/",
    "keyName": "vault-stored-access-key"
  }
}
```

## Consumer: Data Destination Configuration

The consumer specifies `AzureStorage` as the transfer type in the `dataDestination` and provides:

- **Storage account information** - Account name and container
- **Optional blob name** - Destination blob path (for single blob transfers)
- **Optional folder name** - Destination folder path (for multiple blob transfers)
- **Azure credentials** - Vault reference for destination storage account access key

The consumer's connector generates a temporary SAS token from the vault-stored access key and passes it to the provider. The provider's data plane then uses this SAS token to push the data directly from the source Azure Blob Storage to the consumer's destination container.

### Transfer Process

#### 1. Transfer Initiation

The consumer initiates a transfer request via:

- The EDC UI
- A backend application using the Management API

The consumer specifies their Azure Blob Storage container details and credentials.

#### 2. SAS Token Generation

The consumer connector:

- Retrieves the destination storage account access key from the vault (using `keyName` reference)
- Generates a temporary Shared Access Signature (SAS) token from this access key
- Default SAS token expiration: 1 hour after creation
- Configurable via `edc.azure.token.expiry.time` for longer transfers

#### 3. Transfer Request

The consumer's control plane sends a transfer request to the provider's control plane, including:

- Contract agreement reference
- **Consumer's Azure Storage account and container information**
- **Generated SAS token** (time-limited access, not the raw access key)

#### 4. Data Transfer

The provider's data plane:

- Reads data from the provider's Azure Blob Storage using provider's vault-stored access key
- Writes data directly to the consumer's Azure Blob Storage using the **consumer-provided SAS token**
- Uses Azure Storage APIs for efficient transfer
- Handles block blob operations with configurable block sizes

**Naming behavior:**

- Single blob transfers: Uses the `blobName` specified in the destination
- Multiple blob transfers: Source blob names are preserved
- Optional `folderName` can be specified for organizing destination blobs

#### 5. Completion Notification

The transfer is completed asynchronously, and the consumer is notified upon completion or failure.

### Consumer Azure Configuration

- An Azure Blob Storage container ready to receive data
- Vault-stored access key for the destination storage account
- Consumer connector uses this access key to generate SAS tokens (never shared directly with provider)
- Storage account must allow public network access from the provider connector's IP range
- Proper storage account and container permissions for write operations

**Block size configuration:**

- Default: 4MB
- Configuration property: `edc.azure.block.size.mb`
- Note: Adjust block size for files larger than 200GB

**SAS token expiration:**

- Default: 1 hour
- Configuration property: `edc.azure.token.expiry.time`
- Recommendation: Increase for long-running transfers

### Consumer Configuration Examples

#### Single Blob Destination

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "azure-blob-single",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "AzureStorage",
    "account": "consumerStorageAccount",
    "container": "received-data",
    "blobName": "data.csv",
    "keyName": "vault-stored-access-key"
  },
  "protocol": "dataspace-protocol-http",
  "connectorId": "provider-connector-id",
  "connectorAddress": "https://provider.example.com/protocol"
}
```

#### Multiple Blobs Destination with Folder

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "azure-blob-multiple",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "AzureStorage",
    "account": "consumerStorageAccount",
    "container": "received-data",
    "folderName": "monthly-reports",
    "keyName": "vault-stored-access-key"
  },
  "protocol": "dataspace-protocol-http",
  "connectorId": "provider-connector-id",
  "connectorAddress": "https://provider.example.com/protocol"
}
```

## Authentication Methods

### Critical: Vault Secret Format

**IMPORTANT**: All secrets stored in HashiCorp Vault for EDC must follow a specific structure. The secret value must be wrapped in a `content` field.

**Correct Vault Secret Structure for Azure Storage Access Keys:**

```json
{
  "content": "your-azure-storage-access-key"
}
```

### Provider Authentication

The provider connector requires vault-stored access keys for **both** storage accounts:

**Source (Provider's Storage Account):**

```json
{
  "keyName": "provider-storage-access-key"
}
```

- Provider uses its own access key to read from the source storage account
- Key is retrieved from vault using `keyName` reference (which points to a secret with `content` field)
- If vault entry is unavailable, falls back to environment or system variables
- Access key must match the storage account name for proper resolution

**Destination (Consumer's Storage Account):**

- Provider receives a temporary SAS token from the consumer (not a raw access key)
- SAS token provides time-limited write access to consumer's destination
- Provider uses this token to write data to consumer's storage account

### Consumer Authentication

The consumer connector uses vault-stored access keys for **destination only**:

```json
{
  "keyName": "consumer-storage-access-key"
}
```

**Authentication Flow:**

1. Consumer retrieves access key from vault using `keyName` reference
2. Consumer connector generates a temporary SAS token from this access key
3. SAS token (not the raw access key) is transmitted to the provider connector
4. Provider uses the SAS token to write data to consumer's storage account

**SAS Token Properties:**

- Default expiration: 1 hour after creation
- Configurable expiration via `edc.azure.token.expiry.time`
- Recommended: Increase expiration for long-running transfers
- Tokens provide time-limited, secure access without exposing permanent credentials
- Consumer never shares raw access keys with the provider

## Upstream Documentation

For more detailed information about the Azure Storage data plane extension, see:

- [EDC Azure Technology Extensions](https://github.com/eclipse-edc/Technology-Azure/tree/main/extensions/data-plane/data-plane-azure-storage)
