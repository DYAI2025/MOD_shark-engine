package dev.sharkengine.ship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HovercraftController — Tests A–J")
class HovercraftControllerTest {

    private HovercraftController controller;

    @BeforeEach
    void setUp() {
        controller = new HovercraftController();
    }

    private HovercraftState atRest() {
        return new HovercraftState(0f, 0f, 0f, WeightCategory.LIGHT, 100f);
    }

    private HovercraftState moving(float vx, float vy, float vz) {
        return new HovercraftState(vx, vy, vz, WeightCategory.LIGHT, 100f);
    }

    // ── Test A: Neutral / No Drift ────────────────────────────────

    @Nested
    @DisplayName("Test A — Neutral input produces no movement")
    class TestA {

        @Test
        @DisplayName("Zero input over 40 ticks: position delta < 0.01, speed < 0.001, yaw unchanged")
        void neutralInput_noMovement() {
            var input = new HovercraftInput(0f, 0f, 0f, 90f);
            var state = atRest();

            float posX = 0f, posY = 0f, posZ = 0f;

            for (int tick = 0; tick < 40; tick++) {
                var output = controller.tick(input, state);
                posX += output.newVelX();
                posY += output.newVelY();
                posZ += output.newVelZ();
                state = new HovercraftState(output.newVelX(), output.newVelY(), output.newVelZ(),
                        WeightCategory.LIGHT, 100f);
            }

            float totalDelta = (float) Math.sqrt(posX * posX + posY * posY + posZ * posZ);
            assertTrue(totalDelta < 0.01f, "Position delta should be < 0.01 but was " + totalDelta);
            assertTrue(state.speed() < 0.001f, "Speed should be < 0.001 but was " + state.speed());
        }
    }

    // ── Test B: Forward ───────────────────────────────────────────

    @Nested
    @DisplayName("Test B — Forward input moves along player yaw direction")
    class TestB {

        @Test
        @DisplayName("moveForward=1, yaw=0° (south/+Z) → movement along +Z, lateral < 0.01")
        void forward_yaw0_movesSouth() {
            var input = new HovercraftInput(1f, 0f, 0f, 0f);
            var state = atRest();

            var output = controller.tick(input, state);

            assertTrue(output.newVelZ() > 0, "Should move in +Z direction");
            assertTrue(Math.abs(output.newVelX()) < 0.01f, "Lateral component should be < 0.01");
            assertEquals(0f, output.newVelY(), 0.01f, "Vertical should be 0");
        }

        @Test
        @DisplayName("moveForward=1, yaw=90° (west/-X) → movement along -X")
        void forward_yaw90_movesWest() {
            var input = new HovercraftInput(1f, 0f, 0f, 90f);
            var state = atRest();

            var output = controller.tick(input, state);

            assertTrue(output.newVelX() < 0, "Should move in -X direction (west)");
            assertTrue(Math.abs(output.newVelZ()) < 0.01f, "Forward Z component should be near 0");
        }
    }

    // ── Test C: Backward ──────────────────────────────────────────

    @Nested
    @DisplayName("Test C — Backward input moves opposite to player yaw")
    class TestC {

        @Test
        @DisplayName("moveForward=-1, yaw=0° → movement along -Z (opposite of south)")
        void backward_yaw0_movesNorth() {
            var input = new HovercraftInput(-1f, 0f, 0f, 0f);
            var state = atRest();

            var output = controller.tick(input, state);

            assertTrue(output.newVelZ() < 0, "Should move in -Z direction (north)");
            assertTrue(Math.abs(output.newVelX()) < 0.01f, "Lateral should be < 0.01");
        }
    }

    // ── Test D: Strafe ────────────────────────────────────────────

    @Nested
    @DisplayName("Test D — Strafe input moves orthogonally to look direction")
    class TestD {

        @Test
        @DisplayName("moveStrafe=1, yaw=0° → movement along +X (right), forward < 0.01")
        void strafeRight_yaw0_movesEast() {
            var input = new HovercraftInput(0f, 1f, 0f, 0f);
            var state = atRest();

            var output = controller.tick(input, state);

            assertTrue(output.newVelX() > 0, "Should move in +X (right)");
            assertTrue(Math.abs(output.newVelZ()) < 0.01f, "Forward component should be < 0.01");
        }

        @Test
        @DisplayName("moveStrafe=-1, yaw=0° → movement along -X (left)")
        void strafeLeft_yaw0_movesWest() {
            var input = new HovercraftInput(0f, -1f, 0f, 0f);
            var state = atRest();

            var output = controller.tick(input, state);

            assertTrue(output.newVelX() < 0, "Should move in -X (left)");
            assertTrue(Math.abs(output.newVelZ()) < 0.01f, "Forward component should be < 0.01");
        }
    }

    // ── Test E: Vertical ──────────────────────────────────────────

    @Nested
    @DisplayName("Test E — Vertical input affects only Y axis")
    class TestE {

        @Test
        @DisplayName("moveVertical=1 → only Y increases, X/Z < 0.01")
        void verticalUp_onlyYChanges() {
            var input = new HovercraftInput(0f, 0f, 1f, 45f);
            var state = atRest();

            var output = controller.tick(input, state);

            assertTrue(output.newVelY() > 0, "Y velocity should be positive");
            assertTrue(Math.abs(output.newVelX()) < 0.01f, "X should be < 0.01");
            assertTrue(Math.abs(output.newVelZ()) < 0.01f, "Z should be < 0.01");
        }

        @Test
        @DisplayName("moveVertical=-1 → only Y decreases, X/Z < 0.01")
        void verticalDown_onlyYChanges() {
            var input = new HovercraftInput(0f, 0f, -1f, 180f);
            var state = atRest();

            var output = controller.tick(input, state);

            assertTrue(output.newVelY() < 0, "Y velocity should be negative");
            assertTrue(Math.abs(output.newVelX()) < 0.01f, "X should be < 0.01");
            assertTrue(Math.abs(output.newVelZ()) < 0.01f, "Z should be < 0.01");
        }
    }

    // ── Test F: Combination ───────────────────────────────────────

    @Nested
    @DisplayName("Test F — Multi-axis combination produces correct vector sum")
    class TestF {

        @Test
        @DisplayName("Forward + strafe + vertical simultaneously: all axes contribute, no dominance")
        void combination_allAxesContribute() {
            var input = new HovercraftInput(1f, 1f, 1f, 0f);
            var state = atRest();

            var output = controller.tick(input, state);

            // All three components should be non-zero
            assertTrue(Math.abs(output.newVelX()) > 0.001f, "X should be non-zero (strafe)");
            assertTrue(Math.abs(output.newVelY()) > 0.001f, "Y should be non-zero (vertical)");
            assertTrue(Math.abs(output.newVelZ()) > 0.001f, "Z should be non-zero (forward)");
        }

        @Test
        @DisplayName("Diagonal normalization: forward+strafe combined speed ≤ max single-axis speed")
        void combination_diagonalNormalized() {
            float maxSpeed = WeightCategory.LIGHT.getMaxSpeed() / 20.0f;

            var forwardOnly = controller.tick(new HovercraftInput(1f, 0f, 0f, 0f), atRest());
            float forwardSpeed = (float) Math.sqrt(
                    forwardOnly.newVelX() * forwardOnly.newVelX() +
                    forwardOnly.newVelZ() * forwardOnly.newVelZ());

            var diagonal = controller.tick(new HovercraftInput(1f, 1f, 0f, 0f), atRest());
            float diagonalSpeed = (float) Math.sqrt(
                    diagonal.newVelX() * diagonal.newVelX() +
                    diagonal.newVelZ() * diagonal.newVelZ());

            assertTrue(diagonalSpeed <= maxSpeed + 0.001f,
                    "Diagonal horizontal speed " + diagonalSpeed + " should not exceed maxSpeed " + maxSpeed);
            assertEquals(forwardSpeed, diagonalSpeed, 0.01f,
                    "Diagonal speed should equal single-axis speed after normalization");
        }
    }

    // ── Test G: Deceleration ──────────────────────────────────────

    @Nested
    @DisplayName("Test G — Input release stops vehicle within 10 ticks")
    class TestG {

        @Test
        @DisplayName("Vehicle at max speed, all inputs released → speed < 0.001 within 10 ticks")
        void deceleration_stopsWithin10Ticks() {
            float maxSpeed = WeightCategory.LIGHT.getMaxSpeed() / 20.0f;
            var state = moving(maxSpeed, 0f, maxSpeed);
            var zeroInput = new HovercraftInput(0f, 0f, 0f, 0f);

            for (int tick = 0; tick < 10; tick++) {
                var output = controller.tick(zeroInput, state);
                state = new HovercraftState(output.newVelX(), output.newVelY(), output.newVelZ(),
                        WeightCategory.LIGHT, 100f);
            }

            assertTrue(state.speed() < 0.001f,
                    "Speed should be < 0.001 after 10 ticks but was " + state.speed());
        }

        @Test
        @DisplayName("Deceleration is monotonic — no oscillation or reversal")
        void deceleration_monotonic() {
            float maxSpeed = WeightCategory.LIGHT.getMaxSpeed() / 20.0f;
            var state = moving(maxSpeed, 0f, 0f);
            var zeroInput = new HovercraftInput(0f, 0f, 0f, 0f);

            float prevSpeed = state.speed();
            for (int tick = 0; tick < 10; tick++) {
                var output = controller.tick(zeroInput, state);
                state = new HovercraftState(output.newVelX(), output.newVelY(), output.newVelZ(),
                        WeightCategory.LIGHT, 100f);
                float currentSpeed = state.speed();
                assertTrue(currentSpeed <= prevSpeed,
                        "Speed must decrease monotonically: tick " + tick +
                        " speed=" + currentSpeed + " prev=" + prevSpeed);
                prevSpeed = currentSpeed;
            }
        }
    }

    // ── Test J: Look Direction ────────────────────────────────────

    @Nested
    @DisplayName("Test J — Same input with different yaw produces different direction")
    class TestJ {

        @Test
        @DisplayName("moveForward=1 with yaw=0° vs yaw=90° → different movement vectors")
        void sameInput_differentYaw_differentDirection() {
            var state = atRest();

            var output0 = controller.tick(new HovercraftInput(1f, 0f, 0f, 0f), state);
            var output90 = controller.tick(new HovercraftInput(1f, 0f, 0f, 90f), state);

            // At yaw=0: mostly +Z, near-zero X
            // At yaw=90: mostly -X, near-zero Z
            assertNotEquals(output0.newVelX(), output90.newVelX(), 0.01f,
                    "X velocity should differ between yaw 0° and 90°");
            assertNotEquals(output0.newVelZ(), output90.newVelZ(), 0.01f,
                    "Z velocity should differ between yaw 0° and 90°");
        }

        @Test
        @DisplayName("Movement follows yaw, not a fixed direction")
        void movement_followsYaw_notFixed() {
            var state = atRest();

            // At yaw=180° (north), forward should go -Z
            var output180 = controller.tick(new HovercraftInput(1f, 0f, 0f, 180f), state);
            assertTrue(output180.newVelZ() < 0, "At yaw=180° forward should move in -Z");
            assertTrue(Math.abs(output180.newVelX()) < 0.01f, "At yaw=180° X should be near 0");
        }
    }

    // ── Acceleration ─────────────────────────────────────────────

    @Nested
    @DisplayName("Acceleration — velocity ramps up gradually")
    class Acceleration {

        @Test
        @DisplayName("First tick of forward input does not reach max speed")
        void firstTick_doesNotReachMaxSpeed() {
            var input = new HovercraftInput(1f, 0f, 0f, 0f);
            var state = atRest();

            var output = controller.tick(input, state);

            float maxSpeed = WeightCategory.LIGHT.getMaxSpeed() / 20.0f;
            float outputSpeed = (float) Math.sqrt(
                    output.newVelX() * output.newVelX() + output.newVelZ() * output.newVelZ());

            assertTrue(outputSpeed < maxSpeed * 0.5f,
                    "First tick should be well below max speed, was " + outputSpeed + " vs max " + maxSpeed);
            assertTrue(outputSpeed > 0.001f,
                    "First tick should produce some movement");
        }

        @Test
        @DisplayName("Speed increases over multiple ticks toward max")
        void multiTick_speedIncreases() {
            var input = new HovercraftInput(1f, 0f, 0f, 0f);
            var state = atRest();

            float prevSpeed = 0f;
            for (int tick = 0; tick < 20; tick++) {
                var output = controller.tick(input, state);
                float speed = (float) Math.sqrt(
                        output.newVelX() * output.newVelX() + output.newVelZ() * output.newVelZ());
                assertTrue(speed >= prevSpeed - 0.001f,
                        "Speed should increase or stay at tick " + tick);
                prevSpeed = speed;
                state = new HovercraftState(output.newVelX(), output.newVelY(), output.newVelZ(),
                        WeightCategory.LIGHT, 100f);
            }
            float maxSpeed = WeightCategory.LIGHT.getMaxSpeed() / 20.0f;
            assertTrue(prevSpeed > maxSpeed * 0.8f,
                    "After 20 ticks should be near max speed, was " + prevSpeed);
        }
    }
}
