# Contract Negotiation Manual Approval Proposal

## Overview

This document outlines a proposal for implementing manual approval functionality in the contract negotiation process. This feature allows specific contract offers to be flagged for manual review and approval (or rejection) instead of proceeding through the negotiation process automatically.

## Requirements

- Ability to mark specific contract offers for manual approval
- Prevent automatically approved negotiations for marked contracts
- Provide a mechanism for manual approval or rejection of flagged negotiations

## Current Scenario

The Contract Negotiation handshake in EDC (Eclipse Dataspace Connector) automatically progresses through different states between the two parties, as described in the [EDC documentation](https://eclipse-edc.github.io/documentation/for-contributors/control-plane/entities/#4-contract-negotiations).

While EDC doesn't provide built-in functionality for manual approval of negotiations, it offers the necessary tools to implement this feature.

Specifically, in the `StateMachine` component, that's the base of the negotiation flow, there's the possibility to set
what's called a `PendingGuard` (https://github.com/eclipse-edc/Connector/tree/bfe92d537217474f9e7c8e4d8758b16714ec3f0e/docs/developer/decision-records/2023-07-20-state-machine-guards).
In the `ContractNegotiation` state machine a `ContractNegotiationPendingGuard` service is already injected and ready to
be implemented.

It is a `Predicate` that takes in input a `ContractNegotiation` and returns a boolean.
If the result is false, the flow continues, otherwise the entity gets `pending` attribute set to `true`, that means that
the state machine won't process it anymore until the `pending` is `false` again.

## Implementation Proposal

### 1. Flagging for Manual Approval

To maintain flexibility, we propose allowing providers to decide which assets require manual approval. This can be implemented in one of three ways:

1. Bind to the `ContractDefinition` by setting a private property: `manualApproval=true`
2. Bind to the `Asset` by setting a private property: `manualApproval=true`
3. Use both of the above methods

### 2. Implementing the PendingGuard

We'll implement a `ManualApprovalPendingGuard` that sets appropriate conditions to put negotiations in a `pending` state. This guard will affect negotiations that are:
- Of type `PROVIDER`
- In the `REQUESTED` state
- Related to a contract definition or asset flagged for manual approval

Here's a sample implementation:

```java
public class ManualApprovalPendingGuard implements ContractNegotiationPendingGuard {
    @Override
    public boolean test(ContractNegotiation contractNegotiation) {
        boolean blockingCondition = // if the contract definition and/or asset are set for manual approval
        return (contractNegotiation.getType() == ContractNegotiation.Type.PROVIDER
                && contractNegotiation.getState() == ContractNegotiationStates.REQUESTED.code()
                && blockingCondition);
    }
}
```

### 3. Retrieving Pending Negotiations

Negotiations that are `REQUESTED` and have the `pending` attribute can be retrieved by the user through the [request contract negotiations endpoint](https://eclipse-edc.github.io/Connector/openapi/management-api/#/Contract%20Negotiation%20V3/queryNegotiationsV3).

### 4. Implementing Approval/Rejection Commands

We need to define two new `Command`s:
1. `ApproveContractNegotiation`
2. `RejectContractNegotiation`

The corresponding `CommandHandler` implementations will:
- For approval: Set `pending` to false, transition state to `AGREEING`, and save the negotiation
- For rejection: Set `pending` to false, transition state to `TERMINATING` with the reason set in the `errorDetail` attribute, and save the negotiation

### 5. API Endpoints

We'll need to add two new API endpoints for approving or rejecting negotiations:

```
POST .../negotiations/<contract_negotiation_id>/approve
POST .../negotiations/<contract_negotiation_id>/reject
```

## Additional Feature: Eventing

To enable notifications when a new negotiation is waiting for approval, we can implement an eventing system:

1. The `ManualApprovalPendingGuard` will emit a `ContractNegotiationApprovalRequired` event.
2. This event will be published on the `EventRouter`:
```java
eventRouter.publish(ContractNegotiationApprovalRequired.Builder.newInstance()
    // additional data to be put in the event
    .build()
);
```

3. Implement an `EventSubscriber` to dispatch the event to a configured destination (if not already existing).

Additionally, we can add two more events:
- `ContractNegotiationApproved`
- `ContractNegotiationRejected`

These events will be thrown by the approve/reject command handlers respectively.

## Conclusion

This proposal outlines a flexible and extensible approach to implementing manual approval for contract negotiations. It leverages existing EDC components while introducing new elements to support the required functionality. This implementation will allow for greater control and oversight in the contract negotiation process, particularly for sensitive or high-value transactions.
