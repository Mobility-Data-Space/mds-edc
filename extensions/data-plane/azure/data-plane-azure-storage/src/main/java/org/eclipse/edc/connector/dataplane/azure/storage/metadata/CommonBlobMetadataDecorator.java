/*
 *  Copyright (c) 2023 Robert Bosch GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Robert Bosch GmbH - adaption for blob metadata
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage.metadata;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.util.string.StringUtils;

public class CommonBlobMetadataDecorator implements BlobMetadataDecorator {

    public static final String ORIGINAL_NAME = "originalName";
    public static final String REQUEST_ID = "requestId";
    public static final String PROCESS_ID = "processId";
    public static final String CONNECTOR_ID = "connectorId";
    public static final String PARTICIPANT_ID = "participantId";
    private final String componentId;
    private final String participantId;

    public CommonBlobMetadataDecorator(String participantId, String componentId) {
        this.participantId = participantId;
        this.componentId = componentId;
    }

    @Override
    public BlobMetadata.Builder decorate(DataFlowStartMessage request, DataSource.Part part, BlobMetadata.Builder builder) {
        builder.put(ORIGINAL_NAME, part.name())
                .put(REQUEST_ID, request.getId())
                .put(PROCESS_ID, request.getProcessId())
                .put(CONNECTOR_ID, componentId)
                .put(PARTICIPANT_ID, participantId);

        var dataAddress = request.getDestinationDataAddress();
        var correlationId = dataAddress.getStringProperty(AzureBlobStoreSchema.CORRELATION_ID);

        if (!StringUtils.isNullOrEmpty(correlationId)) {
            builder.put(AzureBlobStoreSchema.CORRELATION_ID, correlationId);
        }

        return builder;
    }
}
