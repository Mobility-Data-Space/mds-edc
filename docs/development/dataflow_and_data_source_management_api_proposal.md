# DataFlow & Data Source Management API Proposal

## Overview

This document proposes two additions to the connector's Management API:

1. **DataFlow interaction** — expose the data plane's `DataFlow` entity so operators can query transfers and inspect
   the `DataAddress` a flow resolved to. Two endpoints: a `POST .../dataflows/request` query and a
   `GET .../dataflows/{id}/dataaddress` lookup. **These endpoints replace the V3 EDR cache endpoints, which EDC does
   not carry forward to Management API V4** (see [below](#the-v3-edr-endpoints-are-going-away)).
2. **Data Sources** — introduce a reusable, named set of properties (a *Data Source*) that serves as a **template** for
   asset creation: its properties are copied into the new asset's `dataplaneMetadata` so the connection details for a
   backend are entered once instead of re-typed on every asset. A Data Source is a convenience; it keeps **no link** to
   the assets created from it. This feature ships with its own persistence layer and a CRUD/query Management API.

Both features are designed for the [EDC 1.0 direction](./upgrade_to_1.0.md): they target **Management API V4 only**
(V3 is deprecated and removed in 1.0), and they are shaped around **Data Plane Signaling**, under which the
`DataAddress` is owned by the data plane rather than the control plane.

## Requirements

- Preserve, on Management API V4, the query and data-address-retrieval capability currently provided by the V3-only EDR
  cache endpoints, which are removed in EDC 1.0.
- Query the data flows known to the (embedded) data plane, filtered by state, asset, agreement, etc.
- Retrieve the `DataAddress` a given data flow is reading from.
- Define a data source once and reuse its properties when creating assets, without re-typing the metadata each time.
- Keep data sources and assets independent: editing or deleting a data source must not affect assets already created
  from it, nor transfers already in flight.
- Persist data sources in both in-memory and PostgreSQL deployments.
- Expose everything on Management API V4, with correct JSON-LD contexts and mandatory `@type`.

## Current Scenario

### DataFlow is opaque

A [`DataFlow`](https://github.com/eclipse-edc/Connector) (`org.eclipse.edc.connector.dataplane.spi.DataFlow`, a
`StatefulEntity`) is the data plane's record of a transfer: it binds a transfer process to a source and destination
`DataAddress` and tracks its state. Following the [EDC DataPlane removal](./upgrade_to_1.0.md#3-edc-dataplane-removal),
the data plane modules are **vendored in-tree** under `extensions/data-plane/**` (the `DataFlow` entity, the
`DataPlaneStore`, and the SQL store are all source in this repo). There is, however, no Management API surface to list
flows or to see where a flow is routing data — today that requires going to the database or the logs.

Under [Data Plane Signaling](./upgrade_to_1.0.md#2-data-plane-signaling) this gap matters more: the `DataAddress` is no
longer owned by the control plane, so the data flow becomes the authoritative place to answer "where is this transfer
reading from?".

### The V3 EDR endpoints are going away

The capability to query transfers and fetch a transfer's `DataAddress` exists today only through the **EDR cache API**
(`EndpointDataReferenceStore`, backed by `org.eclipse.edc:edr-cache-api`). That API is **V3-only** — there is no V4
controller in EDC `0.18.1` — so with [Management API V4](./upgrade_to_1.0.md#1-management-api-v4) and the removal of V3
in 1.0, these endpoints disappear. The new DataFlow endpoints are their V4 replacement; the mapping is almost
one-to-one (a `DataFlow`'s id is the transfer process id):

| Removed V3 EDR endpoint | New V4 DataFlow endpoint |
| --- | --- |
| `POST /v3/edrs/request` | `POST /v4/dataflows/request` |
| `GET /v3/edrs/{transferProcessId}/dataaddress` | `GET /v4/dataflows/{id}/dataaddress` |
| `DELETE /v3/edrs/{transferProcessId}` | not replaced (data flows are managed by the data plane lifecycle, not deleted via the API) |

### Data sources are duplicated across assets

Today the coordinates for a backend (endpoint URL, key alias, bucket, region, topic, …) are carried per-asset. With the
move away from `Asset.dataAddress` toward [`dataplaneMetadata`](./upgrade_to_1.0.md#2-data-plane-signaling) — from which
the data plane reconstructs a `DataAddress` at flow start — those properties still live on each asset individually. When
many assets share one backend, the metadata is duplicated and must be edited asset-by-asset when the backend changes
(a rotated credential alias, a moved endpoint). The existing `AssetDataAddressPatch`
(`extensions/patches/.../AssetDataAddressPatch.java`) already rewrites an incoming `edc:dataAddress` into
`edc:dataplaneMetadata` — a Data Source is the natural next step: a *named template* for that `dataplaneMetadata`, whose
properties are copied into the asset at creation time so the operator does not re-type them for every asset.

## Implementation Proposal

The project already has a complete end-to-end template for a custom Management API + store: the
`retirement-evaluation-{spi,core,store-sql}` and `-api` modules under `extensions/agreements/`. Both features below
follow that structure (versioned JAX-RS controller, JSON-LD transformers, `QuerySpec` querying, `@ExtensionPoint`
store, in-memory default overridden by an opt-in SQL implementation, centralized Flyway migration).

### Part 1 — DataFlow endpoints

Two **read-only** endpoints, registered on the Management V4 context exactly as `AgreementsRetirementApiExtension`
does (`webService.registerResource(...)` + a `JerseyJsonLdInterceptor` bound to `MANAGEMENT_SCOPE_V4`).

| Method | Path | Body | Returns |
| --- | --- | --- | --- |
| `POST` | `/v4/dataflows/request` | `QuerySpec` (JSON-LD) | `DataFlow[]` (JSON-LD array) |
| `GET`  | `/v4/dataflows/{id}/dataaddress` | — | `DataAddress` (JSON-LD) / `404` |

#### Fields exposed

`DataFlow` already carries everything useful: `id`, `state` (`stateAsString()` — `STARTED`, `COMPLETED`, `SUSPENDED`,
`TERMINATED`, `FAILED`, …), `assetId`, `agreementId`, `participantId`, `transferType`, and the `source` / `destination`
`DataAddress`. The `GET .../dataaddress` endpoint returns the flow's **source** `DataAddress` (`getSource()`);
`getActualSource()` and `getDestination()` are available if a different address is wanted.

#### The one real change: querying the store

`DataPlaneStore` extends `StateEntityStore`, which exposes only `findById`, `nextNotLeased` and the lease operations —
**there is no `findAll(QuerySpec)`**. Therefore:

- `GET .../dataaddress` is trivial: `dataPlaneStore.findById(id)` → `getSource()`.
- `POST .../request` needs a query capability the store does not have today. Because the data plane is vendored, add a
  `Stream<DataFlow> findAll(QuerySpec)` to the local `DataPlaneStore` and implement it in:
    - `InMemoryDataPlaneStore` — via `ReflectionBasedQueryResolver`, as the in-memory stores already do.
    - `SqlDataPlaneStore` — via `SqlQueryStatement` + the existing `DataFlowMapping` / `PostgresDataFlowStatements` in
      `extensions/data-plane/sql/data-plane-store-sql`.

This is the bulk of the DataFlow work.

#### Transformers and service

There is no existing `DataFlow`→JSON-LD transformer, so add a `JsonObjectFromDataFlowTransformer`; the `DataAddress`
response reuses EDC's upstream `DataAddress` transformer. A thin injected `DataFlowService` wraps the store's `findById`
+ the new `findAll`, mirroring how the retirement controller delegates to `AgreementsRetirementService`.

> **Secrets:** a `source` / `destination` `DataAddress` may reference a vault key alias. The response must surface the
> alias, never a resolved secret, consistent with `VaultDataAddressStorePatch`.

### Part 2 — Data Source feature

A **Data Source** is a named, reusable bag of properties describing one backend. It is a creation-time template: its
properties are copied into an asset's `dataplaneMetadata` when the asset is created, and nothing links the two
afterwards.

#### Entity

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `String` | Stable identifier, client- or server-generated |
| `name` | `String` | Human-readable label, required |
| `type` | `String` | Backend kind (`HttpData`, `AmazonS3`, `AzureStorage`, `Kafka`, …) |
| `properties` | `Map<String, Object>` | The reusable coordinates (endpoint, bucket, region, key alias, topic, …); stored in the vault, opaque, **not queryable** |
| `createdAt` / `updatedAt` | `long` | Epoch millis, set by the store |

It lives in a new `datasource-spi` module as `eu.dataspace.connector.datasource.spi.types.DataSource`, with a JSON-LD
type term (`DataSource`) and property namespace registered in `management-jsonld-context` alongside the existing MDS
context (`https://w3id.org/mobility-dataspace/connector/management/v1`), used together with the EDC management context
(`https://w3id.org/edc/connector/management/v2`).

#### No link to assets — client-side copy

There is deliberately **no persistent relationship** between a Data Source and the assets created from it, and the
connector plays no part in the copy. The Data Source API is a pure CRUD/lookup store: a client fetches a source,
assembles the create-asset payload from its `properties`, and submits a normal asset. From that point the asset is
self-contained, and the connector is unaware a data source was ever involved.

Keeping it client-side (rather than expanding a source id server-side at asset creation) is deliberate for two reasons:

- The source `DataAddress` may already be **allocated in the data plane** for an in-flight or completed transfer;
  mutating it retroactively would be meaningless or disruptive.
- Assets that are already being consumed must not change underneath their consumers.

Consequently, editing or deleting a data source has **no effect** on existing assets — it only changes what a client
copies into the next asset. No referential integrity, join table, reverse lookup, or server-side merge is needed.

#### Management endpoints

| Method   | Path                      | Body                   | Returns                        |
|----------|---------------------------|------------------------|--------------------------------|
| `POST`   | `/v4/datasources`         | `DataSource` (JSON-LD) | `IdResponse` (`201`)           |
| `PUT`    | `/v4/datasources`         | `DataSource` (JSON-LD) | `204` / `404`                  |
| `POST`   | `/v4/datasources/request` | `QuerySpec` (JSON-LD)  | `DataSource[]` (JSON-LD array) |
| `GET`    | `/v4/datasources/{id}`    | —                      | `DataSource` (JSON-LD) / `404` |
| `DELETE` | `/v4/datasources/{id}`    | —                      | `204` / `404`                  |

Each shape gets a pair of transformers (`JsonObjectToDataSourceTransformer`, `JsonObjectFromDataSourceTransformer`)
registered on the management transformer registry, and a `JsonObjectValidator` for the create/update bodies — the same
wiring as `AgreementsRetirementApiExtension`. All request bodies must carry `@type` (mandatory in V4).

`POST /v4/datasources/request` filters only over `name` and `type`; the `properties` are opaque and never part of a
query. The endpoint exists so an operator can find a source to reuse by its label or backend kind.

#### Persistence layer

Built as the four-module quartet used by `retirement-evaluation`: a `datasource-spi` (the `DataSource` type and the
`@ExtensionPoint DataSourceStore` interface with `create` / `update` / `findById` / `query` / `deleteById`), a
`datasource-core` (service, in-memory default store, API controllers and transformers), a `datasource-store-sql`
(PostgreSQL store following `SqlAgreementsRetirementStore`), and a Flyway migration in
`database-schema-migration-connector`. The in-memory store is the default `@Provider`, so SQL is opt-in per launcher.

The `edc_data_source` table holds only the queryable attributes — `id`, `name`, `type` — and acts as the lookup index
for `POST /v4/datasources/request`. The `properties` are sensitive backend configuration, **stored in the vault** keyed
by data source id and hydrated on read, mirroring `VaultDataAddressStorePatch`. The migration adds `edc_data_source`
after the current head (`V1_11_0__Asset_DataAddress_DataplaneMetadata.sql`), as `V1_12_0`.

## Implementation Plan

The two features are independent and can ship separately; the DataFlow endpoints are the smaller, lower-risk first
increment (they only extend the vendored `DataPlaneStore` with `findAll(QuerySpec)` and add a controller). The Data
Source feature follows with the SPI, in-memory store, API, SQL store, and the `V1_11_0` migration, wired into the
`connector-inmemory` and `connector-vault-postgresql` launchers.

Testing reuses the `*StoreTestBase` pattern against both store implementations (SQL on Testcontainers) plus transformer
and controller tests, as in `retirement-evaluation`. The change is fully additive — no impact on existing transfer,
catalog, or asset behaviour — and the migration runs automatically on startup.

## Design Decisions

These were open during drafting and are now settled:

- **The copy is client-side.** The Data Source API is a pure CRUD/lookup store; the client reads a source and composes
  the create-asset payload itself. The connector does not expand a source id at asset creation, so there is no
  server-side merge and no overlap between a source and inline metadata to resolve.
- **DataFlow API ownership — no remote plane.** The DataFlow endpoints are owned by the data plane and ship with the
  vendored data-plane modules. There is no data-plane-selector (remote plane) topology to support: the `DataPlaneStore`
  is in-process and stays that way, so `findAll(QuerySpec)` runs against the local store.
- **Data sources live in the vault, queried only by name and type.** A data source is sensitive backend configuration,
  so its `properties` are stored in the vault, keyed by id — never in the connector database. There is no need to query
  over `properties`: data sources are looked up only by `name` and `type`, which are kept in the `edc_data_source` table
  as a small queryable index (mirroring `VaultDataAddressStorePatch`). `DataFlow` source/destination addresses likewise
  reference vault key aliases, never raw credentials.

## References

- [Upgrade to EDC 1.0](./upgrade_to_1.0.md) — Management API V4, Data Plane Signaling, EDC DataPlane removal.
- [Management API upcoming versions](https://eclipse-edc.github.io/blog/2026/02/19/management-api-upcoming-versions/)
- [Data Plane Signaling specification](https://eclipse-dataplane-signaling.github.io/dataplane-signaling)
- [Data plane migration guide](https://eclipse-edc.github.io/blog/2026/05/19/data-plane-migration/)
- Reference implementation in-repo: `extensions/agreements/retirement-evaluation-*`.
