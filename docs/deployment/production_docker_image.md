# Production Deployment Guide using the MDS EDC Docker Image

## About this Guide

This is a productive deployment guide for self-hosting a functional MDS Connector.

## Prerequisites

### Technical Skills

- Ability to deploy, run and expose containered applications to the internet.
- Ability to configure ingress routes or a reverse proxy of your choice to merge multiple services under a single domain.
- Know-how on how to secure an otherwise unprotected application with an auth proxy or other solutions fitting your situation.

### Dataspace

- Must have a running DAPS that follows the subset of OAuth2 as described in the DSP Specification.
- You must have a valid Connector Certificate in the form of a .p12 and a password.
- You must have a valid Participant ID / Connector ID, which is configured in the claim "referringConnector" in the DAPS.
- You must have a Postgres database available and a Hashicorp instance. If not, feel encouraged to use the provided docker images.

## Deployment Units

To deploy an EDC multiple deployment units must be deployed and configured.

| Deployment Unit                                                   | Version / Details                                                 |
|-------------------------------------------------------------------|-------------------------------------------------------------------|
| An Auth Proxy / Auth solution of your choice.                     | (deployment specific, required to secure UI and management API)   |
| Reverse Proxy that merges multiple services and removes the ports | (deployment specific)                                             |
| Postgresql                                                        | 16 or compatible version, one for the EDC-data one for the LH-data|
| Hashicorp Vault                                                   | 1.8.4 |
| Connector                                                         | MDS-EDC |

## Configuration

### What ports does the MDS EDC Connector use?

Default port mapping (can be configured):

| Port | Component | Purpose | Public Exposure |
|------|-----------|---------|-----------------|
| 8181 | Default API | General/Health endpoints | Optional |
| 8182 | Management API | Asset/Policy/Contract management | Optional |
| 8183 | Protocol API (DSP) | Connector-to-connector communication | Yes (required) |
| 8184 | Version API | Versions endpoint | Optional |
| 8185 | Public API | Public data plane endpoint | Yes (required) |
| 8186 | Control API | Control plane operations | No (internal only) |

In production, these are typically merged behind a reverse proxy under a single domain with different paths.

### Reverse Proxy Configuration

To make the deployment work, the connector needs to be exposed to the internet. Connector-to-Connector
communication is asynchronous and done with authentication via the DAPS. Thus, if the target connector cannot reach
your connector under its self-declared URLs, contract negotiation and transfer processes will fail.

The MDS connector opens up multiple ports with different functionalities. They are expected to be merged by a reverse
proxy (at least the protocol endpoint needs to be).

- The MDS EDC Connector is meant to be deployed with a reverse proxy merging the following ports:
  - The MDS connector's `8182` port. Henceforth, called the Management API.
  - The MDS connector's `8183` port. Henceforth, called the Protocol API.
  - The MDS connector's `8185` port. Henceforth, called the Public API.
- The mapping should look like this:
  - `https://[EDC_HOSTNAME]/api/dsp` -> `edc:8183/api/dsp`
  - `https://[EDC_HOSTNAME]/public` -> `edc:8185/public`
  - `https://[EDC_HOSTNAME]/api/management` -> **Auth Proxy** -> `edc:8182/api/management`
- Regarding TLS/HTTPS:
  - All endpoints need to be secured by TLS/HTTPS. A productive connector won't work without it.
  - All endpoint should have HTTP to HTTPS redirects.
- Regarding Authentication:
  - The Management API need to be secured by an auth proxy. Otherwise, access to either would mean full control of the application.
  - The connector's `8183` and `8185` ports need to be unsecured. Authentication between connectors is done via the Data Space Authority / DAPS and the configured certificates.
- Exposing to the internet:
  - The Protocol API and the public API must be reachable via the internet.
  - Exposing the Management Endpoint to the internet requires an intermediate auth proxy, we recommend restricting the access to the Management Endpoint to your internal network.
- Security:
  - Limit the header size in the proxy so that only a certain number of API Keys can be tested with one API-request (e.g. limit to 8kb).
  - Limit the access rate to the API endpoints and monitor access for attacks like brute force attacks.

### Vault Configuration

Add a key pair for securing the data plane and the DAPS certificate to your vault with the same names used in the connector configuration.
See our helper script `init_vault.sh` for guidance.

### EDC Connector Configuration

[Example of how to configure MDS EDC connector](mds_connector_default_configuration.md).
