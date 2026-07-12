package dev.sharkengine.ship.part;

import java.util.Collection;

/**
 * Aggregates a ship structure's resolved {@link VehiclePartDefinition} kennwerte into a
 * single {@link ShipStats} (REQ-S2) — the role-based replacement for the now-deleted
 * {@code ThrusterRequirements} (B4).
 *
 * <p>Takes the same {@code Collection<String>} block-id shape
 * {@code ThrusterRequirements.countThrusters}/{@code hasThruster} used, deliberately:
 * {@code ShipAssemblyService.scanStructure} already builds that list while walking the
 * structure, and keeping the signature id-based (never {@code BlockState}/
 * {@code ShipBlueprint}) means this class carries no Minecraft registry/NBT dependency —
 * it resolves and runs in a plain unit test, same as {@link VehiclePartRegistry}
 * (see that class's javadoc for why that matters for the client-side renderer too).</p>
 *
 * <p>Aggregation is pure summation over {@link VehiclePartRegistry#resolve(String)}
 * results, so it is deterministic and order-independent for any multiset of block ids.
 * Unknown ids resolve to the STRUCTURE fallback (mass 1) rather than being skipped —
 * every scanned block always contributes to the total.</p>
 */
public final class ShipPartAnalyzer {

    private ShipPartAnalyzer() {}

    /**
     * Resolves every block id via {@link VehiclePartRegistry} and sums mass, lift,
     * thrust, drag, and fuel capacity; also counts how many resolved parts have role
     * {@link PartRole#PROPULSION}.
     *
     * @param blockIds block ids (namespace:path form) for every block in the structure;
     *                 {@code null} or empty yields {@link ShipStats#EMPTY}
     */
    public static ShipStats analyze(Collection<String> blockIds) {
        if (blockIds == null || blockIds.isEmpty()) {
            return ShipStats.EMPTY;
        }

        int mass = 0;
        int lift = 0;
        int thrust = 0;
        int drag = 0;
        int fuelCapacity = 0;
        int propulsionCount = 0;

        for (String id : blockIds) {
            VehiclePartDefinition def = VehiclePartRegistry.resolve(id);
            mass += def.mass();
            lift += def.lift();
            thrust += def.thrust();
            drag += def.drag();
            fuelCapacity += def.fuelCapacity();
            if (def.role() == PartRole.PROPULSION) {
                propulsionCount++;
            }
        }

        return new ShipStats(mass, lift, thrust, drag, fuelCapacity, propulsionCount);
    }
}
