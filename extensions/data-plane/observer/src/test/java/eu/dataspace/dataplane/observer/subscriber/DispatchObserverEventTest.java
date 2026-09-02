package eu.dataspace.dataplane.observer.subscriber;

import eu.dataspace.dataplane.observer.model.event.ObserverEventStored;
import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import eu.dataspace.dataplane.observer.store.PendingObserverEvent;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DispatchObserverEventTest {

    private final ObserverEventStore store = mock();
    private final ObserverEventDispatcher dispatcher = mock();
    private final Monitor monitor = mock();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));

    private final DispatchObserverEvent subscriber = new DispatchObserverEvent(store, dispatcher, monitor, clock, Duration.ofSeconds(30));

    @Test
    void shouldDeleteFromStore_whenDispatchSucceeds() {
        var pending = new PendingObserverEvent("event-id", "{}", 0, clock.instant());
        when(store.findById("event-id")).thenReturn(pending);
        when(dispatcher.dispatch(pending)).thenReturn(Result.success(null));

        subscriber.on(envelope(new ObserverEventStored("event-id")));

        verify(store).delete("event-id");
        verify(store, never()).save(pending.markFailed(clock, Duration.ofSeconds(30)));
    }

    @Test
    void shouldSaveFailedEvent_whenDispatchFails() {
        var pending = new PendingObserverEvent("event-id", "{}", 0, clock.instant());
        when(store.findById("event-id")).thenReturn(pending);
        when(dispatcher.dispatch(pending)).thenReturn(Result.failure("connection refused"));

        subscriber.on(envelope(new ObserverEventStored("event-id")));

        verify(store).save(pending.markFailed(clock, Duration.ofSeconds(30)));
        verify(store, never()).delete(anyString());
        verify(monitor).warning(anyString());
    }

    @Test
    void shouldLogWarning_whenPendingEventNotFound() {
        when(store.findById("missing-id")).thenReturn(null);

        subscriber.on(envelope(new ObserverEventStored("missing-id")));

        verify(monitor).warning(anyString());
        verifyNoInteractions(dispatcher);
    }

    @Test
    void shouldDoNothing_whenEventTypeIsNotObserverEventStored() {
        subscriber.on(envelope(mock(Event.class)));

        verifyNoInteractions(store, dispatcher, monitor);
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> EventEnvelope<T> envelope(T event) {
        return EventEnvelope.Builder.newInstance()
                .at(1)
                .payload(event)
                .build();
    }
}
