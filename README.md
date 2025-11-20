# MDS Connector

The MDS Connector is a distribution based on the Eclipse Dataspace Components (EDC) tailored for the Mobility Data Space. It facilitates secure and standardized data exchange between participants in the mobility ecosystem.

## Quick Start

For a quick start guide on building and deploying the MDS Connector, please refer to our [Deployment Guide](docs/deployment/production_blueprint_nginx.md).

## Documentation

Our documentation is organized into three main categories:

### Development

Documentation about features, architecture, and design for maintainers and contributors working on or extending the MDS Connector.

### Usage

Resources for using the MDS Connector:

#### Management API

- [Management API Overview](docs/usage/management_api/overview.md) - Complete guide to all management endpoints
- [Assets](docs/usage/management_api/assets.md) - Creating and managing assets
- [Policy Definitions](docs/usage/management_api/policy_definition.md) - Defining access and usage policies
- [Contract Definitions](docs/usage/management_api/contract_definition.md) - Linking assets with policies
- [Catalog](docs/usage/management_api/catalog.md) - Discovering available assets
- [Contract Negotiation](docs/usage/management_api/contract_negotiation.md) - Negotiating data access agreements
- [Contract Agreement](docs/usage/management_api/contract_agreement.md) - Managing finalized contracts
- [Transfer Process](docs/usage/management_api/transfer_process.md) - Executing data transfers
- [EDR Cache](docs/usage/management_api/edr_cache.md) - Endpoint Data Reference management
- [Contract Retirement](docs/usage/management_api/contract_retirement.md) - Retiring contract agreements

#### MDS Features

- [OAuth2 Protected Data Sources](docs/usage/mds_features/oauth2-protected-data-source.md) - Integrating OAuth2-protected APIs
- [MDS Vocabulary & Semantics](docs/usage/mds_features/mds_vocabulary_semantics.md) - MDS metadata standards
- [Manual Approval](docs/usage/mds_features/contract_negotiation_manual_approval_rejection.md) - Manual contract negotiation approval workflow
- [On Request Data Offers](docs/usage/mds_features/on_request_data_offers.md) - Creating data offers available on request

#### Data Transfer Modes

- [Transfer Modes Overview](docs/usage/mds_transfer_modes/README.md) - Understanding PUSH and PULL transfers
- [HTTP Data](docs/usage/mds_transfer_modes/http-data.md) - HTTP-based data transfers
- [AWS S3](docs/usage/mds_transfer_modes/aws-s3.md) - S3 bucket transfers
- [Azure Blob](docs/usage/mds_transfer_modes/azure-blob.md) - Azure Blob Storage transfers
- [Kafka](docs/usage/mds_transfer_modes/kafka.md) - Kafka streaming data transfers

### Deployment

Guides on deploying and configuring the MDS Connector:

- [Production Blueprint with Nginx](docs/deployment/production_blueprint_nginx.md) - Production deployment guide with data persistence considerations
- [Production Docker Image](docs/deployment/production_docker_image.md) - Docker-based deployment
- [Default Configuration](docs/deployment/mds_connector_default_configuration.md) - Configuration reference
- [Production Vault Setup](docs/deployment/production_vault_setup.md) - Configuring Vault for production use
- [Backup and Recovery](docs/deployment/backup_and_recovery.md) - Backup strategies and disaster recovery procedures

## Directory Structure

- `docs/`: Contains all documentation files.
- `extensions/`: Contains extension modules for the MDS Connector.
- `launchers/`: Contains executable modules for the MDS Connector.
- `tests/`: Contains end-to-end tests for MDS Connector features.

## Support

For support, please open an issue in the GitHub repository or contact our support team.

For more detailed information on EDC configuration and usage, refer to the official [Eclipse Dataspace Components documentation](https://eclipse-edc.github.io/docs/).
