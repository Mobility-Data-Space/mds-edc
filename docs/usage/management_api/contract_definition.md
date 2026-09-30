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

### With Manual Approval

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
