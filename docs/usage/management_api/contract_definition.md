# Contract Definition

A contract definition links assets with usage policies.

## Create Contract Definition

### For All Assets

```http
POST /v4/contractdefinitions
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "ContractDefinition",
  "@id": "definition-id",
  "accessPolicyId": "aPolicy",
  "contractPolicyId": "aPolicy",
  "assetsSelector": []
}
```

### For Specific Asset

```http
POST /v4/contractdefinitions
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "ContractDefinition",
  "@id": "definition-id",
  "accessPolicyId": "aPolicy",
  "contractPolicyId": "aPolicy",
  "assetsSelector": [
    {
      "@type": "Criterion",
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
      "operator": "in",
      "operandRight": "asset-id"
    }
  ]
}
```

### With Manual Approval (deprecated)

> **Deprecated**: the `manualApproval` private property will be removed. Use a contract policy with the
> [ManualApproval](policy_definition.md#manualapproval) constraint instead, that also makes the requirement visible in the catalog.

```http
POST /v4/contractdefinitions
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "ContractDefinition",
  "@id": "definition-id",
  "accessPolicyId": "aPolicy",
  "contractPolicyId": "aPolicy",
  "assetsSelector": [
    {
      "@type": "Criterion",
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
      "operator": "in",
      "operandRight": "asset-id"
    }
  ],
  "privateProperties": {
    "manualApproval": "true"
  }
}
```
