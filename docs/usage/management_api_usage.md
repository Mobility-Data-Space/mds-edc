# Management API Overview

## Introduction

This walkthrough attempts to be a reference for systems integrators attempting to expose APIs safely to the internet. It is not a comprehensive guide to the EDC, but rather a starting point for understanding how to use the Management API to create and manage assets, policies, and contract definitions.

The EDC implements the [DataSpace Protocol (DSP)](https://github.com/International-Data-Spaces-Association/ids-specification/tree/main/protocol/v0.9.1), as specified by the IDSA. As the DSP uses JSON-LD for all payloads, the EDC Management API reflects this as well, even though it is not a part of the DSP.

## Endpoints

The `MANAGEMENT_URL` specifies the URL of the management API and the prefixes `v2` and `v3` respect the fact that the endpoints are currently versioned independently of each other.

| Resource | Endpoint | Involved Actors |
|----------|----------|-----------------|
| Asset | `<MANAGEMENT_URL>/v3/assets` | Provider Admin & Provider EDC |
| Policy Definition | `<MANAGEMENT_URL>/v3/policydefinitions` | Provider Admin & Provider EDC |
| Contract Definition | `<MANAGEMENT_URL>/v3/contractdefinitions` | Provider Admin & Provider EDC |
| Catalog | `<MANAGEMENT_URL>/v3/catalog` | Consumer App, Consumer EDC & Provider EDC |
| Contract Negotiation | `<MANAGEMENT_URL>/v3/contractnegotiations` | Consumer App, Consumer EDC & Provider EDC |
| Contract Agreement | `<MANAGEMENT_URL>/v3/contractagreements` | Provider Admin & Provider EDC |
| Transfer Process | `<MANAGEMENT_URL>/v3/transferprocesses` | Consumer App, Consumer EDC & Provider EDC |
| EDR | `<MANAGEMENT_URL>/v3/edrs` | Consumer App, Consumer EDC & Provider EDC |
| Data Plane | `<DATAPLANE_URL>` | Consumer App & Provider EDC |


## Brief JSON-LD Introduction

JSON-LD (JSON for Linked Data) is an extension of JSON that introduces a set of principles and mechanisms to serialize RDF-graphs and thus open new opportunities for interoperability. As such, there is a clear separation into identifiable resources (IRIs) and Literals holding primitive data like strings or integers.For developers used to working with JSON, JSON-LD can act in unexpected ways, for example a list with one entry will always unwrap to an object which may cause schema validation to fail on the client side. Please also refer to the [JSON-LD spec](https://json-ld.org/spec/latest/json-ld/) and try it out on the [JSON-LD Playground](https://json-ld.org/playground/).

## Asset

An asset represents a resource that can be shared within the Dataspace, such as a file or a service endpoint.

### Create an Asset

```http
POST /assets
Content-Type: application/json

{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "your-asset-id",
  "properties": {
    "name": "product description",
    "contenttype": "application/json"
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://example.com"
  }
}
```

## Policy Definition

A policy definition specifies the rules and conditions for accessing an asset.

### Create a Policy Definition

```http
POST /policydefinitions
Content-Type: application/json

{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "aPolicy",
  "policy": {
    "@type": "set",
    "odrl:permission": [],
    "odrl:prohibition": [],
    "odrl:obligation": []
  }
}
```

## Contract Definition

A contract definition links assets with usage policies.

### Create a Contract Definition

```http
POST /contractdefinitions
Content-Type: application/json

{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "definition-id",
  "accessPolicyId": "aPolicy",
  "contractPolicyId": "aPolicy",
  "assetsSelector": []
}
```

## Catalog

The catalog endpoint allows consumers to discover available assets.

### Request Catalog

```http
POST /catalog/request
Content-Type: application/json

{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "providerUrl": "http://localhost:8282/api/v1/dsp"
}
```

## Contract Negotiation

Contract negotiation is the process of establishing an agreement between a provider and a consumer for asset usage.

### Initiate Contract Negotiation

```http
POST /contractnegotiations
Content-Type: application/json

{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "NegotiationInitiateRequestDto",
  "connectorId": "provider",
  "connectorAddress": "http://localhost:8282/api/v1/dsp",
  "protocol": "dataspace-protocol-http",
  "offer": {
    "offerId": "a-contract-offer-id",
    "assetId": "asset-id",
    "policy": {
      "@id": "offer-id:policy",
      "@type": "odrl:Set",
      "odrl:permission": [],
      "odrl:prohibition": [],
      "odrl:obligation": [],
      "odrl:target": {
        "@id": "asset-id"
      }
    }
  }
}
```

## Transfer Process

The transfer process manages the actual data transfer between provider and consumer.

### Initiate Transfer Process

```http
POST /transferprocesses
Content-Type: application/json

{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequestDto",
  "connectorId": "provider",
  "connectorAddress": "http://localhost:8282/api/v1/dsp",
  "contractId": "contract-id",
  "assetId": "asset-id",
  "protocol": "dataspace-protocol-http",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "HttpProxy"
  },
  "managedResources": false,
  "transferType": {
    "@id": "HttpProxy"
  }
}
```

## Conclusion

This walkthrough provides a basic understanding of how to use the Management API to create and manage assets, policies, and contract definitions in the Eclipse Dataspace Connector. For more detailed information, please refer to the full API documentation and the EDC project documentation.
