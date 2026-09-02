package eu.dataspace.dataplane.observer.store;

import java.time.Clock;

class InMemoryObserverEventStoreTest extends ObserverEventStoreTestBase {

    private final InMemoryObserverEventStore store = new InMemoryObserverEventStore(Clock.systemUTC());

    @Override
    protected ObserverEventStore getStore() {
        return store;
    }
}
