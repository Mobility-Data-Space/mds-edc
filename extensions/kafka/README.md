# Data-Plane Kafka Extension

## Overview

The Kafka Broker Extension is a Control Plane extension that enables secure, dynamic access to Kafka topics within the
Eclipse Dataspace Connector (EDC) framework. This extension allows data providers to share Kafka streams with consumers
while maintaining full control over access permissions.

## Key Components

The extension consists of the following modules:

- **data-plane-kafka**: Data Plane extension that manages access to Kafka topics by creating credentials and managing ACLs
- **data-plane-kafka-spi**: Defines the data address format for Kafka assets

## DataAddress Schema

When creating a Kafka asset, use the following properties in the DataAddress:

| Key                          | Description                                                      |
|:-----------------------------|:-----------------------------------------------------------------|
| `type`                       | Identifier of Kafka data address. Should be set as 'Kafka'       |
| `topic`                      | Defines the Kafka topic                                          |
| `kafka.bootstrap.servers`    | Defines a custom endpoint URL to Kafka                           |
| `kafka.sasl.mechanism`       | Defines the SASL Kafka mechanism (OAUTHBEARER)                   |
| `kafka.security.protocol`    | Defines the Kafka security protocol (SASL_PLAINTEXT or SASL_SSL) |
| `oidcDiscoveryUrl`           | Discovery OpenID-connect endpoint                                |
| `oidcRegisterClientTokenKey` | Token that will be used to register the oidc client              |
| `kafkaAdminPropertiesKey`    | The properties to configure the KafkaAdminClient to manage ACLs  |

### Secret Resolution

These properties point to `vault` entries:
- `oidcRegisterClientTokenKey`: the vault entry contains a string object with the token that can be used to register the client
- `kafkaAdminPropertes` the vault entry contains the properties that can be used to instantiate a `KafkaAdminClient` to be used
  to manage the ACLs. It's a `Properties` object serialized to String.

## Example

Please look at the [KafkaTransferTest](../../tests/src/test/java/eu/dataspace/connector/tests/feature/KafkaTransferTest.java)
