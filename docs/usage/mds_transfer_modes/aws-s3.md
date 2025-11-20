# AWS S3

## Provider: Data Source Configuration

The provider's data must be stored in an **AWS S3 bucket** or S3-compatible storage.

### Data Address Properties

The provider configures the asset's `dataAddress` with:

- `type`: "AmazonS3" (required)
- `region`: AWS region (e.g., "us-east-1") (required)
- `bucketName`: S3 bucket name (required)
- `objectName`: Specific S3 object key (optional, for single object)
- `objectPrefix`: Filter for multiple objects (optional, for multiple objects)
- `folderName`: Logical grouping path (optional)
- `keyName`: Vault reference for AWS credentials (optional)

### Selection Modes

- **Single object**: Use `objectName` to transfer one specific object
- **Multiple objects**: Use `objectPrefix` to transfer all matching objects
- **Combined filtering**: Use both `folderName` and `objectPrefix` for filtered selection
- **Full bucket**: Omit all selection parameters to transfer entire bucket contents

### Provider AWS Configuration

- Asset data must be stored in an S3 bucket
- Connector must have IAM credentials or vault-stored credentials with the following permissions:
  - `s3:GetObject` on source objects
  - `s3:ListBucket` (when using `objectPrefix` for multiple objects)

### Provider Configuration Examples

#### Single Object Transfer

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "owl": "http://www.w3.org/2002/07/owl#",
    "mobilitydcatap": "https://w3id.org/mobilitydcat-ap/",
    "mobilitydcatap-theme": "https://w3id.org/mobilitydcat-ap/mobility-theme/",
    "adms": "http://www.w3.org/ns/adms#",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "skos": "http://www.w3.org/2004/02/skos/core#",
    "rdf": "http://www.w3.org/2000/01/rdf-schema#"
  },
  "@type": "Asset",
  "@id": "s3-asset-single",
  "properties": {
    "dct:title": "S3 Single File Dataset",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "INFRASTRUCTURE_AND_LOGISTICS"
    }
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "AmazonS3",
    "region": "us-east-1",
    "bucketName": "provider-data-bucket",
    "objectName": "datasets/data.csv",
    "keyName": "vault-stored-credentials-key"
  }
}
```

#### Multiple Objects Transfer

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "owl": "http://www.w3.org/2002/07/owl#",
    "mobilitydcatap": "https://w3id.org/mobilitydcat-ap/",
    "mobilitydcatap-theme": "https://w3id.org/mobilitydcat-ap/mobility-theme/",
    "adms": "http://www.w3.org/ns/adms#",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "skos": "http://www.w3.org/2004/02/skos/core#",
    "rdf": "http://www.w3.org/2000/01/rdf-schema#"
  },
  "@type": "Asset",
  "@id": "s3-asset-multiple",
  "properties": {
    "dct:title": "S3 Multiple Files Dataset",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "INFRASTRUCTURE_AND_LOGISTICS"
    }
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "AmazonS3",
    "region": "us-east-1",
    "bucketName": "provider-data-bucket",
    "objectPrefix": "datasets/",
    "folderName": "my-datasets",
    "keyName": "vault-stored-credentials-key"
  }
}
```

## Consumer: Data Destination Configuration

The consumer specifies `AmazonS3` as the transfer type in the `dataDestination` and provides:

- **S3 bucket information** - Region, bucket name, and optional object name or folder
- **AWS credentials** - Access key ID and secret access key, or temporary credentials with session token

The provider's data plane pushes the data directly to the consumer's destination S3 bucket.

### Transfer Process

#### 1. Transfer Initiation

The consumer initiates a transfer request via:

- The EDC UI
- A backend application using the Management API

The consumer specifies their S3 bucket details and credentials. The consumer's control plane sends a transfer request to the provider's control plane, including:

- Contract agreement reference
- **Consumer's S3 bucket information** (region, bucket name, optional object name or folder)
- **AWS credentials** (access key ID, secret access key, or temporary credentials)

#### 2. Data Transfer

The provider's data plane:

- Reads data from the provider's S3 bucket source
- Writes data directly to the consumer's S3 bucket using the provided credentials
- Uses configurable chunk sizes (default: 500MB) for stream reading
- Preserves object names for multi-object transfers, or uses specified `objectName` for single transfers

**Naming behavior:**

- Single-part transfers: Uses the `objectName` specified in the destination
- Multi-part transfers: Source object names become destination names
- The `folderName` groups objects regardless of source count

### Consumer AWS Configuration

- An S3 bucket ready to receive data
- IAM credentials with write access to the destination bucket:
  - `s3:PutObject` on destination objects
- Proper S3 bucket policies and permissions

**Chunk size configuration:**

- Default: 500MB
- Configuration property: `edc.dataplane.aws.sink.chunk.size.mb`
- Environment variable: `EDC_DATAPLANE_AWS_SINK_CHUNK_SIZE_MB`

### Consumer Configuration Examples

#### Single Object Destination

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "s3-asset-single",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "AmazonS3",
    "region": "us-west-2",
    "bucketName": "consumer-data-bucket",
    "objectName": "received-data/data.csv",
    "accessKeyId": "AKIA...",
    "secretAccessKey": "secret..."
  },
  "protocol": "dataspace-protocol-http",
  "connectorId": "provider-connector-id",
  "connectorAddress": "https://provider.example.com/protocol"
}
```

#### Multiple Objects Destination with Folder

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "s3-asset-multiple",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "AmazonS3",
    "region": "us-west-2",
    "bucketName": "consumer-data-bucket",
    "folderName": "received-datasets",
    "accessKeyId": "AKIA...",
    "secretAccessKey": "secret..."
  },
  "protocol": "dataspace-protocol-http",
  "connectorId": "provider-connector-id",
  "connectorAddress": "https://provider.example.com/protocol"
}
```

## Authentication Methods

### Vault-Based Credentials (Recommended)

Credentials can be stored in EDC's vault system and referenced by key:

```json
{
  "keyName": "vault-stored-credentials-key"
}
```

#### Critical: Vault Secret Format

**⚠️ IMPORTANT**: Vault secrets must be stored with a specific structure. The credentials JSON must be **serialized as a string** and wrapped in a `content` field.

**Correct Vault Secret Structure:**

All secrets stored in HashiCorp Vault for EDC must follow this format:

```json
{
  "content": "{\"accessKeyId\":\"AKIA...\",\"secretAccessKey\":\"secret...\"}"
}
```

#### Credential Types

The vault can store two types of AWS credentials inside the `content` field:

**Standard credentials (AwsSecretToken):**

```json
{
  "content": "{\"accessKeyId\":\"AKIA...\",\"secretAccessKey\":\"secret...\"}"
}
```

The serialized JSON string contains:
```json
{
  "accessKeyId": "AKIA...",
  "secretAccessKey": "secret..."
}
```

**Temporary credentials (AwsTemporarySecretToken):**

```json
{
  "content": "{\"accessKeyId\":\"ASIA...\",\"secretAccessKey\":\"secret...\",\"sessionToken\":\"IQoJb3JpZ2lu...\",\"expiration\":1234567890}"
}
```

The serialized JSON string contains:
```json
{
  "accessKeyId": "ASIA...",
  "secretAccessKey": "secret...",
  "sessionToken": "IQoJb3JpZ2lu...",
  "expiration": 1234567890
}
```

Note: `expiration` is a Unix timestamp indicating when the temporary credentials expire.

### Direct Credentials (Development Only)

For development environments, credentials can be specified directly in the data address:

```json
{
  "accessKeyId": "AKIA...",
  "secretAccessKey": "secret..."
}
```

### Temporary Credentials via AWS STS

Use AWS Security Token Service (STS) to generate temporary credentials with:

- Limited scope and lifetime
- Session token for enhanced security
- Automatic expiration (Unix timestamp format)

## Upstream Documentation

For more detailed information about the AWS S3 data plane extension, see:

- [EDC AWS Technology Extensions](https://github.com/eclipse-edc/Technology-Aws/tree/main/extensions/data-plane/data-plane-aws-s3)
