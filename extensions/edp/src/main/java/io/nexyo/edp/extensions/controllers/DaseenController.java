package io.nexyo.edp.extensions.controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexyo.edp.extensions.dtos.internal.DaseenCreateEntryRequestDto;
import io.nexyo.edp.extensions.dtos.internal.DaseenResourceDto;
import io.nexyo.edp.extensions.dtos.internal.GenericResponseDto;
import io.nexyo.edp.extensions.dtos.internal.Status;
import io.nexyo.edp.extensions.services.AssetHelperService;
import io.nexyo.edp.extensions.services.DaseenService;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.UUID;

/**
 * DaseenController
 */
public class DaseenController implements DaseenInterface {

    private final Monitor logger;
    private final DaseenService daseenService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AssetHelperService assetHelperService;
    private static final String CALLBACK_INFO = "Check specified dataplane-callback address for updates.";

    /**
     * Constructor for the DaseenController.
     *
     * @param daseenService the daseen service
     * @param assetService  the asset service
     * @param monitor       the monitor
     */
    public DaseenController(DaseenService daseenService, AssetService assetService, Monitor monitor) {
        this.logger = monitor;
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.daseenService = daseenService;
        this.assetHelperService = new AssetHelperService(assetService, monitor);
    }

    @Override
    public Response create(String assetId, DaseenCreateEntryRequestDto daseenCreateEntryRequestDto) {
        this.logger.info(String.format("Creating Daseen resource for EDP asset %s", assetId));
        var daseenResponseDto = this.daseenService.createDaseenResource(assetId,
                daseenCreateEntryRequestDto.contractId());
        var daseenResourceId = daseenResponseDto.id();
        var daseenResourceDto = new DaseenResourceDto(UUID.randomUUID().toString(), assetId, daseenResourceId,
                daseenCreateEntryRequestDto.contractId());

        this.assetHelperService.persist(assetId, AssetHelperService.DASEEN_RESOURCE_KEY, daseenResourceDto);

        this.daseenService.publishToDaseen(daseenResourceDto);
        final var response = new GenericResponseDto(
                "Publishing job for EDP result asset to Daseen dispatched to dataplane. " +
                        CALLBACK_INFO,
                Status.OK);

        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @Override
    public Response update(String assetId) {
        this.logger.info(String.format("Updating Daseen resource for asset with id %s", assetId));

        final var daseenResourceOptional = this.assetHelperService.load(assetId,
                AssetHelperService.DASEEN_RESOURCE_KEY, DaseenResourceDto.class);

        if (daseenResourceOptional.isEmpty()) {
            var response = new GenericResponseDto("No resource found for asset: " + assetId, Status.NOT_FOUND);
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }

        var daseenResourceDto = daseenResourceOptional.get();
        this.daseenService.updateInDaseen(daseenResourceDto);

        return Response.status(Response.Status.OK)
                .entity(new GenericResponseDto("Update job for Daseen resource dispatched to dataplane. " +
                        CALLBACK_INFO, Status.OK))
                .build();
    }

    @Override
    public Response delete(String assetId) {
        final var daseenResourceOptional = this.assetHelperService.load(assetId,
                AssetHelperService.DASEEN_RESOURCE_KEY, DaseenResourceDto.class);

        if (daseenResourceOptional.isEmpty()) {
            var response = new GenericResponseDto("No resource found for asset: " + assetId, Status.NOT_FOUND);
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }

        var daseenResourceDto = daseenResourceOptional.get();
        this.daseenService.deleteInDaseen(daseenResourceDto);

        return Response.status(Response.Status.OK)
                .entity(new GenericResponseDto("Resource deleted successfully. " +
                        CALLBACK_INFO, Status.OK))
                .build();
    }

}
