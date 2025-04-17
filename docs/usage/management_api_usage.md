# Management API Overview

## Introduction

This walkthrough attempts to be a reference for systems integrators attempting to expose APIs safely to the internet. It is not a comprehensive guide to the EDC, but rather a starting point for understanding how to use the Management API to create and manage assets, policies, and contract definitions.

The EDC implements the [DataSpace Protocol (DSP)](https://github.com/International-Data-Spaces-Association/ids-specification/tree/main/protocol/v0.9.1), as specified by the IDSA. As the DSP uses JSON-LD for all payloads, the EDC Management API reflects this as well, even though it is not a part of the DSP.

## Endpoints

The `MANAGEMENT_URL` specifies the URL of the management API and the prefixes `v3`, `v3.1alpha` and `v4alpha` respect the fact that the endpoints are currently versioned independently of each other.

| Resource | Endpoint | 
|----------|----------|
| Asset | `<MANAGEMENT_URL>/v3/assets` | 
| Policy Definition | `<MANAGEMENT_URL>/v3/policydefinitions` |
| Contract Definition | `<MANAGEMENT_URL>/v3/contractdefinitions` |
| Catalog | `<MANAGEMENT_URL>/v3/catalog` |
| Contract Negotiation | `<MANAGEMENT_URL>/v3/contractnegotiations` |
| Contract Agreement | `<MANAGEMENT_URL>/v3/contractagreements` |
| Transfer Process | `<MANAGEMENT_URL>/v3/transferprocesses` | 
| EDR | `<MANAGEMENT_URL>/v3/edrs` | 
| Agreements Retirement | `<MANAGEMENT_URL>/v3.1alpha/retireagreements` |
| EDP (Experimental) | `<MANAGEMENT_URL>/edp` |


## Brief JSON-LD Introduction

JSON-LD (JSON for Linked Data) is an extension of JSON that introduces a set of principles and mechanisms to serialize RDF-graphs and thus open new opportunities for interoperability. As such, there is a clear separation into identifiable resources (IRIs) and Literals holding primitive data like strings or integers.For developers used to working with JSON, JSON-LD can act in unexpected ways, for example a list with one entry will always unwrap to an object which may cause schema validation to fail on the client side. Please also refer to the [JSON-LD spec](https://json-ld.org/spec/latest/json-ld/) and try it out on the [JSON-LD Playground](https://json-ld.org/playground/).

## Asset

An asset represents a resource that can be shared within the Dataspace, such as a file or a service endpoint.

### Create an Asset

```http
POST /v3/assets
Content-Type: application/json

{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "your-asset-id",
  "properties": {

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
POST /v3/policydefinitions
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
POST /v3/contractdefinitions
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
POST /v3/catalog/request
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "CatalogRequest",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "counterPartyId": "MY_MDS_ID",
  "protocol": "dataspace-protocol-http",
  "additionalScopes": [
  ]
}
```

## Contract Negotiation

Contract negotiation is the process of establishing an agreement between a provider and a consumer for asset usage.

### Initiate Contract Negotiation

```http
POST /v3/contractnegotiations
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "https://w3id.org/edc/v0.0.1/ns/ContractRequest",
  "counterPartyAddress": "https://provider.dataspaces.think-it.io/api/dsp",
  "protocol": "dataspace-protocol-http",
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
  },
  "callbackAddresses": [
    {
      "transactional": false,
      "uri": "http://webhook",
      "events": [
        "contract.negotiation"
      ]
    }
  ]
}
```

## Transfer Process

The transfer process manages the actual data transfer between provider and consumer.

### Initiate Transfer Process

```http
POST /v3/transferprocesses
Content-Type: application/json

{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "protocol": "dataspace-protocol-http",
  "counterPartyAddress": "{{https://provider.dataspaces.think-it.io/api/dsp}}",
  "contractId": "{{contract-id}}",
  "transferType": "HttpData-PULL"
}
```

## Conclusion

This walkthrough provides a basic understanding of how to use the Management API to create and manage assets, policies, and contract definitions with the MDS Connector based on EDC. 

For more detailed information, please refer to the full API documentation and the EDC project documentation.
