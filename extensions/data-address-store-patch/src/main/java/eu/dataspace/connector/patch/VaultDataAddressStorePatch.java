package eu.dataspace.connector.patch;

import org.eclipse.edc.connector.controlplane.transfer.dataaddress.VaultDataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataPlaneProtocolInUse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;

/**
 * NOTE: this class provides a patch that attaches the secret to the data address in any case, and not only when the address
 * is not stored in the vault (that should always be the case). This to permit the PUSH cases in which the consumer is
 * providing a secret to the provider to work.
 * This is true only for legacy signaling transfers (using EDC Data-Plane), because with the new DPS, the data
 * address, will be managed in its entirety by the data-plane.
 */
class VaultDataAddressStorePatch extends VaultDataAddressStore {

    private final Vault vault;

    public VaultDataAddressStorePatch(Vault vault, TypeTransformerRegistry typeTransformerRegistry, JsonLd jsonLd,
                                      DataPlaneProtocolInUse dataPlaneProtocolInUse) {
        super(vault, typeTransformerRegistry, jsonLd, dataPlaneProtocolInUse);
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
        return Optional.of(dataAddress).map(DataAddress::getKeyName)
                .map(keyName -> vault.resolveSecret(transferProcess.getParticipantContextId(), keyName))
                .map(secret -> {
                    var properties = new HashMap<>(dataAddress.getProperties());
                    properties.remove(DataAddress.EDC_DATA_ADDRESS_KEY_NAME);
                    properties.put(DataAddress.EDC_DATA_ADDRESS_SECRET, secret);
                    return DataAddress.Builder.newInstance().properties(properties).build();
                })
                .orElse(dataAddress);
    }

}
