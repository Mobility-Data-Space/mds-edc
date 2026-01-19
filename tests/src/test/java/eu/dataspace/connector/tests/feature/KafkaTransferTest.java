package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import static java.util.Map.entry;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

class KafkaTransferTest {

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

    @Test
    void shouldSupportKafkaPullTransfer() throws IOException {
        var topic = "topic-" + UUID.randomUUID();
        var oidcRegisterClientTokenKey = UUID.randomUUID().toString();

        var producer = KAFKA_EXTENSION.initializeKafkaProducer();
        Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())
                .scheduleAtFixedRate(() -> KafkaExtension.sendMessage(producer, topic, "key", "payload"), 0, 1, SECONDS);

        var providerVault = PROVIDER.getService(Vault.class);
        var kafkaAdminPropertiesKey = "kafkaAdminProperties";
        providerVault.storeSecret(oidcRegisterClientTokenKey, KAFKA_EXTENSION.createInitialAccessToken());
        providerVault.storeSecret(kafkaAdminPropertiesKey, serializeToString(KAFKA_EXTENSION.getAdminProperties()));

        // provider creates the asset, policy and offer on EDC
        var dataAddressProperties = createKafkaDataAddress(topic, oidcRegisterClientTokenKey, kafkaAdminPropertiesKey);
        var assetId = PROVIDER.createOffer(dataAddressProperties);

        // consumer initiates a kafka transfer with proper resource tracking
        var consumerEdrReceiver = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
        consumerEdrReceiver.start();
        consumerEdrReceiver.stubFor(WireMock.any(WireMock.urlEqualTo("/edr"))
            .willReturn(WireMock.aResponse()));

        var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("Kafka-PULL")
                .withCallbacks(Json.createArrayBuilder()
                        .add(createCallback("http://localhost:%s/edr".formatted(consumerEdrReceiver.port()), true, Set.of("transfer.process.started")))
                        .build())
                .execute();

        CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

        var edrRequests = await().until(() -> consumerEdrReceiver.getAllServeEvents().stream()
            .filter(e -> e.getRequest().getUrl().equals("/edr"))
            .toList(), it -> !it.isEmpty());
        var objectMapper = new JacksonTypeManager().getMapper();
        var edr = objectMapper.readTree(edrRequests.get(0).getRequest().getBody()).get("payload").get("dataAddress").get("properties");
        var edrData = objectMapper.convertValue(edr, KafkaEdr.class);

        var props = deserialize(edrData.kafkaConsumerProperties());
        var kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(List.of(edrData.topic()));

        var records = kafkaConsumer.poll(Duration.ofSeconds(10));
        assertThat(records).isNotEmpty();
        assertThat(records.records(edrData.topic())).isNotEmpty();

        var providerTransferProcessId = PROVIDER.getTransferProcesses().stream()
                .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

        PROVIDER.terminateTransfer(providerTransferProcessId);
        PROVIDER.awaitTransferToBeInState(providerTransferProcessId, DEPROVISIONED);

        await().untilAsserted(() -> {
            assertThatThrownBy(() -> kafkaConsumer.poll(Duration.ZERO)).isInstanceOf(TopicAuthorizationException.class);
        });

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
        Map<String, Object> properties = Map.ofEntries(
                entry(EDC_NAMESPACE + "type", "Kafka"),
                entry(EDC_NAMESPACE + "kafka.bootstrap.servers", KAFKA_EXTENSION.getBootstrapServers()),
                entry(EDC_NAMESPACE + "kafka.sasl.mechanism", "OAUTHBEARER"),
                entry(EDC_NAMESPACE + "kafka.security.protocol", "SASL_PLAINTEXT"),
                entry(EDC_NAMESPACE + "topic", topic),
                entry(EDC_NAMESPACE + "oidcDiscoveryUrl", KAFKA_EXTENSION.oidcDiscoveryUrl()),
                entry(EDC_NAMESPACE + "oidcRegisterClientTokenKey", oidcRegisterClientTokenKey),
                entry(EDC_NAMESPACE + "kafkaAdminPropertiesKey", kafkaAdminPropertiesKey)
        );

        return properties;
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
