#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/local-common.sh

if [[ "${1:-}" != "--yes" ]]; then
  echo "This deletes local PostgreSQL and RabbitMQ volumes." >&2
  echo "Run ./scripts/local-reset.sh --yes to confirm." >&2
  exit 2
fi

ensure_docker
compose down --volumes --remove-orphans
