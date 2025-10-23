package eu.dataspace.connector.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
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
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
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
                entry("web.http.version.path", "/version"),
                entry("web.http.version.port", getFreePort() + ""),
                entry("web.http.public.path", "/public"),
                entry("web.http.public.port", getFreePort() + ""),
                entry("edc.dsp.callback.address", getProtocolUrl()),
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
                .add("@context", Json.createObjectBuilder()
                        .add("@vocab", "https://w3id.org/edc/v0.0.1/ns/")
                        .add("dct", "http://purl.org/dc/terms/")
                        .add("mobilitydcatap", "https://w3id.org/mobilitydcat-ap/")
                        .add("mobilitydcatap-theme", "https://w3id.org/mobilitydcat-ap/mobility-theme/")
                )
                .add("@id", assetId)
                .add("properties", baseProperties.addAll(createObjectBuilder(properties)))
                .add("dataAddress", Json.createObjectBuilder(dataAddressProperties))
                .build();

        return this.baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when().post("/v3/assets")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath()
                .getString(ID);
    }

    public ValidatableResponse retireAgreement(String agreementId) {
        var body = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("@vocab", EDC_NAMESPACE))
                .add(TYPE, "AgreementsRetirementEntry")
                .add("agreementId", agreementId)
                .add("reason", "a good reason")
                .build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(body)
                .when()
                .post("/v3/contractagreements/retirements")
                .then()
                .log().ifValidationFails();
    }

    public JsonObject getTransferProcess(String transferProcessId) {
        return baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/v3/transferprocesses/{id}", transferProcessId)
                .then().statusCode(200).extract().body().as(JsonObject.class);
    }

    public JsonObject getContractNegotiation(String id) {
        return baseManagementRequest()
                .contentType(JSON)
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

    public JsonObject getPendingNegotiation(String negotiationId) {
        return getContractNegotiations(createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add("filterExpression", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("operandLeft", "pending")
                                .add("operator", "=")
                                .add("operandRight", true)
                        )
                )
                .build()).stream()
                .map(JsonValue::asJsonObject)
                .filter(it -> it.getString(ID).equals(negotiationId))
                .findAny()
                .orElse(null);
    }

    public JsonObject createEdpsJob(String assetId, String edpsContractAgreementId) {
        return baseManagementRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add("contractId", edpsContractAgreementId).build())
                .when()
                .post("/edp/edps/{assetId}/jobs", assetId)
                .then().statusCode(200).extract().body().as(JsonObject.class);
    }

    public JsonObject getEdpsResult(String assetId, String jobId, String edpsContractAgreementId) {
        return baseManagementRequest()
                .contentType(JSON)
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

    @Override
    public JsonArray getCatalogDatasets(Participant provider) {
        var requestBodyBuilder = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", "https://w3id.org/edc/v0.0.1/ns/"))
                .add("@type", "CatalogRequest")
                .add("counterPartyId", provider.getId())
                .add("counterPartyAddress", provider.getProtocolUrl())
                .add("protocol", this.protocol);

        var response = this.baseManagementRequest()
                .contentType(ContentType.JSON)
                .when().body(requestBodyBuilder.build())
                .post("/v3/catalog/request")
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
                .when().post("/v3/assets")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath()
                .getString(ID);
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

    public ValidatableResponse getCatalog(MdsParticipant provider) {
        var requestBodyBuilder = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", "https://w3id.org/edc/v0.0.1/ns/"))
                .add("@type", "CatalogRequest")
                .add("counterPartyId", provider.id)
                .add("counterPartyAddress", provider.getProtocolUrl())
                .add("protocol", protocol);

        return baseManagementRequest()
                .contentType(ContentType.JSON)
                .when()
                .body(requestBodyBuilder.build())
                .post("/v3/catalog/request")
                .then()
                .log().ifValidationFails();
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
            participant.enrichManagementRequest = request -> request.header("x-api-key", participant.managementAuthKey);
            return super.build();
        }

        public Builder runtime(Function<MdsParticipant, EmbeddedRuntime> runtimeSupplier) {
            participant.runtime = runtimeSupplier.apply(participant);
            return this;
        }

        public Builder eventReceiver(boolean enabled) {
            participant.eventReceiverEnabled = enabled;
            return this;
        }
    }

    private class EdcEventSubscriber implements ServeEventListener {
        @Override
        public String getName() {
            return "events";
        }

        @Override
        public void onEvent(RequestPhase requestPhase, ServeEvent serveEvent, Parameters parameters) {
            var bodyAsRawBytes = serveEvent.getRequest().getBody();
            var event = Json.createReader(new ByteArrayInputStream(bodyAsRawBytes)).readObject();
            events.add(event);
        }
    }
}
