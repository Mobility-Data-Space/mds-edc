/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.compatibility.tests.transfer;

import jakarta.json.JsonObject;
import org.eclipse.edc.compatibility.tests.fixtures.BaseParticipant;
import org.eclipse.edc.compatibility.tests.fixtures.MdsCeConnector;
import org.eclipse.edc.compatibility.tests.fixtures.RemoteParticipant;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;


@EndToEndTest
public class TransferEndToEndTest {

    protected static final RemoteParticipant PROVIDER_PARTICIPANT = RemoteParticipant.Builder.newInstance()
            .name("local")
            .id("local")
            .build();

    protected static final RemoteParticipant CONSUMER_PARTICIPANT = RemoteParticipant.Builder.newInstance()
            .name("remote")
            .id("remote")
            .build();

    private static final MdsCeConnector PROVIDER = new MdsCeConnector("latest", "provider", PROVIDER_PARTICIPANT.controlPlaneEnv())
            .copyResource("keystore.p12")
            .copyResource("provider-vault.properties", "dataspaceconnector-vault.properties");

    private static final MdsCeConnector CONSUMER = new MdsCeConnector("latest", "provider", CONSUMER_PARTICIPANT.controlPlaneEnv())
            .copyResource("keystore.p12")
            .copyResource("consumer-vault.properties", "dataspaceconnector-vault.properties");

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.4")
            .withUsername("postgres")
            .withPassword("password")
            .withCreateContainerCmdModifier(cmd -> cmd.withName("postgres"));

    @Order(0)
    @RegisterExtension
    static final BeforeAllCallback CREATE_DATABASES = context -> {
        PG.setPortBindings(List.of("5432:5432"));
        PG.start();
        createDatabase(PROVIDER_PARTICIPANT.getName());
        createDatabase(CONSUMER_PARTICIPANT.getName());
    };

    private static ClientAndServer providerDataSource;

    @BeforeAll
    static void beforeAll() {
        PROVIDER.start();
        CONSUMER.start();
        providerDataSource = startClientAndServer(getFreePort());
    }

    @AfterAll
    static void afterAll() {
        PROVIDER.stop();
        CONSUMER.stop();
        providerDataSource.stop();
    }

    @BeforeEach
    void storeKeys() {
        // TODO: through management api?
        // var vault = PROVIDER_PARTICIPANT.getService(Vault.class);
        // vault.storeSecret("private-key", PROVIDER_PARTICIPANT.getPrivateKey());
        // vault.storeSecret("public-key", PROVIDER_PARTICIPANT.getPublicKey());
    }

    @ParameterizedTest
    @ArgumentsSource(ParticipantsArgProvider.class)
    void httpPullTransfer(BaseParticipant consumer, BaseParticipant provider) {
        provider.waitForDataPlane();
        providerDataSource.when(HttpRequest.request()).respond(HttpResponse.response().withBody("data"));
        var assetId = UUID.randomUUID().toString();
        var sourceDataAddress = httpSourceDataAddress();
        createResourcesOnProvider(provider, assetId, PolicyFixtures.contractExpiresIn("5s"), sourceDataAddress);

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("HttpData-PULL")
                .execute();

        consumer.awaitTransferToBeInState(transferProcessId, STARTED);

        var edr = await().atMost(consumer.getTimeout())
                .until(() -> consumer.getEdr(transferProcessId), Objects::nonNull);

        // Do the transfer
        var msg = UUID.randomUUID().toString();
        await().atMost(consumer.getTimeout())
                .untilAsserted(() -> consumer.pullData(edr, Map.of("message", msg), body -> assertThat(body).isEqualTo("data")));

        // checks that the EDR is gone once the contract expires
        await().atMost(consumer.getTimeout())
                .untilAsserted(() -> assertThatThrownBy(() -> consumer.getEdr(transferProcessId)));

        // checks that transfer fails
        await().atMost(consumer.getTimeout())
                .untilAsserted(() -> assertThatThrownBy(() -> consumer.pullData(edr, Map.of("message", msg), body -> assertThat(body).isEqualTo("data"))));

        providerDataSource.verify(HttpRequest.request("/source").withMethod("GET"));

    }

    private @NotNull Map<String, Object> httpSourceDataAddress() {
        return Map.of(
                EDC_NAMESPACE + "name", "transfer-test",
                EDC_NAMESPACE + "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source",
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "proxyQueryParams", "true"
        );
    }

    protected void createResourcesOnProvider(BaseParticipant provider, String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        provider.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var contractPolicyId = provider.createPolicyDefinition(contractPolicy);
        var noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());

        provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, contractPolicyId);
    }

    private static class ParticipantsArgProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(CONSUMER_PARTICIPANT, PROVIDER_PARTICIPANT),
                    Arguments.of(PROVIDER_PARTICIPANT, CONSUMER_PARTICIPANT)
            );
        }
    }

}
