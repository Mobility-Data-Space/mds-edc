CREATE TABLE IF NOT EXISTS edc_participant_context_config (
    participant_context_id VARCHAR PRIMARY KEY,
    created_date           BIGINT NOT NULL,
    last_modified_date     BIGINT,
    entries                JSON DEFAULT '{}',
    private_entries        JSON DEFAULT '{}'
);
