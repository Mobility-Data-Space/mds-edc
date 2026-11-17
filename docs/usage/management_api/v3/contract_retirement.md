# Contract Agreement Retirement

> **Deprecated**: these endpoints delegate to the native EDC contract agreement retirement and will be removed. Use the
> [v4 retirement endpoints](../contract_retirement.md) instead.

The retirement endpoint allows participants to retire contract agreements.

## Retire Agreement

```http
POST /v3/contractagreements/retirements
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "AgreementsRetirementEntry",
  "agreementId": "contract-agreement-id",
  "reason": "This contract agreement was retired since the physical counterpart is no longer valid."
}
```
