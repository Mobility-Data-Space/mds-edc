package eu.dataspace.dataplane.observer.store;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryObserverEventStoreTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    private final InMemoryObserverEventStore store = new InMemoryObserverEventStore(clock);

    @Test
    void shouldSaveAndFindById() {
        var event = pending("id-1", clock.instant());

        store.save(event);

        assertThat(store.findById("id-1")).isEqualTo(event);
    }

    @Test
    void shouldReturnNull_whenNotFound() {
        assertThat(store.findById("missing")).isNull();
    }

    @Test
    void shouldDelete() {
        store.save(pending("id-1", clock.instant()));

        store.delete("id-1");

        assertThat(store.findById("id-1")).isNull();
    }

    @Test
    void shouldReturnPendingEvents_whenNextRetryAtIsNowOrPast() {
        var past = pending("past", clock.instant().minusSeconds(1));
        var now = pending("now", clock.instant());
        var future = pending("future", clock.instant().plusSeconds(1));
        store.save(past);
        store.save(now);
        store.save(future);

        var result = store.nextPending();

        assertThat(result).extracting(PendingObserverEvent::id).containsExactlyInAnyOrder("past", "now");
    }

    @Test
    void shouldUpdateEvent_whenSavedAgainAfterMarkFailed() {
        var original = pending("id-1", clock.instant());
        store.save(original);

        store.save(original.markFailed(clock, Duration.ofSeconds(30)));

        var updated = store.findById("id-1");
        assertThat(updated).isNotNull();
        assertThat(updated.retryCount()).isEqualTo(1);
        assertThat(updated.nextRetryAt()).isAfter(clock.instant());
    }

    @Test
    void shouldNotReturnUpdatedEvent_whenNextRetryAtIsInFuture() {
        var original = pending("id-1", clock.instant());
        store.save(original);
        store.save(original.markFailed(clock, Duration.ofSeconds(30)));

        assertThat(store.nextPending()).isEmpty();
    }

    private PendingObserverEvent pending(String id, Instant nextRetryAt) {
        return new PendingObserverEvent(id, "{}", 0, nextRetryAt);
    }
}
