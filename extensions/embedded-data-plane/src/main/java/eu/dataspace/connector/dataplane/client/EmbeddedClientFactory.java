package eu.dataspace.connector.dataplane.client;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.signaling.port.ClientFactory;
import org.eclipse.edc.signaling.port.DataPlaneSignalingClient;
import org.eclipse.edc.spi.monitor.Monitor;

public class EmbeddedClientFactory extends ClientFactory {

    private final DataPlaneManager dataPlaneManager;
    private final Monitor monitor;
    private final AssetIndex assetIndex;

    public EmbeddedClientFactory(DataPlaneManager dataPlaneManager, Monitor monitor, AssetIndex assetIndex) {
        super(null, null, null);
        this.dataPlaneManager = dataPlaneManager;
        this.monitor = monitor;
        this.assetIndex = assetIndex;
    }

    @Override
    public DataPlaneSignalingClient createClient(DataPlaneInstance instance) {
        return new EmbeddedDataPlaneSignalingClient(instance, dataPlaneManager, monitor, assetIndex);
    }

}
