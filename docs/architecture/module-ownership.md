# Module ownership

Nitrogen uses one business module per PostgreSQL schema. A module owns its
schema, tables, repositories, services and REST surface. Other modules may call
only `api`, `dto` and, when it exists, `events`.

## Modules and schemas

| Module | Schema | Current owned tables |
|---|---|---|
| `identity` | `identity` | `users` |
| `curriculum` | `curriculum` | Schema reserved; no runtime table yet |
| `chemistry` | `chemistry` | Schema reserved; no runtime table yet |
| `content` | `content` | Schema reserved; no runtime table yet |
| `assessment` | `assessment` | Schema reserved; no runtime table yet |
| `examination` | `examination` | Schema reserved; no runtime table yet |
| `practice` | `practice` | `practice_attempts`, `attempt_items`, `exercise_responses`, `grading_runs`, `attempt_topic_results` |
| `progress` | `progress` | Schema reserved; no runtime table yet |
| `flashcard` | `flashcard` | Schema reserved; no runtime table yet |
| `simulation` | `simulation` | Schema reserved; no runtime table yet |
| `integration` | `integration` | `outbox_events`, `processed_messages` |
| `administration` | `administration` | Schema reserved; no runtime table yet |

## Migration ownership

- Migrations live under `src/main/resources/db/migration/<module>/`.
- File name convention is `V<timestamp>__<module>_<description>.sql`.
- Version numbers are unique across the whole repository.
- A migration writes only to its owner schema.
- Cross-schema references are allowed only when reviewed and listed in the architecture test.

Currently approved cross-schema reference:

| Owner | Referenced schema | Reason |
|---|---|---|
| `practice` | `identity` | `practice.practice_attempts.user_id` references `identity.users(id)` for database integrity |

Database foreign keys across schemas are allowed; JPA object associations across modules are not.
