# Environment And Secrets

Nitrogen backend uses one deployable artifact and combines two profile families:

| Profile family | Values | Purpose |
|---|---|---|
| Runtime mode | `web`, `worker` | Selects the application role. |
| Environment | `local`, `dev`, `prod` | Selects configuration source and operational safety rules. |

Use profiles in pairs:

```text
SPRING_PROFILES_ACTIVE=web,local
SPRING_PROFILES_ACTIVE=worker,dev
SPRING_PROFILES_ACTIVE=web,prod
```

`application.yml` contains shared configuration only. Local defaults live in
`application-local.yml`. Shared development and production environments must
provide endpoints and credentials through environment variables or a secret
store.

`web` and `worker` both activate the `core` profile group automatically, so
cross-module facades (`@Profile("core")`) exist in either mode. Tests run with
the `test` profile (`src/test/resources/application-test.yml`), where
Testcontainers supplies PostgreSQL and RabbitMQ.

`application.yml` also imports `optional:file:.env.local[.properties]`, so a
process started from the repository root reads `.env.local` directly.

## Profile Behavior

| Profile | Flyway | Hibernate DDL | Credential source | Intended use |
|---|---:|---|---|---|
| `local` | Enabled | `validate` | `.env.local`, falling back to safe local defaults in `application-local.yml` | Developer laptop |
| `dev` | Enabled | `validate` | GitHub Environment, AWS SSM Parameter Store, or runtime host env | Shared development |
| `prod` | Disabled | `validate` | GitHub Environment plus production host secret store | Production runtime |

Production keeps Flyway disabled in the application process. The deployment
pipeline must run migration as a separate step before rolling out the app.

| Runtime mode | HTTP port | Scheduler / outbox publisher | RabbitMQ listeners | Rabbit health |
|---|---|---|---|---|
| `web` | `NITROGEN_WEB_PORT` (default `8080`) | On | Off | `NITROGEN_RABBIT_HEALTH_ENABLED` (default `false`; `true` under `local`) |
| `worker` | `NITROGEN_WORKER_PORT` (default `8081`, actuator only) | Off | On, manual ack | Always on |

## Required Variables

These have no default outside `local` and must be set for `dev` and `prod`.

| Name | Type | Example | Stored in | Used by |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Variable | `web,dev` | GitHub Environment variable; runtime service env | Spring profile activation |
| `NITROGEN_ENVIRONMENT` | Variable | `dev` | GitHub Environment variable; AWS SSM `/nitrogen/dev/environment`; Hetzner runtime env | Banner, metrics tag, logs, OpenTelemetry resource |
| `NITROGEN_DB_URL` | Variable | `jdbc:postgresql://db.example:5432/nitrogen?sslmode=require` | GitHub Environment variable; AWS SSM `/nitrogen/dev/db/url`; Hetzner runtime env | Spring datasource |
| `NITROGEN_DB_USER` | Variable | `nitrogen_app` | GitHub Environment variable; AWS SSM `/nitrogen/dev/db/user`; Hetzner runtime env | Spring datasource |
| `NITROGEN_DB_PASSWORD` | Secret | — | AWS Secrets Manager or SSM SecureString `/nitrogen/dev/db/password`; Hetzner host secret | Spring datasource |
| `NITROGEN_RABBIT_HOST` | Variable | `rabbitmq.internal` | GitHub Environment variable; AWS SSM `/nitrogen/dev/rabbit/host`; Hetzner runtime env | Spring AMQP |
| `NITROGEN_RABBIT_PORT` | Variable | `5672` | GitHub Environment variable; AWS SSM `/nitrogen/dev/rabbit/port`; Hetzner runtime env | Spring AMQP |
| `NITROGEN_RABBIT_USER` | Variable | `nitrogen_app` | GitHub Environment variable; AWS SSM `/nitrogen/dev/rabbit/user`; Hetzner runtime env | Spring AMQP |
| `NITROGEN_RABBIT_PASSWORD` | Secret | — | AWS Secrets Manager or SSM SecureString `/nitrogen/dev/rabbit/password`; Hetzner host secret | Spring AMQP |

`NITROGEN_OTLP_ENDPOINT` is also required in practice whenever tracing is
enabled outside a laptop: its default is `http://localhost:4318/v1/traces`.

## Variables With Defaults

| Name | Default | Local | Dev | Prod | Used by |
|---|---|---|---|---|---|
| `NITROGEN_DB_POOL_SIZE` | `10` | `5` | small shared value | capacity value | HikariCP |
| `NITROGEN_RABBIT_HEALTH_ENABLED` | `false` | `true` | `false` for web | `false` for web | Actuator health (worker ignores it and always checks Rabbit) |
| `NITROGEN_LOG_LEVEL` | `INFO` | `INFO` | `INFO`, `DEBUG` temporarily | `INFO` | `vn.nitrogen` log level |
| `NITROGEN_APPLICATION_VERSION` | `spring.application.version` or `unknown` | `local` | image tag or git sha | release tag or git sha | Banner, metrics tag, logs, OpenTelemetry resource |
| `NITROGEN_TRACING_ENABLED` | `false` | `false` | `true` | `true` | Micrometer/OpenTelemetry tracing |
| `NITROGEN_TRACING_SAMPLING_PROBABILITY` | `0.0` | `0.0` | `1.0` | `0.1` | Trace sampling |
| `NITROGEN_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | same | collector URL | collector URL | OTLP trace export |
| `NITROGEN_WEB_PORT` | `8080` | `8080` | — | — | `web` server port; Docker Compose host port |
| `NITROGEN_WORKER_PORT` | `8081` | — | — | — | `worker` server port |
| `NITROGEN_RABBIT_PREFETCH` | `10` | | | | Worker listener prefetch |
| `NITROGEN_RABBIT_CONCURRENCY` | `2` | | | | Worker listener consumers |
| `NITROGEN_RABBIT_MAX_CONCURRENCY` | `8` | | | | Worker listener max consumers |
| `NITROGEN_OUTBOX_PUBLISH_INTERVAL` | `PT5S` | | | | Outbox publisher (web) |
| `NITROGEN_OUTBOX_BATCH_SIZE` | `100` | | | | Outbox publisher |
| `NITROGEN_OUTBOX_LEASE_DURATION` | `PT30S` | | | | Outbox publisher |
| `NITROGEN_OUTBOX_CONFIRM_TIMEOUT` | `PT10S` | | | | Outbox publisher |
| `NITROGEN_OUTBOX_MAX_RETRIES` | `8` | | | | Outbox publisher |
| `NITROGEN_OUTBOX_RETRY_BASE_DELAY` | `PT5S` | | | | Outbox publisher |
| `NITROGEN_OUTBOX_RETRY_MAX_DELAY` | `PT15M` | | | | Outbox publisher |

Outbox settings are described in [Messaging](design/messaging.md).

## Optional Local Variables

| Name | Example | Used by |
|---|---|---|
| `NITROGEN_DB_NAME` | `nitrogen` | Docker Compose PostgreSQL bootstrap |
| `NITROGEN_WEB_SCHEME` | `http` | Startup URL logged by `WebStartupReporter`; `open-browser-when-ready.sh` |
| `NITROGEN_WEB_HOST` | `localhost` | Same as above |
| `NITROGEN_RABBIT_MANAGEMENT_PORT` | `15672` | RabbitMQ Management UI host port |
| `NITROGEN_HIBERNATE_SQL_LOG_LEVEL` | `WARN` | `org.hibernate.SQL` level under `local` |
| `NITROGEN_DOCKER_OTLP_ENDPOINT` | `http://host.docker.internal:4318/v1/traces` | OTLP endpoint for the Compose backend container |

## Local Workflow

```bash
cp .env.local.example .env.local
./scripts/local-up.sh
./scripts/local-smoke.sh
./scripts/local-down.sh
```

| Script | What it does |
|---|---|
| `local-up.sh` | Creates `.env.local` from the example if missing, starts PostgreSQL 16 and RabbitMQ Management, builds and starts the `backend-web` container, waits for health, then runs the smoke test |
| `local-smoke.sh` | Checks PostgreSQL and RabbitMQ, backend liveness/readiness, `/actuator/info`, and that Flyway applied SQL migrations with no failures |
| `local-down.sh` | Stops the Compose stack |
| `local-reset.sh --yes` | Stops the stack and deletes the PostgreSQL and RabbitMQ volumes |
| `run-web-dev.sh` | Runs the backend on the host with `./mvnw spring-boot:run` (profiles from `SPRING_PROFILES_ACTIVE`, default `web,local`) |
| `open-browser-when-ready.sh [path]` | Waits for readiness, then opens the browser (default path `/swagger-ui.html`) |

PostgreSQL, RabbitMQ and backend ports are bound to `127.0.0.1` only.
RabbitMQ Management is available at `http://localhost:15672` by default.

## IntelliJ Run Configuration

- Working directory: the repository root, so `.env.local` is picked up by the
  application's own `spring.config.import`.
- Active profiles: `web,local`.

Start PostgreSQL and RabbitMQ first (for example `docker compose -f
compose.local.yml up -d postgres rabbitmq`). The startup log prints the backend
URL once the web server is up.

## Shared Dev On AWS

Recommended storage for the current learning setup:

| Category | Storage |
|---|---|
| Non-sensitive values | GitHub Environment `development` variables or AWS SSM String parameters |
| Passwords/tokens | AWS Secrets Manager or SSM SecureString |
| RDS password rotation | Rotate through AWS, then update the app runtime secret |

The `dev` profile expects every endpoint and credential to be supplied. It does
not contain localhost fallbacks.

## Production Runtime

For production on Hetzner or another VM target, keep runtime secrets on the host
or in the platform secret store. GitHub Actions should pass deployment metadata
and only the secrets required to authenticate to the host. The application
container receives the same variable names from this contract.

Do not commit `.env.local`, copied `.pem` files, database passwords, broker
passwords, AWS keys, SSH keys, or Sonar tokens.
