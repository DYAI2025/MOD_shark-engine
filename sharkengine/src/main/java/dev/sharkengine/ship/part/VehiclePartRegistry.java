package dev.sharkengine.ship.part;

import dev.sharkengine.SharkEngineMod;

import java.util.Map;

/**
 * Block-id → {@link VehiclePartDefinition} registry (REQ-S1).
 *
 * <p>This replaces the hardcoded ID comparisons in the now-removed
 * {@code ThrusterRequirements} (B4): assembly, physics, and rendering resolve a
 * part's role and kennwerte here instead of string-comparing block IDs.</p>
 *
 * <p><b>Common entrypoint, both sides.</b> Definitions are populated in this class's
 * static initializer, which runs on first reference from either the server-side
 * assembly/physics code ({@code src/main/java}) or the client-side renderer
 * ({@code src/client/java}, which compiles against {@code src/main/java} under Loom's
 * split source sets). Do not move registration into a server- or client-only
 * entrypoint: {@code ShipEntityRenderer} (client-only) must resolve {@code
 * ROTOR_BLADE} parts from this exact same registry to render rotor animation
 * (AIR-051), so a server-only registration path would make the renderer's lookups
 * fail. This class has no dependency on any Fabric bootstrap/registry — it is
 * resolvable in a plain unit test.</p>
 */
public final class VehiclePartRegistry {

    /** Fallback for any block not explicitly registered: a plain structural block. */
    public static final VehiclePartDefinition FALLBACK = new VehiclePartDefinition(
            PartRole.STRUCTURE, 1, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE
    );

    private static final Map<String, VehiclePartDefinition> DEFINITIONS = Map.of(
            SharkEngineMod.MOD_ID + ":thruster", new VehiclePartDefinition(
                    PartRole.PROPULSION, 2, 0, 20, 0, 0, VehiclePartDefinition.LiftMode.DIRECT
            ),
            SharkEngineMod.MOD_ID + ":steering_wheel", new VehiclePartDefinition(
                    PartRole.CONTROL, 2, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE
            ),
            // "bug" (Schiffsbug): recovered BUG-Frontsystem block (March 2026 work,
            // restored after the main force-push incident) — required per AIR-031's
            // resource contract, which demands a VehiclePartDefinition for every
            // registered ModBlocks entry.
            SharkEngineMod.MOD_ID + ":bug", new VehiclePartDefinition(
                    PartRole.CONTROL, 1, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE
            )
    );

    private VehiclePartRegistry() {}

    /**
     * No-op call target for the common mod entrypoint ({@code SharkEngineMod.init()}).
     * Definitions are already populated by this class's static initializer; this
     * method exists to make registry initialization an explicit, discoverable step
     * in the same style as {@code ModBlocks.init()}/{@code ModEntities.init()}, and
     * to guarantee the class (and therefore its static initializer) has run before
     * gameplay code starts resolving parts.
     */
    public static void init() {
        SharkEngineMod.LOGGER.info("VehiclePartRegistry initialized with {} part definitions", DEFINITIONS.size());
    }

    /**
     * Resolves a vehicle part definition by block id (namespace:path form, e.g.
     * {@code "sharkengine:thruster"}). Unknown or {@code null} ids resolve to
     * {@link #FALLBACK} (generic STRUCTURE block, mass 1).
     */
    public static VehiclePartDefinition resolve(String blockId) {
        if (blockId == null) {
            return FALLBACK;
        }
        return DEFINITIONS.getOrDefault(blockId, FALLBACK);
    }
}
