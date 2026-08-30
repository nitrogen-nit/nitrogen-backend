#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/local-common.sh

create_env_if_missing
load_env_file

exec ./mvnw spring-boot:run -Dspring-boot.run.profiles="${SPRING_PROFILES_ACTIVE:-web,local}"
