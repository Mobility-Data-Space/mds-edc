package eu.dataspace.dataplane.observer.subscriber;

import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import eu.dataspace.dataplane.observer.store.PendingObserverEvent;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObserverEventRetryJobTest {

    private final ObserverEventStore store = mock();
    private final ObserverEventDispatcher dispatcher = mock();
    private final Monitor monitor = mock();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));

    private final ObserverEventRetryJob job = new ObserverEventRetryJob(store, dispatcher, monitor, clock, Duration.ofSeconds(30));

    @Test
    void shouldDeleteFromStore_whenDispatchSucceeds() {
        var event = new PendingObserverEvent("id-1", "{}", 0, clock.instant());
        when(store.nextPending()).thenReturn(List.of(event));
        when(dispatcher.dispatch(event)).thenReturn(Result.success(null));

        job.run();

        verify(store).delete("id-1");
        verify(store, never()).save(event.markFailed(clock, Duration.ofSeconds(30)));
    }

    @Test
    void shouldSaveFailedEvent_whenDispatchFails() {
        var event = new PendingObserverEvent("id-1", "{}", 2, clock.instant());
        when(store.nextPending()).thenReturn(List.of(event));
        when(dispatcher.dispatch(event)).thenReturn(Result.failure("timeout"));

        job.run();

        verify(store).save(event.markFailed(clock, Duration.ofSeconds(30)));
        verify(store, never()).delete(anyString());
        verify(monitor).warning(anyString());
    }

    @Test
    void shouldProcessAllPendingEvents() {
        var event1 = new PendingObserverEvent("id-1", "{}", 0, clock.instant());
        var event2 = new PendingObserverEvent("id-2", "{}", 1, clock.instant());
        when(store.nextPending()).thenReturn(List.of(event1, event2));
        when(dispatcher.dispatch(event1)).thenReturn(Result.success(null));
        when(dispatcher.dispatch(event2)).thenReturn(Result.success(null));

        job.run();

        verify(store).delete("id-1");
        verify(store).delete("id-2");
    }

    @Test
    void shouldDoNothing_whenNoPendingEvents() {
        when(store.nextPending()).thenReturn(List.of());

        job.run();

        verifyNoInteractions(dispatcher);
    }
}
