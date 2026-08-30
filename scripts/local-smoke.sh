#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/local-common.sh

ensure_docker
ensure_curl
load_env_file

BASE_URL="http://localhost:${NITROGEN_WEB_PORT:-8080}"
POSTGRES_USER="${NITROGEN_DB_USER:-nitrogen}"
POSTGRES_DB="${NITROGEN_DB_NAME:-nitrogen}"
POSTGRES_PASSWORD="${NITROGEN_DB_PASSWORD:-nitrogen}"

postgres_ready() {
  compose exec -T postgres pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null
}

rabbit_ready() {
  compose exec -T rabbitmq rabbitmq-diagnostics -q ping >/dev/null
}

postgres_query() {
  compose exec -T \
    -e PGPASSWORD="${POSTGRES_PASSWORD}" \
    postgres \
    psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -tAc "$1"
}

http_status_up() {
  local body
  body="$(curl -fsS "$1")"
  grep -q '"status":"UP"' <<< "${body}"
}

http_ok() {
  curl -fsS "$1" >/dev/null
}

flyway_history_exists() {
  local result
  result="$(postgres_query "SELECT to_regclass('flyway_history.flyway_schema_history') IS NOT NULL;" | tr -d '[:space:]')"
  [[ "${result}" == "t" ]]
}

flyway_migrations_successful() {
  local failed_count
  failed_count="$(postgres_query "SELECT COUNT(*) FROM flyway_history.flyway_schema_history WHERE success IS NOT TRUE;" | tr -d '[:space:]')"
  [[ "${failed_count}" == "0" ]]
}

flyway_has_sql_migrations() {
  local migration_count
  migration_count="$(postgres_query "SELECT COUNT(*) FROM flyway_history.flyway_schema_history WHERE type = 'SQL' AND success IS TRUE;" | tr -d '[:space:]')"
  [[ "${migration_count}" -gt 0 ]]
}

retry_until "PostgreSQL connection" 120 postgres_ready
retry_until "RabbitMQ readiness" 120 rabbit_ready
retry_until "Backend liveness" 180 http_status_up "${BASE_URL}/actuator/health/liveness"
retry_until "Backend readiness" 180 http_status_up "${BASE_URL}/actuator/health/readiness"
retry_until "Flyway history table" 120 flyway_history_exists
retry_until "Flyway successful migrations" 120 flyway_migrations_successful
retry_until "Flyway SQL migrations" 120 flyway_has_sql_migrations
retry_until "Backend info endpoint" 120 http_ok "${BASE_URL}/actuator/info"
