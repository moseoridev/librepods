#!/usr/bin/env python3
"""Compare a pinned independent settings fixture with an already captured S25 reference.

Only the test fixture is temporarily installed. Restore the original APK even on
failure. No LibrePods product APK, vendor APK, Bluetooth, display or root changes.
"""
import argparse
import hashlib
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

import numpy as np
from PIL import Image

PKG = "dev.librepods.uipreview"
TEMP_XML = "/data/local/tmp/librepods-current-fixture-inspect.xml"

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def adb(serial, *args, timeout=35):
    return subprocess.check_output(
        ["adb", "-s", serial, *[str(x) for x in args]], timeout=timeout,
    )

def installed_sha(serial):
    paths = adb(serial, "shell", "pm", "path", PKG).decode().strip().splitlines()
    if len(paths) != 1 or not paths[0].startswith("package:"):
        raise RuntimeError("Expected one installed preview APK")
    return adb(serial, "shell", "sha256sum", paths[0][8:]).decode().split()[0]

def card_rect(document, enabled):
    root = ET.fromstring(document)
    parents = {child: parent for parent in root.iter("node") for child in parent}
    labels = [n for n in root.iter("node") if n.get("text") == "좌우 소리 균형"]
    if len(labels) != 1:
        raise RuntimeError(f"Expected one balance label, got {len(labels)}")
    n = labels[0]
    while n is not None:
        sliders = [c for c in n.iter("node") if c.get("class") == "android.widget.SeekBar"]
        if len(sliders) == 1:
            if sliders[0].get("enabled") != str(enabled).lower():
                raise RuntimeError("Captured slider enabled state does not match the intended test")
            rect = [int(v) for v in re.findall(r"\d+", n.get("bounds"))]
            control = [int(v) for v in re.findall(r"\d+", sliders[0].get("bounds"))]
            # The full card contains the range with horizontal inset; a text
            # column or intermediate range container is not the card surface.
            if rect[0] < control[0] and rect[2] > control[2]:
                return rect
        n = parents.get(n)
    raise RuntimeError("Could not find balance card ancestor")

def crop(png, bounds, display_size=None):
    data = np.asarray(Image.open(png).convert("RGB"))
    x, y, x2, y2 = bounds
    if display_size is not None and data.shape[:2] != (display_size[1], display_size[0]):
        raise RuntimeError(f"Screenshot shape differs from native display: {data.shape} vs {display_size}")
    if not (0 <= x < x2 <= data.shape[1] and 0 <= y < y2 <= data.shape[0]):
        raise RuntimeError(f"Card rectangle is out of bounds: {bounds} / {data.shape}")
    return data[y:y2, x:x2]

def main(a):
    if a.output.exists():
        raise ValueError("Use a new evidence directory")
    reference = json.loads((a.reference_run / "accessibility-probe.json").read_text())
    if reference["result"] != "accessibility_visible":
        raise ValueError("Reference Accessibility page missing")
    a.output.mkdir(parents=True)
    enabled = not a.initial_disabled
    size = reference.get("display_size", [1080, 2340])
    result = {
        "scope": "Android17 independent original-to-fixture whole balance card RGB comparison, one configuration and value 0.",
        "display_size": size,
        "dark": a.dark,
        "expected_enabled": enabled,
        "reference": str(a.reference_run),
        "reference_manager_sha256": json.loads((a.reference_run / "manifest.json").read_text())["apk_sha256"],
        "fixture_apk_sha256": sha(a.fixture_apk),
        "previous_fixture_apk_sha256": sha(a.backup_apk),
        "comparisons": [],
    }
    installed = False
    error = None
    try:
        if installed_sha(a.serial) != sha(a.backup_apk):
            raise RuntimeError("Installed preview differs from the saved rollback APK")
        if sha(a.fixture_apk) != a.fixture_sha:
            raise RuntimeError("Pinned fixture hash is not correct")
        ref_bytes = (a.reference_run / "state-22.xml").read_bytes()
        ref_rect = card_rect(ref_bytes, enabled)
        ref_frames = [a.reference_run / f"state-{n:02d}.png" for n in (21, 22, 23, 24)]
        source = [crop(path, ref_rect, size) for path in ref_frames]
        if any(not np.array_equal(source[0], x) for x in source[1:]):
            raise RuntimeError("Reference card was not stable over four frames")
        result["reference_card_rect"] = ref_rect
        result["reference_card_sha256"] = hashlib.sha256(source[0].tobytes()).hexdigest()
        print("reference stable", ref_rect, flush=True)

        output = adb(a.serial, "install", "-r", a.fixture_apk, timeout=90).decode()
        if "Success" not in output:
            raise RuntimeError("Preview install did not report Success: " + output)
        installed = True
        if installed_sha(a.serial) != a.fixture_sha:
            raise RuntimeError("On-device fixture differs from pinned fixture")
        adb(a.serial, "shell", "am", "force-stop", PKG)
        launched = adb(a.serial, "shell", "am", "start", "-W", "-n", PKG + "/.MainActivity",
                       "--es", "scene", "native-accessibility",
                       "--ez", "sliderEnabled", str(enabled).lower(),
                       "--ef", "sliderValue", "0",
                       "--ez", "balanceWearingHint", "true",
                       *(["--ez", "sliderSequenceControls", "true"] if a.initial_disabled else []),
                       "--ez", "dark", str(a.dark).lower(), timeout=35)
        (a.output / "launch.txt").write_bytes(launched)
        time.sleep(1.3)
        adb(a.serial, "shell", "uiautomator", "dump", TEMP_XML, timeout=30)
        hierarchy = adb(a.serial, "shell", "cat", TEMP_XML)
        (a.output / "hierarchy.xml").write_bytes(hierarchy)
        rect = card_rect(hierarchy, enabled)
        result["fixture_card_rect"] = rect
        result["card_bounds_equal"] = ref_rect == rect
        if ref_rect != rect:
            raise RuntimeError(f"Independent card bounds differ: {ref_rect} versus {rect}")

        stable = []
        for n in range(18):
            target = a.output / f"frame-{n:02d}.png"
            target.write_bytes(adb(a.serial, "exec-out", "screencap", "-p", timeout=25))
            pixels = crop(target, rect, size)
            stable = [pixels] if not stable or not np.array_equal(stable[-1], pixels) else stable + [pixels]
            if len(stable) == 3:
                break
            time.sleep(.3)
        if len(stable) != 3:
            raise RuntimeError("Could not get three stable fixture card frames")
        sample = stable[-1]
        if a.surface_bridge and a.surface_observer:
            from verify_current_buds4_surface_copy import copy_surface
            pids = adb(a.serial, "shell", "pidof", PKG).decode().split()
            if len(pids) != 1:
                raise RuntimeError("Could not select one standalone preview PID")
            surface = copy_surface(a.serial, int(pids[0]), PKG,
                a.surface_bridge, a.surface_observer,
                a.output / "surface-readback", rect)
            if not np.array_equal(sample, crop(a.output / "surface-readback/before.png", rect, size)):
                raise RuntimeError("Surface observer baseline differs from the stable card")
            result["surface_readback"] = {
                "path": str(a.output / "surface-readback/result.json"),
                "accepted": surface["card_unchanged_after_observation"],
                "formats": [r["format"] for r in surface["formats"]],
            }
        delta = np.abs(source[0].astype(np.int16) - sample.astype(np.int16))
        mask = np.any(delta != 0, axis=2)
        points = np.argwhere(mask)
        result["comparisons"] = [{
            "source_frame": str(ref_frames[0]),
            "fixture_frame": str(target),
            "pixels": int(mask.size),
            "exact_unequal_pixels": int(mask.sum()),
            "sum_rgb_delta_above_3": int((delta.sum(axis=2) > 3).sum()),
            "max_channel_delta": int(delta.max()),
            "difference_local_bounds": None if len(points) == 0 else [
                int(points[:, 1].min()), int(points[:, 0].min()),
                int(points[:, 1].max() + 1), int(points[:, 0].max() + 1)],
        }]
        print(json.dumps(result["comparisons"], ensure_ascii=False), flush=True)
    except Exception as exc:
        error = repr(exc)
        result["error"] = error
        print("verification error", error, flush=True)
    finally:
        if installed:
            try:
                adb(a.serial, "shell", "am", "force-stop", PKG)
                result["restoration_command"] = adb(
                    a.serial, "install", "-r", a.backup_apk, timeout=90).decode().strip()
                result["restored_sha256"] = installed_sha(a.serial)
                result["restored_original"] = result["restored_sha256"] == sha(a.backup_apk)
            except Exception as exc:
                result["restoration_error"] = repr(exc)
        try:
            adb(a.serial, "shell", "rm", "-f", TEMP_XML)
        except Exception:
            pass
        (a.output / "result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
    if error or not result.get("restored_original", not installed):
        raise RuntimeError(f"Verification or restore failed; inspect {a.output / 'result.json'}")
    print("restored original preview", result["restored_original"], flush=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--reference-run", type=Path, required=True)
    parser.add_argument("--fixture-apk", type=Path, required=True)
    parser.add_argument("--fixture-sha", required=True)
    parser.add_argument("--backup-apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--initial-disabled", action="store_true")
    parser.add_argument("--dark", choices=["true", "false"], default="true")
    parser.add_argument("--surface-bridge", type=Path)
    parser.add_argument("--surface-observer", type=Path)
    main(parser.parse_args())
