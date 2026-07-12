# Shark Engine Asset Generator

Deterministic pixel-art texture pipeline for the aircraft-extension parts
(AIRCRAFT_CONCEPT_V2.md §5, task AIR-032). Textures are **not hand-pixeled**
— every one is produced by a pure Python drawing function so regenerating
is diff-stable and style stays consistent across ~16 parts without manual
upkeep.

## Style: Industrial Kupfer-Stahl

- **16×16 px** per texture, no resolution mixing.
- **Palette-only.** Every color used must come from `palette.json` (+ full
  transparency for alpha). Never hardcode a hex value in a part script —
  if a color is missing, add it to `palette.json` first. This is enforced
  by `ResourceValidationTest` (AIR-032): any pixel using a color outside
  the palette fails CI.
- **3–5 tones per material family** (`light`/`base`/`shadow`/`deep`/`seam`),
  used for rivets, panel seams, and structural highlights — not
  photorealistic micro-detail.
- **Directional readability.** Leading edge light, trailing edge dark; root
  vs. tip of a part should be visually distinguishable at a glance. Use
  `shade_leading_trailing()` from `parts/_common.py` for this.

## Drawing rules

- **Rivet grid:** single-pixel rivets every 4px, inset 2px from the region's
  top-left corner (`draw_rivet_grid()`).
- **Seams:** 1px wide, drawn with the family's `seam` tone (or `deep` where
  no `seam` tone exists, e.g. `painted_accent`).
- **Material assignment** (`palette.json`'s `material_assignment` block):
  - Hull/wing surfaces → `dark_steel` base + `painted_accent` edge trim.
  - Mechanism parts (engine, hub, shaft) → `copper_brass` + `dark_steel`.
  - Skids/frames → `light_alloy`.

## Adding a new part texture

1. Create `parts/<part_id>.py` with a `draw(img: PIL.Image.Image, palette: dict) -> None`
   function. `img` is a pre-allocated 16×16 transparent RGBA canvas — draw
   directly onto it (don't reassign/resize it).
2. Optionally set a module-level `CATEGORY = "item"` if this texture belongs
   under `textures/item/` instead of the default `textures/block/`.
3. Use the helpers in `parts/_common.py` (`fill_rect`, `set_pixel`,
   `draw_seam`, `draw_rivet_grid`, `shade_leading_trailing`) rather than
   reimplementing pixel logic — keeps every part visually consistent.
4. Run `python3 generate.py <part_id>` to render it, then `runClient` to
   eyeball it in-game. The pipeline makes iteration cheap; the human eye is
   still the actual quality gate (concept §5.2).
5. Commit the generated PNG under
   `sharkengine/src/main/resources/assets/sharkengine/textures/{block,item}/`
   alongside the script — generated output is checked in, not built at
   compile time.

## Commands

```bash
python3 generate.py                 # regenerate every registered part
python3 generate.py airframe_panel  # regenerate just this one
python3 generate.py --check         # CI: regenerate to a temp dir, diff
                                     # against committed output, fail on
                                     # any mismatch (diff-stability gate)
```

Requires Python 3.12+ and Pillow (`pip install pillow`).
