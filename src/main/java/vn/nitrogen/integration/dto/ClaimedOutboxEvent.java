package vn.nitrogen.integration.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Event đã được một publisher giữ lease và có thể gửi ra broker. */
public record ClaimedOutboxEvent(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String routingKey,
        int schemaVersion,
        Map<String, Object> payload,
        int retryCount,
        UUID correlationId,
        UUID causationId,
        Instant occurredAt) {
}
