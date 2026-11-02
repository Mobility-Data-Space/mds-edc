-- Connector schema changes introduced by EDC between 0.18.0 and 1.0.0-RC5.
--
-- Only additive, code-required changes are applied here. EDC 1.0.0 also moved several
-- stores to a composite primary key (participant_context_id, id) on edc_asset,
-- edc_contract_definitions and edc_policydefinitions, but ships no in-place migration for
-- them: on an existing database the CREATE TABLE IF NOT EXISTS is a no-op and the old
-- single-column key is kept. The SQL stores do not rely on those keys (no ON CONFLICT),
-- so the primary keys are intentionally left unchanged. Likewise, columns dropped from the
-- 1.0.0 CREATE TABLE statements are left in place: they are unused and harmless.

-- edc_data_plane_instance: the store was redesigned from (id, data JSON) to explicit
-- columns, and EDC ships no in-place migration. The table is empty at migration time and
-- the embedded data plane re-registers its instance at boot, so it is safe to recreate it.
DROP TABLE IF EXISTS edc_data_plane_instance;
CREATE TABLE edc_data_plane_instance
(
    id                          VARCHAR NOT NULL PRIMARY KEY,
    state                       INTEGER NOT NULL,
    state_time_stamp            BIGINT,
    created_at                  BIGINT  NOT NULL,
    updated_at                  BIGINT  NOT NULL,
    url                         VARCHAR NOT NULL,
    last_active                 BIGINT,
    participant_context_id      VARCHAR,
    allowed_source_types        JSON DEFAULT '[]',
    allowed_transfer_types      JSON DEFAULT '[]',
    destination_provision_types JSON DEFAULT '[]',
    labels                      JSON DEFAULT '[]',
    properties                  JSON DEFAULT '{}',
    authorization_profile       JSON
);
CREATE INDEX IF NOT EXISTS data_plane_instance_participant_context_id ON edc_data_plane_instance (participant_context_id);
CREATE INDEX IF NOT EXISTS data_plane_instance_state ON edc_data_plane_instance (state);

-- edc_transfer_process: the lease moved from the shared edc_lease table onto the row, and
-- an optimistic-locking version column was added.
ALTER TABLE edc_transfer_process ADD COLUMN IF NOT EXISTS leased_by VARCHAR;
ALTER TABLE edc_transfer_process ADD COLUMN IF NOT EXISTS leased_at BIGINT;
ALTER TABLE edc_transfer_process ADD COLUMN IF NOT EXISTS lease_duration INTEGER;
ALTER TABLE edc_transfer_process ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
CREATE INDEX IF NOT EXISTS transfer_process_participant_context_id_created_at_index ON edc_transfer_process (participant_context_id, created_at);
CREATE INDEX IF NOT EXISTS transfer_process_correlation_id_index ON edc_transfer_process (correlation_id, participant_context_id);

-- edc_contract_negotiation: counter-party claims, row-based lease and optimistic-locking version.
ALTER TABLE edc_contract_negotiation ADD COLUMN IF NOT EXISTS negotiation_claims JSON;
ALTER TABLE edc_contract_negotiation ADD COLUMN IF NOT EXISTS leased_by VARCHAR;
ALTER TABLE edc_contract_negotiation ADD COLUMN IF NOT EXISTS leased_at BIGINT;
ALTER TABLE edc_contract_negotiation ADD COLUMN IF NOT EXISTS lease_duration INTEGER;
ALTER TABLE edc_contract_negotiation ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
CREATE INDEX IF NOT EXISTS contract_negotiation_participant_context_id_created_at_index ON edc_contract_negotiation (participant_context_id, created_at);

-- edc_contract_agreement: contract retirement support.
ALTER TABLE edc_contract_agreement ADD COLUMN IF NOT EXISTS retired BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE edc_contract_agreement ADD COLUMN IF NOT EXISTS retirement_reason VARCHAR;
ALTER TABLE edc_contract_agreement ADD COLUMN IF NOT EXISTS retirement_date BIGINT DEFAULT 0 NOT NULL;
CREATE INDEX IF NOT EXISTS contract_agreement_participant_context_id_index ON edc_contract_agreement (agr_participant_context_id, signing_date);
CREATE INDEX IF NOT EXISTS contract_agreement_asset_id_index ON edc_contract_agreement (asset_id);

-- edc_policy_monitor: the lease moved from the shared edc_lease table onto the row.
ALTER TABLE edc_policy_monitor ADD COLUMN IF NOT EXISTS leased_by VARCHAR;
ALTER TABLE edc_policy_monitor ADD COLUMN IF NOT EXISTS leased_at BIGINT;
ALTER TABLE edc_policy_monitor ADD COLUMN IF NOT EXISTS lease_duration INTEGER;

-- edc_contract_definitions: lookup indexes added in 1.0.0 (e.g. policy-deletion checks).
CREATE INDEX IF NOT EXISTS contract_definitions_access_policy_index ON edc_contract_definitions (participant_context_id, access_policy_id);
CREATE INDEX IF NOT EXISTS contract_definitions_contract_policy_index ON edc_contract_definitions (participant_context_id, contract_policy_id);
