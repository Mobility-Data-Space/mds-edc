# HTTP Data

## Provider: Data Source Configuration

The provider's data must be accessible via **HTTP/HTTPS**. Supported source types:

- **REST APIs** - RESTful web services
- **Web Services** - SOAP or other HTTP-based services
- **HTTP Endpoints** - Any HTTP-accessible data source
- **OAuth2-protected APIs** - APIs requiring OAuth2 authentication (see [OAuth2 Protected Data Sources](../mds_features/oauth2-protected-data-source.md))

### Data Address Properties

The provider configures the asset's `dataAddress` with:

- `type`: "HttpData"
- `baseUrl`: The HTTP endpoint URL
- Optional OAuth2 configuration for protected sources

### Provider Configuration Example

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "owl": "http://www.w3.org/2002/07/owl#",
    "mobilitydcatap": "https://w3id.org/mobilitydcat-ap/",
    "mobilitydcatap-theme": "https://w3id.org/mobilitydcat-ap/mobility-theme/",
    "adms": "http://www.w3.org/ns/adms#",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "skos": "http://www.w3.org/2004/02/skos/core#",
    "rdf": "http://www.w3.org/2000/01/rdf-schema#"
  },
  "@type": "Asset",
  "@id": "http-api-asset",
  "properties": {
    "dct:title": "Weather Data API",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "INFRASTRUCTURE_AND_LOGISTICS"
    }
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://api.provider.example.com/weather"
  }
}
```

## Consumer: Pull Mode (HTTP-PULL)

The consumer specifies `HttpData-PULL` as the transfer type in the `dataDestination`.

The consumer receives an **Endpoint Data Reference (EDR)** containing:

- Provider's public data plane endpoint
- Authentication token (time-limited JWT)
- Metadata for accessing the data

The consumer uses the EDR to make authenticated HTTP requests directly to the provider's data plane, which proxies requests to the actual data source.

### Transfer Process

#### 1. Transfer Initiation

The consumer initiates a transfer request either via:

- The EDC Management UI
- A backend application using the Management API

The consumer specifies `HttpData-PULL` as the transfer type instead of providing a data sink URL. The consumer's control plane sends a transfer request to the provider's control plane, including:

- Contract agreement reference
- Transfer type specification (`HttpData-PULL`)
- Any additional context information

#### 2. Validation & EDR Generation

The provider's control plane:

- Validates the request (checks contract agreement, consumer eligibility, etc.)
- Generates an **Endpoint Data Reference (EDR)** containing:
  - Provider's public data plane endpoint
  - Authentication token
  - Additional metadata

The EDR is returned to the consumer and forwarded to the consumer's backend application (pull backend).

#### 3. Data Retrieval

The consumer uses the EDR to:

- Make authenticated requests to the provider's data plane endpoint
- The provider's data plane proxies these requests to the actual data source
- Data is streamed back through the provider's data plane to the consumer

The consumer can make multiple requests using the same EDR until the authentication token expires.

### Endpoint Data Reference (EDR) Structure

An EDR contains all the information needed for the consumer to access the provider's data:

```json
{
  "id": "2d5348ea-b1e0-4b69-a625-07e7b093944a",
  "endpoint": "https://provider-dataplane.example.com/public",
  "authKey": "Authorization",
  "authCode": "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

- **id**: Unique identifier for this EDR
- **endpoint**: Provider's public data plane endpoint URL
- **authKey**: HTTP header name for authentication (typically "Authorization")
- **authCode**: Authentication token value (typically a JWT Bearer token)

### Consumer Configuration Example (Pull)

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "http-api-asset",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "HttpData-PULL"
  },
  "callbackAddresses": [
    {
      "@type": "CallbackAddress",
      "transactional": true,
      "uri": "http://consumer-backend.example.com/edr",
      "events": ["transfer.process.started"]
    }
  ],
  "protocol": "dataspace-protocol-http",
  "connectorId": "provider-connector-id",
  "connectorAddress": "https://provider.example.com/protocol"
}
```

## Consumer: Push Mode (HTTP-PUSH)

The consumer specifies `HttpData-PUSH` as the transfer type in the `dataDestination` and provides:

- **Data sink URL** - HTTP endpoint where the provider should push the data
- **Authentication credentials** - API keys, bearer tokens, or other auth methods
- **Additional configuration** - Headers, method, content type, etc.

### Transfer Process

#### 1. Transfer Initiation

The consumer initiates a transfer request either via:

- The EDC Management UI
- A backend application using the Management API

The consumer provides details about their data sink (endpoint URL and credentials). The consumer's control plane sends a transfer request to the provider's control plane, including:

- Contract agreement reference
- **Access credentials for the consumer's data sink**
- **Data sink URL** (where the provider should push the data)
- Any additional context information

#### 2. Validation & Orchestration

The provider's control plane:

- Validates the request (checks contract agreement, consumer eligibility, etc.)
- Orchestrates the transfer by instructing its data plane to execute the transfer
- Passes the data sink credentials securely to the data plane

The provider's data plane fetches the data from the configured data source using the information defined in the asset's data address.

#### 3. Data Push

The provider's data plane pushes the data to the consumer's designated data sink using the provided credentials.

### Consumer Configuration Example (Push)

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "http-api-asset",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "HttpData-PUSH",
    "baseUrl": "https://consumer-backend.example.com/data-sink"
  },
  "protocol": "dataspace-protocol-http",
  "connectorId": "provider-connector-id",
  "connectorAddress": "https://provider.example.com/protocol"
}
```

## Comparison: Pull vs Push

| Aspect | HTTP Pull | HTTP Push |
|--------|-----------|-----------|
| **Data Flow** | Consumer pulls from provider | Provider pushes to consumer |
| **Control** | Consumer controls retrieval timing | Provider controls delivery |
| **Multiple Requests** | Yes, within token lifetime | No, one-time transfer |
| **Data Sink Required** | No | Yes |
| **Real-time Access** | Yes | No |
| **Use Case** | On-demand, interactive | Batch, one-time delivery |
