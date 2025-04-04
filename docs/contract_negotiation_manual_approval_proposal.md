# Contract negotiation manual approval proposal

### Requirements

Some use cases request to have the possibility to mark specific contract offers in a way that they won't get through
negotiation automatically but that they'll need user manual approval (or rejection).


### Current scenario

The Contract Negotiation handshake automatically goes through the different states between the two parts as described in
the documentation: https://eclipse-edc.github.io/documentation/for-contributors/control-plane/entities/#4-contract-negotiations

EDC doesn't provide a way to let users approve certain negotiations manually out of the box, but it provides the tool to
implement that.

Specifically, in the `StateMachine` component, that's the base of the negotiation flow, there's the possibility to set
what's called a `PendingGuard` (https://github.com/eclipse-edc/Connector/tree/bfe92d537217474f9e7c8e4d8758b16714ec3f0e/docs/developer/decision-records/2023-07-20-state-machine-guards).
In the `ContractNegotiation` state machine a `ContractNegotiationPendingGuard` service is already injected and ready to
be implemented.

It is a `Predicate` that takes in input a `ContractNegotiation` and returns a boolean.
If the result is false, the flow continues, otherwise the entity gets `pending` attribute set to `true`, that means that
the state machine won't process it anymore until the `pending` is `false` again.

### Implementation proposal

We can keep this as generic as possible and let the provider decide which assets must go through manual approval.
There are three possibilities here:
- bind this to the `ContractDefintion`, by setting a private property like `manualApproval=true`
- bind this to the `Asset`, by setting a private property like `manualApproval=true`
- both of them

Then the `PendingGuard` can set the appropriate condition to put in `pending` all the negotiations that are `PROVIDER`,
in `REQUESTED` state and that are related to definition/asset that have been set for manual approval:

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

So the negotiations that are `REQUESTED` and with the `pending` attribute can be obtained by the final user through the
[request contract negotiations endpoint](https://eclipse-edc.github.io/Connector/openapi/management-api/#/Contract%20Negotiation%20V3/queryNegotiationsV3)

The last missing part is the definition of two `Command`s:
- `ApproveContractNegotiation`
- `RejectContractNegotiation`

with the relative `CommandHandler` implementations that do respectively:
- set `pending` to false, transition state to `AGREEING`, save negotiation
- set `pending` to false, transition state to `TERMINATING` with the reason set in the `errorDetail` attribute, save
  negotiation

A pair of API endpoints will be needed to approve/reject the negotiation, and they would be like:

`POST .../negotiations/<contract_negotiation_id>/approve`
`POST .../negotiations/<contract_negotiation_id>/reject`

#### Additional feature: eventing

With the outlined approach the provider won't be able to be notified when there's a new negotiation waiting for approval.
To make that possible, we'll need to have the `ManualApprovalPendingGuard` to emit an event.
The event could be called `ContractNegotiationApprovalRequired` and it needs to be published on the `EventRouter`:
```java
eventRouter.publish(ContractNegotiationApprovalRequired.Builder.newInstance()
    // additional data to be put in the event
    .build()
);
```

If not already existent, an `EventSubscriber` that takes care to dispatch the event to a configured destination needs to
be implemented.

Other 2 events can be added:
- `ContractNegotiationApproved`
- `ContractNegotiationRejected`

and they will be thrown by the approve/reject command handler.
