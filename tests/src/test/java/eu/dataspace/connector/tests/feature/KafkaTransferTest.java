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
import org.apache.kafka.common.resource.ResourceType;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import static java.util.Map.entry;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

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
        var registerClientTokenKey = UUID.randomUUID().toString();

        KAFKA_EXTENSION.createAcls(KAFKA_EXTENSION.userCanDoAll("myclient", ResourceType.TOPIC, topic));
        var producer = KAFKA_EXTENSION.initializeKafkaProducer();
        Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())
                .scheduleAtFixedRate(() -> KafkaExtension.sendMessage(producer, topic, "key", "payload"), 0, 1, SECONDS);

        var providerVault = PROVIDER.getService(Vault.class);
        providerVault.storeSecret(registerClientTokenKey, KAFKA_EXTENSION.createInitialAccessToken());

        // provider creates the asset, policy and offer on EDC
        var dataAddressProperties = createKafkaDataAddress(topic, registerClientTokenKey);
        var assetId = PROVIDER.createOffer(dataAddressProperties);

        // consumer initiates a kafka transfer with proper resource tracking
        var consumerEdrReceiver = ClientAndServer.startClientAndServer(getFreePort());
        consumerEdrReceiver.when(request("/edr")).respond(response());

        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("Kafka-PULL")
                .withCallbacks(Json.createArrayBuilder()
                        .add(createCallback("http://localhost:%s/edr".formatted(consumerEdrReceiver.getPort()), true, Set.of("transfer.process.started")))
                        .build())
                .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);

        var edrRequests = await().until(() -> consumerEdrReceiver.retrieveRecordedRequests(request("/edr")), it -> it.length > 0);
        var objectMapper = new JacksonTypeManager().getMapper();
        var edr = objectMapper.readTree(edrRequests[0].getBodyAsRawBytes()).get("payload").get("dataAddress").get("properties");

        var edrData = objectMapper.convertValue(edr, KafkaEdr.class);

        try (var consumer = KafkaExtension.createKafkaConsumer(edrData)) {
            var edrDataTopic = edrData.topic();
            consumer.subscribe(List.of(edrDataTopic));

            var records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records).isNotEmpty();
            assertThat(records.records(edrDataTopic)).isNotEmpty();
        }
    }

    private Map<String, Object> createKafkaDataAddress(String topic, String registerClientTokenKey) {
        Map<String, Object> properties = Map.ofEntries(
                entry(EDC_NAMESPACE + "type", "Kafka"),
                entry(EDC_NAMESPACE + "kafka.bootstrap.servers", KAFKA_EXTENSION.getBootstrapServers()),
                entry(EDC_NAMESPACE + "kafka.sasl.mechanism", "OAUTHBEARER"),
                entry(EDC_NAMESPACE + "kafka.security.protocol", "SASL_PLAINTEXT"),
                entry(EDC_NAMESPACE + "topic", topic),
                entry(EDC_NAMESPACE + "openIdConnectDiscoveryUrl", KAFKA_EXTENSION.openIdConnectDiscoveryUrl()),
                entry(EDC_NAMESPACE + "registerClientTokenKey", registerClientTokenKey)
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

}
