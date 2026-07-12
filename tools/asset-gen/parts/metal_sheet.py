"""metal_sheet — raw sheet-metal crafting stock, intermediate ingredient for
airframe_panel/fuselage_frame and other structural parts (concept doc §4
"Zwischenprodukte & Rezepte": II/CC of iron_ingot+copper_ingot -> x4). Material
assignment (palette.json): dark_steel only — this is pre-assembly raw
material, so it deliberately skips the painted_accent edge trim that marks a
finished hull/wing part.

Directional readability doesn't apply here (§5.1's leading/trailing-edge rule
is for placed, oriented parts) — raw stock has no facing. Instead the design
goal is to read as visually "flatter"/plainer than a placed structural block
at a glance: a flat base fill with sparse brushed-metal diagonal streaks, no
rivet grid (rivets imply assembly) and no seam lines (seams imply welded
panel joints).
"""
from __future__ import annotations

from PIL import Image

from parts._common import fill_rect, set_pixel

CATEGORY = "block"


def draw(img: Image.Image, palette: dict) -> None:
    steel = palette["dark_steel"]

    w, h = img.size
    x0, y0, x1, y1 = 0, 0, w - 1, h - 1

    # Flat base plate — no frame, no border. Raw stock, not an assembled
    # panel edge.
    fill_rect(img, x0, y0, x1, y1, steel["base"])

    # Brushed-metal grain: a handful of 1px diagonal streaks scattered over
    # the base fill (unevenly spaced, not a repeating grid) so most of the
    # surface stays flat base tone. This reads as rolled sheet stock rather
    # than a riveted structural panel.
    light_diagonals = {-10, -2, 7}
    shadow_diagonals = {-6, 3, 12}
    for x in range(w):
        for y in range(h):
            diag = x - y
            if diag in light_diagonals:
                set_pixel(img, x, y, steel["light"])
            elif diag in shadow_diagonals:
                set_pixel(img, x, y, steel["shadow"])
