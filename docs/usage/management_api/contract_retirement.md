# Contract Agreement Retirement

The retirement endpoint allows participants to retire active contract agreements and list or reactivate retired ones.

## Retire Agreement

```http
POST /v4/contractagreements/retirements
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "AgreementsRetirementEntry",
  "agreementId": "contract-agreement-id",
  "reason": "This contract agreement was retired since the physical counterpart is no longer valid."
}
```

## Query Retired Agreements

```http
POST /v4/contractagreements/retirements/request
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "QuerySpec",
  "offset": 0,
  "limit": 50
}
```

## Reactivate Agreement

```http
DELETE /v4/contractagreements/retirements/{agreementId}
```
