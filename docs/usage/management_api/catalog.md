# Catalog

The catalog endpoint allows consumers to discover available assets from a provider connector.

## Request Catalog

```http
POST /v3/catalog/request
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "CatalogRequest",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "counterPartyId": "PROVIDER_MDS_ID",
  "protocol": "dataspace-protocol-http",
  "additionalScopes": []
}
```

### Pagination

By default, catalog requests have a limit of 50 elements. To request more elements, you need to explicitly specify the limit using the `querySpec` parameter:

```http
POST /v3/catalog/request
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "CatalogRequest",
  "protocol": "dataspace-protocol-http",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "counterPartyId": "PROVIDER_MDS_ID",
  "querySpec": {
    "limit": 100,
    "offset": 0
  }
}
```
