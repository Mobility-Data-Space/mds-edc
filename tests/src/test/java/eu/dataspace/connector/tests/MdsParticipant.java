package eu.dataspace.connector.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.SystemExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Map.entry;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class MdsParticipant extends Participant implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private final LazySupplier<Integer> eventReceiverPort = new LazySupplier<>(Ports::getFreePort);
    private final LazySupplier<URI> stsEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/sts"));
    private final String managementAuthKey = UUID.randomUUID().toString();
    private WireMockServer eventReceiver;
    private EmbeddedRuntime runtime;
    private final BlockingQueue<JsonObject> events = new LinkedBlockingDeque<>();
    private boolean eventReceiverEnabled = true;

    protected MdsParticipant() {

    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (runtime != null) {
            runtime.boot(false);
            if (eventReceiverEnabled) {
                eventReceiver = new WireMockServer(options().port(eventReceiverPort.get()));
                eventReceiver.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
                eventReceiver.addMockServiceRequestListener((request, response) -> {
                    var bodyAsRawBytes = request.getBodyAsString().getBytes();
                    var event = Json.createReader(new ByteArrayInputStream(bodyAsRawBytes)).readObject();
                    events.add(event);
                });
                eventReceiver.start();

            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (runtime != null) {
            runtime.shutdown();
            if (eventReceiverEnabled && eventReceiver != null) {
                eventReceiver.stop();
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
    }

    @Override
    public void afterEach(ExtensionContext context) {
        events.clear();
    }

    public Config getConfiguration() {
        var settings = Map.ofEntries(
                entry("edc.participant.id", id),
                entry("edc.participant.context.id", id),
                entry("web.http.path", "/api"),
                entry("web.http.port", getFreePort() + ""),
                entry("web.http.control.path", "/control"),
                entry("web.http.control.port", getFreePort() + ""),
                entry("web.http.management.path", controlPlaneManagement.get().getPath()),
                entry("web.http.management.port", controlPlaneManagement.get().getPort() + ""),
                entry("web.http.management.auth.type", "tokenbased"),
                entry("web.http.management.auth.key", managementAuthKey),
                entry("web.http.protocol.path", controlPlaneProtocol.get().getPath()),
                entry("web.http.protocol.port", controlPlaneProtocol.get().getPort() + ""),
                entry("web.http.public.path", "/public"),
                entry("web.http.public.port", getFreePort() + ""),
                entry("edc.core.retry.retries.max", "0"),
                entry("edc.transfer.proxy.token.verifier.publickey.alias", "public-key-alias"),
                entry("edc.transfer.proxy.token.signer.privatekey.alias", "private-key-alias"),

                entry("edc.logginghouse.extension.enabled", "false"),

                // DCP settings
                entry("edc.iam.did.web.use.https", "false"),
                entry("edc.iam.issuer.id", id),
                entry("edc.iam.sts.oauth.client.id", id),
                entry("edc.iam.sts.oauth.client.secret.alias", id + "-sts-client-secret"),
                entry("edc.iam.sts.oauth.token.url", stsEndpoint.get() + "/token"),
                entry("edc.iam.credential.revocation.mimetype", "application/json")
        );

        var config = ConfigFactory.fromMap(settings);

        if (eventReceiverEnabled) {
            var eventReceiverSettings = Map.ofEntries(
                    entry("edc.callback.default.events", "contract"),
                    entry("edc.callback.default.uri", "http://localhost:" + eventReceiverPort.get()),
                    entry("edc.callback.default.transactional", "true")
            );

            return config.merge(ConfigFactory.fromMap(eventReceiverSettings));
        }

        return config;
    }

    public EmbeddedRuntime getRuntime() {
        return runtime;
    }

    public ServiceExtension seedVaultKeys() {
        var keyPair = Crypto.generateKeyPair();
        var map = Map.of(
                "private-key-alias", Crypto.encode(keyPair.getPrivate()),
                "public-key-alias", Crypto.encode(keyPair.getPublic())
        );
        return SeedVault.fromMap(c -> map);
    }

    public String createOffer(Map<String, Object> dataAddressProperties) {
        var assetId = UUID.randomUUID().toString();
        createAsset(assetId, Collections.emptyMap(), dataAddressProperties);
        var noConstraintPolicyId = createPolicyDefinition(noConstraintPolicy());
        createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);
        return assetId;
    }

    @Override
    public String createAsset(String assetId, Map<String, Object> properties, Map<String, Object> dataAddressProperties) {
        var baseProperties = createObjectBuilder(properties)
                .add("dct:title", "any")
                .add("mobilitydcatap:mobilityTheme", createObjectBuilder()
                        .add("mobilitydcatap-theme:data-content-category", "OTHER")
                );

        var requestBody = Json.createObjectBuilder()
                .add(CONTEXT, managementContext)
                .add(TYPE, "Asset")
                .add("@id", assetId)
                .add("properties", baseProperties.addAll(createObjectBuilder(properties)))
                .add("dataAddress", Json.createObjectBuilder(dataAddressProperties))
                .build();

        return this.baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when().post("/assets")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath()
                .getString(ID);
    }

    public ValidatableResponse retireAgreement(String agreementId) {
        var body = createObjectBuilder()
                .add(CONTEXT, managementContext)
                .add(TYPE, "AgreementsRetirementEntry")
                .add("agreementId", agreementId)
                .add("reason", "a good reason")
                .build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(body)
                .when()
                .post("/contractagreements/retirements")
                .then()
                .log().ifValidationFails();
    }

    public JsonObject getTransferProcess(String transferProcessId) {
        return baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/transferprocesses/{id}", transferProcessId)
                .then().statusCode(200).extract().body().as(JsonObject.class);
    }

    public JsonObject getContractNegotiation(String id) {
        return baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/contractnegotiations/{id}", id)
                .then().statusCode(200).extract().body().as(JsonObject.class);
    }

    public JsonArray getContractNegotiations(JsonObject query) {
        return baseManagementRequest()
                .contentType(JSON)
                .body(query)
                .when()
                .post("/contractnegotiations/request")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .as(JsonArray.class);
    }

    public JsonArray getContractNegotiationsWith(String counterPartyId) {
        var query = createObjectBuilder()
                .add(CONTEXT, managementContext)
                .add(TYPE, "QuerySpec")
                .add("filterExpression", createObjectBuilder()
                        .add(TYPE, "Criterion")
                        .add("operandLeft", "counterPartyId")
                        .add("operator", "=")
                        .add("operandRight", counterPartyId)
                )
                .build();

        return getContractNegotiations(query);
    }

    public JsonObject getPendingNegotiation(String negotiationId) {
        var query = createObjectBuilder()
                .add(CONTEXT, managementContext)
                .add(TYPE, "QuerySpec")
                .add("filterExpression", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, "Criterion")
                                .add("operandLeft", "pending")
                                .add("operator", "=")
                                .add("operandRight", true)
                        )
                )
                .build();

        return getContractNegotiations(query).stream()
                .map(JsonValue::asJsonObject)
                .filter(it -> it.getString(ID).equals(negotiationId))
                .findAny()
                .orElse(null);
    }

    public JsonArray getTransferProcessesOn(String contractAgreementId) {
        var query = createObjectBuilder()
                .add(CONTEXT, managementContext)
                .add(TYPE, "QuerySpec")
                .add("filterExpression", createObjectBuilder()
                        .add(TYPE, "Criterion")
                        .add("operandLeft", "contractId")
                        .add("operator", "=")
                        .add("operandRight", contractAgreementId)
                )
                .build();

        return getTransferProcesses(query);
    }

    @Override
    public JsonArray getCatalogDatasets(Participant provider) {
        var requestBodyBuilder = Json.createObjectBuilder()
                .add(CONTEXT, managementContext)
                .add(TYPE, "CatalogRequest")
                .add("counterPartyId", provider.getId())
                .add("counterPartyAddress", provider.getProtocolUrl())
                .add("protocol", this.protocol.name());

        var response = this.baseManagementRequest()
                .contentType(ContentType.JSON)
                .when().body(requestBodyBuilder.build())
                .post("/catalog/request")
                .then().log().ifValidationFails()
                .statusCode(200).extract().body().asString();

        try {
            var responseBody = objectMapper.readValue(response, JsonObject.class);
            var catalog = jsonLd.expand(responseBody).orElseThrow((f) -> new EdcException(f.getFailureDetail()));
            return catalog.getJsonArray("http://www.w3.org/ns/dcat#dataset");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public String createAsset(String body) {
        return baseManagementRequest()
                .contentType(JSON)
                .body(body)
                .when().post("/assets")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath()
                .getString(ID);
    }

    public String createContractDefinitionWithManualApproval(String assetId, String policyId) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, managementContext)
                .add(TYPE, "ContractDefinition")
                .add("accessPolicyId", policyId)
                .add("contractPolicyId", policyId)
                .add("assetsSelector", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, "Criterion")
                                .add("operandLeft", EDC_NAMESPACE + "id")
                                .add("operator", "=")
                                .add("operandRight", assetId)
                                .build())
                        .build())
                .add("privateProperties", Json.createObjectBuilder()
                        .add("manualApproval", "true"))
                .build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/contractdefinitions")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().jsonPath().getString(ID);
    }

    public <T> T getService(Class<T> clazz) {
        return runtime.getService(clazz);
    }

    public JsonObject waitForEvent(String eventType) {
        try {
            do {
                var event = events.poll(timeout.getSeconds(), TimeUnit.SECONDS);
                if (event == null) {
                    throw new TimeoutException("No event of type " + eventType + " received");
                }
                if (Objects.equals(event.getString("type"), eventType)) {
                    return event;
                }
            } while (true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MdsParticipant configurationProvider(Supplier<Config> configurationProvider) {
        runtime.configurationProvider(configurationProvider);
        return this;
    }

    public <T extends SystemExtension> MdsParticipant registerServiceExtension(ServiceExtension extension) {
        runtime.registerSystemExtension(ServiceExtension.class, extension);
        return this;
    }

    public static class Builder extends Participant.Builder<MdsParticipant, Builder> {

        public static Builder newInstance() {
            return new Builder(new MdsParticipant());
        }

        protected Builder(MdsParticipant participant) {
            super(participant);
        }

        @Override
        public MdsParticipant build() {
            participant.managementContext = Json.createValue("https://w3id.org/mobility-dataspace/connector/management/v1");
            participant.enrichManagementRequest = request -> request.header("x-api-key", participant.managementAuthKey);
            return super.build();
        }

        public Builder runtime(Function<MdsParticipant, EmbeddedRuntime> runtimeSupplier) {
            participant.runtime = runtimeSupplier.apply(participant);
            return this;
        }

    }

}
