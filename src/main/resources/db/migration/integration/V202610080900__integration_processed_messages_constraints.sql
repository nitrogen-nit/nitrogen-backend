ALTER TABLE integration.processed_messages
    ADD CONSTRAINT chk_processed_messages_consumer_name_not_blank
        CHECK (length(trim(consumer_name)) > 0);

CREATE INDEX ix_processed_messages_processed_at
    ON integration.processed_messages (processed_at);
