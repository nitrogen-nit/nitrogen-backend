package vn.nitrogen.platform.outbox;

import java.time.Duration;
import java.util.Objects;

/** Tính thời gian chờ exponential backoff có giới hạn trên. */
public final class OutboxRetryPolicy {

    private final int maxRetries;
    private final Duration baseDelay;
    private final Duration maximumDelay;

    public OutboxRetryPolicy(int maxRetries, Duration baseDelay, Duration maximumDelay) {
        if (maxRetries <= 0) {
            throw new IllegalArgumentException("maxRetries must be positive");
        }
        this.baseDelay = requirePositive(baseDelay, "baseDelay");
        this.maximumDelay = requirePositive(maximumDelay, "maximumDelay");
        if (baseDelay.compareTo(maximumDelay) > 0) {
            throw new IllegalArgumentException("baseDelay must not exceed maximumDelay");
        }
        this.maxRetries = maxRetries;
    }

    public boolean exhausted(int retryCount) {
        return retryCount >= maxRetries;
    }

    public Duration delayFor(int retryCount) {
        if (retryCount <= 0) {
            throw new IllegalArgumentException("retryCount must be positive");
        }

        Duration delay = baseDelay;
        for (int attempt = 1; attempt < retryCount; attempt++) {
            if (delay.compareTo(maximumDelay.dividedBy(2)) > 0) {
                return maximumDelay;
            }
            delay = delay.multipliedBy(2);
        }
        return delay.compareTo(maximumDelay) > 0 ? maximumDelay : delay;
    }

    private static Duration requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }
}
