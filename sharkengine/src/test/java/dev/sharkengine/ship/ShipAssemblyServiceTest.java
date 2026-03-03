package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThrusterRequirements and ShipAssemblyService validation logic.
 * All tests run without a real Minecraft server (pure-Java classes only).
 */
@DisplayName("ShipAssembly / ThrusterRequirements Tests")
class ShipAssemblyServiceTest {

    // ─── hasThruster ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasThruster: detects single thruster block")
    void detectsThrusters() {
        assertTrue(ThrusterRequirements.hasThruster(List.of("sharkengine:thruster")),
                "Thruster block should be detected");
    }

    @Test
    @DisplayName("hasThruster: returns false with no thrusters (only planks)")
    void detectsMissingThrusters() {
        assertFalse(ThrusterRequirements.hasThruster(List.of("minecraft:oak_planks")),
                "Structure without thruster must be reported as missing thrusters");
    }

    @Test
    @DisplayName("hasThruster: returns false for empty block list")
    void emptyList_noThruster() {
        assertFalse(ThrusterRequirements.hasThruster(Collections.emptyList()),
                "Empty structure has no thruster");
    }

    @Test
    @DisplayName("hasThruster: returns false for null input (defensive)")
    void nullList_noThruster() {
        assertFalse(ThrusterRequirements.hasThruster(null),
                "Null block list should return false, not throw");
    }

    // ─── countThrusters ───────────────────────────────────────────────────────

    @Test
    @DisplayName("countThrusters: counts multiple thruster blocks")
    void countsMultipleThrusters() {
        List<String> blocks = List.of(
                "sharkengine:thruster",
                "minecraft:oak_planks",
                "sharkengine:thruster"
        );
        assertEquals(2, ThrusterRequirements.countThrusters(blocks));
    }

    @Test
    @DisplayName("countThrusters: returns 0 for empty list")
    void countThrusters_empty() {
        assertEquals(0, ThrusterRequirements.countThrusters(Collections.emptyList()));
    }

    @Test
    @DisplayName("countThrusters: returns 0 for null list (defensive)")
    void countThrusters_null() {
        assertEquals(0, ThrusterRequirements.countThrusters(null));
    }

    @Test
    @DisplayName("countThrusters: only exact ID 'sharkengine:thruster' matches")
    void countThrusters_partialIdDoesNotMatch() {
        // Substrings or similar IDs must NOT be counted
        List<String> blocks = List.of(
                "sharkengine:thruster_mk2",   // future block, different ID
                "minecraft:thruster",          // wrong namespace
                "SHARKENGINE:THRUSTER"         // wrong case
        );
        assertEquals(0, ThrusterRequirements.countThrusters(blocks),
                "Only exact ID 'sharkengine:thruster' should match");
    }

    // ─── StructureScan.canAssemble ────────────────────────────────────────────

    @Test
    @DisplayName("StructureScan.canAssemble: true when all constraints met")
    void structureScan_canAssemble_whenValid() {
        var scan = new ShipAssemblyService.StructureScan(
                null,                                          // origin (not needed for this logic)
                List.of(fakeBlock()),                         // at least one block
                Collections.emptyList(),                      // no invalid attachments
                0,                                             // no terrain contact
                true,                                          // has thruster
                1,                                             // thruster count
                4                                             // core neighbours satisfied
        );
        assertTrue(scan.canAssemble());
    }

    @Test
    @DisplayName("StructureScan.canAssemble: false when no thruster")
    void structureScan_cannotAssemble_noThruster() {
        var scan = new ShipAssemblyService.StructureScan(
                null,
                List.of(fakeBlock()),
                Collections.emptyList(),
                0,
                false,   // <-- no thruster
                0,
                4
        );
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("StructureScan.canAssemble: false when terrain contact exists")
    void structureScan_cannotAssemble_terrainContact() {
        var scan = new ShipAssemblyService.StructureScan(
                null,
                List.of(fakeBlock()),
                Collections.emptyList(),
                1,       // <-- terrain contact
                true,
                1,
                4
        );
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("StructureScan.canAssemble: false when invalid attachments present")
    void structureScan_cannotAssemble_invalidAttachments() {
        var invalidPos = new net.minecraft.core.BlockPos(0, 0, 0);
        var scan = new ShipAssemblyService.StructureScan(
                null,
                List.of(fakeBlock()),
                List.of(invalidPos),  // <-- invalid attachment
                0,
                true,
                1,
                4
        );
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("StructureScan.canAssemble: false when fewer than 4 core neighbours")
    void structureScan_cannotAssemble_insufficientCoreNeighbours() {
        var scan = new ShipAssemblyService.StructureScan(
                null,
                List.of(fakeBlock()),
                Collections.emptyList(),
                0,
                true,
                1,
                3    // <-- only 3 neighbours
        );
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("StructureScan.canAssemble: false when block list is empty")
    void structureScan_cannotAssemble_emptyBlocks() {
        var scan = new ShipAssemblyService.StructureScan(
                null,
                Collections.emptyList(),   // <-- empty
                Collections.emptyList(),
                0,
                true,
                1,
                4
        );
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("StructureScan.blockCount: matches block list size")
    void structureScan_blockCount_matchesList() {
        var blocks = List.of(fakeBlock(), fakeBlock(), fakeBlock());
        var scan = new ShipAssemblyService.StructureScan(
                null, blocks, Collections.emptyList(), 0, true, 1, 4
        );
        assertEquals(3, scan.blockCount());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Creates a minimal ShipBlock for testing.
     * BlockState is {@code null} because assembly-validation logic (canAssemble,
     * blockCount, etc.) never inspects the block's state – only the offset and
     * count matter in these pure-Java tests.
     */
    private static ShipBlueprint.ShipBlock fakeBlock() {
        return new ShipBlueprint.ShipBlock(0, 0, 0, null);
    }
}
