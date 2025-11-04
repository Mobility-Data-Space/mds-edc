/*
 * Copyright (c) 2025 Mobility Data Space
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *      Think-it GmbH - initial API and implementation
 */

package eu.dataspace.issuer.api.attestation.database;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.ServiceResultHandler;

import java.util.Base64;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Path("/v1alpha/participants/{participantContextId}/holders")
public class DatabaseAttestationApiController {

    private final DatabaseAttestationSourceStore store;

    public DatabaseAttestationApiController(DatabaseAttestationSourceStore store) {
        this.store = store;
    }

    @POST
    @Path("/{encodedHolderId}/attestation")
    @Produces(APPLICATION_JSON)
    public void createAttestation(@PathParam("participantContextId") String participantContextId,
                                  @PathParam("encodedHolderId") String encodedHolderId,
                                  @Context SecurityContext context,
                                  HolderAttestationDto holderAttestationDto) {

        var holderId = new String(Base64.getDecoder().decode(encodedHolderId));
        store.create(new HolderAttestation(holderId, holderAttestationDto.participantName(), holderAttestationDto.membershipType(), holderAttestationDto.membershipStartDate()))
                .flatMap(ServiceResult::from)
                .orElseThrow(ServiceResultHandler.exceptionMapper(HolderAttestation.class, holderId));

    }
}
