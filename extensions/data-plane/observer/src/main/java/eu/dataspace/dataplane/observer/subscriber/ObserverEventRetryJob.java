package eu.dataspace.dataplane.observer.subscriber;

import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import org.eclipse.edc.spi.monitor.Monitor;

import java.time.Clock;
import java.time.Duration;

public class ObserverEventRetryJob implements Runnable {

    private final ObserverEventStore store;
    private final ObserverEventDispatcher dispatcher;
    private final Monitor monitor;
    private final Clock clock;
    private final Duration retryInterval;

    public ObserverEventRetryJob(ObserverEventStore store, ObserverEventDispatcher dispatcher,
                                 Monitor monitor, Clock clock, Duration retryInterval) {
        this.store = store;
        this.dispatcher = dispatcher;
        this.monitor = monitor;
        this.clock = clock;
        this.retryInterval = retryInterval;
    }

    @Override
    public void run() {
        try {
            store.nextPending().forEach(pending ->
                    dispatcher.dispatch(pending)
                            .onSuccess(ignored -> store.delete(pending.id()))
                            .onFailure(failure -> {
                                store.save(pending.markFailed(clock, retryInterval));
                                monitor.warning("Retry failed for observer event %s: %s"
                                        .formatted(pending.id(), failure.getFailureDetail()));
                            })
            );
        } catch (Exception e) {
            monitor.severe("Unexpected exception in executing %s".formatted(this.getClass().getSimpleName()), e);
        }
    }
}
