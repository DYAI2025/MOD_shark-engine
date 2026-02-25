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
    @DisplayName("calculateRemainingFlightTime: Phase 1-2 (1 energy/sec)")
    void testCalculateRemainingFlightTime_Low() {
        // 100 energy / 1 energy/sec = 100 seconds
        assertEquals(100, FuelSystem.calculateRemainingFlightTime(100, AccelerationPhase.PHASE_1));
        assertEquals(50, FuelSystem.calculateRemainingFlightTime(50, AccelerationPhase.PHASE_2));
    }
    
    @Test
    @DisplayName("calculateRemainingFlightTime: Phase 3-4 (2 energy/sec)")
    void testCalculateRemainingFlightTime_Medium() {
        // 100 energy / 2 energy/sec = 50 seconds
        assertEquals(50, FuelSystem.calculateRemainingFlightTime(100, AccelerationPhase.PHASE_3));
        assertEquals(25, FuelSystem.calculateRemainingFlightTime(50, AccelerationPhase.PHASE_4));
    }
    
    @Test
    @DisplayName("calculateRemainingFlightTime: Phase 5 (3 energy/sec)")
    void testCalculateRemainingFlightTime_High() {
        // 100 energy / 3 energy/sec = 33 seconds
        assertEquals(33, FuelSystem.calculateRemainingFlightTime(100, AccelerationPhase.PHASE_5));
        assertEquals(16, FuelSystem.calculateRemainingFlightTime(50, AccelerationPhase.PHASE_5));
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
    @DisplayName("formatRemainingTime: Shows seconds for short times")
    void testFormatRemainingTime_Seconds() {
        assertEquals("30s", FuelSystem.formatRemainingTime(30, AccelerationPhase.PHASE_1));
        assertEquals("15s", FuelSystem.formatRemainingTime(30, AccelerationPhase.PHASE_3));
    }
    
    @Test
    @DisplayName("formatRemainingTime: Shows minutes for long times")
    void testFormatRemainingTime_Minutes() {
        assertEquals("1m", FuelSystem.formatRemainingTime(120, AccelerationPhase.PHASE_1));
        assertEquals("2m", FuelSystem.formatRemainingTime(240, AccelerationPhase.PHASE_1));
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
        assertFalse(FuelSystem.isCritical(20, 100)); // 20%
        assertFalse(FuelSystem.isCritical(50, 100)); // 50%
    }
}
