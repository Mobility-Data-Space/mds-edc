# Contract Agreement

Contract agreements represent finalized contracts between a provider and consumer.

## Query Agreements

```http
POST /v3/contractagreements/request
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
