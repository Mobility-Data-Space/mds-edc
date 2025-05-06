# Contract Negotiation manual approval/rejection

This feature permits manual contract negotiation approval/rejection on the provider side.

## How To
To set up contract offers that need to be approved manually, there is the need to set this private property on the contract
definition:
- `https://w3id.org/edc/v0.0.1/ns/manualApproval = true`

All the contract offer generated from will be stopped at the "REQUESTED" state, waiting for provider interaction.

The provider can find the "pending" negotiation by filtering them through the `/v3/contractnegotiations/request` endpoint
using this body:
```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "filterExpression": [
    {
      "operandLeft": "pending",
      "operator": "=",
      "operandRight": true
    }
  ]
}
```

To approve a negotiation:
`POST /v3/contractnegotiations/<id>/approve`

To reject a negotiation:
`POST /v3/contractnegotiations/<id>/reject`
