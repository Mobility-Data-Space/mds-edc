# Observer: DPS-based Event Dispatch Proposal

## Overview

This proposal replaces the observer's current reliance on control-plane (CP) events with events sourced from the
**Data Plane Signaling (DPS)** API. The result is a simpler, more precise event model (two event types instead of
three) that is architecturally decoupled from the control plane and naturally ready for a future where the data plane
could run independently of the connector.

This work is already partially anticipated by two `TODO` comments in the codebase pointing to
[issue #558](https://github.com/Mobility-Data-Space/mds-edc/issues/558):

- `StartObserverTransfer.java:41` — the hardcoded `HttpData` data destination is flagged as temporary until DPS is
  adopted.
- `StoreObserverAddress.java:44` — the `TransferProcessStarted` listener is flagged as a candidate for replacement
  with a DPS `Started` event listener.

---

## Current Situation

The observer currently reacts to three CP-side events emitted by the EDC state machines:

| CP Event                       | Observer Event Type                            | Side     | Meaning                                          |
|--------------------------------|------------------------------------------------|----------|--------------------------------------------------|
| `ContractNegotiationFinalized` | `org.eclipse.edc.ContractNegotiationFinalized` | both     | A contract agreement has been established        |
| `TransferProcessStarted`       | `org.eclipse.edc.TransferProcessStarted`       | both     | A data transfer channel has been opened          |
| `ContractAgreementRetired`     | `eu.dataspace.mds.ContractAgreementRetired`    | provider | An agreement has been retired and transfer ended |

This model has several drawbacks:

- **Semantic mismatch** — `ContractNegotiationFinalized` signals that an agreement exists, but not that data is
  actually flowing. `TransferProcessStarted` is the real "data flow is live" signal, but because the two events are
  separate the observer consumer must correlate them.
- **Future fragility** — if the data plane is ever detached from the connector process, these CP events will no
  longer be reachable from the data plane without adding a new cross-process event bus, which defeats the purpose of
  the separation.
- **Asymmetric coverage** — the retirement event is a custom MDS event; the other two are generic EDC events.
  Terminations for reasons _other_ than retirement (e.g. expiry, explicit suspension, protocol error) are not covered.

---

## Proposed Change

Replace the three CP-based events with two DPS-lifecycle events:

| DPS trigger             | New Observer Event Type               | Side              | Meaning                                                  |
|-------------------------|---------------------------------------|-------------------|----------------------------------------------------------|
| DPS `start` / `started` | `eu.dataspace.mds.DataFlowStarted`    | provider/consumer | An agreement is in place and a data flow has been opened |
| DPS `terminate`         | `eu.dataspace.mds.DataFlowTerminated` | provider/consumer | A data flow has ended, for any reason                    |

### Why DPS

The DPS API (`/v1/dataflows`) is the single lifecycle authority for active data flows. It already has clean
request/response semantics:

- `POST /v1/dataflows` (start) — called by the consumer to open a new flow. On the **provider** side this is the
  inbound `start` request handled by the data-plane. On the **consumer** side there is no outbound `start` call;
  instead, the data-plane receives a `started` notification from the provider, which carries the same lifecycle
  meaning: the flow is now live. Both hooks therefore serve as the trigger for `DataFlowStarted`.
- `DELETE /v1/dataflows/{id}` (terminate) — either party can trigger this; the reasons include retirement, expiry,
  protocol termination, or explicit operator action.

These two transitions map directly onto what the observer actually cares about:
1. *"A data flow is now live between two parties for a given asset and agreement."*
2. *"A data flow has ended, and here is why."*

### Event Payloads

**`DataFlowStarted`**

The full `ContractAgreement` is attached to this event, retrieved from the agreement store at dispatch time using
the `agreementId` carried in the DPS request/notification. This makes the event self-contained: consumers get the
agreement's policy, signing date, provider/consumer identities, and asset ID in one payload without a separate lookup.

```json
{
  "type": "eu.dataspace.mds.DataFlowStarted",
  "source": "did:web:participant.example.com",
  "dataFlowId": "...",
  "contractAgreement": {
    "id": "...",
    "providerId": "...",
    "consumerId": "...",
    "assetId": "...",
    "contractSigningDate": 1700000000,
    "policy": { "...": "..." }
  }
}
```

**`DataFlowTerminated`**

```json
{
  "type": "eu.dataspace.mds.DataFlowTerminated",
  "source": "did:web:participant.example.com",
  "dataFlowId": "...",
  "agreementId": "...",
  "assetId": "...",
  "providerId": "...",
  "consumerId": "...",
  "reason": "retirement | expiry | terminated | ..."
}
```

The `reason` field captures the termination cause, making retirement-driven terminations distinguishable from other
causes without requiring a separate event type.

---

## Architecture Improvement

```
Current flow (CP-coupled):
  CP state machine ──► ContractNegotiationFinalized ──► StoreObserverEvent
  CP state machine ──► TransferProcessStarted        ──► StoreObserverEvent
  Retirement SPI   ──► ContractAgreementRetired       ──► StoreObserverEvent

Proposed flow (DPS-coupled):
  DPS API layer ──► DataFlowStarted    ──► StoreObserverEvent
  DPS API layer ──► DataFlowTerminated ──► StoreObserverEvent
```

The DPS layer is the natural boundary: it is already the entry point for all data flow lifecycle operations and is
decoupled from the CP state machines. If the data plane is eventually extracted into a standalone service, the
DPS API layer moves with it and the observer integration remains intact — no additional cross-process wiring needed.

---

## Migration Plan

Because external consumers already rely on the current event types, the change can be introduced gradually:

1. **Add the two new DPS-backed event types** to the observer alongside the existing three CP-backed types.
   Both sets of events are dispatched in parallel.
2. **Wire the connector to emit `DataFlowStarted` / `DataFlowTerminated`** from the DPS layer.
3. **Announce deprecation** of the three CP-backed event types.
4. **Allow a stabilisation period** so the testing team can migrate to the new event types.
5. **Remove the CP-backed event types**.

This approach avoids a big-bang migration and keeps the observer usable throughout the transition.

---

## Implementation Scope

The change is contained and surgical:

- Two new `ObserverEvent` record types: `DataFlowStarted` and `DataFlowTerminated`.
- A new subscriber (or extension of `StoreObserverEvent`) that reacts to the DPS lifecycle hooks instead of CP events.
- No schema changes to the event store or the retry mechanism — the store is payload-agnostic.
- No changes to `ObserverEventDispatcher`, `ObserverEventRetryJob`, or any delivery-side code.
- The existing three CP-backed types remain untouched until the deprecation window closes.

Time estimate: the new events and their wiring should be achievable within a single sprint.
