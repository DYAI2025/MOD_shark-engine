package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThrusterRequirements, BUG validation, and ShipAssemblyService logic.
 */
@DisplayName("ShipAssembly / ThrusterRequirements / BUG Tests")
class ShipAssemblyServiceTest {

    // ─── hasThruster ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasThruster: detects single thruster block")
    void detectsThrusters() {
        assertTrue(ThrusterRequirements.hasThruster(List.of("sharkengine:thruster")));
    }

    @Test
    @DisplayName("hasThruster: returns false with no thrusters")
    void detectsMissingThrusters() {
        assertFalse(ThrusterRequirements.hasThruster(List.of("minecraft:oak_planks")));
    }

    @Test
    @DisplayName("hasThruster: returns false for empty block list")
    void emptyList_noThruster() {
        assertFalse(ThrusterRequirements.hasThruster(Collections.emptyList()));
    }

    @Test
    @DisplayName("hasThruster: returns false for null input")
    void nullList_noThruster() {
        assertFalse(ThrusterRequirements.hasThruster(null));
    }

    // ─── countThrusters ───────────────────────────────────────────────────────

    @Test
    @DisplayName("countThrusters: counts multiple thruster blocks")
    void countsMultipleThrusters() {
        List<String> blocks = List.of("sharkengine:thruster", "minecraft:oak_planks", "sharkengine:thruster");
        assertEquals(2, ThrusterRequirements.countThrusters(blocks));
    }

    @Test
    @DisplayName("countThrusters: returns 0 for empty list")
    void countThrusters_empty() {
        assertEquals(0, ThrusterRequirements.countThrusters(Collections.emptyList()));
    }

    @Test
    @DisplayName("countThrusters: returns 0 for null list")
    void countThrusters_null() {
        assertEquals(0, ThrusterRequirements.countThrusters(null));
    }

    @Test
    @DisplayName("countThrusters: only exact ID matches")
    void countThrusters_partialIdDoesNotMatch() {
        List<String> blocks = List.of("sharkengine:thruster_mk2", "minecraft:thruster", "SHARKENGINE:THRUSTER");
        assertEquals(0, ThrusterRequirements.countThrusters(blocks));
    }

    // ─── StructureScan.canAssemble (with BUG) ────────────────────────────────

    /** Helper to build a valid scan with BUG defaults */
    private static ShipAssemblyService.StructureScan makeScan(
            List<ShipBlueprint.ShipBlock> blocks,
            List<net.minecraft.core.BlockPos> invalidAttachments,
            int contactPoints, boolean hasThruster, int thrusterCount, int coreNeighbors,
            int bugCount, boolean bugOnEdge) {
        return new ShipAssemblyService.StructureScan(
                null, blocks, invalidAttachments, contactPoints,
                hasThruster, thrusterCount, coreNeighbors,
                bugCount, bugOnEdge, 0.0f
        );
    }

    @Test
    @DisplayName("canAssemble: true when all constraints met including BUG")
    void structureScan_canAssemble_whenValid() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, true, 1, 4, 1, true);
        assertTrue(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when no thruster")
    void structureScan_cannotAssemble_noThruster() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, false, 0, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when terrain contact exists")
    void structureScan_cannotAssemble_terrainContact() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                1, true, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when invalid attachments present")
    void structureScan_cannotAssemble_invalidAttachments() {
        var invalidPos = new net.minecraft.core.BlockPos(0, 0, 0);
        var scan = makeScan(List.of(fakeBlock()), List.of(invalidPos),
                0, true, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when fewer than 4 core neighbours")
    void structureScan_cannotAssemble_insufficientCoreNeighbours() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, true, 1, 3, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when block list is empty")
    void structureScan_cannotAssemble_emptyBlocks() {
        var scan = makeScan(Collections.emptyList(), Collections.emptyList(),
                0, true, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    // ─── BUG-specific validation ─────────────────────────────────────────────

    @Test
    @DisplayName("canAssemble: false when no BUG block (bugCount=0)")
    void structureScan_cannotAssemble_noBug() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, true, 1, 4, 0, false);
        assertFalse(scan.canAssemble(), "Ship without BUG must not be assemblable");
    }

    @Test
    @DisplayName("canAssemble: false when multiple BUG blocks (bugCount=2)")
    void structureScan_cannotAssemble_multipleBugs() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, true, 1, 4, 2, true);
        assertFalse(scan.canAssemble(), "Ship with multiple BUGs must not be assemblable");
    }

    @Test
    @DisplayName("canAssemble: false when BUG is inside (not on edge)")
    void structureScan_cannotAssemble_bugInside() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, true, 1, 4, 1, false);
        assertFalse(scan.canAssemble(), "BUG inside ship must block assembly");
    }

    @Test
    @DisplayName("hasBug: returns true only when exactly 1 BUG")
    void structureScan_hasBug() {
        assertTrue(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, true, 1, 4, 1, true).hasBug());
        assertFalse(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, true, 1, 4, 0, false).hasBug());
        assertFalse(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, true, 1, 4, 2, true).hasBug());
    }

    @Test
    @DisplayName("blockCount: matches block list size")
    void structureScan_blockCount_matchesList() {
        var blocks = List.of(fakeBlock(), fakeBlock(), fakeBlock());
        var scan = makeScan(blocks, Collections.emptyList(), 0, true, 1, 4, 1, true);
        assertEquals(3, scan.blockCount());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static ShipBlueprint.ShipBlock fakeBlock() {
        return new ShipBlueprint.ShipBlock(0, 0, 0, null);
    }
}
