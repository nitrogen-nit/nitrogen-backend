#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-${ROOT_DIR}/.env.local}"
ENV_EXAMPLE_FILE="${ROOT_DIR}/.env.local.example"
COMPOSE_FILE="${ROOT_DIR}/compose.local.yml"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-nitrogen-local}"
DOCKER_BIN="${DOCKER_BIN:-docker}"
DOCKER_DESKTOP_BIN="/Applications/Docker.app/Contents/Resources/bin"

if [[ "${ENV_FILE}" != /* ]]; then
  ENV_FILE="${ROOT_DIR}/${ENV_FILE}"
fi

if [[ -d "${DOCKER_DESKTOP_BIN}" ]]; then
  PATH="${DOCKER_DESKTOP_BIN}:${PATH}"
  export PATH
fi

if ! command -v "${DOCKER_BIN}" >/dev/null 2>&1 && [[ -x /usr/local/bin/docker ]]; then
  DOCKER_BIN="/usr/local/bin/docker"
fi

if ! command -v "${DOCKER_BIN}" >/dev/null 2>&1 && [[ -x /Applications/Docker.app/Contents/Resources/bin/docker ]]; then
  DOCKER_BIN="/Applications/Docker.app/Contents/Resources/bin/docker"
fi

compose() {
  local env_file="${ENV_FILE}"
  if [[ ! -f "${env_file}" ]]; then
    env_file="${ENV_EXAMPLE_FILE}"
  fi

  "${DOCKER_BIN}" compose \
    --env-file "${env_file}" \
    -f "${COMPOSE_FILE}" \
    --project-name "${COMPOSE_PROJECT_NAME}" \
    "$@"
}

ensure_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    if [[ ! -x "${DOCKER_BIN}" ]]; then
      echo "Docker CLI is required." >&2
      exit 1
    fi
  fi

  "${DOCKER_BIN}" compose version >/dev/null
  if ! "${DOCKER_BIN}" info >/dev/null 2>&1; then
    if [[ -z "${DOCKER_CONTEXT:-}" && -z "${DOCKER_HOST:-}" ]] \
        && "${DOCKER_BIN}" context inspect desktop-linux >/dev/null 2>&1 \
        && DOCKER_CONTEXT=desktop-linux "${DOCKER_BIN}" info >/dev/null 2>&1; then
      export DOCKER_CONTEXT=desktop-linux
      return
    fi

    "${DOCKER_BIN}" info >/dev/null
  fi
}

ensure_curl() {
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required for local smoke checks." >&2
    exit 1
  fi
}

create_env_if_missing() {
  if [[ ! -f "${ENV_FILE}" ]]; then
    cp "${ENV_EXAMPLE_FILE}" "${ENV_FILE}"
    echo "Created .env.local from .env.local.example"
  fi
}

load_env_file() {
  local env_file="${ENV_FILE}"
  if [[ ! -f "${env_file}" ]]; then
    env_file="${ENV_EXAMPLE_FILE}"
  fi

  while IFS= read -r line || [[ -n "${line}" ]]; do
    [[ -z "${line}" || "${line}" == \#* ]] && continue

    local key="${line%%=*}"
    local value="${line#*=}"
    if [[ "${key}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ && -z "${!key+x}" ]]; then
      export "${key}=${value}"
    fi
  done < "${env_file}"
}

wait_for_container_health() {
  local service="$1"
  local timeout_seconds="$2"
  local deadline=$((SECONDS + timeout_seconds))

  while (( SECONDS < deadline )); do
    local container_id
    container_id="$(compose ps -q "${service}")"

    if [[ -n "${container_id}" ]]; then
      local status
      status="$("${DOCKER_BIN}" inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"

      case "${status}" in
        healthy|running)
          echo "${service} is ${status}"
          return 0
          ;;
        unhealthy|exited|dead)
          echo "${service} is ${status}" >&2
          compose logs --no-color --tail=120 "${service}" >&2
          return 1
          ;;
      esac
    fi

    sleep 2
  done

  echo "Timed out waiting for ${service} health." >&2
  compose logs --no-color --tail=120 "${service}" >&2
  return 1
}

retry_until() {
  local label="$1"
  local timeout_seconds="$2"
  shift 2

  local deadline=$((SECONDS + timeout_seconds))
  until "$@"; do
    if (( SECONDS >= deadline )); then
      echo "${label} failed after ${timeout_seconds}s." >&2
      return 1
    fi
    sleep 2
  done

  echo "${label} OK"
}
