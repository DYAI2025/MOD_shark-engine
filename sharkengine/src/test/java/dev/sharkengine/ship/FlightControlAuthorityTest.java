package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-015/AC-015 (T16) falsifying-test contract (test-plan {@code
 * docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-015 — AIR flight
 * controls"). Two contracts:
 *
 * <ol>
 *   <li><b>Real, direction-consistent yaw change</b> — the tester's counter-thesis: bank/turn
 *       implemented as a purely cosmetic client-side roll would visibly "bank" while the heading
 *       never turns. {@link ShipPhysics#calculateYawStep} is the REAL server-side heading step
 *       (extracted from {@code ShipEntity.updatePhysics()} by T16; the cosmetic clientRoll/
 *       clientPitch fields are a separate, never-synced render path). These tests also lock the
 *       empirically-corrected steering SIGN from the 2026-07-12 P0 inversion hotfix: {@code +1}
 *       (A key / stick-left) must DECREASE yaw — in Minecraft's yaw space increasing yaw turns
 *       the ship right.</li>
 *   <li><b>NaN/Infinity safety across the full input range</b> — AC-015: "reagiert … ohne
 *       NaN-/Infinity- oder Autoritätsfehler". Regression note: the pre-T16 clamp
 *       ({@code Math.max(a, Math.min(b, v))}) PROPAGATED NaN — a single NaN float in a
 *       {@code HelmInputC2SPayload} reached {@code inputTurn}, poisoned yaw, then the whole
 *       movement vector and entity position. The equivalent guard existed on the abandoned
 *       pre-recovery line ("hovercraft input clamp rejects NaN/Infinity") and was lost in the
 *       code recovery; {@link ShipPhysics#clampInput} is the restored single sanitization
 *       point.</li>
 * </ol>
 *
 * <p><b>Honestly-disclosed scope:</b> the zero-fuel/engine-out movement path uses inline
 * constant velocities in {@code ShipEntity.tick()} (bob ±0.02, fall −0.15 — trivially finite)
 * and vanilla {@code move()} handling; it is not part of this pure-math surface. Pilot
 * authority over these inputs is REQ-008's already-shipped gate
 * ({@code PilotControlAuthorityGameTest}), not re-proven here.</p>
 */
@DisplayName("REQ-015/T16: flight control authority — real yaw change + NaN/Infinity safety")
class FlightControlAuthorityTest {

    @Test
    @DisplayName("Sustained left input (+1) decreases yaw by exactly TURN_RATE every tick — a real turn, not a cosmetic bank")
    void sustainedLeftInputTurnsLeftConsistently() {
        float yaw = 0.0f;
        for (int tick = 1; tick <= 40; tick++) {
            float before = yaw;
            yaw = ShipPhysics.calculateYawStep(yaw, 1.0f);
            assertEquals(before - ShipPhysics.TURN_RATE_DEG_PER_TICK, yaw, 1e-6,
                    "yaw must decrease by exactly one turn step at tick " + tick);
        }
        assertEquals(-40 * ShipPhysics.TURN_RATE_DEG_PER_TICK, yaw, 1e-4,
                "40 sustained left-input ticks must accumulate to a 120° left turn");
    }

    @Test
    @DisplayName("Sustained right input (-1) increases yaw by exactly TURN_RATE every tick")
    void sustainedRightInputTurnsRightConsistently() {
        float yaw = 90.0f;
        for (int tick = 1; tick <= 40; tick++) {
            float before = yaw;
            yaw = ShipPhysics.calculateYawStep(yaw, -1.0f);
            assertEquals(before + ShipPhysics.TURN_RATE_DEG_PER_TICK, yaw, 1e-6,
                    "yaw must increase by exactly one turn step at tick " + tick);
        }
    }

    @Test
    @DisplayName("Zero turn input leaves yaw untouched over many ticks")
    void zeroInputHoldsHeading() {
        float yaw = 42.5f;
        for (int tick = 0; tick < 200; tick++) {
            yaw = ShipPhysics.calculateYawStep(yaw, 0.0f);
        }
        assertEquals(42.5f, yaw, 1e-6, "no input must mean no heading change");
    }

    @Test
    @DisplayName("Helm payload sanitization: NaN becomes neutral input (0), Infinity clamps to the range bounds")
    void nanAndInfinityPayloadsAreSanitized() {
        assertEquals(0.0f, ShipPhysics.clampInput(Float.NaN, -1.0f, 1.0f),
                "a NaN turn payload must be neutralized to 'no input', never propagated");
        assertEquals(0.0f, ShipPhysics.clampInput(Float.NaN, 0.0f, 1.0f),
                "a NaN forward payload must be neutralized to 'no input', never propagated");
        assertEquals(1.0f, ShipPhysics.clampInput(Float.POSITIVE_INFINITY, -1.0f, 1.0f),
                "+Infinity must clamp to the upper bound");
        assertEquals(-1.0f, ShipPhysics.clampInput(Float.NEGATIVE_INFINITY, -1.0f, 1.0f),
                "-Infinity must clamp to the lower bound");
        // In-range values pass through untouched (sanitization must not distort real input)
        assertEquals(0.5f, ShipPhysics.clampInput(0.5f, -1.0f, 1.0f));
        assertEquals(-1.0f, ShipPhysics.clampInput(-1.0f, -1.0f, 1.0f));
    }

    @Test
    @DisplayName("Fuzz: full payload range (incl. NaN/±Inf) × speeds × start yaws, 200 ticks — yaw, velocity and position stay finite")
    void fuzzSweepNeverProducesNaNOrInfinity() {
        float[] rawPayloads = {Float.NEGATIVE_INFINITY, -1.0f, -0.5f, -0.0f, 0.0f, 0.5f, 1.0f,
                Float.POSITIVE_INFINITY, Float.NaN};
        float[] speeds = {0.0f, 5.0f, 30.0f};
        float[] startYaws = {0.0f, 179.9f, -180.0f, 36000.0f};

        for (float rawTurn : rawPayloads) {
            for (float rawVertical : rawPayloads) {
                for (float speed : speeds) {
                    for (float startYaw : startYaws) {
                        // Same sanitization the C2S handler applies via ShipEntity.setInputs
                        float turn = ShipPhysics.clampInput(rawTurn, -1.0f, 1.0f);
                        float vertical = ShipPhysics.clampInput(rawVertical, -1.0f, 1.0f);
                        float yaw = startYaw;
                        double x = 0.0, y = 100.0, z = 0.0;
                        String scenario = "turn=" + rawTurn + " vertical=" + rawVertical
                                + " speed=" + speed + " startYaw=" + startYaw;

                        for (int tick = 0; tick < 200; tick++) {
                            yaw = ShipPhysics.calculateYawStep(yaw, turn);
                            double vx = ShipPhysics.calculateVelocityX(yaw, speed);
                            double vy = ShipPhysics.calculateVelocityY(vertical);
                            double vz = ShipPhysics.calculateVelocityZ(yaw, speed);
                            x += vx;
                            y += vy;
                            z += vz;

                            final float yawNow = yaw;
                            final double xNow = x, yNow = y, zNow = z;
                            final double vxNow = vx, vyNow = vy, vzNow = vz;
                            final int tickNow = tick;
                            assertTrue(Float.isFinite(yawNow),
                                    () -> "yaw not finite (" + yawNow + ") at tick " + tickNow + ": " + scenario);
                            assertTrue(Double.isFinite(vxNow) && Double.isFinite(vyNow) && Double.isFinite(vzNow),
                                    () -> "velocity not finite at tick " + tickNow + ": " + scenario);
                            assertTrue(Double.isFinite(xNow) && Double.isFinite(yNow) && Double.isFinite(zNow),
                                    () -> "position not finite at tick " + tickNow + ": " + scenario);
                        }
                    }
                }
            }
        }
    }
}
