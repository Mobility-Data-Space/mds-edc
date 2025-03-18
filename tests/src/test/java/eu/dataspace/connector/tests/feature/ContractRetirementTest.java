package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.DapsExtension;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.PostgresqlExtension;
import eu.dataspace.connector.tests.VaultExtension;
import jakarta.json.Json;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;

public class ContractRetirementTest {

    private static final MdsParticipant PROVIDER = MdsParticipant.Builder.newInstance()
            .id("provider").name("provider")
            .build();

    private static final MdsParticipant CONSUMER = MdsParticipant.Builder.newInstance()
            .id("consumer").name("consumer")
            .build();

    @Nested
    class InMemory extends Tests {

        @RegisterExtension
        private static final RuntimeExtension PROVIDER_EXTENSION = new RuntimePerClassExtension(
                new EmbeddedRuntime("provider", ":launchers:connector-inmemory")
                        .configurationProvider(PROVIDER::getConfiguration))
                .registerSystemExtension(ServiceExtension.class, PROVIDER.seedVaultKeys());

        @RegisterExtension
        private static final RuntimeExtension CONSUMER_EXTENSION = new RuntimePerClassExtension(
                new EmbeddedRuntime("consumer", ":launchers:connector-inmemory")
                        .configurationProvider(CONSUMER::getConfiguration));

    }

    @Nested
    class HashicorpVaultPostgresql extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(1)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension(PROVIDER.getName(), CONSUMER.getName());

        @RegisterExtension
        @Order(2)
        private static final DapsExtension DAPS_EXTENSION = new DapsExtension();

        @RegisterExtension
        private static final RuntimeExtension PROVIDER_EXTENSION = new RuntimePerClassExtension(
                new EmbeddedRuntime("provider", ":launchers:connector-vault-postgresql")
                        .configurationProvider(PROVIDER::getConfiguration)
                        .configurationProvider(() -> VAULT_EXTENSION.getConfig("provider"))
                        .registerSystemExtension(ServiceExtension.class, PROVIDER.seedVaultKeys())
                        .configurationProvider(() -> DAPS_EXTENSION.dapsConfig("provider"))
                        .registerSystemExtension(ServiceExtension.class, DAPS_EXTENSION.seedExtension())
                        .configurationProvider(() -> POSTGRES_EXTENSION.getConfig(PROVIDER.getName()))
        );

        @RegisterExtension
        private static final RuntimeExtension CONSUMER_EXTENSION = new RuntimePerClassExtension(
                new EmbeddedRuntime("consumer", ":launchers:connector-vault-postgresql")
                        .configurationProvider(CONSUMER::getConfiguration)
                        .configurationProvider(() -> DAPS_EXTENSION.dapsConfig("consumer"))
                        .configurationProvider(() -> VAULT_EXTENSION.getConfig("consumer"))
                        .registerSystemExtension(ServiceExtension.class, DAPS_EXTENSION.seedExtension())
                        .configurationProvider(() -> POSTGRES_EXTENSION.getConfig(CONSUMER.getName()))
        );

    }

    abstract static class Tests {

        @Test
        void shouldTerminateRunningTransfers_andPreventNewOnes() {
            var assetId = PROVIDER.createOffer(Map.of("type", "HttpData", "baseUrl", "https://localhost/any"));

            var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            var agreementId = CONSUMER.getTransferProcess(consumerTransferProcessId).getString("contractId");

            PROVIDER.retireProviderAgreement(agreementId)
                    .statusCode(204);

            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);

            var failedTransferId = CONSUMER.initiateTransfer(PROVIDER, agreementId, null, null, "HttpData-PULL");
            CONSUMER.awaitTransferToBeInState(failedTransferId, TERMINATED);
        }

        @Test
        void shouldFail_whenAgreementDoesNotExist() {
            PROVIDER.retireProviderAgreement(UUID.randomUUID().toString()).statusCode(404);
        }

    }

}
