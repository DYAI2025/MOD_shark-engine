package dev.sharkengine.ship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FuelSystem utility class.
 * Tests fuel conversion, display formatting, and flight time calculations.
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
@DisplayName("FuelSystem Tests")
class FuelSystemTest {
    
    @Test
    @DisplayName("ENERGY_PER_WOOD constant is 100")
    void testEnergyPerWoodConstant() {
        assertEquals(100, FuelSystem.ENERGY_PER_WOOD);
    }
    
    @Test
    @DisplayName("MAX_FUEL constant is 100")
    void testMaxFuelConstant() {
        assertEquals(100, FuelSystem.MAX_FUEL);
    }
    
    @Test
    @DisplayName("woodToEnergy: 1 wood = 100 energy")
    void testWoodToEnergy() {
        assertEquals(100, FuelSystem.woodToEnergy(1));
        assertEquals(500, FuelSystem.woodToEnergy(5));
        assertEquals(1000, FuelSystem.woodToEnergy(10));
        assertEquals(0, FuelSystem.woodToEnergy(0));
    }
    
    @Test
    @DisplayName("energyToWood: Converts energy back to wood blocks")
    void testEnergyToWood() {
        assertEquals(1.0f, FuelSystem.energyToWood(100));
        assertEquals(5.0f, FuelSystem.energyToWood(500));
        assertEquals(0.5f, FuelSystem.energyToWood(50));
        assertEquals(0.0f, FuelSystem.energyToWood(0));
    }
    
    @Test
    @DisplayName("calculateRemainingFlightTime: Phase 1-2 (1 energy/sec nominal, "
            + "x0.25 FUEL_CONSUMPTION_RATE = 4x flight time, 2026-07-13 tuning)")
    void testCalculateRemainingFlightTime_Low() {
        // 100 energy / (1 * 0.25 energy/sec) = 400 seconds
        assertEquals(400, FuelSystem.calculateRemainingFlightTime(100, AccelerationPhase.PHASE_1));
        assertEquals(200, FuelSystem.calculateRemainingFlightTime(50, AccelerationPhase.PHASE_2));
    }

    @Test
    @DisplayName("calculateRemainingFlightTime: Phase 3-4 (2 energy/sec nominal, x0.25 rate)")
    void testCalculateRemainingFlightTime_Medium() {
        // 100 energy / (2 * 0.25 energy/sec) = 200 seconds
        assertEquals(200, FuelSystem.calculateRemainingFlightTime(100, AccelerationPhase.PHASE_3));
        assertEquals(100, FuelSystem.calculateRemainingFlightTime(50, AccelerationPhase.PHASE_4));
    }

    @Test
    @DisplayName("calculateRemainingFlightTime: Phase 5 (3 energy/sec nominal, x0.25 rate)")
    void testCalculateRemainingFlightTime_High() {
        // 100 energy / (3 * 0.25 energy/sec) = 133.33 -> 133 seconds
        assertEquals(133, FuelSystem.calculateRemainingFlightTime(100, AccelerationPhase.PHASE_5));
        assertEquals(66, FuelSystem.calculateRemainingFlightTime(50, AccelerationPhase.PHASE_5));
    }
    
    @Test
    @DisplayName("formatFuelDisplay: Shows correct percentage and bar")
    void testFormatFuelDisplay() {
        String display100 = FuelSystem.formatFuelDisplay(100, 100);
        assertTrue(display100.contains("100%"));
        assertTrue(display100.contains("██████████"));
        
        String display80 = FuelSystem.formatFuelDisplay(80, 100);
        assertTrue(display80.contains("80%"));
        
        String display50 = FuelSystem.formatFuelDisplay(50, 100);
        assertTrue(display50.contains("50%"));
        
        String display0 = FuelSystem.formatFuelDisplay(0, 100);
        assertTrue(display0.contains("0%"));
    }
    
    @Test
    @DisplayName("formatFuelDisplay: Uses correct colors")
    void testFormatFuelDisplay_Colors() {
        // Green for > 50%
        String displayGood = FuelSystem.formatFuelDisplay(80, 100);
        assertTrue(displayGood.contains("§a")); // Green
        
        // Yellow for 20-50%
        String displayWarning = FuelSystem.formatFuelDisplay(30, 100);
        assertTrue(displayWarning.contains("§e")); // Yellow
        
        // Red for < 20%
        String displayCritical = FuelSystem.formatFuelDisplay(10, 100);
        assertTrue(displayCritical.contains("§c")); // Red
    }
    
    @Test
    @DisplayName("formatRemainingTime: Shows seconds for short times (inputs scaled down from "
            + "pre-2026-07-13 values so the x0.25 rate still lands under the 60s boundary)")
    void testFormatRemainingTime_Seconds() {
        assertEquals("20s", FuelSystem.formatRemainingTime(5, AccelerationPhase.PHASE_1));
        assertEquals("20s", FuelSystem.formatRemainingTime(10, AccelerationPhase.PHASE_3));
    }

    @Test
    @DisplayName("formatRemainingTime: Shows minutes for long times (inputs scaled down from "
            + "pre-2026-07-13 values so the x0.25 rate lands on the same 1m/2m marks)")
    void testFormatRemainingTime_Minutes() {
        assertEquals("1m", FuelSystem.formatRemainingTime(15, AccelerationPhase.PHASE_1));
        assertEquals("2m", FuelSystem.formatRemainingTime(30, AccelerationPhase.PHASE_1));
    }
    
    @Test
    @DisplayName("formatRemainingTime: Shows infinity for very long times")
    void testFormatRemainingTime_Infinity() {
        assertEquals("∞", FuelSystem.formatRemainingTime(10000, AccelerationPhase.PHASE_1));
    }
    
    @Test
    @DisplayName("isCritical: Returns true for < 20% fuel")
    void testIsCritical() {
        assertTrue(FuelSystem.isCritical(10, 100));  // 10%
        assertTrue(FuelSystem.isCritical(19, 100));  // 19%
        assertFalse(FuelSystem.isCritical(20, 100)); // 20% – exactly at boundary, NOT critical
        assertFalse(FuelSystem.isCritical(50, 100)); // 50%
    }

    // ─── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("woodToEnergy: negative input returns 0 (cannot subtract fuel)")
    void testWoodToEnergy_Negative() {
        assertEquals(0, FuelSystem.woodToEnergy(-1));
        assertEquals(0, FuelSystem.woodToEnergy(-100));
    }

    @Test
    @DisplayName("woodToEnergy: 0 wood = 0 energy")
    void testWoodToEnergy_Zero() {
        assertEquals(0, FuelSystem.woodToEnergy(0));
    }

    @Test
    @DisplayName("formatFuelDisplay: handles maxFuel = 0 without crash")
    void testFormatFuelDisplay_ZeroMax() {
        // Must not throw ArithmeticException (division by zero)
        String result = FuelSystem.formatFuelDisplay(0, 0);
        assertNotNull(result);
        assertTrue(result.contains("ERROR"),
                "formatFuelDisplay with maxFuel=0 should return error string");
    }

    @Test
    @DisplayName("formatFuelDisplay: fuel > maxFuel clamps to 100%")
    void testFormatFuelDisplay_OverMax() {
        String result = FuelSystem.formatFuelDisplay(150, 100);
        assertTrue(result.contains("100%"), "Overflow fuel should display as 100%");
    }

    @Test
    @DisplayName("isCritical: maxFuel = 0 returns false without crash")
    void testIsCritical_ZeroMax() {
        assertFalse(FuelSystem.isCritical(0, 0));
    }

    @Test
    @DisplayName("calculateRemainingFlightTime: 0 fuel returns 0 seconds")
    void testRemainingTime_NoFuel() {
        assertEquals(0, FuelSystem.calculateRemainingFlightTime(0, AccelerationPhase.PHASE_1));
    }

    @Test
    @DisplayName("energyToWood: negative energy returns negative (informational only)")
    void testEnergyToWood_Negative() {
        // energyToWood is used for display; we do not clamp it (caller must validate)
        assertEquals(-1.0f, FuelSystem.energyToWood(-100));
    }
}
