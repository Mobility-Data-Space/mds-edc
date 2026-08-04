package eu.dataspace.dataplane.dfrs;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


public class DfrsObserverExtension implements ServiceExtension {

    @Configuration(context = "edc.dfrs.observer")
    private DfrsObserverConfig observerConfig;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (!observerConfig.isConfigured()) {
            monitor.warning("DFRS observer hasn't configured correctly");
        }
    }

    @Settings
    private record DfrsObserverConfig(
            @Setting(key = "url", required = false,
                    description = "The DSP url of the participant responsible to observe DFRS events")
            String url,
            @Setting(key = "dataset.id", required = false,
                    description = "The dataset ID on which the observe offer will be exposed in the dataspace")
            String datasetId
    ) {
        public boolean isConfigured() {
            return url != null && datasetId != null;
        }
    }
}
