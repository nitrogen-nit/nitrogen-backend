# 0003 Transaction and outbox boundaries

## Status

Accepted.

## Context

Nitrogen uses PostgreSQL for authoritative state and RabbitMQ for async work.
Holding a database transaction while calling HTTP, RabbitMQ or object storage
can exhaust the connection pool and spread external latency into core writes.

The current design already uses an integration outbox table instead of Spring
Modulith's event publication registry.

## Decision

`@Transactional` belongs in service packages.

Controllers, repositories, DTOs, entities, configuration classes and event
handlers do not own transaction boundaries unless a future ADR names a specific
exception.

External calls must not happen inside a database transaction. Durable
publish-after-commit work goes through the integration outbox:

- write domain state and outbox row in one transaction;
- commit;
- publish from the outbox worker/publisher;
- record retry or processed-message state in integration tables.

## Consequences

Use cases have visible transaction scope. Controllers stay thin and do not hold
connections during serialization. Repository methods do not accidentally split
one use case into many small transactions.

The outbox adds a little operational code, but it gives retryability and avoids
two durable event mechanisms in the same backend.

## Alternatives considered

- Transactional controllers: simple at first, but keeps DB connections open for web concerns.
- Repository-owned transactions: hides use-case boundaries and makes multi-step writes non-atomic.
- Direct RabbitMQ publish inside transactions: can lose or duplicate messages around commit failures.
- Spring Modulith event publication registry: powerful, but would add a second durable event mechanism and tables not present in the current database design.

## Enforcement

- `TransactionBoundaryTest` blocks class or method `@Transactional` outside `service`.
- Module repository tests block repository transaction declarations.
- `NoExternalCallInTransactionTest` blocks HTTP/RabbitMQ/S3/MinIO calls from transactional methods/classes.
- CI runs these rules through the `backend-architecture-test` job.
