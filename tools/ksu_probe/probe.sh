#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ADB_BIN=${ADB:-adb}
ADB_SERIAL_VALUE=${ADB_SERIAL:-}
PACKAGE=dev.moseori.ksuprobe
COMPONENT="$PACKAGE/.CommandActivity"
APK="$SCRIPT_DIR/dist/KernelSUProbe.apk"
WAIT_TIMEOUT=${KSU_PROBE_TIMEOUT:-20}
MIN_HELPER_VERSION=10
INSTALL=0
PRIME=0

usage() {
  cat >&2 <<EOF
Usage: $0 [--serial SERIAL] [--wait-timeout SEC] [--install|--prime] [--] [COMMAND ...]

The timeout bounds result polling only. If it expires, the root command may
still be running; the script reports the correlated device output path.
EOF
}

die() {
  printf '%s\n' "$*" >&2
  exit 2
}

adb_run() {
  if [ -n "$ADB_SERIAL_VALUE" ]; then
    "$ADB_BIN" -s "$ADB_SERIAL_VALUE" "$@"
  else
    "$ADB_BIN" "$@"
  fi
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --install)
      INSTALL=1
      PRIME=1
      shift
      ;;
    --prime)
      PRIME=1
      shift
      ;;
    --serial|-s)
      [ "$#" -ge 2 ] || die "$1 requires a serial"
      ADB_SERIAL_VALUE=$2
      shift 2
      ;;
    --wait-timeout|--timeout)
      [ "$#" -ge 2 ] || die "$1 requires seconds"
      WAIT_TIMEOUT=$2
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    --)
      shift
      break
      ;;
    *)
      break
      ;;
  esac
done

case "$WAIT_TIMEOUT" in
  ''|*[!0-9]*) die "wait timeout must be a positive integer" ;;
esac
[ "$WAIT_TIMEOUT" -gt 0 ] || die "wait timeout must be greater than zero"
SERIAL_CR=$(printf '\r')
case "$ADB_SERIAL_VALUE" in
  *'
'*|*"$SERIAL_CR"*) die "ADB serial must not contain CR or LF" ;;
esac

if [ "$INSTALL" -eq 1 ]; then
  "$SCRIPT_DIR/build.sh"
  adb_run install -r "$APK"
fi

if ! adb_run shell pm path "$PACKAGE" >/dev/null 2>&1; then
  printf 'Probe is not installed. Run: %s --install\n' "$0" >&2
  exit 1
fi

INSTALLED_VERSION=$(adb_run shell dumpsys package "$PACKAGE" 2>/dev/null |
  sed -n 's/.*versionCode=\([0-9][0-9]*\).*/\1/p' | head -n 1 | tr -d '\r')
case "$INSTALLED_VERSION" in
  ''|*[!0-9]*)
    printf 'Cannot determine probe version. Reinstall with: %s --install\n' "$0" >&2
    exit 1
    ;;
esac
if [ "$INSTALLED_VERSION" -lt "$MIN_HELPER_VERSION" ]; then
  printf 'Probe helper %s is too old; reinstall with: %s --install\n' \
    "$INSTALLED_VERSION" "$0" >&2
  exit 1
fi

if [ "$PRIME" -eq 1 ]; then
  adb_run shell am force-stop "$PACKAGE"
  adb_run shell am start -W -n "$PACKAGE/.MainActivity" >/dev/null
  sleep 1
fi

if [ "$#" -gt 0 ]; then
  USER_COMMAND=$*
else
  USER_COMMAND='id; getenforce; cat /proc/1/attr/current'
fi

RUN_TOKEN=$(date -u '+%Y%m%dT%H%M%SZ')_$$
RUN_OUTPUT="/sdcard/Download/.ksu_probe.$RUN_TOKEN.txt"
RUN_MARKER="__KSU_PROBE_RC_${RUN_TOKEN}__="
LOCAL_OUTPUT=$(mktemp "${TMPDIR:-/tmp}/ksu-probe.XXXXXX")

cleanup() {
  rm -f "$LOCAL_OUTPUT"
}
trap cleanup EXIT
trap 'printf "Interrupted; the root command may still be running. Device output: %s\n" "$RUN_OUTPUT" >&2; exit 130' INT
trap 'printf "Terminated; the root command may still be running. Device output: %s\n" "$RUN_OUTPUT" >&2; exit 143' TERM HUP

USER_COMMAND_B64=$(printf '%s' "$USER_COMMAND" | base64 | tr -d '\n')
WRAPPED_COMMAND="umask 077
exec >'$RUN_OUTPUT' 2>&1
printf '%s' '$USER_COMMAND_B64' | base64 -d | sh
rc=\$?
printf '\\n%s%s\\n' '$RUN_MARKER' \"\$rc\"
exit \"\$rc\""
COMMAND_B64=$(printf '%s' "$WRAPPED_COMMAND" | base64 | tr -d '\n')

adb_run shell unlink "$RUN_OUTPUT" >/dev/null 2>&1 || true
adb_run shell am start -W -n "$COMPONENT" --es cmd_b64 "$COMMAND_B64" >/dev/null

elapsed=0
while [ "$elapsed" -lt "$WAIT_TIMEOUT" ]; do
  if adb_run shell cat "$RUN_OUTPUT" >"$LOCAL_OUTPUT" 2>/dev/null; then
    REMOTE_RC=$(sed -n "s/^$RUN_MARKER//p" "$LOCAL_OUTPUT" | tail -n 1 | tr -d '\r')
    if [ -n "$REMOTE_RC" ]; then
      case "$REMOTE_RC" in
        *[!0-9]*)
          printf 'Malformed completion marker in %s\n' "$RUN_OUTPUT" >&2
          exit 3
          ;;
      esac
      [ "$REMOTE_RC" -le 255 ] || {
        printf 'Out-of-range remote status: %s\n' "$REMOTE_RC" >&2
        exit 3
      }
      sed "\|^$RUN_MARKER[0-9][0-9]*\r\{0,1\}$|d" "$LOCAL_OUTPUT"
      printf '__KSU_PROBE_RC__=%s\n' "$REMOTE_RC"
      adb_run shell rm -f "$RUN_OUTPUT" >/dev/null 2>&1 || true
      exit "$REMOTE_RC"
    fi
  fi
  elapsed=$((elapsed + 1))
  sleep 1
done

printf 'No correlated result after %s seconds. The root command may still be running. Device output: %s\n' \
  "$WAIT_TIMEOUT" "$RUN_OUTPUT" >&2
printf 'Probe errors and su exit codes: adb logcat -d -s KSUProbe:V (filter by the probe PID/time for this run).\n' >&2
exit 124
