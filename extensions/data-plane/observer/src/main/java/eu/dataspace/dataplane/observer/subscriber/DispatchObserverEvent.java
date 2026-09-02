package eu.dataspace.dataplane.observer.subscriber;

import eu.dataspace.dataplane.observer.model.event.ObserverEventStored;
import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

import java.time.Clock;
import java.time.Duration;

public class DispatchObserverEvent implements EventSubscriber {

    private final ObserverEventStore store;
    private final ObserverEventDispatcher dispatcher;
    private final Monitor monitor;
    private final Clock clock;
    private final Duration retryInterval;

    public DispatchObserverEvent(ObserverEventStore store, ObserverEventDispatcher dispatcher,
                                  Monitor monitor, Clock clock, Duration retryInterval) {
        this.store = store;
        this.dispatcher = dispatcher;
        this.monitor = monitor;
        this.clock = clock;
        this.retryInterval = retryInterval;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (!(event.getPayload() instanceof ObserverEventStored stored)) {
            return;
        }

        var pending = store.findById(stored.getPendingEventId());
        if (pending == null) {
            monitor.warning("No pending observer event found with id: " + stored.getPendingEventId());
            return;
        }

        dispatcher.dispatch(pending)
                .onSuccess(ignored -> {
                    store.delete(pending.id());
                    monitor.debug("Event %s dispatched to observer".formatted(pending.id()));
                })
                .onFailure(failure -> {
                    store.save(pending.markFailed(clock, retryInterval));
                    monitor.warning("Failed to dispatch event %s to observer, will retry: %s"
                            .formatted(pending.id(), failure.getFailureDetail()));
                });
    }
}
