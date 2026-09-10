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

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProvider;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProviderImpl;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobPrefix;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.mockito.Mockito.mock;

class AzureStorageDataSinkFactoryTest {
    private final BlobStoreApi blobStoreApi = mock();
    private final Vault vault = mock();
    private final TypeManager typeManager = new JacksonTypeManager();
    private final Monitor monitor = mock();
    private final BlobMetadataProvider metadataProvider = new BlobMetadataProviderImpl(monitor);
    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("participant-id")
            .identity("identity")
            .build();
    private final SingleParticipantContextSupplier participantContextSupplier = ()  -> ServiceResult.success(participantContext);
    private final AzureStorageDataSinkFactory factory = new AzureStorageDataSinkFactory(participantContextSupplier, blobStoreApi, Executors.newFixedThreadPool(1), 5, monitor, vault, typeManager, metadataProvider);
    private final DataFlowStartMessage.Builder request = createRequest(AzureBlobStoreSchema.TYPE);
    private final DataFlowStartMessage.Builder invalidRequest = createRequest("test-type");
    private final DataAddress.Builder dataAddress = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE);

    private final String accountName = createAccountName();
    private final String containerName = createContainerName();

    private final String blobPrefix = createBlobPrefix();
    private final String keyName = "test-keyname";
    private final AzureSasToken token = new AzureSasToken("test-writeonly-sas", new Random().nextLong());

    @Test
    void validate_whenRequestValid_succeeds() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .keyName(keyName)
                                .build())
                        .build())
                .succeeded()).isTrue();
    }

    @Test
    void validate_whenFolderRequestValid_succeeds() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .keyName(keyName)
                                .build())
                        .sourceDataAddress(dataAddress.property(AzureBlobStoreSchema.BLOB_PREFIX, blobPrefix).build())
                        .build())
                .succeeded()).isTrue();
    }

    @Test
    void validate_whenMissingAccountName_fails() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .keyName(keyName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void validate_whenMissingContainerName_fails() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .keyName(keyName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void validate_whenMissingKeyName_fails() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void createSink_whenValidRequest_succeeds() {
        var validRequest = request.destinationDataAddress(dataAddress
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                .property(DataAddress.EDC_DATA_ADDRESS_SECRET, typeManager.writeValueAsString(token))
                .keyName(keyName)
                .build());
        assertThat(factory.createSink(validRequest.build())).isNotNull();
    }

    @Test
    void createSink_whenInvalidRequest_fails() {
        assertThatThrownBy(() -> factory.createSink(invalidRequest.build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("AzureStorage destination address is invalid: Invalid account name, the name may not be null, empty or blank");
    }

    @Test
    void createSink_whenSecretNotFoundRequest_fails() {
        var validRequest = request.destinationDataAddress(dataAddress
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                .keyName(keyName)
                .build());
        assertThatThrownBy(() -> factory.createSink(validRequest.build()))
                .isInstanceOf(EdcException.class)
                .hasMessageStartingWith("SAS token for the Azure Blob DataSink not found");
    }
}
