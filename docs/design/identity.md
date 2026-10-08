---
layout: default
title: Identity Schema And Token Model
---

# Identity Schema And Token Model

Tài liệu này mô tả phần nền tảng của module Identity: user, role, OAuth account, refresh token và password reset token.

## UML

```mermaid
classDiagram
    User "1" --> "*" OAuthAccount : links
    User "1" --> "*" RefreshToken : owns
    User "1" --> "*" PasswordResetToken : owns
    User "1" --> "*" EmailVerificationToken : owns
    User "1" --> "*" UserRole : has

    class User {
        UUID id
        String email
        String displayName
        UserStatus status
        String passwordHash
        boolean emailVerified
        Instant emailVerifiedAt
        String timezone
        Instant lastLoginAt
        long rowVersion
    }

    class UserRole {
        UUID userId
        String role
        Instant assignedAt
    }

    class OAuthAccount {
        UUID id
        User user
        String provider
        String providerSubject
        String providerEmail
        Instant linkedAt
        Instant updatedAt
        long rowVersion
    }

    class RefreshToken {
        UUID id
        User user
        String tokenHash
        Instant issuedAt
        Instant expiresAt
        Instant revokedAt
        long rowVersion
    }

    class PasswordResetToken {
        UUID id
        User user
        String tokenHash
        Instant issuedAt
        Instant expiresAt
        Instant consumedAt
        long rowVersion
    }
```

## Registration And Email Verification Flow

`POST /api/v1/auth/registrations` và `POST /api/v1/auth/email-verifications` là endpoint công khai.

```mermaid
flowchart TD
    A[Client gửi email, password, displayName] --> B[Kiểm password ≤ 72 byte, băm BCrypt — ngoài transaction]
    B --> C{Email đã tồn tại?}
    C -- Có --> D[Security event REGISTRATION_DUPLICATE_EMAIL]
    C -- Không --> E[Tạo user PENDING + email_verification_tokens hash]
    E --> F[Audit USER_REGISTERED + outbox UserRegistered, EmailVerificationRequested]
    F --> G[Commit, rồi security event REGISTRATION_SUCCESS]
    D --> H[202 VERIFICATION_PENDING — giống hệt nhau ở hai nhánh]
    G --> H
    H --> I[User mở link, client gửi token]
    I --> J{Token tồn tại, chưa dùng, chưa hết hạn?}
    J -- Không --> K[400 EMAIL_VERIFICATION_TOKEN_INVALID — một lỗi chung]
    J -- Có --> L[consumed_at, email_verified, PENDING → ACTIVE, audit USER_EMAIL_VERIFIED]
    L --> M[204]
```

- Không lộ email đã tồn tại: cùng status, cùng body, không trả user id; nhánh trùng vẫn băm mật khẩu.
- Chỉ lưu SHA-256 của token (256 bit ngẫu nhiên). Token thô chỉ nằm trong payload `identity.EmailVerificationRequested` cho bên gửi email; `identity.UserRegistered` không mang email hay token.
- TTL token: `nitrogen.identity.email-verification.ttl` (mặc định `PT24H`).

## Password Reset Flow

```mermaid
flowchart TD
    A[User yêu cầu quên mật khẩu] --> B[Tạo raw reset token]
    B --> C[Hash token]
    C --> D[Lưu password_reset_tokens]
    D --> E[Gửi link reset qua email]
    E --> F[User mở link reset]
    F --> G[Hash token từ link]
    G --> H[Tìm token còn hiệu lực]
    H --> I{Token tồn tại, chưa dùng, chưa hết hạn?}
    I -- Không --> J[Trả lỗi token không hợp lệ]
    I -- Có --> K[Cập nhật password_hash]
    K --> L[Set consumed_at]
    L --> M[Hoàn tất reset password]
```

## Refresh Token Flow

```mermaid
flowchart TD
    A[Client gửi refresh token] --> B[Hash refresh token]
    B --> C[Tìm refresh_tokens còn hiệu lực]
    C --> D{Token tồn tại, chưa revoke, chưa hết hạn?}
    D -- Không --> E[Trả 401]
    D -- Có --> F[Phát hành access token mới]
    F --> G[Tuỳ chính sách: rotate refresh token]
    G --> H[Trả token response]
```

## Database Tables

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    USERS ||--o{ OAUTH_ACCOUNTS : links
    USERS ||--o{ REFRESH_TOKENS : owns
    USERS ||--o{ PASSWORD_RESET_TOKENS : owns

    USERS {
        uuid id PK
        varchar email
        varchar display_name
        varchar status
        varchar password_hash
        boolean email_verified
        timestamptz email_verified_at
        varchar timezone
        timestamptz last_login_at
        bigint row_version
    }

    USER_ROLES {
        uuid user_id PK,FK
        varchar role PK
        timestamptz assigned_at
    }

    OAUTH_ACCOUNTS {
        uuid id PK
        uuid user_id FK
        varchar provider
        varchar provider_subject
        varchar provider_email
        bigint row_version
    }

    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        varchar token_hash
        timestamptz issued_at
        timestamptz expires_at
        timestamptz revoked_at
        bigint row_version
    }

    PASSWORD_RESET_TOKENS {
        uuid id PK
        uuid user_id FK
        varchar token_hash
        timestamptz issued_at
        timestamptz expires_at
        timestamptz consumed_at
        bigint row_version
    }
```
