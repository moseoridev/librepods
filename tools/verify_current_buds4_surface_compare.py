#!/usr/bin/env python3
"""Audit matched current-S25 native/independent fixture Surface PixelCopy data.

Reads saved immutable ARGB8888 bitmaps and little-endian RGBA_F16 buffers.
Does not connect to a device, resample pixels or change image coordinates.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops

def hash_file(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def rgb(path, rect):
    with Image.open(path) as img:
        if img.size != (1080, 2340):
            raise ValueError(f"Unexpected Surface source size {img.size}")
        image = img.convert("RGB").crop(tuple(rect))
    return np.asarray(image, dtype=np.int16), image

def metrics(a, b):
    d = np.abs(a - b)
    different = np.any(d != 0, axis=2)
    y, x = np.where(different)
    return ({
        "total_pixels": int(different.size),
        "unequal_pixels": int(different.sum()),
        "max_channel_delta": float(d.max()),
        "difference_local_bounds": None if not len(x) else [
            int(x.min()), int(y.min()), int(x.max() + 1), int(y.max() + 1)]
    }, different)

def read_surface(folder, rect):
    meta = json.loads((folder / "result.json").read_text())
    if meta.get("errors") or meta.get("card_unchanged_after_observation") is not True:
        raise ValueError(f"Rejected Surface sample {folder}")
    refs = {item["format"]: item for item in meta["formats"]}
    if set(refs) != {"ARGB_8888", "RGBA_F16"}:
        raise ValueError("Missing matched PixelCopy formats")
    full = refs["ARGB_8888"]
    half = refs["RGBA_F16"]
    if (full.get("result") != 0 or half.get("result") != 0 or
            full.get("requestedSourceRect") != [0, 0, 1080, 2340] or
            half.get("requestedSourceRect") != rect or
            full.get("destinationSize") != [1080, 2340] or
            half.get("destinationSize") != [rect[2] - rect[0], rect[3] - rect[1]]):
        raise ValueError("PixelCopy source/destination/configuration mismatch")
    if half.get("nativeByteOrder") != "LITTLE_ENDIAN":
        raise ValueError(f"Unsupported F16 byte order {half.get('nativeByteOrder')}")
    h = rect[3] - rect[1]
    w = rect[2] - rect[0]
    buffer = np.fromfile(folder / "RGBA_F16.raw", dtype="<f2")
    if buffer.size != h * w * 4 or buffer.nbytes != half["byteCount"]:
        raise ValueError("F16 native raw pixel count incorrect")
    buffer = buffer.reshape((h, w, 4)).astype(np.float32)
    image, image_obj = rgb(folder / "ARGB_8888.png", rect)
    composed, _ = rgb(folder / "before.png", rect)
    after, _ = rgb(folder / "after.png", rect)
    within, _ = metrics(image, composed)
    post, _ = metrics(composed, after)
    if within["unequal_pixels"] != 0 or post["unequal_pixels"] != 0:
        raise ValueError("PixelCopy observation changed card or compositor disagrees")
    return {
        "folder": str(folder), "metadata": meta,
        "ARGB_8888_png_sha256": hash_file(folder / "ARGB_8888.png"),
        "RGBA_F16_raw_sha256": hash_file(folder / "RGBA_F16.raw"),
        "screen_to_surface_card": within, "before_after_card": post,
    }, image, image_obj, buffer

def main(args):
    if args.output.exists():
        raise ValueError("Use a new evidence output directory")
    rect = [30, 486, 1050, 816]
    n, png_native, pil_native, native = read_surface(args.native_surface, rect)
    f, png_fixture, pil_fixture, fixture = read_surface(args.fixture_surface, rect)
    ar, ar_mask = metrics(png_native, png_fixture)
    f16, f16_mask = metrics(native[:, :, :3], fixture[:, :, :3])
    alpha, _ = metrics(native[:, :, 3:], fixture[:, :, 3:])
    pil_diff = ImageChops.difference(pil_native, pil_fixture)
    independent = sum(1 for p in pil_diff.get_flattened_data() if any(p))
    if independent != ar["unequal_pixels"]:
        raise ValueError("ImageChops pixel count differs from NumPy")
    if not np.array_equal(ar_mask, f16_mask):
        raise ValueError("ARGB and F16 discrepancy masks differ")
    if alpha["unequal_pixels"] != 0:
        raise ValueError("Alpha also differs; need separate analysis")
    native_card = n["metadata"]["card_rect"]
    fixture_card = f["metadata"]["card_rect"]
    if native_card != rect or fixture_card != rect:
        raise ValueError("Independent source card boundaries differ")
    report = {
        "scope": "Android17/API37 Samsung original vs pinned independent fixture initial disabled BUDSABLE 1020x330 card, PixelCopy existing Activity Surface, ARGB8888 and RGBA_F16 destinations",
        "rect": rect,
        "native": n,
        "fixture": f,
        "comparisons": {
            "surface_ARGB8888_rgb": ar,
            "surface_RGBA_F16_rgb": f16,
            "surface_RGBA_F16_alpha": alpha,
            "ARGB_F16_difference_mask_intersection": int((ar_mask & f16_mask).sum()),
            "ARGB_F16_difference_mask_union": int((ar_mask | f16_mask).sum()),
        },
        "independent": {"Pillow_ImageChops_ARGB8888_unequal": independent,
                        "agrees_with_NumPy": True},
        "interpretation_limit": "PixelCopy RGBA_F16 is the destination bitmap conversion, not an assertion of underlying hardware framebuffer bit depth. Source/fixture must have independent matching app state; shadow root cause remains unknown.",
    }
    args.output.mkdir(parents=True)
    (args.output / "result.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({"comparisons": report["comparisons"], "independent": report["independent"]}, ensure_ascii=False))

if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--native-surface", type=Path, required=True)
    p.add_argument("--fixture-surface", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    main(p.parse_args())
