"""rotor_shaft — vertical mechanism drive shaft/axle. Material assignment
(palette.json): mechanism = copper_brass rod + dark_steel collar rings.

Rotor topology (AIRCRAFT_CONCEPT_V2.md §6): the shaft sits in the assembly
chain between a spinning `rotor_hub` above and a stationary
`helicopter_engine` below, so it must read as a driveshaft, not a flat
panel — vertical color bands fake a round cross-section (dark_steel socket
edges framing a copper_brass rod that highlights toward the center column),
and two dark_steel collar rings mark the mounting joints where the shaft
connects to its neighbors.

Directional readability (concept §5.1, "root vs tip"): the top collar uses
lighter dark_steel tones (tip, toward the spinning hub); the bottom collar
uses darker tones (root, toward the grounded engine mount) — so the block
reads as vertically oriented even though the rod gradient itself is
symmetric.
"""
from __future__ import annotations

from PIL import Image

from parts._common import draw_seam, fill_rect, set_pixel, shade_leading_trailing

CATEGORY = "block"


def draw(img: Image.Image, palette: dict) -> None:
    steel = palette["dark_steel"]
    copper = palette["copper_brass"]

    w, h = img.size
    x0, y0, x1, y1 = 0, 0, w - 1, h - 1

    # Cylindrical roundness: a symmetric column gradient, dark_steel socket
    # edges framing a copper_brass rod that brightens toward the center.
    column_tones = [
        steel["deep"], steel["shadow"],
        copper["deep"], copper["shadow"], copper["base"], copper["base"],
        copper["light"], copper["light"], copper["light"], copper["light"],
        copper["base"], copper["base"], copper["shadow"], copper["deep"],
        steel["shadow"], steel["deep"],
    ]
    for x, tone in enumerate(column_tones):
        fill_rect(img, x, y0, x, y1, tone)

    # Socket seam — 1px line where the copper rod meets the dark_steel edge
    # framing, full height, so it reads as a groove the rod sits in.
    draw_seam(img, 2, y0, 2, y1, copper["seam"])
    draw_seam(img, 13, y0, 13, y1, copper["seam"])

    # Top collar (rows 2-4) — connects up to rotor_hub; lighter tones mark
    # it as the "tip" end per the root/tip readability rule.
    fill_rect(img, x0, 2, x1, 4, steel["base"])
    shade_leading_trailing(img, x0, 2, x1, 4, steel["light"], steel["shadow"], vertical=True)

    # Bottom collar (rows 11-13) — connects down to helicopter_engine;
    # darker tones mark it as the grounded "root" end.
    fill_rect(img, x0, 11, x1, 13, steel["shadow"])
    shade_leading_trailing(img, x0, 11, x1, 13, steel["base"], steel["deep"], vertical=True)

    # Bolt rivets on each collar's center row, every 4px offset 2px — the
    # standard fastener cadence (README), placed by hand since both collars
    # need exactly one rivet row rather than a full 2D grid.
    for x in (2, 6, 10, 14):
        set_pixel(img, x, 3, steel["deep"])
        set_pixel(img, x, 12, steel["seam"])
