package dev.sharkengine.ship;

import dev.sharkengine.ship.part.AssemblyIssue;
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
 *
 * <p>REQ-005/T05: {@code makeScan} takes an explicit {@code pilotSeatCount} parameter,
 * given the same role-based-count treatment as {@code propulsionCount}. Every
 * pre-existing test below that exercises a DIFFERENT failure condition passes
 * {@code pilotSeatCount=1} (the "otherwise valid" baseline) so that condition alone
 * stays isolated — exactly like those tests already do for
 * {@code propulsionCount=1}/{@code coreNeighbors=4}/{@code bugCount=1} elsewhere in
 * this file.</p>
 *
 * <p>REQ-006/T06: {@code makeScan} additionally takes an explicit {@code seatAnchorValid}
 * parameter — same isolation treatment again: every pre-existing test below that
 * exercises a DIFFERENT failure condition passes {@code seatAnchorValid=true} (the
 * "otherwise valid" baseline). The occupied-front-slot failure case itself and the
 * per-facing offset math are covered by {@code dev.sharkengine.gametest.PilotSeatAnchorGameTest}
 * (needs a real {@code ServerLevel} to check an actual block at a computed world position)
 * and {@link ShipTransformTest} (pure rotation math) respectively, not here.</p>
 */
@DisplayName("ShipAssembly / StructureScan / BUG Tests")
class ShipAssemblyServiceTest {

    // ─── StructureScan.canAssemble (with BUG) ────────────────────────────────

    /** Helper to build a valid scan with BUG defaults */
    private static ShipAssemblyService.StructureScan makeScan(
            List<ShipBlueprint.ShipBlock> blocks,
            List<net.minecraft.core.BlockPos> invalidAttachments,
            int contactPoints, int propulsionCount, int pilotSeatCount, int coreNeighbors,
            int bugCount, boolean bugOnEdge) {
        return makeScan(blocks, invalidAttachments, contactPoints, propulsionCount,
                pilotSeatCount, coreNeighbors, bugCount, bugOnEdge, true);
    }

    /** Helper to build a valid scan with BUG defaults, with explicit seat-anchor validity (REQ-006). */
    private static ShipAssemblyService.StructureScan makeScan(
            List<ShipBlueprint.ShipBlock> blocks,
            List<net.minecraft.core.BlockPos> invalidAttachments,
            int contactPoints, int propulsionCount, int pilotSeatCount, int coreNeighbors,
            int bugCount, boolean bugOnEdge, boolean seatAnchorValid) {
        ShipStats stats = new ShipStats(0, 0, 0, 0, 0, propulsionCount, pilotSeatCount);
        // REQ-009/T07: copilotSeatAnchors always empty here -- every test in this file
        // exercises a condition independent of the copilot seat, same isolation treatment
        // already given to pilotSeatCount/seatAnchorValid above.
        return new ShipAssemblyService.StructureScan(
                null, blocks, invalidAttachments, contactPoints,
                stats, coreNeighbors,
                bugCount, bugOnEdge, 0.0f, seatAnchorValid, List.of()
        );
    }

    @Test
    @DisplayName("canAssemble: true when all constraints met including BUG")
    void structureScan_canAssemble_whenValid() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, true);
        assertTrue(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when no PROPULSION part (role-based, AIR-021)")
    void structureScan_cannotAssemble_noPropulsion() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 0, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
        assertFalse(scan.hasThruster());
        assertEquals(0, scan.thrusterCount());
    }

    @Test
    @DisplayName("canAssemble: false when terrain contact exists")
    void structureScan_cannotAssemble_terrainContact() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                1, 1, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when invalid attachments present")
    void structureScan_cannotAssemble_invalidAttachments() {
        var invalidPos = new net.minecraft.core.BlockPos(0, 0, 0);
        var scan = makeScan(List.of(fakeBlock()), List.of(invalidPos),
                0, 1, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when fewer than 4 core neighbours")
    void structureScan_cannotAssemble_insufficientCoreNeighbours() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 3, 1, true);
        assertFalse(scan.canAssemble());
    }

    @Test
    @DisplayName("canAssemble: false when block list is empty")
    void structureScan_cannotAssemble_emptyBlocks() {
        var scan = makeScan(Collections.emptyList(), Collections.emptyList(),
                0, 1, 1, 4, 1, true);
        assertFalse(scan.canAssemble());
    }

    // ─── BUG-specific validation ─────────────────────────────────────────────

    @Test
    @DisplayName("canAssemble: false when no BUG block (bugCount=0)")
    void structureScan_cannotAssemble_noBug() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 0, false);
        assertFalse(scan.canAssemble(), "Ship without BUG must not be assemblable");
    }

    @Test
    @DisplayName("canAssemble: false when multiple BUG blocks (bugCount=2)")
    void structureScan_cannotAssemble_multipleBugs() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 2, true);
        assertFalse(scan.canAssemble(), "Ship with multiple BUGs must not be assemblable");
    }

    @Test
    @DisplayName("canAssemble: false when BUG is inside (not on edge)")
    void structureScan_cannotAssemble_bugInside() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, false);
        assertFalse(scan.canAssemble(), "BUG inside ship must block assembly");
    }

    @Test
    @DisplayName("hasBug: returns true only when exactly 1 BUG")
    void structureScan_hasBug() {
        assertTrue(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, true).hasBug());
        assertFalse(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 0, false).hasBug());
        assertFalse(makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 2, true).hasBug());
    }

    @Test
    @DisplayName("blockCount: matches block list size")
    void structureScan_blockCount_matchesList() {
        var blocks = List.of(fakeBlock(), fakeBlock(), fakeBlock());
        var scan = makeScan(blocks, Collections.emptyList(), 0, 1, 1, 4, 1, true);
        assertEquals(3, scan.blockCount());
    }

    @Test
    @DisplayName("hasThruster/thrusterCount delegate to ShipStats.hasPropulsion/propulsionCount (AIR-021)")
    void structureScan_thrusterAccessorsDelegateToStats() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 3, 1, 4, 1, true);
        assertTrue(scan.hasThruster());
        assertEquals(3, scan.thrusterCount());
    }

    // ─── REQ-005/T05: pilot-seat count validation ──────────────────────────────
    //
    // Mirrors the thruster-accessor/canAssemble coverage above one-to-one, but for
    // the PILOT_SEAT-role count: assembly requires EXACTLY one (not "at least one"
    // like propulsion), so both zero and more-than-one are rejected.

    @Test
    @DisplayName("pilotSeatCount delegates to ShipStats.pilotSeatCount (REQ-005)")
    void structureScan_pilotSeatCountDelegatesToStats() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, true);
        assertEquals(1, scan.pilotSeatCount());
    }

    @Test
    @DisplayName("canAssemble: false when zero pilot seats present (REQ-005)")
    void structureScan_cannotAssemble_noPilotSeat() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 0, 4, 1, true);
        assertFalse(scan.canAssemble(), "Ship without a pilot seat must not be assemblable");
        assertEquals(0, scan.pilotSeatCount());
    }

    @Test
    @DisplayName("canAssemble: false when two pilot seats present, even though every other "
            + "condition is met (REQ-005 'exactly one', not 'at least one')")
    void structureScan_cannotAssemble_twoPilotSeats() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 2, 4, 1, true);
        assertFalse(scan.canAssemble(), "Ship with two pilot seats must not be assemblable");
        assertEquals(2, scan.pilotSeatCount());
    }

    @Test
    @DisplayName("canAssemble: true with exactly one pilot seat and every other condition met (REQ-005)")
    void structureScan_canAssemble_exactlyOnePilotSeat() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, true);
        assertTrue(scan.canAssemble());
    }

    // ─── REQ-006/T06: seat-anchor validity ─────────────────────────────────────
    //
    // Mirrors the canAssemble/issues coverage above one-to-one, but for
    // seatAnchorValid: even with a count of exactly one pilot seat (T05's own
    // invariant satisfied), assembly still requires that ONE seat to sit at the
    // single deterministic front-of-wheel position -- not merely exist somewhere
    // in the structure.

    @Test
    @DisplayName("canAssemble: false when seatAnchorValid=false, even with an otherwise-valid " +
            "structure and exactly one pilot seat present (REQ-006)")
    void structureScan_cannotAssemble_seatAnchorInvalid() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, true, false);
        assertFalse(scan.canAssemble(),
                "A pilot seat that exists but isn't at the front-of-wheel position must not assemble");
    }

    @Test
    @DisplayName("canAssemble: true when seatAnchorValid=true and every other condition is met (REQ-006)")
    void structureScan_canAssemble_seatAnchorValid() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, true, true);
        assertTrue(scan.canAssemble());
    }

    // ─── issues() — structured assembly validation codes (AIR-022, REQ-S3) ────────────────
    //
    // One test per AssemblyIssue.Code, mirroring canAssemble()'s conditions one-to-one:
    // issues() must report every currently-failing condition (not just the first, unlike
    // the tryAssemble() chat-message chain), and must be empty exactly when canAssemble()
    // is true.

    @Test
    @DisplayName("issues(): empty when canAssemble() is true")
    void issues_emptyWhenAssemblable() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, true);
        assertTrue(scan.canAssemble());
        assertTrue(scan.issues().isEmpty());
    }

    @Test
    @DisplayName("issues(): EMPTY_STRUCTURE when block list is empty, and nothing else")
    void issues_emptyStructure() {
        var scan = makeScan(Collections.emptyList(), Collections.emptyList(),
                0, 1, 1, 4, 1, true);
        assertEquals(List.of(AssemblyIssue.of(AssemblyIssue.Code.EMPTY_STRUCTURE)), scan.issues());
    }

    @Test
    @DisplayName("issues(): INVALID_ATTACHMENTS carries the invalid-block count")
    void issues_invalidAttachments() {
        var invalidPos = new net.minecraft.core.BlockPos(0, 0, 0);
        var scan = makeScan(List.of(fakeBlock()), List.of(invalidPos),
                0, 1, 1, 4, 1, true);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.INVALID_ATTACHMENTS, 1)));
    }

    @Test
    @DisplayName("issues(): TERRAIN_CONTACT carries the contact-point count")
    void issues_terrainContact() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                2, 1, 1, 4, 1, true);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.TERRAIN_CONTACT, 2)));
    }

    @Test
    @DisplayName("issues(): NO_PROPULSION when no PROPULSION part is present")
    void issues_noPropulsion() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 0, 1, 4, 1, true);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.NO_PROPULSION)));
    }

    @Test
    @DisplayName("issues(): NO_PILOT_SEAT when zero pilot seats are present (REQ-005)")
    void issues_noPilotSeat() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 0, 4, 1, true);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.NO_PILOT_SEAT)));
    }

    @Test
    @DisplayName("issues(): MULTI_PILOT_SEAT carries the pilot-seat count when more than one is present (REQ-005)")
    void issues_multiPilotSeat() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 2, 4, 1, true);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.MULTI_PILOT_SEAT, 2)));
    }

    @Test
    @DisplayName("issues(): TOO_FEW_CORE_NEIGHBORS carries the actual core-neighbor count")
    void issues_tooFewCoreNeighbors() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 2, 1, true);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.TOO_FEW_CORE_NEIGHBORS, 2)));
    }

    @Test
    @DisplayName("issues(): NO_BUG when bugCount is 0")
    void issues_noBug() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 0, false);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.NO_BUG)));
    }

    @Test
    @DisplayName("issues(): MULTI_BUG carries the bug count when more than one BUG is present")
    void issues_multiBug() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 2, true);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.MULTI_BUG, 2)));
    }

    @Test
    @DisplayName("issues(): BUG_INSIDE when the single BUG is not on the edge")
    void issues_bugInside() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, false);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.BUG_INSIDE)));
    }

    @Test
    @DisplayName("issues(): SEAT_ANCHOR_INVALID when the front-of-wheel slot doesn't hold the " +
            "pilot seat, even though exactly one BUG resolves a facing (REQ-006)")
    void issues_seatAnchorInvalid() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 1, true, false);
        assertTrue(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.SEAT_ANCHOR_INVALID)));
    }

    @Test
    @DisplayName("issues(): SEAT_ANCHOR_INVALID is NOT reported when bugCount != 1 (NO_BUG/MULTI_BUG " +
            "already cover the undefined-facing case; would otherwise be redundant noise, REQ-006)")
    void issues_seatAnchorInvalidNotReportedWithoutAWellDefinedBug() {
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 1, 1, 4, 0, false, false);
        assertFalse(scan.issues().contains(AssemblyIssue.of(AssemblyIssue.Code.SEAT_ANCHOR_INVALID)));
    }

    @Test
    @DisplayName("issues(): multiple simultaneous failures are all reported at once")
    void issues_reportsAllSimultaneousFailures() {
        // No propulsion AND too few core neighbors AND no bug, all at once.
        // pilotSeatCount=1 keeps the pilot-seat condition isolated out of this
        // particular test's scope (covered on its own above).
        var scan = makeScan(List.of(fakeBlock()), Collections.emptyList(),
                0, 0, 1, 1, 0, false);
        List<AssemblyIssue> issues = scan.issues();
        assertTrue(issues.contains(AssemblyIssue.of(AssemblyIssue.Code.NO_PROPULSION)));
        assertTrue(issues.contains(AssemblyIssue.of(AssemblyIssue.Code.TOO_FEW_CORE_NEIGHBORS, 1)));
        assertTrue(issues.contains(AssemblyIssue.of(AssemblyIssue.Code.NO_BUG)));
        assertEquals(3, issues.size());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static ShipBlueprint.ShipBlock fakeBlock() {
        return new ShipBlueprint.ShipBlock(0, 0, 0, null);
    }
}
