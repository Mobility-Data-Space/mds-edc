package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.IssuerExtension;
import eu.dataspace.connector.tests.extensions.LoggingHouseExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import eu.dataspace.connector.tests.tags.DapsTest;
import eu.dataspace.connector.tests.tags.DcpTest;
import io.restassured.response.ValidatableResponse;
import jakarta.json.JsonArray;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;

public class ContractRetirementTest {

    public static final java.util.function.Supplier<Config> POLICY_MONITOR_PERIOD = () -> ConfigFactory.fromMap(Map.of(
            "edc.policy.monitor.period", "PT2S"
    ));

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
        private static final LoggingHouseExtension LOGGING_HOUSE = new LoggingHouseExtension();

        @RegisterExtension
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION)
                .configurationProvider(LOGGING_HOUSE::getConfiguration)
                .configurationProvider(POLICY_MONITOR_PERIOD);

        @RegisterExtension
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION)
                .configurationProvider(LOGGING_HOUSE::getConfiguration);

        protected Daps() {
            super(PROVIDER, CONSUMER, LOGGING_HOUSE);
        }
    }

    @DcpTest
    @Nested
    @Disabled("FIXME: enable once the logging house client supports DCP authentication")
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
                .configurationProvider(LOGGING_HOUSE::getConfiguration)
                .configurationProvider(POLICY_MONITOR_PERIOD);

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
        void shouldExposeRetirementOnAgreement_andAllowUnretire() {
            var providerAgreementId = startTransferAndGetProviderAgreementId();

            provider.retireAgreement(providerAgreementId).statusCode(204);
            provider.retireAgreement(providerAgreementId).statusCode(409);

            var retired = provider.getContractAgreement(providerAgreementId);
            assertThat(retired.getBoolean("retired")).isTrue();
            assertThat(retired.getString("retirementReason")).isEqualTo("a good reason");

            provider.unretireAgreement(providerAgreementId).statusCode(204);
            provider.waitForEvent("ContractAgreementUnretired");

            assertThat(provider.getContractAgreement(providerAgreementId).getBoolean("retired")).isFalse();
            provider.unretireAgreement(providerAgreementId).statusCode(409);
        }

        @Test
        void shouldFail_whenAgreementDoesNotExist() {
            provider.retireAgreement(UUID.randomUUID().toString()).statusCode(404);
            provider.unretireAgreement(UUID.randomUUID().toString()).statusCode(404);
        }

        @Test
        @SuppressWarnings("deprecation")
        void legacyEndpoints_shouldBeWiredToUpstreamRetirement() {
            var providerAgreementId = startTransferAndGetProviderAgreementId();

            provider.legacyRetireAgreement(providerAgreementId).statusCode(204);
            provider.legacyRetireAgreement(providerAgreementId).statusCode(409);

            assertThat(provider.getContractAgreement(providerAgreementId).getBoolean("retired")).isTrue();
            var retiredEntries = retiredAgreementIds(provider.legacyQueryRetiredAgreements().statusCode(200));
            assertThat(retiredEntries).contains(providerAgreementId);

            provider.legacyReactivateAgreement(providerAgreementId).statusCode(204);
            provider.legacyReactivateAgreement(providerAgreementId).statusCode(404);

            assertThat(provider.getContractAgreement(providerAgreementId).getBoolean("retired")).isFalse();
            var entriesAfterReactivation = retiredAgreementIds(provider.legacyQueryRetiredAgreements().statusCode(200));
            assertThat(entriesAfterReactivation).doesNotContain(providerAgreementId);

            provider.legacyRetireAgreement(UUID.randomUUID().toString()).statusCode(404);
        }

        private List<String> retiredAgreementIds(ValidatableResponse response) {
            // the legacy response is untyped, so its properties are compacted with the "edc" prefix
            return response.extract().body().as(JsonArray.class).stream()
                    .map(it -> it.asJsonObject().getString("edc:agreementId"))
                    .toList();
        }

        private String startTransferAndGetProviderAgreementId() {
            var assetId = provider.createOffer(Map.of("type", "HttpData", "baseUrl", "https://localhost/any"));

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

            return provider.getTransferProcesses().stream()
                    .map(it -> it.asJsonObject())
                    .filter(it -> it.getString("correlationId").equals(consumerTransferProcessId))
                    .findFirst().orElseThrow()
                    .getString("contractId");
        }
    }

}
