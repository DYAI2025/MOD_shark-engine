package dev.sharkengine.ship.part;

import dev.sharkengine.SharkEngineMod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VehiclePartRegistry}.
 *
 * <p>These tests run as plain JUnit — no {@code Bootstrap.bootStrap()}, no
 * {@code BuiltInRegistries} access, no Minecraft server/client environment. This is
 * deliberate: it proves the registry resolves part definitions from a common
 * (non-client, non-server) code path, per AIR-020's design constraint that
 * {@code ShipEntityRenderer} (client) must be able to resolve definitions from the
 * same registry that server-side assembly logic uses (REQ-S1).</p>
 */
@DisplayName("VehiclePartRegistry Tests")
class VehiclePartRegistryTest {

    private static final String THRUSTER_ID = SharkEngineMod.MOD_ID + ":thruster";
    private static final String STEERING_WHEEL_ID = SharkEngineMod.MOD_ID + ":steering_wheel";
    private static final String BUG_ID = SharkEngineMod.MOD_ID + ":bug";

    // ─── thruster (legacy PROPULSION, DIRECT lift) ──────────────────────────────

    @Test
    @DisplayName("thruster resolves as PROPULSION role")
    void thrusterResolvesAsPropulsion() {
        VehiclePartDefinition def = VehiclePartRegistry.resolve(THRUSTER_ID);
        assertEquals(PartRole.PROPULSION, def.role());
    }

    @Test
    @DisplayName("thruster resolves with liftMode=DIRECT (not via ID comparison — REQ-S1)")
    void thrusterResolvesAsDirectLift() {
        VehiclePartDefinition def = VehiclePartRegistry.resolve(THRUSTER_ID);
        assertEquals(VehiclePartDefinition.LiftMode.DIRECT, def.liftMode());
    }

    @Test
    @DisplayName("thruster has thrust=20, mass=2 per concept §4 balance table")
    void thrusterHasBalanceTableValues() {
        VehiclePartDefinition def = VehiclePartRegistry.resolve(THRUSTER_ID);
        assertEquals(20, def.thrust());
        assertEquals(2, def.mass());
    }

    // ─── steering_wheel (legacy CONTROL) ─────────────────────────────────────────

    @Test
    @DisplayName("steering_wheel resolves as CONTROL role, mass=2")
    void steeringWheelResolves() {
        VehiclePartDefinition def = VehiclePartRegistry.resolve(STEERING_WHEEL_ID);
        assertEquals(PartRole.CONTROL, def.role());
        assertEquals(2, def.mass());
    }

    @Test
    @DisplayName("steering_wheel is not a propulsion/lift source")
    void steeringWheelHasNoLiftOrThrust() {
        VehiclePartDefinition def = VehiclePartRegistry.resolve(STEERING_WHEEL_ID);
        assertEquals(VehiclePartDefinition.LiftMode.NONE, def.liftMode());
        assertEquals(0, def.thrust());
        assertEquals(0, def.lift());
    }

    // ─── bug (recovered BUG-Frontsystem block, CONTROL) ──────────────────────────

    @Test
    @DisplayName("bug resolves as CONTROL role, mass=1 (AIR-031 requires a definition for every ModBlocks entry)")
    void bugResolves() {
        VehiclePartDefinition def = VehiclePartRegistry.resolve(BUG_ID);
        assertEquals(PartRole.CONTROL, def.role());
        assertEquals(1, def.mass());
        assertEquals(VehiclePartDefinition.LiftMode.NONE, def.liftMode());
    }

    // ─── fallback for unknown blocks ──────────────────────────────────────────────

    @Test
    @DisplayName("unknown block id resolves to STRUCTURE fallback, mass=1")
    void unknownBlockResolvesToFallback() {
        VehiclePartDefinition def = VehiclePartRegistry.resolve("minecraft:oak_planks");
        assertEquals(PartRole.STRUCTURE, def.role());
        assertEquals(1, def.mass());
        assertEquals(VehiclePartDefinition.LiftMode.NONE, def.liftMode());
    }

    @Test
    @DisplayName("null block id resolves to STRUCTURE fallback (defensive)")
    void nullBlockIdResolvesToFallback() {
        VehiclePartDefinition def = VehiclePartRegistry.resolve(null);
        assertEquals(PartRole.STRUCTURE, def.role());
        assertEquals(1, def.mass());
    }

    @Test
    @DisplayName("fallback definition is a stable singleton, reusable across unknown ids")
    void fallbackIsSharedAcrossUnknownIds() {
        VehiclePartDefinition a = VehiclePartRegistry.resolve("minecraft:stone");
        VehiclePartDefinition b = VehiclePartRegistry.resolve("some_other_mod:mystery_block");
        assertEquals(a, b);
        assertEquals(VehiclePartRegistry.FALLBACK, a);
    }

    // ─── every registered id → exactly one definition ────────────────────────────

    @Test
    @DisplayName("every registered block id resolves to exactly one stable, non-fallback definition")
    void registeredIdsResolveConsistentlyAndAreNotFallback() {
        for (String id : new String[] {THRUSTER_ID, STEERING_WHEEL_ID, BUG_ID}) {
            VehiclePartDefinition first = VehiclePartRegistry.resolve(id);
            VehiclePartDefinition second = VehiclePartRegistry.resolve(id);
            assertEquals(first, second, "resolving the same id twice must yield an equal definition: " + id);
            assertNotEquals(VehiclePartRegistry.FALLBACK, first,
                    "registered id must not silently resolve to the fallback definition: " + id);
        }
    }
}
