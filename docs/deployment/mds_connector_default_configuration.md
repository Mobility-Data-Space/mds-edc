# Default configuration of the MDS Connector

This document outlines an example of the configuration settings for deploying the MDS Connector. 
The provided connector deployment is configured through environment variables in the `docker-compose.yml` file.


```properties
# Connector Identification
# Set the participant ID and hostname for your connector
edc.participant.id = my_participant_id

# API Endpoints Configuration
# Configure the paths and ports for various API endpoints
web.http.path = /api
web.http.port = 8181
web.http.control.path = /api/control
web.http.control.port = 8186
web.http.management.path = /api/management
web.http.management.auth.key = x-api-key
web.http.management.port = 8182
web.http.protocol.path = /api/dsp
web.http.protocol.port = 8183
web.http.version.path = /api/version
web.http.version.port = 8184
web.http.public.path = /public
web.http.public.port = 8185

edc.dsp.callback.address = "https://my-connector-address/api/dsp"
edc.dataplane.api.public.baseurl = "http://my-connector-address/public"

# Security Configuration
# Set up the public and private key aliases for token verification and signing
edc.transfer.proxy.token.verifier.publickey.alias = public-key
edc.transfer.proxy.token.signer.privatekey.alias = private-key

# Vault Configuration
# Configure the Hashicorp Vault settings for secure secret management
edc.vault.hashicorp.url = "http://vault:8200"
edc.vault.hashicorp.token = root

# Postgres Configuration
# Configure Postgresql Database to use for persistence
edc.datasource.default.url = "jdbc:postgresql://postgres:5432/edc?currentSchema=mds_edc_schema"
edc.datasource.default.user = user
edc.datasource.default.password = password
org.eclipse.tractusx.edc.postgresql.migration.schema = "mds_edc_schema"

# DAPS Configuration
edc.oauth.token.url = "https://daps_url/token"
edc.oauth.client.id = my_client_id
edc.oauth.private.key.alias = "daps-private-key"
edc.oauth.certificate.alias = "daps-public-key"
edc.oauth.provider.jwks.url = "https://daps_url/certs"
edc.oauth.provider.audience = "https://daps.url/token"
edc.iam.token.scope = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL"
edc.oauth.endpoint.audience = "idsc:IDS_CONNECTORS_ALL"
edc.agent.identity.key = "referringConnector"

# Logging House configuration
edc.logginghouse.extension.enabled = true
edc.logginghouse.extension.url = "https://logging_house.url"
edc.datasource.logginghouse.url = "jdbc:postgresql://postgres:5432/edc?currentSchema=mds_edc_schema"
edc.datasource.logginghouse.user = user
edc.datasource.logginghouse.password = password
```

For more detailed information on each configuration property and advanced deployment scenarios, please refer to the full documentation or reach out in the discussion.
