package io.nexyo.edp.extensions.controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexyo.edp.extensions.dtos.internal.*;
import io.nexyo.edp.extensions.services.AssetHelperService;
import io.nexyo.edp.extensions.services.EdpsService;
import io.nexyo.edp.extensions.utils.LoggingUtils;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Controller class for handling EDP-related operations.
 */
public class EdpsController implements EdpsInterface {

        private final Monitor logger;
        private final EdpsService edpsService;
        private final ObjectMapper mapper = new ObjectMapper();
        private static final String CALLBACK_INFO = "Check specified dataplane-callback address for updates.";
        private final AssetHelperService assetHelperService;

        /**
         * Constructs an instance of EdpsController.
         *
         *
         * @param edpsService  the service responsible for handling EDPS operations
         * @param assetService the service responsible for handling asset operations
         */
        public EdpsController(EdpsService edpsService, AssetService assetService) {
                this.logger = LoggingUtils.getLogger();
                this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                this.edpsService = edpsService;
                this.assetHelperService = new AssetHelperService(assetService);
        }

        @Override
        public Response getEdpsJob(String assetId) {
                logger.info("Getting latest EDP job for asset " + assetId);

                var jobDtoOptional = this.assetHelperService.load(assetId, AssetHelperService.EDPS_JOB_KEY,
                                EdpsJobDto.class);
                if (jobDtoOptional.isEmpty()) {
                        var response = new GenericResponseDto("No Job found for asset: " + assetId, Status.NOT_FOUND);
                        return Response.status(Response.Status.NOT_FOUND).entity(response).build();
                }

                var edpsJobDto = jobDtoOptional.get();

                var edpsJobResponseDto = this.edpsService.getEdpsJobStatus(edpsJobDto.getJobId(),
                                edpsJobDto.getContractId());

                edpsJobDto.setState(edpsJobResponseDto.state());
                edpsJobDto.setDetails(edpsJobResponseDto.details());

                this.assetHelperService.persist(assetId, AssetHelperService.EDPS_JOB_KEY, edpsJobDto);

                return Response.status(Response.Status.OK)
                                .entity(edpsJobDto)
                                .build();
        }

        @Override
        public Response getEdpsJobStatus(String assetId, String jobId) {
                return this.getEdpsJob(assetId);
        }

        @Override
        public Response createEdpsJob(String assetId, EdpsCreateJobRequestDto edpsCreateJobRequestDto) {
                logger.info("Creating EDP job...");
                var edpsJobResponseDto = this.edpsService.createEdpsJob(assetId, edpsCreateJobRequestDto.contractId());
                var edpsJobDto = mapper.convertValue(edpsJobResponseDto, EdpsJobDto.class);
                edpsJobDto.setAssetId(assetId);
                edpsJobDto.setDetails("Posting analysis data to EDPS initiated. " + CALLBACK_INFO);
                edpsJobDto.setContractId(edpsCreateJobRequestDto.contractId());

                this.assetHelperService.persist(assetId, AssetHelperService.EDPS_JOB_KEY, edpsJobDto);

                this.edpsService.sendAnalysisData(edpsJobDto);

                return Response.status(Response.Status.OK)
                                .entity(edpsJobDto)
                                .build();
        }

        @Override
        public Response fetchEdpsJobResult(String assetId, String jobId, EdpsResultRequestDto edpResultRequestDto) {
                logger.info("Storing EDP result ZIP to destination address..."
                                + edpResultRequestDto.destinationAddress());

                var jobDtoOptional = this.assetHelperService.load(assetId, AssetHelperService.EDPS_JOB_KEY,
                                EdpsJobDto.class);
                if (jobDtoOptional.isEmpty()) {
                        var response = new GenericResponseDto("No Job found for asset: " + assetId, Status.NOT_FOUND);
                        return Response.status(Response.Status.NOT_FOUND).entity(response).build();
                }

                var edpsJobDto = jobDtoOptional.get();

                var edpsJobResponseDto = this.edpsService.getEdpsJobStatus(edpsJobDto.getJobId(),
                                edpsJobDto.getContractId());

                edpsJobDto.setState(edpsJobResponseDto.state());
                edpsJobDto.setDetails(edpsJobResponseDto.details());
                this.assetHelperService.persist(assetId, AssetHelperService.EDPS_JOB_KEY, edpsJobDto);

                this.edpsService.fetchEdpsJobResult(edpsJobDto, edpResultRequestDto);

                final var response = new GenericResponseDto(
                                "Storing EDPS asset to destination address initiated. " +
                                                CALLBACK_INFO,
                                Status.OK);

                return Response.status(Response.Status.OK)
                                .entity(response)
                                .build();
        }
}
