package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.IssuerExtension;
import eu.dataspace.connector.tests.extensions.LoggingHouseExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;

public class ContractRetirementTest {

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
        private static final LoggingHouseExtension LOGGING_HOUSE = new LoggingHouseExtension();

        @RegisterExtension
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION)
                .configurationProvider(LOGGING_HOUSE::getConfiguration);

        @RegisterExtension
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION)
                .configurationProvider(LOGGING_HOUSE::getConfiguration);

        protected Daps() {
            super(PROVIDER, CONSUMER, LOGGING_HOUSE);
        }
    }

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
        private static final LoggingHouseExtension LOGGING_HOUSE = new LoggingHouseExtension();

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVaultDcp("provider", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did())
                .configurationProvider(LOGGING_HOUSE::getConfiguration);

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVaultDcp("consumer", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did())
                .configurationProvider(LOGGING_HOUSE::getConfiguration);

        protected Dcp() {
            super(PROVIDER, CONSUMER, LOGGING_HOUSE);
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
        private final LoggingHouseExtension loggingHouse;

        protected Tests(MdsParticipant provider, MdsParticipant consumer, LoggingHouseExtension loggingHouse) {
            this.provider = provider;
            this.consumer = consumer;
            this.loggingHouse = loggingHouse;
        }

        @Test
        void shouldTerminateRunningTransfers_andPreventNewOnes() {
            var assetId = provider.createOffer(Map.of("type", "HttpData", "baseUrl", "https://localhost/any"));

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var providerTransferProcess = provider.getTransferProcesses().stream()
                    .filter(it -> it.asJsonObject().getString("correlationId").equals(consumerTransferProcessId)).findFirst().get();
            var providerAgreementId = providerTransferProcess.asJsonObject().getString("contractId");

            provider.retireAgreement(providerAgreementId)
                    .statusCode(204);

            var event = provider.waitForEvent("ContractAgreementRetired");
            loggingHouse.waitForEvent("ContractAgreementRetired");

            assertThat(event.getJsonObject("payload").getString("contractAgreementId")).isEqualTo(providerAgreementId);
            consumer.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);

            var consumerAgreementId = consumer.getTransferProcess(consumerTransferProcessId).getString("contractId");
            var failedTransferId = consumer.initiateTransfer(provider, consumerAgreementId, null, null, "HttpData-PULL");
            consumer.awaitTransferToBeInState(failedTransferId, TERMINATED);
        }

        @Test
        void shouldFail_whenAgreementDoesNotExist() {
            provider.retireAgreement(UUID.randomUUID().toString()).statusCode(404);
        }
    }

}
