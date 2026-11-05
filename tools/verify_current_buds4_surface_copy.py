#!/usr/bin/env python3
"""Read-only Surface PixelCopy helper for the currently focused Android Activity.

Uses the maintainer's private, reviewed Frida Java bridge and archived observer
source by explicit path. No renderer substitutions, state writes or system setup.
"""
import base64
import hashlib
import json
import subprocess
from pathlib import Path

import frida
import numpy as np
from PIL import Image

def sha(data):
    return hashlib.sha256(data).hexdigest()

def focused_card(serial, rect):
    data = subprocess.check_output(["adb", "-s", serial, "exec-out", "screencap", "-p"], timeout=25)
    from io import BytesIO
    im = Image.open(BytesIO(data)).convert("RGB")
    if im.size != (1080, 2340):
        raise RuntimeError(f"Unexpected physical screenshot {im.size}")
    x, y, x2, y2 = rect
    return data, np.asarray(im)[y:y2, x:x2].copy()

def copy_surface(serial, pid, expected_name, bridge_js, observer_js, output, card_rect):
    output = Path(output)
    if output.exists():
        raise ValueError("Use a new evidence directory for each Surface capture")
    output.mkdir(parents=True)
    baseline, before = focused_card(serial, card_rect)
    (output / "before.png").write_bytes(baseline)
    bridge = Path(bridge_js).read_bytes()
    observer = Path(observer_js).read_bytes()
    evidence = {
        "scope": "Current focused Activity's existing Surface via public PixelCopy; no state, renderer or window write.",
        "serial": serial,
        "pid": int(pid),
        "expected_name": expected_name,
        "card_rect": card_rect,
        "java_bridge_sha256": sha(bridge),
        "observer_sha256": sha(observer),
        "before_sha256": sha(baseline),
        "formats": [],
        "errors": [],
    }
    session = None
    script = None
    try:
        d = frida.get_device_manager().add_remote_device("127.0.0.1:27042")
        found = [p for p in d.enumerate_processes() if p.pid == int(pid)]
        if len(found) != 1 or found[0].name != expected_name:
            raise RuntimeError(f"Refusing attach: PID name differs, got {[(x.pid,x.name) for x in found]}")
        session = d.attach(pid)
        source = bridge.decode() + "\n" + observer.decode()
        script = session.create_script(source, name="librepods-readonly-surface")
        messages = []
        script.on("message", lambda msg, _: messages.append(msg))
        script.load()
        for fmt in ("ARGB_8888", "RGBA_F16"):
            payload = script.exports_sync.copy(fmt, card_rect)
            png = base64.b64decode(payload.pop("png_base64"))
            target = output / f"{fmt}.png"
            target.write_bytes(png)
            if "raw_base64" in payload:
                raw = base64.b64decode(payload.pop("raw_base64"))
                raw_path = output / f"{fmt}.raw"
                raw_path.write_bytes(raw)
                if len(raw) != payload["byteCount"]:
                    raise RuntimeError("Native F16 buffer byte size mismatch")
                payload["raw_sha256"] = sha(raw)
                payload["raw_byte_count"] = len(raw)
            if payload.get("result") != 0:
                raise RuntimeError("PixelCopy did not succeed")
            evidence["formats"].append({"format": fmt, "png_sha256": sha(png), **payload})
        evidence["message_events"] = messages
    except Exception as error:
        evidence["errors"].append(repr(error))
    finally:
        if script is not None:
            try:
                script.unload()
            except Exception as error:
                evidence["errors"].append("script unload: " + repr(error))
        if session is not None:
            try:
                session.detach()
            except Exception as error:
                evidence["errors"].append("session detach: " + repr(error))
        try:
            after_png, after = focused_card(serial, card_rect)
            (output / "after.png").write_bytes(after_png)
            evidence["after_sha256"] = sha(after_png)
            evidence["card_unchanged_after_observation"] = bool(np.array_equal(before, after))
        except Exception as error:
            evidence["errors"].append("post-observation sample: " + repr(error))
        (output / "result.json").write_text(json.dumps(evidence, indent=2, ensure_ascii=False) + "\n")
    if evidence["errors"] or not evidence.get("card_unchanged_after_observation"):
        raise RuntimeError(f"Surface sample not accepted; inspect {output / 'result.json'}")
    return evidence
