ALTER TABLE edc_contract_agreement ADD COLUMN claims JSON;
ALTER TABLE edc_transfer_process ADD COLUMN claims JSON;

CREATE TABLE IF NOT EXISTS edc_target_node_directory
(
    id                      VARCHAR PRIMARY KEY NOT NULL,
    name                    VARCHAR NOT NULL,
    target_url              VARCHAR NOT NULL,
    supported_protocols     JSON
);

COMMENT ON COLUMN edc_target_node_directory.supported_protocols IS 'List<String> serialized as JSON';

CREATE TABLE IF NOT EXISTS edc_federated_catalog
(
    id                    VARCHAR PRIMARY KEY NOT NULL,
    catalog               JSON,
    marked                BOOLEAN DEFAULT FALSE
);