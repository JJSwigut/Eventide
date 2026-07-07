#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
AVD_NAME="${EVENTIDE_AVD_NAME:-EventideSmoke}"
FORCE_EMULATOR="${EVENTIDE_FORCE_EMULATOR:-0}"
PACKAGE_NAME="com.jjswigut.eventide"
MAIN_ACTIVITY="com.jjswigut.eventide/.MainActivity"
SMOKE_DIR="${REPO_ROOT}/build/smoke"

# shellcheck source=tools/eventide_env.sh
source "${SCRIPT_DIR}/eventide_env.sh"

cd "${REPO_ROOT}"
mkdir -p "${SMOKE_DIR}"

connected_device() {
  adb devices | awk 'NR > 1 && $2 == "device" { print $1; exit }'
}

connected_emulator() {
  adb devices | awk 'NR > 1 && $1 ~ /^emulator-/ && $2 == "device" { print $1; exit }'
}

wait_for_emulator_device() {
  local serial=""
  for _ in {1..90}; do
    serial="$(connected_emulator || true)"
    if [[ -n "${serial}" ]]; then
      echo "${serial}"
      return 0
    fi
    sleep 2
  done
  return 1
}

wait_for_boot() {
  local serial="$1"
  adb -s "${serial}" wait-for-device
  local booted=""
  for _ in {1..90}; do
    booted="$(adb -s "${serial}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "${booted}" == "1" ]]; then
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for Android target to boot: ${serial}" >&2
  return 1
}

if [[ "${FORCE_EMULATOR}" == "1" ]]; then
  ANDROID_SERIAL="$(connected_emulator || true)"
  export ANDROID_SERIAL
elif [[ -z "${ANDROID_SERIAL:-}" ]]; then
  ANDROID_SERIAL="$(connected_device || true)"
  export ANDROID_SERIAL
fi

EMULATOR_STARTED=0
if [[ -z "${ANDROID_SERIAL:-}" && ( "${FORCE_EMULATOR}" == "1" || -z "$(connected_device || true)" ) ]]; then
  if emulator -list-avds | grep -Fxq "${AVD_NAME}"; then
    nohup emulator -avd "${AVD_NAME}" -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect \
      > "${SMOKE_DIR}/emulator.log" 2>&1 &
    EMULATOR_STARTED=1
    if [[ "${FORCE_EMULATOR}" == "1" ]]; then
      ANDROID_SERIAL="$(wait_for_emulator_device || true)"
    else
      adb wait-for-device
      ANDROID_SERIAL="$(connected_device || true)"
    fi
    export ANDROID_SERIAL
  else
    echo "No connected Android device and AVD '${AVD_NAME}' was not found." >&2
    exit 1
  fi
fi

if [[ -z "${ANDROID_SERIAL:-}" ]]; then
  echo "No Android device became available." >&2
  exit 1
fi

wait_for_boot "${ANDROID_SERIAL}"

./gradlew :app:assembleDebug

APK_PATH="${REPO_ROOT}/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "${APK_PATH}" ]]; then
  echo "Debug APK not found: ${APK_PATH}" >&2
  exit 1
fi

adb -s "${ANDROID_SERIAL}" install -r -d "${APK_PATH}"
adb -s "${ANDROID_SERIAL}" shell pm grant "${PACKAGE_NAME}" android.permission.ACCESS_COARSE_LOCATION >/dev/null 2>&1 || true
adb -s "${ANDROID_SERIAL}" shell pm grant "${PACKAGE_NAME}" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
adb -s "${ANDROID_SERIAL}" shell am start -W -n "${MAIN_ACTIVITY}"
sleep 5

SCREENSHOT_REMOTE="/sdcard/eventide-smoke.png"
SCREENSHOT_LOCAL="${SMOKE_DIR}/eventide-smoke-$(date +%Y%m%d-%H%M%S).png"
adb -s "${ANDROID_SERIAL}" shell screencap -p "${SCREENSHOT_REMOTE}"
adb -s "${ANDROID_SERIAL}" pull "${SCREENSHOT_REMOTE}" "${SCREENSHOT_LOCAL}" >/dev/null
adb -s "${ANDROID_SERIAL}" shell rm "${SCREENSHOT_REMOTE}" >/dev/null 2>&1 || true

echo "Smoke target: ${ANDROID_SERIAL}"
echo "Screenshot: ${SCREENSHOT_LOCAL}"

if [[ "${EVENTIDE_STOP_EMULATOR:-0}" == "1" && "${EMULATOR_STARTED}" == "1" ]]; then
  adb -s "${ANDROID_SERIAL}" emu kill >/dev/null 2>&1 || true
fi
