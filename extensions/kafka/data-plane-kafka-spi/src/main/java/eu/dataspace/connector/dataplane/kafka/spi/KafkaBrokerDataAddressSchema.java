package eu.dataspace.connector.dataplane.kafka.spi;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Defines the schema of a DataAddress representing a Kafka endpoint.
 */
public interface KafkaBrokerDataAddressSchema {

    String KAFKA_TYPE = "Kafka";

    String TOPIC = EDC_NAMESPACE + "topic";

    String OIDC_TOKEN_ENDPOINT = EDC_NAMESPACE + "tokenEndpoint";
    String OIDC_CLIENT_ID = EDC_NAMESPACE + "clientId";
    String OIDC_CLIENT_SECRET = EDC_NAMESPACE + "clientSecret";
    String OIDC_DISCOVERY_URL = EDC_NAMESPACE + "oidcDiscoveryUrl";
    String OIDC_REGISTER_CLIENT_TOKEN_KEY = EDC_NAMESPACE + "oidcRegisterClientTokenKey";
    String KAFKA_ADMIN_PROPERTIES_KEY = EDC_NAMESPACE + "kafkaAdminPropertiesKey";
    String KAFKA_CONSUMER_PROPERTIES = EDC_NAMESPACE + "kafkaConsumerProperties";

    String BOOTSTRAP_SERVERS = EDC_NAMESPACE + "kafka.bootstrap.servers";
    String SECURITY_PROTOCOL = EDC_NAMESPACE + "kafka.security.protocol";
    String SASL_MECHANISM = EDC_NAMESPACE + "kafka.sasl.mechanism";

}
