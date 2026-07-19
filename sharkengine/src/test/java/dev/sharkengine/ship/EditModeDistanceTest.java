package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-012/AC-012 (T12) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-012 — Safe edit-mode
 * gate", described there as "the sharpest counter-thesis in the whole plan"): OQ-001 (PRD,
 * User decision 2026-07-18) resolved the "five-block rule" as EUCLIDEAN 3D distance from the
 * player to the vehicle's Control Anchor, &lt;=5 blocks in every direction. But
 * {@code ShipAssemblyService#scanStructure} — the existing, shipped BFS assembly scan — already
 * uses MANHATTAN distance for its own, unrelated radius check ({@code
 * current.distManhattan(wheelPos) > MAX_RADIUS}, {@code ShipAssemblyService.java} line ~380) —
 * that is the codebase's existing idiom for "how far is this block" checks. A coder reusing that
 * idiom here under time pressure would silently ship a Manhattan (or Chebyshev) gate instead of
 * the resolved Euclidean one, and a test using only axis-aligned offsets (all three metrics agree
 * on those) would never notice.
 *
 * <p>These tests therefore use genuinely diagonal offsets where Euclidean and Manhattan/Chebyshev
 * verdicts DISAGREE, and assert the actual distance VALUE returned by {@link
 * EditModeDistanceGate#euclideanDistance}, not merely an accept/reject boolean — so a Manhattan
 * (or Chebyshev) swap fails LOUDLY here (wrong numeric value) rather than only failing silently on
 * some later boundary case.</p>
 *
 * <p>Pure JUnit: plain doubles only, zero Minecraft/Fabric imports — matches the {@code
 * dev.sharkengine.ship.session}/{@code CockpitVisibility} pattern of running with no game
 * bootstrap.</p>
 */
@DisplayName("EditModeDistanceGate Tests (REQ-012/T12)")
class EditModeDistanceTest {

    private static final double DELTA = 1e-9;

    @Test
    @DisplayName("offset (3,3,0): Euclidean ~4.2426 (<=5, ACCEPT) -- Manhattan=6 would wrongly REJECT")
    void offsetThreeThreeZeroDiscriminatesEuclideanFromManhattan() {
        double dx = 3;
        double dy = 3;
        double dz = 0;

        double euclidean = EditModeDistanceGate.euclideanDistance(dx, dy, dz);
        assertEquals(Math.sqrt(18.0), euclidean, DELTA,
                "must be the genuine Euclidean sqrt(dx^2+dy^2+dz^2) (~4.2426), not any other metric");
        assertTrue(euclidean < 5.0,
                "Euclidean distance for (3,3,0) must sit comfortably under the 5-block boundary");

        // Sanity/documentation of the exact counter-thesis case: the Manhattan verdict disagrees.
        double manhattanSum = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        assertEquals(6.0, manhattanSum,
                "sanity check on the counter-thesis's own numbers: |3|+|3|+|0| = 6");
        assertTrue(manhattanSum > 5.0,
                "a Manhattan-based gate would wrongly REJECT this offset -- proves the two metrics "
                        + "diverge on this exact point, which is why this case alone falsifies a "
                        + "Manhattan-based implementation");

        assertTrue(EditModeDistanceGate.withinRange(dx, dy, dz),
                "must ACCEPT under the resolved Euclidean metric (OQ-001)");
    }

    @Test
    @DisplayName("offset (3,4,0): Euclidean == 5.0 exactly (boundary, must ACCEPT per \"<=5\")")
    void offsetThreeFourZeroIsExactBoundaryAndAccepted() {
        double dx = 3;
        double dy = 4;
        double dz = 0;

        double euclidean = EditModeDistanceGate.euclideanDistance(dx, dy, dz);
        assertEquals(5.0, euclidean, DELTA, "the classic 3-4-5 right triangle -- exactly 5.0");
        assertTrue(EditModeDistanceGate.withinRange(dx, dy, dz),
                "a boundary distance of exactly 5.0 must be ACCEPTED (\"<=5\", inclusive)");
    }

    @Test
    @DisplayName("offset (3,4,1): Euclidean ~5.0990 (must REJECT, just past the 5-block boundary)")
    void offsetThreeFourOneIsJustOverBoundaryAndRejected() {
        double dx = 3;
        double dy = 4;
        double dz = 1;

        double euclidean = EditModeDistanceGate.euclideanDistance(dx, dy, dz);
        assertEquals(Math.sqrt(26.0), euclidean, DELTA);
        assertFalse(EditModeDistanceGate.withinRange(dx, dy, dz),
                "a distance just over 5.0 (~5.099) must be REJECTED");
    }

    @Test
    @DisplayName("full gate: in-range + pilot + not destroyed + stationary + conflict-free + same dimension => ACCEPTED")
    void gateAcceptsWhenAllPreconditionsHold() {
        EditModeDistanceGate.Reason reason = EditModeDistanceGate.evaluate(
                true, false, true, false, true, 3, 3, 0);
        assertEquals(EditModeDistanceGate.Reason.ACCEPTED, reason);
        assertTrue(EditModeDistanceGate.isAccepted(reason));
    }

    @Test
    @DisplayName("full gate: distance beyond 5 blocks => REJECTED_TOO_FAR even with every other precondition satisfied")
    void gateRejectsWhenTooFar() {
        EditModeDistanceGate.Reason reason = EditModeDistanceGate.evaluate(
                true, false, true, false, true, 3, 4, 1);
        assertEquals(EditModeDistanceGate.Reason.REJECTED_TOO_FAR, reason);
        assertFalse(EditModeDistanceGate.isAccepted(reason));
    }

    @Test
    @DisplayName("full gate: a MOVING vehicle is rejected regardless of distance (distance 0)")
    void gateRejectsMovingVehicleRegardlessOfDistance() {
        EditModeDistanceGate.Reason reason = EditModeDistanceGate.evaluate(
                true, false, false, false, true, 0, 0, 0);
        assertEquals(EditModeDistanceGate.Reason.REJECTED_MOVING, reason,
                "a non-stationary vehicle must be rejected even at distance 0");
    }

    @Test
    @DisplayName("full gate: a DESTROYED vehicle is rejected regardless of distance (distance 0)")
    void gateRejectsDestroyedVehicleRegardlessOfDistance() {
        EditModeDistanceGate.Reason reason = EditModeDistanceGate.evaluate(
                true, true, true, false, true, 0, 0, 0);
        assertEquals(EditModeDistanceGate.Reason.REJECTED_DESTROYED, reason,
                "a destroyed vehicle must be rejected even at distance 0");
    }

    @Test
    @DisplayName("full gate: already-active Edit Mode (conflict) is rejected regardless of distance (distance 0)")
    void gateRejectsConflictingEditModeRegardlessOfDistance() {
        EditModeDistanceGate.Reason reason = EditModeDistanceGate.evaluate(
                true, false, true, true, true, 0, 0, 0);
        assertEquals(EditModeDistanceGate.Reason.REJECTED_CONFLICT, reason,
                "an already-open Edit Mode must reject a second concurrent open attempt even at distance 0");
    }

    @Test
    @DisplayName("full gate: a non-pilot requester is rejected regardless of distance (distance 0)")
    void gateRejectsNonPilotRegardlessOfDistance() {
        EditModeDistanceGate.Reason reason = EditModeDistanceGate.evaluate(
                false, false, true, false, true, 0, 0, 0);
        assertEquals(EditModeDistanceGate.Reason.REJECTED_NOT_PILOT, reason,
                "REQ-008 reserves Edit-class commands to the assigned pilot; a non-pilot must be "
                        + "rejected even at distance 0");
    }

    @Test
    @DisplayName("security fix (attempt-2 review finding): a requester in a DIFFERENT dimension is "
            + "rejected even at numerically-matching offset (0,0,0)")
    void gateRejectsWrongDimensionEvenAtNumericallyMatchingOffsetOfZero() {
        // This is the exact scenario the attempt-1 review flagged: a player standing at raw
        // coordinates that numerically equal the ship's own (dx=dy=dz=0) but in an unrelated
        // dimension (e.g. the Nether/End share the same coordinate space as the Overworld) must
        // NOT be treated as "physically next to the ship." Every other precondition here is
        // deliberately satisfied so ONLY the sameDimension axis can be responsible for the
        // rejection -- proving the check is real, not incidentally covered by another axis.
        EditModeDistanceGate.Reason reason = EditModeDistanceGate.evaluate(
                true, false, true, false, false, 0, 0, 0);
        assertEquals(EditModeDistanceGate.Reason.REJECTED_WRONG_DIMENSION, reason,
                "a requester in a different dimension must be rejected even when raw numeric "
                        + "coordinate offsets equal zero");
        assertFalse(EditModeDistanceGate.isAccepted(reason));
    }

    @Test
    @DisplayName("security fix: a DIFFERENT dimension is rejected regardless of an otherwise in-range offset")
    void gateRejectsWrongDimensionRegardlessOfOtherwiseInRangeOffset() {
        EditModeDistanceGate.Reason reason = EditModeDistanceGate.evaluate(
                true, false, true, false, false, 3, 3, 0);
        assertEquals(EditModeDistanceGate.Reason.REJECTED_WRONG_DIMENSION, reason,
                "wrong dimension must reject even at an offset that would otherwise be well within "
                        + "the <=5 Euclidean boundary");
    }
}
