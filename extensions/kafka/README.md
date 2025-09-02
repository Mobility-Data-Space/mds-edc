# Kafka Broker Extension

## Overview

The Kafka Broker Extension is a Control Plane extension that enables secure, dynamic access to Kafka topics within the
Eclipse Dataspace Connector (EDC) framework. This extension allows data providers to share Kafka streams with consumers
while maintaining full control over access permissions.

## Key Components

The extension consists of the following modules:

- **data-plane-kafka**: Data Plane extension that manages access to Kafka topics by creating credentials
  and tokens
- **data-plane-kafka-spi**: Defines the data address format for Kafka assets

## Technical Implementation

The extension implements the `DataFlowController` interface to handle the "Kafka-PULL" transfer type. The main
controller class, `KafkaBrokerDataFlowController`, manages the following operations:

1. **Start Transfer**: Creates OAuth2 credentials, generates tokens, and builds an Endpoint Data Reference (EDR) for the
   consumer
2. **Suspend/Terminate Transfer**: Revokes tokens and cleans up credentials
3. **Token Management**: Integrates with OAuth2 services for secure authentication

## DataAddress Schema

When creating a Kafka asset, use the following properties in the DataAddress:

| Key                       | Description                                                                                                        | Mandatory |
|:--------------------------|:-------------------------------------------------------------------------------------------------------------------|-----------|
| `type`                    | Identifier of Kafka data address. Should be set as 'KafkaBroker'                                                   | `true`    |
| `topic`                   | Defines the Kafka topic                                                                                            | `true`    |
| `kafka.bootstrap.servers` | Defines a custom endpoint URL to Kafka                                                                             | `true`    |
| `kafka.sasl.mechanism`    | Defines the SASL Kafka mechanism (OAUTHBEARER)                                                                     | `true`    |
| `kafka.security.protocol` | Defines the Kafka security protocol (SASL_PLAINTEXT or SASL_SSL)                                                   | `true`    |
| `kafka.poll.duration`     | Defines the duration of the consumer polling. The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds. | `false`   |
| `kafka.group.prefix`      | Defines the group prefix that will be allowed to use for the consumer                                              | `true`    |
| `tokenUrl`                | The OAuth2 token URL for retrieving access tokens                                                                  | `true`    |
| `revokeUrl`               | The OAuth2 revoke URL for invalidating tokens                                                                      | `true`    |
| `clientId`                | The OAuth2 client ID                                                                                               | `true`    |
| `clientSecretKey`         | Defines the `vault` entry containing the OAuth2 client secret to the `clientId`                                    | `true`    |

### Secret Resolution

The `clientSecretKey` property should point to a `vault` entry that contains a string object with the OAuth2 client
secret. The client should have permission to read the topic which is part of the Asset.

## Usage

For detailed usage instructions, refer to:

- [Admin Manual](../../docs/administration/admin-manual.md): Installation and configuration
- [Solution Design](../../docs/architecture/solution-design-kafka-pull.md): Detailed architecture and workflow

## Example

The [Bruno collection](collections/Kafka%20PoC%20Bruno%20collection) contains example HTTP requests that demonstrate how
to:

1. Create a Kafka asset
2. Create policies and contract definitions
3. Initiate a transfer process
4. Monitor the transfer
