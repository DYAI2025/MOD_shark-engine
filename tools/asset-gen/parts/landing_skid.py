"""landing_skid — the aircraft's landing rail ("Kufenprofil"). Material
assignment (palette.json): skid_frame = light_alloy only, no steel/brass.

Shape, not a flat panel: a thin runner bar with a shallow upward curve at
both ends (AIRCRAFT_CONCEPT_V2.md §5.3 — "landing_skid Kufenprofil"), drawn
across the lower-middle rows so it reads as a rail rather than a block face.
Everything outside the rail stays fully transparent.

Directional readability: top row of the rail is the light_alloy highlight
(light hitting the curve from above), bottom row is the shadow/deep
underside — see AIRCRAFT_CONCEPT_V2.md §5.1.
"""
from __future__ import annotations

from PIL import Image

from parts._common import set_pixel

CATEGORY = "block"


def draw(img: Image.Image, palette: dict) -> None:
    alloy = palette["light_alloy"]

    w, h = img.size

    # Shallow arc: the rail's top-surface row per column, dipping 1px lower
    # in the center than at the ends — a runner curving up off the ground
    # at nose and tail. Columns 0-15; center sits lowest.
    # top_y(x): 10 at the outer edges, 11 across the middle span.
    top_y = [10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 10, 10, 10]

    for x in range(w):
        ty = top_y[x]
        # Rail cross-section is 4px tall: highlight row, base row, shadow
        # row, deep-tone contact line. Adjacent columns whose top_y differs
        # by 1 (the step at the curve) are diagonally contiguous, so the
        # arc reads as one continuous bent rail with no extra riser needed.
        set_pixel(img, x, ty, alloy["light"])
        set_pixel(img, x, ty + 1, alloy["base"])
        set_pixel(img, x, ty + 2, alloy["shadow"])
        set_pixel(img, x, ty + 3, alloy["deep"])

    # Leading (left) tip highlight, trailing (right) tip shadow — keeps the
    # part orientable at a glance even though the rail is symmetric in
    # shape.
    set_pixel(img, 0, top_y[0], alloy["light"])
    set_pixel(img, w - 1, top_y[w - 1], alloy["shadow"])
