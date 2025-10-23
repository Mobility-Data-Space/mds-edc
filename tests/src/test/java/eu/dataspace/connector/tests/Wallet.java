package eu.dataspace.connector.tests;

import io.restassured.http.ContentType;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.dataspace.connector.tests.Crypto.generateEcKey;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Map.entry;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.is;

public class Wallet implements BeforeAllCallback, AfterAllCallback {

    private static final String SUPER_USER = "super-user";
    private static final String SUPER_USER_API_KEY = Base64.getEncoder().encodeToString(SUPER_USER.getBytes()) + "." + UUID.randomUUID();

    private final EmbeddedRuntime runtime;
    private final String[] participants;
    private final Map<String, WalletParticipantContext> participantContexts = new HashMap<>();
    private final LazySupplier<URI> stsEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/sts"));
    private final LazySupplier<URI> didEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/"));
    private final LazySupplier<URI> credentialsEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/credentials"));
    private final LazySupplier<URI> identityEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/identity"));

    public Wallet(EmbeddedRuntime runtime, String[] participants) {
        this.runtime = runtime;
        this.participants = participants;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        runtime.configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                entry("web.http.credentials.port", String.valueOf(credentialsEndpoint.get().getPort())),
                entry("web.http.credentials.path", credentialsEndpoint.get().getPath()),
                entry("web.http.did.port", String.valueOf(didEndpoint.get().getPort())),
                entry("web.http.did.path", didEndpoint.get().getPath()),
                entry("web.http.identity.port", String.valueOf(identityEndpoint.get().getPort())),
                entry("web.http.identity.path", identityEndpoint.get().getPath()),
                entry("web.http.sts.port", String.valueOf(stsEndpoint.get().getPort())),
                entry("web.http.sts.path", stsEndpoint.get().getPath()),
                entry("edc.identityhub.superuser.id", SUPER_USER),
                entry("edc.identityhub.superuser.api.key", SUPER_USER_API_KEY),
                entry("edc.iam.did.web.use.https", "false")
        )));

        runtime.boot();

        for (var participant : participants) {
            var did = didFor(participant);
            participantContexts.put(did.get(), registerParticipantContext(did));
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        runtime.shutdown();
    }

    public void requestCredentialIssuance(String participantDid, String issuerDid) {
        var holderPid = UUID.randomUUID().toString();
        var requestCredentialIssuance = Map.of(
                "holderPid", holderPid,
                "issuerDid", issuerDid,
                "credentials", List.of(Map.of(
                        "id", "membershipCredential-id",
                        "type", "MembershipCredential",
                        "format", "VC1_0_JWT"
                ))
        );

        given()
                .baseUri(identityEndpoint.get().toString())
                .contentType(JSON)
                .header("x-api-key", participantContexts.get(participantDid).apiKey())
                .body(requestCredentialIssuance)
                .post("/v1alpha/participants/%s/credentials/request".formatted(Base64.getEncoder().encodeToString(participantDid.getBytes())))
                .then()
                .log().ifValidationFails()
                .statusCode(201);

        await().untilAsserted(() -> {
            var path = "/v1alpha/participants/%s/credentials/request/%s"
                    .formatted(Base64.getEncoder().encodeToString(participantDid.getBytes()), holderPid);

            given()
                    .baseUri(identityEndpoint.get().toString())
                    .header("x-api-key", participantContexts.get(participantDid).apiKey())
                    .get(path)
                    .then()
                    .statusCode(200)
                    .body("status", is("ISSUED"));
        });
    }

    public LazySupplier<String> didFor(String name) {
        return new LazySupplier<>(() -> "did:web:localhost%%3A%d:%s".formatted(didEndpoint.get().getPort(), name));
    }

    public String tokenEndpoint() {
        return stsEndpoint.get() + "/token";
    }

    public WalletParticipantContext participantContext(String did) {
        return participantContexts.get(did);
    }

    private WalletParticipantContext registerParticipantContext(LazySupplier<String> did) {
        return given()
                .baseUri(identityEndpoint.get().toString())
                .header("x-api-key", SUPER_USER_API_KEY)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "participantContextId", did.get(),
                        "did", did.get(),
                        "active", "true",
                        "serviceEndpoint", Map.of(
                                "id", UUID.randomUUID().toString(),
                                "type", "CredentialService",
                                "serviceEndpoint", "%s/v1/participants/%s".formatted(
                                        credentialsEndpoint.get().toString(),
                                        Base64.getEncoder().encodeToString(did.get().getBytes())
                                )
                        ),
                        "key", Map.of(
                                "keyId", "%s#key1".formatted(did.get()),
                                "privateKeyAlias", "%s-privatekey-alias".formatted(did.get()),
                                "keyGeneratorParams", Map.of(
                                        "algorithm", "EC"
                                )
                        )
                ))
                .post("/v1alpha/participants")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body().as(WalletParticipantContext.class);
    }

}
