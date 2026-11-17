# Agreements Retirement API (deprecated)

Contract agreement retirement is natively supported by EDC since version 1.0.0: the retirement state
(`retired`, `retirementReason`, `retirementDate`) is stored on the `ContractAgreement` itself, a policy pre-validator
registered on the `transfer process` and `policy monitor` scopes terminates running transfers and rejects new ones, and
the EDC management API exposes:

- `POST /v4/contractagreements/{id}/retire` (body of type `RetireAgreement` with a `reason`)
- `POST /v4/contractagreements/{id}/unretire`
- `POST /v4/contractagreements/request` (filter on `retired = true` to list retired agreements)

The former MDS `retirement-evaluation-{spi,core,store-sql}` modules have been removed in favour of the upstream
implementation, and the `edc_agreement_retirement` table is migrated onto `edc_contract_agreement` by the `V1_13_0`
database migration. The `ContractAgreementRetired`/`ContractAgreementUnretired` events are now the EDC ones
(`org.eclipse.edc.connector.controlplane.contract.spi.event.contractagreement`).

## retirement-evaluation-api

This module only keeps the legacy `/v3/contractagreements/retirements` and `/v4/contractagreements/retirements`
endpoints for backward compatibility. They are **deprecated** and delegate every operation to the EDC
`ContractAgreementService`:

| Deprecated endpoint                                     | Delegates to                                         |
|---------------------------------------------------------|------------------------------------------------------|
| `POST /contractagreements/retirements`                  | `ContractAgreementService.retireAgreement`           |
| `POST /contractagreements/retirements/request`          | `ContractAgreementService.search` with `retired = true` |
| `DELETE /contractagreements/retirements/{agreementId}`  | `ContractAgreementService.unretireAgreement`         |

The legacy `AgreementsRetirementEntry` request/response format and status codes are preserved (reactivating an agreement
that is not retired still answers `404`). Query fields `agreementId`, `reason` and `agreementRetirementDate` are
translated to `id`, `retirementReason` and `retirementDate`.
