package vn.nitrogen.integration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
import vn.nitrogen.common.domain.AbstractIdentifiableEntity;
import vn.nitrogen.common.jpa.JsonbConverter;

@Entity
@Table(schema = "integration", name = "outbox_events")
public class OutboxEvent extends AbstractIdentifiableEntity {

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "routing_key", nullable = false, length = 160)
    private String routingKey;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Convert(converter = JsonbConverter.class)
    @ColumnTransformer(write = "CAST(? AS jsonb)")
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "causation_id")
    private UUID causationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "last_error_code", length = 80)
    private String lastErrorCode;

    @Column(name = "last_error")
    private String lastError;

    protected OutboxEvent() {
    }

    public static OutboxEvent pending(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String routingKey,
            int schemaVersion,
            UUID correlationId,
            UUID causationId,
            Map<String, Object> payload,
            Instant occurredAt) {
        String normalizedAggregateType =
                requireNonBlank(aggregateType, "aggregateType", 80);
        String normalizedEventType =
                requireNonBlank(eventType, "eventType", 120);
        String normalizedRoutingKey =
                requireNonBlank(routingKey, "routingKey", 160);

        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(occurredAt, "occurredAt");

        if (schemaVersion <= 0) {
            throw new IllegalArgumentException(
                    "schemaVersion must be positive");
        }

        OutboxEvent event = new OutboxEvent();
        event.aggregateType = normalizedAggregateType;
        event.aggregateId = aggregateId;
        event.eventType = normalizedEventType;
        event.routingKey = normalizedRoutingKey;
        event.schemaVersion = schemaVersion;
        event.payload = Collections.unmodifiableMap(
                new LinkedHashMap<>(payload));
        event.status = OutboxStatus.PENDING;
        event.retryCount = 0;
        event.correlationId = correlationId;
        event.causationId = causationId;
        event.occurredAt = occurredAt;
        event.nextRetryAt = occurredAt;
        return event;
    }

    private static String requireNonBlank(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        String normalized = value.trim();

        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed "
                            + maximumLength + " characters");
        }

        return normalized;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Map<String, Object> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public UUID getCausationId() {
        return causationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getLastError() {
        return lastError;
    }
}
