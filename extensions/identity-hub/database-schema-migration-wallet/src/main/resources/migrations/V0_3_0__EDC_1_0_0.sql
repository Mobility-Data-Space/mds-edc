-- Schema changes introduced by EDC IdentityHub between 0.18.0 and 1.0.0-RC5.
-- Releases 0.18.0, 0.18.1, 1.0.0-RC1, 1.0.0-RC3 and 1.0.0-RC4 carried no
-- schema changes for the stores bundled in the wallet runtime; every change
-- below was introduced in 1.0.0-RC5.

-- credential_resource: the primary key moved from (id) to the composite
-- (participant_context_id, id), so the credential ID is now unique only per
-- participant context. The old single-column constraint/index is replaced by
-- a composite unique index, and a lookup index on usage/state is added.
ALTER TABLE credential_resource DROP CONSTRAINT IF EXISTS credential_resource_pkey;
DROP INDEX IF EXISTS credential_resource_credential_id_uindex;
CREATE UNIQUE INDEX IF NOT EXISTS credential_resource_participant_id_pk ON credential_resource (participant_context_id, id);
CREATE INDEX IF NOT EXISTS credential_resource_participant_usage_state ON credential_resource (participant_context_id, usage, vc_state);

-- keypair_resource: track whether the private key is managed by IdentityHub,
-- and index key pairs by participant context. A participant context may hold at
-- most one key pair per key ID that is in use (state CREATED=100 or ACTIVATED=200).
ALTER TABLE keypair_resource ADD COLUMN IF NOT EXISTS private_key_managed BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS keypair_resource_participant_context_id ON keypair_resource (participant_context_id);
DO
$$
    BEGIN
        CREATE UNIQUE INDEX IF NOT EXISTS keypair_resource_participant_key_id ON keypair_resource (participant_context_id, key_id) WHERE state IN (100, 200);
    EXCEPTION
        WHEN unique_violation THEN
            RAISE WARNING 'Index keypair_resource_participant_key_id not created: a participant context has several key pairs in use with the same key ID';
    END
$$;

-- edc_holder_credentialrequest: the lease moved from the shared edc_lease table
-- onto the row, and an optimistic-locking version column was added. The state
-- index was renamed from issuance_process_state to holder_credentialrequest_state.
ALTER TABLE edc_holder_credentialrequest ADD COLUMN IF NOT EXISTS leased_by VARCHAR;
ALTER TABLE edc_holder_credentialrequest ADD COLUMN IF NOT EXISTS leased_at BIGINT;
ALTER TABLE edc_holder_credentialrequest ADD COLUMN IF NOT EXISTS lease_duration INTEGER;
ALTER TABLE edc_holder_credentialrequest ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
DROP INDEX IF EXISTS issuance_process_state;
CREATE INDEX IF NOT EXISTS holder_credentialrequest_state ON edc_holder_credentialrequest (state, state_time_stamp);

-- edc_jti_validation: index to support periodic deletion of expired entries.
CREATE INDEX IF NOT EXISTS jti_validation_expires_at_index ON edc_jti_validation (expires_at);

-- edc_credential_offers: store for the holder-credential-offer store, which is
-- part of the wallet runtime but was never created by the earlier migrations.
-- Created here directly in its 1.0.0-RC5 shape (row-based lease + version column).
CREATE TABLE IF NOT EXISTS edc_credential_offers
(
    id                     VARCHAR NOT NULL PRIMARY KEY, -- this is also the holderPid
    state                  INTEGER NOT NULL,
    state_count            INTEGER          DEFAULT 0 NOT NULL,
    state_time_stamp       BIGINT,
    created_at             BIGINT  NOT NULL,
    updated_at             BIGINT  NOT NULL,
    trace_context          JSON,
    error_detail           VARCHAR,
    participant_context_id VARCHAR NOT NULL,
    issuer_did             VARCHAR NOT NULL,
    credentials            JSON    NOT NULL DEFAULT '{}',
    leased_by              VARCHAR,
    leased_at              BIGINT,
    lease_duration         INTEGER,
    version                BIGINT  NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS credential_offer_state ON edc_credential_offers (state, state_time_stamp);
