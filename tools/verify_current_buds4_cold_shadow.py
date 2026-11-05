#!/usr/bin/env python3
"""Audit two cold disabled native S25 cards against an independent UI fixture.

Reads preserved screenshots and manifests only; never touches a device. All
comparisons use original pixel coordinates and complete matching card regions.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops

from verify_current_buds4_fixture import card_rect

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def rgb_card(path, rect):
    with Image.open(path) as src:
        if src.size != (1080, 2340):
            raise ValueError(f"Unexpected screenshot size for {path}: {src.size}")
        image = src.convert("RGB")
        box = image.crop(tuple(rect))
    return image, box, np.asarray(box, dtype=np.int16)

def diff(a, b):
    difference = np.abs(a - b)
    mask = np.any(difference != 0, axis=2)
    ys, xs = np.where(mask)
    return {
        "pixels": int(mask.size),
        "exact_unequal_pixels": int(mask.sum()),
        "sum_rgb_above_3": int((difference.sum(axis=2) > 3).sum()),
        "max_channel_delta": int(difference.max()),
        "difference_local_bounds": None if len(xs) == 0 else [
            int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)],
    }, mask

def load_native(run):
    probe = json.loads((run / "accessibility-probe.json").read_text())
    profile = json.loads((run / "profile-provenance.json").read_text())
    manifest = json.loads((run / "manifest.json").read_text())
    if probe["result"] != "accessibility_visible" or profile["model"] != "zn.c.G / BUDSABLE":
        raise ValueError(f"Reference model or Accessibility state differs: {run}")
    if len(probe.get("seekbars", [])) != 1 or probe["seekbars"][0]["enabled"] != "false":
        raise ValueError(f"Native initial slider must be disabled: {run}")
    if not probe.get("launcher_exit_code") == 0:
        raise ValueError(f"Native launcher cleanup failed: {run}")
    rect = card_rect((run / "state-22.xml").read_bytes(), False)
    captures = [run / f"state-{i:02d}.png" for i in (21, 22, 23, 24)]
    arrays = [rgb_card(p, rect)[2] for p in captures]
    if any(not np.array_equal(arrays[0], arr) for arr in arrays[1:]):
        raise ValueError(f"Native card not stable across accepted states: {run}")
    return {
        "run": str(run),
        "model": profile["model"],
        "profile": profile["profile"],
        "profile_bootstrap_sha256": profile["bootstrap_sha256"],
        "manager_sha256": manifest["apk_sha256"],
        "card_rect": rect,
        "accepted_source_pngs": [{"path": str(p), "sha256": sha(p)} for p in captures],
        "card_rgb_sha256": hashlib.sha256(arrays[0].astype(np.uint8).tobytes()).hexdigest(),
    }, arrays[0]

def main(args):
    if args.output.exists():
        raise ValueError("Evidence output already exists")
    n1, a = load_native(args.native_1)
    n2, b = load_native(args.native_2)
    third = load_native(args.native_3) if args.native_3 else None
    if third:
        n3, c = third
        if (n3["card_rect"] != n1["card_rect"] or
                n3["manager_sha256"] != n1["manager_sha256"] or
                n3["profile_bootstrap_sha256"] != n1["profile_bootstrap_sha256"]):
            raise ValueError("Third cold source input, version or geometry differs")
    if n1["card_rect"] != n2["card_rect"]:
        raise ValueError("Native card geometry differs across cold runs")
    if n1["manager_sha256"] != n2["manager_sha256"]:
        raise ValueError("Native Manager version differs")
    if n1["profile_bootstrap_sha256"] != n2["profile_bootstrap_sha256"]:
        raise ValueError("Native synthetic input selection differs")

    prior = json.loads((args.fixture_run / "result.json").read_text())
    if (prior.get("expected_enabled") is not False or
            prior.get("restored_original") is not True or
            prior.get("card_bounds_equal") is not True):
        raise ValueError("Fixture run lacks disabled-state or restoration proof")
    if prior["reference_card_rect"] != n1["card_rect"]:
        raise ValueError("Fixture and native card positions differ")
    fixture_file = Path(prior["comparisons"][0]["fixture_frame"])
    if fixture_file.resolve().parent != args.fixture_run.resolve():
        raise ValueError("Fixture comparison refers outside specified run")
    _, fixture_pil, fixture = rgb_card(fixture_file, n1["card_rect"])
    d1, m1 = diff(a, fixture)
    d2, m2 = diff(b, fixture)
    cross, _ = diff(a, b)
    if third:
        d3, m3 = diff(c, fixture)
        cross13, _ = diff(a, c)
        cross23, _ = diff(b, c)

    _, original_pil, _ = rgb_card(args.native_1 / "state-21.png", n1["card_rect"])
    pil_diff = ImageChops.difference(original_pil, fixture_pil)
    pillow_count = sum(1 for channels in pil_diff.get_flattened_data() if any(channels))
    if pillow_count != d1["exact_unequal_pixels"]:
        raise ValueError("Pillow/ImageChops and NumPy disagree")

    old_rect = n1["card_rect"]
    _, _, old_source = rgb_card(args.historical_source, old_rect)
    _, _, old_fixture = rgb_card(args.historical_fixture, old_rect)
    historical, old_mask = diff(old_source, old_fixture)
    report = {
        "scope": "Original complete balance cards, Android17/API37/Manager9.0.10.1724, first disabled frame versus pinned independent UI v199; old Android16 pairs for historical context only",
        "geometry": n1["card_rect"],
        "inputs": {"native_1": n1, "native_2": n2,
                   **({"native_3": n3} if third else {}),
                   "fixture_run": str(args.fixture_run),
                   "fixture_sha256": prior["fixture_apk_sha256"],
                   "fixture_png_sha256": sha(fixture_file),
                   "original_preview_restored": prior["restored_original"],
                   "original_preview_sha256": prior["restored_sha256"],
                   "historical_source_png": str(args.historical_source),
                   "historical_source_png_sha256": sha(args.historical_source),
                   "historical_fixture_png": str(args.historical_fixture),
                   "historical_fixture_png_sha256": sha(args.historical_fixture)},
        "comparisons": {
            "current_native_1_vs_fixture": d1,
            "current_native_2_vs_fixture": d2,
            "current_native_1_vs_2": cross,
            **({"current_native_3_vs_fixture": d3,
                "current_native_1_vs_3": cross13,
                "current_native_2_vs_3": cross23} if third else {}),
            "historical_android16_native_vs_fixture": historical,
        },
        "independent_check": {"Pillow_ImageChops_unequal_pixels": pillow_count,
                              "agrees_with_NumPy": True},
        "historical_vs_current_difference_masks": {
            "intersection_pixels": int((old_mask & m1).sum()),
            "union_pixels": int((old_mask | m1).sum()),
            "same_mask_current_cold_runs": bool(np.array_equal(m1, m2) and
                (not third or np.array_equal(m1, m3))),
        },
        "limits": [
            "This cold-run PNG comparison is distinct from current Android17 PixelCopy ARGB/F16 evidence", 
            "No initial-state drawing color/position fix is supported by this observation",
            "Different original models from regular BUDS4 are used to obtain native disabled state",
            "Neither pixel parity nor Bluetooth/physical hardware operation is established outside these cards",
            "No image resizing, offsets, painting, or thresholding of differences",
        ],
    }
    args.output.mkdir(parents=True)
    (args.output / "result.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({key: val for key, val in report.items() if key in
                      ("geometry", "comparisons", "independent_check",
                       "historical_vs_current_difference_masks")}, ensure_ascii=False))

if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--native-1", type=Path, required=True)
    ap.add_argument("--native-2", type=Path, required=True)
    ap.add_argument("--native-3", type=Path)
    ap.add_argument("--fixture-run", type=Path, required=True)
    ap.add_argument("--historical-source", type=Path, required=True)
    ap.add_argument("--historical-fixture", type=Path, required=True)
    ap.add_argument("--output", type=Path, required=True)
    main(ap.parse_args())
