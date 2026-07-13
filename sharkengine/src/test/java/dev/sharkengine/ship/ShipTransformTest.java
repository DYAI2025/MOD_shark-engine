package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-math tests for {@link ShipTransform} — the single rotation authority
 * shared by rendering, collision, and disassembly (AIR-010).
 *
 * <p>World truth pinned by cross-checking against real Minecraft 1.21.1
 * bytecode: yaw is clockwise-positive from above, 0 = south, matching
 * {@code directionToYaw()} in ShipAssemblyService (SOUTH=0, WEST=90,
 * NORTH=180, EAST=-90). {@code rotateOffset} at +90 degrees must produce
 * the exact same (dx,dz) mapping as vanilla's
 * {@code net.minecraft.core.BlockPos.rotate(Rotation.CLOCKWISE_90)}, which
 * decompiles to {@code (x,z) -> (-z, x)}.</p>
 */
@DisplayName("ShipTransform")
class ShipTransformTest {

    private static final double EPSILON = 1e-9;

    @Nested
    @DisplayName("rotateOffset")
    class RotateOffsetTests {

        @Test
        @DisplayName("0 degrees is identity")
        void zeroDegreesIsIdentity() {
            double[] result = ShipTransform.rotateOffset(3, -5, 0f);
            assertEquals(3.0, result[0], EPSILON);
            assertEquals(-5.0, result[1], EPSILON);
        }

        @Test
        @DisplayName("REGRESSION: +90 degrees matches vanilla Rotation.CLOCKWISE_90 exactly: (dx,dz) -> (-dz,dx)")
        void ninetyDegreesMatchesVanillaClockwise90() {
            double[] result = ShipTransform.rotateOffset(3, -5, 90f);
            assertEquals(-(-5.0), result[0], EPSILON, "worldX must equal -dz");
            assertEquals(3.0, result[1], EPSILON, "worldZ must equal dx");
        }

        @Test
        @DisplayName("180 degrees matches vanilla Rotation.CLOCKWISE_180: (dx,dz) -> (-dx,-dz)")
        void oneEightyDegreesMatchesVanillaClockwise180() {
            double[] result = ShipTransform.rotateOffset(3, -5, 180f);
            assertEquals(-3.0, result[0], EPSILON);
            assertEquals(5.0, result[1], EPSILON);
        }

        @Test
        @DisplayName("270 degrees matches vanilla Rotation.COUNTERCLOCKWISE_90: (dx,dz) -> (dz,-dx)")
        void twoSeventyDegreesMatchesVanillaCounterclockwise90() {
            double[] result = ShipTransform.rotateOffset(3, -5, 270f);
            assertEquals(-5.0, result[0], EPSILON);
            assertEquals(-3.0, result[1], EPSILON);
        }

        @Test
        @DisplayName("full 360-degree roundtrip returns to origin offset")
        void threeSixtyRoundtrip() {
            double dx = 7, dz = -2;
            double[] r0 = ShipTransform.rotateOffset(dx, dz, 0f);
            double[] r360 = ShipTransform.rotateOffset(dx, dz, 360f);
            assertEquals(r0[0], r360[0], EPSILON);
            assertEquals(r0[1], r360[1], EPSILON);
        }

        @Test
        @DisplayName("arbitrary angle: 45 degrees preserves vector length")
        void arbitraryAngleLengthPreserved() {
            double dx = 4, dz = 3; // length 5
            double[] result = ShipTransform.rotateOffset(dx, dz, 45f);
            double length = Math.sqrt(result[0] * result[0] + result[1] * result[1]);
            assertEquals(5.0, length, 1e-6);
        }
    }

    @Nested
    @DisplayName("wrapDegrees")
    class WrapDegreesTests {

        @Test
        @DisplayName("value already in (-180,180] is unchanged")
        void alreadyInRangeUnchanged() {
            assertEquals(45f, ShipTransform.wrapDegrees(45f), 1e-6);
            assertEquals(-179f, ShipTransform.wrapDegrees(-179f), 1e-6);
            assertEquals(180f, ShipTransform.wrapDegrees(180f), 1e-6);
        }

        @Test
        @DisplayName("wraps values above 180")
        void wrapsAbove180() {
            assertEquals(-170f, ShipTransform.wrapDegrees(190f), 1e-6);
            assertEquals(-1f, ShipTransform.wrapDegrees(359f), 1e-6);
        }

        @Test
        @DisplayName("wraps values at/below -180")
        void wrapsBelowNegative180() {
            assertEquals(170f, ShipTransform.wrapDegrees(-190f), 1e-6);
            assertEquals(180f, ShipTransform.wrapDegrees(-180f), 1e-6);
        }
    }

    @Nested
    @DisplayName("effectiveYaw")
    class EffectiveYawTests {

        @Test
        @DisplayName("SOUTH-facing BUG (assemblyYaw=0) at rest: effective yaw equals entity yaw")
        void southFacingBugAtRest() {
            assertEquals(0f, ShipTransform.effectiveYaw(0f, 0f), 1e-6);
            assertEquals(45f, ShipTransform.effectiveYaw(45f, 0f), 1e-6);
        }

        @Test
        @DisplayName("WEST-facing BUG (assemblyYaw=90) at rest: effective yaw is 0 (no visual snap)")
        void westFacingBugAtRestHasZeroEffectiveYaw() {
            // The exact bug this REQ exists to fix: entity spawns with yaw=bugYawDeg,
            // so effectiveYaw must be 0 at the moment of assembly regardless of facing.
            assertEquals(0f, ShipTransform.effectiveYaw(90f, 90f), 1e-6);
        }

        @Test
        @DisplayName("wraps around the -180/180 boundary")
        void wrapsAroundBoundary() {
            assertEquals(-170f, ShipTransform.effectiveYaw(170f, -20f), 1e-6);
        }
    }

    @Nested
    @DisplayName("snapToCardinal")
    class SnapToCardinalTests {

        @Test
        @DisplayName("snaps a table of boundary and mid-range values to the nearest cardinal")
        void snapsToNearestCardinal() {
            assertEquals(0, ShipTransform.snapToCardinal(0f));
            assertEquals(0, ShipTransform.snapToCardinal(44.9f));
            assertEquals(90, ShipTransform.snapToCardinal(45.1f));
            assertEquals(90, ShipTransform.snapToCardinal(89f));
            assertEquals(90, ShipTransform.snapToCardinal(91f));
            assertEquals(90, ShipTransform.snapToCardinal(134.9f));
            assertEquals(180, ShipTransform.snapToCardinal(135.1f));
            assertEquals(270, ShipTransform.snapToCardinal(225.1f));
            assertEquals(0, ShipTransform.snapToCardinal(-44.9f));
            assertEquals(270, ShipTransform.snapToCardinal(-45.1f));
            assertEquals(270, ShipTransform.snapToCardinal(-89f));
            assertEquals(270, ShipTransform.snapToCardinal(-91f));
        }
    }

    @Nested
    @DisplayName("worldBlock")
    class WorldBlockTests {

        @Test
        @DisplayName("rotating then rounding dedupes offsets that collide onto the same integer position")
        void dedupesRoundingCollisions() {
            // Two adjacent-but-distinct local offsets can round onto the same
            // world block after a non-cardinal rotation; worldBlock must be
            // deterministic (same input -> same output) so callers can dedupe
            // via a Set, not silently duplicate/lose blocks.
            long a = ShipTransform.worldBlock(0, 0, 0, 44f);
            long b = ShipTransform.worldBlock(0, 0, 0, 44f);
            assertEquals(a, b, "same input must produce the same packed world position");
        }

        @Test
        @DisplayName("0 degrees maps local offset directly onto world position")
        void zeroDegreesIsDirectMapping() {
            long packed = ShipTransform.worldBlock(5, 2, -3, 0f);
            long expected = ShipTransform.packBlockPos(5, 2, -3);
            assertEquals(expected, packed);
        }
    }

    @Nested
    @DisplayName("rollFromTurnInput (FLR-002, docs/plans/flight-bank-roll.md)")
    class RollFromTurnInputTests {

        private static final float MAX_BANK = 25.0f;

        @Test
        @DisplayName("zero input produces zero roll")
        void zeroInputIsZeroRoll() {
            assertEquals(0.0f, ShipTransform.rollFromTurnInput(0f, MAX_BANK), (float) EPSILON);
        }

        @Test
        @DisplayName("full positive input produces full positive bank")
        void fullPositiveInputIsFullBank() {
            assertEquals(MAX_BANK, ShipTransform.rollFromTurnInput(1f, MAX_BANK), (float) EPSILON);
        }

        @Test
        @DisplayName("full negative input produces full negative bank")
        void fullNegativeInputIsFullBank() {
            assertEquals(-MAX_BANK, ShipTransform.rollFromTurnInput(-1f, MAX_BANK), (float) EPSILON);
        }

        @Test
        @DisplayName("half input produces half bank (linear, proportional to turn input)")
        void halfInputIsHalfBank() {
            assertEquals(MAX_BANK / 2f, ShipTransform.rollFromTurnInput(0.5f, MAX_BANK), (float) EPSILON);
        }

        @Test
        @DisplayName("REGRESSION: same sign as turnInput — positive turnInput (turns left, per the " +
                "P0-verified ShipEntity.tick() convention: yaw = getYRot() - inputTurn*3f) must bank " +
                "the same sign, not opposite")
        void sameSignAsTurnInput() {
            float positiveRoll = ShipTransform.rollFromTurnInput(0.3f, MAX_BANK);
            float negativeRoll = ShipTransform.rollFromTurnInput(-0.3f, MAX_BANK);
            assertEquals(true, positiveRoll > 0, "positive turnInput must produce positive roll");
            assertEquals(true, negativeRoll < 0, "negative turnInput must produce negative roll");
        }

        @Test
        @DisplayName("out-of-range turnInput stays clamped to maxBankDeg (defensive, callers should " +
                "already clamp to -1..1)")
        void outOfRangeInputIsClamped() {
            assertEquals(MAX_BANK, ShipTransform.rollFromTurnInput(1.5f, MAX_BANK), (float) EPSILON);
            assertEquals(-MAX_BANK, ShipTransform.rollFromTurnInput(-2f, MAX_BANK), (float) EPSILON);
        }
    }

    @Nested
    @DisplayName("pitchFromVerticalInput (FLP-002, docs/plans/flight-pitch.md)")
    class PitchFromVerticalInputTests {

        private static final float MAX_PITCH = 18.0f;

        @Test
        @DisplayName("zero input produces zero pitch")
        void zeroInputIsZeroPitch() {
            assertEquals(0.0f, ShipTransform.pitchFromVerticalInput(0f, MAX_PITCH), (float) EPSILON);
        }

        @Test
        @DisplayName("full positive input (Space/climb) produces full positive pitch (nose up)")
        void fullPositiveInputIsFullPitch() {
            assertEquals(MAX_PITCH, ShipTransform.pitchFromVerticalInput(1f, MAX_PITCH), (float) EPSILON);
        }

        @Test
        @DisplayName("full negative input (Shift/descend) produces full negative pitch (nose down)")
        void fullNegativeInputIsFullPitch() {
            assertEquals(-MAX_PITCH, ShipTransform.pitchFromVerticalInput(-1f, MAX_PITCH), (float) EPSILON);
        }

        @Test
        @DisplayName("half input produces half pitch (linear, proportional to vertical input)")
        void halfInputIsHalfPitch() {
            assertEquals(MAX_PITCH / 2f, ShipTransform.pitchFromVerticalInput(0.5f, MAX_PITCH), (float) EPSILON);
        }

        @Test
        @DisplayName("REGRESSION: same sign as verticalInput — positive verticalInput (Space/climb) " +
                "must pitch the same sign (nose up), not opposite")
        void sameSignAsVerticalInput() {
            float positivePitch = ShipTransform.pitchFromVerticalInput(0.3f, MAX_PITCH);
            float negativePitch = ShipTransform.pitchFromVerticalInput(-0.3f, MAX_PITCH);
            assertEquals(true, positivePitch > 0, "positive verticalInput must produce positive pitch");
            assertEquals(true, negativePitch < 0, "negative verticalInput must produce negative pitch");
        }

        @Test
        @DisplayName("out-of-range verticalInput stays clamped to maxPitchDeg (defensive, callers " +
                "should already clamp to -1..1)")
        void outOfRangeInputIsClamped() {
            assertEquals(MAX_PITCH, ShipTransform.pitchFromVerticalInput(1.5f, MAX_PITCH), (float) EPSILON);
            assertEquals(-MAX_PITCH, ShipTransform.pitchFromVerticalInput(-2f, MAX_PITCH), (float) EPSILON);
        }
    }
}
