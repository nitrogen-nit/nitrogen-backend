#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/local-common.sh

ensure_docker
create_env_if_missing
load_env_file

compose up -d --build postgres rabbitmq
wait_for_container_health postgres 120
wait_for_container_health rabbitmq 120

compose up -d --build backend-web
wait_for_container_health backend-web 240

scripts/local-smoke.sh

echo "Nitrogen local environment is ready at http://localhost:${NITROGEN_WEB_PORT:-8080}"
echo "RabbitMQ Management UI is ready at http://localhost:${NITROGEN_RABBIT_MANAGEMENT_PORT:-15672}"
