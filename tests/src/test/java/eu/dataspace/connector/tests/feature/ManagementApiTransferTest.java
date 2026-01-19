package eu.dataspace.connector.tests.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.atomicConstraint;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.policy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

class ManagementApiTransferTest {

    @Nested
    class InMemory extends Tests {

        @RegisterExtension
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.inMemory("provider");

        @RegisterExtension
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.inMemory("consumer");

        protected InMemory() {
            super(PROVIDER, CONSUMER);
        }
    }

    @Nested
    class HashicorpVaultPostgresql extends Tests {

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
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        protected HashicorpVaultPostgresql() {
            super(PROVIDER, CONSUMER);
        }

        @Test
        void shouldNotOfferAssets_whenPolicyIsConstrained() {
            var assetId = UUID.randomUUID().toString();
            PROVIDER.createAsset(assetId, Collections.emptyMap(), Map.of("type", "HttpData", "baseUrl", "http://any"));
            var permission = createObjectBuilder()
                    .add("action", "use")
                    .add("constraint", atomicConstraint("REFERRING_CONNECTOR", "eq", "not-consumer"))
                    .build();
            var policyId = PROVIDER.createPolicyDefinition(policy(List.of(permission)));
            PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), policyId, policyId);

            var catalogDatasets = CONSUMER.getCatalogDatasets(PROVIDER);

            assertThat(catalogDatasets).noneSatisfy(dataset ->
                    assertThat(dataset.asJsonObject().getString(ID)).isEqualTo(assetId)
            );
        }
    }

    abstract static class Tests {

        private final MdsParticipant provider;
        private final MdsParticipant consumer;

        protected Tests(MdsParticipant provider, MdsParticipant consumer) {
            this.provider = provider;
            this.consumer = consumer;
        }

        @Test
        void shouldSupportHttpPushTransfer() {
            var providerDataSource = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            providerDataSource.start();
            providerDataSource.stubFor(WireMock.any(WireMock.urlEqualTo("/source"))
                .willReturn(WireMock.aResponse().withBody("data")));
            var consumerDataDestination = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            consumerDataDestination.start();
            consumerDataDestination.stubFor(WireMock.any(WireMock.urlEqualTo("/destination"))
                .willReturn(WireMock.aResponse()));
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
                providerDataSource.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/source")));
                consumerDataDestination.verify(WireMock.anyRequestedFor(WireMock.urlEqualTo("/destination"))
                    .withRequestBody(WireMock.binaryEqualTo("data".getBytes())));
            });

            consumerDataDestination.stop();
            providerDataSource.stop();
        }

        @Test
        void shouldSupportHttpPullTransfer() throws IOException {
            var providerDataSource = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            providerDataSource.start();
            providerDataSource.stubFor(WireMock.any(WireMock.urlEqualTo("/source"))
                .willReturn(WireMock.aResponse().withBody("data")));
            var consumerEdrReceiver = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            consumerEdrReceiver.start();
            consumerEdrReceiver.stubFor(WireMock.any(WireMock.urlEqualTo("/edr"))
                .willReturn(WireMock.aResponse()));
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

            var edrRequests = await().until(() -> consumerEdrReceiver.getAllServeEvents().stream()
                .filter(e -> e.getRequest().getUrl().equals("/edr"))
                .toList(), it -> !it.isEmpty());

            var edr = new ObjectMapper().readTree(edrRequests.get(0).getRequest().getBody()).get("payload").get("dataAddress").get("properties");

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
            var sourceBackend = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            sourceBackend.start();
            sourceBackend.stubFor(WireMock.any(WireMock.urlEqualTo("/source"))
                .willReturn(WireMock.aResponse().withBody("data")));
            var oauth2server = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            oauth2server.start();
            oauth2server.stubFor(WireMock.any(WireMock.urlEqualTo("/token"))
                .willReturn(WireMock.aResponse().withBody("{\"access_token\":\"token\"}")));
            var destinationBackend = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            destinationBackend.start();
            destinationBackend.stubFor(WireMock.any(WireMock.urlEqualTo("/destination"))
                .willReturn(WireMock.aResponse()));

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

            oauth2server.verify(WireMock.anyRequestedFor(WireMock.urlEqualTo("/token"))
                .withRequestBody(WireMock.equalTo("grant_type=client_credentials&client_secret=clientSecretValue&client_id=clientId")));
            sourceBackend.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/source"))
                .withHeader("Authorization", WireMock.equalTo("Bearer token")));
            destinationBackend.verify(WireMock.anyRequestedFor(WireMock.urlEqualTo("/destination"))
                .withRequestBody(WireMock.binaryEqualTo("data".getBytes())));

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

}
