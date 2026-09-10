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
 *       Robert Bosch GmbH - initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage.metadata;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator.ORIGINAL_NAME;
import static org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator.PROCESS_ID;
import static org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator.REQUEST_ID;
import static org.mockito.Mockito.mock;

public class BlobMetadataProviderImplTest {

    private static final String FILE_SIZE = "fileSize";
    private static final String TEST_FILE_NAME = "test-file-name";
    private static final long TEST_FILE_SIZE = 12345L;

    @Test
    public void provideSinkMetadata_shouldHandleMoreThanOneDecorator() {

        var sampleRequest = createRequest(AzureBlobStoreSchema.TYPE).build();
        var samplePart = new DataSource.Part() { // close is no-op
            @Override
            public String name() {
                return TEST_FILE_NAME;
            }

            @Override
            public InputStream openStream() {
                return null;
            }

            @Override
            public long size() {
                return TEST_FILE_SIZE;
            }
        };

        // Registering two decorators providing different sets of metadata fields
        var provider = new BlobMetadataProviderImpl(mock());
        provider.registerDecorator((request, part, builder) -> {
            builder.put(PROCESS_ID, request.getProcessId())
                    .put(ORIGINAL_NAME, part.name());
            return builder;
        });
        provider.registerDecorator((request, part, builder) -> {
            builder.put(REQUEST_ID, request.getId())
                    .put(FILE_SIZE, String.valueOf(part.size()));
            return builder;
        });

        var map = provider.provideSinkMetadata(sampleRequest, samplePart).getMetadata();

        assertThat(map.get(PROCESS_ID)).isEqualTo(sampleRequest.getProcessId());
        assertThat(map.get(REQUEST_ID)).isEqualTo(sampleRequest.getId());
        assertThat(map.get(ORIGINAL_NAME)).isEqualTo(samplePart.name());
        assertThat(map.get(FILE_SIZE)).isEqualTo(String.valueOf(TEST_FILE_SIZE));
        assertThat(map.size()).isEqualTo(4);
    }
}
