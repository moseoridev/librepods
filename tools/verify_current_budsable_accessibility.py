#!/usr/bin/env python3
"""Test a source-backed BUDSABLE synthetic model with a private Android17 launcher.

This references (but does not copy) private local Java-bridge scripts. The
installed Manager/renderers and LibrePods product remain untouched. A test run
does not establish real hardware or Bluetooth support.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
PROBE_FILE = HERE / "verify_current_buds4_accessibility.py"

BUDSABLE_BOOTSTRAP = """
import pathlib, sys
p = pathlib.Path(sys.argv[1])
probe = pathlib.Path(sys.argv[2])
sys.argv = [str(p), *sys.argv[3:]]
def replace_once(text, original, adjusted, description):
    if text.count(original) != 1:
        raise RuntimeError('Profile mapping mismatch: ' + description)
    return text.replace(original, adjusted)
def transform(source, name):
    rules = {
        'initialize-synthetic-input.js': [
            ("Java.use('zn.c').E.value", "Java.use('zn.c').G.value"),
            ('Galaxy Buds4', 'Galaxy Buds On'),
            ('355', '363'),
            ('Synthetic regular Buds4 model', 'Synthetic BUDSABLE model')
        ],
        'initialize-feature-plugin-input.js': [
            ("Java.use('zn.c').E.value", "Java.use('zn.c').G.value"),
            ('Galaxy Buds4', 'Galaxy Buds On'),
            ('355', '363')
        ],
        'seed-home-cards.js': [
            ("Java.use('zn.c').E.value", "Java.use('zn.c').G.value"),
            ('HANDEL_BLACK', 'ABLE_BLACK')
        ],
        'seed-live-home.js': [('Galaxy Buds4', 'Galaxy Buds On')],
        'initialize-native-entry-input.js': [('Galaxy Buds4', 'Galaxy Buds On')],
        'launch-welcome-from-settings.js': [('Galaxy Buds4', 'Galaxy Buds On')],
        'launch-settings-entry.js': [("'device_id', 355", "'device_id', 363")],
    }
    for before, after in rules.get(name, []):
        if before not in source:
            raise RuntimeError('Profile input not found: ' + name + ' ' + before)
        source = source.replace(before, after)
    return source
code = p.read_text()
code = replace_once(
    code,
    "adb('shell','am','start','--user','0','-n','com.android.settings/.Settings')",
    "adb('shell','am','start','-W','--user','0','-n','com.android.settings/.Settings'); time.sleep(2)",
    'Settings startup'
)
code = replace_once(
    code,
    "source=(HERE/'scripts'/name).read_text()",
    "source=transform((HERE/'scripts'/name).read_text(),name)",
    'private input loader'
)
code = replace_once(code, "'Synthetic regular Buds4 model'",
                    "'Synthetic BUDSABLE model'", 'init completion marker')
code = replace_once(code, "'seed-home-cards.js','Seeded noise state'",
                    "'seed-home-cards.js','Seeded battery state'",
                    'battery completion marker')
ns = {'__name__': 'local_budsable_probe_module', '__file__': str(p),
      'transform': transform}
exec(compile(code, str(p), 'exec'), ns)
ns['PROBE'] = probe
ns['main']()
"""

def run(args):
    spec = importlib.util.spec_from_file_location("current_accessibility_probe", PROBE_FILE)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.BOOTSTRAP = BUDSABLE_BOOTSTRAP
    if args.output.exists():
        raise RuntimeError("Use a new evidence directory")
    details = {
        "model": "zn.c.G / BUDSABLE",
        "profile": "Galaxy Buds On, SM-U600, device ID 363, ABLE_BLACK",
        "source": "Current Manager 9.0.10.1724 (not historical 9.0.10.1704)",
        "scope": "Transient plugin/model/name/color/device-ID inputs; native UI renderer unchanged.",
        "bootstrap_sha256": hashlib.sha256(BUDSABLE_BOOTSTRAP.encode()).hexdigest(),
    }
    error = None
    try:
        result = module.run(args)
        details["probe_result"] = result["result"]
        details["seekbars"] = result.get("seekbars", [])
        details["card_bounds"] = result.get("card_bounds")
    except Exception as exc:
        error = repr(exc)
        details["error"] = error
    finally:
        if args.output.is_dir():
            (args.output / "profile-provenance.json").write_text(
                json.dumps(details, indent=2, ensure_ascii=False) + "\n")
    if error:
        raise RuntimeError(f"Profile probe failed: {error}")
    if not details["seekbars"] or any(s["enabled"] != "false" for s in details["seekbars"]):
        raise RuntimeError("BUDSABLE initial disabled balance was not established")
    print(json.dumps(details, ensure_ascii=False), flush=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--launcher", type=Path, required=True)
    parser.add_argument("--probe", type=Path,
                        default=HERE / "ksu_probe/probe.sh")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--surface-bridge", type=Path)
    parser.add_argument("--surface-observer", type=Path)
    run(parser.parse_args())
