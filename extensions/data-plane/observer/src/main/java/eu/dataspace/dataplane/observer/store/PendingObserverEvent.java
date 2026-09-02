package eu.dataspace.dataplane.observer.store;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public record PendingObserverEvent(String id, String envelopeJson, int retryCount, Instant nextRetryAt) {

    public static final Duration MAX_BACKOFF = Duration.ofHours(1);

    public PendingObserverEvent markFailed(Clock clock, Duration baseInterval) {
        var nextRetryAt = clock.instant().plus(backoff(baseInterval));

        return new PendingObserverEvent(id, envelopeJson, retryCount + 1, nextRetryAt);
    }

    private Duration backoff(Duration baseInterval) {
        var backoff = baseInterval.multipliedBy(retryCountPow2());
        if (backoff.compareTo(MAX_BACKOFF) < 0) {
            return backoff;
        } else {
            return MAX_BACKOFF;
        }
    }

    private long retryCountPow2() {
        return retryCount < Long.SIZE - 1 ? (1L << retryCount) : Long.MAX_VALUE;
    }
}
