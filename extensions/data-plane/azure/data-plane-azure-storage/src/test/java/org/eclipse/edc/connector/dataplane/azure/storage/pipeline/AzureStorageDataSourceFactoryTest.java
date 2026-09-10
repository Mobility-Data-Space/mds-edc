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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobPrefix;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createSharedKey;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureStorageDataSourceFactoryTest {
    private final BlobStoreApi blobStoreApi = mock();
    private final Vault vault = mock();
    private final AzureStorageDataSourceFactory factory = new AzureStorageDataSourceFactory(blobStoreApi, RetryPolicy.ofDefaults(), mock(Monitor.class), vault);
    private final DataFlowStartMessage.Builder request = createRequest(AzureBlobStoreSchema.TYPE);
    private final DataAddress.Builder dataAddress = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE);

    private final String accountName = createAccountName();
    private final String containerName = createContainerName();
    private final String blobName = createBlobName();
    private final String blobPrefix = createBlobPrefix();
    private final String sharedKey = createSharedKey();

    @Test
    void validate_whenBlobRequestValid_succeeds() {
        assertThat(factory.validateRequest(request.sourceDataAddress(dataAddress
                                .keyName(accountName)
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .property(AzureBlobStoreSchema.BLOB_NAME, blobName)
                                .build())
                        .build())
                .succeeded()).isTrue();
    }

    @Test
    void validate_whenBlobFolderRequestValid_succeeds() {
        assertThat(factory.validateRequest(request.sourceDataAddress(dataAddress
                                .keyName(accountName)
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .property(AzureBlobStoreSchema.BLOB_PREFIX, blobPrefix)
                                .build())
                        .build())
                .succeeded()).isTrue();
    }

    @Test
    void validate_whenMissingAccountName_fails() {
        assertThat(factory.validateRequest(request.sourceDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .property(AzureBlobStoreSchema.BLOB_NAME, blobName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void validate_whenMissingContainerName_fails() {
        assertThat(factory.validateRequest(request.sourceDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.BLOB_NAME, blobName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void validate_whenMissingBlobNameAndBlobPrefix_fails() {
        assertThat(factory.validateRequest(request.sourceDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void createSource_whenValidRequest_succeeds() {
        var keyName = "test-key-name";
        when(vault.resolveSecret(keyName)).thenReturn(sharedKey);
        var validRequest = request.sourceDataAddress(dataAddress
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                .property(AzureBlobStoreSchema.BLOB_NAME, blobName)
                .keyName(keyName)
                .build());
        assertThat(factory.createSource(validRequest.build())).isNotNull();
    }
}
