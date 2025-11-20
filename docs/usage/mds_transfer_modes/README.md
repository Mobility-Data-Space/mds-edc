# Data Transfer Modes

EDC supports asynchronous, decoupled data exchange between participants. Understanding the different transfer modes is essential for implementing both data providers and consumers effectively.

Data planes are specialized, independently deployed components that handle the actual data movement using their own wire protocol—separate from control plane messaging.

## Data Sources vs Data Destinations

It's important to understand the distinction between:

### Data Source (Provider Side)

The **Data Source** is where the provider's data originates. When creating an asset, the provider configures the `dataAddress` to specify where the data plane should fetch the data from.

**Supported Data Sources:**

- **HTTP** - REST APIs, web services
- **Kafka** - Kafka topics for streaming data
- **S3** - AWS S3 buckets or S3-compatible storage
- **Azure Blob** - Azure Blob Storage containers

### Data Destination (Consumer Side)

The **Data Destination** determines how the data is delivered to the consumer. When initiating a transfer, the consumer specifies the `dataDestination` type.

**Supported Data Destinations:**

- **HTTP-PULL** - Consumer pulls data via EDR from provider's data plane
- **HTTP-PUSH** - Provider pushes data to consumer's HTTP endpoint
- **S3-PUSH** - Provider pushes data to consumer's S3 bucket
- **Azure Blob-PUSH** - Provider pushes data to consumer's Azure Blob Storage
- **Kafka-PULL** - Consumer pulls messages from provider's Kafka topic via EDR

## PUSH vs PULL Flows

### Consumer Pull Mode

In this approach, the consumer actively initiates data retrieval and takes responsibility for requesting and obtaining data from the provider's endpoint.

**Key Characteristics:**

- Consumer initiates the data flow operation
- The Endpoint Data Reference (EDR) provides all necessary coordinates for accessing the provider's public data endpoint
- EDR typically points to an HTTP endpoint but can reference other infrastructure like message brokers or object storage
- Common examples include HTTP requests to endpoints or pulling messages from queues
- Consumer controls when and how often to retrieve data

**Process Flow:**

![Consumer Pull](../../images/consumer-pull.png)

The consumer's data plane retrieves data directly from the provider's data plane by pulling it through the established endpoint reference.

### Provider Push Mode

In this mechanism, the provider actively transmits data to the consumer, giving the provider control over when and how data is delivered.

**Key Characteristics:**

- Provider actively pushes data to the consumer's destination
- Consumer's data plane prepares the destination endpoint
- Consumer supplies an access token when initiating the transfer process
- Provider uses the token to authenticate subsequent push operations
- Useful when consumers want datasets delivered to endpoints they control, such as object storage systems
- Data transfer happens once at the provider's discretion

**Process Flow:**

![Provider Push](../../images/provider-push.png)

The provider's data plane delivers data directly to the consumer's prepared destination endpoint, with the consumer maintaining control over where data arrives.

## Transfer Modes by Data Type

Different data sources support different transfer modes:

| Data Source | Supported Modes | Notes |
|------------|----------------|-------|
| HTTP | PULL, PUSH | Most flexible option |
| Kafka | PULL | Streaming data access |
| S3 | PUSH | Batch data delivery |
| Azure Blob | PUSH | Batch data delivery |
