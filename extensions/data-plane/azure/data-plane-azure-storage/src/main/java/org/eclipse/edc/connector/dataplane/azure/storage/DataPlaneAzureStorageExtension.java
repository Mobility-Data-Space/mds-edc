/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProvider;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProviderImpl;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator;
import org.eclipse.edc.connector.dataplane.azure.storage.pipeline.AzureStorageDataSinkFactory;
import org.eclipse.edc.connector.dataplane.azure.storage.pipeline.AzureStorageDataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Provides support for reading data from an Azure Storage Blob endpoint and sending data to an Azure Storage Blob endpoint.
 */
@Extension(value = DataPlaneAzureStorageExtension.NAME)
public class DataPlaneAzureStorageExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Azure Storage";

    @Setting(description = "Configures the participant id this runtime is operating on behalf of", key = "edc.participant.id", defaultValue = "anonymous")
    public String participantId;

    @Inject
    private RetryPolicy<Object> retryPolicy;
    @Inject
    private PipelineService pipelineService;
    @Inject
    private BlobStoreApi blobStoreApi;
    @Inject
    private DataTransferExecutorServiceContainer executorContainer;
    @Inject
    private Vault vault;
    @Inject
    private TypeManager typeManager;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var metadataProvider = new BlobMetadataProviderImpl(monitor);
        context.registerService(BlobMetadataProvider.class, metadataProvider);
        metadataProvider.registerDecorator(new CommonBlobMetadataDecorator(participantId, context.getComponentId()));

        var sourceFactory = new AzureStorageDataSourceFactory(blobStoreApi, retryPolicy, monitor, vault);
        pipelineService.registerFactory(sourceFactory);
        var sinkFactory = new AzureStorageDataSinkFactory(participantContextSupplier, blobStoreApi, executorContainer.getExecutorService(), 5, monitor, vault, typeManager, metadataProvider);
        pipelineService.registerFactory(sinkFactory);
    }
}
