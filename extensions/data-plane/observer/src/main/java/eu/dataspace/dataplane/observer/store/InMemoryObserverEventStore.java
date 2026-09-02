package eu.dataspace.dataplane.observer.store;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryObserverEventStore implements ObserverEventStore {

    private final ConcurrentHashMap<String, PendingObserverEvent> store = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryObserverEventStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(PendingObserverEvent event) {
        store.put(event.id(), event);
    }

    @Override
    public PendingObserverEvent findById(String id) {
        return store.get(id);
    }

    @Override
    public List<PendingObserverEvent> nextPending() {
        var now = clock.instant();
        return store.values().stream()
                .filter(e -> !e.nextRetryAt().isAfter(now))
                .toList();
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

}
