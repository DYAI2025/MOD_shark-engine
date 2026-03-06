package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccelerationPhase enum.
 * Covers phase transitions, speed values, particle intensities, and boundary conditions.
 *
 * <p>Phase schedule (20 ticks = 1 second):</p>
 * <ul>
 *   <li>Phase 1: tick   0–39  →  5 blocks/sec (intensity 0.2)</li>
 *   <li>Phase 2: tick  40–79  → 15 blocks/sec (intensity 0.4)</li>
 *   <li>Phase 3: tick  80–99  → 20 blocks/sec (intensity 0.6)</li>
 *   <li>Phase 4: tick 100–119 → 25 blocks/sec (intensity 0.8)</li>
 *   <li>Phase 5: tick 120+    → 30 blocks/sec (intensity 1.0)</li>
 * </ul>
 */
@DisplayName("AccelerationPhase Tests")
class AccelerationPhaseTest {

    // ─── fromTick – lower boundaries ───────────────────────────────────────────

    @Test
    @DisplayName("fromTick(0) → PHASE_1 (start)")
    void tick0_isPhase1() {
        assertEquals(AccelerationPhase.PHASE_1, AccelerationPhase.fromTick(0));
    }

    @Test
    @DisplayName("fromTick(39) → PHASE_1 (last tick)")
    void tick39_isPhase1() {
        assertEquals(AccelerationPhase.PHASE_1, AccelerationPhase.fromTick(39));
    }

    @Test
    @DisplayName("fromTick(40) → PHASE_2 (transition)")
    void tick40_isPhase2() {
        assertEquals(AccelerationPhase.PHASE_2, AccelerationPhase.fromTick(40));
    }

    @Test
    @DisplayName("fromTick(79) → PHASE_2 (last tick)")
    void tick79_isPhase2() {
        assertEquals(AccelerationPhase.PHASE_2, AccelerationPhase.fromTick(79));
    }

    @Test
    @DisplayName("fromTick(80) → PHASE_3 (transition)")
    void tick80_isPhase3() {
        assertEquals(AccelerationPhase.PHASE_3, AccelerationPhase.fromTick(80));
    }

    @Test
    @DisplayName("fromTick(99) → PHASE_3 (last tick)")
    void tick99_isPhase3() {
        assertEquals(AccelerationPhase.PHASE_3, AccelerationPhase.fromTick(99));
    }

    @Test
    @DisplayName("fromTick(100) → PHASE_4 (transition)")
    void tick100_isPhase4() {
        assertEquals(AccelerationPhase.PHASE_4, AccelerationPhase.fromTick(100));
    }

    @Test
    @DisplayName("fromTick(119) → PHASE_4 (last tick)")
    void tick119_isPhase4() {
        assertEquals(AccelerationPhase.PHASE_4, AccelerationPhase.fromTick(119));
    }

    @Test
    @DisplayName("fromTick(120) → PHASE_5 (transition)")
    void tick120_isPhase5() {
        assertEquals(AccelerationPhase.PHASE_5, AccelerationPhase.fromTick(120));
    }

    @Test
    @DisplayName("fromTick(999) → PHASE_5 (no upper limit)")
    void tick999_isPhase5() {
        assertEquals(AccelerationPhase.PHASE_5, AccelerationPhase.fromTick(999));
    }

    @Test
    @DisplayName("fromTick(Integer.MAX_VALUE) → PHASE_5 (extreme case)")
    void tickMax_isPhase5() {
        assertEquals(AccelerationPhase.PHASE_5, AccelerationPhase.fromTick(Integer.MAX_VALUE));
    }

    // ─── speed values ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("PHASE_1 speed = 5 blocks/sec")
    void phase1_speed_is5() {
        assertEquals(5.0f, AccelerationPhase.PHASE_1.getSpeed());
    }

    @Test
    @DisplayName("PHASE_2 speed = 15 blocks/sec")
    void phase2_speed_is15() {
        assertEquals(15.0f, AccelerationPhase.PHASE_2.getSpeed());
    }

    @Test
    @DisplayName("PHASE_3 speed = 20 blocks/sec")
    void phase3_speed_is20() {
        assertEquals(20.0f, AccelerationPhase.PHASE_3.getSpeed());
    }

    @Test
    @DisplayName("PHASE_4 speed = 25 blocks/sec")
    void phase4_speed_is25() {
        assertEquals(25.0f, AccelerationPhase.PHASE_4.getSpeed());
    }

    @Test
    @DisplayName("PHASE_5 speed = 30 blocks/sec (max)")
    void phase5_speed_is30() {
        assertEquals(30.0f, AccelerationPhase.PHASE_5.getSpeed());
    }

    @Test
    @DisplayName("Speed values are strictly increasing across phases")
    void speedsAreMonotonicallyIncreasing() {
        AccelerationPhase[] phases = AccelerationPhase.values();
        for (int i = 1; i < phases.length; i++) {
            assertTrue(phases[i].getSpeed() > phases[i - 1].getSpeed(),
                    "Phase " + phases[i] + " speed should be > phase " + phases[i - 1] + " speed");
        }
    }

    // ─── particle intensity ────────────────────────────────────────────────────

    @Test
    @DisplayName("PHASE_1 particle intensity = 0.2 (20%)")
    void phase1_intensity_is02() {
        assertEquals(0.2f, AccelerationPhase.PHASE_1.getParticleIntensity(), 1e-6f);
    }

    @Test
    @DisplayName("PHASE_5 particle intensity = 1.0 (100%)")
    void phase5_intensity_is10() {
        assertEquals(1.0f, AccelerationPhase.PHASE_5.getParticleIntensity(), 1e-6f);
    }

    @Test
    @DisplayName("Particle intensities are strictly increasing across phases")
    void intensitiesAreMonotonicallyIncreasing() {
        AccelerationPhase[] phases = AccelerationPhase.values();
        for (int i = 1; i < phases.length; i++) {
            assertTrue(phases[i].getParticleIntensity() > phases[i - 1].getParticleIntensity(),
                    "Phase " + phases[i] + " intensity should be > phase " + phases[i - 1] + " intensity");
        }
    }

    @Test
    @DisplayName("All intensities are in range [0, 1]")
    void allIntensitiesInValidRange() {
        for (AccelerationPhase phase : AccelerationPhase.values()) {
            float intensity = phase.getParticleIntensity();
            assertTrue(intensity >= 0.0f && intensity <= 1.0f,
                    "Intensity for " + phase + " must be in [0, 1], was " + intensity);
        }
    }

    // ─── tick range consistency ────────────────────────────────────────────────

    @Test
    @DisplayName("PHASE_1 starts at tick 0 (no delay)")
    void phase1_startTick_is0() {
        assertEquals(0, AccelerationPhase.PHASE_1.getStartTick());
    }

    @Test
    @DisplayName("Each phase startTick == previous phase endTick (no gaps)")
    void phaseRanges_areContiguous() {
        assertEquals(AccelerationPhase.PHASE_1.getEndTick(), AccelerationPhase.PHASE_2.getStartTick());
        assertEquals(AccelerationPhase.PHASE_2.getEndTick(), AccelerationPhase.PHASE_3.getStartTick());
        assertEquals(AccelerationPhase.PHASE_3.getEndTick(), AccelerationPhase.PHASE_4.getStartTick());
        assertEquals(AccelerationPhase.PHASE_4.getEndTick(), AccelerationPhase.PHASE_5.getStartTick());
    }

    @Test
    @DisplayName("PHASE_5 endTick = -1 (open-ended, no upper limit)")
    void phase5_endTick_isNegative() {
        assertEquals(-1, AccelerationPhase.PHASE_5.getEndTick());
    }

    // ─── deceleration (fromTick with negative / 0) ─────────────────────────────

    @Test
    @DisplayName("fromTick(negative) → PHASE_1 (deceleration clamps to start)")
    void negativeTickClamps_toPhase1() {
        // Math.max(0, accelerationTicks - 3) prevents negative input reaching here,
        // but the method should still be safe.
        assertEquals(AccelerationPhase.PHASE_1, AccelerationPhase.fromTick(-1));
    }
}
