package vn.nitrogen.administration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vn.nitrogen.administration.dto.SecurityEventOutcome;
import vn.nitrogen.administration.dto.SecurityEventSeverity;
import vn.nitrogen.administration.dto.SecurityEventType;

@Tag("unit")
class SecurityEventTest {

    @Test
    void recordCreatesSecurityEventSnapshot() {
        UUID actorId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-10-06T00:00:00Z");

        SecurityEvent event = SecurityEvent.record(
                actorId,
                SecurityEventType.LOGIN_FAILURE,
                SecurityEventSeverity.WARNING,
                SecurityEventOutcome.FAILURE,
                "127.0.0.1",
                "Mozilla/5.0",
                "bad credentials",
                "correlation-123",
                occurredAt);

        assertThat(event.getActorId()).isEqualTo(actorId);
        assertThat(event.getEventType()).isEqualTo(SecurityEventType.LOGIN_FAILURE);
        assertThat(event.getSeverity()).isEqualTo(SecurityEventSeverity.WARNING);
        assertThat(event.getOutcome()).isEqualTo(SecurityEventOutcome.FAILURE);
        assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(event.getReason()).isEqualTo("bad credentials");
        assertThat(event.getCorrelationId()).isEqualTo("correlation-123");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }
}
