package vn.nitrogen.platform.outbox;

import java.time.Duration;

public record OutboxPublisherSettings(
        String workerId,
        int batchSize,
        Duration leaseDuration,
        Duration confirmTimeout,
        OutboxRetryPolicy retryPolicy) {
}
