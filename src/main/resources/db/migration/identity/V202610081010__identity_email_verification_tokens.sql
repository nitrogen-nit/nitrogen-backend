-- Token xác minh email sau đăng ký (NIT-20).
-- Cùng hình dạng với password_reset_tokens: chỉ lưu hash, token thô chỉ đi qua email.
CREATE TABLE identity.email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_email_verification_token_hash_not_blank CHECK (length(trim(token_hash)) > 0),
    CONSTRAINT chk_email_verification_token_expiry CHECK (expires_at > issued_at),
    CONSTRAINT chk_email_verification_token_version CHECK (row_version >= 0)
);

CREATE UNIQUE INDEX uk_email_verification_tokens_hash
    ON identity.email_verification_tokens(token_hash);

CREATE INDEX ix_email_verification_tokens_user_active
    ON identity.email_verification_tokens(user_id, expires_at)
    WHERE consumed_at IS NULL;
