#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env.local ]]; then
  echo "Missing .env.local. Copy .env.example to .env.local and fill real values." >&2
  exit 1
fi

exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=web
