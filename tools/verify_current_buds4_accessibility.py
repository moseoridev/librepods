#!/usr/bin/env python3
"""Bounded read-only UI observation using an existing private Buds4 launcher.

The launcher and private inputs are supplied by path and not embedded in this
repository. This probe never installs packages or changes product sources.
"""
import argparse
import hashlib
import json
import queue
import re
import subprocess
import threading
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BOOTSTRAP = """
import pathlib, sys
p = pathlib.Path(sys.argv[1])
probe = pathlib.Path(sys.argv[2])
sys.argv = [str(p), *sys.argv[3:]]
code = p.read_text()
old = "adb('shell','am','start','--user','0','-n','com.android.settings/.Settings')"
new = "adb('shell','am','start','-W','--user','0','-n','com.android.settings/.Settings'); time.sleep(2)"
assert code.count(old) == 1, 'Unexpected launcher version'
ns = {'__name__': 'local_probe_module', '__file__': str(p)}
exec(compile(code.replace(old, new), str(p), 'exec'), ns)
ns['PROBE'] = probe
ns['main']()
"""

def adb(serial, *arguments, timeout=30):
    return subprocess.check_output(
        ["adb", "-s", serial, *[str(x) for x in arguments]],
        timeout=timeout,
    )

def bounds(node):
    return list(map(int, re.findall(r"\d+", node.attrib["bounds"])))

def capture(serial, directory, index):
    remote = "/data/local/tmp/librepods-current-ui-observation.xml"
    for attempt in range(3):
        try:
            adb(serial, "shell", "uiautomator", "dump", remote, timeout=25)
            raw = adb(serial, "shell", "cat", remote)
            root = ET.fromstring(raw)
            break
        except (ValueError, ET.ParseError, subprocess.SubprocessError):
            if attempt == 2:
                raise
            time.sleep(1)
    png = adb(serial, "exec-out", "screencap", "-p", timeout=20)
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError("screencap is not PNG")
    (directory / f"state-{index:02d}.xml").write_bytes(raw)
    (directory / f"state-{index:02d}.png").write_bytes(png)
    nodes = list(root.iter("node"))
    texts = sorted({n.get("text", "") for n in nodes if n.get("text")})
    windows = adb(serial, "shell", "dumpsys", "window", timeout=15).decode(errors="replace")
    focus = next((l.strip() for l in windows.splitlines() if "mCurrentFocus=" in l), "")
    return {
        "index": index,
        "text": texts,
        "focus": focus,
        "image_sha256": hashlib.sha256(png).hexdigest(),
        "hierarchy_sha256": hashlib.sha256(raw).hexdigest(),
    }, nodes

def run(args):
    if not re.fullmatch(r"[A-Za-z0-9_.:-]+", args.serial):
        raise ValueError("Invalid serial")
    if args.output.exists():
        raise ValueError("Evidence output must not exist")
    if not args.launcher.is_file() or not args.probe.is_file():
        raise ValueError("Missing private launcher or root probe")
    command = [
        "uv", "run", "--offline", "--with", "frida==17.25.2",
        "python", "-u", "-c", BOOTSTRAP, str(args.launcher), str(args.probe),
        "--serial", args.serial, "--output", str(args.output),
    ]
    process = subprocess.Popen(
        command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT, text=True, bufsize=1,
    )
    lines = queue.Queue()
    all_lines = []
    threading.Thread(
        target=lambda: [lines.put(line) for line in process.stdout],
        daemon=True,
    ).start()
    report = {"serial": args.serial, "launcher": str(args.launcher),
              "scope": "Original Android17 settings route; navigation only, no setting changes.",
              "states": [], "result": "not_started"}
    started = False
    try:
        deadline = time.monotonic() + 110
        while time.monotonic() < deadline:
            try:
                line = lines.get(timeout=1)
                all_lines.append(line)
                if "READY: Manager remains open" in line:
                    started = True
                    break
            except queue.Empty:
                if process.poll() is not None:
                    break
        if not started:
            raise RuntimeError("Native launcher did not become ready")
        sizes = adb(args.serial, "shell", "wm", "size").decode()
        match = re.findall(r"(?:Physical|Override) size:\s*(\d+)x(\d+)", sizes)
        if not match:
            raise RuntimeError("Cannot establish the current display dimensions")
        width, height = map(int, match[-1])
        report["display_size"] = [width, height]
        for i in range(9):
            state, nodes = capture(args.serial, args.output, i)
            report["states"].append(state)
            print(json.dumps({"step": i, "focus": state["focus"], "texts": state["text"]},
                             ensure_ascii=False), flush=True)
            if "com.samsung.accessory.budsunitemgr/" not in state["focus"]:
                raise RuntimeError("Native Manager is not the focused Activity")
            accessibility = [n for n in nodes if n.get("text") in ("접근성", "Accessibility")]
            if accessibility:
                x0, y0, x1, y1 = bounds(accessibility[0])
                point = [(x0 + x1)//2, (y0 + y1)//2]
                report["accessibility_tap"] = point
                adb(args.serial, "shell", "input", "tap", *point)
                time.sleep(1.1)
                page, items = capture(args.serial, args.output, 20)
                report["states"].append(page)
                controls = [n for n in items if n.get("class") == "android.widget.SeekBar"]
                report["seekbars"] = [
                    {"bounds": bounds(n), "enabled": n.get("enabled"),
                     "text": n.get("text"), "range_description": n.get("content-desc")}
                    for n in controls
                ]
                report["result"] = "accessibility_visible" if controls else "page_without_seekbar"
                if controls:
                    parsed = ET.parse(args.output / "state-20.xml")
                    parents = {child: parent for parent in parsed.iter()
                               for child in parent}
                    labels = [n for n in parsed.iter("node")
                              if n.get("text") == "좌우 소리 균형"]
                    if len(labels) == 1:
                        candidate = labels[0]
                        while candidate is not None:
                            seekbars = [n for n in candidate.iter("node")
                                        if n.get("class") == "android.widget.SeekBar"]
                            if len(seekbars) == 1:
                                candidate_rect = bounds(candidate)
                                slider_rect = bounds(seekbars[0])
                                # An intermediate range column shares the
                                # slider's horizontal edges. The card has
                                # independently inset range content.
                                if (candidate_rect[0] < slider_rect[0] and
                                        candidate_rect[2] > slider_rect[2]):
                                    report["card_bounds"] = candidate_rect
                                    break
                            candidate = parents.get(candidate)
                    for sample_index in range(21, 25):
                        time.sleep(0.4)
                        frame, _ = capture(args.serial, args.output, sample_index)
                        if "com.samsung.accessory.budsunitemgr/" not in frame["focus"]:
                            raise RuntimeError("Native focus lost during stable-frame sampling")
                        report["states"].append(frame)
                    if args.surface_bridge and args.surface_observer:
                        from verify_current_buds4_surface_copy import copy_surface
                        pids = adb(args.serial, "shell", "pidof",
                                   "com.samsung.accessory.budsunitemgr").decode().split()
                        if len(pids) != 1:
                            raise RuntimeError("Could not select one Manager PID")
                        surface = copy_surface(args.serial, int(pids[0]),
                            "com.samsung.accessory.budsunitemgr", args.surface_bridge,
                            args.surface_observer, args.output / "surface-readback",
                            report["card_bounds"])
                        report["surface_readback"] = {
                            "path": str(args.output / "surface-readback/result.json"),
                            "accepted": surface["card_unchanged_after_observation"],
                            "formats": [r["format"] for r in surface["formats"]],
                        }
                break
            adb(args.serial, "shell", "input", "swipe",
                width // 2, int(height * .8), width // 2, int(height * .3), 430)
            time.sleep(0.8)
        else:
            report["result"] = "accessibility_entry_not_found"
    except Exception as exc:
        report["result"] = "probe_error"
        report["error"] = repr(exc)
        raise
    finally:
        if process.poll() is None:
            try:
                process.stdin.write("\n")
                process.stdin.flush()
                process.wait(timeout=35)
            except (OSError, subprocess.TimeoutExpired):
                process.terminate()
                try:
                    process.wait(timeout=8)
                except subprocess.TimeoutExpired:
                    process.kill()
        while not lines.empty():
            all_lines.append(lines.get_nowait())
        if args.output.exists():
            (args.output / "launcher-stdout.log").write_text("".join(all_lines))
            report["launcher_exit_code"] = process.returncode
            (args.output / "accessibility-probe.json").write_text(
                json.dumps(report, indent=2, ensure_ascii=False) + "\n")
        try:
            adb(args.serial, "shell", "rm", "-f",
                "/data/local/tmp/librepods-current-ui-observation.xml", timeout=10)
        except subprocess.SubprocessError:
            pass
    return report

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--launcher", type=Path, required=True)
    parser.add_argument("--probe", type=Path,
                        default=ROOT / "tools/ksu_probe/probe.sh")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--surface-bridge", type=Path)
    parser.add_argument("--surface-observer", type=Path)
    arguments = parser.parse_args()
    print(json.dumps(run(arguments), ensure_ascii=False), flush=True)
