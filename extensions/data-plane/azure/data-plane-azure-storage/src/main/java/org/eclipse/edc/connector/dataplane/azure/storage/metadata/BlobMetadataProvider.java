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
 * {@summary This service provides the user the possibility to register decorators to be used for metadata provisioning.}
 * The metadata is set on the transferred blob on consumer side.
 * The user should consume this interface only to register new decorators.
 * The provisioning is used in {@link org.eclipse.edc.connector.dataplane.azure.storage.pipeline.AzureStorageDataSink}
 */
public interface BlobMetadataProvider {

    /**
     * {@summary Register a decorator for providing metadata}
     *
     * @param decorator The decorator to be registered for additional metadata provisioning
     */
    void registerDecorator(BlobMetadataDecorator decorator);

    /**
     * {@summary Provision metadata for all registered decorators}
     *
     * @param request The data flow request to be passed as a source of information to each registered decorator
     * @param part    The transferred data part to be passed as a source of information to each registered decorator
     * @return The final set of metadata to be set on a destination blob
     */
    BlobMetadata provideSinkMetadata(DataFlowStartMessage request, DataSource.Part part);
}
