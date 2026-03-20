/*
 * Copyright (c) 2026 Mobility Data Space
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *      Think-it GmbH - initial API and implementation
 */

package eu.dataspace.connector.extension.kafka.broker.acls;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.KAFKA_ADMIN_PROPERTIES_KEY;
import static eu.dataspace.connector.extension.kafka.broker.acls.KafkaAccessControlLists.KAFKA_PRINCIPAL_NAME_KEY_PREFIX;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class KafkaAccessControlListsTest {

    private final DataPlaneStore dataPlaneStore = Mockito.mock();
    private final Vault vault = Mockito.mock();
    private final KafkaAccessControlLists kafkaAccessControlLists = new KafkaAccessControlLists(vault, dataPlaneStore);

    @Nested
    class DenyAccess {

        @Test
        void shouldReturnError_whenDataFlowCannotBeFetched() {
            when(dataPlaneStore.findById(any())).thenReturn(null);
            when(vault.resolveSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + "dataFlowId")).thenReturn("principal:name");

            var result = kafkaAccessControlLists.denyAccessTo("dataFlowId");

            assertThat(result).isFailed().detail().contains("Cannot retrieve DataFlow");
        }

        @Test
        void shouldReturnError_whenDataAddressDoesNotContainAdminKeyProperties() {
            var source = DataAddress.Builder.newInstance().type("Kafka").build();
            when(dataPlaneStore.findById(any())).thenReturn(DataFlow.Builder.newInstance().source(source).build());
            when(vault.resolveSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + "dataFlowId")).thenReturn("principal:name");

            var result = kafkaAccessControlLists.denyAccessTo("dataFlowId");

            assertThat(result).isFailed().detail().contains("Source DataAddress doesn't contain mandatory key");
        }

        @Test
        void shouldReturnError_whenVaultDoesNotContainAdminProperties() {
            var source = DataAddress.Builder.newInstance().type("Kafka").property(KAFKA_ADMIN_PROPERTIES_KEY, "admin.properties.key").build();
            when(dataPlaneStore.findById(any())).thenReturn(DataFlow.Builder.newInstance().source(source).build());
            when(vault.resolveSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + "dataFlowId")).thenReturn("principal:name");
            when(vault.resolveSecret("admin.properties.key")).thenReturn(null);

            var result = kafkaAccessControlLists.denyAccessTo("dataFlowId");

            assertThat(result).isFailed().detail().contains("Cannot get Kafka Admin properties from Vault");
        }

        @Test
        void shouldReturnError_whenAdminCannotBeInstantiated() {
            var source = DataAddress.Builder.newInstance().type("Kafka").property(KAFKA_ADMIN_PROPERTIES_KEY, "admin.properties.key").build();
            when(dataPlaneStore.findById(any())).thenReturn(DataFlow.Builder.newInstance().source(source).build());
            when(vault.resolveSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + "dataFlowId")).thenReturn("principal:name");
            when(vault.resolveSecret("admin.properties.key")).thenReturn("");

            var result = kafkaAccessControlLists.denyAccessTo("dataFlowId");

            assertThat(result).isFailed().detail().contains("Failed to create new KafkaAdminClient");
        }
    }

}
