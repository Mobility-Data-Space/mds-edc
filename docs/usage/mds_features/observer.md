# Observer

The Observer feature enables a participant connector to automatically establish a contractual channel to a designated
observer at startup, then dispatch CloudEvents-compliant notifications to that observer whenever relevant dataspace events occur.

The feature involves two distinct roles:

- **Observer** — a connector that exposes an event-sink HTTP endpoint and receives event notifications
- **Participant** — a connector configured to negotiate with the observer and forward events to it

## How It Works

1. At startup, the participant connector fetches the observer's catalog and initiates a contract negotiation for the observer's event-sink dataset.
2. Once the negotiation reaches `FINALIZED`, a transfer process is started using the configured transfer profile.
3. When the transfer reaches `STARTED`, the data address (endpoint URL and authorization token) delivered by the observer is stored in the vault.
4. From that point on, every qualifying event is serialized as a CloudEvent and POSTed to the observer endpoint.

The feature is **idempotent**: if a finalized negotiation with the observer already exists at startup, no new negotiation is initiated.

---

## Observer Setup

The observer connector must create a data offer that exposes its event-sink endpoint. The offer's data address points to 
the HTTP endpoint that will receive events, protected by an API key stored in the vault.

### 1. Store the API Key in the Vault

Before creating the offer, add the API key that protects your event endpoint to the vault. The secret value must be wrapped in a `content` field:

```bash
vault kv put secret/observer-api-key content='your-api-key-value'
```

### 2. Create the Event-Sink Offer

**Endpoint:** `POST /v3/assets`

**Headers:**

```
X-Api-Key: YourApiKey
Content-Type: application/json
```

**Request Body:**

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "Asset",
  "@id": "event-sink",
  "properties": {},
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://observer.example.com/api/v1/events",
    "method": "POST",
    "proxyBody": "true",
    "authKey": "X-Api-Key",
    "secretName": "observer-api-key"
  }
}
```

**Replace:**

- `https://observer.example.com/api/v1/events` — your mds-observer endpoint
- `observer-api-key` — the vault key containing the API key

The asset `@id` (`event-sink` in the example) is the **dataset ID** that must be communicated to participant connectors so they can identify the correct offer in the catalog.

### 3. Create a Contract Definition

Create a policy and contract definition that allows participant connectors to negotiate access to the event-sink asset. 
The policy requirements depend on your dataspace setup (DAPS or DCP).

---

## Participant Configuration

The participant connector requires five configuration properties to enable the observer feature. If any property is missing, the feature is disabled at startup with a warning log.

| Property                            | Description                                   | Example                                |
|-------------------------------------|-----------------------------------------------|----------------------------------------|
| `edc.mds.observer.id`               | Participant ID of the observer connector      | `did:web:observer.example.com`         |
| `edc.mds.observer.url`              | DSP protocol URL of the observer connector    | `https://observer.example.com/api/dsp` |
| `edc.mds.observer.dataset.id`       | Dataset ID of the observer's event-sink offer | `event-sink`                      |
| `edc.mds.observer.profile`          | DSP communication profile                     | `dataspace-protocol-http:2025-1`       |
| `edc.mds.observer.transfer.profile` | Transfer profile for the observer channel     | `HttpData-PULL`                        |

### Example: `config.properties`

```properties
edc.mds.observer.id=did:web:observer.example.com
edc.mds.observer.url=https://observer.example.com/api/dsp
edc.mds.observer.dataset.id=event-sink
edc.mds.observer.profile=dataspace-protocol-http:2025-1
edc.mds.observer.transfer.profile=HttpData-PULL
```

### Example: Environment Variables

```bash
EDC_MDS_OBSERVER_ID=did:web:observer.example.com
EDC_MDS_OBSERVER_URL=https://observer.example.com/api/dsp
EDC_MDS_OBSERVER_DATASET_ID=event-sink
EDC_MDS_OBSERVER_PROFILE=dataspace-protocol-http:2025-1
EDC_MDS_OBSERVER_TRANSFER_PROFILE=HttpData-PULL
```

---

## Dispatched Events

Events are dispatched as [CloudEvents 1.0](https://cloudevents.io/) JSON payloads via `POST` to the observer endpoint. Each event uses `Content-Type: application/json` and the configured authorization header.
Please refer to the [json-schema](https://mobility-data-space.github.io/mds-observer/schemas/v1/event-envelope.json) for details

---

## Startup Sequence

The following diagram shows what happens when the participant connector starts with the observer properties configured:

```
Participant                         Observer
    |                                   |
    |-- Fetch catalog (DSP) ----------->|
    |<-- Dataset offer with policy -----|
    |                                   |
    |-- Initiate contract negotiation -->|
    |<-- ContractNegotiationFinalized --|
    |                                   |
    |-- Initiate transfer process ------>|
    |<-- TransferProcessStarted --------|
    |   (data address stored in vault)   |
    |                                   |
    |  [events now dispatched via HTTP]  |
```

If a FINALIZED negotiation with the observer already exists in the database (e.g. after a connector restart), the negotiation phase is skipped and the transfer is re-initiated directly on top of the existing agreement.
