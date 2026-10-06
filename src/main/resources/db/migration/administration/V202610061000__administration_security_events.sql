CREATE TABLE administration.security_events
(
    id             UUID PRIMARY KEY,
    actor_id       UUID,
    event_type     VARCHAR(80)  NOT NULL,
    severity       VARCHAR(20)  NOT NULL,
    outcome        VARCHAR(20)  NOT NULL,
    ip_address     VARCHAR(45),
    user_agent     VARCHAR(512),
    reason         TEXT,
    correlation_id VARCHAR(128) NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_security_event_type
        CHECK (event_type IN (
                              'LOGIN_SUCCESS',
                              'LOGIN_FAILURE',
                              'ACCOUNT_LOCKED',
                              'UNAUTHORIZED_ACCESS',
                              'RATE_LIMITED'
            )),
    CONSTRAINT chk_security_event_severity
        CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_security_event_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILURE', 'BLOCKED')),
    CONSTRAINT chk_security_event_correlation_id_not_blank
        CHECK (length(trim(correlation_id)) > 0)
);

CREATE INDEX ix_security_events_actor_time
    ON administration.security_events (actor_id, occurred_at DESC)
    WHERE actor_id IS NOT NULL;

CREATE INDEX ix_security_events_type_time
    ON administration.security_events (event_type, occurred_at DESC);

CREATE INDEX ix_security_events_correlation_id
    ON administration.security_events (correlation_id);

CREATE INDEX ix_security_events_ip_time
    ON administration.security_events (ip_address, occurred_at DESC)
    WHERE ip_address IS NOT NULL;