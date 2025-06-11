package io.nexyo.edp.extensions.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexyo.edp.extensions.dtos.external.EdpsJobResponseDto;
import io.nexyo.edp.extensions.dtos.internal.EdpsJobDto;
import io.nexyo.edp.extensions.dtos.internal.EdpsResultRequestDto;
import io.nexyo.edp.extensions.exceptions.EdpException;
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

    public static final String EDR_PROPERTY_EDPS_BASE_URL_KEY = "https://w3id.org/edc/v0.0.1/ns/endpoint";
    public static final String EDR_PROPERTY_EDPS_AUTH_KEY = "https://w3id.org/edc/v0.0.1/ns/authorization";

    /**
     * Constructs an instance of EdpsService.
     *
     * @param dataplaneService the service responsible for handling data transfers.
     * @param monitor          the monitor
     */
    public EdpsService(DataplaneService dataplaneService, EdrService edrService, Monitor monitor) {
        this.logger = monitor;
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
        this.logger.debug(String.format("Creating EDP job for %s...", assetId));
        final var edpsBaseUrlFromContract = this.edrService.getEdrProperty(contractId,
                        EDR_PROPERTY_EDPS_BASE_URL_KEY);
        final var edpsAuthorizationFromContract = this.edrService.getEdrProperty(contractId,
                        EDR_PROPERTY_EDPS_AUTH_KEY);

        var jsonb = JsonbBuilder.create();
        var requestBody = MockUtils.createRequestBody(assetId);
        String jsonRequestBody = jsonb.toJson(requestBody);

        var apiResponse = httpClient
                        .target(String.format("%s%s", edpsBaseUrlFromContract, "/v1/dataspace/analysisjob"))
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", edpsAuthorizationFromContract)
                        .post(Entity.entity(jsonRequestBody, MediaType.APPLICATION_JSON));

        if (!(apiResponse.getStatus() >= 200 && apiResponse.getStatus() <= 300)) {
                this.logger.warning("Failed to create EDPS job for asset id: " + assetId + ". Status was: "
                                                + apiResponse.getStatus());
                throw new EdpException("EDPS job creation failed for asset id: " + assetId);
        }

        var responseBody = apiResponse.readEntity(String.class);
        this.logger.debug("EDPS job created successfully for asset id: " + assetId + ". Edps Server responded: "
            + responseBody);
        try {
            var edpsJobResponseDto = this.mapper.readValue(responseBody, EdpsJobResponseDto.class);

            // map upload url to correct url
            String baseUrl = edpsJobResponseDto.uploadUrl().replace("/api/", "/v1/dataspace/analysisjob/");
            String uploadUrl = baseUrl + "/data/file.csv";
            String resultUrl = baseUrl + "/result";
            edpsJobResponseDto = new EdpsJobResponseDto(
                    edpsJobResponseDto.jobUuid(),
                    edpsJobResponseDto.state(),
                    edpsJobResponseDto.details(),
                    uploadUrl, resultUrl
            );

            return edpsJobResponseDto;
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
        this.logger.debug(String.format("Fetching EDPS Job status for job %s...", jobId));
        final var edpsBaseUrl = this.edrService.getEdrProperty(contractId,
                        EDR_PROPERTY_EDPS_BASE_URL_KEY);
        final var edpsAuthorizationFromContract = this.edrService.getEdrProperty(contractId,
                        EDR_PROPERTY_EDPS_AUTH_KEY);

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

        var destinationAddress = HttpDataAddress.Builder.newInstance()
                        .type(FlowType.PUSH.toString())
                        .property("header:accept", "application/json")
                        .baseUrl(edpsJobDto.getUploadUrl())
                        .build();

        this.dataplaneService.start(edpsJobDto.getAssetId(), destinationAddress, transferProcess.getId(),
                        participantId,
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
        this.logger.debug(String.format("Fetching EDPS Job Result ZIP for asset %s for job %s...",
                                edpsJobDto.getAssetId(), edpsJobDto.getAssetId()));
        var contractId = edpsJobDto.getContractId();

        // TODO: check if transfer process is still running
        var transferProcess = this.edrService.getCurrentTransferProcess(contractId);
        var participantId = this.edrService.getContractAgreement(contractId).getProviderId();

        var sourceAddress = HttpDataAddress.Builder.newInstance()
                        .type(FlowType.PULL.toString())
                        .baseUrl(edpsJobDto.getResultUrl())
                        .build();

        var destinationAddress = HttpDataAddress.Builder.newInstance()
                        .type(FlowType.PUSH.toString())
                        .baseUrl(edpResultRequestDto.destinationAddress())
                        .build();

        this.dataplaneService.start(sourceAddress, destinationAddress, transferProcess.getId(), participantId,
                        contractId);
    }

    /**
     * Closes the HTTP client.
     */
    public void close() {
        this.logger.debug("Closing HTTP client...");
        this.httpClient.close();
    }

}
