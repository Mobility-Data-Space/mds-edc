package eu.dataspace.connector.tests.feature;

import com.github.tomakehurst.wiremock.WireMockServer;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import eu.dataspace.connector.tests.tags.DapsTest;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

class DataFlowManagementApiTest {

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

    abstract static class Tests {

        private final MdsParticipant provider;
        private final MdsParticipant consumer;

        protected Tests(MdsParticipant provider, MdsParticipant consumer) {
            this.provider = provider;
            this.consumer = consumer;
        }

        @Test
        void shouldQueryDataFlows_andRetrieveDataAddress() {
            var providerDataSource = new WireMockServer(Ports.getFreePort());
            providerDataSource.stubFor(get(urlPathEqualTo("/source")).willReturn(ok("data")));
            providerDataSource.start();
            var sourceUrl = "http://localhost:%s/source".formatted(providerDataSource.port());

            try {
                var assetId = provider.createOffer(Map.of(
                        EDC_NAMESPACE + "type", "HttpData",
                        EDC_NAMESPACE + "baseUrl", sourceUrl
                ));

                var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                        .withTransferType("HttpData-PULL")
                        .execute();

                consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

                var providerDataFlowId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);

                // the request endpoint lists the data flow known to the provider's data plane
                await().untilAsserted(() -> assertThat(provider.requestDataFlows())
                        .anySatisfy(flow -> assertThat(flow.asJsonObject().getString(ID)).isEqualTo(providerDataFlowId)));

                // the dataaddress endpoint returns the data address stored for the transfer process (the EDR, on the
                // consumer side), resolved from the DataAddressStore with its secret included
                await().untilAsserted(() -> {
                    var dataAddress = consumer.getDataFlowDataAddress(consumerTransferProcessId)
                            .statusCode(200)
                            .extract().body().asString();

                    assertThat(dataAddress).contains("endpoint");
                });
            } finally {
                providerDataSource.stop();
            }
        }

        @Test
        void shouldReturnNotFound_whenDataFlowDoesNotExist() {
            provider.getDataFlowDataAddress("unexistent").statusCode(404);
        }
    }
}
