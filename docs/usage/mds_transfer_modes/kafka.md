# Kafka

## Provider: Data Source Configuration

The provider's data must be available in a **Kafka topic**. The MDS connector supports:

- Apache Kafka or Kafka-compatible brokers
- OAuth2/OIDC authentication (OAUTHBEARER mechanism)
- SASL_PLAINTEXT security protocol

### Data Address Properties

The provider configures the asset's `dataAddress` with:

- `type`: "Kafka"
- `topic`: Kafka topic name
- `kafka.bootstrap.servers`: Kafka broker addresses
- `kafka.security.protocol`: Security protocol (e.g., "SASL_PLAINTEXT")
- `kafka.sasl.mechanism`: SASL mechanism (e.g., "OAUTHBEARER")
- `oidcDiscoveryUrl`: OIDC provider discovery URL
- `oidcRegisterClientTokenKey`: Vault reference for OIDC initial access token
- `kafkaAdminPropertiesKey`: Vault reference for Kafka admin properties

### Provider Kafka Configuration

- Kafka broker with SASL_PLAINTEXT and OAUTHBEARER support
- OIDC provider for client registration and token validation
- Kafka ACLs enabled for fine-grained access control

### Provider Configuration Example

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "Asset",
  "@id": "kafka-traffic-events",
  "properties": {
    "name": "Real-time Traffic Events Stream",
    "description": "Live stream of traffic events and incidents",
    "contenttype": "application/json"
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "Kafka",
    "https://w3id.org/edc/v0.0.1/ns/topic": "traffic-events",
    "https://w3id.org/edc/v0.0.1/ns/kafka.bootstrap.servers": "kafka.example.com:9092",
    "https://w3id.org/edc/v0.0.1/ns/kafka.security.protocol": "SASL_PLAINTEXT",
    "https://w3id.org/edc/v0.0.1/ns/kafka.sasl.mechanism": "OAUTHBEARER",
    "https://w3id.org/edc/v0.0.1/ns/oidcDiscoveryUrl": "https://auth.example.com/.well-known/openid-configuration",
    "https://w3id.org/edc/v0.0.1/ns/oidcRegisterClientTokenKey": "oidc-initial-access-token",
    "https://w3id.org/edc/v0.0.1/ns/kafkaAdminPropertiesKey": "kafka-admin-properties"
  }
}
```

## Consumer: Pull Mode (Kafka-PULL)

The consumer specifies `Kafka-PULL` as the transfer type in the `dataDestination`.

The consumer receives an **Endpoint Data Reference (EDR)** containing:

- Kafka bootstrap servers
- Topic name
- Consumer credentials (OAuth2 tokens)
- Consumer configuration properties

The consumer uses these credentials to directly connect to the provider's Kafka broker and consume messages.

### Transfer Process

#### 1. Transfer Initiation

The consumer initiates a transfer request via:

- The EDC UI
- A backend application using the Management API

The consumer specifies `Kafka-PULL` as the transfer type. The consumer's control plane sends a transfer request to the provider's control plane, including:

- Contract agreement reference
- Transfer type specification (`Kafka-PULL`)
- Callback address for EDR delivery

#### 2. Client Registration & EDR Generation

The provider connector:

- Registers a client for the consumer with the OIDC provider
- Generates OAuth2 credentials for the consumer
- Creates an EDR containing Kafka consumer credentials and configuration
- Configures Access Control Lists (ACLs) to restrict consumer to specific topics

The EDR is delivered to the consumer via the callback address.

#### 3. Topic Subscription

The consumer uses the EDR to:

- Create a Kafka consumer with the provided configuration
- Authenticate with Kafka using OAUTHBEARER mechanism
- Subscribe to the topic specified in the EDR

#### 4. Message Consumption

The consumer pulls messages directly from the Kafka topic. The Kafka broker validates OAuth2 tokens with the OIDC provider and enforces ACL restrictions.

#### 5. Access Revocation

When the transfer terminates, the provider revokes the consumer's topic access.

### Authentication Flow

The Kafka extension uses OpenID Connect (OIDC) for dynamic client registration and token-based authentication:

1. Provider registers a client for the consumer with the OIDC provider
2. Consumer receives OAuth2 credentials in the EDR
3. Consumer uses OAUTHBEARER mechanism to authenticate with Kafka
4. Kafka broker validates tokens with the OIDC provider
5. Access Control Lists (ACLs) restrict consumer to specific topics

### Consumer Kafka Configuration

- Kafka client library (Apache Kafka clients)
- Ability to receive EDR callbacks
- Network connectivity to provider's Kafka broker

### Consumer Configuration Example

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "assetId": "kafka-traffic-events",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "Kafka-PULL"
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

### EDR Structure

The consumer receives an EDR with the following structure:

```json
{
  "@type": "DataAddress",
  "type": "Kafka",
  "https://w3id.org/edc/v0.0.1/ns/topic": "traffic-events",
  "https://w3id.org/edc/v0.0.1/ns/kafka.bootstrap.servers": "kafka.example.com:9092",
  "https://w3id.org/edc/v0.0.1/ns/kafkaConsumerProperties": "bootstrap.servers=kafka.example.com:9092\nsecurity.protocol=SASL_PLAINTEXT\nsasl.mechanism=OAUTHBEARER\nsasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId='client-id' clientSecret='client-secret';\nsasl.login.callback.handler.class=org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler\nsasl.oauthbearer.token.endpoint.url=https://auth.example.com/token"
}
```

## Comparison with HTTP Pull

| Aspect | Kafka Pull | HTTP Pull |
|--------|------------|-----------|
| **Data Type** | Streaming, event-driven | Request-response, on-demand |
| **Throughput** | Very high (millions msg/sec) | Moderate |
| **Latency** | Low (milliseconds) | Higher (request overhead) |
| **Persistence** | Messages persisted in Kafka | Depends on source |
| **Multiple Requests** | Continuous stream | One-off or periodic |
| **Use Case** | Real-time streams, events | On-demand queries, APIs |
