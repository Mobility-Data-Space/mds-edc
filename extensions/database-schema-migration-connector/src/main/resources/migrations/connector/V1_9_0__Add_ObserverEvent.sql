CREATE TABLE IF NOT EXISTS mds_observer_event
(
    id            VARCHAR PRIMARY KEY,
    envelope_json JSONB   NOT NULL,
    retry_count   INT     NOT NULL DEFAULT 0,
    next_retry_at BIGINT  NOT NULL
);
