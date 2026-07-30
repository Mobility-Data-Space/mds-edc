# Catalog

The catalog endpoint allows consumers to discover available assets from a provider connector.

## Request Catalog

```http
POST /v4/catalog/request
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "CatalogRequest",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp/2025-1",
  "counterPartyId": "PROVIDER_MDS_ID",
  "protocol": "dataspace-protocol-http:2025-1",
  "additionalScopes": []
}
```

### Pagination

By default, catalog requests have a limit of 50 elements. To request more elements, specify the limit using the `querySpec` parameter:

```http
POST /v4/catalog/request
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "CatalogRequest",
  "protocol": "dataspace-protocol-http:2025-1",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp/2025-1",
  "counterPartyId": "PROVIDER_MDS_ID",
  "querySpec": {
    "limit": 100,
    "offset": 0
  }
}
```

### Filter by Asset ID

```http
POST /v4/catalog/request
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "CatalogRequest",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp/2025-1",
  "counterPartyId": "PROVIDER_MDS_ID",
  "protocol": "dataspace-protocol-http:2025-1",
  "querySpec": {
    "offset": 0,
    "limit": 50,
    "filterExpression": [
      {
        "@type": "Criterion",
        "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
        "operator": "=",
        "operandRight": "asset-id"
      }
    ]
  }
}
```

## Request Dataset

```http
POST /v4/catalog/dataset/request
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "DatasetRequest",
  "@id": "asset-id",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp/2025-1",
  "counterPartyId": "PROVIDER_MDS_ID",
  "protocol": "dataspace-protocol-http:2025-1"
}
```
