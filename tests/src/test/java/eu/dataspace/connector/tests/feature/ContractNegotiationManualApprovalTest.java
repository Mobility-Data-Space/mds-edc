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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.atomicConstraint;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.policy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class ContractNegotiationManualApprovalTest {

    private static final String MDS_MANUAL_APPROVAL = "https://w3id.org/mobility-dataspace/2026/1/policy/ManualApproval";

    @DapsTest
    @Nested
    class Daps extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(1)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("provider", "consumer");

        @RegisterExtension
        @Order(2)
        private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        protected Daps() {
            super(PROVIDER, CONSUMER);
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
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("issuer", "wallet", "consumer", "provider");

        @RegisterExtension
        @Order(1)
        private static final IssuerExtension ISSUER = new IssuerExtension(POSTGRES_EXTENSION, VAULT_EXTENSION);

        @RegisterExtension
        @Order(2)
        private static final Wallet IDENTITY_HUB = MdsParticipantFactory.wallet(POSTGRES_EXTENSION, VAULT_EXTENSION, "consumer", "provider");

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVaultDcp("provider", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVaultDcp("consumer", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        protected Dcp() {
            super(PROVIDER, CONSUMER);
        }

        @BeforeAll
        static void setUp() {
            ISSUER.registerAttestationAndCredentialDefinition();
            ISSUER.registerHolder(PROVIDER.getId(), PROVIDER.getName());
            ISSUER.registerHolder(CONSUMER.getId(), CONSUMER.getName());
            IDENTITY_HUB.requestCredentialIssuance(PROVIDER.getId(), ISSUER.did().get());
            IDENTITY_HUB.requestCredentialIssuance(CONSUMER.getId(), ISSUER.did().get());
        }
    }

    abstract static class Tests {

        private final MdsParticipant provider;
        private final MdsParticipant consumer;

        protected Tests(MdsParticipant provider, MdsParticipant consumer) {
            this.provider = provider;
            this.consumer = consumer;
        }

        @Test
        void shouldPublishManualApprovalRequirementInCatalog() {
            var assetId = createOfferWithManualApproval();

            var catalogDatasets = consumer.getCatalogDatasets(provider);

            assertThat(catalogDatasets).anySatisfy(dataset -> {
                assertThat(dataset.asJsonObject().getString(ID)).isEqualTo(assetId);
                assertThat(dataset.toString()).contains(MDS_MANUAL_APPROVAL);
            });
        }

        @Test
        @Deprecated
        void shouldManuallyApproveNegotiation_whenContractDefinitionHasManualApprovalProperty() {
            var assetId = UUID.randomUUID().toString();
            provider.createAsset(assetId, emptyMap(), dataAddressProperties());
            var noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());
            provider.createContractDefinitionWithManualApproval(assetId, noConstraintPolicyId);
            var consumerContractNegotiationId = consumer.initContractNegotiation(provider, assetId);

            var pending = awaitPendingOnProvider(consumerContractNegotiationId);

            provider.approveNegotiation(pending.getString(ID)).statusCode(204);

            await().untilAsserted(() -> {
                assertThat(consumer.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(FINALIZED.name());
            });
        }

        @Test
        void shouldManuallyApproveNegotiation() {
            var assetId = createOfferWithManualApproval();
            var consumerContractNegotiationId = consumer.initContractNegotiation(provider, assetId);

            var pending = awaitPendingOnProvider(consumerContractNegotiationId);

            provider.approveNegotiation(pending.getString(ID)).statusCode(204);

            await().untilAsserted(() -> {
                assertThat(consumer.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(FINALIZED.name());
            });
        }

        @Test
        void shouldManuallyRejectNegotiation() {
            var assetId = createOfferWithManualApproval();
            var consumerContractNegotiationId = consumer.initContractNegotiation(provider, assetId);

            var pending = awaitPendingOnProvider(consumerContractNegotiationId);

            provider.rejectNegotiation(pending.getString(ID)).statusCode(204);

            await().untilAsserted(() -> {
                assertThat(consumer.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(TERMINATED.name());
            });
        }

        @Test
        void shouldManuallyApproveNegotiationWithEvents() {
            var assetId = createOfferWithManualApproval();
            var consumerNegotiationId = consumer.initContractNegotiation(provider, assetId);

            var contractNegotiationRequested = provider.waitForEvent("ContractNegotiationRequested");
            var providerNegotiationId = contractNegotiationRequested.getJsonObject("payload").getString("contractNegotiationId");

            consumer.waitForEvent("ContractNegotiationRequested");
            await().untilAsserted(() -> provider.approveNegotiation(providerNegotiationId).statusCode(204));

            provider.waitForEvent("ContractNegotiationApproved");
            var contractNegotiationFinalized = consumer.waitForEvent("ContractNegotiationFinalized");

            assertThat(contractNegotiationFinalized.getJsonObject("payload").getString("contractNegotiationId")).isEqualTo(consumerNegotiationId);
            assertThat(consumer.getContractNegotiationState(consumerNegotiationId)).isEqualTo(FINALIZED.name());
        }

        @Test
        void shouldManuallyRejectNegotiationWithEvents() {
            var assetId = createOfferWithManualApproval();
            var consumerNegotiationId = consumer.initContractNegotiation(provider, assetId);

            var contractNegotiationRequested = provider.waitForEvent("ContractNegotiationRequested");
            var providerNegotiationId = contractNegotiationRequested.getJsonObject("payload").getString("contractNegotiationId");

            consumer.waitForEvent("ContractNegotiationRequested");
            await().untilAsserted(() -> provider.rejectNegotiation(providerNegotiationId).statusCode(204));

            provider.waitForEvent("ContractNegotiationRejected");
            var contractNegotiationTerminated = consumer.waitForEvent("ContractNegotiationTerminated");

            assertThat(contractNegotiationTerminated.getJsonObject("payload").getString("contractNegotiationId")).isEqualTo(consumerNegotiationId);
            assertThat(consumer.getContractNegotiationState(consumerNegotiationId)).isEqualTo(TERMINATED.name());
        }

        @Test
        void shouldReturnConflict_whenApprovalOnConsumerSide() {
            var assetId = createOfferWithManualApproval();
            var consumerContractNegotiationId = consumer.initContractNegotiation(provider, assetId);

            await().untilAsserted(() -> {
                assertThat(consumer.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(REQUESTED.name());
            });

            consumer.approveNegotiation(consumerContractNegotiationId).statusCode(409);
        }

        @Test
        void shouldReturnConflict_whenRejectionOnConsumerSide() {
            var assetId = createOfferWithManualApproval();
            var consumerContractNegotiationId = consumer.initContractNegotiation(provider, assetId);

            await().untilAsserted(() -> {
                assertThat(consumer.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(REQUESTED.name());
            });

            consumer.rejectNegotiation(consumerContractNegotiationId).statusCode(409);
        }

        private JsonObject awaitPendingOnProvider(String consumerContractNegotiationId) {
            await().untilAsserted(() -> {
                assertThat(consumer.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(REQUESTED.name());
            });

            var providerNegotiationId = consumer.getContractNegotiation(consumerContractNegotiationId).getString("correlationId");
            var pending = await().until(() -> provider.getPendingNegotiation(providerNegotiationId), Objects::nonNull);
            assertThat(pending.getString("state")).isEqualTo(REQUESTED.name());
            return pending;
        }

        private String createOfferWithManualApproval() {
            var assetId = UUID.randomUUID().toString();
            provider.createAsset(assetId, emptyMap(), dataAddressProperties());
            var noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());
            var manualApprovalPermission = createObjectBuilder()
                    .add("action", "use")
                    .add("constraint", atomicConstraint(MDS_MANUAL_APPROVAL, "eq", "true"))
                    .build();
            var manualApprovalPolicyId = provider.createPolicyDefinition(policy(List.of(manualApprovalPermission)));
            provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, manualApprovalPolicyId);
            return assetId;
        }

        private Map<String, Object> dataAddressProperties() {
            return Map.of(
                    EDC_NAMESPACE + "type", "HttpData",
                    EDC_NAMESPACE + "baseUrl", "http://localhost/any"
            );
        }
    }
}
