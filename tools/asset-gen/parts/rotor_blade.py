"""rotor_blade — a single helicopter rotor blade (AIRCRAFT_CONCEPT_V2.md §6,
ROTOR_BLADE part role). Material assignment (palette.json): hull_wing =
dark_steel base + painted_accent trim, same family as airframe_panel since
a blade is aerodynamically a hull/wing surface, not a mechanism part.

Model note (§5.3): "rotor_blade flach (~3 px hoch)" — the block model itself
is a thin flat slab, so the texture reads as a narrow horizontal bar across
the 16x16 canvas rather than a full-face fill; everything outside the blade
stays fully transparent.

Layout: root (mount point, attaches to rotor_hub) at the left edge x=0,
tip at the right edge x=15. The blade is 4px tall at the root and tapers to
3px tall over the outer third, per real rotor-blade chord taper toward the
tip. A seam marks the taper break.

Directional readability (§5.1): leading edge (top row) is the light
dark_steel tone, trailing edge (bottom row) is the dark tone — via
shade_leading_trailing(vertical=True).

Authentic detail: a painted_accent visibility stripe near the tip, the same
hi-viz marking real rotor blades carry so ground crews can see the disc
while it's turning.
"""
from __future__ import annotations

from PIL import Image

from parts._common import draw_seam, fill_rect, set_pixel, shade_leading_trailing

CATEGORY = "block"

# Root segment (mount end): full 4px chord, columns [0, ROOT_END].
ROOT_END = 11
ROOT_TOP, ROOT_BOTTOM = 6, 9

# Tip segment: tapered to 3px chord, columns [TIP_START, 15].
TIP_START = 12
TIP_TOP, TIP_BOTTOM = 6, 8

# Visibility stripe columns within the tip segment.
STRIPE_START, STRIPE_END = 13, 14


def draw(img: Image.Image, palette: dict) -> None:
    steel = palette["dark_steel"]
    accent = palette["painted_accent"]

    w, h = img.size
    x1 = w - 1

    # Blade body — root segment (wider) and tip segment (tapered), leaving
    # the rest of the 16x16 canvas fully transparent per the pre-allocated
    # canvas contract.
    fill_rect(img, 0, ROOT_TOP, ROOT_END, ROOT_BOTTOM, steel["base"])
    fill_rect(img, TIP_START, TIP_TOP, x1, TIP_BOTTOM, steel["base"])

    # Leading edge (top) light, trailing edge (bottom) dark — applied per
    # segment since the chord height changes at the taper break.
    shade_leading_trailing(img, 0, ROOT_TOP, ROOT_END, ROOT_BOTTOM, steel["light"], steel["shadow"], vertical=True)
    shade_leading_trailing(img, TIP_START, TIP_TOP, x1, TIP_BOTTOM, steel["light"], steel["shadow"], vertical=True)

    # Taper-break seam — the structural joint where the chord steps down
    # from 4px to 3px.
    draw_seam(img, TIP_START, TIP_TOP, TIP_START, TIP_BOTTOM, steel["seam"])

    # Root mounting bolts — two rivets marking where the blade bolts to the
    # rotor_hub's bearing assembly.
    set_pixel(img, 1, 7, steel["deep"])
    set_pixel(img, 1, 8, steel["deep"])

    # Tip visibility stripe — bright hi-viz band near the outer tip, spanning
    # the full tapered chord so it reads at a glance while the rotor spins.
    fill_rect(img, STRIPE_START, TIP_TOP, STRIPE_END, TIP_BOTTOM, accent["light"])
