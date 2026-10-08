# Coding conventions

These conventions keep the modular monolith small enough to reason about. Use
the smallest rule that protects the design; avoid framework ceremony that does
not buy safety.

## Java and Spring

- Use the smallest useful access modifier. Package-private is preferred for implementation classes.
- Use constructor injection.
- Do not use field injection.
- Do not use `@Lazy` or `ObjectProvider` to hide dependency cycles.
- Module API facades live in `api`, implement `ModuleApi`, are annotated `@Controller` + `@Profile("core")`, and most are `@Lazy`. Facades that are still skeletons throw `UnsupportedOperationException("TODO: ...")` and keep the TODO in their Javadoc until a real service backs them.
- Class and method names should describe domain intent, not technical plumbing.
- Constants use `SCREAMING_SNAKE_CASE`.
- Manual logger fields are named `log`.
- Keep methods focused on one responsibility.
- Do not duplicate business logic across modules; expose a module API instead.
- Do not hard-code OS-specific paths.

## Module layout

Each business module uses the same subpackages:

| Package | Contents | Visible to other modules |
|---|---|---|
| `api` | Facades (`*Api`) for other modules | Yes |
| `dto` | Immutable records used by `api` and REST | Yes |
| `events` | Published event types, when they exist | Yes |
| `domain` | JPA entities and enums | No |
| `repository` | Spring Data repositories and `*RepositoryImpl` | No |
| `service` | Use cases and transaction boundaries | No |
| `web` | `@RestController` classes | No |

Dependencies inside a module point one way: `web → service → repository → domain`. `api` must not depend on `web`.

## Persistence

- JPA entities live in `domain`.
- Repositories live in `repository`.
- `EntityManager` is only for custom repository implementations named `*RepositoryImpl`.
- Associations default to `LAZY`. Do not use `EAGER` to hide `LazyInitializationException`.
- Cross-module object associations are forbidden. Store a UUID reference and call the other module through `api`.
- Mark cross-module database references with `@ModuleReference("<module>")`; the field type must be `UUID`.
- SQL and JPQL should be formatted for review, not compressed into one hard-to-read line.

## Transactions and external calls

- `@Transactional` belongs in `service`.
- Every `@Service` declares a transaction boundary: `@Transactional` on the class or on at least one public method.
- Controllers, repositories, DTOs, entities, config classes and event handlers do not own transaction boundaries unless an ADR explicitly approves a named exception.
- Do not call HTTP clients, RabbitMQ, S3 or MinIO inside a database transaction.
- Use the outbox pattern for durable work that must be published after commit.

## REST and contracts

- Controllers are thin: validation, authorization handoff, service call, mapping.
- Request and response contracts are DTOs.
- Query parameters are declared explicitly.
- Public API drift is controlled through OpenAPI and JSON Schema under `contracts/`.

## Enforcement

- Spotless handles whitespace and line endings.
- SpotBugs handles high-confidence bytecode defects.
- ArchUnit and Spring Modulith enforce boundaries, transaction ownership, repository ownership, DTO-only REST signatures and persistence rules.
- JaCoCo fails `verify` below 80% line and 70% branch coverage (`*Application*`, `*Config*`, `api/` and `dto/` are excluded).
- CI (`.github/workflows/ci.yml`) runs format, unit, architecture, integration, observability, migration, build and Sonar jobs.
