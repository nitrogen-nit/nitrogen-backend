CREATE TABLE administration.audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID NOT NULL,
    reason TEXT,
    correlation_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_audit_action_not_blank CHECK (length(trim(action)) > 0),
    CONSTRAINT chk_audit_target_type_not_blank CHECK (length(trim(target_type)) > 0),
    CONSTRAINT chk_audit_correlation_id_not_blank CHECK (length(trim(correlation_id)) > 0)
);

CREATE INDEX ix_audit_logs_target
    ON administration.audit_logs (target_type, target_id, occurred_at DESC);

CREATE INDEX ix_audit_logs_actor_time
    ON administration.audit_logs (actor_id, occurred_at DESC)
    WHERE actor_id IS NOT NULL;

CREATE INDEX ix_audit_logs_correlation_id
    ON administration.audit_logs (correlation_id);
