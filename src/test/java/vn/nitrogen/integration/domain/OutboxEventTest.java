package vn.nitrogen.integration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    @Test
    void pendingCreatesValidInitialState() {
        UUID aggregateId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-10-08T00:00:00Z");

        OutboxEvent event = OutboxEvent.pending(
                " USER ",
                aggregateId,
                " UserRegistered ",
                " identity.user.registered.v1 ",
                1,
                correlationId,
                null,
                Map.of("email", "user@example.com"),
                occurredAt);

        assertThat(event.getAggregateType()).isEqualTo("USER");
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getEventType()).isEqualTo("UserRegistered");
        assertThat(event.getRoutingKey()).isEqualTo("identity.user.registered.v1");
        assertThat(event.getSchemaVersion()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getCausationId()).isNull();
        assertThat(event.getPayload()).containsEntry("email", "user@example.com");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getNextRetryAt()).isEqualTo(occurredAt);
        assertThat(event.getLockedBy()).isNull();
        assertThat(event.getLockedUntil()).isNull();
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getFailedAt()).isNull();
        assertThat(event.getLastErrorCode()).isNull();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void pendingRejectsNonPositiveSchemaVersion() {
        UUID aggregateId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Map<String, Object> payload = Map.of();
        Instant occurredAt = Instant.now();

        assertThatThrownBy(() -> OutboxEvent.pending(
                        "USER",
                        aggregateId,
                        "UserRegistered",
                        "identity.user.registered.v1",
                        0,
                        correlationId,
                        null,
                        payload,
                        occurredAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schemaVersion");
    }

    @Test
    void pendingRejectsBlankRoutingKey() {
        UUID aggregateId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Map<String, Object> payload = Map.of();
        Instant occurredAt = Instant.now();

        assertThatThrownBy(() -> OutboxEvent.pending(
                        "USER",
                        aggregateId,
                        "UserRegistered",
                        " ",
                        1,
                        correlationId,
                        null,
                        payload,
                        occurredAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routingKey");
    }
}
