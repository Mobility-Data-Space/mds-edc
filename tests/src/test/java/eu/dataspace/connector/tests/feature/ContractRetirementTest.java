package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.PostgresqlExtension;
import eu.dataspace.connector.tests.SovityDapsExtension;
import eu.dataspace.connector.tests.VaultExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;

public class ContractRetirementTest {

    @Nested
    class InMemory extends Tests {

        @RegisterExtension
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.inMemory("provider");

        @RegisterExtension
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.inMemory("consumer");

        InMemory() {
            super(PROVIDER, CONSUMER);
        }
    }

    @Nested
    class HashicorpVaultPostgresql extends Tests {

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
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        @RegisterExtension
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);;

        HashicorpVaultPostgresql() {
            super(PROVIDER, CONSUMER);
        }
    }

    abstract static class Tests {

        private final MdsParticipant provider;
        private final MdsParticipant consumer;

        Tests(MdsParticipant provider, MdsParticipant consumer) {
            this.provider = provider;
            this.consumer = consumer;
        }

        @Test
        void shouldTerminateRunningTransfers_andPreventNewOnes() {
            var assetId = provider.createOffer(Map.of("type", "HttpData", "baseUrl", "https://localhost/any"));

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var agreementId = consumer.getTransferProcess(consumerTransferProcessId).getString("contractId");

            provider.retireProviderAgreement(agreementId)
                    .statusCode(204);

            consumer.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);

            var failedTransferId = consumer.initiateTransfer(provider, agreementId, null, null, "HttpData-PULL");
            consumer.awaitTransferToBeInState(failedTransferId, TERMINATED);
        }

        @Test
        void shouldFail_whenAgreementDoesNotExist() {
            provider.retireProviderAgreement(UUID.randomUUID().toString()).statusCode(404);
        }

    }

}
