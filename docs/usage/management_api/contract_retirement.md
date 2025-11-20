# Contract Agreement Retirement

The retirement endpoint allows participants to retire contract agreements.

## Retire Agreement

```http
POST /v3/contractagreements/retirements
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/"
  },
  "agreementId": "contract-agreement-id",
  "tx:reason": "This contract agreement was retired since the physical counterpart is no longer valid."
}
```
