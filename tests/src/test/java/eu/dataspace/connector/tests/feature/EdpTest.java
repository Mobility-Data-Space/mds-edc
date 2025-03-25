package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;

import java.util.Map;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

public class EdpTest {

    private static final MdsParticipant PROVIDER = MdsParticipant.Builder.newInstance()
            .id("edps_daseen").name("edps_daseen")
            .build();

    private static final MdsParticipant CONSUMER = MdsParticipant.Builder.newInstance()
            .id("data_holder").name("data_holder")
            .build();

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_EXTENSION = new RuntimePerClassExtension(
            new EmbeddedRuntime("provider", ":launchers:connector-inmemory-edp")
                    .configurationProvider(PROVIDER::getConfiguration)
                    .configurationProvider(() 
                        -> ConfigFactory.fromMap(Map.ofEntries(entry("edp.dataplane.callback.url", "http://localhost"))))
                    .registerSystemExtension(ServiceExtension.class, PROVIDER.seedVaultKeys())
    );
    
    @RegisterExtension
    private static final RuntimeExtension CONSUMER_EXTENSION = new RuntimePerClassExtension(
            new EmbeddedRuntime("consumer", ":launchers:connector-inmemory-edp")
                    .configurationProvider(CONSUMER::getConfiguration)
                    .configurationProvider(() 
                        -> ConfigFactory.fromMap(Map.ofEntries(entry("edp.dataplane.callback.url", "http://localhost"))))
                    .registerSystemExtension(ServiceExtension.class, CONSUMER.seedVaultKeys())
    );
    

    @Test
    void shouldAllowEDPSJob_andResultAsset() {
        var edpsBackendService = startClientAndServer(getFreePort());
        // Register EDPS endpoints
        edpsBackendService.when(
            request()
                .withMethod("POST")
                .withPath("/v1/dataspace/analysisjob")
        ).respond(
            response()
                .withStatusCode(201)
                .withBody("{\"job_id\": \"40c70511-9427-43d1-811b-97231145cce1\", \"state\": \"WAITING_FOR_DATA\", \"state_detail\": \"Job is waiting for data to be uploaded.\"}")
        );

        edpsBackendService.when(
            request()
                .withMethod("GET")
                .withPath("/v1/dataspace/analysisjob/40c70511-9427-43d1-811b-97231145cce1/result")
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/zip")
                .withBody(new byte[]{}) // Simulating an empty zip file
        );

        edpsBackendService.when(
            request()
                .withMethod("GET")
                .withPath("/v1/dataspace/analysisjob/40c70511-9427-43d1-811b-97231145cce1/status")
        ).respond(
            response()
                .withStatusCode(200)
                .withBody("{\"job_id\": \"40c70511-9427-43d1-811b-97231145cce1\", \"state\": \"WAITING_FOR_DATA\", \"state_detail\": \"Job is waiting for data to be uploaded.\"}")
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

        assert edpsResults != null : "EDPS results should not be null";

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

        // Update the published asset

        // Delete the published asset

        daseenBackendService.stop();
        fileserver.stop();
    }

}
