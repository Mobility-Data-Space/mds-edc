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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.util.string.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CORRELATION_ID;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator.CONNECTOR_ID;
import static org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator.ORIGINAL_NAME;
import static org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator.PARTICIPANT_ID;
import static org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator.PROCESS_ID;
import static org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator.REQUEST_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommonBlobMetadataDecoratorTest {

    private static final String TEST_ORIGINAL_NAME = "original-name";
    private static final String TEST_CONNECTOR_ID = "some-connector-id";
    private static final String TEST_PARTICIPANT_ID = "some-participant-id";
    private final DataFlowStartMessage.Builder requestBuilder = createRequest(AzureBlobStoreSchema.TYPE);

    @ParameterizedTest
    @CsvSource(value = { "correlation-id", "''", "null" }, nullValues = { "null" })
    void decorate_succeeds(String correlationId) {
        var samplePart = new DataSource.Part() { // close is no-op
            @Override
            public String name() {
                return TEST_ORIGINAL_NAME;
            }

            @Override
            public InputStream openStream() {
                return null;
            }
        };

        var decorator = new CommonBlobMetadataDecorator(TEST_PARTICIPANT_ID, TEST_CONNECTOR_ID);
        var builder = mock(BlobMetadata.Builder.class);
        when(builder.put(anyString(), anyString())).thenReturn(builder);

        DataFlowStartMessage sampleRequest;
        if (correlationId != null) {
            sampleRequest = requestBuilder.destinationDataAddress(
                    DataAddress.Builder.newInstance()
                            .type(AzureBlobStoreSchema.TYPE)
                            .property(CORRELATION_ID, correlationId)
                            .build()).build();
        } else {
            sampleRequest = requestBuilder.build();
        }

        var result = decorator.decorate(sampleRequest, samplePart, builder);

        verify(builder).put(ORIGINAL_NAME, TEST_ORIGINAL_NAME);

        verify(builder).put(REQUEST_ID, sampleRequest.getId());
        verify(builder).put(PROCESS_ID, sampleRequest.getProcessId());

        verify(builder).put(CONNECTOR_ID, TEST_CONNECTOR_ID);
        verify(builder).put(PARTICIPANT_ID, TEST_PARTICIPANT_ID);

        if (!StringUtils.isNullOrEmpty(correlationId)) {
            verify(builder).put(CORRELATION_ID, correlationId);
        }

        assertThat(result).isInstanceOf(BlobMetadata.Builder.class);
    }
}
