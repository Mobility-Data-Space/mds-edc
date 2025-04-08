package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.PostgresqlExtension;
import eu.dataspace.connector.tests.SovityDapsExtension;
import eu.dataspace.connector.tests.VaultExtension;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class EdpTest {

    private static final MdsParticipant PROVIDER = MdsParticipant.Builder.newInstance()
            .id("provider").name("provider")
            .build();

    private static final MdsParticipant CONSUMER = MdsParticipant.Builder.newInstance()
            .id("consumer").name("consumer")
            .build();

    @RegisterExtension
    @Order(0)
    private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

    @RegisterExtension
    @Order(1)
    private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension(PROVIDER.getName(), CONSUMER.getName());

    @RegisterExtension
    @Order(2)
    private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_EXTENSION = new RuntimePerClassExtension(
            new EmbeddedRuntime("provider", ":launchers:connector-vault-postgresql-edp")
                    .configurationProvider(PROVIDER::getConfiguration)
                    .configurationProvider(() -> VAULT_EXTENSION.getConfig(PROVIDER.getName()))
                    .registerSystemExtension(ServiceExtension.class, PROVIDER.seedVaultKeys())
                    .configurationProvider(() -> DAPS_EXTENSION.dapsConfig(PROVIDER.getId()))
                    .registerSystemExtension(ServiceExtension.class, DAPS_EXTENSION.seedExtension())
                    .configurationProvider(() -> POSTGRES_EXTENSION.getConfig(PROVIDER.getName()))
                    .configurationProvider(() -> ConfigFactory.fromMap(
                        Map.ofEntries(entry("edp.dataplane.callback.url", "http://localhost:8080"))))
    );

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_EXTENSION = new RuntimePerClassExtension(
            new EmbeddedRuntime("consumer", ":launchers:connector-vault-postgresql-edp")
                    .configurationProvider(CONSUMER::getConfiguration)
                    .configurationProvider(() -> VAULT_EXTENSION.getConfig(CONSUMER.getName()))
                    .registerSystemExtension(ServiceExtension.class, CONSUMER.seedVaultKeys())
                    .configurationProvider(() -> DAPS_EXTENSION.dapsConfig(CONSUMER.getId()))
                    .registerSystemExtension(ServiceExtension.class, DAPS_EXTENSION.seedExtension())
                    .configurationProvider(() -> POSTGRES_EXTENSION.getConfig(CONSUMER.getName()))
                    .configurationProvider(() -> ConfigFactory.fromMap(
                        Map.ofEntries(entry("edp.dataplane.callback.url", "http://localhost:8080"))))
    );
    
    @Test
    void shouldAllowEDPSJob_andResultAsset() {
        var edpsBackendService = startClientAndServer(getFreePort());
        // Register EDPS endpoints
        // Mock POST /v1/dataspace/analysisjob
        edpsBackendService.when(
            request()
                .withMethod("POST")
                .withPath("/v1/dataspace/analysisjob")
        ).respond(
            response()
                .withStatusCode(200)
                .withBody("{\"job_id\": \"40c70511-9427-43d1-811b-97231145cce1\", \"state\": \"WAITING_FOR_DATA\", \"state_detail\": \"Job is waiting for data to be uploaded.\"}")
        );

        // Mock POST /v1/dataspace/analysisjob/{job_id}/data/file
        edpsBackendService.when(
            request()
                .withMethod("POST")
                .withPath("/v1/dataspace/analysisjob/40c70511-9427-43d1-811b-97231145cce1/data/file")
        ).respond(
            response()
                .withStatusCode(200)
                .withBody("{\"status\": \"success\", \"message\": \"File uploaded and processed successfully.\"}")
        );

        // Mock GET /v1/dataspace/analysisjob/{job_id}/result
        edpsBackendService.when(
            request()
                .withMethod("GET")
                .withPath("/v1/dataspace/analysisjob/40c70511-9427-43d1-811b-97231145cce1/result")
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/zip")
                .withBody(new byte[]{1, 2, 3, 4, 5}) // Simulating a non-empty zip file
        );

        // Mock GET /v1/dataspace/analysisjob/{job_id}/status
        edpsBackendService.when(
            request()
                .withMethod("GET")
                .withPath("/v1/dataspace/analysisjob/40c70511-9427-43d1-811b-97231145cce1/status")
        ).respond(
            response()
                .withStatusCode(200)
                .withBody("{\"job_id\": \"40c70511-9427-43d1-811b-97231145cce1\", \"state\": \"COMPLETED\", \"state_detail\": \"Job has been completed successfully.\"}")
        );

        // Prepare the contract agreement ID for the EDPS asset
        Map<String, Object> edpsDataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s".formatted(edpsBackendService.getPort()),
                EDC_NAMESPACE + "proxyPath", "true",
                EDC_NAMESPACE + "proxyMethod", "true",
                EDC_NAMESPACE + "proxyQueryParams", "true",
                EDC_NAMESPACE + "proxyBody", "true"
        );
        var edpsAssetId = PROVIDER.createOffer(edpsDataAddressProperties);

        var transferProcessId = CONSUMER.requestAssetFrom(edpsAssetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);

        var edpsContractAgreementId = CONSUMER.getTransferProcess(transferProcessId).getString("contractId");

        // Create an asset
        var sourceBackend = ClientAndServer.startClientAndServer(getFreePort());
        sourceBackend.when(request("/source")).respond(response("data"));
        Map<String, Object> dataAddressProperties = Map.of(
                    EDC_NAMESPACE + "type", "HttpData",
                    EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(sourceBackend.getPort())
            );
        var assetId = CONSUMER.createOffer(dataAddressProperties);

        // Run job and get status
        var jobId = CONSUMER.createEdpsJob(assetId, edpsContractAgreementId).getString("jobId");

        // Get EDPS Results zip file
        var edpsResults = CONSUMER.getEdpsResult(assetId, jobId, edpsContractAgreementId);

        assertThat(edpsResults).isNotNull();
        assertThat(edpsResults.getString("status")).isEqualTo("OK");

        // Verify EDPS backend calls
        edpsBackendService.verify(
            request()
                .withMethod("POST")
                .withPath("/v1/dataspace/analysisjob")
        );

        edpsBackendService.verify(
            request()
                .withMethod("GET")
                .withPath("/v1/dataspace/analysisjob/40c70511-9427-43d1-811b-97231145cce1/status")
        );

        edpsBackendService.stop();
        sourceBackend.stop();
    }

    @Test
    void shouldAllowPublishUpdate_andDeleteDaseen() {
        var daseenBackendService = startClientAndServer(getFreePort());

        daseenBackendService.when(
            request()
                .withMethod("POST")
                .withPath("/connector/edp")
        ).respond(
            response()
                .withStatusCode(201)
                .withBody("{\"state\": \"SUCCESS\", \"id\": \"12345\", \"message\": \"EDPS connector created\"}")
        );

        Map<String, Object> daseenDataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s".formatted(daseenBackendService.getPort()),
                EDC_NAMESPACE + "proxyPath", "true",
                EDC_NAMESPACE + "proxyMethod", "true",
                EDC_NAMESPACE + "proxyQueryParams", "true",
                EDC_NAMESPACE + "proxyBody", "true"
        );

        // Prepare the contract agreement ID for the Daseen asset
        var daseenAssetId = PROVIDER.createOffer(daseenDataAddressProperties);
        
        var transferProcessId = CONSUMER.requestAssetFrom(daseenAssetId, PROVIDER)
                    .withTransferType("HttpData-PULL")
                    .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, STARTED);
        var daseenContractAgreementId = CONSUMER.getTransferProcess(transferProcessId).getString("contractId");

        // Create a new result asset
        var fileserver = startClientAndServer(getFreePort());
        fileserver.when(request("/results_asset"));
        Map<String, Object> dataAddressProperties = Map.of(
                    EDC_NAMESPACE + "type", "HttpData",
                    EDC_NAMESPACE + "baseUrl", "http://localhost:%s/results_asset".formatted(fileserver.getPort())
            );
        var resultAssetId = CONSUMER.createOffer(dataAddressProperties);

        CONSUMER.publishDassen(resultAssetId, daseenContractAgreementId);

        // Verify Daseen backend calls
        daseenBackendService.verify(
            request()
                .withMethod("POST")
                .withPath("/connector/edp")
        );

        daseenBackendService.stop();
        fileserver.stop();
    }

}
