package eu.dataspace.connector.tests.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.KafkaExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static java.util.Map.entry;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class KafkaTransferTest {

    private static final String TEST_TOPIC_PREFIX = "test-topic";
    private static final String OAUTH_REALM = "kafka";
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String CLIENT_ID = "myclient";
    private static final String CLIENT_SECRET_KEY = "mysecretkey";
    private static final String CLIENT_SECRET_VALUE = "mysecret";

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
        var topic = TEST_TOPIC_PREFIX + "-oauth2";

        // Store client secret for each test
        PROVIDER.getService(Vault.class).storeSecret(CLIENT_SECRET_KEY, CLIENT_SECRET_VALUE);

        // provider creates the asset, policy and offer on EDC
        var dataAddressProperties = createKafkaDataAddress(topic); // OAuth2 includes client registration
        var assetId = PROVIDER.createOffer(dataAddressProperties);

        // provider runs producer application to generate data
        try {
            KAFKA_EXTENSION.runProducer(topic);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // consumer initiates a kafka transfer with proper resource tracking
        var consumerEdrReceiver = ClientAndServer.startClientAndServer(getFreePort());
        consumerEdrReceiver.when(request("/edr")).respond(response());

        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("Kafka-PULL")
                .withDestination(createKafkaDestinationDataAddress())
                .withCallbacks(Json.createArrayBuilder()
                        .add(createCallback("http://localhost:%s/edr".formatted(consumerEdrReceiver.getPort()), true, Set.of("transfer.process.started")))
                        .build())
                .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);

        var edrRequests = await().until(() -> consumerEdrReceiver.retrieveRecordedRequests(request("/edr")), it -> it.length > 0);
        var edr = new ObjectMapper().readTree(edrRequests[0].getBodyAsRawBytes()).get("payload").get("dataAddress").get("properties");

        // Verify EDR contains expected properties

        // Runs the Kafka Consumer App with appropriate authentication mode
        KAFKA_EXTENSION.runConsumer(edr);
    }

    private Map<String, Object> createKafkaDataAddress(String topic) {
        Map<String, Object> properties = Map.ofEntries(
                entry(EDC_NAMESPACE + "type", "KafkaBroker"),
                entry(EDC_NAMESPACE + "kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS),
                entry(EDC_NAMESPACE + "kafka.sasl.mechanism", "OAUTHBEARER"),
                entry(EDC_NAMESPACE + "kafka.security.protocol", "SASL_PLAINTEXT"),
                entry(EDC_NAMESPACE + "topic", topic),
                entry(EDC_NAMESPACE + "tokenUrl", format("http://localhost:%s/realms/%s/protocol/openid-connect/token", KAFKA_EXTENSION.getOAuthServicePort(), OAUTH_REALM)),
                entry(EDC_NAMESPACE + "revokeUrl", format("http://localhost:%s/realms/%s/protocol/openid-connect/revoke", KAFKA_EXTENSION.getOAuthServicePort(), OAUTH_REALM)),
                entry(EDC_NAMESPACE + "clientId", CLIENT_ID),
                entry(EDC_NAMESPACE + "clientSecretKey", CLIENT_SECRET_KEY),
                entry(EDC_NAMESPACE + "clientRegistration", format("http://localhost:%s/realms/%s/protocol/openid-connect/register", KAFKA_EXTENSION.getOAuthServicePort(), OAUTH_REALM))
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

    private JsonObject createKafkaDestinationDataAddress() {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "KafkaBroker")
                .build();
    }
}
