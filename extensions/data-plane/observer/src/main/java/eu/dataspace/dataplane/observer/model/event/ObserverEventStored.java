package eu.dataspace.dataplane.observer.model.event;

import org.eclipse.edc.spi.event.Event;

public class ObserverEventStored extends Event {

    private final String pendingEventId;

    public ObserverEventStored(String pendingEventId) {
        this.pendingEventId = pendingEventId;
    }

    public String getPendingEventId() {
        return pendingEventId;
    }

    @Override
    public String name() {
        return "observer.event.stored";
    }
}
