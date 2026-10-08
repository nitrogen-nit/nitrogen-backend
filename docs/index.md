---
layout: default
title: Nitrogen Design Docs
---

# Nitrogen Design Docs

Trang này gom tài liệu thiết kế, quyết định kiến trúc và ghi chú triển khai cho backend Nitrogen.

## Trạng thái triển khai

| Phần | Trạng thái |
|---|---|
| Identity | Schema, entity và repository cho user, role, OAuth account, refresh/reset token; chưa có endpoint xác thực |
| Practice | Bắt đầu và tra cứu lượt luyện tập qua REST |
| Administration | Ghi và tra cứu audit log, security event |
| Integration / messaging | Transactional outbox, outbox publisher, idempotent consumer, RabbitMQ topology |
| Curriculum, chemistry, content, assessment, examination, progress, flashcard, simulation | Mới có schema và facade khung (`UnsupportedOperationException`) |
| Security | Filter chain tối thiểu; chưa có JWT resource server và phân quyền theo role |

## Thiết Kế Feature

- [Identity schema and token model](design/identity.md)
- [Audit log and security events](design/audit-and-security-events.md)
- [Messaging, outbox and idempotent consumer](design/messaging.md)

## Architecture

- [Module ownership](architecture/module-ownership.md)
- [Coding conventions](architecture/coding-conventions.md)
- [REST API conventions](architecture/rest-api-conventions.md)
- [Review checklist](architecture/review-checklist.md)

## ADR

- [ADR 0001: Modular monolith boundaries](adr/0001-modular-monolith-boundaries.md)
- [ADR 0002: Cross-module references](adr/0002-cross-module-references.md)
- [ADR 0003: Transaction and outbox boundaries](adr/0003-transaction-and-outbox-boundaries.md)

## Operations

- [Environment and secrets](environment-and-secrets.md)
- [Observability](observability.md)
