package eu.dataspace.connector.tests.extensions;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.apache.kafka.common.security.oauthbearer.internals.secured.BasicOAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ConfigurationUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS;
import static org.apache.kafka.common.config.SaslConfigs.SASL_LOGIN_CONNECT_TIMEOUT_MS;
import static org.apache.kafka.common.config.SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS;
import static org.apache.kafka.common.config.SaslConfigs.SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS;
import static org.apache.kafka.common.config.SaslConfigs.SASL_LOGIN_REFRESH_WINDOW_FACTOR;
import static org.apache.kafka.common.config.SaslConfigs.SASL_LOGIN_REFRESH_WINDOW_JITTER;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_SCOPE_CLAIM_NAME;
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL;

/**
 * JUnit extension that sets up Kafka and Keycloak containers for testing.
 */
public class KafkaExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.2";
    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    private final Network network = Network.newNetwork() ;
    private final GenericContainer<?> keycloakContainer = new GenericContainer<>(KEYCLOAK_IMAGE)
        .withNetwork(network)
        .withNetworkAliases("keycloak")
        .withCommand("start-dev --import-realm")
        .withCopyFileToContainer(
                MountableFile.forClasspathResource("kafka/config/keycloak-realm.json"),
                "/opt/keycloak/data/import/realm.json"
        )
        .withEnv(Map.ofEntries(
                entry("KC_BOOTSTRAP_ADMIN_USERNAME", "admin"),
                entry("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin"),
                entry("KC_DB", "dev-mem"),
                entry("KC_HOSTNAME", "localhost"),
                entry("KC_HOSTNAME_STRICT", "false"),
                entry("KC_HEALTH_ENABLED", "true")
        ))
        .withExposedPorts(8080, 9000)
        .waitingFor(Wait.forHttp("/health/ready").forPort(9000).forStatusCode(200));

    private final GenericContainer<?> kafkaContainer = new GenericContainer<>(KAFKA_IMAGE)
        .withNetwork(network)
        .withExtraHost("kafka-kraft", "127.0.0.1")
        .withEnv(Map.ofEntries(
                entry("KAFKA_PROCESS_ROLES", "broker,controller"),
                entry("KAFKA_NODE_ID", "1"),
                entry("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@kafka-kraft:29093"),
                entry("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER"),
                entry("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,OIDC:SASL_PLAINTEXT"),
                entry("KAFKA_LISTENERS", "PLAINTEXT://kafka-kraft:29092,CONTROLLER://kafka-kraft:29093,OIDC://0.0.0.0:9092"),
                entry("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://kafka-kraft:29092,OIDC://kafka-kraft:9092"),
                entry("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT"),
                entry("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1"),
                entry("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1"),
                entry("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1"),
                entry("KAFKA_SASL_ENABLED_MECHANISMS", "OAUTHBEARER"),
                entry("KAFKA_SASL_OAUTHBEARER_JWKS_ENDPOINT_URL", "http://keycloak:8080/realms/kafka/protocol/openid-connect/certs"),
                entry("KAFKA_SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL", "http://keycloak:8080/realms/kafka/protocol/openid-connect/token"),
                entry("KAFKA_SASL_OAUTHBEARER_CLIENT_ID", "myclient"),
                entry("KAFKA_SASL_OAUTHBEARER_CLIENT_SECRET", "mysecret"),
                entry("KAFKA_SASL_OAUTHBEARER_EXPECTED_AUDIENCE", "account"),
                entry(
                        "KAFKA_LISTENER_NAME_OIDC_OAUTHBEARER_SASL_SERVER_CALLBACK_HANDLER_CLASS",
                        "org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler"
                ),
                entry(
                        "KAFKA_OPTS",
                        "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf -Dorg.apache.kafka.sasl.oauthbearer.allowed.urls=http://keycloak:8080/realms/kafka/protocol/openid-connect/certs,http://keycloak:8080/realms/kafka/protocol/openid-connect/token")
                )
        )
        .withExposedPorts(29092, 9092, 29093)
        .withCopyFileToContainer(
                MountableFile.forClasspathResource("kafka/config/kafka-jaas.conf"),
                "/etc/kafka/kafka_server_jaas.conf"
        )
        .dependsOn(keycloakContainer)
        .waitingFor(Wait.forListeningPort()); ;

    @Override
    public void beforeAll(ExtensionContext context) {
        keycloakContainer.start();
        kafkaContainer.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        kafkaContainer.stop();
        keycloakContainer.stop();
    }

    public Integer getOAuthServicePort() {
        return keycloakContainer.getMappedPort(8080);
    }

    public void runProducer(String topic) throws InterruptedException {
        final KafkaProducer<String, String> producer = initializeKafkaProducer();
        //while (true) {
            sendMessage(producer, topic, "key", "payload");
            TimeUnit.MILLISECONDS.sleep(5000);
        //}

    }

    public void runConsumer(JsonNode edr) {
        EDRData edrData = new EDRData(edr) ;
        try (final KafkaConsumer<String, String> consumer = initializeKafkaConsumer(edrData)) {
            final List<String> topics = List.of(edrData.topic);

            consumer.subscribe(topics);
            //while (true) {
                final ConsumerRecords<String, String> records = consumer.poll(Duration.parse(edrData.kafkaPollDuration));
                for (final ConsumerRecord<String, String> consumerRecord : records) {
                    System.out.printf("Received record(topic=%s key=%s, value=%s) meta(partition=%s, offset=%s)%n",consumerRecord.topic(), consumerRecord.key(), consumerRecord.value(), consumerRecord.partition(), consumerRecord.offset());
                }
            //}
        }
    }

    private static void sendMessage(final KafkaProducer<String, String> producer, final String topic, final String key, final String value) {
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
        producer.send(producerRecord, (final RecordMetadata metadata, final Exception e) -> {
            if (e != null) {
                System.out.println("Failed to send record: " + e.getMessage() + e);
            } else {
                System.out.println("Sent record(topic={} key={} value={}) meta(partition={}, offset={})" +
                        producerRecord.topic() + producerRecord.key() + producerRecord.value() + metadata.partition() + metadata.offset());
            }
        });
    }

    private static KafkaConsumer<String, String> initializeKafkaConsumer(final EDRData edrData) {
        Objects.requireNonNull(edrData, "EDR data cannot be null");

        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, edrData.kafkaBootstrapServers);
        props.put(GROUP_ID_CONFIG, edrData.kafkaGroupPrefix);
        props.put(ENABLE_AUTO_COMMIT_CONFIG, "true"); // Automatically commit offsets
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest"); // Automatically reset the offset to the earliest offset
        props.put(AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        // Security settings from EDR Token (SASL/OAUTHBEARER)
        props.put(SECURITY_PROTOCOL_CONFIG, edrData.kafkaSecurityProtocol);
        props.put(SASL_MECHANISM, edrData.kafkaSaslMechanism);

        props.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS, OAuthBearerLoginCallbackHandler.class.getName());

        props.put(SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");

        props.put(SASL_LOGIN_CONNECT_TIMEOUT_MS, "15000"); // (optional) timeout for external authentication provider connection in ms
        props.put(SASL_LOGIN_REFRESH_BUFFER_SECONDS, "120"); // Refresh 2 minutes before expiry
        props.put(SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS, "30"); // Don't refresh more than once per 30 seconds
        props.put(SASL_LOGIN_REFRESH_WINDOW_FACTOR, "0.8"); // Refresh at 80% of token lifetime
        props.put(SASL_LOGIN_REFRESH_WINDOW_JITTER, "0.05"); // Add small random jitter

        return new KafkaConsumer<>(props);
    }

    private KafkaProducer<String, String> initializeKafkaProducer() {
        final String KEYCLOAK_CLIENT_ID = "myclient";
        final String KEYCLOAK_CLIENT_SECRET = "mysecret";
        final String KEYCLOAK_TOKEN_URL = "http://localhost:" + keycloakContainer.getMappedPort(8080) + "/realms/kafka/protocol/openid-connect/token";
        final String KAFKA_BOOTSTRAP_SERVERS = "localhost:" + kafkaContainer.getMappedPort(9092);

        final Properties props = new Properties();

        // Basic producer settings
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        props.put(ACKS_CONFIG, "all");
        props.put(RETRIES_CONFIG, 0);
        props.put(BATCH_SIZE_CONFIG, 16384); // 16KB
        props.put(LINGER_MS_CONFIG, 1);
        props.put(BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        props.put(DELIVERY_TIMEOUT_MS_CONFIG, 3000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        // Security settings for SASL/OAUTHBEARER
        props.put(SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put(SASL_MECHANISM, "OAUTHBEARER");

        // OAuth properties
        props.put(SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, KEYCLOAK_TOKEN_URL);
        props.put("sasl.oauthbearer.client.id", KEYCLOAK_CLIENT_ID);
        props.put("sasl.oauthbearer.client.secret", KEYCLOAK_CLIENT_SECRET);
        props.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS,
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler");

        // JAAS configuration for OAuth2
        props.put(SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
                        "clientId=\"" + KEYCLOAK_CLIENT_ID + "\" " +
                        "clientSecret=\"" + KEYCLOAK_CLIENT_SECRET + "\";"
        );

        return new KafkaProducer<>(props);
    }

    public static class EDRData {
        private static final String KAFKA_SECURITY_PROTOCOL = "kafka.security.protocol";
        private static final String KAFKA_SASL_MECHANISM = "kafka.sasl.mechanism";
        private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
        private static final String KAFKA_GROUP_PREFIX = "kafka.group.prefix";
        private static final String KAFKA_POLL_DURATION = "kafka.poll.duration";
        private String id;
        private String contractId;
        private String endpoint;
        private String topic;
        private String authKey;
        private String authCode;
        private String token;

        @JsonProperty(KAFKA_POLL_DURATION)
        private String kafkaPollDuration;
        @JsonProperty(KAFKA_GROUP_PREFIX)
        private String kafkaGroupPrefix;
        @JsonProperty(KAFKA_SECURITY_PROTOCOL)
        private String kafkaSecurityProtocol;
        @JsonProperty(KAFKA_SASL_MECHANISM)
        private String kafkaSaslMechanism;
        @JsonProperty(KAFKA_BOOTSTRAP_SERVERS)
        private String kafkaBootstrapServers;

        public EDRData(JsonNode edr) {

        }
        public String getToken() {
            return token;
        }
    }

    private static class EdrTokenCallbackHandler implements AuthenticateCallbackHandler {
        private boolean isInitialized = false;
        private String scopeClaimName;
        private final EdrDataProvider edrDataProvider;

        private EdrTokenCallbackHandler(EdrDataProvider edrDataProvider) {
            this.edrDataProvider = edrDataProvider;
        }

        @Override
        public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
            ConfigurationUtils cu = new ConfigurationUtils(configs, saslMechanism);
            scopeClaimName = cu.get(SASL_OAUTHBEARER_SCOPE_CLAIM_NAME);
            isInitialized = true;
        }

        @Override
        public void close() {

        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            checkInitialized();

            for (Callback callback : callbacks) {
                if (callback instanceof OAuthBearerTokenCallback tokenCallback) {
                    try {
                        handleTokenCallback(tokenCallback);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted while handling OAuthBearerTokenCallback" + e);
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw new UnsupportedCallbackException(callback, "Unsupported callback type");
                }
            }
        }

        private void handleTokenCallback(final OAuthBearerTokenCallback callback) throws IOException, InterruptedException {
            System.out.println("Handling OAuthBearerTokenCallback");
            String accessToken = fetchAccessTokenFromEDC();
            OAuthBearerToken oAuthBearerToken = toOAuthBearerToken(accessToken);
            callback.token(oAuthBearerToken);
            System.out.println("OAuth bearer token successfully provided to callback");
        }

        private OAuthBearerToken toOAuthBearerToken(final String accessToken) {
            System.out.println("Converting JWT access token to OAuthBearerToken");
            try {
                DecodedJWT jwt = JWT.decode(accessToken);
                long issuedAt = jwt.getIssuedAtAsInstant().toEpochMilli();
                long expiresAt = jwt.getExpiresAtAsInstant().toEpochMilli();
                String subject = jwt.getSubject();
                String scopeClaim = jwt.getClaim(scopeClaimName).asString();
                Set<String> scopes = Set.of(scopeClaim);

                return new BasicOAuthBearerToken(accessToken, scopes, expiresAt, subject, issuedAt);
            } catch (JWTDecodeException e) {
                throw new IllegalStateException("Failed to decode JWT token: " + e.getMessage(), e);
            }
        }

        private String fetchAccessTokenFromEDC() throws IOException, InterruptedException {
            System.out.println("Fetching access token from EDC");
            EDRData edrData = edrDataProvider.getEdrData();

            if (edrData == null) {
                throw new IOException("No EDR data found");
            }

            String token = edrData.getToken();
            if (token == null || token.isEmpty()) {
                throw new IOException("EDR data contains no token");
            }

            System.out.println("Successfully retrieved access token from EDC");
            return token;
        }

        private void checkInitialized() {
            if (!isInitialized) {
                throw new IllegalStateException(
                        format("To use %s, first call the configure method", getClass().getSimpleName())
                );
            }
        }

        /**
         * Interface for providing EDR data, allows for dependency injection and testing
         */
        public interface EdrDataProvider {
            EDRData getEdrData() throws IOException, InterruptedException;
        }

        /**
         * Default implementation of EdrDataProvider that uses EdrProvider
         */
        private static class DefaultEdrDataProvider implements EdrDataProvider {

            @Override
            public EDRData getEdrData() throws IOException, InterruptedException {
                return null ;
            }
        }
    }
}
