package vn.nitrogen.administration.dto;

import java.time.Instant;
import java.util.UUID;
import vn.nitrogen.administration.domain.SecurityEventOutcome;
import vn.nitrogen.administration.domain.SecurityEventSeverity;
import vn.nitrogen.administration.domain.SecurityEventType;

public record SecurityEventView(
        UUID id,
        UUID actorId,
        SecurityEventType eventType,
        SecurityEventSeverity severity,
        SecurityEventOutcome outcome,
        String ipAddress,
        String userAgent,
        String reason,
        String correlationId,
        Instant occurredAt) {
}
