#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_DIR="$SCRIPT_DIR/build"
DIST_DIR="$SCRIPT_DIR/dist"
KEYSTORE="$BUILD_DIR/debug.keystore"
UNSIGNED_APK="$DIST_DIR/KernelSUProbe-unsigned.apk"
SIGNED_APK="$DIST_DIR/KernelSUProbe.apk"

mkdir -p "$BUILD_DIR" "$DIST_DIR"

apktool b "$SCRIPT_DIR" -o "$UNSIGNED_APK"

if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -dname 'CN=KernelSU Probe,O=Local,C=KR' \
    -keyalg RSA \
    -keysize 2048 \
    -validity 3650 \
    -noprompt
fi

if command -v "${APKSIGNER:-apksigner}" >/dev/null 2>&1; then
  "${APKSIGNER:-apksigner}" sign --ks "$KEYSTORE" --ks-pass pass:android \
    --key-pass pass:android --ks-key-alias androiddebugkey \
    --out "$SIGNED_APK" "$UNSIGNED_APK"
elif [ -n "${APKSIG_JAR:-}" ]; then
  java -cp "$APKSIG_JAR" "$SCRIPT_DIR/SignApk.java" \
    "$KEYSTORE" "$UNSIGNED_APK" "$SIGNED_APK"
else
  printf 'Set APKSIGNER to SDK apksigner or APKSIG_JAR to an existing apksig jar.\n' >&2
  exit 1
fi

printf '%s\n' "$SIGNED_APK"
