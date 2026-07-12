package dev.sharkengine.ship.part;

/**
 * Aggregated vehicle kennwerte obtained by summing every part's
 * {@link VehiclePartDefinition} across a ship structure (REQ-S2).
 *
 * <p>Produced by {@link ShipPartAnalyzer#analyze(java.util.Collection)} — a single,
 * role-based aggregation pass that replaces the now-removed
 * {@code ThrusterRequirements} (B4). Aggregation is pure summation, so it is
 * deterministic and order-independent for any multiset of block ids.</p>
 *
 * @param mass            summed mass across all resolved parts (drives {@code WeightCategory}
 *                        once AIR-023 switches it from block count to mass); unknown
 *                        parts resolve via {@link VehiclePartRegistry#FALLBACK}
 *                        (STRUCTURE, mass 1), never silently dropped
 * @param lift            summed lift contribution (rotor blades, wing surfaces)
 * @param thrust          summed thrust contribution (propulsion parts)
 * @param drag            summed drag contribution (fixed-wing surfaces)
 * @param fuelCapacity    summed extra fuel capacity (fuel storage parts)
 * @param propulsionCount number of parts whose {@link VehiclePartDefinition#role()} is
 *                        {@link PartRole#PROPULSION} — the role-based replacement for
 *                        {@code ThrusterRequirements.countThrusters}
 */
public record ShipStats(
        int mass,
        int lift,
        int thrust,
        int drag,
        int fuelCapacity,
        int propulsionCount
) {
    /** Stats for an empty part set (no blocks, or a null/empty input list). */
    public static final ShipStats EMPTY = new ShipStats(0, 0, 0, 0, 0, 0);

    /**
     * Whether this ship has at least one PROPULSION part — the role-based
     * replacement for {@code ThrusterRequirements.hasThruster}. Assembly still
     * requires this to be {@code true} (REQ-S2), just no longer via block-ID
     * comparison.
     */
    public boolean hasPropulsion() {
        return propulsionCount > 0;
    }
}
