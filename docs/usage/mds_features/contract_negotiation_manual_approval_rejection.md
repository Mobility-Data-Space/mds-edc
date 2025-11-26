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

To enable manual approval for specific contract offers, set the following **private property** on the contract definition:

```text
https://w3id.org/edc/v0.0.1/ns/manualApproval = true
```

> **Important**: This is a **private property**, meaning it is not shared in the catalog and is only visible to the provider.

### Contract Definition Example

#### Via Management API

**Endpoint**: `POST /v3/contractdefinitions`

**Request Body**:

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractDefinition",
  "@id": "contract-def-manual-approval",
  "accessPolicyId": "policy-id",
  "contractPolicyId": "policy-id",
  "assetsSelector": [
    {
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
      "operator": "=",
      "operandRight": "asset-id"
    }
  ],
  "privateProperties": {
    "manualApproval": true
  }
}
```

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

**Endpoint**: `POST /v3/contractnegotiations/{negotiationId}/approve`

**Example**:

```bash
curl -X POST "https://provider.example.com/api/management/v3/contractnegotiations/negotiation-123/approve" \
  -H "X-Api-Key: your-api-key" \
  -H "Content-Type: application/json"
```

**Result**: Negotiation moves from "REQUESTED" to "AGREED" state.

### Rejecting a Negotiation

To reject a pending negotiation:

**Endpoint**: `POST /v3/contractnegotiations/{negotiationId}/reject`

**Example**:

```bash
curl -X POST "https://provider.example.com/api/management/v3/contractnegotiations/negotiation-123/reject" \
  -H "X-Api-Key: your-api-key" \
  -H "Content-Type: application/json"
```

**Result**: Negotiation moves to "TERMINATED" state

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
