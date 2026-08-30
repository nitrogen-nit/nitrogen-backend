# Architecture review checklist

Use this checklist for pull requests that add backend behavior, persistence,
REST endpoints, messaging or module dependencies.

## Module boundary

- [ ] New code belongs to the module that owns the use case.
- [ ] Cross-module calls go through `api`, `dto` or documented `events`.
- [ ] New module dependency is declared in `@ApplicationModule(allowedDependencies = ...)`.
- [ ] Spring Modulith verification and ArchUnit tests pass.

## Public API/events

- [ ] Public module facade exposes business intent, not repositories or entities.
- [ ] DTOs are stable enough for cross-module use.
- [ ] Events are immutable and versionable if introduced.

## DTO-only REST

- [ ] Base path starts with `/api/v1`.
- [ ] Resource path uses plural nouns and kebab-case.
- [ ] Request and response bodies are DTOs.
- [ ] Bean Validation is applied to input DTOs.
- [ ] OpenAPI/JSON Schema contract is updated when the public contract changes.

## Repository ownership

- [ ] Repository is in the owning module's `repository` package.
- [ ] Repository is used only by the owning module's `service` or `api`.
- [ ] Controllers do not call repositories.
- [ ] `EntityManager` is limited to named custom repository implementations.

## Transaction boundary

- [ ] `@Transactional` is declared only in `service`.
- [ ] Transaction scope matches one use case.
- [ ] No controller, repository, entity, DTO or config class opens a transaction.

## Cross-module UUID reference

- [ ] No JPA association points to another module.
- [ ] Cross-module database references use `UUID`.
- [ ] Cross-module ID fields are marked with `@ModuleReference("<module>")`.
- [ ] Database FK is kept when integrity matters.

## Flyway ownership

- [ ] Migration is under `db/migration/<module>/`.
- [ ] Migration file name starts with the owner module after the version.
- [ ] Migration version is unique.
- [ ] Migration writes only to the owner schema.
- [ ] Any cross-schema reference is documented and approved in architecture tests.

## External call/outbox

- [ ] No HTTP/RabbitMQ/S3/MinIO call happens inside a DB transaction.
- [ ] Durable publish-after-commit work uses the integration outbox.
- [ ] Retry/idempotency behavior is clear.

## Security

- [ ] Endpoints require the correct authentication and authorization.
- [ ] Error responses do not expose stack traces, SQL or infrastructure detail.
- [ ] Secrets are read from environment/secret store, never committed.

## Tests

- [ ] Unit tests cover business logic.
- [ ] Integration tests cover PostgreSQL/Flyway behavior when persistence changes.
- [ ] Architecture tests are updated when a new module or boundary rule is added.
- [ ] Negative fixtures are added for new architecture rules.

## Observability

- [ ] Logs include correlation ID where request context exists.
- [ ] Important use cases have meaningful metrics/traces.
- [ ] Health/readiness behavior matches the dependency being added.

## Documentation/ADR

- [ ] README or docs are updated for new conventions.
- [ ] ADR is added or updated for a new architectural decision.
