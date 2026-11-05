#!/usr/bin/env python3
"""Run a bounded Android17 Samsung native/fixture comparison at one display size.

Changes only temporary device display/theme/font settings and standalone preview
APK; restores prior settings and checks restoration even after failed comparison.
"""
import argparse
import json
import re
import subprocess
import sys
import time
from pathlib import Path

from verify_settings_ui_config_matrix import adb, get_settings, set_override, restore

TOOLS = Path(__file__).resolve().parent

def run_logged(cmd, path, timeout=190):
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    path.write_text(r.stdout + "\n" + r.stderr)
    print("executed", Path(cmd[-1]).name, "exit", r.returncode, flush=True)
    if r.returncode:
        raise RuntimeError("Subprocess failed: " + repr(cmd) + " / " + str(path))
    return r

def main(args):
    if args.output.exists():
        raise ValueError("Use fresh evidence output")
    if not re.fullmatch(r"[A-Za-z0-9_.:-]+", args.serial):
        raise ValueError("Unsafe serial")
    if adb(args.serial, "shell", "getprop", "ro.product.model") != "SM-S931N":
        raise RuntimeError("Only S25 SM-S931N is approved")
    initial = get_settings(args.serial)
    args.output.mkdir(parents=True)
    output = {"initial":initial, "serial":args.serial,
              "target":{"size":args.size,"density":args.density,
                        "font":args.font,"theme":args.theme,"model":args.model},
              "scope":"Single Android17 Samsung reference slider card RGB parity, not entire settings-ui.",
              "finished":False}
    try:
        set_override(args.serial,"size",args.size)
        set_override(args.serial,"density",str(args.density))
        adb(args.serial,"shell","settings","put","system","font_scale",str(args.font))
        adb(args.serial,"shell","cmd","uimode","night",args.theme)
        time.sleep(3)
        output["active"] = get_settings(args.serial)
        if (output["active"]["size"].get("Override")!=args.size or
            output["active"]["density"].get("Override")!=str(args.density) or
            abs(float(output["active"]["font"])-args.font)>.02 or
            output["active"]["night"]!=args.theme):
            raise RuntimeError("Device did not acquire requested comparison configuration")
        native = TOOLS / ("verify_current_budsable_accessibility.py"
                          if args.model=="budsable" else "verify_current_buds4_accessibility.py")
        native_dir = args.output / "native"
        fixture_dir = args.output / "fixture"
        run_logged([sys.executable, str(native), "--launcher", str(args.launcher),
                    "--probe", str(TOOLS/"ksu_probe/probe.sh"),
                    "--serial", args.serial, "--output", str(native_dir)],
                   args.output / "native.stdout.log", timeout=185)
        original = json.loads((native_dir / "accessibility-probe.json").read_text())
        if original.get("result")!="accessibility_visible" or not original.get("card_bounds"):
            raise RuntimeError("Native comparison state or card bounds unavailable")
        states=original.get("seekbars",[])
        disabled=args.model=="budsable"
        if len(states)!=1 or (states[0]["enabled"]=="false")!=disabled:
            raise RuntimeError("Native slider state differs from selected test profile")
        fixture_cmd=["uv","run","--offline","--with","numpy","--with","pillow",
                     "python","-u",str(TOOLS/"verify_current_buds4_fixture.py"),
                     "--serial",args.serial,"--reference-run",str(native_dir),
                     "--fixture-apk",str(args.fixture_apk),"--fixture-sha",args.fixture_sha,
                     "--backup-apk",str(args.backup_apk),"--dark",
                     "true" if args.theme=="yes" else "false",
                     "--output",str(fixture_dir)]
        if disabled:
            fixture_cmd.append("--initial-disabled")
        run_logged(fixture_cmd,args.output/"fixture.stdout.log",timeout=160)
        paired=json.loads((fixture_dir/"result.json").read_text())
        if not paired.get("card_bounds_equal") or not paired.get("restored_original"):
            raise RuntimeError("Fixture geometry mismatch or restoration unverified")
        output["native_card_rect"]=original["card_bounds"]
        output["comparison"]=paired["comparisons"][0]
        output["fixture_restored"]=paired["restored_original"]
        output["finished"]=True
    except Exception as exc:
        output["error"]=repr(exc)
        print("matrix failed",repr(exc),flush=True)
    finally:
        output["restoration_errors"]=restore(args.serial,initial)
        try:
            current=get_settings(args.serial)
            output["restored"]=current
            output["device_restored"]=(current["size"]==initial["size"] and
                                      current["density"]==initial["density"] and
                                      current["night"]==initial["night"] and
                                      abs(float(current["font"])-float(initial["font"]))<.02)
        except Exception as exc:
            output["restoration_errors"].append(repr(exc))
            output["device_restored"]=False
        (args.output/"result.json").write_text(json.dumps(output,indent=2,ensure_ascii=False)+"\n")
    if not output["finished"] or not output["device_restored"]:
        raise RuntimeError(f"Pixel test incomplete or device restoration failed: {args.output/'result.json'}")
    print(json.dumps({"target":output["target"],"comparison":output["comparison"],
                      "device_restored":output["device_restored"]},ensure_ascii=False),flush=True)

if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--serial",required=True)
    p.add_argument("--size",required=True)
    p.add_argument("--density",type=int,required=True)
    p.add_argument("--font",type=float,default=1.0)
    p.add_argument("--theme",choices=["yes","no"],required=True)
    p.add_argument("--model",choices=["regular","budsable"],default="regular")
    p.add_argument("--launcher",type=Path,required=True)
    p.add_argument("--fixture-apk",type=Path,required=True)
    p.add_argument("--fixture-sha",required=True)
    p.add_argument("--backup-apk",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    main(p.parse_args())
