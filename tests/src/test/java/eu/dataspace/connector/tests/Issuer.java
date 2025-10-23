package eu.dataspace.connector.tests;

import io.restassured.http.ContentType;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.dataspace.connector.tests.Crypto.generateEcKey;
import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class Issuer implements BeforeAllCallback, AfterAllCallback {

    private final EmbeddedRuntime runtime;
    private final LazySupplier<URI> didEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/"));
    private final LazySupplier<URI> issuanceEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/issuance"));
    private final LazySupplier<URI> issueradminEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/issueradmin"));
    private final LazySupplier<String> did = new LazySupplier<>(() -> "did:web:localhost%%3A%d:issuer".formatted(didEndpoint.get().getPort()));
    private CreateParticipantContextResponse createParticipantContextResponse;

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
                entry("web.http.issuance.port", String.valueOf(issuanceEndpoint.get().getPort())),
                entry("web.http.issuance.path", issuanceEndpoint.get().getPath()),
                entry("web.http.issueradmin.port", String.valueOf(issueradminEndpoint.get().getPort())),
                entry("web.http.issueradmin.path", issueradminEndpoint.get().getPath()),
                entry("edc.iam.did.web.use.https", "false"),
                entry("edc.issuer.statuslist.signing.key.alias", "%s-privatekey-alias".formatted(did.get()))
        )));
        runtime.registerSystemExtension(ServiceExtension.class, new CreateParticipantContextExtension(did));
        runtime.registerSystemExtension(ServiceExtension.class, new ServiceExtension() {

            @Inject
            private AttestationSourceFactoryRegistry attestationSourceFactoryRegistry;

            @Inject
            private AttestationDefinitionValidatorRegistry attestationDefinitionValidatorRegistry;

            @Override
            public void initialize(ServiceExtensionContext context) {
                // TODO: use DatabaseAttestation (first we need a db though :') )
                // TODO: see https://github.com/eclipse-edc/IdentityHub/issues/838
                attestationSourceFactoryRegistry.registerFactory("demo", new AttestationSourceFactory() {
                    @Override
                    public AttestationSource createSource(AttestationDefinition definition) {
                        return new AttestationSource() {
                            @Override
                            public Result<Map<String, Object>> execute(AttestationContext context) {
                                // TODO: what?
                                return Result.success(Map.of("onboarding", Map.of("signedDocuments", true), "participant", Map.of("name", "Alice")));
                            }
                        };
                    }
                });

                attestationDefinitionValidatorRegistry.registerValidator("demo", _ -> ValidationResult.success());
            }
        });
        runtime.boot();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        runtime.shutdown();
    }

    public LazySupplier<String> did() {
        return did;
    }

    public void registerHolder(String holderDid) {
        given()
                .baseUri(issueradminEndpoint.get().toString())
                .header("x-api-key", createParticipantContextResponse.apiKey())
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "holderId", holderDid,
                        "did", holderDid,
                        "name", holderDid
                ))
                .post("/v1alpha/participants/{participantContextId}/holders", Base64.getEncoder().encodeToString(did.get().getBytes()))
                .then()
                .statusCode(201);
    }

    public void registerAttestationAndCredentialDefinition() {
        given()
                .baseUri(issueradminEndpoint.get().toString())
                .header("x-api-key", createParticipantContextResponse.apiKey())
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "id", "attestation-id",
                        "attestationType", "demo",
                        "configuration", Map.of(
                                "credentialType", "aaa", // TODO: what?
                                "outputClaim", "participant" // TODO: what?
                        )
                ))
                .post("/v1alpha/participants/{participantContextId}/attestations", Base64.getEncoder().encodeToString(did.get().getBytes()))
                .then()
                .statusCode(201);

        given()
                .baseUri(issueradminEndpoint.get().toString())
                .header("x-api-key", createParticipantContextResponse.apiKey())
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
                                    entry("input", "participant.name"),
                                    entry("output", "credentialSubject.name"),
                                    entry("required", "true")
                                )
                        )),
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
                .post("/v1alpha/participants/{participantContextId}/credentialdefinitions", Base64.getEncoder().encodeToString(did.get().getBytes()))
                .then()
                .log().ifValidationFails()
                .statusCode(201);
    }

    public class CreateParticipantContextExtension implements ServiceExtension {

        @Inject
        private Vault vault;
        @Inject
        private ParticipantContextService participantContextService;

        private final LazySupplier<String> did;

        public CreateParticipantContextExtension(LazySupplier<String> did) {
            this.did = did;
        }

        @Override
        public void prepare() {
            // TODO do it through API call

            var participantKey = generateEcKey("%s#key1".formatted(did.get()));

            // STS secret
            vault.storeSecret(did.get() + "-sts-client-secret", did.get());

            var privateKeyAlias = "%s-privatekey-alias".formatted(did.get());
            vault.storeSecret(privateKeyAlias, participantKey.toJSONString());
            var manifest = ParticipantManifest.Builder.newInstance()
                    .participantId(did.get())
                    .did(did.get())
                    .active(true)
                    .serviceEndpoint(new Service(
                            UUID.randomUUID().toString(), "IssuerService",
                            "%s/v1alpha/participants/%s".formatted(issuanceEndpoint.get().toString(), Base64.getEncoder().encodeToString(did.get().getBytes())))
                    )
                    .key(KeyDescriptor.Builder.newInstance()
                            .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                            .privateKeyAlias(privateKeyAlias)
                            .keyId(participantKey.getKeyID())
                            .build())
                    .build();

            createParticipantContextResponse = participantContextService.createParticipantContext(manifest).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        }
    }
}
