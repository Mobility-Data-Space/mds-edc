# Management API V3 (Archived)

> **This documentation is archived.** V3 was deprecated at release `v0.17.0` alongside the promotion of V4 to stable, and is scheduled for removal approximately 3 months after that release.
>
> Please use the [V4 documentation](../README.md) for new integrations.

## Introduction

This walkthrough is a reference for systems integrators using the Management API V3. It covers how to create and manage assets, policies, and contract definitions to share data within the Dataspace.

The EDC implements the [Dataspace Protocol (DSP)](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1-err1/), as specified by the IDSA. As the DSP uses JSON-LD for all payloads, the EDC Management API reflects this as well.

## Endpoints

The `MANAGEMENT_URL` specifies the URL of the management API and the prefixes `v3`, `v3.1alpha` and `v4alpha` respect the fact that the endpoints are currently versioned independently of each other.

| Resource             | Endpoint                                             | Documentation                                     |
|----------------------|------------------------------------------------------|---------------------------------------------------|
| Asset                | `<MANAGEMENT_URL>/v3/assets`                         | [Assets](./assets.md)                             |
| Policy Definition    | `<MANAGEMENT_URL>/v3/policydefinitions`              | [Policy Definition](./policy_definition.md)       |
| Contract Definition  | `<MANAGEMENT_URL>/v3/contractdefinitions`            | [Contract Definition](./contract_definition.md)   |
| Catalog              | `<MANAGEMENT_URL>/v3/catalog`                        | [Catalog](./catalog.md)                           |
| Contract Negotiation | `<MANAGEMENT_URL>/v3/contractnegotiations`           | [Contract Negotiation](./contract_negotiation.md) |
| Contract Agreement   | `<MANAGEMENT_URL>/v3/contractagreements`             | [Contract Agreement](./contract_agreement.md)     |
| Transfer Process     | `<MANAGEMENT_URL>/v3/transferprocesses`              | [Transfer Process](./transfer_process.md)         |
| EDR Cache            | `<MANAGEMENT_URL>/v3/edrs`                           | [EDR Cache](./edr_cache.md)                       |
| Contract Retirement  | `<MANAGEMENT_URL>/v3/contractagreements/retirements` | [Contract Retirement](./contract_retirement.md)   |

## Brief JSON-LD Introduction

JSON-LD (JSON for Linked Data) is an extension of JSON that introduces a set of principles and mechanisms to serialize RDF-graphs and thus open new opportunities for interoperability. As such, there is a clear separation into identifiable resources (IRIs) and Literals holding primitive data like strings or integers. For developers used to working with JSON, JSON-LD can act in unexpected ways, for example a list with one entry will always unwrap to an object which may cause schema validation to fail on the client side. Please also refer to the [JSON-LD spec](https://json-ld.org/spec/latest/json-ld/) and try it out on the [JSON-LD Playground](https://json-ld.org/playground/).
