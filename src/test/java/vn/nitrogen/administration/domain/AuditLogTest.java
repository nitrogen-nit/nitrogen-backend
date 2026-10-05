package vn.nitrogen.administration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AuditLogTest {

    @Test
    void recordCreatesAuditLogSnapshot() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-10-05T10:15:30Z");

        AuditLog log = AuditLog.record(
                actorId,
                "USER_REGISTERED",
                "USER",
                targetId,
                "registration accepted",
                "correlation-123",
                occurredAt);

        assertThat(log.getActorId()).isEqualTo(actorId);
        assertThat(log.getAction()).isEqualTo("USER_REGISTERED");
        assertThat(log.getTargetType()).isEqualTo("USER");
        assertThat(log.getTargetId()).isEqualTo(targetId);
        assertThat(log.getReason()).isEqualTo("registration accepted");
        assertThat(log.getCorrelationId()).isEqualTo("correlation-123");
        assertThat(log.getOccurredAt()).isEqualTo(occurredAt);
    }
}
