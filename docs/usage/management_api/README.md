# Management API Overview

## Introduction

This walkthrough is a reference for systems integrators using the Management API V4. It covers how to create and manage assets, policies, and contract definitions to share data within the Dataspace.

The EDC implements the [Dataspace Protocol (DSP)](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1-err1/), as specified by the IDSA. The Management API V4 introduces JSON Schema validation for all payloads, making request structures more predictable while retaining JSON-LD semantics under the hood.

> **Migrating from V3?** The archived V3 documentation is available in the [v3/](./v3/) subfolder. V3 was deprecated at release `v2.6.0` alongside the promotion of V4 to stable.

## What Changed in V4

The main change in V4 is a simplified `@context`. Instead of a verbose namespace object, all requests now use a single common context URL:

```json
"@context": "https://w3id.org/mobility-dataspace/connector/management/v1"
```

This replaces the V3 pattern of:

```json
"@context": {
  "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
  "dcat": "http://www.w3.org/ns/dcat#",
  ...
}
```

Additionally:
- All payloads are now validated against JSON Schema definitions
- The `@type` field is now mandatory in all request bodies
- The EDR cache endpoint has been removed (as the DataAddress is now managed by the Data-Plane, [upgrade to 1.0 document](../../development/upgrade_to_1.0.md) for details) 
- The `policydefinitions/{id}/validate` endpoint was added
- The `approve` and `reject` endpoints for contract negotiations are available at `/v4/contractnegotiations/{id}/approve` and `/v4/contractnegotiations/{id}/reject`

## Endpoints

The `MANAGEMENT_URL` specifies the base URL of the management API.

| Resource | Endpoint | Documentation |
|----------|----------|---------------|
| Asset | `<MANAGEMENT_URL>/v4/assets` | [Assets](./assets.md) |
| Policy Definition | `<MANAGEMENT_URL>/v4/policydefinitions` | [Policy Definition](./policy_definition.md) |
| Contract Definition | `<MANAGEMENT_URL>/v4/contractdefinitions` | [Contract Definition](./contract_definition.md) |
| Catalog | `<MANAGEMENT_URL>/v4/catalog` | [Catalog](./catalog.md) |
| Contract Negotiation | `<MANAGEMENT_URL>/v4/contractnegotiations` | [Contract Negotiation](./contract_negotiation.md) |
| Contract Agreement | `<MANAGEMENT_URL>/v4/contractagreements` | [Contract Agreement](./contract_agreement.md) |
| Contract Agreement Retirement | `<MANAGEMENT_URL>/v4/contractagreements/retirements` | [Contract Agreement Retirement](./contract_retirement.md) |
| Transfer Process | `<MANAGEMENT_URL>/v4/transferprocesses` | [Transfer Process](./transfer_process.md) |

## Brief JSON-LD Introduction
JSON-LD (JSON for Linked Data) is an extension of JSON that introduces a set of principles and mechanisms to serialize RDF-graphs and thus open new opportunities for interoperability. As such, there is a clear separation into identifiable resources (IRIs) and Literals holding primitive data like strings or integers. For developers used to working with JSON, JSON-LD can act in unexpected ways — for example, a list with one entry will always unwrap to an object which may cause schema validation to fail on the client side. Please also refer to the [JSON-LD spec](https://json-ld.org/spec/latest/json-ld/) and try it out on the [JSON-LD Playground](https://json-ld.org/playground/).
