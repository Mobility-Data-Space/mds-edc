package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.SeedVault;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.DfrsObserverServerExtension;
import eu.dataspace.connector.tests.extensions.IssuerExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import eu.dataspace.connector.tests.tags.DapsTest;
import eu.dataspace.connector.tests.tags.DcpTest;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class DfrsObserverTest {

    @DapsTest
    @Nested
    class Daps extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(1)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("provider", "consumer", "observer");

        @RegisterExtension
        @Order(2)
        private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

        @RegisterExtension
        @Order(2)
        private static final DfrsObserverServerExtension OBSERVER_SERVER = new DfrsObserverServerExtension();

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant OBSERVER = MdsParticipantFactory.hashicorpVault("observer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION)
                .registerServiceExtension(SeedVault.fromMap(i ->
                        Map.of("observer-api-key", OBSERVER_SERVER.getApiKey())));

        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        protected Daps() {
            super(OBSERVER, PROVIDER, OBSERVER_SERVER);
        }

    }

    @DcpTest
    @Nested
    class Dcp extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(0)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension(
                "issuer", "wallet", "consumer", "provider", "observer");

        @RegisterExtension
        @Order(1)
        private static final IssuerExtension ISSUER = new IssuerExtension(POSTGRES_EXTENSION, VAULT_EXTENSION);

        @RegisterExtension
        @Order(2)
        private static final Wallet IDENTITY_HUB = MdsParticipantFactory.wallet(POSTGRES_EXTENSION, VAULT_EXTENSION,
                "consumer", "provider", "observer");

        @RegisterExtension
        @Order(2)
        private static final DfrsObserverServerExtension OBSERVER_SERVER = new DfrsObserverServerExtension();

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant OBSERVER = MdsParticipantFactory.hashicorpVaultDcp("observer", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did())
                .registerServiceExtension(SeedVault.fromMap(i ->
                        Map.of("observer-api-key", OBSERVER_SERVER.getApiKey())));

        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVaultDcp("provider", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        protected Dcp() {
            super(OBSERVER, PROVIDER, OBSERVER_SERVER);
        }

        @BeforeAll
        static void setUp() {
            ISSUER.registerAttestationAndCredentialDefinition();
            ISSUER.registerHolder(PROVIDER.getId(), PROVIDER.getName());
            ISSUER.registerHolder(OBSERVER.getId(), OBSERVER.getName());
            IDENTITY_HUB.requestCredentialIssuance(PROVIDER.getId(), ISSUER.did().get());
            IDENTITY_HUB.requestCredentialIssuance(OBSERVER.getId(), ISSUER.did().get());
        }
    }

    abstract static class Tests {

        private final MdsParticipant observer;
        private final MdsParticipant provider;
        private final MdsParticipant consumer;
        private final DfrsObserverServerExtension observerServer;
        private final Duration timeout = Duration.ofSeconds(10);

        public Tests(MdsParticipant observer, MdsParticipant provider, DfrsObserverServerExtension observerServer) {
            this.observer = observer;
            // note: to avoid adding another connector, the Observer will also act as a DFRS consumer to trigger a negotiation
            this.consumer = observer;
            this.provider = provider;
            this.observerServer = observerServer;
        }

        @Test
        void shouldStartObserverNegotiationAtStartup() {
            var observerDatasetId = observer.createOffer(Map.of(
                    "type", "HttpData",
                    "baseUrl", observerServer.getBaseUrl() + "/api/v1/events",
                    "method", "POST",
                    "proxyBody", "true",
                    "authKey", "X-Api-Key",
                    "secretName", "observer-api-key"
            ));

            provider.configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.mds.dfrs.observer.id", observer.getId(),
                    "edc.mds.dfrs.observer.url", observer.getProtocolUrl(),
                    "edc.mds.dfrs.observer.dataset.id", observerDatasetId,
                    "edc.mds.dfrs.observer.profile", "dataspace-protocol-http:2025-1",
                    "edc.mds.dfrs.observer.transfer.profile", "HttpData-PULL"
            )));

            provider.beforeAll(null); // start provider

            var providerContractAgreementId = new AtomicReference<String>();
            await().atMost(timeout).untilAsserted(() -> {
                var contractNegotiations = provider.getContractNegotiationsWith(observer.getId());

                assertThat(contractNegotiations).hasSizeGreaterThan(0).last().extracting(JsonValue::asJsonObject).satisfies(negotiation -> {
                    assertThat(negotiation.getString("state")).isEqualTo(ContractNegotiationStates.FINALIZED.name());

                    providerContractAgreementId.set(negotiation.getString("contractAgreementId"));
                    var transferProcesses = provider.getTransferProcessesOnAgreement(providerContractAgreementId.get());
                    assertThat(transferProcesses).hasSize(1).last().extracting(JsonValue::asJsonObject).satisfies(transfer -> {
                        assertThat(transfer.getString("state")).isEqualTo(TransferProcessStates.STARTED.name());
                    });
                });
            });

            var assetId = provider.createOffer(Map.of(
                    "type", "HttpData",
                    "baseUrl", "http://any"
            ));

            consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            observerServer.waitForEvent(provider.getId(), "org.eclipse.edc.ContractNegotiationFinalized");
            observerServer.waitForEvent(provider.getId(), "org.eclipse.edc.TransferProcessStarted");

            provider.retireAgreement(providerContractAgreementId.get()).statusCode(204);

            observerServer.waitForEvent(provider.getId(), "eu.dataspace.mds.ContractAgreementRetired");

            provider.afterAll(null); // stop provider
        }

        @Test
        void shouldReinitiateObserverNegotiation_whenTransferGetsTerminated() {
            var observerDatasetId = observer.createOffer(Map.of(
                    "type", "HttpData",
                    "baseUrl", observerServer.getBaseUrl() + "/api/v1/events",
                    "method", "POST",
                    "proxyBody", "true",
                    "authKey", "X-Api-Key",
                    "secretName", "observer-api-key"
            ));

            provider.configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.mds.dfrs.observer.id", observer.getId(),
                    "edc.mds.dfrs.observer.url", observer.getProtocolUrl(),
                    "edc.mds.dfrs.observer.dataset.id", observerDatasetId,
                    "edc.mds.dfrs.observer.profile", "dataspace-protocol-http:2025-1",
                    "edc.mds.dfrs.observer.transfer.profile", "HttpData-PULL"
            )));

            provider.beforeAll(null); // start provider

            var providerContractAgreementId = new AtomicReference<String>();
            await().atMost(timeout).untilAsserted(() -> {
                var contractNegotiations = provider.getContractNegotiationsWith(observer.getId());

                assertThat(contractNegotiations).hasSize(1).first().extracting(JsonValue::asJsonObject).satisfies(negotiation -> {
                    assertThat(negotiation.getString("state")).isEqualTo(ContractNegotiationStates.FINALIZED.name());

                    providerContractAgreementId.set(negotiation.getString("contractAgreementId"));
                    var transferProcesses = provider.getTransferProcessesOnAgreement(providerContractAgreementId.get());
                    assertThat(transferProcesses).hasSize(1).first().extracting(JsonValue::asJsonObject).satisfies(transfer -> {
                        assertThat(transfer.getString("state")).isEqualTo(TransferProcessStates.STARTED.name());
                    });
                });
            });

            // observer terminates transfer, for any reason
            var observerTransferProcesses = observer.getTransferProcessesOnAsset(observerDatasetId);
            assertThat(observerTransferProcesses).hasSizeGreaterThan(0);
            var observerTransferProcessCount = observerTransferProcesses.size();
            var observerTransferProcessId = observerTransferProcesses.get(0).asJsonObject().getString("@id");
            observer.terminateTransfer(observerTransferProcessId);

            observer.awaitTransferToBeInState(observerTransferProcessId, TransferProcessStates.TERMINATED);

            // wait to find another observer negotiation & transfer on provider
            await().untilAsserted(() -> {
                assertThat(observer.getTransferProcessesOnAsset(observerDatasetId)).hasSize(observerTransferProcessCount + 1);
            });

            // events are flowing again
            var assetId = provider.createOffer(Map.of(
                    "type", "HttpData",
                    "baseUrl", "http://any"
            ));

            consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            observerServer.waitForEvent(provider.getId(), "org.eclipse.edc.ContractNegotiationFinalized");

            provider.afterAll(null); // stop provider
        }
    }
}
