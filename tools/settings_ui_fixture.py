#!/usr/bin/env python3
"""Open the debug product settings with synthetic state; never connects Bluetooth."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import re


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit adb device/emulator serial")
    parser.add_argument("--scenario", choices=["normal", "charging", "zero_unknown", "headset", "disconnected", "pending", "long_name"], default="normal")
    parser.add_argument("--page", choices=["Home", "Audio", "Controls", "Accessibility", "Battery", "Microphone", "Equalizer", "Routing", "Left", "Right", "Mute", "EndCall", "Head", "Version", "Name", "Extras"], default="Home")
    parser.add_argument("--model", help="AirPodsModel enum name, e.g. AIRPODS_PRO_2_USBC")
    parser.add_argument("--theme", choices=["light", "dark"], default="dark")
    parser.add_argument("--width-dp", type=int, help="Composition-only logical viewport, 320..840 dp")
    parser.add_argument("--font-scale", type=float, default=1.0, help="Composition-only font scale, 1..2")
    parser.add_argument("--run-tests", action="store_true", help="Update built debug/test APKs with -r, run UI tests, retain apps/data")
    parser.add_argument("--capture-matrix", action="store_true", help="Generate visual comparison evidence separately from behavior tests")
    parser.add_argument("--test-method", help="Run one DeviceSettingsScreenTest method while resolving a failure")
    parser.add_argument("--output", type=Path, help="Host directory for synthetic test captures")
    args = parser.parse_args()
    if args.capture_matrix and (not args.run_tests or args.test_method):
        parser.error("--capture-matrix requires --run-tests and cannot be combined with --test-method")
    if args.test_method and (not args.run_tests or not args.test_method.isascii() or not args.test_method.isidentifier()):
        parser.error("--test-method requires --run-tests and an ASCII method identifier")
    if args.width_dp is not None and not 320 <= args.width_dp <= 840:
        parser.error("--width-dp must be 320..840")
    if not 1 <= args.font_scale <= 2:
        parser.error("--font-scale must be 1..2")
    adb = os.environ.get("ADB") or shutil.which("adb")
    if not adb:
        sdk = Path(os.environ.get("ANDROID_HOME", str(Path.home() / "Library/Android/sdk")))
        adb = str(sdk / "platform-tools/adb")
    base = [adb, "-s", args.serial]
    if args.run_tests:
        root = Path(__file__).resolve().parents[1]
        apks = [root / "android/app/build/outputs/apk/foss/debug/app-foss-debug.apk",
                root / "android/app/build/outputs/apk/androidTest/foss/debug/app-foss-debug-androidTest.apk"]
        for apk in apks:
            if not apk.is_file():
                parser.error(f"Build :app:assembleFossDebug :app:assembleFossDebugAndroidTest first; missing {apk}")
        for apk in apks:
            subprocess.run(base + ["install", "-r", "-t", str(apk)], check=True)
        test_class = "me.kavishdevar.librepods.presentation.screens.apple." + ("DeviceSettingsCaptureTest" if args.capture_matrix else "DeviceSettingsScreenTest")
        if args.test_method:
            test_class += "#" + args.test_method
        result = subprocess.run(base + ["shell", "am", "instrument", "-w", "-r", "-e", "class", test_class,
            "me.kavishdevar.librepods.test/androidx.test.runner.AndroidJUnitRunner"],
            check=True, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=600)
        print(result.stdout)
        output = args.output or root / "android/app/build/outputs/settings-fixtures"
        output.mkdir(parents=True, exist_ok=True)
        subprocess.run(base + ["pull", "/sdcard/Android/data/me.kavishdevar.librepods/files/settings-fixtures/.", str(output)], check=True)
        if not re.search(r"OK \(\d+ tests?\)", result.stdout):
            raise SystemExit("UI tests failed; see instrumentation output above")
        print(f"Synthetic captures: {output}")
        if args.capture_matrix:
            print("Capture job complete. Screenshots require visual comparison; this is not a pixel-parity test.")
        return
    command = base + ["shell", "am", "start", "-W", "-n",
               "me.kavishdevar.librepods/.presentation.screens.apple.SettingsFixtureActivity",
               "--es", "scenario", args.scenario, "--es", "page", args.page,
               "--es", "theme", args.theme, "--ef", "font_scale", str(args.font_scale)]
    if args.width_dp is not None:
        command += ["--ei", "width_dp", str(args.width_dp)]
    if args.model:
        command += ["--es", "model", args.model]
    subprocess.run(command, check=True)


if __name__ == "__main__":
    main()
