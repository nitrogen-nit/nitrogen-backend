# Observability

Nitrogen exposes a small observability baseline for local, shared dev, and
production runtime. The application owns log, metric, trace, and health
contracts. The local observability stack lives in
`nitrogen-infrastructure/docs/observability.md`.

## Log Field Contract

Local profile keeps readable console logs with the correlation ID in the level
column (`INFO [cid:<id>]`). `dev` and `prod` profiles write structured JSON logs
(`logstash` format) using Spring Boot structured logging; extra members come
from `NitrogenStructuredLoggingJsonMembersCustomizer`.

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

`CorrelationIdFilter` runs first in the filter chain and logs one line per
request: `HTTP request completed method=... path=... status=... durationMs=...`.

Audit logs and security events store the same correlation ID, so a request can
be traced from logs to `administration.audit_logs` and
`administration.security_events`.

RabbitMQ propagation should use the existing `MessageEnvelope.correlationId`.
`CorrelationId.currentUuidOrNew()` is available for producers that need a UUID
correlation value from the current HTTP request context.

## Metrics

Prometheus is exposed at:

```text
/actuator/prometheus
```

Exposed actuator endpoints:

| Profile | Endpoints |
|---|---|
| default, `local`, `dev` | `health`, `info`, `metrics`, `prometheus`, `modulith` |
| `prod` | `health`, `info`, `prometheus` |

Security permits anonymous `GET` on `/actuator/health/**`, `/actuator/info` and
`/actuator/prometheus`; other actuator endpoints require authentication.

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
| `NITROGEN_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | set to the collector | set to the collector |

`application.yml` falls back to `http://localhost:4318/v1/traces` in every
profile, so dev and prod must set `NITROGEN_OTLP_ENDPOINT` explicitly when
tracing is on. A missing collector does not stop the application.

The OpenTelemetry resource carries `service.name`, `service.version` and
`deployment.environment.name`. OTLP metric export is disabled.

## Health Groups

Endpoints:

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
```

Liveness includes only `livenessState`. Readiness includes `readinessState` and
PostgreSQL (`db`). RabbitMQ health is off by default for `web` because web
requests can continue and durable publication is protected by the outbox.
`local` turns it on (`NITROGEN_RABBIT_HEALTH_ENABLED` defaults to `true` there),
and the `worker` profile always turns it on.

Health details are `when-authorized` by default and in `dev`; `prod` sets them to
`never`.

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
The application logs the local backend URL after the embedded web server starts.

## CI

The `backend-observability-test` job runs `CorrelationIdFilterTest`,
`MessageEnvelopeTest`, `ObservabilityEndpointIntegrationTest` and
`NitrogenApplicationTests`.

## Troubleshooting

If `/actuator/prometheus` is missing, confirm `micrometer-registry-prometheus`
is on the classpath and that `management.endpoints.web.exposure.include`
contains `prometheus`.

If readiness is `DOWN`, check PostgreSQL connectivity first. RabbitMQ is not
part of the readiness group, but with Rabbit health enabled (`local`, `worker`,
or `NITROGEN_RABBIT_HEALTH_ENABLED=true`) a broker outage turns overall
`/actuator/health` `DOWN`.

If traces are absent, verify `NITROGEN_TRACING_ENABLED=true`, sampling is above
`0.0`, and the OTLP endpoint points to the collector visible from the process
that runs the app.
