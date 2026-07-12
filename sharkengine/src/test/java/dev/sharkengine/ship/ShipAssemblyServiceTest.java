package dev.sharkengine.ship;

import dev.sharkengine.ship.part.ShipStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@code StructureScan}/BUG validation logic in {@link ShipAssemblyService}.
 *
 * <p>AIR-021: block-ID-based thruster counting ({@code ThrusterRequirements}, deleted)
 * has been replaced by role-based {@link ShipStats} aggregation
 * ({@code dev.sharkengine.ship.part.ShipPartAnalyzer}) — see
 * {@code dev.sharkengine.ship.part.ShipPartAnalyzerTest} for aggregation-specific
 * coverage. This file now only ports {@code StructureScan} construction to the new
 * {@code ShipStats}-carrying record shape and keeps the BUG/assembly-condition tests
 * that were always independent of the thruster-counting mechanism.</p>
 */
@DisplayName("ShipAssembly / StructureScan / BUG Tests")
class ShipAssemblyServiceTest {

    // ─── StructureScan.canAssemble (with BUG) ────────────────────────────────

    /** Helper to build a valid scan with BUG defaults */
    private static ShipAssemblyService.StructureScan makeScan(
            List<ShipBlueprint.ShipBlock> blocks,
            List<net.minecraft.core.BlockPos> invalidAttachments,
            int contactPoints, int propulsionCount, int coreNeighbors,
            int bugCount, boolean bugOnEdge) {
        ShipStats stats = new ShipStats(0, 0, 0, 0, 0, propulsionCount);
        return new ShipAssemblyService.StructureScan(
                null, blocks, invalidAttachments, contactPoints,
                stats, coreNeighbors,
                bugCount, bugOnEdge, 0.0f
        );
    }

    @Test
    @DisplayName("canAssemble: true when all constraints met including BUG")
    void structureScan_canAssemble_whenValid() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 4, 1, true);
        assertTrue(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when no PROPULSION part (role-based, AIR-021)")
    void structureScan_cannotAssemble_noPropulsion() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 0, 4, 1, true);
        assertFalse(scan.canAssemble());
        assertFalse(scan.hasThruster());
        assertEquals(0, scan.thrusterCount());
    }

    @Test
    @DisplayName("canAssemble: false when terrain contact exists")
    void structureScan_cannotAssemble_terrainContact() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                1, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when invalid attachments present")
    void structureScan_cannotAssemble_invalidAttachments() {
        var invalidPos = new net.minecraft.core.BlockPos(0, 0, 0);
        var scan = makeScan(List.of(fakeBlock()), List.of(invalidPos),
                0, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when fewer than 4 core neighbours")
    void structureScan_cannotAssemble_insufficientCoreNeighbours() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 3, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when block list is empty")
    void structureScan_cannotAssemble_emptyBlocks() {
        var scan = makeScan(Collections.emptyList(), Collections.emptyList(),
                0, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    // ─── BUG-specific validation ─────────────────────────────────────────────

    @Test
    @DisplayName("canAssemble: false when no BUG block (bugCount=0)")
    void structureScan_cannotAssemble_noBug() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 4, 0, false);
        assertFalse(scan.canAssemble(), "Ship without BUG must not be assemblable");
    }

    @Test
    @DisplayName("canAssemble: false when multiple BUG blocks (bugCount=2)")
    void structureScan_cannotAssemble_multipleBugs() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 4, 2, true);
        assertFalse(scan.canAssemble(), "Ship with multiple BUGs must not be assemblable");
    }

    @Test
    @DisplayName("canAssemble: false when BUG is inside (not on edge)")
    void structureScan_cannotAssemble_bugInside() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 4, 1, false);
        assertFalse(scan.canAssemble(), "BUG inside ship must block assembly");
    }

    @Test
    @DisplayName("hasBug: returns true only when exactly 1 BUG")
    void structureScan_hasBug() {
        assertTrue(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 4, 1, true).hasBug());
        assertFalse(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 4, 0, false).hasBug());
        assertFalse(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 4, 2, true).hasBug());
    }

    @Test
    @DisplayName("blockCount: matches block list size")
    void structureScan_blockCount_matchesList() {
        var blocks = List.of(fakeBlock(), fakeBlock(), fakeBlock());
        var scan = makeScan(blocks, Collections.emptyList(), 0, 1, 4, 1, true);
        assertEquals(3, scan.blockCount());
    }

    @Test
    @DisplayName("hasThruster/thrusterCount delegate to ShipStats.hasPropulsion/propulsionCount (AIR-021)")
    void structureScan_thrusterAccessorsDelegateToStats() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 3, 4, 1, true);
        assertTrue(scan.hasThruster());
        assertEquals(3, scan.thrusterCount());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static ShipBlueprint.ShipBlock fakeBlock() {
        return new ShipBlueprint.ShipBlock(0, 0, 0, null);
    }
}
