package vn.nitrogen.platform.outbox;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("web")
public class OutboxPublisherConfig {

    @Bean
    OutboxPublisherSettings outboxPublisherSettings(
            @Value("${nitrogen.outbox.worker-id:${HOSTNAME:local}-${random.uuid}}") String workerId,
            @Value("${nitrogen.outbox.batch-size:100}") int batchSize,
            @Value("${nitrogen.outbox.lease-duration:PT30S}") Duration leaseDuration,
            @Value("${nitrogen.outbox.confirm-timeout:PT10S}") Duration confirmTimeout,
            @Value("${nitrogen.outbox.max-retries:8}") int maxRetries,
            @Value("${nitrogen.outbox.retry-base-delay:PT5S}") Duration retryBaseDelay,
            @Value("${nitrogen.outbox.retry-max-delay:PT15M}") Duration retryMaximumDelay) {
        return new OutboxPublisherSettings(
                normalizeWorkerId(workerId),
                batchSize,
                leaseDuration,
                confirmTimeout,
                new OutboxRetryPolicy(maxRetries, retryBaseDelay, retryMaximumDelay));
    }

    private static String normalizeWorkerId(String workerId) {
        String normalized = workerId == null ? "local" : workerId.trim();
        if (normalized.isEmpty()) {
            normalized = "local";
        }
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }
}
