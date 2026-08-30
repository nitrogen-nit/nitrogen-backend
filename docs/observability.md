# Observability

Nitrogen exposes a small observability baseline for local, shared dev, and
production runtime. The application owns log, metric, trace, and health
contracts. The local observability stack lives in
`nitrogen-infrastructure/docs/observability.md`.

## Log Field Contract

Local profile keeps readable console logs. `dev` and `prod` profiles write
structured JSON logs using Spring Boot structured logging.

Every structured log must include:

| Field | Source |
|---|---|
| `timestamp` | Logging event time |
| `level` | Logging level |
| `logger` | Logger name |
| `message` | Log message |
| `service` | `spring.application.name` |
| `environment` | `NITROGEN_ENVIRONMENT` |
| `applicationVersion` | `NITROGEN_APPLICATION_VERSION` |
| `correlationId` | `X-Correlation-ID` or generated UUID |
| `traceId` | Micrometer/OpenTelemetry MDC value when tracing is active |
| `spanId` | Micrometer/OpenTelemetry MDC value when tracing is active |

Do not log passwords, Authorization headers, cookies, tokens, database URLs
with credentials, or request/response bodies that may contain sensitive data.

## Correlation ID

HTTP convention:

```text
Request header: X-Correlation-ID
Response header: X-Correlation-ID
MDC key: correlationId
```

The filter reuses a client correlation ID only when it is at most 128
characters and matches `[A-Za-z0-9][A-Za-z0-9._:-]*`. Missing or invalid values
are replaced with a generated UUID. The MDC value is removed in `finally`.

RabbitMQ propagation should use the existing `MessageEnvelope.correlationId`.
`CorrelationId.currentUuidOrNew()` is available for producers that need a UUID
correlation value from the current HTTP request context.

## Metrics

Prometheus is exposed at:

```text
/actuator/prometheus
```

Common tags are applied to all meters:

```text
application=nitrogen-backend
environment=<local|dev|prod>
version=<artifact version or git sha>
```

Do not add high-cardinality metric tags such as user IDs, correlation IDs,
dynamic URLs, raw exception messages, or request payload values.

Expected baseline metrics include JVM, HTTP server, HikariCP, process/system,
application startup, and RabbitMQ metrics when Spring instrumentation exposes
them for the active runtime.

Metrics are exported by Prometheus scraping. OTLP metric push is disabled in
this baseline so tracing can be enabled without requiring an OTLP metrics
receiver.

## Tracing

Tracing uses Micrometer Tracing with OpenTelemetry/OTLP. Runtime variables:

| Variable | Local default | Dev default | Prod default |
|---|---|---|---|
| `NITROGEN_TRACING_ENABLED` | `false` | `true` | `true` |
| `NITROGEN_TRACING_SAMPLING_PROBABILITY` | `0.0` | `1.0` | `0.1` |
| `NITROGEN_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | required env | required env |

The collector URL is never hard-coded in application config. A missing local
collector must not stop the application unless a future explicit fail-fast
setting is added.

## Health Groups

Endpoints:

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
```

Liveness includes only `livenessState`. Readiness includes `readinessState` and
PostgreSQL. RabbitMQ does not block web readiness by default because web
requests can continue and durable publication is protected by the outbox. Worker
runtime can enable Rabbit health with `NITROGEN_RABBIT_HEALTH_ENABLED=true`.

Production sets health details to `never` for anonymous users.

## Local Commands

```bash
cp .env.local.example .env.local
./scripts/local-up.sh
./scripts/local-smoke.sh
./scripts/local-down.sh
```

To test local Docker tracing with the infrastructure collector:

```bash
NITROGEN_TRACING_ENABLED=true \
NITROGEN_TRACING_SAMPLING_PROBABILITY=1.0 \
NITROGEN_DOCKER_OTLP_ENDPOINT=http://host.docker.internal:4318/v1/traces \
./scripts/local-up.sh
```

For IntelliJ runs, use `SPRING_PROFILES_ACTIVE=web,local` and
`NITROGEN_OTLP_ENDPOINT=http://localhost:4318/v1/traces` when tracing is enabled.

## Troubleshooting

If `/actuator/prometheus` is missing, confirm `micrometer-registry-prometheus`
is on the classpath and that `management.endpoints.web.exposure.include`
contains `prometheus`.

If readiness is `DOWN`, check PostgreSQL connectivity first. RabbitMQ should not
affect web readiness unless `NITROGEN_RABBIT_HEALTH_ENABLED=true`.

If traces are absent, verify `NITROGEN_TRACING_ENABLED=true`, sampling is above
`0.0`, and the OTLP endpoint points to the collector visible from the process
that runs the app.
