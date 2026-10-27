/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       ZF Friedrichshafen AG - Initial implementation
 *       Cofinity-X - additional test for secret deserialization
 *
 */

package org.eclipse.edc.connector.dataplane.aws.s3;

import org.eclipse.edc.aws.s3.AwsClientProvider;
import org.eclipse.edc.aws.s3.AwsSecretToken;
import org.eclipse.edc.aws.s3.AwsTemporarySecretToken;
import org.eclipse.edc.aws.s3.S3ClientRequest;
import org.eclipse.edc.aws.s3.spi.S3BucketSchema;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.controlplane.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.dataplane.aws.s3.TestFunctions.s3DataAddressWithCredentials;
import static org.eclipse.edc.connector.dataplane.aws.s3.TestFunctions.s3DataAddressWithoutCredentials;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3DataSourceFactoryTest {

    private final AwsClientProvider clientProvider = mock();
    private final TypeManager typeManager = new JacksonTypeManager();
    private final Vault vault = mock();

    private final S3DataSourceFactory factory = new S3DataSourceFactory(clientProvider,
            vault, typeManager.getMapper());

    @Test
    void validate_shouldSucceed_whenValidatorSucceeds() {
        var source = s3DataAddressWithCredentials();
        var request = createRequest(source);

        var result = factory.validateRequest(request);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_shouldFail_whenValidatorFails() {
        // invalid S3 address (missing bucket/region) -> S3SourceDataAddressValidator fails
        var source = DataAddress.Builder.newInstance().type(S3BucketSchema.TYPE).build();
        var request = createRequest(source);

        var result = factory.validateRequest(request);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void createSource_shouldCreateDataSource() {
        var source = s3DataAddressWithCredentials();
        var request = createRequest(source);

        var dataSource = factory.createSource(request);

        assertThat(dataSource).isNotNull().isInstanceOf(S3DataSource.class);
    }

    @Test
    void createSink_shouldLetTheProviderGetTheCredentialsIfNotProvidedByTheAddress() {
        var destination = s3DataAddressWithoutCredentials();
        var request = createRequest(destination);

        var sink = factory.createSource(request);

        assertThat(sink).isNotNull().isInstanceOf(S3DataSource.class);
        var captor = ArgumentCaptor.forClass(S3ClientRequest.class);
        verify(clientProvider).s3Client(captor.capture());
        var s3ClientRequest = captor.getValue();
        assertThat(s3ClientRequest.region()).isEqualTo(TestFunctions.VALID_REGION);
        assertThat(s3ClientRequest.secretToken()).isNull();
        assertThat(s3ClientRequest.endpointOverride()).isNull();
    }
    
    @Test
    void createSource_shouldGetTheSecretTokenFromTheVault() {
        var source = TestFunctions.s3DataAddressWithCredentials();
        var secretToken = new AwsSecretToken("accessKeyId", "secretAccessKey");
        when(vault.resolveSecret(source.getStringProperty("keyName"))).thenReturn(typeManager.writeValueAsString(secretToken));
        var request = createRequest(source);
        
        var s3Source = factory.createSource(request);
        
        assertThat(s3Source).isNotNull().isInstanceOf(S3DataSource.class);
        var captor = ArgumentCaptor.forClass(S3ClientRequest.class);
        verify(clientProvider).s3Client(captor.capture());
        var s3ClientRequest = captor.getValue();
        assertThat(s3ClientRequest.region()).isEqualTo(TestFunctions.VALID_REGION);
        assertThat(s3ClientRequest.secretToken()).isInstanceOf(AwsSecretToken.class);
        assertThat(s3ClientRequest.endpointOverride()).isNull();
    }

    @Test
    void createSource_shouldGetTheTemporarySecretTokenFromTheVault() {
        var source = TestFunctions.s3DataAddressWithCredentials();
        var temporaryKey = new AwsTemporarySecretToken("temporaryId", "temporarySecret", null, 0);
        when(vault.resolveSecret(source.getStringProperty("keyName"))).thenReturn(typeManager.writeValueAsString(temporaryKey));
        var request = createRequest(source);

        var s3Source = factory.createSource(request);

        assertThat(s3Source).isNotNull().isInstanceOf(S3DataSource.class);
        var captor = ArgumentCaptor.forClass(S3ClientRequest.class);
        verify(clientProvider).s3Client(captor.capture());
        var s3ClientRequest = captor.getValue();
        assertThat(s3ClientRequest.region()).isEqualTo(TestFunctions.VALID_REGION);
        assertThat(s3ClientRequest.secretToken()).isInstanceOf(AwsTemporarySecretToken.class);
        assertThat(s3ClientRequest.endpointOverride()).isNull();
    }

    @Test
    void createSource_shouldThrowExceptionIfValidationFails() {
        var source = DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .build();

        var request = createRequest(source);

        assertThatThrownBy(() -> factory.createSource(request)).isInstanceOf(EdcException.class);
    }

    private DataFlowStartMessage createRequest(DataAddress source) {
        return DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(DataAddress.Builder.newInstance().type(S3BucketSchema.TYPE).build())
                .build();
    }

}
