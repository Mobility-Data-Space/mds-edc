# MDS Connector Deployment Configuration

This document outlines the configuration settings for deploying the MDS Connector. The connector is primarily configured through environment variables in the `docker-compose.yml` file.

```properties
# Connector Identification
# Set the participant ID and hostname for your connector
edc.participant.id = my_participant_id
edc.hostname = my_edc_hostname

# API Endpoints Configuration
# Configure the paths and ports for various API endpoints
web.http.path = /api
web.http.port = 11001
web.http.control.path = /control
web.http.control.port = 11002
web.http.management.path = /management
web.http.management.port = 11003
web.http.protocol.path = /protocol
web.http.protocol.port = 11004
web.http.public.path = /public
web.http.public.port = 11005
web.http.version.path = /version
web.http.version.port = 11006

# Security Configuration
# Set up the public and private key aliases for token verification and signing
edc.transfer.proxy.token.verifier.publickey.alias = public-key
edc.transfer.proxy.token.signer.privatekey.alias = private-key

# Vault Configuration
# Configure the Hashicorp Vault settings for secure secret management
edc.vault.hashicorp.url = http://hashicorp_vault_url
edc.vault.hashicorp.token = root
edc.vault.hashicorp.folder = my_mds_edc
```

## Deployment Steps

1. Ensure you have Docker and Docker Compose installed on your deployment machine.
2. Copy the `docker-compose.yml` file to your deployment environment.
3. Modify the environment variables in the `docker-compose.yml` file according to your specific deployment needs.
4. Run `docker-compose up -d` to start the MDS Connector.

For more detailed information on each configuration property and advanced deployment scenarios, please refer to the full documentation.
