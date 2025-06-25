## Context
MDS Logging House as a trusted party for auditing, verifiability and compliance.

Currently, the MDS Logging House logs the following control plane events:
* Negotiations and Agreements
* Transfers

Code: https://github.com/Mobility-Data-Space/mds-logging-house-client/blob/develop/extension/src/main/java/com/truzzt/extension/logginghouse/client/LoggingHouseClientExtension.java

Zoom-in:
```java
eventRouter.registerSync(ContractNegotiationAgreed.class, eventSubscriber);
eventRouter.registerSync(ContractNegotiationVerified.class, eventSubscriber);
eventRouter.registerSync(ContractNegotiationFinalized.class, eventSubscriber);
eventRouter.registerSync(ContractNegotiationTerminated.class, eventSubscriber);

eventRouter.registerSync(TransferProcessRequested.class, eventSubscriber);
eventRouter.registerSync(TransferProcessInitiated.class, eventSubscriber);
eventRouter.registerSync(TransferProcessStarted.class, eventSubscriber);
eventRouter.registerSync(TransferProcessCompleted.class, eventSubscriber);
eventRouter.registerSync(TransferProcessTerminated.class, eventSubscriber);

eventRouter.registerSync(CustomLoggingHouseEvent.class, eventSubscriber);
```

### Messages sent to the LH
Current implementation of the LH client enables the following logging capacities:
* Contract Agreements
    * Participant ID
    * **In case of Provider**: Asset ID, Name, Description, Version, ContentType and Properties
    * Agreement ID, SigningDate, Policy, ..
* Transfer Process
    * Process ID, State, Protocol, Contract ID, Asset ID

Code: https://github.com/Mobility-Data-Space/mds-logging-house-client/blob/develop/extension/src/main/java/com/truzzt/extension/logginghouse/client/events/LoggingHouseEventSubscriber.java

## Additional requirements
* MDS wants to log contract agreement retirements
* MDS wants to provide proofs for the Data For Road Safety (DFRS) use case, that providers and consumers have exchanged specific type of data (category), within specific intervals, ..
    * Kafka topics (!) consumed / provided
    ```code
    Third-Party Monitoring and Auditability
        REQ-MON-001: An independent third party shall be able to monitor all data transactions.
        REQ-MON-002: The system shall maintain tamper-proof audit trails of data exchanges.
        REQ-MON-003: Monitoring must preserve privacy and access only allowed metadata.
        REQ-MON-004: The system shall support configurable audit scopes and frequencies.
        REQ-MON-005: The system shall provide alerting mechanisms for policy violations.
        REQ-MON-006: Monitoring shall be aware of and tailored to data classification levels.
        REQ-MON-007: Different audit log retention and policy configurations shall exist for each data level.
    ```
* MDS wants to log the data volume that is being transfered between participants (DataSource to DataSink) of the data space (in bytes).

## Proposal for the additional requirements

> WIP

* Control plane
    * TODO: agreement retirement (using custom logging house event class)
    * Nbr of transfers for assets with Data Category (Attribute) (?)
* Data Plane
    * After transfer
        * Volume of the data provided/consumed per successfull Transfer Process (in Bytes)
            * HTTP Data
            * S3 and Azure
        * Kafka (!!)
            * Per Topics