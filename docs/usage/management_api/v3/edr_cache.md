# EDR Cache

The EDR (Endpoint Data Reference) cache stores data references received from transfer processes.

## Query EDR Entries

```http
POST /v3/edrs/request
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "QuerySpec",
  "offset": 0,
  "limit": 50
}
```

## Get EDR Entry Data Address

```http
GET /v3/edrs/{process-id}/dataaddress
```
