---
layout: default
title: Audit Log And Security Events
---

# Audit Log And Security Events

Module `administration` sở hữu hai luồng ghi nhận dùng chung cho mọi module:

| | Audit log | Security event |
|---|---|---|
| Bảng | `administration.audit_logs` | `administration.security_events` |
| Ghi nhận | Ai đã làm gì với đối tượng nào | Sự kiện bảo mật: đăng nhập, khoá tài khoản, truy cập trái phép, rate limit |
| Facade | `AuditApi` | `SecurityEventApi` |
| Transaction | `MANDATORY` — phải nằm trong transaction của hành động được audit | `REQUIRES_NEW` — commit độc lập với transaction gọi |
| Ai đọc | Chỉ module `administration` | Chỉ module `administration` |

Module khác gọi hai facade này qua named interface `api`; enum của security event nằm trong `administration.dto` để module gọi không chạm vào `domain`.

## Audit Log

```java
auditApi.record(actorId, "USER_REGISTERED", "USER", userId, reason);
```

- `AuditService.record` dùng `Propagation.MANDATORY`: gọi ngoài transaction là lỗi. Audit và thay đổi dữ liệu commit hoặc rollback cùng nhau — audit rơi mất thì bản ghi mất giá trị pháp lý.
- `reason` đi qua `AuditRedactor` trước khi lưu.
- `correlation_id` lấy từ MDC của request hiện tại (`CorrelationId.currentOrNew()`).

| Cột | Ghi chú |
|---|---|
| `actor_id` | UUID người thực hiện; null với hành động hệ thống |
| `action` | Bắt buộc, tối đa 120 ký tự |
| `target_type`, `target_id` | Đối tượng bị tác động; bắt buộc |
| `reason` | Tuỳ chọn, đã redact |
| `correlation_id` | Bắt buộc |
| `occurred_at` | Thời điểm ghi |

Tra cứu: `AuditApi.findByTarget(targetType, targetId)` và `findByActor(actorId, from, to, limit)`.

## Security Event

```java
securityEventApi.record(actorId, SecurityEventType.LOGIN_FAILURE,
        SecurityEventSeverity.WARNING, SecurityEventOutcome.FAILURE,
        ipAddress, userAgent, reason);
```

- `SecurityEventService.record` dùng `Propagation.REQUIRES_NEW`: sự kiện vẫn được lưu kể cả khi transaction gọi rollback — một lần đăng nhập thất bại phải để lại dấu vết dù use case kết thúc bằng lỗi.
- `SecurityEventSanitizer` làm sạch đầu vào trước khi lưu:
  - `ip_address`: trim, cắt tối đa 45 ký tự (đủ cho IPv6);
  - `user_agent`: trim, cắt tối đa 512 ký tự, bỏ ký tự điều khiển;
  - `reason`: qua `AuditRedactor`.

| Enum | Giá trị |
|---|---|
| `SecurityEventType` | `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `ACCOUNT_LOCKED`, `UNAUTHORIZED_ACCESS`, `RATE_LIMITED` |
| `SecurityEventSeverity` | `INFO`, `WARNING`, `CRITICAL` |
| `SecurityEventOutcome` | `SUCCESS`, `FAILURE`, `BLOCKED` |

Tập giá trị được khoá bằng CHECK constraint trong DB; thêm giá trị mới cần cả enum và migration mở rộng constraint.

Tra cứu: `SecurityEventApi.findByActor(actorId, from, to, limit)`, `findByCorrelationId(correlationId)`, `findByEventType(eventType, limit)`.

Index: `(actor_id, occurred_at DESC)`, `(event_type, occurred_at DESC)`, `(correlation_id)`, `(ip_address, occurred_at DESC)`.

## Redaction

`AuditRedactor` thay giá trị của các cặp `key=value` nhạy cảm bằng `[REDACTED]`, không phân biệt hoa thường:

```text
password, token, secret, authorization, refresh_token,
access_token, reset_token, verification_token
```

Ví dụ: `"login failed password=hunter2"` → `"login failed password=[REDACTED]"`.

Redactor chỉ bắt dạng `key=value`; không truyền payload hay header thô vào `reason`.

## Giới hạn truy vấn

Các truy vấn có `limit` bị kẹp trong khoảng `1..100`. `from` null nghĩa là từ đầu (`Instant.EPOCH`), `to` null nghĩa là hiện tại.
