package eu.dataspace.connector.tests;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.dataspace.connector.tests.Crypto.generateEcKey;
import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class Issuer implements BeforeAllCallback, AfterAllCallback {

    private static final String SUPER_USER = "super-user";
    private static final String SUPER_USER_API_KEY = Base64.getEncoder().encodeToString(SUPER_USER.getBytes()) + "." + UUID.randomUUID();

    private final EmbeddedRuntime runtime;
    private final LazySupplier<URI> didEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/"));
    private final LazySupplier<URI> issuanceEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/issuance"));
    private final LazySupplier<URI> issueradminEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/issueradmin"));
    private final LazySupplier<URI> identityEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/identity"));
    private final LazySupplier<String> did = new LazySupplier<>(() -> "did:web:localhost%%3A%d:issuer".formatted(didEndpoint.get().getPort()));

    private String issuerParticipantApiKey;

    public Issuer(EmbeddedRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        runtime.configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                entry("web.http.port", String.valueOf(getFreePort())),
                entry("web.http.path", "/api"),
                entry("web.http.version.port", String.valueOf(getFreePort())),
                entry("web.http.version.path", "/version"),
                entry("web.http.did.port", String.valueOf(didEndpoint.get().getPort())),
                entry("web.http.did.path", didEndpoint.get().getPath()),
                entry("web.http.identity.port", String.valueOf(identityEndpoint.get().getPort())),
                entry("web.http.identity.path", identityEndpoint.get().getPath()),
                entry("web.http.issuance.port", String.valueOf(issuanceEndpoint.get().getPort())),
                entry("web.http.issuance.path", issuanceEndpoint.get().getPath()),
                entry("web.http.issueradmin.port", String.valueOf(issueradminEndpoint.get().getPort())),
                entry("web.http.issueradmin.path", issueradminEndpoint.get().getPath()),
                entry("edc.identityhub.superuser.id", SUPER_USER),
                entry("edc.identityhub.superuser.api.key", SUPER_USER_API_KEY),
                entry("edc.iam.did.web.use.https", "false"),
                entry("edc.issuer.statuslist.signing.key.alias", "%s-privatekey-alias".formatted(did.get()))
        )));

        runtime.boot();

        registerIssuerParticipantContext();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        runtime.shutdown();
    }

    public LazySupplier<String> did() {
        return did;
    }

    public void registerHolder(String holderDid, String name) {
        given()
                .baseUri(issueradminEndpoint.get().toString())
                .header("x-api-key", issuerParticipantApiKey)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "holderId", holderDid,
                        "did", holderDid,
                        "name", name,
                        "properties", Map.of(
                                "participantName", name,
                                "membershipType", "standard member",
                                "membershipStartDate", Instant.now().getEpochSecond()
                        )
                ))
                .post("/v1alpha/participants/{participantContextId}/holders", Base64.getEncoder().encodeToString(did.get().getBytes()))
                .then()
                .log().ifValidationFails()
                .statusCode(201);
    }

    public void registerAttestationAndCredentialDefinition() {
        given()
                .baseUri(issueradminEndpoint.get().toString())
                .header("x-api-key", issuerParticipantApiKey)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "id", "attestation-id",
                        "attestationType", "holder",
                        "configuration", emptyMap()
                ))
                .post("/v1alpha/participants/{participantContextId}/attestations", encodeToString(did.get()))
                .then()
                .log().ifValidationFails()
                .statusCode(201);

        given()
                .baseUri(issueradminEndpoint.get().toString())
                .header("x-api-key", issuerParticipantApiKey)
                .contentType(ContentType.JSON)
                .body(Map.ofEntries(
                        entry("id", "membershipCredential-id"),
                        entry("jsonSchema", "{}"),
                        entry("jsonSchemaUrl", "https://example.com/schema"),
                        entry("credentialType", "MembershipCredential"),
                        entry("attestations", List.of("attestation-id")),
                        entry("validity", Duration.ofDays(365).toSeconds()),
                        entry("mappings", List.of(
                                Map.ofEntries(
                                        entry("input", "participantName"),
                                        entry("output", "credentialSubject.name"),
                                        entry("required", "true")
                                )
                        )),
                        // TODO: enable rules?
//                        entry("rules", List.of(
//                                Map.ofEntries(
//                                    entry("type", "expression"),
//                                    entry("expression", Map.ofEntries(
//                                            entry("claim", "onboarding.signedDocuments"),
//                                            entry("operator", "eq"),
//                                            entry("value", true)
//                                    )))
//                        )),
                        entry("format", "VC1_0_JWT")
                ))
                .post("/v1alpha/participants/{participantContextId}/credentialdefinitions", encodeToString(did.get()))
                .then()
                .log().ifValidationFails()
                .statusCode(201);
    }

    private void registerIssuerParticipantContext() {
        var participantKey = generateEcKey("%s#key1".formatted(did.get()));

        var privateKeyAlias = "%s-privatekey-alias".formatted(did.get());
        runtime.getService(Vault.class).storeSecret(privateKeyAlias, participantKey.toJSONString());

        issuerParticipantApiKey = given()
                .baseUri(identityEndpoint.get().toString())
                .header("x-api-key", SUPER_USER_API_KEY)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "participantContextId", did.get(),
                        "did", did.get(),
                        "active", "true",
                        "serviceEndpoint", Map.of(
                                "id", UUID.randomUUID().toString(),
                                "type", "IssuerService",
                                "serviceEndpoint", "%s/v1alpha/participants/%s".formatted(
                                        issuanceEndpoint.get().toString(),
                                        encodeToString(did.get())
                                )
                        ),
                        "key", Map.of(
                                "keyId", participantKey.getKeyID(),
                                "privateKeyAlias", privateKeyAlias,
                                "publicKeyJwk", participantKey.toPublicJWK().toJSONObject()
                        )
                ))
                .post("/v1alpha/participants")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body().jsonPath().getString("apiKey");
    }

    public ValidatableResponse revokeCredentials(String participantContextId) {
        var body = given()
                .baseUri(issueradminEndpoint.get().toString())
                .header("x-api-key", issuerParticipantApiKey)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "filterExpression", List.of(
                                Map.of(
                                        "operandLeft", "holderId",
                                        "operator", "=",
                                        "operandRight", participantContextId
                                )
                        )
                ))
                .post("/v1alpha/participants/{participantContextId}/credentials/query", encodeToString(did.get()))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body();
        var soasdsa = body.asString();
        System.out.println(soasdsa);
        var credentialId = body.jsonPath().getString("[0].id");

        return given()
                .baseUri(issueradminEndpoint.get().toString())
                .header("x-api-key", issuerParticipantApiKey)
                .contentType(ContentType.JSON)
                .post("/v1alpha/participants/{participantContextId}/credentials/{credentialId}/revoke", encodeToString(did.get()), credentialId)
                .then()
                .log().ifValidationFails();
    }

    private String encodeToString(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
