# MDS Connector Distribution
A dependency analysis of the MDS Connector extensions and usage of EDC.

## EDC Extensions for MDS Distribution 2.1
Note: based on EDC version **0.2.1.X**

| Extension Category | Extension Name | Extension Description | Default Configuration (*TODO*) |
|---------------------|-----------------|------------------------|------------------------|
| Core | configurationFilesystem | Manages system configuration using the filesystem | edc.fs.config=/path/to/config |
| Core | oauth2Core | Implements OAuth 2.0 authentication for the DAPS | daps.url=https://example.com/daps, daps.token=your-daps-token |
| Core | vaultFilesystem | Provides secure storage using the filesystem | edc.vault.path=/path/to/vault |
| Control-Plane | controlPlaneCore | Core functionality for the control plane | edc.controlplane.mode=default |
| Control-Plane | controlPlaneAggregateServices | Aggregated services for the control plane | edc.controlplane.services=all |
| Control-Plane | managementApi | API for managing the control plane | edc.api.auth.key=your-api-key |
| Control-Plane | authTokenbased | Token-based authentication for the control plane | edc.auth.token.validation.endpoint=https://example.com/validate |
| Control-Plane | apiObservability | Observability features for APIs | edc.api.observability.enabled=true |
| Control-Plane | http | HTTP-based communication for the control plane | edc.web.http.port=8080 |
| Control-Plane | dsp | Data Space Protocol implementation | edc.dsp.callback.address=https://example.com/dsp/callback |
| Control-Plane | jsonLd | JSON-LD support for the control plane | edc.jsonld.context=https://example.com/context.jsonld |
| Control-plane to Data-plane | transferDataPlane | Manages data transfer between control and data planes | edc.transfer.dataplane.token.validity.seconds=3600 |
| Control-plane to Data-plane | dataPlaneSelectorCore | Core functionality for selecting data planes | edc.dataplane.selector.strategy=random |
| Control-plane to Data-plane | dataPlaneSelectorClient | Client for data plane selection | edc.dataplane.selector.url=https://example.com/selector |
| Data-plane | dataPlaneHttp | HTTP-based communication for the data plane | edc.dataplane.http.port=8181 |
| Data-plane | dataPlaneFramework | Framework for the data plane | edc.dataplane.framework.enabled=true |
| Data-plane | dataPlaneCore | Core functionality for the data plane | edc.dataplane.token.validation.endpoint=https://example.com/validate |
| Data-plane | dataPlaneUtil | Utility functions for the data plane | edc.dataplane.util.enabled=true |
| MDS Extensions | MDS LoggingHouse Binder (Sovity) | Binder for MDS LoggingHouse by Sovity | edc.mds.logginghouse.endpoint=https://logginghouse.example.com |
| MDS Policies Extensions | policy-referring-connector | Extension for referring connector policies | edc.policy.referring.connector.enabled=true |
| MDS Policies Extensions | policy-time-interval | Extension for time interval policies | edc.policy.time.interval.enabled=true |
| MDS Policies Extensions | policy-always-true | Extension for always-true policies | edc.policy.always.true.enabled=true |
| MDS API Extensions | contract-termination | API extension for contract termination | edc.api.contract.termination.enabled=true |
| MDS API Extensions | edc-ui-config | API extension for EDC UI configuration | edc.ui.config.path=/path/to/ui/config |
| MDS API Extensions | last-commit-info | API extension for last commit information | edc.api.last.commit.info.enabled=true |
| MDS API Extensions | wrapper | API extension wrapper | edc.api.wrapper.enabled=true |
| MDS API Extensions | dataset-bugfix | API extension for dataset bug fixes | edc.api.dataset.bugfix.enabled=true |

## Comparison and Compatibility with EDC Upstream
### Version Compatibility Testing
- Use EDC Compatibility Test (*TODO*)

### Analysis of breaking changes
#### Connector to Connector communication: DSP
| EDC Upstream Version | Compatible DSP Version | Backward Compatible |
|-------------|------------------------|---------------------|
| 0.2.1       | TODO                   | TODO                |
| 0.3.0       | TODO                   | TODO                |
| 0.3.1       | TODO                   | TODO                |
| 0.4.0       | TODO                   | TODO                |
| 0.4.1       | TODO                   | TODO                |
| 0.5.0       | TODO                   | TODO                |
| 0.5.1       | TODO                   | TODO                |
| 0.6.0       | TODO                   | TODO                |
| 0.6.1       | TODO                   | TODO                |
| 0.6.2       | TODO                   | TODO                |
| 0.6.3       | TODO                   | TODO                |
| 0.6.4       | TODO                   | TODO                |
| 0.7.0       | TODO                   | TODO                |
| 0.7.1       | TODO                   | TODO                |
| 0.7.2       | TODO                   | TODO                |
| 0.8.0       | TODO                   | TODO                |
| 0.8.1       | TODO                   | TODO                |
| 0.9.0       | TODO                   | TODO                |
| 0.9.1       | TODO                   | TODO                |
| 0.10.0      | TODO                   | TODO                |

#### Users to Connector communication: Management API/SPI changes
| EDC Version | Management API Version | Backward Compatible |
|-------------|------------------------|---------------------|
| 0.2.1       | TODO                   | TODO                |
| 0.3.0       | TODO                   | TODO                |
| 0.3.1       | TODO                   | TODO                |
| 0.4.0       | TODO                   | TODO                |
| 0.4.1       | TODO                   | TODO                |
| 0.5.0       | TODO                   | TODO                |
| 0.5.1       | TODO                   | TODO                |
| 0.6.0       | TODO                   | TODO                |
| 0.6.1       | TODO                   | TODO                |
| 0.6.2       | TODO                   | TODO                |
| 0.6.3       | TODO                   | TODO                |
| 0.6.4       | TODO                   | TODO                |
| 0.7.0       | TODO                   | TODO                |
| 0.7.1       | TODO                   | TODO                |
| 0.7.2       | TODO                   | TODO                |
| 0.8.0       | TODO                   | TODO                |
| 0.8.1       | TODO                   | TODO                |
| 0.9.0       | TODO                   | TODO                |
| 0.9.1       | TODO                   | TODO                |
| 0.10.0      | TODO                   | TODO                |
