package eu.dataspace.dataplane.observer.store;

import java.util.List;

public interface ObserverEventStore {
    void save(PendingObserverEvent event);

    PendingObserverEvent findById(String id);

    List<PendingObserverEvent> nextPending();

    void delete(String id);
}
