#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/local-common.sh

ensure_docker
compose down --remove-orphans
