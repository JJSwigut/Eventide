#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# shellcheck source=tools/eventide_env.sh
source "${SCRIPT_DIR}/eventide_env.sh"

cd "${REPO_ROOT}"

"${SCRIPT_DIR}/eventide_verify.sh"

EVENTIDE_FORCE_EMULATOR="${EVENTIDE_FORCE_EMULATOR:-1}" \
  EVENTIDE_STOP_EMULATOR="${EVENTIDE_STOP_EMULATOR:-1}" \
  "${SCRIPT_DIR}/eventide_smoke.sh"

./gradlew \
  :app:spotlessCheck \
  :app:testReleaseUnitTest \
  :app:lintVitalRelease
