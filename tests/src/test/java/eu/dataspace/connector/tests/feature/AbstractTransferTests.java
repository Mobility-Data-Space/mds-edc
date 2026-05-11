package eu.dataspace.connector.tests.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import eu.dataspace.connector.tests.MdsParticipant;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.requestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

abstract class AbstractTransferTests {

    private final MdsParticipant provider;
    private final MdsParticipant consumer;

    protected AbstractTransferTests(MdsParticipant provider, MdsParticipant consumer) {
        this.provider = provider;
        this.consumer = consumer;
    }

    @Test
    void shouldSupportHttpPushTransfer() {
        var providerDataSource = new WireMockServer(Ports.getFreePort());
        providerDataSource.stubFor(get(urlPathEqualTo("/source")).willReturn(ok("data")));
        providerDataSource.start();
        var consumerDataDestination = new WireMockServer(Ports.getFreePort());
        consumerDataDestination.stubFor(post(urlPathEqualTo("/destination")).willReturn(ok()));
        consumerDataDestination.start();
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(providerDataSource.port())
        );

        var assetId = provider.createOffer(dataAddressProperties);

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("HttpData-PUSH")
                .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.port() + "/destination"))
                .execute();

        consumer.awaitTransferToBeInState(transferProcessId, COMPLETED);

        await().untilAsserted(() -> {
            providerDataSource.verify(anyRequestedFor(urlPathEqualTo("/source")));
            consumerDataDestination.verify(anyRequestedFor(urlPathEqualTo("/destination")).withRequestBody(equalTo("data")));
        });

        consumerDataDestination.stop();
        providerDataSource.stop();
    }

    @Test
    void shouldSupportHttpPullTransfer() throws IOException {
        var providerDataSource = new WireMockServer(Ports.getFreePort());
        providerDataSource.stubFor(get(urlPathEqualTo("/source")).willReturn(ok("data")));
        providerDataSource.start();
        var consumerEdrReceiver = new WireMockServer(Ports.getFreePort());
        consumerEdrReceiver.stubFor(post(urlPathEqualTo("/edr")).willReturn(ok()));
        consumerEdrReceiver.start();
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(providerDataSource.port())
        );

        var assetId = provider.createOffer(dataAddressProperties);

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("HttpData-PULL")
                .withCallbacks(Json.createArrayBuilder()
                        .add(createCallback("http://localhost:%s/edr".formatted(consumerEdrReceiver.port()), true, Set.of("transfer.process.started")))
                        .build())
                .execute();

        consumer.awaitTransferToBeInState(transferProcessId, STARTED);

        var edrRequests = await().until(() -> consumerEdrReceiver.findRequestsMatching(requestedFor("POST", urlPathEqualTo("/edr")).build()), it -> !it.getRequests().isEmpty());

        var edr = new ObjectMapper().readTree(edrRequests.getRequests().get(0).getBody()).get("payload").get("dataAddress").get("properties");

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
        providerDataSource.stop();
    }

    @Test
    void shouldSupportOauth2OnProviderSide() {
        var sourceBackend = new WireMockServer(Ports.getFreePort());
        sourceBackend.stubFor(get(urlPathEqualTo("/source")).willReturn(ok("data")));
        sourceBackend.start();
        var oauth2server = new WireMockServer(Ports.getFreePort());
        oauth2server.stubFor(post(urlPathEqualTo("/token")).willReturn(jsonResponse(Map.of("access_token", "token"), 200)));
        oauth2server.start();
        var destinationBackend = new WireMockServer(Ports.getFreePort());
        destinationBackend.stubFor(post(urlPathEqualTo("/destination")).willReturn(ok()));
        destinationBackend.start();

        var clientSecretKey = UUID.randomUUID().toString();
        provider.getService(Vault.class).storeSecret(clientSecretKey, "clientSecretValue");

        Map<String, Object> dataSource = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(sourceBackend.port()),
                EDC_NAMESPACE + "oauth2:clientId", "clientId",
                EDC_NAMESPACE + "oauth2:clientSecretKey", clientSecretKey,
                EDC_NAMESPACE + "oauth2:tokenUrl", "http://localhost:%s/token".formatted(oauth2server.port())
        );

        var assetId = provider.createOffer(dataSource);

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("HttpData-PUSH")
                .withDestination(httpDataAddress("http://localhost:" + destinationBackend.port() + "/destination"))
                .execute();

        consumer.awaitTransferToBeInState(transferProcessId, COMPLETED);

        oauth2server.verify(anyRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(equalTo("grant_type=client_credentials&client_secret=clientSecretValue&client_id=clientId")));
        sourceBackend.verify(anyRequestedFor(urlPathEqualTo("/source")).withHeader("Authorization", equalTo("Bearer token")));
        destinationBackend.verify(anyRequestedFor(urlPathEqualTo("/destination")).withRequestBody(equalTo("data")));

        oauth2server.stop();
        sourceBackend.stop();
        destinationBackend.stop();
    }

    JsonObject createCallback(String url, boolean transactional, Set<String> events) {
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

    JsonObject httpDataAddress(String baseUrl) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                .build();
    }
}
