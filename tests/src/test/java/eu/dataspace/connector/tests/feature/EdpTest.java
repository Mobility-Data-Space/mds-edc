package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.PostgresqlExtension;
import eu.dataspace.connector.tests.SovityDapsExtension;
import eu.dataspace.connector.tests.VaultExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class EdpTest {

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
    private static final MdsParticipant PROVIDER = MdsParticipantFactory.edp("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

    @RegisterExtension
    private static final MdsParticipant CONSUMER = MdsParticipantFactory.edp("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

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
                .withBody("{\"job_id\": \"40c70511-9427-43d1-811b-97231145cce1\", \"state\": \"WAITING_FOR_DATA\", \"state_detail\": \"Job is waiting for data to be uploaded.\", \"upload_url\": \"http://edps-base-url/api/40c70511-9427-43d1-811b-97231145cce1\"}")
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
                .withPath("/connector/edp/")
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
                .withPath("/connector/edp/")
        );

        daseenBackendService.stop();
        fileserver.stop();
    }

}
