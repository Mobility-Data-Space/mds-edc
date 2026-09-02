package eu.dataspace.dataplane.observer;

import eu.dataspace.dataplane.observer.store.InMemoryObserverEventStore;
import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.time.Clock;

public class ObserverDefaultServicesExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Observer Default Services";
    }

    @Inject
    private Clock clock;

    @Provider(isDefault = true)
    public ObserverEventStore observerEventStore() {
        return new InMemoryObserverEventStore(clock);
    }

}
