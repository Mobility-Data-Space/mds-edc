package eu.dataspace.connector.dataplane.client;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.signaling.port.ClientFactory;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides(ClientFactory.class)
public class EmbeddedDataPlaneClientExtension implements ServiceExtension {

    @Inject
    private ClientFactory clientFactory; // needed to permit service overriding
    @Inject
    private DataPlaneManager dataPlaneManager;
    @Inject
    private AssetIndex assetIndex;


    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(ClientFactory.class, new EmbeddedClientFactory(dataPlaneManager, context.getMonitor(), assetIndex));
    }

}
