package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Hovercraft Data Records")
class HovercraftDataRecordsTest {

    @Test
    @DisplayName("HovercraftInput: isZero returns true when all axes are zero")
    void input_isZero_allZero() {
        var input = new HovercraftInput(0.0f, 0.0f, 0.0f, 90.0f);
        assertTrue(input.isZero());
    }

    @Test
    @DisplayName("HovercraftInput: isZero returns false when forward is non-zero")
    void input_isZero_forwardNonZero() {
        var input = new HovercraftInput(0.5f, 0.0f, 0.0f, 0.0f);
        assertFalse(input.isZero());
    }

    @Test
    @DisplayName("HovercraftInput: isZero returns false when strafe is non-zero")
    void input_isZero_strafeNonZero() {
        var input = new HovercraftInput(0.0f, -1.0f, 0.0f, 0.0f);
        assertFalse(input.isZero());
    }

    @Test
    @DisplayName("HovercraftInput: isZero returns false when vertical is non-zero")
    void input_isZero_verticalNonZero() {
        var input = new HovercraftInput(0.0f, 0.0f, 1.0f, 0.0f);
        assertFalse(input.isZero());
    }

    @Test
    @DisplayName("HovercraftInput: accepts negative moveForward for backward flight")
    void input_acceptsNegativeForward() {
        var input = new HovercraftInput(-1.0f, 0.0f, 0.0f, 0.0f);
        assertEquals(-1.0f, input.moveForward());
    }

    @Test
    @DisplayName("HovercraftState: speed computes 3D magnitude correctly")
    void state_speed_3dMagnitude() {
        var state = new HovercraftState(3.0f, 4.0f, 0.0f, WeightCategory.LIGHT, 100.0f);
        assertEquals(5.0f, state.speed(), 0.001f);
    }

    @Test
    @DisplayName("HovercraftState: speed is zero when stationary")
    void state_speed_zero() {
        var state = new HovercraftState(0.0f, 0.0f, 0.0f, WeightCategory.MEDIUM, 50.0f);
        assertEquals(0.0f, state.speed(), 0.001f);
    }

    @Test
    @DisplayName("HovercraftOutput: stores velocity components correctly")
    void output_storesVelocity() {
        var output = new HovercraftOutput(1.5f, -0.3f, 0.7f);
        assertEquals(1.5f, output.newVelX());
        assertEquals(-0.3f, output.newVelY());
        assertEquals(0.7f, output.newVelZ());
    }

    @Test
    @DisplayName("Records have value equality")
    void records_valueEquality() {
        var a = new HovercraftInput(1.0f, 0.0f, 0.0f, 45.0f);
        var b = new HovercraftInput(1.0f, 0.0f, 0.0f, 45.0f);
        assertEquals(a, b);
    }
}
