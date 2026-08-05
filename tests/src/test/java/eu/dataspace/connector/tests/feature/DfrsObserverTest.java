package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.IssuerExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import eu.dataspace.connector.tests.tags.DapsTest;
import eu.dataspace.connector.tests.tags.DcpTest;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

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
        @Order(3)
        private static final MdsParticipant OBSERVER = MdsParticipantFactory.hashicorpVault("observer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        protected Daps() {
            super(OBSERVER, PROVIDER);
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
        @Order(3)
        private static final MdsParticipant OBSERVER = MdsParticipantFactory.hashicorpVaultDcp("observer", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVaultDcp("provider", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        protected Dcp() {
            super(OBSERVER, PROVIDER);
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

        public Tests(MdsParticipant observer, MdsParticipant provider) {
            this.observer = observer;
            this.provider = provider;
        }

        @Test
        void shouldStartObserverNegotiationAtStartup() {
            var observerDatasetId = "observerDatasetId";
            Map<String, Object> dataAddressProperties = Map.of(
                    EDC_NAMESPACE + "type", "HttpData",
                    EDC_NAMESPACE + "baseUrl", "http://localhost/any"
            );
            observer.createAsset(observerDatasetId, emptyMap(), dataAddressProperties);
            var policyDefinitionId = observer.createPolicyDefinition(PolicyFixtures.noConstraintPolicy());
            observer.createContractDefinition(observerDatasetId, UUID.randomUUID().toString(), policyDefinitionId, policyDefinitionId);

            provider.configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.mds.dfrs.observer.id", observer.getId(),
                    "edc.mds.dfrs.observer.url", observer.getProtocolUrl(),
                    "edc.mds.dfrs.observer.dataset.id", observerDatasetId,
                    "edc.mds.dfrs.observer.profile", "dataspace-protocol-http:2025-1"
            )));

            provider.beforeAll(null); // start provider

            await().untilAsserted(() -> {
                var contractNegotiations = provider.getContractNegotiationsWith(observer.getId());

                assertThat(contractNegotiations).hasSize(1).allSatisfy(it -> {
                    assertThat(it.asJsonObject().getString("state")).isEqualTo(FINALIZED.name());
                });
            });

            provider.afterAll(null); // stop provider
        }
    }
}
