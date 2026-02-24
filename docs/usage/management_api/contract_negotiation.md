# Contract Negotiation

Contract negotiation is the process of establishing an agreement between a provider and a consumer for asset usage.

Before initiating a negotiation, you must first request the catalog from the provider to obtain the offer ID and policy details for the desired asset. The catalog response contains the available datasets with their associated offers and policies.

## Initiate Contract Negotiation

```http
POST /v3/contractnegotiations
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "https://w3id.org/edc/v0.0.1/ns/ContractRequest",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp/2025-1",
  "protocol": "dataspace-protocol-http:2025-1",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "odrl:Offer",
    "@id": "offer-id",
    "assigner": "MDSLXXX.XXXXX",
    "odrl:permission": {
      "odrl:action": {
        "@id": "use"
      }
    },
    "odrl:prohibition": [],
    "odrl:obligation": [],
    "target": "asset-id"
  }
}
```
