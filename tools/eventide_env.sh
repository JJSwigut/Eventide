#!/usr/bin/env bash
set -euo pipefail

DEFAULT_ANDROID_HOME="${HOME}/Library/Android/sdk"

if [[ -z "${ANDROID_HOME:-}" ]]; then
  export ANDROID_HOME="${DEFAULT_ANDROID_HOME}"
fi

if [[ ! -d "${ANDROID_HOME}" ]]; then
  echo "ANDROID_HOME does not exist: ${ANDROID_HOME}" >&2
  exit 1
fi

export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${ANDROID_HOME}/cmdline-tools/latest/bin:${PATH}"
