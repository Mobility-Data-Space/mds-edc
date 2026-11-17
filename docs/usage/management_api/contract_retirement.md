# Contract Agreement Retirement

A provider can _prematurely_ retire an active contract agreement, e.g. when it is the digital representation of a
physical agreement that changed through legal means. Once an agreement is retired:

- every running transfer process based on it is terminated by the policy monitor;
- new transfer requests using it are rejected.

Retirement is a native EDC feature: the retirement state (`retired`, `retirementReason`, `retirementDate`) is stored on
the contract agreement itself and is managed through the [Contract Agreement](./contract_agreement.md) API.

## Retire Agreement

```http
POST /v4/contractagreements/{agreementId}/retire
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "RetireAgreement",
  "reason": "This contract agreement was retired since the physical counterpart is no longer valid."
}
```

Responses: `204` retired, `404` agreement not found, `409` agreement already retired.
A `ContractAgreementRetired` event is emitted.

## Un-retire Agreement

```http
POST /v4/contractagreements/{agreementId}/unretire
```

Responses: `204` un-retired, `404` agreement not found, `409` agreement is not retired.
A `ContractAgreementUnretired` event is emitted.

## Query Retired Agreements

```http
POST /v4/contractagreements/request
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "QuerySpec",
  "filterExpression": [
    { "operandLeft": "retired", "operator": "=", "operandRight": true }
  ]
}
```

The retirement state is also part of every contract agreement returned by `GET /v4/contractagreements/{agreementId}`:

```json
{
  "@type": "ContractAgreement",
  "@id": "contract-agreement-id",
  "retired": true,
  "retirementReason": "This contract agreement was retired since the physical counterpart is no longer valid.",
  "retirementDate": 1763374979
}
```

## Deprecated endpoints

The former MDS `/v4/contractagreements/retirements` (and `/v3/contractagreements/retirements`) endpoints are
**deprecated** and will be removed in a future release. They are kept for backward compatibility and operate on the
same retirement state as the endpoints above:

| Deprecated                                                                 | Replacement                                                        |
|----------------------------------------------------------------------------|--------------------------------------------------------------------|
| `POST /v4/contractagreements/retirements` (`AgreementsRetirementEntry`)    | `POST /v4/contractagreements/{agreementId}/retire` (`RetireAgreement`) |
| `POST /v4/contractagreements/retirements/request`                          | `POST /v4/contractagreements/request` filtering on `retired = true` |
| `DELETE /v4/contractagreements/retirements/{agreementId}`                  | `POST /v4/contractagreements/{agreementId}/unretire`               |

On the deprecated query endpoint the legacy fields `agreementId`, `reason` and `agreementRetirementDate` are
translated to `id`, `retirementReason` and `retirementDate`.

## Migration

Existing retirements stored in the `edc_agreement_retirement` table are moved onto the corresponding contract agreements
by the `V1_13_0` database migration, which then drops the legacy table.
