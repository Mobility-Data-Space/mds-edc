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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage.pipeline;

import com.azure.storage.blob.models.BlobItem;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobPrefix;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createSharedKey;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureStorageDataSourceTest {

    Monitor monitor = mock();
    BlobStoreApi blobStoreApi = mock();
    DataFlowStartMessage.Builder request = createRequest(AzureBlobStoreSchema.TYPE);

    String accountName = createAccountName();
    String containerName = createContainerName();
    String sharedKey = createSharedKey();
    String blobName = createBlobName();
    String blobPrefix = createBlobPrefix();
    String content = "Test Content";

    Exception exception = new TestCustomException("Test exception message");

    AzureStorageDataSource dataSource = AzureStorageDataSource.Builder.newInstance()
            .accountName(accountName)
            .containerName(containerName)
            .blobName(blobName)
            .sharedKey(sharedKey)
            .requestId(request.build().getId())
            .retryPolicy(RetryPolicy.ofDefaults())
            .blobStoreApi(blobStoreApi)
            .monitor(monitor)
            .build();

    AzureStorageDataSource dataSourceFolder = AzureStorageDataSource.Builder.newInstance()
            .accountName(accountName)
            .containerName(containerName)
            .blobPrefix(blobPrefix)
            .sharedKey(sharedKey)
            .requestId(request.build().getId())
            .retryPolicy(RetryPolicy.ofDefaults())
            .blobStoreApi(blobStoreApi)
            .monitor(monitor)
            .build();

    AzureStorageDataSource dataSourceFolderWithoutSharedKey = AzureStorageDataSource.Builder.newInstance()
            .accountName(accountName)
            .containerName(containerName)
            .blobPrefix(blobPrefix)
            .requestId(request.build().getId())
            .retryPolicy(RetryPolicy.ofDefaults())
            .blobStoreApi(blobStoreApi)
            .monitor(monitor)
            .build();
    BlobAdapter destination = mock();
    BlobItem blobItem = mock();
    ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(UTF_8));

    @BeforeEach
    void setUp() {
        when(destination.openInputStream()).thenReturn(input);
        when(blobStoreApi.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenReturn(destination);
        when(blobStoreApi.getBlobAdapter(
                accountName,
                containerName,
                blobName))
                .thenReturn(destination);
    }

    @Test
    void openPartStreamBlob_succeeds() {
        var result = dataSource.openPartStream().getContent();
        assertThat(result).map(DataSource.Part::openStream).containsExactly(input);
    }

    @Test
    void openPartStreamBlobs_succeeds() {
        when(blobStoreApi.listContainerFolder(
                accountName,
                containerName,
                blobPrefix,
                sharedKey))
                .thenReturn(List.of(blobItem));
        when(blobItem.getName()).thenReturn(blobName);

        var result = dataSourceFolder.openPartStream().getContent();
        assertThat(result).map(DataSource.Part::openStream).containsExactly(input);
    }

    @Test
    void openPartStreamBlobsWithoutSharedKey_succeeds() {
        when(blobStoreApi.listContainerFolder(
                accountName,
                containerName,
                blobPrefix,
                null))
                .thenReturn(List.of(blobItem));
        when(blobItem.getName()).thenReturn(blobName);

        var result = dataSourceFolderWithoutSharedKey.openPartStream().getContent();
        assertThat(result).map(DataSource.Part::openStream).containsExactly(input);
    }

    @Test
    void openPartStream_whenBlobClientCreationFails_fails() {
        when(blobStoreApi.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenThrow(exception);

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> dataSource.openPartStream())
                .withCause(exception);
        verify(monitor).severe(format("Error accessing blob %s on account %s", blobName, accountName), exception);
    }

    @Test
    void openPartStream_whenEmptyFolderBlobs_fails() {
        when(blobStoreApi.listContainerFolder(
                accountName,
                containerName,
                blobPrefix,
                sharedKey))
                .thenReturn(List.of());
        when(blobItem.getName()).thenReturn(blobName);

        var result = dataSourceFolderWithoutSharedKey.openPartStream();
        assertTrue(result.failed());
        verify(monitor).severe(format("Error listing blobs in the container %s with prefix %s", containerName, blobPrefix));
    }
}
