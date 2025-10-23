/*
 * Copyright (c) 2025 Mobility Data Space
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *      Think-it GmbH - initial API and implementation
 */

package eu.dataspace.connector.tests;

import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static eu.dataspace.connector.tests.Crypto.generateEcKey;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Map.entry;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

public class IdentityHub implements BeforeAllCallback, AfterAllCallback {

    private final EmbeddedRuntime runtime;
    private final String[] participants;
    private final Map<String, CreateParticipantContextResponse> participantContexts = new HashMap<>();
    private final LazySupplier<URI> stsEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/sts"));
    private final LazySupplier<URI> didEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/"));
    private final LazySupplier<URI> credentialsEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/credentials"));
    private final LazySupplier<URI> identityEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/identity"));

    public IdentityHub(EmbeddedRuntime runtime, String[] participants) {
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
                entry("edc.iam.did.web.use.https", "false")
        )));
        for (var participant : participants) {
            runtime.registerSystemExtension(ServiceExtension.class, new CreateParticipantContextExtension(didFor(participant)));
        }
        runtime.boot();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        runtime.shutdown();
    }

    public void requestCredentialIssuance(String participantDid, String issuerDid) {
        // TODO: holderPid should be random generated
        // TODO: use json object or map
        var request = """
                    {
                      "issuerDid": "%s",
                      "holderPid": "test-request-id",
                      "credentials": [{ "format": "VC1_0_JWT", "id": "membershipCredential-id", "type": "MembershipCredential" }]
                    }
                    """.formatted(issuerDid);

        var statusPath = given()
                .baseUri(identityEndpoint.get().toString())
                .contentType(JSON)
                .header("x-api-key", participantContexts.get(participantDid).apiKey())
                .body(request)
                .post("/v1alpha/participants/%s/credentials/request".formatted(Base64.getEncoder().encodeToString(participantDid.getBytes())))
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .header("Location", endsWith("/credentials/request/test-request-id"))
                .extract().header("Location");


        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> { // TODO: remove timeout
            given()
                    .baseUri(identityEndpoint.get().toString())
                    .header("x-api-key", participantContexts.get(participantDid).apiKey())
                    .get("/v1alpha/participants/%s/credentials/request/test-request-id".formatted(Base64.getEncoder().encodeToString(participantDid.getBytes())))
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

    public CreateParticipantContextResponse participantContext(String did) {
        return participantContexts.get(did);
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
                            UUID.randomUUID().toString(), "CredentialService",
                            "%s/v1/participants/%s".formatted(credentialsEndpoint.get().toString(), Base64.getEncoder().encodeToString(did.get().getBytes())))
                    )
                    .key(KeyDescriptor.Builder.newInstance()
                            .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                            .privateKeyAlias(privateKeyAlias)
                            .keyId(participantKey.getKeyID())
                            .build())
                    .build();

            var createParticipantContextResponse = participantContextService.createParticipantContext(manifest).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
            participantContexts.put(did.get(), createParticipantContextResponse);
        }
    }
}
