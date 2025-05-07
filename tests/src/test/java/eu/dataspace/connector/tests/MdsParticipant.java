package eu.dataspace.connector.tests;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static eu.dataspace.connector.tests.Crypto.encode;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Map.entry;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;

public class MdsParticipant extends Participant implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private final LazySupplier<Integer> eventReceiverPort = new LazySupplier<>(Ports::getFreePort);
    private ClientAndServer eventReceiver;
    private EmbeddedRuntime runtime;

    private MdsParticipant() {

    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (runtime != null) {
            eventReceiver = ClientAndServer.startClientAndServer(eventReceiverPort.get());
            runtime.boot(false);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (runtime != null) {
            runtime.shutdown();
            eventReceiver.stop();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        eventReceiver.reset();
        eventReceiver.when(request()).respond(HttpResponse.response());
    }

    public Config getConfiguration() {
        var settings = Map.ofEntries(
                entry("edc.participant.id", id),
                entry("web.http.path", "/api"),
                entry("web.http.port", getFreePort() + ""),
                entry("web.http.control.path", "/control"),
                entry("web.http.control.port", getFreePort() + ""),
                entry("web.http.management.path", controlPlaneManagement.get().getPath()),
                entry("web.http.management.port", controlPlaneManagement.get().getPort() + ""),
                entry("web.http.protocol.path", controlPlaneProtocol.get().getPath()),
                entry("web.http.protocol.port", controlPlaneProtocol.get().getPort() + ""),
                entry("web.http.version.path", "/version"),
                entry("web.http.version.port", getFreePort() + ""),
                entry("web.http.public.path", "/public"),
                entry("web.http.public.port", getFreePort() + ""),
                entry("edc.transfer.proxy.token.verifier.publickey.alias", "public-key-alias"),
                entry("edc.transfer.proxy.token.signer.privatekey.alias", "private-key-alias"),

                entry("edc.callback.default.events", "contract.negotiation"),
                entry("edc.callback.default.uri", "http://localhost:" + eventReceiverPort.get()),
                entry("edc.callback.default.transactional", "true")
        );

        return ConfigFactory.fromMap(settings);
    }

    public ServiceExtension seedVaultKeys() {
        return new SeedVaultKeys();
    }

    public String createOffer(Map<String, Object> dataAddressProperties) {
        var assetId = UUID.randomUUID().toString();
        createAsset(assetId, Map.of("http://w3id.org/mds#dataCategory", "any"), dataAddressProperties);
        var noConstraintPolicyId = createPolicyDefinition(noConstraintPolicy());
        createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);
        return assetId;
    }

    public ValidatableResponse retireProviderAgreement(String agreementId) {
        var body = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "AgreementsRetirementEntry")
                .add(EDC_NAMESPACE + "agreementId", agreementId)
                .add(TX_NAMESPACE + "reason", "a good reason")
                .build();
        return baseManagementRequest()
                .contentType(JSON)
                .body(body)
                .when()
                .post("/v3.1alpha/retireagreements")
                .then();
    }

    public JsonObject getTransferProcess(String transferProcessId) {
        return baseManagementRequest()
                .contentType(ContentType.JSON)
                .when()
                .get("/v3/transferprocesses/{id}", transferProcessId)
                .then().statusCode(200).extract().body().as(JsonObject.class);
    }

    public JsonObject getContractNegotiation(String id) {
        return baseManagementRequest()
                .contentType(ContentType.JSON)
                .when()
                .get("/v3/contractnegotiations/{id}", id)
                .then().statusCode(200).extract().body().as(JsonObject.class);
    }

    public JsonArray getContractNegotiations(JsonObject query) {
        return baseManagementRequest()
                .contentType(JSON)
                .body(query)
                .when()
                .post("/v3/contractnegotiations/request")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .as(JsonArray.class);
    }

    public JsonArray getPendingNegotiations() {
        return getContractNegotiations(createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add("filterExpression", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("operandLeft", "pending")
                                .add("operator", "=")
                                .add("operandRight", true)
                        )
                )
                .build());
    }

    public JsonObject createEdpsJob(String assetId, String edpsContractAgreementId) {
        return baseManagementRequest()
                .contentType(ContentType.JSON)
                .body(createObjectBuilder().add("contractId", edpsContractAgreementId).build())
                .when()
                .post("/edp/edps/{assetId}/jobs", assetId)
                .then().statusCode(200).extract().body().as(JsonObject.class);
    }

    public JsonObject getEdpsResult(String assetId, String jobId, String edpsContractAgreementId) {
        return baseManagementRequest()
                .contentType(ContentType.JSON)
                .body(createObjectBuilder().add("contractId", edpsContractAgreementId).build())
                .when()
                .post("/edp/edps/{assetId}/jobs/{jobId}/result", assetId, jobId)
                .then().statusCode(200).extract().body().as(JsonObject.class);
    }

    public ValidatableResponse publishDassen(String resultAssetId, String daseenContractAgreementId) {
        return baseManagementRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add("contractId", daseenContractAgreementId).build())
                .when()
                .post("/edp/daseen/{resultAssetId}", resultAssetId)
                .then();
    }

    public <T> T getService(Class<T> clazz) {
        return runtime.getService(clazz);
    }

    public JsonObject waitForEvent(String eventType) {
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

    public static class Builder extends Participant.Builder<MdsParticipant, Builder> {

        public static Builder newInstance() {
            return new Builder(new MdsParticipant());
        }

        protected Builder(MdsParticipant participant) {
            super(participant);
        }

        public Builder runtime(Function<MdsParticipant, EmbeddedRuntime> runtimeSupplier) {
            participant.runtime = runtimeSupplier.apply(participant);
            return this;
        }
    }

    private static class SeedVaultKeys implements ServiceExtension {

        @Inject
        private Vault vault;

        @Override
        public void initialize(ServiceExtensionContext context) {
            var keyPair = Crypto.generateKeyPair();
            vault.storeSecret("private-key-alias", encode(keyPair.getPrivate()));
            vault.storeSecret("public-key-alias", encode(keyPair.getPublic()));
        }

    }
}
