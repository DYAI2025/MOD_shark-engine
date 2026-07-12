"""engine_core — the raw powerplant block before final assembly into
helicopter_engine. Material assignment (palette.json): mechanism =
copper_brass base + dark_steel accents.

Directional readability: top edge (intake side, root of the assembly) is
the light copper_brass tone; bottom edge (exhaust side, trailing) is the
dark tone — see AIRCRAFT_CONCEPT_V2.md §5.1. This is a dense mechanical
part rather than a broad structural panel, so it uses a compact
piston-chamber silhouette instead of a full rivet grid: a copper_brass
casing with a recessed dark_steel chamber and four corner bolt-heads.
"""
from __future__ import annotations

from PIL import Image

from parts._common import draw_seam, fill_rect, set_pixel, shade_leading_trailing

CATEGORY = "block"


def draw(img: Image.Image, palette: dict) -> None:
    copper = palette["copper_brass"]
    steel = palette["dark_steel"]

    w, h = img.size
    x0, y0, x1, y1 = 0, 0, w - 1, h - 1

    # Casing — the copper_brass housing that wraps the whole core.
    fill_rect(img, x0, y0, x1, y1, copper["base"])

    # Casing frame — shadow border reads as the seam against neighboring
    # engine parts.
    draw_seam(img, x0, y0, x1, y0, copper["shadow"])
    draw_seam(img, x0, y1, x1, y1, copper["shadow"])
    draw_seam(img, x0, y0, x0, y1, copper["shadow"])
    draw_seam(img, x1, y0, x1, y1, copper["shadow"])

    # Recessed piston chamber — a dark_steel rectangle inset in the middle
    # of the casing, the core mechanism the casing is built around.
    cx0, cy0, cx1, cy1 = x0 + 4, y0 + 4, x1 - 4, y1 - 4
    fill_rect(img, cx0, cy0, cx1, cy1, steel["base"])
    draw_seam(img, cx0, cy0, cx1, cy0, steel["light"])
    draw_seam(img, cx0, cy1, cx1, cy1, steel["deep"])
    draw_seam(img, cx0, cy0, cx0, cy1, steel["light"])
    draw_seam(img, cx1, cy0, cx1, cy1, steel["deep"])

    # Chamber core seam — the piston bore running through the middle.
    mid = (cy0 + cy1) // 2
    draw_seam(img, cx0 + 1, mid, cx1 - 1, mid, steel["seam"])

    # Four corner bolt-heads on the casing — deliberately sparse (not a
    # full draw_rivet_grid) to keep this dense mechanical part readable.
    bolt_positions = (
        (x0 + 1, y0 + 1),
        (x1 - 1, y0 + 1),
        (x0 + 1, y1 - 1),
        (x1 - 1, y1 - 1),
    )
    for bx, by in bolt_positions:
        set_pixel(img, bx, by, copper["deep"])

    # Directional readability: intake (top) light, exhaust (bottom) dark.
    shade_leading_trailing(img, x0, y0, x1, y1, copper["light"], copper["deep"], vertical=True)
