"""fuselage_frame — structural skeleton block that hull/wing panels attach
to. Material assignment (palette.json): hull_wing = dark_steel base +
painted_accent edge trim, same family as airframe_panel but a different
read: airframe_panel is a finished, solid skin; fuselage_frame is the bare
truss underneath it, so the interior stays mostly transparent instead of a
solid fill, with diagonal cross-braces standing in open space.

Directional readability: left edge (leading) is the light painted_accent
tone; right edge (trailing) is the dark tone — see AIRCRAFT_CONCEPT_V2.md
§5.1, the same convention airframe_panel uses.
"""
from __future__ import annotations

from PIL import Image

from parts._common import draw_rivet_grid, draw_seam, fill_rect, set_pixel

CATEGORY = "block"

BAND = 3  # thickness of the solid steel frame band around the open core


def draw(img: Image.Image, palette: dict) -> None:
    steel = palette["dark_steel"]
    accent = palette["painted_accent"]

    w, h = img.size
    x0, y0, x1, y1 = 0, 0, w - 1, h - 1
    inner_lo, inner_hi = BAND, x1 - BAND  # 3..12 on a 16px canvas

    # Solid steel band around the perimeter — this is the material the
    # rest of the airframe bolts onto. Left unfilled inside, unlike
    # airframe_panel's full plate, so the truss reads as an open frame.
    fill_rect(img, x0, y0, x1, y0 + BAND - 1, steel["base"])  # top band
    fill_rect(img, x0, y1 - BAND + 1, x1, y1, steel["base"])  # bottom band
    fill_rect(img, x0, y0, x0 + BAND - 1, y1, steel["base"])  # left band
    fill_rect(img, x1 - BAND + 1, y0, x1, y1, steel["base"])  # right band

    # Diagonal cross-braces spanning the open core — the truss members
    # doing the structural work, standing in transparent space rather
    # than against a filled backdrop.
    draw_seam(img, inner_lo, inner_lo, inner_hi, inner_hi, steel["seam"])
    draw_seam(img, inner_hi, inner_lo, inner_lo, inner_hi, steel["seam"])

    # Gusset rivets anchoring the braces to the frame's inner corners.
    for gx in (inner_lo, inner_hi):
        for gy in (inner_lo, inner_hi):
            set_pixel(img, gx, gy, steel["deep"])

    # Outer silhouette edge — the panel's riveted boundary against
    # neighboring blocks, same convention as airframe_panel's border.
    draw_seam(img, x0, y0, x1, y0, steel["shadow"])
    draw_seam(img, x0, y1, x1, y1, steel["shadow"])
    draw_seam(img, x0, y0, x0, y1, steel["shadow"])
    draw_seam(img, x1, y0, x1, y1, steel["shadow"])

    # Inner rim seam — where the solid band meets the open core, marking
    # the weld line of the frame.
    draw_seam(img, x0, inner_lo, x1, inner_lo, steel["seam"])
    draw_seam(img, x0, inner_hi, x1, inner_hi, steel["seam"])
    draw_seam(img, inner_lo, y0, inner_lo, y1, steel["seam"])
    draw_seam(img, inner_hi, y0, inner_hi, y1, steel["seam"])

    # Rivet line down the middle of the top/bottom bands, kept clear of
    # the left/right bands' accent stripes.
    draw_rivet_grid(img, inner_lo, y0, inner_hi, y0 + BAND - 1, steel["deep"], spacing=4, offset=1)
    draw_rivet_grid(img, inner_lo, y1 - BAND + 1, inner_hi, y1, steel["deep"], spacing=4, offset=1)

    # Leading/trailing edge accent stripes (painted_accent) down the
    # middle of the left/right bands, matching airframe_panel's convention.
    draw_seam(img, x0 + 1, y0 + 1, x0 + 1, y1 - 1, accent["light"])
    draw_seam(img, x1 - 1, y0 + 1, x1 - 1, y1 - 1, accent["shadow"])
