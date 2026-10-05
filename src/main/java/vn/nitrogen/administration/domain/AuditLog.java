package vn.nitrogen.administration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.nitrogen.common.domain.AbstractIdentifiableEntity;

@Entity
@Table(schema = "administration", name = "audit_logs")
public class AuditLog extends AbstractIdentifiableEntity {

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "action", nullable = false, length = 120)
    private String action;

    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "correlation_id", nullable = false, length = 128)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLog() {
    }

    public static AuditLog record(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            String reason,
            String correlationId,
            Instant occurredAt) {
        AuditLog log = new AuditLog();
        log.actorId = actorId;
        log.action = action;
        log.targetType = targetType;
        log.targetId = targetId;
        log.reason = reason;
        log.correlationId = correlationId;
        log.occurredAt = occurredAt;
        return log;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
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
