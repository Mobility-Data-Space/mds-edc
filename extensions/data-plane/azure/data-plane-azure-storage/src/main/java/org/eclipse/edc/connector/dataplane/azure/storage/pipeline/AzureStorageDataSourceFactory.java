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

package org.eclipse.edc.connector.dataplane.azure.storage.pipeline;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_PREFIX;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateBlobName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateBlobPrefix;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateContainerName;

/**
 * Instantiates {@link AzureStorageDataSource}s for requests whose source data type is {@link AzureBlobStoreSchema#TYPE}.
 */
public class AzureStorageDataSourceFactory implements DataSourceFactory {
    private final BlobStoreApi blobStoreApi;
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final Vault vault;

    public AzureStorageDataSourceFactory(BlobStoreApi blobStoreApi, RetryPolicy<Object> retryPolicy, Monitor monitor, Vault vault) {
        this.blobStoreApi = blobStoreApi;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.vault = vault;
    }

    @Override
    public String supportedType() {
        return AzureBlobStoreSchema.TYPE;
    }

    @Override
    public DataSource createSource(DataFlowStartMessage request) {
        validateRequest(request).orElseThrow(f -> new EdcException(f.getFailureDetail()));

        var dataAddress = request.getSourceDataAddress();

        var builder = AzureStorageDataSource.Builder.newInstance()
                .accountName(dataAddress.getStringProperty(ACCOUNT_NAME))
                .containerName(dataAddress.getStringProperty(CONTAINER_NAME))
                .blobStoreApi(blobStoreApi)
                .blobName(dataAddress.getStringProperty(BLOB_NAME))
                .blobPrefix(dataAddress.getStringProperty(BLOB_PREFIX))
                .requestId(request.getId())
                .retryPolicy(retryPolicy)
                .monitor(monitor);

        Optional.ofNullable(dataAddress.getKeyName())
                .filter(it -> !it.isBlank())
                .ifPresentOrElse(keyName -> {
                    monitor.debug("Attempting to use shared key authentication for Azure Storage data source");
                    builder.sharedKey(vault.resolveSecret(dataAddress.getKeyName()));
                }, () -> monitor.debug("Attempting to use default identity for Azure Storage data source"));

        return builder.build();
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        var dataAddress = request.getSourceDataAddress();
        try {
            validateAccountName(dataAddress.getStringProperty(ACCOUNT_NAME));
            validateContainerName(dataAddress.getStringProperty(CONTAINER_NAME));
            if (dataAddress.hasProperty(BLOB_PREFIX)) {
                validateBlobPrefix(dataAddress.getStringProperty(BLOB_PREFIX));
            } else {
                validateBlobName(dataAddress.getStringProperty(BLOB_NAME));
            }
        } catch (IllegalArgumentException e) {
            return Result.failure("AzureStorage source address is invalid: " + e.getMessage());
        }
        return Result.success();
    }
}
