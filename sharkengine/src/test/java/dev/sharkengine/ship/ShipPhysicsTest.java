package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ShipPhysics utility class.
 * Tests all static calculation methods.
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
@DisplayName("ShipPhysics Tests")
class ShipPhysicsTest {
    
    @Test
    @DisplayName("calculateMaxSpeed: Light ships (1-20 blocks) get 30 blocks/sec")
    void testCalculateMaxSpeed_Light() {
        assertEquals(30.0f, ShipPhysics.calculateMaxSpeed(1));
        assertEquals(30.0f, ShipPhysics.calculateMaxSpeed(10));
        assertEquals(30.0f, ShipPhysics.calculateMaxSpeed(20));
    }
    
    @Test
    @DisplayName("calculateMaxSpeed: Medium ships (21-40 blocks) get 20 blocks/sec")
    void testCalculateMaxSpeed_Medium() {
        assertEquals(20.0f, ShipPhysics.calculateMaxSpeed(21));
        assertEquals(20.0f, ShipPhysics.calculateMaxSpeed(30));
        assertEquals(20.0f, ShipPhysics.calculateMaxSpeed(40));
    }
    
    @Test
    @DisplayName("calculateMaxSpeed: Heavy ships (41-60 blocks) get 10 blocks/sec")
    void testCalculateMaxSpeed_Heavy() {
        assertEquals(10.0f, ShipPhysics.calculateMaxSpeed(41));
        assertEquals(10.0f, ShipPhysics.calculateMaxSpeed(50));
        assertEquals(10.0f, ShipPhysics.calculateMaxSpeed(60));
    }
    
    @Test
    @DisplayName("calculateMaxSpeed: Overloaded ships (61+ blocks) cannot fly")
    void testCalculateMaxSpeed_Overloaded() {
        assertEquals(0.0f, ShipPhysics.calculateMaxSpeed(61));
        assertEquals(0.0f, ShipPhysics.calculateMaxSpeed(100));
        assertEquals(0.0f, ShipPhysics.calculateMaxSpeed(1000));
    }
    
    @Test
    @DisplayName("calculateHeightPenalty: Sea level (Y=0-100) has no penalty")
    void testCalculateHeightPenalty_SeaLevel() {
        assertEquals(1.0f, ShipPhysics.calculateHeightPenalty(0));
        assertEquals(1.0f, ShipPhysics.calculateHeightPenalty(50));
        assertEquals(1.0f, ShipPhysics.calculateHeightPenalty(99));
    }
    
    @Test
    @DisplayName("calculateHeightPenalty: Medium altitude (Y=100-150) has 20% penalty")
    void testCalculateHeightPenalty_MediumAltitude() {
        assertEquals(0.8f, ShipPhysics.calculateHeightPenalty(100));
        assertEquals(0.8f, ShipPhysics.calculateHeightPenalty(125));
        assertEquals(0.8f, ShipPhysics.calculateHeightPenalty(149));
    }
    
    @Test
    @DisplayName("calculateHeightPenalty: High altitude (Y=150-200) has 40% penalty")
    void testCalculateHeightPenalty_HighAltitude() {
        assertEquals(0.6f, ShipPhysics.calculateHeightPenalty(150));
        assertEquals(0.6f, ShipPhysics.calculateHeightPenalty(175));
        assertEquals(0.6f, ShipPhysics.calculateHeightPenalty(199));
    }
    
    @Test
    @DisplayName("calculateHeightPenalty: Very high altitude (Y=200+) has 60% penalty")
    void testCalculateHeightPenalty_VeryHighAltitude() {
        assertEquals(0.4f, ShipPhysics.calculateHeightPenalty(200));
        assertEquals(0.4f, ShipPhysics.calculateHeightPenalty(225));
        assertEquals(0.4f, ShipPhysics.calculateHeightPenalty(256));
    }
    
    @Test
    @DisplayName("calculatePhase: Returns correct phase based on tick count")
    void testCalculatePhase() {
        // Phase 1: 0-39 ticks (0-2 seconds)
        assertEquals(AccelerationPhase.PHASE_1, ShipPhysics.calculatePhase(0));
        assertEquals(AccelerationPhase.PHASE_1, ShipPhysics.calculatePhase(20));
        assertEquals(AccelerationPhase.PHASE_1, ShipPhysics.calculatePhase(39));
        
        // Phase 2: 40-79 ticks (2-4 seconds)
        assertEquals(AccelerationPhase.PHASE_2, ShipPhysics.calculatePhase(40));
        assertEquals(AccelerationPhase.PHASE_2, ShipPhysics.calculatePhase(60));
        assertEquals(AccelerationPhase.PHASE_2, ShipPhysics.calculatePhase(79));
        
        // Phase 3: 80-99 ticks (4-5 seconds)
        assertEquals(AccelerationPhase.PHASE_3, ShipPhysics.calculatePhase(80));
        assertEquals(AccelerationPhase.PHASE_3, ShipPhysics.calculatePhase(90));
        assertEquals(AccelerationPhase.PHASE_3, ShipPhysics.calculatePhase(99));
        
        // Phase 4: 100-119 ticks (5-6 seconds)
        assertEquals(AccelerationPhase.PHASE_4, ShipPhysics.calculatePhase(100));
        assertEquals(AccelerationPhase.PHASE_4, ShipPhysics.calculatePhase(110));
        assertEquals(AccelerationPhase.PHASE_4, ShipPhysics.calculatePhase(119));
        
        // Phase 5: 120+ ticks (6+ seconds)
        assertEquals(AccelerationPhase.PHASE_5, ShipPhysics.calculatePhase(120));
        assertEquals(AccelerationPhase.PHASE_5, ShipPhysics.calculatePhase(200));
        assertEquals(AccelerationPhase.PHASE_5, ShipPhysics.calculatePhase(1000));
    }
    
    @Test
    @DisplayName("calculateFuelConsumption: Phase 1-2 use 1 energy/sec")
    void testCalculateFuelConsumption_Low() {
        assertEquals(1, ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_1));
        assertEquals(1, ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_2));
    }
    
    @Test
    @DisplayName("calculateFuelConsumption: Phase 3-4 use 2 energy/sec")
    void testCalculateFuelConsumption_Medium() {
        assertEquals(2, ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_3));
        assertEquals(2, ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_4));
    }
    
    @Test
    @DisplayName("calculateFuelConsumption: Phase 5 uses 3 energy/sec")
    void testCalculateFuelConsumption_High() {
        assertEquals(3, ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_5));
    }
    
    @Test
    @DisplayName("checkCollision: Single block probe detects solids")
    void testCheckCollision_Solid() {
        boolean result = ShipPhysics.hasCollision(
                new ShipPhysics.BlockVector(0, 0, 0),
                Collections.emptyList(),
                vector -> vector.x() == 0 && vector.y() == 0 && vector.z() == 0
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("checkCollision: Blueprint footprint stops on collisions")
    void testCheckCollision_BlueprintHitsSolid() {
        List<ShipPhysics.BlockVector> offsets = List.of(new ShipPhysics.BlockVector(0, 0, 0));

        boolean result = ShipPhysics.hasCollision(
                new ShipPhysics.BlockVector(5, 5, 5),
                offsets,
                vector -> vector.x() == 5 && vector.y() == 5 && vector.z() == 5
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("checkCollision: Blueprint moves freely through air")
    void testCheckCollision_BlueprintNoSolid() {
        List<ShipPhysics.BlockVector> offsets = List.of(new ShipPhysics.BlockVector(1, 0, 0));

        boolean result = ShipPhysics.hasCollision(
                new ShipPhysics.BlockVector(0, 0, 0),
                offsets,
                vector -> false
        );

        assertFalse(result);
    }
    
    @Test
    @DisplayName("calculateEffectiveSpeed: Combines all modifiers correctly")
    void testCalculateEffectiveSpeed() {
        // Base speed: 30, Height: 100% (1.0), Weight: 100% (1.0)
        float speed1 = ShipPhysics.calculateEffectiveSpeed(30.0f, 1.0f, 1.0f);
        assertEquals(30.0f, speed1);
        
        // Base speed: 30, Height: 40% (0.4), Weight: 100% (1.0)
        float speed2 = ShipPhysics.calculateEffectiveSpeed(30.0f, 0.4f, 1.0f);
        assertEquals(12.0f, speed2);
        
        // Base speed: 30, Height: 40% (0.4), Weight: 33% (0.33)
        float speed3 = ShipPhysics.calculateEffectiveSpeed(30.0f, 0.4f, 0.33f);
        assertEquals(3.96f, speed3, 0.01f);
    }
}
