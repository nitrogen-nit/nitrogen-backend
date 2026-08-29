-- =====================================================================
-- identity.users — bản TỐI THIỂU
--
-- Đặc tả v1.0 không cung cấp DDL đầy đủ cho identity (§11 chỉ đặc tả DDL
-- cho practice và integration). Bảng này tồn tại để FK
-- practice.practice_attempts.user_id có đích tham chiếu và migration chạy
-- được từ DB rỗng.
--
-- TODO: thay bằng DDL đầy đủ khi identity được đặc tả — email verification,
-- password hash, trạng thái khoá tài khoản, oauth link, roles/user_roles,
-- refresh_tokens (§8, §18). Khi đó viết migration expand/contract (§16.3),
-- KHÔNG sửa file này vì nó đã chạy trên môi trường chung.
-- =====================================================================

CREATE TABLE identity.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE','PENDING','LOCKED','DISABLED')),
    CONSTRAINT chk_user_version CHECK (row_version >= 0)
);

CREATE UNIQUE INDEX uk_users_email ON identity.users (LOWER(email));
