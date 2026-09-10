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
 *       Robert Bosch GmbH - adaption for blob metadata
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage.metadata;

import org.eclipse.edc.azure.blob.validator.AzureStorageValidator;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BlobMetadata {

    private final Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    private BlobMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public static class Builder {

        private final Map<String, String> metadata = new HashMap<>();

        private final Monitor monitor;

        public Builder(Monitor monitor) {
            this.monitor = monitor;
        }

        public BlobMetadata.Builder put(String key, String value) {
            AzureStorageValidator.validateMetadata(key);
            AzureStorageValidator.validateMetadata(value);
            if (metadata.containsKey(key)) {
                monitor.warning("Overwriting existing metadata key : " + key);
            }
            metadata.put(key, value);
            return this;
        }

        public BlobMetadata build() {
            return new BlobMetadata(metadata);
        }
    }
}
