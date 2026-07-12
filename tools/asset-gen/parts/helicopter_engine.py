"""helicopter_engine — the finished, placeable rotor-drive engine block
(PROPULSION, liftMode ROTOR: drives a rotor topology, never lifts on its
own — AIRCRAFT_CONCEPT_V2.md §4, §6). Denser/more detailed than the
`engine_core` crafting intermediate it's built from (recipe `M / E / S`,
metal_sheet over engine_core over rotor_shaft — §4), since this is the
final assembled unit players place in a ship.

Material assignment (palette.json): mechanism = copper_brass base +
dark_steel housing/vents.

Directional readability: top edge (intake, "leading") is the light tone;
bottom edge (exhaust vent, "trailing") is the dark tone — see
AIRCRAFT_CONCEPT_V2.md §5.1.
"""
from __future__ import annotations

from PIL import Image

from parts._common import draw_rivet_grid, draw_seam, fill_rect, set_pixel

CATEGORY = "block"


def draw(img: Image.Image, palette: dict) -> None:
    copper = palette["copper_brass"]
    steel = palette["dark_steel"]

    w, h = img.size
    x0, y0, x1, y1 = 0, 0, w - 1, h - 1

    # Base plate — copper_brass casing.
    fill_rect(img, x0, y0, x1, y1, copper["base"])

    # Structural frame — dark_steel housing edge.
    draw_seam(img, x0, y0, x1, y0, steel["shadow"])
    draw_seam(img, x0, y1, x1, y1, steel["shadow"])
    draw_seam(img, x0, y0, x0, y1, steel["shadow"])
    draw_seam(img, x1, y0, x1, y1, steel["shadow"])

    # Full rivet grid — this is a finished assembled part, not an
    # intermediate, so it carries the complete fastener pattern.
    draw_rivet_grid(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, steel["deep"])

    # Central engine chamber — the dark_steel housing block bolted onto
    # the copper_brass casing, upper half of the texture.
    cx0, cy0, cx1, cy1 = 4, 2, 11, 9
    fill_rect(img, cx0, cy0, cx1, cy1, steel["base"])
    draw_seam(img, cx0, cy0, cx1, cy0, steel["seam"])
    draw_seam(img, cx0, cy1, cx1, cy1, steel["seam"])
    draw_seam(img, cx0, cy0, cx0, cy1, steel["seam"])
    draw_seam(img, cx1, cy0, cx1, cy1, steel["seam"])

    # Inner bore/piston core, centered in the chamber.
    fill_rect(img, 6, 4, 9, 7, steel["deep"])
    set_pixel(img, 6, 4, copper["light"])  # glint highlight

    # Exhaust vent — a darker grille near the bottom edge, giving this
    # part directional readability (intake above, exhaust below).
    ex0, ey0, ex1, ey1 = 5, 12, 10, 14
    fill_rect(img, ex0, ey0, ex1, ey1, steel["deep"])
    draw_seam(img, ex0, ey0, ex1, ey0, steel["seam"])
    draw_seam(img, ex0, ey1, ex1, ey1, steel["seam"])
    draw_seam(img, ex0, ey0, ex0, ey1, steel["seam"])
    draw_seam(img, ex1, ey0, ex1, ey1, steel["seam"])
    for gx in (6, 8, 10):
        set_pixel(img, gx, ey0 + 1, steel["seam"])

    # Leading/trailing accent stripes, 1px inset from the outer frame —
    # intake (top) light, exhaust (bottom) dark.
    draw_seam(img, x0 + 1, y0 + 1, x1 - 1, y0 + 1, copper["light"])
    draw_seam(img, x0 + 1, y1 - 1, x1 - 1, y1 - 1, copper["deep"])
