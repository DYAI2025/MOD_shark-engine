package dev.sharkengine.ship.part;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ShipPartAnalyzer} / {@link ShipStats} — the role-based
 * replacement for the deleted {@code ThrusterRequirements} (B4, REQ-S2).
 *
 * <p>Like {@link VehiclePartRegistryTest}, these run as plain JUnit: no
 * {@code Bootstrap.bootStrap()}, no {@code BuiltInRegistries}, no Minecraft
 * server/client environment — {@link ShipPartAnalyzer#analyze(java.util.Collection)}
 * only ever sees block-id strings, mirroring {@code ThrusterRequirements}'s
 * proven-testable {@code Collection<String>} signature.</p>
 */
@DisplayName("ShipPartAnalyzer / ShipStats Tests")
class ShipPartAnalyzerTest {

    private static final String THRUSTER_ID = "sharkengine:thruster";
    private static final String STEERING_WHEEL_ID = "sharkengine:steering_wheel";
    private static final String BUG_ID = "sharkengine:bug";
    private static final String UNKNOWN_ID = "minecraft:oak_planks";

    // ─── empty / null input ───────────────────────────────────────────────────

    @Test
    @DisplayName("empty block list yields ShipStats.EMPTY")
    void emptyListYieldsEmptyStats() {
        assertEquals(ShipStats.EMPTY, ShipPartAnalyzer.analyze(Collections.emptyList()));
    }

    @Test
    @DisplayName("null block list yields ShipStats.EMPTY (defensive)")
    void nullListYieldsEmptyStats() {
        assertEquals(ShipStats.EMPTY, ShipPartAnalyzer.analyze(null));
    }

    @Test
    @DisplayName("ShipStats.EMPTY has no propulsion")
    void emptyStatsHasNoPropulsion() {
        assertFalse(ShipStats.EMPTY.hasPropulsion());
    }

    // ─── single-part aggregation ──────────────────────────────────────────────

    @Test
    @DisplayName("single thruster: mass=2, thrust=20, propulsionCount=1, hasPropulsion=true")
    void singleThrusterAggregates() {
        ShipStats stats = ShipPartAnalyzer.analyze(List.of(THRUSTER_ID));
        assertEquals(2, stats.mass());
        assertEquals(20, stats.thrust());
        assertEquals(0, stats.lift());
        assertEquals(0, stats.drag());
        assertEquals(0, stats.fuelCapacity());
        assertEquals(1, stats.propulsionCount());
        assertTrue(stats.hasPropulsion());
    }

    @Test
    @DisplayName("unknown block resolves as STRUCTURE fallback: mass=1, no propulsion")
    void unknownBlockCountsAsStructureMassOne() {
        ShipStats stats = ShipPartAnalyzer.analyze(List.of(UNKNOWN_ID));
        assertEquals(1, stats.mass());
        assertEquals(0, stats.propulsionCount());
        assertFalse(stats.hasPropulsion());
    }

    @Test
    @DisplayName("steering_wheel contributes mass but no propulsion")
    void steeringWheelHasNoPropulsion() {
        ShipStats stats = ShipPartAnalyzer.analyze(List.of(STEERING_WHEEL_ID));
        assertEquals(2, stats.mass());
        assertEquals(0, stats.propulsionCount());
        assertFalse(stats.hasPropulsion());
    }

    // ─── mixed part set aggregation — deterministic sums ──────────────────────

    @Test
    @DisplayName("mixed set: thruster + steering_wheel + bug + unknown sums deterministically")
    void mixedSetAggregatesDeterministically() {
        List<String> ids = List.of(THRUSTER_ID, STEERING_WHEEL_ID, BUG_ID, UNKNOWN_ID);

        // thruster mass=2 thrust=20, steering_wheel mass=2, bug mass=1, unknown(fallback) mass=1
        ShipStats stats = ShipPartAnalyzer.analyze(ids);
        assertEquals(6, stats.mass());
        assertEquals(20, stats.thrust());
        assertEquals(0, stats.lift());
        assertEquals(0, stats.drag());
        assertEquals(0, stats.fuelCapacity());
        assertEquals(1, stats.propulsionCount());
        assertTrue(stats.hasPropulsion());
    }

    @Test
    @DisplayName("aggregation is order-independent for the same multiset of parts")
    void aggregationIsOrderIndependent() {
        List<String> forward = List.of(THRUSTER_ID, STEERING_WHEEL_ID, BUG_ID, UNKNOWN_ID);
        List<String> reversed = List.of(UNKNOWN_ID, BUG_ID, STEERING_WHEEL_ID, THRUSTER_ID);

        assertEquals(ShipPartAnalyzer.analyze(forward), ShipPartAnalyzer.analyze(reversed));
    }

    @Test
    @DisplayName("multiple thrusters: thrust and propulsionCount both accumulate")
    void multipleThrustersAccumulate() {
        List<String> ids = List.of(THRUSTER_ID, THRUSTER_ID, UNKNOWN_ID);
        ShipStats stats = ShipPartAnalyzer.analyze(ids);
        assertEquals(40, stats.thrust());
        assertEquals(2, stats.propulsionCount());
        assertEquals(5, stats.mass()); // 2 + 2 + 1
    }

    // ─── assembly still requires >=1 PROPULSION part ──────────────────────────

    @Test
    @DisplayName("a part set with zero PROPULSION parts has hasPropulsion=false")
    void noPropulsionPartsMeansNoPropulsion() {
        List<String> ids = List.of(STEERING_WHEEL_ID, BUG_ID, UNKNOWN_ID, UNKNOWN_ID);
        ShipStats stats = ShipPartAnalyzer.analyze(ids);
        assertEquals(0, stats.propulsionCount());
        assertFalse(stats.hasPropulsion());
    }

    @Test
    @DisplayName("exactly one PROPULSION part is enough for hasPropulsion=true")
    void onePropulsionPartIsSufficient() {
        List<String> ids = List.of(STEERING_WHEEL_ID, BUG_ID, THRUSTER_ID);
        ShipStats stats = ShipPartAnalyzer.analyze(ids);
        assertEquals(1, stats.propulsionCount());
        assertTrue(stats.hasPropulsion());
    }
}
