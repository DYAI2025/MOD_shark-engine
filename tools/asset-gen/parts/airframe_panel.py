"""airframe_panel — flat hull/wing skin panel, the most common structural
block in the aircraft extension. Material assignment (palette.json):
hull_wing = dark_steel base + painted_accent edge trim.

Directional readability: left edge (leading, in-flight forward direction
for a hull panel placed facing the BUG) is the light painted_accent tone;
right edge (trailing) is the dark tone — see AIRCRAFT_CONCEPT_V2.md §5.1.
"""
from __future__ import annotations

from PIL import Image

from parts._common import draw_rivet_grid, draw_seam, fill_rect

CATEGORY = "block"


def draw(img: Image.Image, palette: dict) -> None:
    steel = palette["dark_steel"]
    accent = palette["painted_accent"]

    w, h = img.size
    x0, y0, x1, y1 = 0, 0, w - 1, h - 1

    # Base plate.
    fill_rect(img, x0, y0, x1, y1, steel["base"])

    # Structural frame — a 1px shadow border reads as the panel's riveted
    # edge against neighboring panels.
    draw_seam(img, x0, y0, x1, y0, steel["shadow"])
    draw_seam(img, x0, y1, x1, y1, steel["shadow"])
    draw_seam(img, x0, y0, x0, y1, steel["shadow"])
    draw_seam(img, x1, y0, x1, y1, steel["shadow"])

    # Horizontal center seam — two panel halves welded together.
    mid = h // 2
    draw_seam(img, x0 + 1, mid, x1 - 1, mid, steel["seam"])

    # Rivet grid across the interior, inset from the frame.
    draw_rivet_grid(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, steel["deep"])

    # Leading/trailing edge accent stripes (painted_accent), 1px inset from
    # the outer frame so the steel border still reads as the panel edge.
    draw_seam(img, x0 + 1, y0 + 1, x0 + 1, y1 - 1, accent["light"])
    draw_seam(img, x1 - 1, y0 + 1, x1 - 1, y1 - 1, accent["shadow"])
