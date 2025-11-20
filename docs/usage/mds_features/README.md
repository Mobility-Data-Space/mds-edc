# MDS EDC Connector Features

This section documents the specialized features and capabilities of the MDS EDC Connector that extend the standard Eclipse EDC functionality for the Mobility Data Space.

The MDS EDC Connector builds upon the Eclipse Dataspace Components (EDC) framework with additional features tailored for mobility data exchange use cases. These features enhance security, usability, and semantic interoperability within the Mobility Data Space ecosystem.

## Available Features

### Protected Data Source

- **[OAuth2 Protected Data Sources](oauth2-protected-data-source.md)** - Secure integration with OAuth2-protected APIs as data sources

### Contract Management

- **[Manual Contract Negotiation Approval](contract_negotiation_manual_approval_rejection.md)** - Provider-side manual approval/rejection of contract negotiations

### Semantic Interoperability

- **[MDS Vocabulary and Semantics](mds_vocabulary_semantics.md)** - Mobility-specific metadata properties and semantic validation for assets
- **[On-Request Data Offers](on-request-data-offers.md)** - Publish data availability intentions without immediate data source

## Feature Categories

### Security & Authentication

- OAuth2 integration for data source authentication
- Manual contract approval workflows for enhanced provider control

### Semantic Standards

- Mobility DCAT-AP vocabulary support
- Data category and subcategory taxonomies
- Transport mode classifications
- Geospatial metadata

### Flexible Data Publishing

- On-request data offers for demand-driven data sharing
- Custom asset properties for extended metadata

## Getting Started

Each feature documentation includes:

- Overview and use cases
- Configuration examples
- Step-by-step implementation guides
- Security considerations
- Troubleshooting tips

## MDS-Specific Extensions

The MDS EDC Connector includes extensions that are specific to the Mobility Data Space:

### Mobility DCAT-AP Vocabulary

Support for mobility-specific metadata properties including:

- Mobility themes and categories
- Transport modes
- Data content classifications
- Geospatial references

### Contract Negotiation Enhancements

Provider-side controls for:

- Manual approval workflows
- Contract negotiation state management
- Fine-grained access control

## See Also

- [Transfer Modes Documentation](../mds_transfer_modes/README.md)
- [Management API Documentation](../management_api/overview.md)
- [Deployment Guide](../../deployment/production_docker_image.md)
- [FAQ](../../FAQ.md)

## Contributing

For feature requests or bug reports related to MDS-specific features, please visit:

- [GitHub Issues](https://github.com/Mobility-Data-Space/mds-edc/issues)
- [GitHub Discussions](https://github.com/Mobility-Data-Space/mds-edc/discussions)
