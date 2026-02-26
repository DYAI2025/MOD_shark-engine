package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShipAssemblyService Tests")
class ShipAssemblyServiceTest {

    @Test
    @DisplayName("containsThruster detects thruster blocks")
    void detectsThrusters() {
        boolean result = ThrusterRequirements.hasThruster(List.of("sharkengine:thruster"));
        assertTrue(result, "Thruster block should be detected");
    }

    @Test
    @DisplayName("containsThruster returns false without thrusters")
    void detectsMissingThrusters() {
        boolean result = ThrusterRequirements.hasThruster(List.of("minecraft:oak_planks"));
        assertFalse(result, "Structure without thruster must be reported as missing thrusters");
    }
}
