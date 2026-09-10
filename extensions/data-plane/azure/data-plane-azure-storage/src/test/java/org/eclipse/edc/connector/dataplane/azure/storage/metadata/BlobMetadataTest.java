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

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BlobMetadataTest {

    @Test
    public void build_succeeds() {
        var builder = new BlobMetadata.Builder(mock())
                .put("key1", "value1")
                .put("key2", "value2");
        var blobMetadata = builder.build();
        var metadata = blobMetadata.getMetadata();
        assertThat(metadata.get("key1")).isEqualTo("value1");
        assertThat(metadata.get("key2")).isEqualTo("value2");
        assertThat(metadata.size()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"test,#§Ö", "#§Ö,test"})
    public void build_whenHasIllegalCharacters_fails(String key, String value) {
        var builder = new BlobMetadata.Builder(mock());
        assertThatThrownBy(() -> builder.put(key, value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void build_whenDuplicateKeys_shouldIndicateWarningInLog() {
        var monitor = mock(Monitor.class);
        var builder = new BlobMetadata.Builder(monitor)
                .put("key1", "value1")
                .put("key1", "value2");
        var blobMetadata = builder.build();
        var metadata = blobMetadata.getMetadata();
        assertThat(metadata.get("key1")).isEqualTo("value2");
        assertThat(metadata.size()).isEqualTo(1);
        verify(monitor).warning("Overwriting existing metadata key : key1");
    }
}