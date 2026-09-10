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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

/**
 * {@summary For a user of this interface, it represents a method to add additional metadata fields to destination blob}
 * The metadata is set after it was transferred to the consumer side. As possible sources for metadata, e.h. the dat
 * flow request and the data source part can be used.
 * An example use case is to add information about participant ids or correlation ids to the blob, in order to simplify
 * further processing of the blob on consumer side. An implementation is provided in {@link CommonBlobMetadataDecorator}.
 */
public interface BlobMetadataDecorator {

    /**
     * {@summary Provide additional metadata fields using the supplied builder and the supplied sources (see arguments).}
     *
     * @param request The data flow request; intended to be used as source of information for new metadata fields
     * @param part    The transferred file part; intended to be used as source of information for new metadata fields
     * @param builder The builder class to be used to add metadata fields
     * @return The builder class must be again returned from this method
     */
    BlobMetadata.Builder decorate(DataFlowStartMessage request, DataSource.Part part, BlobMetadata.Builder builder);
}
