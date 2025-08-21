# MDS EDC Kafka Streaming extension

## Baseline Work of TX-EDC Kafka PoC
https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension/tree/main

## Adaptations
- Bump EDC from 0.10.1 to 0.13.2
  - DPS Protocol: Move DataAddress creation and creds/auth to data plane manager?
  - Any other considerations?
- hotfix: canHandle() of the KafkaDataFlowController 
- E2E Tests
  - Kafka Extension class to set up the E2E environment
  - Kafka Transfer Test

## Goals

## Open Questions
  - Allow both types during build times? How to go about Kafka DataAddress (determine auth type based on source properties from Provider?):
    ```json
    {
      "type": "Kafka",
      "kafka.bootstrap.servers": "",
      "kafka.poll.duration": "",
      "kafka.group.prefix": "",
      "kafka.security.protocol": "SASL_PLAINTEXT",
      "kafka.sasl.mechanism": "OAUTHBEARER",
      "topic": "",
      "registrationUrl": "for managing client ID/secret pairs ??",
      "clientId": "",
      "clientSecretKey": ""
    }
    ```
  - Kafka as a Data Destination? (Destination from Consumer):
    ```json
    {
      "type": "Kafka"
    }
    ```

## Extension's role

1. Manage / Hold OAuth ClientID/Secret pairs for each active transfer (?)
  - Requires a tight integration (an adapter per oauth provider admin functions(?))
2. Maps Kafka properties to EDRs:
  - Currently JWT
    - jwt token
    - bootstrap servers
    - topic
    - kafka security protocol
    - kafka sasl mechanism
    - group
  - Desired Kafka Config with OAuth Creds
    - clientId
    - client secret (token)
    - tokenUrl
    - bootstrap servers
    - topic
    - kafka security protocol
    - kafka sasl mechanism
    - group
