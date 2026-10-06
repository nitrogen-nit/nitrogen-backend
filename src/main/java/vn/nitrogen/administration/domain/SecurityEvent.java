package vn.nitrogen.administration.domain;

import jakarta.persistence.*;
import vn.nitrogen.common.domain.AbstractIdentifiableEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "administration", name = "security_events")
public class SecurityEvent extends AbstractIdentifiableEntity {

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private SecurityEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private SecurityEventSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private SecurityEventOutcome outcome;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "reason")
    private String reason;

    @Column(name = "correlation_id", nullable = false, length = 128)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected SecurityEvent() {
    }

    public static SecurityEvent record(
            UUID actorId,
            SecurityEventType eventType,
            SecurityEventSeverity severity,
            SecurityEventOutcome outcome,
            String ipAddress,
            String userAgent,
            String reason,
            String correlationId,
            Instant occurredAt) {
        SecurityEvent event = new SecurityEvent();
        event.actorId = actorId;
        event.eventType = eventType;
        event.severity = severity;
        event.outcome = outcome;
        event.ipAddress = ipAddress;
        event.userAgent = userAgent;
        event.reason = reason;
        event.correlationId = correlationId;
        event.occurredAt = occurredAt;
        return event;
    }

    public UUID getActorId() {
        return actorId;
    }

    public SecurityEventType getEventType() {
        return eventType;
    }

    public SecurityEventSeverity getSeverity() {
        return severity;
    }

    public SecurityEventOutcome getOutcome() {
        return outcome;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReason() {
        return reason;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

}
