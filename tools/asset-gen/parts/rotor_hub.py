"""rotor_hub — compact, centered mechanism block that rotor blade chains
bolt onto. Material assignment (palette.json): mechanism = copper_brass +
dark_steel (AIRCRAFT_CONCEPT_V2.md §5.1; "rotor_hub kompakt zentriert",
§5.3).

Rotor topology (§6): a hub takes exactly 2 opposing or 4 cardinal blade
chains, so — unlike bearing_assembly's plain concentric ring — this texture
adds a copper_brass mounting cross that reaches all four edges, marking the
cardinal bolt-on points a blade chain attaches to. The hub disc centered on
top of that cross inverts bearing_assembly's color emphasis: a bright
copper_brass core surrounded by concentric dark_steel housing rings, rather
than a copper outer ring around a dark_steel raceway, so the two mechanism
parts stay visually distinct at a glance.
"""
from __future__ import annotations

import math

from PIL import Image

from parts._common import draw_rivet_grid, draw_seam, fill_rect, set_pixel

CATEGORY = "block"

# Radius bands (outer to inner), measured from the canvas center, for the
# centered bearing disc that sits on top of the mounting cross.
_R_DISC_OUTER = 4.6  # beyond this: cross bracket / mounting plate shows
_R_HOUSING_LIGHT_IN = 3.8  # dark_steel light (outer bevel highlight) down to here
_R_HOUSING_BASE_IN = 3.0  # dark_steel base (main housing body) down to here
_R_HOUSING_SHADOW_IN = 2.3  # dark_steel shadow (inner bevel / groove) down to here
_R_COLLAR_IN = 1.4  # copper_brass base (bearing collar) down to here
# r <= _R_COLLAR_IN: copper_brass light — the bright center bolt.


def draw(img: Image.Image, palette: dict) -> None:
    steel = palette["dark_steel"]
    copper = palette["copper_brass"]

    w, h = img.size
    x0, y0, x1, y1 = 0, 0, w - 1, h - 1
    cx, cy = (w - 1) / 2, (h - 1) / 2

    # Mounting plate background with the standard rivet cadence; only the
    # rivets outside both the cross bracket and the disc radius survive,
    # reading as the flange's outer mounting bolts (same trick as
    # bearing_assembly's corner rivets).
    fill_rect(img, x0, y0, x1, y1, steel["shadow"])
    draw_rivet_grid(img, x0, y0, x1, y1, steel["deep"])

    # Copper mounting cross — the 4 cardinal bolt-on points a rotor_blade
    # chain attaches to (2 opposing or 4 cardinal, §6), reaching edge to
    # edge so it reads as continuing into the neighboring blocks.
    fill_rect(img, x0, 7, x1, 8, copper["base"])
    fill_rect(img, 7, y0, 8, y1, copper["base"])
    draw_seam(img, x0, 7, x1, 7, copper["light"])
    draw_seam(img, x0, 8, x1, 8, copper["shadow"])
    draw_seam(img, 7, y0, 7, y1, copper["light"])
    draw_seam(img, 8, y0, 8, y1, copper["shadow"])

    # Flange bolts at the very tip of each cardinal arm — the visible
    # attach point where the next block in a blade chain bolts on.
    for x, y in ((x0, 7), (x0, 8), (x1, 7), (x1, 8), (7, y0), (8, y0), (7, y1), (8, y1)):
        set_pixel(img, x, y, steel["deep"])

    # Centered bearing disc, layered on top of the cross — concentric
    # dark_steel housing rings around a bright copper_brass core, radially
    # symmetric so the hub reads the same from any of its mounting sides.
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            r = math.hypot(x - cx, y - cy)
            if r > _R_DISC_OUTER:
                continue
            elif r > _R_HOUSING_LIGHT_IN:
                set_pixel(img, x, y, steel["light"])
            elif r > _R_HOUSING_BASE_IN:
                set_pixel(img, x, y, steel["base"])
            elif r > _R_HOUSING_SHADOW_IN:
                set_pixel(img, x, y, steel["shadow"])
            elif r > _R_COLLAR_IN:
                set_pixel(img, x, y, copper["base"])
            else:
                set_pixel(img, x, y, copper["light"])
