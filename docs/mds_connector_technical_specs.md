# MDS Connector Technical Specifications

## Semantics of the Data Exchanged in MDS
MDS provides semantics of the Data Assets exchanged in the MDS, as follows:
- https://github.com/Mobility-Data-Space/mobility-data-space/wiki/MDS-Asset-Attributes
- https://github.com/Mobility-Data-Space/mobility-data-space/wiki/MDS-Ontology

## MDS requirements
Note: MDS follows the IDSA as a reference architecture model

| Service Type       | Components                                   |
|--------------------|----------------------------------------------|
| Clearing Services  | - MDS LoggingHouse Binder (Sovity)           |
| Identity Service   | - DAPS                                       |
| Discovery Service  | - Metadata Broker                            |

## MDS 2.1.X
| Component       | Version      | Release URL                                                        |
|-----------------|--------------|---------------------------------------------------------------------|
| MDS-Portal      | 2.3.1        | https://github.com/sovity/authority-portal/releases                 |
| DAPS            | 24.0.3-1     | https://github.com/sovity/sovity-daps/releases                      |
| Metadata Broker | 4.2.0        | https://github.com/sovity/edc-broker-server-extension/releases      |
| Logging House   | 1.0.0-beta.3 | https://github.com/ids-basecamp/clearinghouse/releases              |
| EDC             | 8.1.0        | https://github.com/sovity/edc-extensions/releases                   |

Note: EDC 8.1.0 is backward compatible with 7.4.2

### MDS Connector Distribution - v8.1.0 (equivalent to MDS 2.1)
#### EDC (0.2.1.3)
Note: The 0.2.1.3 is a patched version that does not exist in the EDC Releases.

##### Exchange Protocol
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| dsp | Facilitates standardized data exchange in the data space | TBD |

##### Identity Service
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| oauth2 | Provides authentication and authorization for data space identity | OAuth2 provider URL, client ID, client secret |

##### Secrets Management (Vault)
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| vaultFilesystem | Manages secure storage of sensitive information | Vault file path, encryption key |

##### Control Plane
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| controlPlaneCore | Core functionality for the control plane | TBD |
| controlPlaneAggregateServices | Aggregates services for the control plane | TBD |
| jsonLD | Supports JSON-LD data format | Context URL, schema mappings |
| managementApi | Provides management interface for the connector | API endpoint, authentication settings |
| token-based-auth | Implements token-based authentication for Management API | Token issuer URL, token validation rules |
| apiObservability | Enables monitoring and observability of APIs | TBD |
| transferDataPlane | Manages data transfer to the data plane | Transfer protocols |

##### Data Plane
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| dataPlaneSelectorCore | Core functionality for selecting data planes | TBD |
| dataPlaneSelectorClient | Client for data plane selection | TBD |
| dataPlaneCore | Core functionality for the data plane | TBD |
| dataPlaneFramework | Framework for building data plane components | TBD |
| dataPlaneUtil | Utility functions for data plane operations | TBD |

##### Available Data Planes
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| dataPlaneHttp | HTTP-based data plane implementation | TBD |

#### MDS
##### Policies
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| policy-referring-connector | Enables referencing policies across connectors | N/A |
| policy-time-interval | Implements time-based access policies | N/A |
| policy-always-true | Provides a simple always-true policy | N/A |

##### Logging House
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| MDS LoggingHouse Binder (Sovity) | Integrates MDS LoggingHouse functionality with the connector | TBD, LoggingHouse endpoint URL, authentication credentials |

##### API Extensions
| Extension | Description | Extension Configuration |
|-----------|-------------|--------------------------|
| contract-termination | Manages contract termination processes and workflows | TBD |
| edc-ui-config | Customizes and configures the EDC user interface | TBD |
| last-commit-info | Retrieves and displays information about the most recent code commit | Repository URL, branch name |
| wrapper | Encapsulates additional functionality around core components for enhanced features | TBD |
| dataset-bugfix | Resolves issues and improves handling of datasets | TBD |

## MDS Connector Features Table
| Feature      | Provided by extensions | Maintained by | Review Outcomes and Notes | Recommendation for Think-it and MDS|
|--------------|------------------------|---------|---------------------------|----------|
| MDS Logging House Support | mds-logginghouse-binder | Sovity | MDS-specific, critical for compliance | Copy and own |
| MDS Active Policy Extensions | policy-referring-connector, policy-time-interval, policy-always-true | Sovity | Custom policies, may need updates | Copy and own |
| MDS Contract Termination capability | contract-termination | Sovity | TBD: Part of standard DSP, evolving | Copy and own |
| MDS DAPS Support | oauth2Core | EDC | Core functionality, well-maintained | Depend on existing extension |
| MDS DSP Support | edc-dsp | EDC | Standard protocol, actively developed | Depend on existing extension |
| MDS EDC UI Support | edc-ui-config | Sovity | UI customization, low criticality | Recommend to remove |
| MDS Last Commit Info Support | last-commit-info | Sovity | Non-essential feature | Recommend to remove |
| Sovity Wrapper API | wrapper | Sovity | Sovity-specific, may limit portability | Further analysis needed |
