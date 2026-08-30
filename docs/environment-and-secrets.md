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

## Profile Behavior

| Profile | Flyway | Hibernate DDL | Credential source | Intended use |
|---|---:|---|---|---|
| `local` | Enabled | `validate` | `.env.local` with safe local defaults | Developer laptop |
| `dev` | Enabled | `validate` | GitHub Environment, AWS SSM Parameter Store, or runtime host env | Shared development |
| `prod` | Disabled | `validate` | GitHub Environment plus production host secret store | Production runtime |

Production keeps Flyway disabled in the application process. The deployment
pipeline must run migration as a separate step before rolling out the app.

## Required Variables

| Name | Local | Dev | Prod | Required | Type | Example | Stored in | Used by |
|---|---|---|---|---:|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `web,local` | `web,dev` or `worker,dev` | `web,prod` or `worker,prod` | Yes | Variable | `web,local` | Local `.env.local`; GitHub Environment variable; runtime service env | Spring profile activation |
| `NITROGEN_ENVIRONMENT` | `local` | `dev` | `prod` | Yes | Variable | `dev` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/environment`; Hetzner runtime env | Banner, diagnostics, logging context |
| `NITROGEN_DB_URL` | localhost JDBC URL | RDS JDBC URL | Production JDBC URL | Yes | Variable | `jdbc:postgresql://db.example:5432/nitrogen?sslmode=require` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/db/url`; Hetzner runtime env | Spring datasource |
| `NITROGEN_DB_USER` | `nitrogen` | app DB user | app DB user | Yes | Variable | `nitrogen_app` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/db/user`; Hetzner runtime env | Spring datasource |
| `NITROGEN_DB_PASSWORD` | local-only password | RDS app password | production DB password | Yes | Secret | `local-db-password` | Local `.env.local`; GitHub Environment secret only when needed by deploy; AWS Secrets Manager or SSM SecureString `/nitrogen/dev/db/password`; Hetzner host secret file/env | Spring datasource |
| `NITROGEN_DB_POOL_SIZE` | `5` | small shared value | production capacity value | Yes | Variable | `10` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/db/pool-size`; Hetzner runtime env | HikariCP |
| `NITROGEN_RABBIT_HOST` | `localhost` | development broker host | production broker host | Yes | Variable | `rabbitmq.internal` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/rabbit/host`; Hetzner runtime env | Spring AMQP |
| `NITROGEN_RABBIT_PORT` | `5672` | broker port | broker port | Yes | Variable | `5672` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/rabbit/port`; Hetzner runtime env | Spring AMQP |
| `NITROGEN_RABBIT_USER` | `nitrogen` | app broker user | app broker user | Yes | Variable | `nitrogen_app` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/rabbit/user`; Hetzner runtime env | Spring AMQP |
| `NITROGEN_RABBIT_PASSWORD` | local-only password | broker password | broker password | Yes | Secret | `local-rabbit-password` | Local `.env.local`; GitHub Environment secret only when needed by deploy; AWS Secrets Manager or SSM SecureString `/nitrogen/dev/rabbit/password`; Hetzner host secret file/env | Spring AMQP |
| `NITROGEN_RABBIT_HEALTH_ENABLED` | `true` | `false` for web, `true` for worker | `false` for web, `true` for worker | Yes | Variable | `false` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/rabbit/health-enabled`; Hetzner runtime env | Actuator health |
| `NITROGEN_LOG_LEVEL` | `INFO` | `INFO` or `DEBUG` temporarily | `INFO` | Yes | Variable | `INFO` | Local `.env.local`; GitHub Environment variable; AWS SSM `/nitrogen/dev/log-level`; Hetzner runtime env | Logging |

## Optional Local Variables

| Name | Required | Type | Example | Stored in | Used by |
|---|---:|---|---|---|---|
| `NITROGEN_DB_NAME` | Local only | Variable | `nitrogen` | Local `.env.local` | Docker Compose PostgreSQL bootstrap |
| `NITROGEN_WEB_PORT` | Local only | Variable | `8080` | Local `.env.local` | Docker Compose backend port |
| `NITROGEN_RABBIT_MANAGEMENT_PORT` | Local only | Variable | `15672` | Local `.env.local` | RabbitMQ Management UI |
| `NITROGEN_HIBERNATE_SQL_LOG_LEVEL` | No | Variable | `WARN` | Local `.env.local` | Local SQL logging |

## Local Workflow

```bash
cp .env.local.example .env.local
./scripts/local-up.sh
./scripts/local-smoke.sh
./scripts/local-down.sh
```

`./scripts/local-up.sh` starts PostgreSQL 16, RabbitMQ Management, and the
backend web process through Docker Compose. PostgreSQL and RabbitMQ ports are
bound to `127.0.0.1` only. RabbitMQ Management is available at
`http://localhost:15672` by default.

`./scripts/local-reset.sh --yes` removes local PostgreSQL and RabbitMQ volumes.

## IntelliJ Run Configuration

Set the backend module root as the working directory:

```text
/Users/macbook/Documents/Nitrogen/nitrogen-backend
```

Use Active profiles:

```text
web,local
```

If IntelliJ does not load `.env.local`, use the EnvFile plugin or set the same
variables in the Run/Debug Configuration environment field.

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
