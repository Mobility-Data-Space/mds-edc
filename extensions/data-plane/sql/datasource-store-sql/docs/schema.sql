--
-- table: edc_data_source
--
-- Lookup index for POST /v4/datasources/request. Holds only the queryable attributes (id, name, type) plus the
-- store-managed timestamps; the opaque `properties` are kept in the vault keyed by data source id, never in this table.
--
CREATE TABLE IF NOT EXISTS edc_data_source
(
    id         VARCHAR NOT NULL
        CONSTRAINT data_source_id_pk PRIMARY KEY,
    name       VARCHAR NOT NULL,
    type       VARCHAR NOT NULL,
    created_at BIGINT  NOT NULL,
    updated_at BIGINT  NOT NULL
);
