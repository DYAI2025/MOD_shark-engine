package dev.sharkengine.ship;

/**
 * REQ-007/AC-007 (T08): pure, server-side-only cockpit visibility check.
 *
 * <p><b>OQ-003 (resolved 2026-07-18):</b> the cockpit visibility metric is
 * EYE-HEIGHT-ONLY, deliberately ignoring armor, skin, and third-person camera entirely — chosen
 * as the simplest correct implementation, explicitly NOT a per-armor/per-skin bounding-box
 * special case. This class is how that decision is enforced structurally rather than by
 * convention: {@link #isFullyExposedAboveHull(double, double, double)} takes exactly three plain
 * {@code double} parameters (a seat's Y-offset, a player's eye height, and the tallest adjacent
 * hull block's top-face Y). There is no {@code Player}, {@code LivingEntity}, {@code ItemStack},
 * armor-value, or skin-id parameter anywhere in this signature — not "ignored", but structurally
 * absent, so no caller could smuggle one through even by mistake. {@code CockpitVisibilityTest}
 * proves this both behaviorally (two simulated players with different armor/skin at the same eye
 * height get identical results) and by reflection (every public method here takes only
 * primitive doubles).</p>
 *
 * <p>Any logic that turns a real {@code ShipBlueprint} (its {@code SeatAnchor} and surrounding
 * {@code ShipBlock}s) into the three doubles this method needs belongs in the caller
 * ({@code ShipEntity}), not here — keeping this class free of every Minecraft/Fabric type is
 * what lets it run as a plain JUnit test with zero game bootstrap, matching the pattern already
 * established by {@code dev.sharkengine.ship.part.VehiclePartRegistry}/{@code ShipPartAnalyzer}.</p>
 */
public final class CockpitVisibility {

    /**
     * Minimum vertical clearance, in blocks, the eye-height point must sit below the tallest
     * adjacent hull block's top face to count as concealed rather than exposed. A tuning
     * constant only — it does not vary with, and is not derived from, any player-specific data.
     */
    public static final double VISIBILITY_MARGIN = 0.1;

    /**
     * Standard/default player eye height (vanilla standing pose), in blocks above the feet.
     * REQ-007/AC-007 (T08 remediation): the ONLY eye-height value {@code
     * ShipAssemblyService#scanStructure} uses for its assembly-time cockpit-visibility gate --
     * deliberately a fixed constant, not any specific player's live {@code getEyeHeight()}, so
     * whether a given structure can assemble never depends on who is attempting the assembly or
     * their momentary pose (e.g. crouching) at that instant. Matches the same literal already
     * used throughout {@code CockpitVisibilityTest} and the mount-time check's typical value
     * (vanilla {@code Player#getEyeHeight()} for the standing pose).
     */
    public static final double STANDARD_PLAYER_EYE_HEIGHT = 1.62;

    private CockpitVisibility() {
    }

    /**
     * Whether a player seated with feet at {@code seatAnchorY} is fully exposed above the ship's
     * hull — i.e. their eye point ({@code seatAnchorY + eyeHeight}) sits at or above
     * {@code tallestAdjacentHullTopY - VISIBILITY_MARGIN}, meaning no part of the surrounding
     * hull structure conceals them (AC-007: "die vollständige Figur [steht] nicht dauerhaft
     * vollständig auf dem Fahrzeug").
     *
     * @param seatAnchorY the seat's Y-offset ({@code SeatAnchor.dy()}, or the equivalent resolved
     *                    world Y) — the seated player's feet position, not a block center
     * @param eyeHeight the player's eye height above their feet. A plain {@code double} so
     *                  callers may pass a real {@code Entity#getEyeHeight()} value, a
     *                  pose-adjusted value, or a fixed constant — this method has no opinion on
     *                  the source, and in particular never receives armor or skin data, only a
     *                  number (OQ-003)
     * @param tallestAdjacentHullTopY the Y of the top face of the tallest block adjacent to the
     *                                seat (one Y level above that block's own Y-offset)
     * @return {@code true} if the eye-height point is fully exposed above the hull (non-compliant
     *         with AC-007); {@code false} if it sits below the tallest adjacent hull block's top
     *         face by at least {@link #VISIBILITY_MARGIN}
     */
    public static boolean isFullyExposedAboveHull(double seatAnchorY, double eyeHeight, double tallestAdjacentHullTopY) {
        double eyeWorldY = seatAnchorY + eyeHeight;
        return eyeWorldY >= tallestAdjacentHullTopY - VISIBILITY_MARGIN;
    }
}
