# EDC - EDP Extension

This documentation describes the EDP plugin, a EDC (Eclipse Dataspace Component) extension for interacting with Enhanced Dataset Profile Service (EDPS) and Data Space Search Engine (Daseen). The plugin facilitates creating, managing, and publishing Enhanced Dataset Profiles (EDPs) within a data space ecosystem.

## System Architecture Overview

![System Architecture](resources/docs/overview.png)

The above diagram illustrates the overall system architecture showing how the Data Holder connects with EDPS and Daseen services through EDC contracts and data flows.

## Introduction

The EDP Plugin extends the standard EDC connector with capabilities to create, analyze, and publish Enhanced Dataset Profiles (EDPs). This plugin provides a bridge between data assets in an EDC connector and services like EDPS (Enhanced Dataset Profile Service) and Daseen (Data Space Search Engine).

### Key Components

1. **Data Holder**: Organization that owns data assets and uses the EDC connector with the EDP Plugin
2. **EDPS (Enhanced Dataset Profile Service)**: Service that analyzes data and generates enhanced dataset profiles
3. **Daseen (Data Space Search Engine)**: Centralized search engine where EDPs are published and made discoverable
4. **EDP Plugin**: Custom plugin that integrates EDPS and Daseen functionalities into the EDC

## System Setup

The setup consists of the following components:

1. **Data Holder**:

   - Standard EDC connector with control plane and data plane
   - EDP Plugin installed and configured
   - Plugin REST API for managing EDP-related operations

2. **EDC Service Providers**:

   - EDPS service with standard EDC connector (control plane and data plane)
   - Daseen service with standard EDC connector (control plane and data plane)

3. **Service Contracts**:
   - HTTP pull contracts between Data Holder and service providers
   - Contracts serve as proxies to the actual service APIs

## Data Flow

There are two primary ways to use the system:

1. **Direct Service Access**:

   - Data Holder extracts endpoint data addresses (EDR) from the EDC transfer process
   - Uses these endpoints to access EDPS or Daseen service APIs directly
   - Suitable for providing local files to the services or integrate it with other systems

2. **EDP Plugin-Managed Flow**:
   - Use the EDP plugin to manage assets directly connected to the EDC
   - Data flows in the data plane layer at all the time
   - All result assets are managed inside the EDC connector
   - Provides seamless integration with existing EDC assets

## Workflow Overview

### Prerequisite

To use EDPS and Daseen, the data holder must establish separate contract agreements with each service through the EDC.

### EDPS Workflow

1. Data Holder creates an analysis job for a specific asset
2. Input data is uploaded to EDPS
3. EDPS processes the data and generates an enhanced dataset profile
4. Data Holder checks job status and retrieves results when complete
5. Data is stored in the data storage of the data holder and a EDP asset is created

### Daseen Workflow

1. Data Holder creates a resource entry in Daseen
2. EDP data is uploaded to Daseen
3. Daseen indexes the EDP for search and discovery
4. Data Holder can delete the resource when needed

## Service Actions

The following table describes the main actions available in the system:

| Action              | Description                                        | Plugin Call                                      | Service API Call                                    |
| ------------------- | -------------------------------------------------- | ------------------------------------------------ | --------------------------------------------------- |
| **EDPS Actions**    |
| Create analysis job | Initiates a new analysis job for dataset profiling | `POST /edp/edps/{assetId}/jobs`                  | `POST /v1/dataspace/analysisjob`                    |
| Upload input data   | Submits data to be analyzed by EDPS                | Handled internally by plugin when creating a job | `POST /v1/dataspace/analysisjob/{job_id}/data/file` |
| Get status          | Checks the status of an analysis job               | `GET /edp/edps/{assetId}/jobs/{jobId}/status`    | `GET /v1/dataspace/analysisjob/{job_id}/status`     |
| Get result data     | Retrieves the enhanced dataset profile             | `POST /edp/edps/{assetId}/jobs/{jobId}/result`   | `GET /v1/dataspace/analysisjob/{job_id}/result`     |
| **Daseen Actions**  |
| Create EDP resource | Creates a new resource entry in Daseen             | `POST /edp/daseen/{edpAssetId}`                  | `POST /connector/edp/`                              |
| Upload EDP data     | Uploads EDP data to Daseen                         | `PUT /edp/daseen/{edpAssetId}`                   | `PUT /connector/edp/{id}/`                          |
| Delete EDP resource | Removes an EDP from Daseen                         | `DELETE /edp/daseen/{edpAssetId}`                | `DELETE /connector/edp/{id}/`                       |

## Requirements

- Java 17 (17.0.8+7)

## Project Structure

The project consists of the following modules:

- `edc-edps-extension`: The actual EDC-EDP connector extension
- utils:
  - `http-file-server`: contains the http file server to provide the demo data.csv
  - `edps_mock-server`: the mock server for the EDPS and Daseen Api

Note: _To switch between the mock server and the real EDPS and Daseen Api, the `application.properties` in the `edc-edps-extension` module has to be adjusted.
It is sufficient to change the `edc.edps.url` and `edc.daseen.url` properties to the real endpoint._

## Documentation

The Api reference for the:

- Management Api can be found [here](https://github.com/eclipse-edc/Connector/blob/gh-pages/openapi/management-api/3.0.6/management-api.yaml).
- Extension Api can be found [here](resources/edc-edps-openapi.yml).

## EDP Workflow

### 1. Start the EDC server

Build the connector:

```bash
./gradlew connector:build
```

Note: The gradle-wrapper.jar needs to be in the `.gradle/wrapper/` directory.

Run the provider connector:

```bash
java -Dedc.fs.config=resources/configuration/provider-configuration.properties -jar connector/build/libs/connector.jar
```

Run the edps and daseen services connector:

```bash
java -Dedc.fs.config=resources/configuration/service-provider-configuration.properties -jar connector/build/libs/connector.jar
```

Alternatively, start the `io/nexyo/edp/extensions/Runner.java` in the IDE.

### 2. Start Services

Start the http server with the following command:

```bash
python ./util/http-file-server/server.py
```

The file server will be used to provide files referenced by the EDC assets.
Additionally, the file server acts as the callback address for the dataplane, logging the results of dataplane operations.

Start the mock server for the EDPS and Daseen Api:

```bash
python ./util/edps-mock-server/server.py
```

### Setup Service Provider side

#### 1. Create EDPS and Daseen Asset

```bash
curl -d @resources/requests/create-edps-asset.json \
  -H 'content-type: application/json' http://localhost:29193/management/v3/assets \
  -s | jq
```

```bash
curl -d @resources/requests/create-daseen-asset.json \
  -H 'content-type: application/json' http://localhost:29193/management/v3/assets \
  -s | jq
```

[Optional] Check if asset is created:

```bash
curl -X POST http://localhost:29193/management/v3/assets/request | jq
```

#### 2. Create Policy

```bash
curl -d @resources/requests/create-policy.json \
  -H 'content-type: application/json' http://localhost:29193/management/v3/policydefinitions \
  -s | jq
```

[Optional] Check if policy is created:

```bash
curl -X POST http://localhost:29193/management/v3/policydefinitions/request | jq
```

#### 3. Create Contract Definition

```bash
curl -d @resources/requests/create-contract-definition.json \
  -H 'content-type: application/json' http://localhost:29193/management/v3/contractdefinitions \
  -s | jq
```

[Optional] Check if contract definition is created:

```bash
curl -X POST http://localhost:29193/management/v3/contractdefinitions/request | jq
```

### Establish EDPS contract

#### 1. Fetch Catalog

```bash
curl -d @resources/requests/fetch-service-provider-catalog.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/catalog/request \
  -s | jq
```

#### 2. Negotiate contracts

Please replace the `{{contract-offer-id}}` placeholder in the `negotiate-edps-contract.json` and `negotiate-daseen-contract.json` file with the contract offer id you found in the catalog at the path `dcat:dataset.odrl:hasPolicy.@id`.

```bash
curl -d @resources/requests/negotiate-edps-contract.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/contractnegotiations \
  -s | jq
```

```bash
curl -d @resources/requests/negotiate-daseen-contract.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/contractnegotiations \
  -s | jq
```

[Optional] Check if contract negotiation is successfull:

```bash
curl -X POST http://localhost:29193/management/v3/contractnegotiations/request | jq
```

[Optional] Lookup contract agreements:

```bash
curl -X POST http://localhost:29193/management/v3/contractagreements/request | jq
```

#### 3. Create transfer process

Please replace the `{{contract-id}}` placeholder in the `start-transfer.json` file with the contract id you found in the lookup contract agreement request. Create a transfer process for edps and daseen.

```bash
curl -d @resources/requests/start-transfer.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/transferprocesses \
  -s | jq
```

[Optional] Check if transfer process is successfull:

```bash
curl -X POST http://localhost:19193/management/v3/transferprocesses/request | jq
```

#### 4. Get EDR for transfer process

Please replace the `<transfer-process-id>` placeholder in the request with the id you found in the transfer processes request.

```bash
curl -X GET http://localhost:19193/management/v3/edrs/<transfer-process-id>/dataaddress | jq
```

### Run EDP flow for asset

### 1. Create Asset

Use the management Api to create an asset:

```bash
curl -d @resources/requests/create-asset.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/assets \
  -s | jq
```

[Optional] Check if asset is created:

```bash
curl -X POST http://localhost:19193/management/v3/assets/request | jq
```

### 2. Create EDPS Job

replace the `{{contract-id}}` placeholder resources/requests/create-edps-job.json with the contract id from the previous steps.

```bash
curl -d @resources/requests/create-edps-job.json \
  -H 'content-type: application/json' http://localhost:19191/api/edp/edps/assetId1/jobs \
  -s | jq
```

Note the `jobId` in the response as it is needed for the next step.

[Optional] Get EDPS job by assetId:

```bash
curl http://localhost:19191/api/edp/edps/assetId1/jobs | jq
```

[Optional] Get EDPS job status:

```bash
curl  http://localhost:19191/api/edp/edps/assetId1/jobs/{jobId}/status  | jq
```

### 3. Get EDPS Result

Replace the jobId in the request with the jobId from the previous step.

```bash
curl -X POST http://localhost:19191/api/edp/edps/assetId1/jobs/{jobId}/result \
  -H 'content-type: application/json' \
  -d @resources/requests/fetch-edps-result.json
```

### 4. Create Result Asset

```bash
curl -d @resources/requests/create-result-asset.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/assets \
  -s | jq
```

### 5. Publish Result to Daseen

Please replace the `{{contract-id}}` placeholder in the `publish-to-daseen-asset.json` file with the contract offer id you found in the catalog at the path `dcat:dataset.odrl:hasPolicy.@id`.

Note: This only works if Daseen and EDPS are running on the same endpoint with the same authorization. If this is not the case, you have to set up a new contract agreement for Daseen (analog to setting up the EDPS service asset and contract agreement).

```bash
curl -d @resources/requests/publish-to-daseen-asset.json \
  -H 'content-type: application/json' http://localhost:19191/api/edp/daseen/resultAssetId1 \
  -s | jq
```

[optional] Update Daseen entry

```bash
curl -X PUT http://localhost:19191/api/edp/daseen/resultAssetId1 | jq
```

[optional] Delete Daseen entry

```bash
curl -X DELETE http://localhost:19191/api/edp/daseen/resultAssetId1 | jq
```

## Using Automation Scripts

The project includes automation scripts to simplify and test the setup and execution of the EDP workflow.

### Using the Setup Script

The `setup_contracts.sh` script automates the initial setup process including creating assets, policies, contract definitions, and establishing contracts with both EDPS and Daseen services:

```bash
# Run the setup script
./setup_contracts.sh
```

This script will:

1. Create EDPS and Daseen assets on the service provider side
2. Create policy and contract definitions
3. Fetch the service provider catalog
4. Negotiate contracts for both EDPS and Daseen services
5. Initiate transfer processes
6. Extract and display the contract IDs for both services

After running this script, you'll receive contract IDs for both EDPS and Daseen services that you'll need for the next step.

### Running the EDP Flow

After completing the setup, use the `run_edp_flow.sh` script to execute the complete EDP workflow. You need to provide the contract IDs obtained from the setup script:

```bash
# Run the workflow script with contract IDs
EDPS_CONTRACT_ID="contract-id-from-setup" DASEEN_CONTRACT_ID="daseen-contract-id-from-setup" ./run_edp_flow.sh
```

This script will:

1. Create a source asset for EDPS
2. Create an EDPS job for analysis
3. Fetch and store the EDPS job result
4. Create a result asset
5. Publish the result to Daseen

These scripts automate the manual steps described in the following sections and provide a streamlined way to test the EDP workflow.

## Limitations

- Only transfers of type `HttpData` are supported at the moment
- EDPS and Daseen Api are mocked as the content type of the requests are determined by the Dataplane
- No error handling for Dataplane operations

## ToDos

- Add tests
- Support for other transfer types
- Use job infos from request rather than currently used test data
- Add routes for dataplane results (POST and GET)
