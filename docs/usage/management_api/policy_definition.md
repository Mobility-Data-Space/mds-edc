# Policy Definition

A policy definition specifies the rules and conditions for accessing an asset.

## Create Policy Definition

```http
POST /v4/policydefinitions
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "PolicyDefinition",
  "@id": "definition-id",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Set",
    "permission": [{
      "action": "use",
      "constraint": [{
        "and": [{
          "leftOperand": "REFERRING_CONNECTOR",
          "operator": "odrl:eq",
          "rightOperand": "MDSLXXX.XXXXX"
        },
        {
          "leftOperand": "POLICY_EVALUATION_TIME",
          "operator": "odrl:lt",
          "rightOperand": "2025-07-20T12:34:56Z"
        }]
      }]
    }]
  }
}
```

## Validate Policy Definition

Use this endpoint to check whether a policy evaluates correctly before linking it to a contract definition.

```http
POST /v4/policydefinitions/{id}/validate
```

## MDS Policy Constraints

### REFERRING_CONNECTOR

The `rightOperand` will be checked against the `referringConnector` claim.
The `rightOperand` needs to be a `String`; if multiple values are supposed to be used (e.g. with the `isPartOf` operator)
they will need to be separated with a comma, e.g.:

```json
"rightOperand": "MDSLXXX.XXXXX,MDSLYYY.YYYYY,MDSLZZZ.ZZZZZ,..."
```

If the `odrl:eq` operator is used, the evaluation will pass if the claim equals the `rightOperand`.
If the `odrl:isPartOf` operator is used, the evaluation will pass if the claim is contained in the `rightOperand`.

### POLICY_EVALUATION_TIME

The `rightOperand` needs to be a `String` containing a date and time formatted in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601), e.g.:

```json
"rightOperand": "2025-07-20T12:34:56Z"
```
