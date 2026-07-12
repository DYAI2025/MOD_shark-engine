#!/usr/bin/env python3
"""Deterministic pixel-art texture generator for Shark Engine's aircraft
extension (AIRCRAFT_CONCEPT_V2.md §5.2, task AIR-032).

Each texture is produced by a pure, deterministic drawing function in
parts/<part_id>.py — no unseeded randomness, so regenerating is diff-stable.
Colors come exclusively from palette.json; nothing here or in a part module
should hardcode a hex value.

Usage:
    python3 generate.py                 # regenerate every registered part
    python3 generate.py airframe_panel  # regenerate just this one
    python3 generate.py --check         # regenerate to a temp dir and diff
                                         # against committed output (CI use);
                                         # exits non-zero on any difference
"""
from __future__ import annotations

import argparse
import filecmp
import importlib
import json
import sys
import tempfile
from pathlib import Path

from PIL import Image

ASSET_GEN_DIR = Path(__file__).resolve().parent
REPO_ROOT = ASSET_GEN_DIR.parent.parent
PARTS_DIR = ASSET_GEN_DIR / "parts"
RESOURCES_ROOT = (
    REPO_ROOT
    / "sharkengine"
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "sharkengine"
    / "textures"
)
TEXTURE_SIZE = 16


def load_palette() -> dict:
    with open(ASSET_GEN_DIR / "palette.json", encoding="utf-8") as f:
        data = json.load(f)
    return data["families"]


def discover_parts() -> list[str]:
    """Every parts/<name>.py (excluding dunder/private files) is a registered part id."""
    return sorted(
        p.stem
        for p in PARTS_DIR.glob("*.py")
        if not p.stem.startswith("_")
    )


def load_part_module(part_id: str):
    sys.path.insert(0, str(ASSET_GEN_DIR))
    try:
        return importlib.import_module(f"parts.{part_id}")
    finally:
        sys.path.pop(0)


def render_part(part_id: str, palette: dict) -> tuple[Image.Image, str]:
    """Returns (image, category) where category is "block" or "item"."""
    module = load_part_module(part_id)
    if not hasattr(module, "draw"):
        raise AttributeError(f"parts/{part_id}.py must define draw(img, palette)")
    img = Image.new("RGBA", (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    module.draw(img, palette)
    if img.size != (TEXTURE_SIZE, TEXTURE_SIZE):
        raise ValueError(f"{part_id}: draw() resized the image — must stay {TEXTURE_SIZE}x{TEXTURE_SIZE}")
    category = getattr(module, "CATEGORY", "block")
    if category not in ("block", "item"):
        raise ValueError(f"{part_id}: CATEGORY must be 'block' or 'item', got {category!r}")
    return img, category


def write_part(part_id: str, palette: dict, output_root: Path) -> Path:
    img, category = render_part(part_id, palette)
    out_dir = output_root / category
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{part_id}.png"
    img.save(out_path)
    return out_path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("parts", nargs="*", help="Part ids to regenerate (default: all registered parts)")
    parser.add_argument("--check", action="store_true", help="Regenerate to a temp dir and diff against committed output; exit non-zero on any difference")
    args = parser.parse_args()

    palette = load_palette()
    part_ids = args.parts or discover_parts()
    if not part_ids:
        print("No parts registered under tools/asset-gen/parts/ — nothing to do.", file=sys.stderr)
        return 1

    if args.check:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_root = Path(tmp)
            mismatches = []
            for part_id in part_ids:
                tmp_path = write_part(part_id, palette, tmp_root)
                category = tmp_path.parent.name
                committed_path = RESOURCES_ROOT / category / f"{part_id}.png"
                if not committed_path.exists():
                    mismatches.append(f"{part_id}: no committed output at {committed_path}")
                elif not filecmp.cmp(tmp_path, committed_path, shallow=False):
                    mismatches.append(f"{part_id}: regenerated output differs from committed {committed_path}")
            if mismatches:
                print("Diff-stability check FAILED:", file=sys.stderr)
                for m in mismatches:
                    print(f"  - {m}", file=sys.stderr)
                return 1
            print(f"Diff-stability check passed for {len(part_ids)} part(s).")
            return 0

    for part_id in part_ids:
        out_path = write_part(part_id, palette, RESOURCES_ROOT)
        print(f"generated {out_path.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
