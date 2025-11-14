CREATE TABLE IF NOT EXISTS edc_asset
(
    asset_id                VARCHAR PRIMARY KEY,
    created_at              BIGINT  NOT NULL,
    properties              JSON    DEFAULT '{}',
    private_properties      JSON    DEFAULT '{}',
    data_address            JSON    DEFAULT '{}',
    participant_context_id  VARCHAR,
    dataplane_metadata      JSON
);

CREATE TABLE IF NOT EXISTS edc_contract_definitions
(
    contract_definition_id VARCHAR PRIMARY KEY,
    created_at             BIGINT  NOT NULL,
    access_policy_id       VARCHAR NOT NULL,
    contract_policy_id     VARCHAR NOT NULL,
    assets_selector        JSON    NOT NULL,
    private_properties     JSON,
    participant_context_id VARCHAR
);

CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER DEFAULT 60000 NOT NULL,
    resource_id    VARCHAR NOT NULL,
    resource_kind  VARCHAR NOT NULL,
    PRIMARY KEY(resource_id, resource_kind)
);


CREATE TABLE IF NOT EXISTS edc_contract_agreement
(
    agr_id                      VARCHAR PRIMARY KEY,
    provider_agent_id           VARCHAR,
    consumer_agent_id           VARCHAR,
    signing_date                BIGINT,
    start_date                  BIGINT,
    end_date                    INTEGER,
    asset_id                    VARCHAR NOT NULL,
    policy                      JSON,
    agr_participant_context_id  VARCHAR,
    agr_agreement_id            VARCHAR NOT NULL,
    UNIQUE (agr_agreement_id, agr_participant_context_id)
);

CREATE TABLE IF NOT EXISTS edc_contract_negotiation
(
    id                      VARCHAR PRIMARY KEY,
    created_at              BIGINT NOT NULL,
    updated_at              BIGINT NOT NULL,
    correlation_id          VARCHAR,
    counterparty_id         VARCHAR NOT NULL,
    counterparty_address    VARCHAR NOT NULL,
    protocol                VARCHAR NOT NULL,
    type                    VARCHAR NOT NULL,
    state                   INTEGER DEFAULT 0 NOT NULL,
    state_count             INTEGER DEFAULT 0,
    state_timestamp         BIGINT,
    error_detail            VARCHAR,
    agreement_id            VARCHAR
        CONSTRAINT contract_negotiation_contract_agreement_id_fk
            REFERENCES edc_contract_agreement,
    contract_offers         JSON,
    callback_addresses      JSON,
    trace_context           JSON,
    pending                 BOOLEAN DEFAULT FALSE,
    protocol_messages       JSON,
    participant_context_id  VARCHAR
);
-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS contract_negotiation_state ON edc_contract_negotiation (state,state_timestamp);
CREATE INDEX IF NOT EXISTS contract_negotiation_correlationid_index ON edc_contract_negotiation (correlation_id);
CREATE INDEX IF NOT EXISTS contract_negotiation_agreement_id_index ON edc_contract_negotiation (agreement_id);

CREATE TABLE IF NOT EXISTS edc_data_plane_instance
(
    id                   VARCHAR PRIMARY KEY,
    data                 JSON
);

CREATE TABLE IF NOT EXISTS edc_edr_entry
(
   transfer_process_id      VARCHAR PRIMARY KEY,
   agreement_id             VARCHAR NOT NULL,
   asset_id                 VARCHAR NOT NULL,
   provider_id              VARCHAR NOT NULL,
   contract_negotiation_id  VARCHAR,
   created_at               BIGINT  NOT NULL,
   participant_context_id   VARCHAR
);

CREATE TABLE IF NOT EXISTS edc_jti_validation
(
    token_id   VARCHAR PRIMARY KEY,
    expires_at BIGINT
);

CREATE TABLE IF NOT EXISTS edc_policydefinitions
(
    policy_id               VARCHAR PRIMARY KEY,
    created_at              BIGINT  NOT NULL,
    permissions             JSON,
    prohibitions            JSON,
    duties                  JSON,
    profiles                JSON,
    extensible_properties   JSON,
    inherits_from           VARCHAR,
    assigner                VARCHAR,
    assignee                VARCHAR,
    target                  VARCHAR,
    policy_type             VARCHAR NOT NULL,
    private_properties      JSON,
    participant_context_id  VARCHAR
);

CREATE TABLE IF NOT EXISTS edc_policy_monitor
(
    entry_id             VARCHAR NOT NULL PRIMARY KEY,
    state                INTEGER NOT NULL            ,
    created_at           BIGINT  NOT NULL            ,
    updated_at           BIGINT  NOT NULL            ,
    state_count          INTEGER DEFAULT 0 NOT NULL,
    state_time_stamp     BIGINT,
    trace_context        JSON,
    error_detail         VARCHAR,
    properties           JSON,
    contract_id          VARCHAR
);
-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS policy_monitor_state ON edc_policy_monitor (state,state_time_stamp);

CREATE TABLE IF NOT EXISTS edc_transfer_process
(
    transferprocess_id          VARCHAR PRIMARY KEY,
    type                        VARCHAR NOT NULL,
    state                       INTEGER NOT NULL,
    state_count                 INTEGER NOT NULL DEFAULT 0,
    state_time_stamp            BIGINT,
    created_at                  BIGINT NOT NULL,
    updated_at                  BIGINT NOT NULL,
    trace_context               JSON,
    error_detail                VARCHAR,
    resource_manifest           JSON,
    provisioned_resource_set    JSON,
    content_data_address        JSON,
    deprovisioned_resources     JSON,
    private_properties          JSON,
    callback_addresses          JSON,
    pending                     BOOLEAN DEFAULT FALSE,
    transfer_type               VARCHAR,
    protocol_messages           JSON,
    data_plane_id               VARCHAR,
    correlation_id              VARCHAR,
    counter_party_address       VARCHAR,
    protocol                    VARCHAR,
    asset_id                    VARCHAR,
    contract_id                 VARCHAR,
    data_destination            JSON,
    participant_context_id      VARCHAR
);
-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS transfer_process_state ON edc_transfer_process (state,state_time_stamp);

CREATE TABLE IF NOT EXISTS edc_accesstokendata
(
    id           VARCHAR PRIMARY KEY,
    claim_token  JSON    NOT NULL,
    data_address JSON    NOT NULL,
    additional_properties JSON DEFAULT '{}'
);


CREATE TABLE IF NOT EXISTS edc_data_plane
(
    process_id                  VARCHAR PRIMARY KEY,
    state                       INTEGER NOT NULL,
    created_at                  BIGINT  NOT NULL,
    updated_at                  BIGINT  NOT NULL,
    state_count                 INTEGER DEFAULT 0 NOT NULL,
    state_time_stamp            BIGINT,
    trace_context               JSON,
    error_detail                VARCHAR,
    callback_address            VARCHAR,
    source                      JSON,
    destination                 JSON,
    properties                  JSON,
    flow_type                   VARCHAR,
    transfer_type_destination   VARCHAR,
    runtime_id                  VARCHAR,
    resource_definitions        JSON DEFAULT '[]'
);

-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS data_plane_state ON edc_data_plane (state,state_time_stamp);

CREATE TABLE IF NOT EXISTS edc_agreement_retirement
(
    contract_agreement_id     VARCHAR PRIMARY KEY,
    reason                    TEXT   NOT NULL,
    agreement_retirement_date BIGINT NOT NULL
);
