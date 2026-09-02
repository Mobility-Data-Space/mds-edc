package eu.dataspace.dataplane.observer.store;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ObserverEventStoreTestBase {

    protected abstract ObserverEventStore getStore();

    @Test
    void shouldSaveAndFindById() {
        var event = pending("id-1", Instant.now().truncatedTo(ChronoUnit.MILLIS));

        getStore().save(event);

        assertThat(getStore().findById("id-1")).isEqualTo(event);
    }

    @Test
    void shouldReturnNull_whenNotFound() {
        assertThat(getStore().findById("missing")).isNull();
    }

    @Test
    void shouldDelete() {
        getStore().save(pending("id-1", Instant.now()));

        getStore().delete("id-1");

        assertThat(getStore().findById("id-1")).isNull();
    }

    @Test
    void shouldReturnPendingEvents_whenNextRetryAtIsNowOrPast() {
        var past = pending("past", Instant.now().minusSeconds(60));
        var future = pending("future", Instant.now().plusSeconds(3600));
        getStore().save(past);
        getStore().save(future);

        var result = getStore().nextPending();

        assertThat(result).extracting(PendingObserverEvent::id).containsExactly("past");
    }

    @Test
    void shouldUpdateEvent_whenSavedAgainAfterMarkFailed() {
        var clock = Clock.systemUTC();
        var original = pending("id-1", Instant.now().minusSeconds(60));
        getStore().save(original);

        getStore().save(original.markFailed(clock, Duration.ofSeconds(30)));

        var updated = getStore().findById("id-1");
        assertThat(updated.retryCount()).isEqualTo(1);
        assertThat(updated.nextRetryAt()).isAfter(clock.instant());
    }

    @Test
    void shouldNotReturnUpdatedEvent_whenNextRetryAtIsInFuture() {
        var clock = Clock.systemUTC();
        var original = pending("id-1", Instant.now().minusSeconds(60));
        getStore().save(original);
        getStore().save(original.markFailed(clock, Duration.ofSeconds(30)));

        assertThat(getStore().nextPending()).isEmpty();
    }

    protected PendingObserverEvent pending(String id, Instant nextRetryAt) {
        return new PendingObserverEvent(id, "{}", 0, nextRetryAt);
    }
}
