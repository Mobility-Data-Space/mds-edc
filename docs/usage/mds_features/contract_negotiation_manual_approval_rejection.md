# Manual Contract Negotiation Approval

The Manual Contract Negotiation Approval feature enables providers to implement human-in-the-loop approval workflows for contract negotiations. Instead of automatically accepting all valid contract requests, providers can review and manually approve or reject each negotiation request.

This feature is essential for use cases requiring:

- Human oversight of data sharing decisions
- Business approval workflows
- Compliance review processes
- Selective data access control

## How It Works

### Default Behavior (Automatic Approval)

By default, EDC connectors automatically process contract negotiations:

1. Consumer requests a contract based on an offer
2. Provider validates the request against policies
3. If policies are satisfied, the contract is automatically agreed
4. Transfer can begin immediately

### Manual Approval Behavior

With manual approval enabled:

1. Consumer requests a contract based on an offer
2. Provider validates the request against policies
3. **Contract negotiation stops at "REQUESTED" state**
4. **Provider must manually approve or reject**
5. Only after approval does the contract proceed to "AGREED" state

## Configuration

### Enabling Manual Approval

The EDC manual approval must be enabled on the provider connector with the following setting (environment variable
`EDC_NEGOTIATION_APPROVAL_ENABLED`):

```properties
edc.negotiation.approval.enabled = true
```

Without it, negotiations are never held, and a warning is logged at startup.

### Requiring Manual Approval on an Offer

To require manual approval for a contract offer, add the MDS `ManualApproval` constraint to a permission of its
**contract policy**:

```json
{ "leftOperand": "ManualApproval", "operator": "eq", "rightOperand": "true" }
```

The constraint is part of the offer, so consumers see in the catalog that the provider approves negotiations by hand.
See [ManualApproval](../management_api/policy_definition.md#manualapproval) for the full policy definition.

### Contract Definition Example

#### Via Management API

Create the policy definition with the `ManualApproval` constraint:

**Endpoint**: `POST /v4/policydefinitions`

**Request Body**:

```json
{
  "@context": [
    "https://w3id.org/edc/connector/management/v2",
    "https://w3id.org/mobility-dataspace/connector/management/v1",
    "https://w3id.org/mobility-dataspace/2026/1/policy/context.jsonld"
  ],
  "@type": "PolicyDefinition",
  "@id": "manual-approval",
  "policy": {
    "@type": "Set",
    "permission": [{
      "action": "use",
      "constraint": [{
        "leftOperand": "ManualApproval",
        "operator": "eq",
        "rightOperand": "true"
      }]
    }]
  }
}
```

Then use it as contract policy:

**Endpoint**: `POST /v4/contractdefinitions`

**Request Body**:

```json
{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "ContractDefinition",
  "@id": "contract-def-manual-approval",
  "accessPolicyId": "access-policy-id",
  "contractPolicyId": "manual-approval",
  "assetsSelector": [
    {
      "@type": "Criterion",
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
      "operator": "=",
      "operandRight": "asset-id"
    }
  ]
}
```

### Deprecated: `manualApproval` contract definition property

Setting the private property `https://w3id.org/edc/v0.0.1/ns/manualApproval = true` on the contract definition still
holds negotiations, but it is **deprecated** and will be removed: it is not visible to consumers, and a warning is logged
every time it is used. Replace it with a contract policy carrying the `ManualApproval` constraint.

## Provider Workflow

### Finding Pending Negotiations

Query for negotiations waiting for approval using the contract negotiations request endpoint:

**Endpoint**: `POST /v3/contractnegotiations/request`

**Request Body**:

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

### Approving a Negotiation

To approve a pending negotiation:

**Endpoint**: `POST /v4/contractnegotiations/{negotiationId}/approve`

**Example**:

```bash
curl -X POST "https://provider.example.com/api/management/v4/contractnegotiations/negotiation-123/approve" \
  -H "X-Api-Key: your-api-key"
```

**Result**: Negotiation moves from "REQUESTED" to "AGREED" state, a `ContractNegotiationApproved` event is emitted.

### Rejecting a Negotiation

To reject a pending negotiation:

**Endpoint**: `POST /v4/contractnegotiations/{negotiationId}/reject`

**Example**:

```bash
curl -X POST "https://provider.example.com/api/management/v4/contractnegotiations/negotiation-123/reject" \
  -H "X-Api-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"@context": "https://w3id.org/mobility-dataspace/connector/management/v1", "@type": "RejectNegotiation"}'
```

**Result**: Negotiation moves to "TERMINATED" state, a `ContractNegotiationRejected` event is emitted.

Both endpoints answer `409 Conflict` when the negotiation is not a provider negotiation pending in "REQUESTED" state.

## Consumer Experience

### What Consumers See

1. **Negotiation Initiated**: Consumer sends contract negotiation request
2. **Waiting State**: Negotiation enters "REQUESTED" state on both sides
3. **Pending**: Consumer sees negotiation is pending provider action
4. **Eventual Response**:
   - **If Approved**: Negotiation completes, contract agreement created, transfer can begin
   - **If Rejected**: Negotiation terminated, no contract agreement

### Consumer Best Practices

- **Monitor State**: Regularly check negotiation state
- **Set Expectations**: Understand that manual approval may take time (hours to days)
- **Contact Provider**: For time-sensitive requests, consider contacting the provider out-of-band
- **Implement Retries**: Build retry logic for rejected negotiations if appropriate
