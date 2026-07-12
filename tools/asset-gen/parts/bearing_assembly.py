"""bearing_assembly — mechanism ring component for rotor hub / drive-shaft
junctions. Material assignment (palette.json): mechanism = copper_brass
base + dark_steel raceway/shaft-hole (AIRCRAFT_CONCEPT_V2.md §5.1; see also
§6 rotor topology, which mounts hub/shaft mechanism parts of this kind).

Not a flat panel — a concentric ring built from distance-from-center bands:
an outer copper_brass housing ring (base tone, bevelled light-out/shadow-in),
a dark_steel/seam raceway groove where the rollers run, a flat dark_steel
hub plate visible through the open center, and a small dark_steel shaft
bore at the very center. The four square corners are the same dark_steel
mounting-plate tone as the hub, with a single surviving rivet at each
outer corner (`draw_rivet_grid` positions that fall outside the ring
radius) reading as the flange's mounting bolts.
"""
from __future__ import annotations

import math

from PIL import Image

from parts._common import draw_rivet_grid, fill_rect, set_pixel

CATEGORY = "block"

# Radius bands (outer to inner), measured from the canvas center. Chosen so
# corner rivets from draw_rivet_grid's default 4px/offset-2 grid survive
# only at the four true corners, reading as flange mounting bolts.
_R_RING_OUTER = 7.3   # beyond this: background mounting plate
_R_RING_LIGHT_IN = 6.5   # copper light (outer bevel highlight) down to here
_R_RING_BASE_IN = 5.8   # copper base (main outer ring body) down to here
_R_RING_SHADOW_IN = 5.2   # copper shadow (inner bevel) down to here
_R_RACEWAY_BASE_IN = 4.6   # dark_steel base (raceway outer edge) down to here
_R_RACEWAY_SEAM_IN = 3.8   # dark_steel seam (raceway groove) down to here
_R_HUB_IN = 1.6   # dark_steel shadow (flat hub plate) down to here
# r <= _R_HUB_IN: dark_steel deep — the center shaft bore.


def draw(img: Image.Image, palette: dict) -> None:
    steel = palette["dark_steel"]
    copper = palette["copper_brass"]

    w, h = img.size
    cx, cy = (w - 1) / 2, (h - 1) / 2

    # Mounting plate background, plus corner bolts — only the rivets that
    # fall outside the ring radius (the four square corners) survive the
    # ring pass below.
    fill_rect(img, 0, 0, w - 1, h - 1, steel["shadow"])
    draw_rivet_grid(img, 0, 0, w - 1, h - 1, steel["deep"])

    for y in range(h):
        for x in range(w):
            dx = x - cx
            dy = y - cy
            r = math.sqrt(dx * dx + dy * dy)

            if r > _R_RING_OUTER:
                continue  # background plate / corner rivet shows through
            elif r > _R_RING_LIGHT_IN:
                set_pixel(img, x, y, copper["light"])
            elif r > _R_RING_BASE_IN:
                set_pixel(img, x, y, copper["base"])
            elif r > _R_RING_SHADOW_IN:
                set_pixel(img, x, y, copper["shadow"])
            elif r > _R_RACEWAY_BASE_IN:
                set_pixel(img, x, y, steel["base"])
            elif r > _R_RACEWAY_SEAM_IN:
                set_pixel(img, x, y, steel["seam"])
            elif r > _R_HUB_IN:
                set_pixel(img, x, y, steel["shadow"])
            else:
                set_pixel(img, x, y, steel["deep"])
