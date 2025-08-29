package eu.dataspace.connector.tests.extensions;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Map.entry;
import static org.apache.kafka.common.config.internals.BrokerSecurityConfigs.ALLOWED_SASL_OAUTHBEARER_URLS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit extension that sets up Kafka and Keycloak containers for testing.
 */
public class KafkaExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.2";
    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";
    private static final String OAUTH_REALM = "kafka";

    private final Network network = Network.newNetwork();
    private final GenericContainer<?> keycloakContainer = new GenericContainer<>(KEYCLOAK_IMAGE)
        .withNetwork(network)
        .withNetworkAliases("keycloak")
        .withCommand("start-dev --import-realm")
        .withLogConsumer(o -> System.out.println("[keycloak] " + o.getUtf8StringWithoutLineEnding()))
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
        .withLogConsumer(o -> System.out.println("[kafka] " + o.getUtf8StringWithoutLineEnding()))
        .withEnv(Map.ofEntries(
                entry("KAFKA_NODE_ID", "1"),
                entry("KAFKA_PROCESS_ROLES", "broker,controller"),
                entry("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@kafka-kraft:29093"),
                entry("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER"),
                entry("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER"),
                entry("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1"),

                entry("KAFKA_AUTHORIZER_CLASS_NAME", "org.apache.kafka.metadata.authorizer.StandardAuthorizer"),

                entry("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true"),
                entry("KAFKA_SASL_ENABLED_MECHANISMS", "OAUTHBEARER"),
                entry("KAFKA_LISTENERS", "OIDC://0.0.0.0:9092,CONTROLLER://kafka-kraft:29093,BROKER://kafka-kraft:29094"),
                entry("KAFKA_ADVERTISED_LISTENERS", "OIDC://localhost:9092,BROKER://kafka-kraft:29094"),
                entry("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "OIDC:SASL_PLAINTEXT,CONTROLLER:PLAINTEXT,BROKER:PLAINTEXT"),

                entry("KAFKA_LISTENER_NAME_OIDC_SASL_ENABLED_MECHANISMS", "OAUTHBEARER"),
                entry("KAFKA_LISTENER_NAME_OIDC_OAUTHBEARER_SASL_SERVER_CALLBACK_HANDLER_CLASS", "org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler"),
                entry("KAFKA_LISTENER_NAME_OIDC_OAUTHBEARER_SASL_JAAS_CONFIG", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;"),

                entry("KAFKA_SASL_OAUTHBEARER_JWKS_ENDPOINT_URL", "http://keycloak:8080/realms/kafka/protocol/openid-connect/certs"),
                entry("KAFKA_SASL_OAUTHBEARER_EXPECTED_AUDIENCE", "account"),

                entry(
                        "KAFKA_OPTS",
                        "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf -Dorg.apache.kafka.sasl.oauthbearer.allowed.urls=http://keycloak:8080/realms/kafka/protocol/openid-connect/certs"
                )
        ))
        .withExposedPorts(9092)
        .withCopyFileToContainer(
                MountableFile.forClasspathResource("kafka/config/kafka-jaas.conf"),
                "/etc/kafka/kafka_server_jaas.conf"
        )
        .dependsOn(keycloakContainer)
        .waitingFor(Wait.forListeningPort());

    @Override
    public void beforeAll(ExtensionContext context) {
        keycloakContainer.start();
        var keycloakAdmin = createAdminClient();
        createRealm(keycloakAdmin, "kafka");
        var clientsResource = keycloakAdmin.realm("kafka").clients();
        var providerClient = new ClientRepresentation();
        providerClient.setId("myclient");
        providerClient.setEnabled(true);
        providerClient.setProtocol("openid-connect");
        providerClient.setSecret("mysecret");
        providerClient.setServiceAccountsEnabled(true);
        providerClient.setStandardFlowEnabled(false);
        providerClient.setPublicClient(false);
        clientsResource.create(providerClient);
        var clientId = clientsResource.findByClientId("myclient").get(0).getId();
        var serviceAccountUserId = clientsResource.get(clientId).getServiceAccountUser().getId();
        kafkaContainer.setPortBindings(List.of("9092:9092"));
        kafkaContainer.withEnv("KAFKA_SUPER_USERS", "User:ANONYMOUS;User:" + serviceAccountUserId).start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        kafkaContainer.stop();
        keycloakContainer.stop();
    }

    public Integer getOAuthServicePort() {
        return keycloakContainer.getMappedPort(8080);
    }

    public static KafkaConsumer<String, String> createKafkaConsumer(final KafkaEdr edrData) {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, edrData.kafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, edrData.kafkaGroupPrefix());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put("auto.offset.reset", "earliest");

        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, edrData.kafkaSecurityProtocol());
        props.put(SaslConfigs.SASL_MECHANISM, edrData.kafkaSaslMechanism());

        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=\"%s\" clientSecret=\"%s\";"
                .formatted(edrData.clientId(), edrData.clientSecret()));
        props.put(SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, edrData.tokenEndpoint());
        props.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, OAuthBearerLoginCallbackHandler.class.getName());

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        return new KafkaConsumer<>(props);
    }

    public KafkaProducer<String, String> initializeKafkaProducer() {
        var tokenUrl = getTokenUrl();

        var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=\"%s\" clientSecret=\"%s\";"
                .formatted("myclient", "mysecret"));
        props.put(SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, tokenUrl);
        props.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, OAuthBearerLoginCallbackHandler.class.getName());

        System.setProperty(ALLOWED_SASL_OAUTHBEARER_URLS_CONFIG, tokenUrl);

        return new KafkaProducer<>(props);
    }

    private void createRealm(Keycloak adminClient, String realmName) {
        var realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);

        adminClient.realms().create(realm);
        var realmResource = adminClient.realms().realm(realmName);

        var config = Map.of(
                "access.token.claim", "true",
                "id.token.claim", "true",
                "userinfo.token.claim", "true",
                "jsonType.label", "String",
                "multivalued", "true"
        );

        var mapper = new ProtocolMapperRepresentation();
        mapper.setName("scope_mapper");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usermodel-attribute-mapper");
        mapper.setConfig(config);

        var clientScopeRepresentation = new ClientScopeRepresentation();
        clientScopeRepresentation.setId("openid");
        clientScopeRepresentation.setDescription("Adds scope claim with openid value");
        clientScopeRepresentation.setName("openid");
        clientScopeRepresentation.setProtocol("openid-connect");
        clientScopeRepresentation.setAttributes(Collections.singletonMap("include.in.token.scope", "true"));
        clientScopeRepresentation.setProtocolMappers(List.of(mapper));

        realmResource.clientScopes().create(clientScopeRepresentation);
        realmResource.addDefaultDefaultClientScope("openid");
    }

    public static Future<RecordMetadata> sendMessage(final KafkaProducer<String, String> producer, final String topic, final String key, final String value) {
        var producerRecord = new ProducerRecord<>(topic, key, value);
        return producer.send(producerRecord, (final RecordMetadata metadata, final Exception e) -> {
            if (e != null) {
                System.out.println("Failed to send record: " + e.getMessage() + e);
            } else {
                System.out.println("Sent record(topic=%s key=%s value=%s) meta(partition=%s, offset=%s)".formatted(
                        producerRecord.topic(), producerRecord.key(), producerRecord.value(), metadata.partition(), metadata.offset()));
            }
        });
    }

    public String createInitialAccessToken() {
        return createAdminClient().realm(OAUTH_REALM)
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(100, 10))
                .getToken();
    }

    public String oidcDiscoveryUrl() {
        return "http://localhost:%s/realms/%s/.well-known/openid-configuration".formatted(getOAuthServicePort(), OAUTH_REALM);
    }

    public CreateAclsResult createAcls(AclBinding... bindings) {
        var adminProperties = new Properties();

        adminProperties.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        adminProperties.put(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
        adminProperties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=\"%s\" clientSecret=\"%s\";"
                .formatted("myclient", "mysecret"));
        adminProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        adminProperties.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, OAuthBearerLoginCallbackHandler.class.getName());
        adminProperties.put(SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, getTokenUrl());

        System.setProperty(ALLOWED_SASL_OAUTHBEARER_URLS_CONFIG, getTokenUrl());

        try (var adminClient = AdminClient.create(adminProperties)) {
            var result = adminClient.createAcls(Arrays.stream(bindings).toList());
            assertThat(result.all()).succeedsWithin(5, TimeUnit.SECONDS);
            return result;
        }
    }

    public AclBinding userCanDoAll(String principalName, ResourceType resourceType, String resourceName) {
        var pattern = new ResourcePattern(resourceType, resourceName, PatternType.LITERAL);
        var entry = new AccessControlEntry("User:" + principalName, "*", AclOperation.ALL, AclPermissionType.ALLOW);
        return new AclBinding(pattern, entry);
    }

    public @NotNull String getBootstrapServers() {
        return "localhost:" + kafkaContainer.getMappedPort(9092);
    }

    private @NotNull String getTokenUrl() {
        return "http://localhost:%d/realms/kafka/protocol/openid-connect/token".formatted(getOAuthServicePort());
    }

    private Keycloak createAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:%s".formatted(keycloakContainer.getFirstMappedPort()))
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();
    }

}
