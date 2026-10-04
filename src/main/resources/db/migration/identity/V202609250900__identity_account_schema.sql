ALTER TABLE identity.users
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN email_verified_at TIMESTAMPTZ,
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    ADD COLUMN last_login_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_users_email_verified_at
        CHECK ((email_verified = false AND email_verified_at IS NULL)
            OR (email_verified = true AND email_verified_at IS NOT NULL)),
    ADD CONSTRAINT chk_users_timezone_not_blank
        CHECK (length(trim(timezone)) > 0);

CREATE TABLE identity.user_roles (
                                     user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
                                     role VARCHAR(64) NOT NULL,
                                     assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     PRIMARY KEY (user_id, role),
                                     CONSTRAINT chk_user_roles_role_not_blank CHECK (length(trim(role)) > 0)
);

CREATE INDEX ix_user_roles_role ON identity.user_roles(role);

CREATE TABLE identity.oauth_accounts (
                                         id UUID PRIMARY KEY,
                                         user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
                                         provider VARCHAR(64) NOT NULL,
                                         provider_subject VARCHAR(255) NOT NULL,
                                         provider_email VARCHAR(255),
                                         linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         row_version BIGINT NOT NULL DEFAULT 0,
                                         CONSTRAINT chk_oauth_provider_not_blank CHECK (length(trim(provider)) > 0),
                                         CONSTRAINT chk_oauth_subject_not_blank CHECK (length(trim(provider_subject)) > 0),
                                         CONSTRAINT chk_oauth_version CHECK (row_version >= 0)
);

CREATE UNIQUE INDEX uk_oauth_provider_subject
    ON identity.oauth_accounts(provider, provider_subject);

CREATE INDEX ix_oauth_user_id ON identity.oauth_accounts(user_id);

CREATE TABLE identity.refresh_tokens (
                                         id UUID PRIMARY KEY,
                                         user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
                                         token_hash VARCHAR(255) NOT NULL,
                                         issued_at TIMESTAMPTZ NOT NULL,
                                         expires_at TIMESTAMPTZ NOT NULL,
                                         revoked_at TIMESTAMPTZ,
                                         row_version BIGINT NOT NULL DEFAULT 0,
                                         CONSTRAINT chk_refresh_token_hash_not_blank CHECK (length(trim(token_hash)) > 0),
                                         CONSTRAINT chk_refresh_token_expiry CHECK (expires_at > issued_at),
                                         CONSTRAINT chk_refresh_token_version CHECK (row_version >= 0)
);

CREATE UNIQUE INDEX uk_refresh_tokens_hash ON identity.refresh_tokens(token_hash);
CREATE INDEX ix_refresh_tokens_user_active
    ON identity.refresh_tokens(user_id, expires_at)
    WHERE revoked_at IS NULL;

CREATE TABLE identity.password_reset_tokens (
                                                id UUID PRIMARY KEY,
                                                user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
                                                token_hash VARCHAR(255) NOT NULL,
                                                issued_at TIMESTAMPTZ NOT NULL,
                                                expires_at TIMESTAMPTZ NOT NULL,
                                                consumed_at TIMESTAMPTZ,
                                                row_version BIGINT NOT NULL DEFAULT 0,
                                                CONSTRAINT chk_reset_token_hash_not_blank CHECK (length(trim(token_hash)) > 0),
                                                CONSTRAINT chk_reset_token_expiry CHECK (expires_at > issued_at),
                                                CONSTRAINT chk_reset_token_version CHECK (row_version >= 0)
);

CREATE UNIQUE INDEX uk_password_reset_tokens_hash
    ON identity.password_reset_tokens(token_hash);

CREATE INDEX ix_password_reset_tokens_user_active
    ON identity.password_reset_tokens(user_id, expires_at)
    WHERE consumed_at IS NULL;