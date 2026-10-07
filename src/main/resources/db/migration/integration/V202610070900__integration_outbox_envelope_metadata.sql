ALTER TABLE integration.outbox_events
    ADD COLUMN routing_key VARCHAR(160),
    ADD COLUMN correlation_id UUID,
    ADD COLUMN causation_id UUID;

UPDATE integration.outbox_events
SET routing_key = event_type
WHERE routing_key IS NULL;

UPDATE integration.outbox_events
SET correlation_id = id
WHERE correlation_id IS NULL;

ALTER TABLE integration.outbox_events
    ALTER COLUMN routing_key SET NOT NULL,
    ALTER COLUMN correlation_id SET NOT NULL;

ALTER TABLE integration.outbox_events
    ADD CONSTRAINT chk_outbox_routing_key_not_blank
        CHECK (length(trim(routing_key)) > 0);

CREATE INDEX ix_outbox_correlation_id
    ON integration.outbox_events (correlation_id);