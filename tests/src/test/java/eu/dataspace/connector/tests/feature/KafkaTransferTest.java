package eu.dataspace.connector.tests.feature;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.IssuerExtension;
import eu.dataspace.connector.tests.extensions.KafkaEdr;
import eu.dataspace.connector.tests.extensions.KafkaExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Map.entry;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

class KafkaTransferTest {

    @Nested
    class Daps extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(1)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("provider", "consumer");

        @RegisterExtension
        @Order(2)
        private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

        @RegisterExtension
        @Order(3)
        private static final KafkaExtension KAFKA_EXTENSION = new KafkaExtension();

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        protected Daps() {
            super(PROVIDER, CONSUMER, KAFKA_EXTENSION);
        }
    }

    @Nested
    class Dcp extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(0)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("issuer", "wallet", "consumer", "provider");

        @RegisterExtension
        @Order(1)
        private static final IssuerExtension ISSUER = new IssuerExtension(POSTGRES_EXTENSION, VAULT_EXTENSION);

        @RegisterExtension
        @Order(2)
        private static final Wallet IDENTITY_HUB = MdsParticipantFactory.wallet(POSTGRES_EXTENSION, VAULT_EXTENSION, "consumer", "provider");

        @RegisterExtension
        @Order(3)
        private static final KafkaExtension KAFKA_EXTENSION = new KafkaExtension();

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVaultDcp("provider", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVaultDcp("consumer", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        protected Dcp() {
            super(PROVIDER, CONSUMER, KAFKA_EXTENSION);
        }

        @BeforeAll
        static void setUp() {
            ISSUER.registerAttestationAndCredentialDefinition();
            ISSUER.registerHolder(PROVIDER.getId(), PROVIDER.getName());
            ISSUER.registerHolder(CONSUMER.getId(), CONSUMER.getName());
            IDENTITY_HUB.requestCredentialIssuance(PROVIDER.getId(), ISSUER.did().get());
            IDENTITY_HUB.requestCredentialIssuance(CONSUMER.getId(), ISSUER.did().get());
        }
    }

    abstract static class Tests {

        private final MdsParticipant provider;
        private final MdsParticipant consumer;
        private final KafkaExtension kafka;

        protected Tests(MdsParticipant provider, MdsParticipant consumer, KafkaExtension kafka) {
            this.provider = provider;
            this.consumer = consumer;
            this.kafka = kafka;
        }

        @Test
        void shouldSupportKafkaPullTransfer() throws IOException {
            var topic = "topic-" + UUID.randomUUID();
            var oidcRegisterClientTokenKey = UUID.randomUUID().toString();

            var producer = kafka.initializeKafkaProducer();
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())
                    .scheduleAtFixedRate(() -> KafkaExtension.sendMessage(producer, topic, "key", "payload"), 0, 1, SECONDS);

            var providerVault = provider.getService(Vault.class);
            var kafkaAdminPropertiesKey = "kafkaAdminProperties";
            providerVault.storeSecret(oidcRegisterClientTokenKey, kafka.createInitialAccessToken());
            providerVault.storeSecret(kafkaAdminPropertiesKey, serializeToString(kafka.getAdminProperties()));

            var dataAddressProperties = createKafkaDataAddress(topic, oidcRegisterClientTokenKey, kafkaAdminPropertiesKey);
            var assetId = provider.createOffer(dataAddressProperties);

            var consumerEdrReceiver = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            consumerEdrReceiver.start();
            consumerEdrReceiver.stubFor(post(urlPathEqualTo("/edr")).willReturn(ok()));

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("Kafka-PULL")
                    .withCallbacks(Json.createArrayBuilder()
                            .add(createCallback("http://localhost:%s/edr".formatted(consumerEdrReceiver.port()), true, Set.of("transfer.process.started")))
                            .build())
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var edrRequests = await().until(() -> consumerEdrReceiver.getAllServeEvents().stream()
                    .filter(e -> e.getRequest().getUrl().equals("/edr"))
                    .toList(), it -> !it.isEmpty());
            var objectMapper = new JacksonTypeManager().getMapper();
            var edr = objectMapper.readTree(edrRequests.get(0).getRequest().getBodyAsString()).get("payload").get("dataAddress").get("properties");
            var edrData = objectMapper.convertValue(edr, KafkaEdr.class);

            assertThat(kafka.clientExistsInKeycloak(edrData.clientId())).isTrue();

            var props = deserialize(edrData.kafkaConsumerProperties());
            var kafkaConsumer = new KafkaConsumer<>(props);
            kafkaConsumer.subscribe(List.of(edrData.topic()));

            var records = kafkaConsumer.poll(Duration.ofSeconds(10));
            assertThat(records).isNotEmpty();
            assertThat(records.records(edrData.topic())).isNotEmpty();

            var providerTransferProcessId = provider.getTransferProcesses().stream()
                    .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                    .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

        provider.terminateTransfer(providerTransferProcessId);
        provider.awaitTransferToBeInState(providerTransferProcessId, TERMINATED);

            await().untilAsserted(() -> {
                assertThatThrownBy(() -> kafkaConsumer.poll(Duration.ZERO)).isInstanceOf(TopicAuthorizationException.class);
            });

            await().untilAsserted(() -> {
                assertThat(kafka.clientExistsInKeycloak(edrData.clientId())).isFalse();
            });
        }

        @Test
        void shouldIncludeOauthbearerExtensionsInEdr() throws IOException {
            var topic = "topic-" + UUID.randomUUID();
            var oidcRegisterClientTokenKey = UUID.randomUUID().toString();
            var oauthbearerExtensions = "logicalCluster=abc123,identityPoolId=pool-xyz";

            var producer = kafka.initializeKafkaProducer();
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())
                    .scheduleAtFixedRate(() -> KafkaExtension.sendMessage(producer, topic, "key", "payload"), 0, 1, SECONDS);

            var providerVault = provider.getService(Vault.class);
            var kafkaAdminPropertiesKey = "kafkaAdminProperties-" + UUID.randomUUID();
            providerVault.storeSecret(oidcRegisterClientTokenKey, kafka.createInitialAccessToken());
            providerVault.storeSecret(kafkaAdminPropertiesKey, serializeToString(kafka.getAdminProperties()));

            var dataAddressProperties = new HashMap<String, Object>(createKafkaDataAddress(topic, oidcRegisterClientTokenKey, kafkaAdminPropertiesKey));
            dataAddressProperties.put(EDC_NAMESPACE + "kafka.sasl.oauthbearer.extensions", oauthbearerExtensions);

            var assetId = provider.createOffer(dataAddressProperties);

            var consumerEdrReceiver = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            consumerEdrReceiver.start();
            consumerEdrReceiver.stubFor(post(urlPathEqualTo("/edr")).willReturn(ok()));

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("Kafka-PULL")
                    .withCallbacks(Json.createArrayBuilder()
                            .add(createCallback("http://localhost:%s/edr".formatted(consumerEdrReceiver.port()), true, Set.of("transfer.process.started")))
                            .build())
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var edrRequests = await().until(() -> consumerEdrReceiver.getAllServeEvents().stream()
                    .filter(e -> e.getRequest().getUrl().equals("/edr"))
                    .toList(), it -> !it.isEmpty());
            var objectMapper = new JacksonTypeManager().getMapper();
            var edr = objectMapper.readTree(edrRequests.get(0).getRequest().getBodyAsString()).get("payload").get("dataAddress").get("properties");
            var edrData = objectMapper.convertValue(edr, KafkaEdr.class);

            var props = deserialize(edrData.kafkaConsumerProperties());
            assertThat(props.getProperty("sasl.oauthbearer.extensions")).isEqualTo(oauthbearerExtensions);

            var providerTransferProcessId = provider.getTransferProcesses().stream()
                    .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                    .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

            provider.terminateTransfer(providerTransferProcessId);
            provider.awaitTransferToBeInState(providerTransferProcessId, TERMINATED);
        }

        private String serializeToString(Properties properties) {
            try (var writer = new StringWriter()) {
                properties.store(writer, "Serialized kafka admin properties");
                return writer.toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Map<String, Object> createKafkaDataAddress(String topic, String oidcRegisterClientTokenKey, String kafkaAdminPropertiesKey) {
            return Map.ofEntries(
                    entry(EDC_NAMESPACE + "type", "Kafka"),
                    entry(EDC_NAMESPACE + "kafka.bootstrap.servers", kafka.getBootstrapServers()),
                    entry(EDC_NAMESPACE + "kafka.sasl.mechanism", "OAUTHBEARER"),
                    entry(EDC_NAMESPACE + "kafka.security.protocol", "SASL_PLAINTEXT"),
                    entry(EDC_NAMESPACE + "topic", topic),
                    entry(EDC_NAMESPACE + "oidcDiscoveryUrl", kafka.oidcDiscoveryUrl()),
                    entry(EDC_NAMESPACE + "oidcRegisterClientTokenKey", oidcRegisterClientTokenKey),
                    entry(EDC_NAMESPACE + "kafkaAdminPropertiesKey", kafkaAdminPropertiesKey)
            );
        }

        private JsonObject createCallback(String url, boolean transactional, Set<String> events) {
            return Json.createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "CallbackAddress")
                    .add(EDC_NAMESPACE + "transactional", transactional)
                    .add(EDC_NAMESPACE + "uri", url)
                    .add(EDC_NAMESPACE + "events", events
                            .stream()
                            .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                            .build())
                    .build();
        }

        private static Properties deserialize(String serializedProperties) {
            var properties = new Properties();
            try (var reader = new StringReader(serializedProperties)) {
                properties.load(reader);
                return properties;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
