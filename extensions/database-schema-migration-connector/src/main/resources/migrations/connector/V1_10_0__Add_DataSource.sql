CREATE TABLE IF NOT EXISTS edc_data_source
(
    id         VARCHAR NOT NULL
        CONSTRAINT data_source_id_pk PRIMARY KEY,
    name       VARCHAR NOT NULL,
    type       VARCHAR NOT NULL,
    created_at BIGINT  NOT NULL,
    updated_at BIGINT  NOT NULL
);
