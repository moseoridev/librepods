#!/usr/bin/env python3
"""Run standalone settings-ui instrumentation under one device configuration.

Captures and restores existing Android wm overrides, system font scale, and
night mode in a finally block. Never installs an emulator or product :app APK.
"""
import argparse
import json
import os
import re
import subprocess
import time
from pathlib import Path

def command(argv, timeout=30, env=None, cwd=None):
    return subprocess.run(argv, check=False, timeout=timeout, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env=env,
                          cwd=cwd)

def adb(serial, *args, timeout=20):
    r = command(["adb", "-s", serial, *args], timeout=timeout)
    if r.returncode:
        raise RuntimeError(f"ADB command {args!r} failed: {r.stdout}")
    return r.stdout.strip()

def get_settings(serial):
    size = adb(serial, "shell", "wm", "size")
    density = adb(serial, "shell", "wm", "density")
    font = adb(serial, "shell", "settings", "get", "system", "font_scale")
    night = adb(serial, "shell", "cmd", "uimode", "night")
    sdk = adb(serial, "shell", "getprop", "ro.build.version.sdk")
    if sdk != "37":
        raise RuntimeError("Target is not the approved Android17/API37 S25")
    def fields(text):
        return dict((label, val) for label,val in
                    re.findall(r"(Physical|Override) (?:size|density):\s*([^\n]+)", text))
    ns = fields(size)
    ds = fields(density)
    if "Physical" not in ns or "Physical" not in ds:
        raise RuntimeError(f"Unrecognized wm state: {size!r}; {density!r}")
    if not font or font == "null" or "Night mode:" not in night:
        raise RuntimeError("Could not save the active font/theme configuration")
    return {"size":ns, "density":ds, "font":font,
            "night":night.split("Night mode:",1)[1].strip(), "sdk":sdk}

def set_override(serial, kind, value):
    adb(serial, "shell", "wm", kind, value)

def restore(serial, state):
    errors=[]
    for kind in ("size","density"):
        prior=state[kind].get("Override")
        try:
            set_override(serial,kind,prior or "reset")
        except Exception as e:
            errors.append(repr(e))
    try:
        adb(serial,"shell","settings","put","system","font_scale",state["font"])
    except Exception as e:
        errors.append(repr(e))
    try:
        adb(serial,"shell","cmd","uimode","night",state["night"])
    except Exception as e:
        errors.append(repr(e))
    return errors

def main(args):
    if not re.fullmatch(r"[A-Za-z0-9_.:-]+",args.serial):
        raise ValueError("Invalid explicit serial")
    if args.output.exists():
        raise ValueError("Use a new evidence directory")
    devices = adb(args.serial, "get-serialno")
    model = adb(args.serial, "shell", "getprop", "ro.product.model")
    if devices != args.serial or model != "SM-S931N":
        raise RuntimeError(f"Incorrect device: {devices} / {model}")
    initial = get_settings(args.serial)
    args.output.mkdir(parents=True)
    result = {"serial":args.serial,"model":model,
              "scope":"Standalone :settings-ui instrumentation only; tests assert component contracts, not OEM pixel parity.",
              "configuration":{"size":args.size,"density":args.density,
                               "font":args.font,"theme":args.theme},
              "initial":initial,"task":":settings-ui:connectedDebugAndroidTest",
              "success":False}
    try:
        set_override(args.serial,"size",args.size)
        set_override(args.serial,"density",str(args.density))
        adb(args.serial,"shell","settings","put","system","font_scale",str(args.font))
        adb(args.serial,"shell","cmd","uimode","night",args.theme)
        time.sleep(3)
        result["active"]=get_settings(args.serial)
        assert result["active"]["size"].get("Override",result["active"]["size"]["Physical"])==args.size
        assert result["active"]["density"].get("Override",result["active"]["density"]["Physical"])==str(args.density)
        assert abs(float(result["active"]["font"])-args.font)<.02
        assert result["active"]["night"]==args.theme
        env=os.environ.copy()
        env.update({"ANDROID_SERIAL":args.serial,
                    "ANDROID_HOME":args.sdk_root,
                    "ANDROID_SDK_ROOT":args.sdk_root})
        gradle=[str(Path(__file__).resolve().parents[1] / "android" / "gradlew"),
                ":settings-ui:connectedDebugAndroidTest",
                "--console=plain","--no-daemon","--max-workers=2",
                "-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8"]
        if args.test_classes:
            gradle.append("-Pandroid.testInstrumentationRunnerArguments.class="+args.test_classes)
        result["command"]=gradle
        try:
            r=command(gradle,timeout=args.test_timeout,env=env,
                      cwd=Path(__file__).resolve().parents[1] / "android")
        except subprocess.TimeoutExpired as exc:
            partial = exc.stdout or b""
            (args.output/"gradle.log").write_text(
                partial.decode(errors="replace") if isinstance(partial, bytes) else partial)
            raise RuntimeError(f"Gradle exceeded {args.test_timeout}s; partial log preserved") from exc
        (args.output/"gradle.log").write_text(r.stdout)
        result["gradle_exit_code"]=r.returncode
        result["build_success"]="BUILD SUCCESSFUL" in r.stdout
        starts = re.findall(r"Starting (\\d+) tests on SM-S931N - 17", r.stdout)
        finishes = re.findall(r"Finished (\\d+) tests on SM-S931N - 17", r.stdout)
        result["executed_test_count"] = int(finishes[-1]) if finishes else 0
        result["test_pass"] = (r.returncode == 0 and result["build_success"] and
            len(starts) == 1 and len(finishes) == 1 and starts[0] == finishes[0] and
            result["executed_test_count"] > 0)
        result["gradle_log_tail"]=r.stdout[-4000:]
        result["success"]=result["test_pass"]
        print("build",r.returncode,"success",result["success"],flush=True)
    except Exception as e:
        result["error"]=repr(e)
        print("matrix error",repr(e),flush=True)
    finally:
        result["restoration_errors"]=restore(args.serial,initial)
        try:
            result["restored"]=get_settings(args.serial)
            expected=initial
            actual=result["restored"]
            result["restored_equal"]= (expected["size"]==actual["size"]
                and expected["density"]==actual["density"]
                and abs(float(expected["font"])-float(actual["font"]))<.02
                and expected["night"]==actual["night"])
        except Exception as e:
            result["restored_equal"]=False
            result["restoration_errors"].append(repr(e))
        (args.output/"result.json").write_text(
            json.dumps(result,indent=2,ensure_ascii=False)+"\n")
    if not result["success"] or not result["restored_equal"]:
        raise RuntimeError(f"Device matrix test failed/restore not verified; see {args.output / 'result.json'}")
    print(json.dumps({"configuration":result["configuration"],
                      "success":result["success"],
                      "restored_equal":result["restored_equal"]},
                     ensure_ascii=False),flush=True)

if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--serial",required=True)
    p.add_argument("--size",required=True)
    p.add_argument("--density",required=True,type=int)
    p.add_argument("--font",required=True,type=float)
    p.add_argument("--theme",choices=["yes","no"],required=True)
    p.add_argument("--sdk-root",default=os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
                   or str(Path.home()/"Library/Android/sdk"))
    p.add_argument("--test-classes",default="")
    p.add_argument("--test-timeout",type=int,default=160)
    p.add_argument("--output",type=Path,required=True)
    main(p.parse_args())
