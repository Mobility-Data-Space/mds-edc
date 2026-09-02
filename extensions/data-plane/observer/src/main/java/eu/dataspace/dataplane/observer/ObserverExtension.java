package eu.dataspace.dataplane.observer;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


public class ObserverExtension implements ServiceExtension {

    @Configuration(context = "edc.mds.observer")
    private ObserverConfig observerConfig;

    @Inject
    private Monitor monitor;
    @Inject
    private ObserverManager manager;

    @Override
    public String name() {
        return "DFRS Observer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (!observerConfig.isConfigured()) {
            monitor.warning("DFRS observer hasn't configured correctly");
        }
    }

    @Override
    public void start() {
        if (observerConfig.isConfigured()) {
            var result = manager.activate(observerConfig);
            if (result.failed()) {
                throw new EdcException("Cannot initialize DFRS Observer: " + result.getFailureDetail());
            }
        }

    }

}
