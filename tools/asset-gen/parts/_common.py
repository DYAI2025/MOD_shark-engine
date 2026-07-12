"""Shared drawing helpers for part texture scripts (AIR-032).

Not a part itself — generate.py's discover_parts() skips any parts/*.py
whose stem starts with "_", so this module is never treated as a
registered texture. Every helper here takes and returns nothing but plain
(width, height, RGBA) pixel coordinates and hex color strings pulled from
palette.json by the caller — no hardcoded colors live in this file.
"""
from __future__ import annotations

from PIL import Image, ImageDraw


def hex_to_rgba(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    h = hex_color.lstrip("#")
    r, g, b = (int(h[i : i + 2], 16) for i in (0, 2, 4))
    return (r, g, b, alpha)


def fill_rect(img: Image.Image, x0: int, y0: int, x1: int, y1: int, hex_color: str) -> None:
    """Inclusive pixel rectangle [x0,x1] x [y0,y1]."""
    draw = ImageDraw.Draw(img)
    draw.rectangle([x0, y0, x1, y1], fill=hex_to_rgba(hex_color))


def set_pixel(img: Image.Image, x: int, y: int, hex_color: str) -> None:
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), hex_to_rgba(hex_color))


def draw_seam(img: Image.Image, x0: int, y0: int, x1: int, y1: int, hex_color: str) -> None:
    """A 1px-wide seam line — either a single row (y0==y1) or single column (x0==x1)."""
    draw = ImageDraw.Draw(img)
    draw.line([(x0, y0), (x1, y1)], fill=hex_to_rgba(hex_color), width=1)


def draw_rivet_grid(
    img: Image.Image,
    x0: int,
    y0: int,
    x1: int,
    y1: int,
    hex_color: str,
    spacing: int = 4,
    offset: int = 2,
) -> None:
    """Single-pixel rivets on a `spacing`-px grid within [x0,x1] x [y0,y1],
    starting `offset` px in from the top-left corner of the region — the
    concept doc's "rivet grid every 4 px" rule (AIRCRAFT_CONCEPT_V2.md §5.2).
    """
    for y in range(y0 + offset, y1 + 1, spacing):
        for x in range(x0 + offset, x1 + 1, spacing):
            set_pixel(img, x, y, hex_color)


def shade_leading_trailing(
    img: Image.Image,
    x0: int,
    y0: int,
    x1: int,
    y1: int,
    light_hex: str,
    dark_hex: str,
    vertical: bool = False,
) -> None:
    """Directional readability rule (concept §5.1): leading edge light, trailing
    edge dark. `vertical=False` shades left(light)/right(dark) columns; True
    shades top(light)/bottom(dark) rows. Only touches the outermost 1px edge
    so it composes with a base fill drawn first.
    """
    if vertical:
        draw_seam(img, x0, y0, x1, y0, light_hex)
        draw_seam(img, x0, y1, x1, y1, dark_hex)
    else:
        draw_seam(img, x0, y0, x0, y1, light_hex)
        draw_seam(img, x1, y0, x1, y1, dark_hex)
