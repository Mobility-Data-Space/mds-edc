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

### Membership

Defined by the [MDS ODRL Profile](https://w3id.org/mobility-dataspace/2026/1/policy/) as
`https://w3id.org/mobility-dataspace/2026/1/policy/Membership`: the counterparty is an MDS participant.
Requires DCP: the evaluation passes when, and only when, the counterparty presents an MDS `MembershipCredential`
(issued by a trusted issuer, valid and not revoked). The credential must be typed `MembershipCredential` with the
MDS credentials context `https://w3id.org/mobility-dataspace/2026/1/credentials/context.jsonld` in its `@context`
(or typed with the full IRI `https://w3id.org/mobility-dataspace/2026/1/credentials/MembershipCredential`):
a `MembershipCredential` of another dataspace does not satisfy the constraint. Under DAPS no credentials are presented, so it always fails.

Only the `eq` operator with the right operand `active` is allowed; any other operator or right operand fails the evaluation.

Add the MDS policy context to use the short `Membership` name:

```http
POST /v4/policydefinitions
Content-Type: application/json

{
  "@context": [
    "https://w3id.org/edc/connector/management/v2",
    "https://w3id.org/mobility-dataspace/connector/management/v1",
    "https://w3id.org/mobility-dataspace/2026/1/policy/context.jsonld"
  ],
  "@type": "PolicyDefinition",
  "@id": "mds-members-only",
  "policy": {
    "@type": "Set",
    "permission": [{
      "action": "use",
      "constraint": [{
        "leftOperand": "Membership",
        "operator": "eq",
        "rightOperand": "active"
      }]
    }]
  }
}
```

The constraint is evaluated on catalog requests, contract negotiations and transfer processes, so non-members don't see the offer in the catalog.

### ParticipantId

Defined by the [MDS ODRL Profile](https://w3id.org/mobility-dataspace/2026/1/policy/) as
`https://w3id.org/mobility-dataspace/2026/1/policy/ParticipantId`: the counterparty's `did:web` DID.
Requires DCP: the DID is read from the `credentialSubject.id` of the MDS `MembershipCredential` presented by the
counterparty (see [Membership](#membership) for how the credential is recognized). Under DAPS no credentials are
presented, so it always fails.

| Operators             | Right operand                                                   |
|-----------------------|-----------------------------------------------------------------|
| `eq`, `neq`           | one DID: a non-empty string without `,`                         |
| `isAnyOf`, `isNoneOf` | one or more DIDs: a list of strings (or a single string)        |

Any other operator or right operand fails the evaluation.
`neq` and `isNoneOf` exclude participants from the MDS members: they fail for a counterparty that doesn't present
an MDS `MembershipCredential`.

Add the MDS policy context to use the short `ParticipantId` name:

```http
POST /v4/policydefinitions
Content-Type: application/json

{
  "@context": [
    "https://w3id.org/edc/connector/management/v2",
    "https://w3id.org/mobility-dataspace/connector/management/v1",
    "https://w3id.org/mobility-dataspace/2026/1/policy/context.jsonld"
  ],
  "@type": "PolicyDefinition",
  "@id": "selected-participants",
  "policy": {
    "@type": "Set",
    "permission": [{
      "action": "use",
      "constraint": [{
        "leftOperand": "ParticipantId",
        "operator": "isAnyOf",
        "rightOperand": ["did:web:connector.mobility-provider.example", "did:web:operator.example"]
      }]
    }]
  }
}
```

The constraint is evaluated on catalog requests, contract negotiations and transfer processes.
It is meant to replace [REFERRING_CONNECTOR](#referring_connector) once DCP is the only supported identity protocol.

### Group

Defined by the [MDS ODRL Profile](https://w3id.org/mobility-dataspace/2026/1/policy/) as
`https://w3id.org/mobility-dataspace/2026/1/policy/Group`: the MDS groups the counterparty belongs to, as issued by MDS.
Requires DCP: the groups are read from the `credentialSubject.groups` of the MDS `GroupMembershipCredential` presented
by the counterparty, a string of group identifiers separated by `,` (e.g. `"public-transport,parking-operators"`).
The credential is recognized like the `MembershipCredential` (see [Membership](#membership)): typed
`GroupMembershipCredential` with the MDS credentials context, or typed with the full IRI
`https://w3id.org/mobility-dataspace/2026/1/credentials/GroupMembershipCredential`. Under DAPS no credentials are
presented, so it always fails.

| Operators             | Right operand                                                                        |
|-----------------------|--------------------------------------------------------------------------------------|
| `eq`                  | one group identifier: a non-empty string without `,`                                 |
| `isAnyOf`, `isAllOf`  | one or more group identifiers: a list of strings without `,` (or a single string)    |

Any other operator or right operand fails the evaluation.
The constraint passes when the counterparty's `GroupMembershipCredential` lists the group (`eq`), at least one of the
groups (`isAnyOf`) or all of them (`isAllOf`). Group identifiers are maintained by the MDS Authority.

Add the MDS policy context to use the short `Group` name:

```http
POST /v4/policydefinitions
Content-Type: application/json

{
  "@context": [
    "https://w3id.org/edc/connector/management/v2",
    "https://w3id.org/mobility-dataspace/connector/management/v1",
    "https://w3id.org/mobility-dataspace/2026/1/policy/context.jsonld"
  ],
  "@type": "PolicyDefinition",
  "@id": "parking-and-transit",
  "policy": {
    "@type": "Set",
    "permission": [{
      "action": "use",
      "constraint": [{
        "leftOperand": "Group",
        "operator": "isAnyOf",
        "rightOperand": ["parking-operators", "transit-authorities"]
      }]
    }]
  }
}
```

The constraint is evaluated on catalog requests, contract negotiations and transfer processes, so counterparties
outside the groups don't see the offer in the catalog.

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
