package vn.nitrogen.integration.dto;

import java.util.Map;
import java.util.UUID;

public record AppendOutboxCommand(
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String routingKey,
        int schemaVersion,
        UUID correlationId,
        UUID causationId,
        Map<String, Object> payload) {
}
