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

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.azure.storage.DestinationBlobName;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProvider;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProviderImpl;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource.Part;
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createSharedAccessSignature;
import static org.eclipse.edc.connector.dataplane.azure.storage.pipeline.TestFunctions.sharedAccessSignatureMatcher;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureStorageDataSinkTest {

    private final Monitor monitor = mock();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final BlobStoreApi blobStoreApi = mock();
    private final DataFlowStartMessage.Builder request = createRequest(AzureBlobStoreSchema.TYPE);
    private final ServiceExtensionContext context = mock();
    private final String accountName = createAccountName();
    private final String containerName = createContainerName();
    private final String sharedAccessSignature = createSharedAccessSignature();
    private final String blobName = createBlobName();
    private final String content = "Test Content";
    private final Exception exception = new TestCustomException("Test custom exception message");
    private final BlobMetadataProvider metadataProvider = new BlobMetadataProviderImpl(monitor);
    private final DestinationBlobName destinationBlobName = mock(DestinationBlobName.class);

    private final AzureStorageDataSink dataSink = AzureStorageDataSink.Builder.newInstance()
            .accountName(accountName)
            .containerName(containerName)
            .sharedAccessSignature(sharedAccessSignature)
            .requestId(request.build().getId())
            .blobStoreApi(blobStoreApi)
            .executorService(executor)
            .monitor(monitor)
            .request(request.build())
            .metadataProvider(metadataProvider)
            .destinationBlobName(destinationBlobName)
            .build();

    private final BlobAdapter destination = mock(BlobAdapter.class);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    InputStreamDataSource part = new InputStreamDataSource(blobName, new ByteArrayInputStream(content.getBytes(UTF_8)));

    @BeforeEach
    void setUp() {
        when(destination.getOutputStream()).thenReturn(output);
        when(blobStoreApi.getBlobAdapter(
                eq(accountName),
                eq(containerName),
                eq(blobName),
                sharedAccessSignatureMatcher(sharedAccessSignature)))
                .thenReturn(destination);

        when(context.getComponentId()).thenReturn("connectorId");
        when(destinationBlobName.resolve(blobName, 1)).thenReturn(blobName);
    }

    @Test
    void transferParts_succeeds() {
        var result = dataSink.transferParts(List.of(part));
        assertThat(result.succeeded()).isTrue();
        assertThat(output.toString(UTF_8)).isEqualTo(content);
    }

    @Test
    void transferParts_whenBlobClientCreationFails_fails() {
        when(blobStoreApi.getBlobAdapter(
                eq(accountName),
                eq(containerName),
                eq(blobName),
                sharedAccessSignatureMatcher(sharedAccessSignature)))
                .thenThrow(exception);
        assertThatTransferPartsFails(part, "Error creating blob %s on account %s", blobName, accountName);
    }

    @Test
    void transferParts_whenReadFails_fails() {
        when(destination.getOutputStream()).thenThrow(exception);
        var part = mock(Part.class);
        when(part.openStream()).thenThrow(exception);
        when(part.name()).thenReturn(blobName);
        assertThatTransferPartsFails(part, "Error reading blob %s", blobName);
    }

    @Test
    void transferParts_whenTransferFails_fails() throws Exception {
        var input = mock(InputStream.class);
        when(input.transferTo(output)).thenThrow(exception);
        var part = mock(Part.class);
        when(part.openStream()).thenReturn(input);
        when(part.name()).thenReturn(blobName);
        assertThatTransferPartsFails(part, "Error transferring blob for %s on account %s", blobName, accountName);
    }

    private void assertThatTransferPartsFails(Part part, String logMessage, Object... args) {
        var message = format(logMessage, args);
        var result = dataSink.transferParts(List.of(part));
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly(message);
        verify(monitor).severe(message, exception);
    }


}
