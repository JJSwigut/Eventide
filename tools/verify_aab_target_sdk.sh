#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 2 ]]; then
  echo "Usage: $0 <aab-path> <minimum-target-sdk>" >&2
  exit 2
fi

AAB_PATH="$1"
MINIMUM_TARGET_SDK="$2"
BUNDLETOOL_VERSION="1.17.1"
BUNDLETOOL_SHA256="45881ead13388872d82c4255b195488b7fc33f2cac5a9a977b0afc5e92367592"
BUNDLETOOL_DIR="${TMPDIR:-/tmp}/eventide-bundletool"
BUNDLETOOL_JAR="${BUNDLETOOL_DIR}/bundletool-all-${BUNDLETOOL_VERSION}.jar"
BUNDLETOOL_URL="https://github.com/google/bundletool/releases/download/${BUNDLETOOL_VERSION}/bundletool-all-${BUNDLETOOL_VERSION}.jar"

if [[ ! -f "${AAB_PATH}" ]]; then
  echo "AAB not found: ${AAB_PATH}" >&2
  exit 1
fi

mkdir -p "${BUNDLETOOL_DIR}"
if [[ ! -f "${BUNDLETOOL_JAR}" ]]; then
  curl --fail --location --silent --show-error "${BUNDLETOOL_URL}" --output "${BUNDLETOOL_JAR}"
fi

ACTUAL_SHA256="$(shasum -a 256 "${BUNDLETOOL_JAR}" | awk '{print $1}')"
if [[ "${ACTUAL_SHA256}" != "${BUNDLETOOL_SHA256}" ]]; then
  echo "bundletool checksum mismatch: ${ACTUAL_SHA256}" >&2
  exit 1
fi

MANIFEST="$(java -jar "${BUNDLETOOL_JAR}" dump manifest --bundle="${AAB_PATH}" --module=base)"
TARGET_SDK="$(printf '%s\n' "${MANIFEST}" | sed -n 's/.*android:targetSdkVersion="\([0-9][0-9]*\)".*/\1/p' | head -n 1)"

if [[ -z "${TARGET_SDK}" ]]; then
  echo "Could not read targetSdkVersion from ${AAB_PATH}." >&2
  exit 1
fi

if (( TARGET_SDK < MINIMUM_TARGET_SDK )); then
  echo "AAB targetSdkVersion ${TARGET_SDK} is below required ${MINIMUM_TARGET_SDK}." >&2
  exit 1
fi

echo "AAB target SDK verified: ${TARGET_SDK} (minimum ${MINIMUM_TARGET_SDK})."
