# Transfer Process

The transfer process manages the actual data transfer between provider and consumer.

## Initiate Transfer Process

### PUSH Flow

```http
POST /v3/transferprocesses
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "protocol": "dataspace-protocol-http",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "contractId": "contract-id",
  "transferType": "HttpData-PUSH",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://example.com"
  }
}
```

### PULL Flow

```http
POST /v3/transferprocesses
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "protocol": "dataspace-protocol-http",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "contractId": "contract-id",
  "transferType": "HttpData-PULL"
}
```

### S3 PUSH Flow

```http
POST /v3/transferprocesses
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "protocol": "dataspace-protocol-http",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "contractId": "contract-id",
  "transferType": "AmazonS3-PUSH",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "AmazonS3",
    "region": "us-east-1",
    "bucketName": "my-bucket",
    "keyName": "my-file.json"
  }
}
```

### Azure Blob PUSH Flow

```http
POST /v3/transferprocesses
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "protocol": "dataspace-protocol-http",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "contractId": "contract-id",
  "transferType": "AzureStorage-PUSH",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "AzureStorage",
    "account": "myaccount",
    "container": "mycontainer",
    "blobName": "my-file.json"
  }
}
```

### Kafka PULL Flow

```http
POST /v3/transferprocesses
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "protocol": "dataspace-protocol-http",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "contractId": "contract-id",
  "transferType": "Kafka-PULL"
}
```
