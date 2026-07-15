#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
AVD_NAME="${EVENTIDE_AVD_NAME:-EventideSmoke}"
FORCE_EMULATOR="${EVENTIDE_FORCE_EMULATOR:-0}"
SCREENSHOT_TIMEOUT_SECONDS="${EVENTIDE_SCREENSHOT_TIMEOUT_SECONDS:-30}"
SCREENSHOT_SETTLE_SECONDS="${EVENTIDE_SCREENSHOT_SETTLE_SECONDS:-2}"
SCREENSHOT_VALIDATION_ATTEMPTS="${EVENTIDE_SCREENSHOT_VALIDATION_ATTEMPTS:-5}"
UI_DUMP_TIMEOUT_SECONDS="${EVENTIDE_UI_DUMP_TIMEOUT_SECONDS:-45}"
UI_DUMP_COMMAND_TIMEOUT_SECONDS="${EVENTIDE_UI_DUMP_COMMAND_TIMEOUT_SECONDS:-10}"
SMOKE_FIXTURE="${EVENTIDE_SMOKE_FIXTURE:-1}"
ANIMATION_SCALE="${EVENTIDE_ANIMATION_SCALE:-}"
PACKAGE_NAME="com.jjswigut.eventide"
MAIN_ACTIVITY="com.jjswigut.eventide/.MainActivity"
SMOKE_DIR="${REPO_ROOT}/build/smoke"
UI_DUMP_LOCAL="${SMOKE_DIR}/eventide-smoke-window.xml"
UI_DUMP_REMOTE="/sdcard/eventide-smoke-window.xml"
EMULATOR_STARTED=0

cleanup() {
  local status=$?
  if [[ "${EVENTIDE_STOP_EMULATOR:-0}" == "1" && "${EMULATOR_STARTED}" == "1" && -n "${ANDROID_SERIAL:-}" ]]; then
    adb -s "${ANDROID_SERIAL}" emu kill >/dev/null 2>&1 || true
  fi
  exit "${status}"
}

trap cleanup EXIT

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

capture_screenshot() {
  local screenshot_output="${1:-${SCREENSHOT_LOCAL}}"
  local screenshot_pid=""
  adb -s "${ANDROID_SERIAL}" exec-out screencap -p > "${screenshot_output}" &
  screenshot_pid=$!

  for _ in $(seq 1 "${SCREENSHOT_TIMEOUT_SECONDS}"); do
    if ! kill -0 "${screenshot_pid}" 2>/dev/null; then
      wait "${screenshot_pid}"
      return $?
    fi
    sleep 1
  done

  kill "${screenshot_pid}" >/dev/null 2>&1 || true
  wait "${screenshot_pid}" >/dev/null 2>&1 || true
  echo "Timed out capturing screenshot from Android target: ${ANDROID_SERIAL}" >&2
  return 1
}

capture_valid_screenshot() {
  local attempt_path=""
  local assertion_output=""
  local last_assertion_output=""

  for attempt in $(seq 1 "${SCREENSHOT_VALIDATION_ATTEMPTS}"); do
    attempt_path="${SCREENSHOT_LOCAL%.png}-attempt-${attempt}.png"
    rm -f "${attempt_path}"
    capture_screenshot "${attempt_path}"

    if [[ ! -s "${attempt_path}" ]]; then
      last_assertion_output="Screenshot was not created or was empty: ${attempt_path}"
      echo "Screenshot validation attempt ${attempt}/${SCREENSHOT_VALIDATION_ATTEMPTS} failed: ${last_assertion_output}" >&2
      sleep "${SCREENSHOT_SETTLE_SECONDS}"
      continue
    fi

    if assertion_output="$(python3 "${SCRIPT_DIR}/eventide_assert_screenshot.py" "${attempt_path}" 2>&1)"; then
      mv "${attempt_path}" "${SCREENSHOT_LOCAL}"
      rm -f "${SCREENSHOT_LOCAL%.png}-attempt-"*.png
      echo "${assertion_output}"
      return 0
    fi

    last_assertion_output="${assertion_output}"
    echo "Screenshot validation attempt ${attempt}/${SCREENSHOT_VALIDATION_ATTEMPTS} failed: ${assertion_output}" >&2
    sleep "${SCREENSHOT_SETTLE_SECONDS}"
  done

  echo "Timed out waiting for a valid screenshot after ${SCREENSHOT_VALIDATION_ATTEMPTS} attempts." >&2
  echo "${last_assertion_output}" >&2
  return 1
}

run_with_timeout() {
  local timeout_seconds="$1"
  shift
  "$@" &
  local command_pid=$!

  for _ in $(seq 1 "${timeout_seconds}"); do
    if ! kill -0 "${command_pid}" 2>/dev/null; then
      wait "${command_pid}"
      return $?
    fi
    sleep 1
  done

  kill "${command_pid}" >/dev/null 2>&1 || true
  wait "${command_pid}" >/dev/null 2>&1 || true
  return 124
}

dump_ui() {
  run_with_timeout "${UI_DUMP_COMMAND_TIMEOUT_SECONDS}" \
    adb -s "${ANDROID_SERIAL}" shell uiautomator dump "${UI_DUMP_REMOTE}" >/dev/null
  run_with_timeout "${UI_DUMP_COMMAND_TIMEOUT_SECONDS}" \
    adb -s "${ANDROID_SERIAL}" exec-out cat "${UI_DUMP_REMOTE}" > "${UI_DUMP_LOCAL}"
}

set_animation_scale() {
  if [[ -z "${ANIMATION_SCALE}" ]]; then
    return 0
  fi

  local applied=0
  for _ in {1..30}; do
    if adb -s "${ANDROID_SERIAL}" shell settings put global window_animation_scale "${ANIMATION_SCALE}" \
      && adb -s "${ANDROID_SERIAL}" shell settings put global transition_animation_scale "${ANIMATION_SCALE}" \
      && adb -s "${ANDROID_SERIAL}" shell settings put global animator_duration_scale "${ANIMATION_SCALE}"; then
      applied=1
      break
    fi
    sleep 1
  done

  if [[ "${applied}" != "1" ]]; then
    echo "Unable to apply Android animation scale '${ANIMATION_SCALE}' on target ${ANDROID_SERIAL}." >&2
    return 1
  fi

  local window_scale transition_scale animator_scale
  window_scale="$(adb -s "${ANDROID_SERIAL}" shell settings get global window_animation_scale | tr -d '\r')"
  transition_scale="$(adb -s "${ANDROID_SERIAL}" shell settings get global transition_animation_scale | tr -d '\r')"
  animator_scale="$(adb -s "${ANDROID_SERIAL}" shell settings get global animator_duration_scale | tr -d '\r')"
  echo "Animation scales: window=${window_scale} transition=${transition_scale} animator=${animator_scale}"
}

wait_for_ui_text() {
  local expected="$1"
  local label="$2"
  for _ in $(seq 1 "${UI_DUMP_TIMEOUT_SECONDS}"); do
    if dump_ui && grep -Fq "${expected}" "${UI_DUMP_LOCAL}"; then
      echo "Asserted UI state: ${label}"
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for UI text '${expected}' (${label}). Last UI dump: ${UI_DUMP_LOCAL}" >&2
  sed -n '1,80p' "${UI_DUMP_LOCAL}" >&2 || true
  return 1
}

swipe_up() {
  local size width height
  size="$(adb -s "${ANDROID_SERIAL}" shell wm size 2>/dev/null | tr -d '\r' | awk -F': ' '/size:/ { value=$2 } END { print value }')"
  width="${size%x*}"
  height="${size#*x}"
  if [[ -z "${width}" || -z "${height}" || "${width}" == "${height}" ]]; then
    adb -s "${ANDROID_SERIAL}" shell input swipe 540 1536 540 768 400
  else
    adb -s "${ANDROID_SERIAL}" shell input swipe \
      "$((width / 2))" "$((height * 4 / 5))" \
      "$((width / 2))" "$((height * 2 / 5))" 400
  fi
}

wait_for_ui_text_after_scroll() {
  local expected="$1"
  local label="$2"
  for _ in {1..5}; do
    if dump_ui && grep -Fq "${expected}" "${UI_DUMP_LOCAL}"; then
      echo "Asserted UI state: ${label}"
      return 0
    fi
    swipe_up
    sleep 1
  done

  echo "Timed out waiting for UI text '${expected}' after scrolling (${label}). Last UI dump: ${UI_DUMP_LOCAL}" >&2
  sed -n '1,80p' "${UI_DUMP_LOCAL}" >&2 || true
  return 1
}

tap_screen_center() {
  local size width height
  size="$(adb -s "${ANDROID_SERIAL}" shell wm size 2>/dev/null | tr -d '\r' | awk -F': ' '/Physical size/ { print $2; exit }')"
  width="${size%x*}"
  height="${size#*x}"
  if [[ -z "${width}" || -z "${height}" || "${width}" == "${height}" ]]; then
    adb -s "${ANDROID_SERIAL}" shell input tap 540 960
  else
    adb -s "${ANDROID_SERIAL}" shell input tap "$((width / 2))" "$((height / 2))"
  fi
}

tap_ui_text_center() {
  local expected="$1"
  local coordinates=""

  dump_ui || {
    echo "Unable to dump UI before tapping '${expected}'." >&2
    return 1
  }

  coordinates="$(
    python3 - "${UI_DUMP_LOCAL}" "${expected}" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

tree = ET.parse(sys.argv[1])
expected = sys.argv[2]
for node in tree.iter("node"):
    if node.attrib.get("text") != expected and node.attrib.get("content-desc") != expected:
        continue
    bounds = node.attrib.get("bounds", "")
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not match:
        continue
    left, top, right, bottom = map(int, match.groups())
    print((left + right) // 2, (top + bottom) // 2)
    break
PY
  )"

  if [[ -z "${coordinates}" ]]; then
    echo "Unable to find UI text '${expected}' in ${UI_DUMP_LOCAL}." >&2
    return 1
  fi

  adb -s "${ANDROID_SERIAL}" shell input tap ${coordinates}
}

assert_app_running() {
  if ! adb -s "${ANDROID_SERIAL}" shell pidof "${PACKAGE_NAME}" >/dev/null 2>&1; then
    echo "Eventide process is not running after launch." >&2
    adb -s "${ANDROID_SERIAL}" logcat -d -t 200 >&2 || true
    return 1
  fi
}

if [[ "${FORCE_EMULATOR}" == "1" ]]; then
  ANDROID_SERIAL="$(connected_emulator || true)"
  export ANDROID_SERIAL
elif [[ -z "${ANDROID_SERIAL:-}" ]]; then
  ANDROID_SERIAL="$(connected_device || true)"
  export ANDROID_SERIAL
fi

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
adb -s "${ANDROID_SERIAL}" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb -s "${ANDROID_SERIAL}" shell wm dismiss-keyguard >/dev/null 2>&1 || true
set_animation_scale

./gradlew :app:assembleDebug

APK_PATH="${REPO_ROOT}/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "${APK_PATH}" ]]; then
  echo "Debug APK not found: ${APK_PATH}" >&2
  exit 1
fi

adb -s "${ANDROID_SERIAL}" install -r -d "${APK_PATH}"
adb -s "${ANDROID_SERIAL}" shell am force-stop "${PACKAGE_NAME}" >/dev/null 2>&1 || true
adb -s "${ANDROID_SERIAL}" shell pm clear "${PACKAGE_NAME}" >/dev/null 2>&1 || true
adb -s "${ANDROID_SERIAL}" shell pm grant "${PACKAGE_NAME}" android.permission.ACCESS_COARSE_LOCATION >/dev/null 2>&1 || true
adb -s "${ANDROID_SERIAL}" shell pm grant "${PACKAGE_NAME}" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
if [[ "${SMOKE_FIXTURE}" == "1" ]]; then
  adb -s "${ANDROID_SERIAL}" shell am start -W -n "${MAIN_ACTIVITY}" --ez eventide.SMOKE_FIXTURE true
  wait_for_ui_text "Open Smoke Station" "debug smoke fixture entry rendered"
  sleep 1
  tap_ui_text_center "Open Smoke Station"
  wait_for_ui_text "Marine conditions" "station detail marine panel rendered"
  wait_for_ui_text "Nearby buoy 44060" "deterministic NDBC buoy content rendered"
  wait_for_ui_text "12 mi" "deterministic NDBC buoy distance rendered"
  wait_for_ui_text "Tides" "station detail tide section rendered"
  wait_for_ui_text "1:14am" "deterministic tide content rendered"
  wait_for_ui_text "National Weather Service" "deterministic weather attribution rendered"
  wait_for_ui_text_after_scroll "Waves" "structured marine metric rows rendered"
else
  adb -s "${ANDROID_SERIAL}" shell am start -W -n "${MAIN_ACTIVITY}"
  wait_for_ui_text "Eventide" "normal app launched"
fi
assert_app_running

if [[ "${SCREENSHOT_SETTLE_SECONDS}" != "0" ]]; then
  sleep "${SCREENSHOT_SETTLE_SECONDS}"
fi

SCREENSHOT_LOCAL="${SMOKE_DIR}/eventide-smoke-$(date +%Y%m%d-%H%M%S).png"
capture_valid_screenshot

echo "Smoke target: ${ANDROID_SERIAL}"
if [[ "${SMOKE_FIXTURE}" == "1" ]]; then
  echo "Asserted screen: debug fixture station detail with tide, NDBC buoy, and NWS forecast content"
else
  echo "Asserted screen: normal Eventide launch text"
fi
echo "Screenshot: ${SCREENSHOT_LOCAL}"
