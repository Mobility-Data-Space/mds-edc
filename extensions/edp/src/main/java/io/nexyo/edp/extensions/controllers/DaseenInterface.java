package io.nexyo.edp.extensions.controllers;

import io.nexyo.edp.extensions.dtos.internal.DaseenCreateEntryRequestDto;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * RESTful interface for managing Daseen resources.
 */
@Path("/edp/daseen")
public interface DaseenInterface {

    /**
     * Publishes an asset to the Daseen API.
     *
     * @param assetId The unique identifier of the asset to be published.
     * @return A {@link Response} indicating the success or failure of the
     *         publication process.
     */
    @POST
    @Path("/{assetId}")
    Response create(@PathParam("assetId") String assetId, DaseenCreateEntryRequestDto daseenCreateEntryRequestDto);

    /**
     * Updates a Daseen resource.
     *
     * @param assetId The unique identifier of the asset to be updated.
     * @return A {@link Response} indicating the success or failure of the update
     *         process.
     */
    @PUT
    @Path("/{assetId}")
    Response update(@PathParam("assetId") String assetId);

    /**
     * Deletes a Daseen resource.
     *
     * @param assetId The unique identifier of the asset to be deleted.
     * @return A {@link Response} indicating the success or failure of the deletion
     *         process.
     */
    @DELETE
    @Path("/{assetId}")
    Response delete(@PathParam("assetId") String assetId);

}
