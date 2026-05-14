package eu.dataspace.connector.patch;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataPlaneProtocolInUse;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

@Provides(DataAddressStore.class)
public class VaultDataAddressPatchExtension implements ServiceExtension {

    @Inject
    private Vault vault;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private DataPlaneProtocolInUse dataPlaneProtocolInUse;

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(DataAddressStore.class, new VaultDataAddressStorePatch(vault, typeTransformerRegistry, jsonLd, dataPlaneProtocolInUse));
    }

}
