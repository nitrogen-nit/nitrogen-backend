# 0002 Cross-module references

## Status

Accepted.

## Context

Nitrogen schemas can use foreign keys for database integrity. JPA object
associations across modules are different: they let Hibernate lazy-load,
cascade and dirty-check through another module's aggregate.

That behavior makes a modular monolith drift toward one large persistence
model.

## Decision

Cross-module references in Java use IDs, not object associations.

- Store the referenced ID as `UUID`.
- Mark explicit cross-module database references with `@ModuleReference("<module>")`.
- Call the referenced module through its `api` facade when behavior or read model data is needed.
- Do not map JPA associations to another module with `@ManyToOne`, `@OneToOne`, `@OneToMany`, `@ManyToMany` or `@ElementCollection`.
- Do not keep direct entity/domain fields from another module.

Database foreign keys across schemas remain allowed when they protect data
integrity and are reviewed.

## Consequences

The database can stay consistent without merging module aggregates in Java.
Services must perform explicit lookups through module APIs instead of relying on
lazy-loading.

Ambiguous fields cannot be inferred safely from names alone. The
`@ModuleReference` marker makes UUID-only enforcement precise for future entity
code.

## Alternatives considered

- JPA associations across modules: convenient, but it makes transaction and loading boundaries unclear.
- Plain ID fields without annotation: low ceremony, but tooling cannot reliably know which IDs cross module boundaries.
- No database foreign keys across schemas: simpler ownership, but weaker integrity for important relations such as attempts to users.

## Enforcement

- `AbstractModuleEntityTest` blocks cross-module JPA/domain references.
- `@ModuleReference` fields must be `UUID`, target an existing module and not point back to the owning module.
- `FlywayMigrationOwnershipTest` requires cross-schema references to be explicitly approved.
- `ArchitectureRuleFixtureTest` includes negative fixtures for cross-module entity associations and non-UUID references.
