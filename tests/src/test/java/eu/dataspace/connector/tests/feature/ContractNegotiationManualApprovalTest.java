package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.PostgresqlExtension;
import eu.dataspace.connector.tests.SovityDapsExtension;
import eu.dataspace.connector.tests.VaultExtension;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.configuration.Configuration;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.event.Level;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;

public class ContractNegotiationManualApprovalTest {

    private static final MdsParticipant PROVIDER = MdsParticipant.Builder.newInstance()
            .id("provider").name("provider")
            .build();

    private static final MdsParticipant CONSUMER = MdsParticipant.Builder.newInstance()
            .id("consumer").name("consumer")
            .build();

    private static final ClientAndServer providerEventReceiver = ClientAndServer.startClientAndServer(getFreePort());
    private static final ClientAndServer consumerEventReceiver = ClientAndServer.startClientAndServer(getFreePort());

    @RegisterExtension
    @Order(0)
    private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

    @RegisterExtension
    @Order(1)
    private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension(PROVIDER.getName(), CONSUMER.getName());

    @RegisterExtension
    @Order(2)
    private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_EXTENSION = new RuntimePerClassExtension(
            new EmbeddedRuntime("provider", ":launchers:connector-vault-postgresql")
                    .configurationProvider(PROVIDER::getConfiguration)
                    .configurationProvider(() -> DAPS_EXTENSION.dapsConfig("provider"))
                    .configurationProvider(() -> VAULT_EXTENSION.getConfig("provider"))
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                            "edc.callback.default.events", "contract.negotiation",
                            "edc.callback.default.uri", "http://localhost:" + providerEventReceiver.getPort(),
                            "edc.callback.default.transactional", "true"
                    )))
                    .registerSystemExtension(ServiceExtension.class, PROVIDER.seedVaultKeys())
                    .registerSystemExtension(ServiceExtension.class, DAPS_EXTENSION.seedExtension())
                    .configurationProvider(() -> POSTGRES_EXTENSION.getConfig(PROVIDER.getName()))
    );

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_EXTENSION = new RuntimePerClassExtension(
            new EmbeddedRuntime("consumer", ":launchers:connector-vault-postgresql")
                    .configurationProvider(CONSUMER::getConfiguration)
                    .configurationProvider(() -> DAPS_EXTENSION.dapsConfig("consumer"))
                    .configurationProvider(() -> VAULT_EXTENSION.getConfig("consumer"))
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                            "edc.callback.default.events", "contract.negotiation",
                            "edc.callback.default.uri", "http://localhost:" + consumerEventReceiver.getPort(),
                            "edc.callback.default.transactional", "true"
                    )))
                    .registerSystemExtension(ServiceExtension.class, DAPS_EXTENSION.seedExtension())
                    .configurationProvider(() -> POSTGRES_EXTENSION.getConfig(CONSUMER.getName()))
    );

    @BeforeAll
    static void beforeAll() {
        providerEventReceiver.when(request()).respond(HttpResponse.response());
        consumerEventReceiver.when(request()).respond(HttpResponse.response());
    }

    @BeforeEach
    void setUp() {
        providerEventReceiver.reset();
        providerEventReceiver.when(request()).respond(HttpResponse.response());
        consumerEventReceiver.reset();
        consumerEventReceiver.when(request()).respond(HttpResponse.response());
    }

    @Test
    void shouldManuallyApproveNegotiation() {
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost/any"
        );

        var assetId = createOfferWithManualApproval(dataAddressProperties);
        var consumerContractNegotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        await().untilAsserted(() -> {
            assertThat(CONSUMER.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(REQUESTED.name());
        });

        var pending = await().until(PROVIDER::getPendingNegotiations, array -> array.size() == 1).getFirst();
        assertThat(pending.asJsonObject().getString("state")).isEqualTo(REQUESTED.name());

        PROVIDER.baseManagementRequest()
                .post("/v3/contractnegotiations/{id}/approve", pending.asJsonObject().getString(ID))
                .then()
                .statusCode(204);

        await().untilAsserted(() -> {
            assertThat(CONSUMER.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(FINALIZED.name());
        });
    }

    @Test
    void shouldManuallyRejectNegotiation() {
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost/any"
        );

        var assetId = createOfferWithManualApproval(dataAddressProperties);

        var consumerContractNegotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        await().untilAsserted(() -> {
            assertThat(CONSUMER.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(REQUESTED.name());
        });

        var pending = await().until(PROVIDER::getPendingNegotiations, array -> array.size() == 1).getFirst();
        assertThat(pending.asJsonObject().getString("state")).isEqualTo(REQUESTED.name());

        PROVIDER.baseManagementRequest()
                .post("/v3/contractnegotiations/{id}/reject", pending.asJsonObject().getString(ID))
                .then()
                .statusCode(204);

        await().untilAsserted(() -> {
            assertThat(CONSUMER.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(TERMINATED.name());
        });
    }

    @Test
    void shouldManuallyApproveNegotiationWithEvents() {
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost/any"
        );

        var assetId = createOfferWithManualApproval(dataAddressProperties);
        var consumerNegotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        var contractNegotiationRequested = waitForEvent("ContractNegotiationRequested", providerEventReceiver);

        var providerNegotiationId = contractNegotiationRequested.getJsonObject("payload").getString("contractNegotiationId");

        waitForEvent("ContractNegotiationRequested", consumerEventReceiver);
        PROVIDER.baseManagementRequest()
                .post("/v3/contractnegotiations/{id}/approve", providerNegotiationId)
                .then()
                .statusCode(204);

        waitForEvent("ContractNegotiationManuallyApproved", providerEventReceiver);
        var contractNegotiationFinalized = waitForEvent("ContractNegotiationFinalized", consumerEventReceiver);

        assertThat(contractNegotiationFinalized.getJsonObject("payload").getString("contractNegotiationId")).isEqualTo(consumerNegotiationId);
        assertThat(CONSUMER.getContractNegotiationState(consumerNegotiationId)).isEqualTo(FINALIZED.name());
    }

    @Test
    void shouldManuallyRejectNegotiationWithEvents() {
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost/any"
        );

        var assetId = createOfferWithManualApproval(dataAddressProperties);
        var consumerNegotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        var contractNegotiationRequested = waitForEvent("ContractNegotiationRequested", providerEventReceiver);

        var providerNegotiationId = contractNegotiationRequested.getJsonObject("payload").getString("contractNegotiationId");

        waitForEvent("ContractNegotiationRequested", consumerEventReceiver);
        PROVIDER.baseManagementRequest()
                .post("/v3/contractnegotiations/{id}/reject", providerNegotiationId)
                .then()
                .statusCode(204);

        waitForEvent("ContractNegotiationManuallyRejected", providerEventReceiver);
        var contractNegotiationFinalized = waitForEvent("ContractNegotiationTerminated", consumerEventReceiver);

        assertThat(contractNegotiationFinalized.getJsonObject("payload").getString("contractNegotiationId")).isEqualTo(consumerNegotiationId);
        assertThat(CONSUMER.getContractNegotiationState(consumerNegotiationId)).isEqualTo(TERMINATED.name());
    }

    private JsonObject waitForEvent(String eventType, ClientAndServer eventReceiver) {
        var request = request().withBody(json(Map.entry("type", eventType)));
        return await()
                .until(
                        () -> Arrays.stream(eventReceiver.retrieveRecordedRequests(request))
                                .findFirst()
                                .map(HttpRequest::getBodyAsRawBytes)
                                .map(ByteArrayInputStream::new)
                                .map(Json::createReader)
                                .map(JsonReader::readObject)
                                .orElse(null),
                        Objects::nonNull
                );
    }

    private String createOfferWithManualApproval(Map<String, Object> dataAddressProperties) {
        var assetId = UUID.randomUUID().toString();
        PROVIDER.createAsset(assetId, Map.of("http://w3id.org/mds#dataCategory", "any"), dataAddressProperties);
        var noConstraintPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());

        var requestBody = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "ContractDefinition")
                .add(EDC_NAMESPACE + "accessPolicyId", noConstraintPolicyId)
                .add(EDC_NAMESPACE + "contractPolicyId", noConstraintPolicyId)
                .add(EDC_NAMESPACE + "assetsSelector", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, "Criterion")
                                .add(EDC_NAMESPACE + "operandLeft", EDC_NAMESPACE + "id")
                                .add(EDC_NAMESPACE + "operator", "=")
                                .add(EDC_NAMESPACE + "operandRight", assetId)
                                .build())
                        .build())
                .add(EDC_NAMESPACE + "privateProperties", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "manualApproval", "true"))
                .build();

        PROVIDER.baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v3/contractdefinitions")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        return assetId;
    }

}
