package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.LoggingHouseExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;

public class ContractRetirementTest {

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

    @Test
    void shouldTerminateRunningTransfers_andPreventNewOnes() {
        var assetId = PROVIDER.createOffer(Map.of("type", "HttpData", "baseUrl", "https://localhost/any"));

        var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("HttpData-PULL")
                .execute();

        CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

        var providerTransferProcess = PROVIDER.getTransferProcesses().stream()
                .filter(it -> it.asJsonObject().getString("correlationId").equals(consumerTransferProcessId)).findFirst().get();
        var providerAgreementId = providerTransferProcess.asJsonObject().getString("contractId");

        PROVIDER.retireAgreement(providerAgreementId)
                .statusCode(204);

        var event = PROVIDER.waitForEvent("ContractAgreementRetired");
        LOGGING_HOUSE.waitForEvent("ContractAgreementRetired");

        assertThat(event.getJsonObject("payload").getString("contractAgreementId")).isEqualTo(providerAgreementId);
        CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);

        var consumerAgreementId = CONSUMER.getTransferProcess(consumerTransferProcessId).getString("contractId");
        var failedTransferId = CONSUMER.initiateTransfer(PROVIDER, consumerAgreementId, null, null, "HttpData-PULL");
        CONSUMER.awaitTransferToBeInState(failedTransferId, TERMINATED);
    }

    @Test
    void shouldFail_whenAgreementDoesNotExist() {
        PROVIDER.retireAgreement(UUID.randomUUID().toString()).statusCode(404);
    }

}
