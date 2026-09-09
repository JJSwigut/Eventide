#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# shellcheck source=tools/eventide_env.sh
source "${SCRIPT_DIR}/eventide_env.sh"

cd "${REPO_ROOT}"

python3 tools/verify_database_migrations.py

./gradlew \
  :app:compileDebugKotlin \
  :app:spotlessCheck \
  :app:testDebugUnitTest \
  :app:assembleDebug
