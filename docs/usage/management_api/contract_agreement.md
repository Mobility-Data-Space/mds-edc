# Contract Agreement

Contract agreements represent finalized contracts between a provider and consumer.

## Query Agreements

```http
POST /v4/contractagreements/request
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "QuerySpec",
  "offset": 0,
  "limit": 50
}
```

## Get Agreement by ID

```http
GET /v4/contractagreements/{agreementId}
```

## Get Negotiation by Agreement ID

```http
GET /v4/contractagreements/{agreementId}/negotiation
```
