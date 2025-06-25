* What do we want to achieve?
    * Improve the MDS Logging House as a trusted party for auditing, verifiability and compliance.
* Concrete additional requirements we want to cover through the LH
    * MDS wants to provide proofs for the Data For Road Safety (DFRS) use case, that providers and consumers have exchanged specific type of data (category), within specific intervals, ..
    * MDS wants to log the data volume that is being transfered between participants of the data space.

* Currently, the MDS Logging House logs the following:
    * Control Plane
        * Negotiations and Agreements
        * Transfers

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

* What do we want to log, additionally:
    * Control plane
        * TODO: agreement retirement (using custom logging house event class)
        * Nbr of transfers for assets with Data Category (Attribute)
    * Data Plane
        * After transfer
            * Volume of the data provided/consumed per successfull Transfer Process (in Bytes)
                * HTTP Data
                * S3 and Azure
            * Kafka (!!)
                * Per Topics