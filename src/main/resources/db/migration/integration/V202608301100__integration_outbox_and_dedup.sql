-- =====================================================================
-- Transactional Outbox + consumer dedup — DDL theo đặc tả §11.4
--
-- Luồng (§5.5): HTTP transaction ghi business state và outbox event cùng
-- lúc. Publisher claim batch bằng FOR UPDATE SKIP LOCKED, ghi lease rồi
-- COMMIT TRƯỚC khi gọi RabbitMQ — nhiều instance không giữ row lock trong
-- lúc chờ broker.
--
-- RabbitMQ là at-least-once ⇒ consumer BẮT BUỘC idempotent: chèn
-- processed_messages trong cùng transaction với business update.
-- =====================================================================

CREATE TABLE integration.outbox_events (
    -- TODO(BL-13): §10.1 mô tả cột này là "message_id" nhưng DDL §11.4 đặt là
    --              "id", trong khi envelope (§17.3) và processed_messages dùng
    --              message_id. Cần thống nhất một tên.
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    schema_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(24) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    occurred_at TIMESTAMPTZ NOT NULL,
    next_retry_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(120),
    locked_until TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    last_error_code VARCHAR(80),
    last_error TEXT,
    CONSTRAINT chk_outbox_version CHECK (schema_version > 0),
    CONSTRAINT chk_outbox_retry CHECK (retry_count >= 0),
    CONSTRAINT chk_outbox_payload CHECK (jsonb_typeof(payload) = 'object'),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED'))
);

-- Partial index: publisher chỉ quét PENDING, không đụng tới hàng đã publish.
-- Giữ index nhỏ kể cả khi bảng đã tích luỹ nhiều event chờ purge (§14.2: 7–30 ngày).
CREATE INDEX idx_outbox_publishable
ON integration.outbox_events (status, locked_until, next_retry_at, occurred_at)
WHERE status = 'PENDING';


CREATE TABLE integration.processed_messages (
    consumer_name VARCHAR(120) NOT NULL,
    message_id UUID NOT NULL,
    correlation_id UUID,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_name, message_id)
);
