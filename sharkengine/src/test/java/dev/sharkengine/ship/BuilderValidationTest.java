package dev.sharkengine.ship;

import dev.sharkengine.ship.part.ShipPartAnalyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for builder validation logic.
 * Covers thruster counting, assembly condition logic, and edge cases
 * that caused the "phantom invalid block" bug (Bug #2).
 *
 * Root cause of Bug #2: tag file was at data/sharkengine/tags/blocks/
 * (plural) instead of data/sharkengine/tags/block/ (singular).
 * When the tag doesn't load, ALL blocks fail the SHIP_ELIGIBLE check,
 * including the steering wheel itself — resulting in permanent "1 invalid".
 *
 * Note: StructureScan construction requires Minecraft BlockPos which is
 * not on the test classpath. Assembly conditions are validated via
 * their individual boolean logic in {@link AssemblyConditionTests}.
 */
@DisplayName("Builder Validation Tests")
class BuilderValidationTest {

    /**
     * Tests the individual boolean conditions that make up canAssemble().
     * The actual canAssemble() method is:
     * <pre>
     *   return !isEmpty()
     *       && invalidAttachments.isEmpty()
     *       && contactPoints == 0
     *       && hasThruster
     *       && coreNeighbors >= 4;
     * </pre>
     */
    @Nested
    @DisplayName("Assembly Condition Logic")
    class AssemblyConditionTests {

        /** Simulates canAssemble() logic without needing Minecraft types. */
        private boolean canAssemble(int blockCount, int invalidCount,
                                    int contactPoints, boolean hasThruster,
                                    int coreNeighbors) {
            boolean isEmpty = blockCount == 0;
            boolean invalidEmpty = invalidCount == 0;
            return !isEmpty
                    && invalidEmpty
                    && contactPoints == 0
                    && hasThruster
                    && coreNeighbors >= 4;
        }

        @Test
        @DisplayName("valid ship: blocks, no invalids, no contacts, thruster, 4 core neighbors")
        void validShipCanAssemble() {
            assertTrue(canAssemble(10, 0, 0, true, 4),
                    "Ship meeting all requirements should assemble");
        }

        @Test
        @DisplayName("empty scan cannot assemble")
        void emptyScanCannotAssemble() {
            assertFalse(canAssemble(0, 0, 0, false, 0),
                    "Empty scan should not assemble");
        }

        @Test
        @DisplayName("REGRESSION: invalid attachments prevent assembly")
        void invalidAttachmentsPreventAssembly() {
            // This is the scenario from Bug #2: even 1 invalid block blocks assembly
            assertFalse(canAssemble(5, 1, 0, true, 4),
                    "Ship with invalid attachments must not assemble");
        }

        @Test
        @DisplayName("REGRESSION: zero blocks + 1 invalid = cannot assemble")
        void zeroBlocksOneInvalidCannotAssemble() {
            // Bug #2 edge case: steering wheel itself counted as invalid
            // when tag doesn't load, resulting in 0 valid + 1 invalid
            assertFalse(canAssemble(0, 1, 0, false, 0),
                    "Zero valid blocks with invalids must not assemble");
        }

        @Test
        @DisplayName("world contacts prevent assembly")
        void contactsPreventAssembly() {
            assertFalse(canAssemble(10, 0, 3, true, 4),
                    "Ship with world contacts should not assemble");
        }

        @Test
        @DisplayName("missing thruster prevents assembly")
        void noThrusterPreventsAssembly() {
            assertFalse(canAssemble(10, 0, 0, false, 4),
                    "Ship without thruster should not assemble");
        }

        @Test
        @DisplayName("fewer than 4 core neighbors prevents assembly")
        void insufficientCoreNeighborsPreventsAssembly() {
            assertFalse(canAssemble(10, 0, 0, true, 3),
                    "Ship with < 4 core neighbors should not assemble");
        }

        @Test
        @DisplayName("exactly 4 core neighbors allows assembly")
        void exactlyFourCoreNeighborsAllows() {
            assertTrue(canAssemble(10, 0, 0, true, 4),
                    "Ship with exactly 4 core neighbors should assemble");
        }

        @Test
        @DisplayName("more than 4 core neighbors also allows assembly")
        void moreThanFourCoreNeighborsAllows() {
            // technically only 4 horizontal neighbors possible, but test the >= logic
            assertTrue(canAssemble(10, 0, 0, true, 6),
                    "Ship with > 4 core neighbors should assemble");
        }

        @Test
        @DisplayName("all conditions false at once")
        void allConditionsFalse() {
            assertFalse(canAssemble(0, 5, 3, false, 0),
                    "Ship failing all conditions must not assemble");
        }

        @Test
        @DisplayName("only invalid condition fails")
        void onlyInvalidConditionFails() {
            // All good except 1 invalid attachment
            assertFalse(canAssemble(10, 1, 0, true, 4),
                    "Single invalid attachment should block assembly");
        }

        @Test
        @DisplayName("only contact condition fails")
        void onlyContactConditionFails() {
            assertFalse(canAssemble(10, 0, 1, true, 4),
                    "Single world contact should block assembly");
        }
    }

    /**
     * AIR-021: propulsion detection moved from the ID-comparison
     * {@code ThrusterRequirements} (deleted, B4) to role-based
     * {@code ShipPartAnalyzer}/{@code ShipStats} aggregation via
     * {@code VehiclePartRegistry}. These tests port the same scenarios to the
     * new API — "thruster" is now just one PROPULSION-role part among
     * potentially several (e.g. the later helicopter_engine).
     */
    @Nested
    @DisplayName("ShipPartAnalyzer (role-based propulsion detection)")
    class PropulsionDetectionTests {

        @Test
        @DisplayName("propulsionCount counts correctly with mixed blocks")
        void propulsionCountWithMixedBlocks() {
            List<String> ids = List.of(
                    "minecraft:oak_planks",
                    "sharkengine:thruster",
                    "minecraft:glass",
                    "sharkengine:thruster",
                    "sharkengine:steering_wheel"
            );
            assertEquals(2, ShipPartAnalyzer.analyze(ids).propulsionCount(),
                    "Should count exactly 2 PROPULSION parts");
        }

        @Test
        @DisplayName("propulsionCount returns 0 for empty list")
        void propulsionCountEmptyList() {
            assertEquals(0, ShipPartAnalyzer.analyze(List.of()).propulsionCount(),
                    "Empty block list should have 0 propulsion parts");
        }

        @Test
        @DisplayName("propulsionCount handles null safely")
        void propulsionCountNull() {
            assertEquals(0, ShipPartAnalyzer.analyze(null).propulsionCount(),
                    "null block list should return 0 propulsion parts");
        }

        @Test
        @DisplayName("propulsionCount ignores unregistered near-miss IDs (fallback is STRUCTURE, not PROPULSION)")
        void propulsionCountIgnoresPartialMatches() {
            List<String> ids = List.of(
                    "sharkengine:thruster_advanced",
                    "other:thruster",
                    "sharkengine:thruster_base"
            );
            assertEquals(0, ShipPartAnalyzer.analyze(ids).propulsionCount(),
                    "Unregistered near-miss IDs should resolve to the STRUCTURE fallback, not PROPULSION");
        }

        @Test
        @DisplayName("hasPropulsion returns true with at least one PROPULSION part")
        void hasPropulsionWithOne() {
            assertTrue(ShipPartAnalyzer.analyze(
                    List.of("minecraft:stone", "sharkengine:thruster")).hasPropulsion(),
                    "hasPropulsion should return true when a PROPULSION part exists");
        }

        @Test
        @DisplayName("hasPropulsion returns false without any PROPULSION parts")
        void hasPropulsionWithNone() {
            assertFalse(ShipPartAnalyzer.analyze(
                    List.of("minecraft:stone", "minecraft:oak_planks")).hasPropulsion(),
                    "hasPropulsion should return false without any PROPULSION parts");
        }

        @Test
        @DisplayName("propulsionCount with single thruster in large list")
        void propulsionCountOnInLargeList() {
            List<String> ids = List.of(
                    "minecraft:oak_planks", "minecraft:oak_planks",
                    "minecraft:oak_log", "minecraft:oak_log",
                    "minecraft:glass", "minecraft:glass",
                    "sharkengine:thruster",
                    "minecraft:white_wool", "minecraft:white_wool"
            );
            assertEquals(1, ShipPartAnalyzer.analyze(ids).propulsionCount(),
                    "Should find exactly 1 PROPULSION part in a large block list");
        }
    }
}
