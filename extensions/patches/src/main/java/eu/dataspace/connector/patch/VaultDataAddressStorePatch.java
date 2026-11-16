/*
 *  Copyright (c) 2024 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package eu.dataspace.connector.patch;

import org.eclipse.edc.connector.controlplane.transfer.dataaddress.VaultDataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.controlplane.DataAddress;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

class VaultDataAddressStorePatch extends VaultDataAddressStore {

    private static final String EDC_DATA_ADDRESS_SECRET = "https://w3id.org/edc/v0.0.1/ns/secret";

    private final Vault vault;

    public VaultDataAddressStorePatch(Vault vault, TypeManager typeManager) {
        super(vault, () -> typeManager.getMapper(JSON_LD));
        this.vault = vault;
    }

    @Override
    public StoreResult<Void> store(DataAddress dataAddress, TransferProcess transferProcess) {
        var toBeStored = loadSecretFromTheVault(dataAddress, transferProcess);
        return super.store(toBeStored, transferProcess);
    }

    @Override
    public StoreResult<DataAddress> resolve(TransferProcess transferProcess) {
        return super.resolve(transferProcess)
                .map(dataAddress -> loadSecretFromTheVault(dataAddress, transferProcess));
    }

    private @NotNull DataAddress loadSecretFromTheVault(DataAddress dataAddress, TransferProcess transferProcess) {
        return Optional.of(dataAddress).map(it -> it.getStringProperty(DataAddress.SIMPLE_KEY_NAME))
                .map(keyName -> vault.resolveSecret(transferProcess.getParticipantContextId(), keyName))
                .map(secret -> {
                    var properties = new HashMap<>(dataAddress.getProperties());
                    properties.remove(DataAddress.EDC_DATA_ADDRESS_KEY_NAME);
                    properties.put(EDC_DATA_ADDRESS_SECRET, secret);
                    return DataAddress.Builder.newInstance().properties(properties).build();
                })
                .orElse(dataAddress);
    }
}
