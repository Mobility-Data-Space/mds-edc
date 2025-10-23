CREATE TABLE IF NOT EXISTS attestation_definitions (
    id                      VARCHAR PRIMARY KEY,
    participant_context_id  VARCHAR NOT NULL,
    attestation_type        VARCHAR NOT NULL UNIQUE,
    configuration           JSON DEFAULT '{}',
    created_date            BIGINT NOT NULL,
    last_modified_date      BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS credential_definitions (
    id                      VARCHAR PRIMARY KEY,
    participant_context_id  VARCHAR NOT NULL,
    credential_type         VARCHAR NOT NULL UNIQUE,
    attestations            JSON NOT NULL DEFAULT '[]',
    rules                   JSON NOT NULL DEFAULT '[]',
    mappings                JSON NOT NULL DEFAULT '[]',
    json_schema             JSON,
    json_schema_url         VARCHAR,
    validity                BIGINT NOT NULL,
    format                  VARCHAR NOT NULL,
    created_date            BIGINT NOT NULL,
    last_modified_date      BIGINT
);

CREATE TABLE IF NOT EXISTS credential_resource (
    id                    VARCHAR PRIMARY KEY,
    create_timestamp      BIGINT NOT NULL,
    issuer_id             VARCHAR NOT NULL,
    holder_id             VARCHAR NOT NULL,
    vc_state              INTEGER NOT NULL,
    metadata              JSON DEFAULT '{}',
    issuance_policy       JSON,
    reissuance_policy     JSON,
    raw_vc                VARCHAR,
    vc_format             INTEGER NOT NULL,
    verifiable_credential JSON NOT NULL,
    participant_context_id VARCHAR,
    usage VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS did_resources (
    did              VARCHAR PRIMARY KEY,
    create_timestamp BIGINT  NOT NULL,
    state_timestamp  BIGINT  NOT NULL,
    state            INT     NOT NULL,
    did_document     JSON    NOT NULL,
    participant_context_id   VARCHAR
);

CREATE TABLE IF NOT EXISTS edc_lease (
     leased_by         VARCHAR NOT NULL,
     leased_at         BIGINT,
     lease_duration    INTEGER NOT NULL,
     resource_id       VARCHAR NOT NULL,
     resource_kind     VARCHAR NOT NULL,
     PRIMARY KEY(resource_id, resource_kind)
);

CREATE TABLE IF NOT EXISTS edc_issuance_process (
    id                          VARCHAR PRIMARY KEY,
    state                       INTEGER NOT NULL,
    state_count                 INTEGER DEFAULT 0 NOT NULL,
    state_time_stamp            BIGINT,
    created_at                  BIGINT NOT NULL,
    updated_at                  BIGINT NOT NULL,
    trace_context               JSON,
    error_detail                VARCHAR,
    pending                     BOOLEAN DEFAULT FALSE,
    holder_id                   VARCHAR NOT NULL,
    participant_context_id      VARCHAR NOT NULL,
    holder_pid                  VARCHAR NOT NULL,
    claims                      JSON NOT NULL,
    credential_definitions      JSONB NOT NULL,
    credential_formats          JSONB NOT NULL
);
-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS issuance_process_state ON edc_issuance_process (state,state_time_stamp);

CREATE TABLE IF NOT EXISTS edc_jti_validation (
    token_id    VARCHAR PRIMARY KEY,
    expires_at  BIGINT
);

CREATE TABLE IF NOT EXISTS edc_sts_client (
    id                     VARCHAR PRIMARY KEY,
    client_id              VARCHAR NOT NULL,
    did                    VARCHAR NOT NULL,
    name                   VARCHAR NOT NULL,
    secret_alias           VARCHAR NOT NULL,
    created_at             BIGINT  NOT NULL,
    participant_context_id VARCHAR NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS sts_client_client_id_index ON edc_sts_client (client_id);

CREATE TABLE IF NOT EXISTS holders (
    holder_id               VARCHAR PRIMARY KEY,
    participant_context_id  VARCHAR NOT NULL,
    did                     VARCHAR NOT NULL,
    holder_name             VARCHAR,
    anonymous               BOOLEAN NOT NULL DEFAULT FALSE,
    created_date            BIGINT NOT NULL,
    last_modified_date      BIGINT,
    properties              JSON DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS keypair_resource (
    id                      VARCHAR PRIMARY KEY,
    participant_context_id  VARCHAR,
    timestamp               BIGINT NOT NULL,
    key_id                  VARCHAR NOT NULL,
    group_name              VARCHAR,
    is_default_pair         BOOLEAN DEFAULT FALSE,
    use_duration            BIGINT,
    rotation_duration       BIGINT,
    serialized_public_key   VARCHAR NOT NULL,
    private_key_alias       VARCHAR NOT NULL,
    state                   INT NOT NULL DEFAULT 100,
    key_context             VARCHAR,
    usage                   VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS participant_context (
    participant_context_id  VARCHAR PRIMARY KEY,
    created_date            BIGINT NOT NULL,
    last_modified_date      BIGINT,
    state                   INTEGER NOT NULL,
    api_token_alias         VARCHAR NOT NULL,
    did                     VARCHAR,
    roles                   JSON,
    properties              JSON DEFAULT '{}'
);
