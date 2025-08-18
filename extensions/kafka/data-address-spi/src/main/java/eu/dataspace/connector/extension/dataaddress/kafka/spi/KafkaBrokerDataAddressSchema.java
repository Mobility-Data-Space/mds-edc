package eu.dataspace.connector.extension.dataaddress.kafka.spi;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Defines the schema of a DataAddress representing a Kafka endpoint.
 */
public interface KafkaBrokerDataAddressSchema {

    /**
     * The transfer type.
     */
    String KAFKA_TYPE = "KafkaBroker";

    /**
     * The Kafka topic that will be allowed to poll for the consumer.
     */
    String TOPIC = EDC_NAMESPACE + "topic";

    /**
     * The kafka.bootstrap.servers property.
     */
    String BOOTSTRAP_SERVERS = EDC_NAMESPACE + "kafka.bootstrap.servers";

    /**
     * The kafka.poll.duration property which specifies the duration of the consumer polling.
     * <p>
     * The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.
     * This parameter is optional. The default value is 1 second.
     *
     * @see java.time.Duration#parse(CharSequence) for ISO-8601 duration format
     */
    String POLL_DURATION = EDC_NAMESPACE + "kafka.poll.duration";

    /**
     * The kafka.group.prefix that will be allowed to use for the consumer.
     */
    String GROUP_PREFIX = EDC_NAMESPACE + "kafka.group.prefix";

    /**
     * The security.protocol property.
     */
    String PROTOCOL = EDC_NAMESPACE + "kafka.security.protocol";

    /**
     * The sasl.mechanism property.
     */
    String MECHANISM = EDC_NAMESPACE + "kafka.sasl.mechanism";

    /**
     * The authentication token.
     */
    String TOKEN = EDC_NAMESPACE + "token";

    /**
     * The OAuth token URL for retrieving access tokens.
     */
    String OAUTH_TOKEN_URL = EDC_NAMESPACE + "tokenUrl";

    /**
     * The OAuth revoke URL for invalidating tokens.
     */
    String OAUTH_REVOKE_URL = EDC_NAMESPACE + "revokeUrl";
    // TODO we might consider adding the create client APIs from provider
    /**
     * The OAuth client ID.
     */
    String OAUTH_CLIENT_ID = EDC_NAMESPACE + "clientId";

    /**
     * The OAuth client secret key.
     */
    String OAUTH_CLIENT_SECRET_KEY = EDC_NAMESPACE + "clientSecretKey";
}