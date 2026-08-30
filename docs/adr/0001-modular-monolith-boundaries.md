# 0001 Modular monolith boundaries

## Status

Accepted.

## Context

Nitrogen is a Spring Boot modular monolith using Spring Modulith and ArchUnit.
The current codebase has 12 business modules plus two open technical modules:
`common` and `platform`.

Business modules start from a minimal dependency set: `common` and `platform`.
Any dependency on another business module must be visible in
`@ApplicationModule(allowedDependencies = ...)`.

## Decision

Each business module owns its package, public facade, DTOs, persistence model
and PostgreSQL schema.

Allowed cross-module packages are:

- `api`
- `dto`
- `events`, only when an events package actually exists

The following packages are internal to the owning module:

- `domain`
- `repository`
- `service`
- `web`

`common` and `platform` remain open modules because they are shared kernel and
technical infrastructure, not business capabilities.

## Consequences

Module coupling is explicit and reviewable. Cross-module shortcuts through
entities, repositories, services or controllers fail in CI. Adding a module
requires a package-info declaration, migration folder and architecture test
class.

The design is stricter than allowing direct domain access across modules. That
strictness keeps Hibernate object graphs inside one module boundary.

## Alternatives considered

- Package-by-layer: simpler initially, but it hides business ownership.
- Microservices: too much operational cost for the current learning/product scope.
- Open domain packages across modules: convenient, but it encourages JPA associations and shared aggregates.

## Enforcement

- `ModularityTest` calls Spring Modulith `ApplicationModules.verify()`.
- `AbstractModuleAccessTest` blocks access to internal packages.
- Module layer tests enforce `domain`, `repository`, `service` and `web` placement.
- CI job `backend-architecture-test` runs `./mvnw -B --no-transfer-progress test -Parchitecture`.
