package io.nexyo.edp.extensions.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexyo.edp.extensions.dtos.external.DaseenCreateResourceResponseDto;
import io.nexyo.edp.extensions.dtos.internal.DaseenResourceDto;
import io.nexyo.edp.extensions.exceptions.EdpException;
import io.nexyo.edp.extensions.utils.ConfigurationUtils;
import io.nexyo.edp.extensions.utils.LoggingUtils;
import io.nexyo.edp.extensions.utils.MockUtils;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;

/**
 * DaseenService
 */
public class DaseenService {

        private final Monitor logger;
        private final Client httpClient = ClientBuilder.newClient();
        private final ObjectMapper mapper = new ObjectMapper();
        private final DataplaneService dataplaneService;
        private final EdrService edrService;

        /**
         * Constructor for the DaseenService.
         *
         * @param dataplaneService the dataplane service
         */
        public DaseenService(DataplaneService dataplaneService, EdrService edrService) {
                this.logger = LoggingUtils.getLogger();
                this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                this.dataplaneService = dataplaneService;
                this.edrService = edrService;
        }

        /**
         * Creates a Daseen resource.
         *
         * @param assetId the asset ID to create the resource for.
         * @return the DaseenCreateResourceResponseDto
         */
        public DaseenCreateResourceResponseDto createDaseenResource(String assetId, String contractId) {
                this.logger.info(String.format("Creating Daseen Resource for Asset: %s...", assetId));
                final var daseenBaseUrlFromContract = this.edrService.getEdrProperty(contractId,
                                ConfigurationUtils.EDR_PROPERTY_EDPS_BASE_URL_KEY);
                final var daseenAuthorizationFromContract = this.edrService.getEdrProperty(contractId,
                                ConfigurationUtils.EDR_PROPERTY_EDPS_AUTH_KEY);

                var jsonb = JsonbBuilder.create();
                var requestBody = MockUtils.createRequestBody(assetId);
                String jsonRequestBody = jsonb.toJson(requestBody);

                var apiResponse = httpClient.target(String.format("%s/connector/edp", daseenBaseUrlFromContract))
                                .request(MediaType.APPLICATION_JSON)
                                .header("Authorization", daseenAuthorizationFromContract)
                                .post(Entity.entity(jsonRequestBody, MediaType.APPLICATION_JSON));

                if (!(apiResponse.getStatus() >= 200 && apiResponse.getStatus() < 300)) {
                        this.logger.warning("Failed to create EDP entry in Daseen for asset id: " + assetId
                                        + ". Status was: "
                                        + apiResponse.getStatus());
                        throw new EdpException("EDPS job creation failed for asset id: " + assetId);
                }

                String responseBody = apiResponse.readEntity(String.class);

                try {
                        return this.mapper.readValue(responseBody, DaseenCreateResourceResponseDto.class);
                } catch (JsonProcessingException e) {
                        throw new EdpException("Unable to map response to DTO ", e);
                }
        }

        /**
         * Publishes the EDPS job result to Daseen.
         *
         * @param daseenResourceDto the DaseenResourceDto to be published.
         */
        public void publishToDaseen(DaseenResourceDto daseenResourceDto) {
                this.logger
                                .info(String.format("Publishing Resource for Asset %s to Daseen...",
                                                daseenResourceDto.getAssetId()));
                final var daseenBaseUrlFromContract = this.edrService.getEdrProperty(daseenResourceDto.getContractId(),
                                ConfigurationUtils.EDR_PROPERTY_EDPS_BASE_URL_KEY);
                final var daseenAuthorizationFromContract = this.edrService.getEdrProperty(
                                daseenResourceDto.getContractId(),
                                ConfigurationUtils.EDR_PROPERTY_EDPS_AUTH_KEY);

                var destinationAddress = HttpDataAddress.Builder.newInstance()
                                .type(FlowType.PUSH.toString())
                                .method(HttpMethod.POST)
                                .addAdditionalHeader("Authorization", daseenAuthorizationFromContract)
                                .baseUrl(String.format("%s/connector/edp/%s", daseenBaseUrlFromContract,
                                                daseenResourceDto.getResourceId()))
                                .build();

                var transferProcess = this.edrService.getCurrentTransferProcess(daseenResourceDto.getContractId());
                var participantId = this.edrService.getContractAgreement(daseenResourceDto.getContractId())
                                .getProviderId();

                this.dataplaneService.start(daseenResourceDto.getAssetId(), destinationAddress,
                                transferProcess.getId(), participantId, daseenResourceDto.getContractId());
        }

        /**
         * Updates the EDPS job result in Daseen.
         *
         * @param daseenResourceDto the DaseenResourceDto to be updated.
         */
        public void updateInDaseen(DaseenResourceDto daseenResourceDto) {
                this.logger.info(String.format("Updating Resource for Asset %s in Daseen...",
                                daseenResourceDto.getAssetId()));
                final var daseenBaseUrlFromContract = this.edrService.getEdrProperty(daseenResourceDto.getContractId(),
                                ConfigurationUtils.EDR_PROPERTY_EDPS_BASE_URL_KEY);
                final var daseenAuthorizationFromContract = this.edrService.getEdrProperty(
                                daseenResourceDto.getContractId(),
                                ConfigurationUtils.EDR_PROPERTY_EDPS_AUTH_KEY);

                var destinationAddress = HttpDataAddress.Builder.newInstance()
                                .type(FlowType.PUSH.toString())
                                .method(HttpMethod.PUT)
                                .addAdditionalHeader("Authorization", daseenAuthorizationFromContract)
                                .baseUrl(String.format("%s/connector/edp/%s", daseenBaseUrlFromContract,
                                                daseenResourceDto.getResourceId()))
                                .build();

                var participantId = this.edrService.getContractAgreement(daseenResourceDto.getContractId())
                                .getProviderId();
                var transferProcess = this.edrService.getCurrentTransferProcess(daseenResourceDto.getContractId());

                this.dataplaneService.start(daseenResourceDto.getAssetId(), destinationAddress,
                                transferProcess.getId(), participantId, daseenResourceDto.getContractId());
        }

        /**
         * Deletes the EDPS job result in Daseen.
         *
         * @param daseenResourceDto the DaseenResourceDto to be deleted.
         */
        public void deleteInDaseen(DaseenResourceDto daseenResourceDto) {
                this.logger
                                .info(String.format("Deleting EDP Entry in Daseen for Asset: %s...",
                                                daseenResourceDto.getAssetId()));
                final var daseenBaseUrlFromContract = this.edrService.getEdrProperty(daseenResourceDto.getContractId(),
                                ConfigurationUtils.EDR_PROPERTY_EDPS_BASE_URL_KEY);
                final var daseenAuthorizationFromContract = this.edrService.getEdrProperty(
                                daseenResourceDto.getContractId(),
                                ConfigurationUtils.EDR_PROPERTY_EDPS_AUTH_KEY);

                var apiResponse = httpClient
                                .target(String.format("%s/connector/edp/%s", daseenBaseUrlFromContract,
                                                daseenResourceDto.getResourceId()))
                                .request(MediaType.APPLICATION_JSON)
                                .header("Authorization", daseenAuthorizationFromContract)
                                .delete();

                if (!(apiResponse.getStatus() == 204 || apiResponse.getStatus() == 200)) {
                        this.logger.warning("Failed to delete EDP entry in Daseen for asset id: "
                                        + daseenResourceDto.getAssetId()
                                        + ". Status was: " + apiResponse.getStatus());
                        throw new EdpException(
                                        "EDPS job creation failed for asset id: " + daseenResourceDto.getAssetId());
                }
        }

        /**
         * Closes the HTTP client.
         */
        public void close() {
                this.logger.info("Closing HTTP client...");
                this.httpClient.close();
        }

}
