package io.nexyo.edp.extensions.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexyo.edp.extensions.dtos.external.EdpsJobResponseDto;
import io.nexyo.edp.extensions.dtos.internal.EdpsJobDto;
import io.nexyo.edp.extensions.dtos.internal.EdpsResultRequestDto;
import io.nexyo.edp.extensions.exceptions.EdpException;
import io.nexyo.edp.extensions.utils.ConfigurationUtils;
import io.nexyo.edp.extensions.utils.LoggingUtils;
import io.nexyo.edp.extensions.utils.MockUtils;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;

/**
 * Service class responsible for handling EDPS-related operations.
 */
public class EdpsService {

    private final Monitor logger;
    private final Client httpClient = ClientBuilder.newClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final DataplaneService dataplaneService;
    private final EdrService edrService;

    /**
     * Constructs an instance of EdpsService.
     *
     * @param dataplaneService the service responsible for handling data transfers.
     */
    public EdpsService(DataplaneService dataplaneService, EdrService edrService) {
        this.logger = LoggingUtils.getLogger();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.dataplaneService = dataplaneService;
        this.edrService = edrService;
    }

    /**
     * Creates a new EDPS job for the specified asset ID.
     *
     * @param assetId the asset ID for which the job is created.
     * @return the response DTO containing job details.
     * @throws EdpException if the job creation fails.
     */
    public EdpsJobResponseDto createEdpsJob(String assetId, String contractId) {
        this.logger.info(String.format("Creating EDP job for %s...", assetId));
        final var edpsBaseUrlFromContract = this.edrService.getEdrProperty(contractId,
                ConfigurationUtils.EDR_PROPERTY_EDPS_BASE_URL_KEY);
        final var edpsAuthorizationFromContract = this.edrService.getEdrProperty(contractId,
                ConfigurationUtils.EDR_PROPERTY_EDPS_AUTH_KEY);

        var jsonb = JsonbBuilder.create();
        var requestBody = MockUtils.createRequestBody(assetId);
        String jsonRequestBody = jsonb.toJson(requestBody);

        var apiResponse = httpClient.target(String.format("%s%s", edpsBaseUrlFromContract, "/v1/dataspace/analysisjob"))
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", edpsAuthorizationFromContract)
                .post(Entity.entity(jsonRequestBody, MediaType.APPLICATION_JSON));

        if (!(apiResponse.getStatus() >= 200 && apiResponse.getStatus() <= 300)) {
            this.logger.warning(
                    "Failed to create EDPS job for asset id: " + assetId + ". Status was: " + apiResponse.getStatus());
            throw new EdpException("EDPS job creation failed for asset id: " + assetId);
        }

        var responseBody = apiResponse.readEntity(String.class);
        this.logger.info(
                "EDPS job created successfully for asset id: " + assetId + ". Edps Server responded: " + responseBody);

        try {
            return this.mapper.readValue(responseBody, EdpsJobResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new EdpException("Unable to map response to DTO ", e);
        }
    }

    /**
     * Retrieves the status of an existing EDPS job.
     *
     * @param jobId the job ID.
     * @return the response DTO containing job status details.
     * @throws EdpException if the request fails.
     */
    public EdpsJobResponseDto getEdpsJobStatus(String jobId, String contractId) {
        this.logger.info(String.format("Fetching EDPS Job status for job %s...", jobId));
        final var edpsBaseUrl = this.edrService.getEdrProperty(contractId,
                ConfigurationUtils.EDR_PROPERTY_EDPS_BASE_URL_KEY);
        final var edpsAuthorizationFromContract = this.edrService.getEdrProperty(contractId,
                ConfigurationUtils.EDR_PROPERTY_EDPS_AUTH_KEY);

        var apiResponse = this.httpClient
                .target(String.format("%s/v1/dataspace/analysisjob/%s/status", edpsBaseUrl, jobId))
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", edpsAuthorizationFromContract)
                .get();

        if (apiResponse.getStatus() < 200 || apiResponse.getStatus() >= 300) {
            String errorMessage = apiResponse.readEntity(String.class);
            this.logger.warning("Failed to fetch EDPS job status: " + errorMessage);
            throw new EdpException("Failed to fetch EDPS job status: " + errorMessage);
        }

        String responseBody = apiResponse.readEntity(String.class);

        try {
            return this.mapper.readValue(responseBody, EdpsJobResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new EdpException("Unable to map response to DTO ", e);
        }
    }

    /**
     * Sends analysis data for a given EDPS job.
     *
     * @param edpsJobDto the job DTO containing job details.
     */
    public void sendAnalysisData(EdpsJobDto edpsJobDto) {
        var contractId = edpsJobDto.getContractId();
        var transferProcess = this.edrService.getCurrentTransferProcess(contractId);
        var participantId = this.edrService.getContractAgreement(contractId).getProviderId();

        final var edpsBaseUrl = this.edrService.getEdrProperty(contractId,
                ConfigurationUtils.EDR_PROPERTY_EDPS_BASE_URL_KEY);
        final var edpsAuthorizationFromContract = this.edrService.getEdrProperty(contractId,
                ConfigurationUtils.EDR_PROPERTY_EDPS_AUTH_KEY);

        var destinationAddress = HttpDataAddress.Builder.newInstance()
                .type(FlowType.PUSH.toString())
                .addAdditionalHeader("Authorization", edpsAuthorizationFromContract)
                .baseUrl(String.format("%s/v1/dataspace/analysisjob/%s/data/file", edpsBaseUrl, edpsJobDto.getJobId()))
                .build();

        this.dataplaneService.start(edpsJobDto.getAssetId(), destinationAddress, transferProcess.getId(), participantId,
                contractId);
    }

    /**
     * Fetches the result of an EDPS job.
     *
     * @param edpsJobDto          the asset ID.
     * @param edpResultRequestDto the request DTO containing result destination
     *                            details.
     */
    public void fetchEdpsJobResult(EdpsJobDto edpsJobDto, EdpsResultRequestDto edpResultRequestDto) {
        this.logger.info(String.format("Fetching EDPS Job Result ZIP for asset %s for job %s...",
                edpsJobDto.getAssetId(), edpsJobDto.getAssetId()));
        var contractId = edpsJobDto.getContractId();
        final var edpsBaseUrl = this.edrService.getEdrProperty(contractId,
                ConfigurationUtils.EDR_PROPERTY_EDPS_BASE_URL_KEY);
        var edpsAuthorizationFromContract = this.edrService.getEdrProperty(contractId,
                ConfigurationUtils.EDR_PROPERTY_EDPS_AUTH_KEY);

        // TODO: Überlegung: Wir müssen nur aufpassen, dass der trnsfer process dann
        // nicht terminiert, vielleicht sollten wir für jeden transfer einen eigenen
        // transfer process spawnen, ist halt async
        var transferProcess = this.edrService.getCurrentTransferProcess(contractId);
        var participantId = this.edrService.getContractAgreement(contractId).getProviderId();

        var sourceAddress = HttpDataAddress.Builder.newInstance()
                .type(FlowType.PULL.toString())
                .addAdditionalHeader("Authorization", edpsAuthorizationFromContract)
                .baseUrl(String.format("%s/v1/dataspace/analysisjob/%s/result", edpsBaseUrl, edpsJobDto.getJobId()))
                .build();

        var destinationAddress = HttpDataAddress.Builder.newInstance()
                .type(FlowType.PUSH.toString())
                // .addAdditionalHeader("Authorization", edpsAuthorizationFromContract)
                .baseUrl(edpResultRequestDto.destinationAddress())
                .build();

        this.dataplaneService.start(sourceAddress, destinationAddress, transferProcess.getId(), participantId,
                contractId);
    }

    /**
     * Closes the HTTP client.
     */
    public void close() {
        this.logger.info("Closing HTTP client...");
        this.httpClient.close();
    }

}
