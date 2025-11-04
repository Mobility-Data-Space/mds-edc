package eu.dataspace.connector.tests.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.tests.IdentityHub;
import eu.dataspace.connector.tests.Issuer;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

class ManagementApiDcpTransferTest {

    @RegisterExtension
    @Order(0)
    private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("issuer");

    @RegisterExtension
    @Order(1)
    private static final Issuer ISSUER = MdsParticipantFactory.issuer(POSTGRES_EXTENSION);

    @RegisterExtension
    @Order(2)
    private static final IdentityHub IDENTITY_HUB = MdsParticipantFactory.identityHub("consumer", "provider");

    @RegisterExtension
    @Order(3)
    private static final MdsParticipant PROVIDER = MdsParticipantFactory.inMemoryDcp("provider", IDENTITY_HUB, ISSUER.did());

    @RegisterExtension
    @Order(3)
    private static final MdsParticipant CONSUMER = MdsParticipantFactory.inMemoryDcp("consumer", IDENTITY_HUB, ISSUER.did());

    @BeforeAll
    static void setUp() {
        ISSUER.registerAttestationAndCredentialDefinition();
        ISSUER.registerHolder(PROVIDER.getId());
        ISSUER.registerHolder(CONSUMER.getId());
        IDENTITY_HUB.requestCredentialIssuance(PROVIDER.getId(), ISSUER.did().get());
        IDENTITY_HUB.requestCredentialIssuance(CONSUMER.getId(), ISSUER.did().get());
    }

    @Test
    void shouldSupportHttpPushTransfer() {
        var providerDataSource = startClientAndServer(getFreePort());
        providerDataSource.when(request("/source")).respond(response("data"));
        var consumerDataDestination = startClientAndServer(getFreePort());
        consumerDataDestination.when(request("/destination")).respond(response());
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(providerDataSource.getPort())
        );

        var assetId = PROVIDER.createOffer(dataAddressProperties);

        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("HttpData-PUSH")
                .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.getPort() + "/destination"))
                .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

        await().untilAsserted(() -> {
            providerDataSource.verify(request("/source").withMethod("GET"));
            consumerDataDestination.verify(request("/destination").withBody(binary("data".getBytes())));
        });

        consumerDataDestination.stop();
        providerDataSource.stop();
    }

    @Test
    void shouldSupportHttpPullTransfer() throws IOException {
        var providerDataSource = startClientAndServer(getFreePort());
        providerDataSource.when(request("/source")).respond(response("data"));
        var consumerEdrReceiver = ClientAndServer.startClientAndServer(getFreePort());
        consumerEdrReceiver.when(request("/edr")).respond(response());
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(providerDataSource.getPort())
        );

        var assetId = PROVIDER.createOffer(dataAddressProperties);

        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("HttpData-PULL")
                .withCallbacks(Json.createArrayBuilder()
                        .add(createCallback("http://localhost:%s/edr".formatted(consumerEdrReceiver.getPort()), true, Set.of("transfer.process.started")))
                        .build())
                .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);

        var edrRequests = await().until(() -> consumerEdrReceiver.retrieveRecordedRequests(request("/edr")), it -> it.length > 0);

        var edr = new ObjectMapper().readTree(edrRequests[0].getBodyAsRawBytes()).get("payload").get("dataAddress").get("properties");

        var endpoint = edr.get(EDC_NAMESPACE + "endpoint").asText();
        var authCode = edr.get(EDC_NAMESPACE + "authorization").asText();

        var body = given()
                .header("Authorization", authCode)
                .when()
                .get(endpoint)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().body().asString();

        assertThat(body).isEqualTo("data");

        consumerEdrReceiver.stop();
    }

    @Test
    void shouldSupportOauth2OnProviderSide() {
        var sourceBackend = ClientAndServer.startClientAndServer(getFreePort());
        sourceBackend.when(request("/source")).respond(response("data"));
        var oauth2server = ClientAndServer.startClientAndServer(getFreePort());
        oauth2server.when(request("/token")).respond(response().withBody(json(Map.of("access_token", "token"))));
        var destinationBackend = ClientAndServer.startClientAndServer(getFreePort());
        destinationBackend.when(request("/destination")).respond(response());

        var clientSecretKey = UUID.randomUUID().toString();
        PROVIDER.getService(Vault.class).storeSecret(clientSecretKey, "clientSecretValue");

        Map<String, Object> dataSource = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(sourceBackend.getPort()),
                EDC_NAMESPACE + "oauth2:clientId", "clientId",
                EDC_NAMESPACE + "oauth2:clientSecretKey", clientSecretKey,
                EDC_NAMESPACE + "oauth2:tokenUrl", "http://localhost:%s/token".formatted(oauth2server.getPort())
        );

        var assetId = PROVIDER.createOffer(dataSource);

        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("HttpData-PUSH")
                .withDestination(httpDataAddress("http://localhost:" + destinationBackend.getPort() + "/destination"))
                .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

        oauth2server.verify(request("/token").withBody("grant_type=client_credentials&client_secret=clientSecretValue&client_id=clientId"));
        sourceBackend.verify(request("/source").withMethod("GET").withHeader("Authorization", "Bearer token"));
        destinationBackend.verify(request("/destination").withBody(binary("data".getBytes())));

        oauth2server.stop();
        sourceBackend.stop();
        destinationBackend.stop();
    }

    public JsonObject createCallback(String url, boolean transactional, Set<String> events) {
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

    private JsonObject httpDataAddress(String baseUrl) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                .build();
    }

}
