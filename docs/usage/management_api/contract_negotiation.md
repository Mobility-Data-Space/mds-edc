# Contract Negotiation

Contract negotiation is the process of establishing an agreement between a provider and a consumer for asset usage.

Before initiating a negotiation, you must first request the catalog from the provider to obtain the offer ID and policy details for the desired asset. The catalog response contains the available datasets with their associated offers and policies.

## Initiate Contract Negotiation

```http
POST /v4/contractnegotiations
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "ContractRequest",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp/2025-1",
  "protocol": "dataspace-protocol-http:2025-1",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Offer",
    "@id": "offer-id",
    "assigner": "MDSLXXX.XXXXX",
    "target": "asset-id",
    "permission": [{
      "action": "use"
    }]
  }
}
```

The `policy` must match the offer returned by the provider's catalog (`@id`, `assigner`, `target`, rules and constraints). When copying it from the catalog response, adapt the compacted form: `@type` is `Offer`, rules are unprefixed (`permission`, `prohibition`, `obligation`) and given as arrays, `action` is a plain string, and empty rule arrays are omitted.

## Get Negotiation State

Poll this endpoint to track negotiation progress.

```http
GET /v4/contractnegotiations/{id}/state
```

## Terminate Negotiation

```http
POST /v4/contractnegotiations/{id}/terminate
Content-Type: application/json

{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "TerminateNegotiation",
  "reason": "Termination reason"
}
```
