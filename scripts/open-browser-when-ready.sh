#!/usr/bin/env bash
# Mở trình duyệt khi backend đã sẵn sàng.
#
# Dùng làm "Before launch -> Run External Tool" trong IntelliJ: run configuration
# kiểu Spring Boot không có tuỳ chọn "Open browser", và before-launch chạy TRƯỚC
# khi app khởi động nên không thể gọi `open` trực tiếp. Script trả về ngay, phần
# chờ readiness chạy nền để IntelliJ không bị chặn.
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/local-common.sh

ensure_curl
load_env_file

SCHEME="${NITROGEN_WEB_SCHEME:-http}"
HOST="${NITROGEN_WEB_HOST:-localhost}"
PORT="${NITROGEN_WEB_PORT:-8080}"
CONTEXT_PATH="${NITROGEN_WEB_CONTEXT_PATH:-}"
OPEN_PATH="${1:-${NITROGEN_WEB_OPEN_PATH:-/swagger-ui.html}}"

# Thời gian chờ instance cũ tắt hẳn trước khi coi readiness là của instance mới.
SHUTDOWN_TIMEOUT_SECONDS="${NITROGEN_WEB_OPEN_SHUTDOWN_TIMEOUT:-20}"
STARTUP_TIMEOUT_SECONDS="${NITROGEN_WEB_OPEN_TIMEOUT:-120}"

BASE_URL="${SCHEME}://${HOST}:${PORT}${CONTEXT_PATH%/}"
READINESS_URL="${BASE_URL}/actuator/health/readiness"
TARGET_URL="${BASE_URL}${OPEN_PATH}"
LOG_FILE="${ROOT_DIR}/target/open-browser.log"

resolve_opener() {
  if command -v open >/dev/null 2>&1; then
    echo open
  elif command -v xdg-open >/dev/null 2>&1; then
    echo xdg-open
  fi
}

OPENER="$(resolve_opener)"
if [[ -z "${OPENER}" ]]; then
  echo "No browser opener (open/xdg-open) found; skipping." >&2
  exit 0
fi

is_ready() {
  curl -fs --max-time 2 "${READINESS_URL}" 2>/dev/null | grep -q '"status":"UP"'
}

log() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $*"
}

wait_and_open() {
  # Nếu instance cũ còn sống, readiness đã UP sẵn — mở ngay sẽ trỏ vào app cũ.
  # Chờ nó tắt trước, nhưng không chờ vô hạn phòng khi cổng bị tiến trình khác giữ.
  local deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
  while (( SECONDS < deadline )) && is_ready; do
    sleep 1
  done

  deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
  until is_ready; do
    if (( SECONDS >= deadline )); then
      log "Timed out after ${STARTUP_TIMEOUT_SECONDS}s waiting for ${READINESS_URL}"
      return 1
    fi
    sleep 1
  done

  log "Opening ${TARGET_URL}"
  "${OPENER}" "${TARGET_URL}"
}

mkdir -p "$(dirname "${LOG_FILE}")"
wait_and_open >>"${LOG_FILE}" 2>&1 &
disown
