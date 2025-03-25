package io.nexyo.edp.extensions.controllers;

import io.nexyo.edp.extensions.dtos.internal.EdpsCreateJobRequestDto;
import io.nexyo.edp.extensions.dtos.internal.EdpsResultRequestDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * RESTful interface for managing EDPS jobs and their results.
 * Provides endpoints for creating, monitoring, and retrieving results from EDPS
 * jobs.
 */
@Path("/edp/edps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EdpsInterface {

    /**
     * Retrieves all EDPS jobs associated with a specific asset.
     *
     * @param assetId The unique identifier of the asset to retrieve jobs for
     * @return Response containing the list of EDPS jobs for the specified asset
     */
    @GET
    @Path("/{assetId}/jobs")
    Response getEdpsJob(@PathParam("assetId") String assetId);

    /**
     * Creates a new EDPS job for a specific asset and submits the associated file
     * to EDPS.
     *
     * @param assetId                 The unique identifier of the asset to create a
     *                                job for
     * @param edpsCreateJobRequestDto The request payload containing create job
     *                                parameters
     * @return Response containing the details of the created job
     */
    @POST
    @Path("/{assetId}/jobs")
    Response createEdpsJob(@PathParam("assetId") String assetId, EdpsCreateJobRequestDto edpsCreateJobRequestDto);

    /**
     * Retrieves the current status of a specific EDPS job.
     *
     * @param assetId The unique identifier of the asset associated with the job
     * @param jobId   The unique identifier of the job to check status for
     * @return Response containing the current status of the specified job
     */
    @GET
    @Path("/{assetId}/jobs/{jobId}/status")
    Response getEdpsJobStatus(@PathParam("assetId") String assetId,
            @PathParam("jobId") String jobId);

    /**
     * Retrieves the result from EDPS and stores the result file in the system.
     *
     * @param assetId              The unique identifier of the original asset
     * @param jobId                The unique identifier of the completed job
     * @param edpsResultRequestDto The request payload containing result processing
     *                             parameters
     * @return Response containing the details of the newly created result asset
     */
    @POST
    @Path("/{assetId}/jobs/{jobId}/result")
    Response fetchEdpsJobResult(@PathParam("assetId") String assetId,
            @PathParam("jobId") String jobId,
            EdpsResultRequestDto edpsResultRequestDto);

}