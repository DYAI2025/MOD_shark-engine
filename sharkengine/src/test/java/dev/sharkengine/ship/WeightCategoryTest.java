package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WeightCategory enum.
 * Covers all four weight categories, boundary values, canFly(), and warnings.
 *
 * <p>AIR-023: switched from block-count thresholds (20/40/60) to mass thresholds
 * per {@code docs/AIRCRAFT_CONCEPT_V2.md} §4 — {@code fromBlockCount} is
 * gone, {@code fromMass} is the only entry point, sourcing its boundaries from
 * {@link dev.sharkengine.ship.part.VehicleBalance} (single authority, no duplicated
 * thresholds). Mass thresholds were originally 30/60/90, raised 4x to 120/240/360
 * on 2026-07-13 (user request: larger builds should stay flyable).</p>
 */
@DisplayName("WeightCategory Tests")
class WeightCategoryTest {

    // ─── fromMass ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("fromMass: mass=0 → LIGHT (lower boundary)")
    void zeroMass_isLight() {
        assertEquals(WeightCategory.LIGHT, WeightCategory.fromMass(0));
    }

    @Test
    @DisplayName("fromMass: mass=1 → LIGHT")
    void oneMass_isLight() {
        assertEquals(WeightCategory.LIGHT, WeightCategory.fromMass(1));
    }

    @Test
    @DisplayName("fromMass: mass=120 → LIGHT (upper boundary)")
    void oneHundredTwentyMass_isLight() {
        assertEquals(WeightCategory.LIGHT, WeightCategory.fromMass(120));
    }

    @Test
    @DisplayName("fromMass: mass=121 → MEDIUM (lower boundary)")
    void oneHundredTwentyOneMass_isMedium() {
        assertEquals(WeightCategory.MEDIUM, WeightCategory.fromMass(121));
    }

    @Test
    @DisplayName("fromMass: mass=180 → MEDIUM (midpoint)")
    void oneHundredEightyMass_isMedium() {
        assertEquals(WeightCategory.MEDIUM, WeightCategory.fromMass(180));
    }

    @Test
    @DisplayName("fromMass: mass=240 → MEDIUM (upper boundary)")
    void twoHundredFortyMass_isMedium() {
        assertEquals(WeightCategory.MEDIUM, WeightCategory.fromMass(240));
    }

    @Test
    @DisplayName("fromMass: mass=241 → HEAVY (lower boundary)")
    void twoHundredFortyOneMass_isHeavy() {
        assertEquals(WeightCategory.HEAVY, WeightCategory.fromMass(241));
    }

    @Test
    @DisplayName("fromMass: mass=300 → HEAVY (midpoint)")
    void threeHundredMass_isHeavy() {
        assertEquals(WeightCategory.HEAVY, WeightCategory.fromMass(300));
    }

    @Test
    @DisplayName("fromMass: mass=360 → HEAVY (upper boundary)")
    void threeHundredSixtyMass_isHeavy() {
        assertEquals(WeightCategory.HEAVY, WeightCategory.fromMass(360));
    }

    @Test
    @DisplayName("fromMass: mass=361 → OVERLOADED (lower boundary)")
    void threeHundredSixtyOneMass_isOverloaded() {
        assertEquals(WeightCategory.OVERLOADED, WeightCategory.fromMass(361));
    }

    @Test
    @DisplayName("fromMass: mass=1536 (512 max-BFS blocks * mass 3 fuel_tank) → OVERLOADED")
    void largeMass_isOverloaded() {
        assertEquals(WeightCategory.OVERLOADED, WeightCategory.fromMass(1536));
    }

    @Test
    @DisplayName("fromMass: Integer.MAX_VALUE → OVERLOADED")
    void maxInt_isOverloaded() {
        assertEquals(WeightCategory.OVERLOADED, WeightCategory.fromMass(Integer.MAX_VALUE));
    }

    // ─── maxSpeed ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LIGHT maxSpeed = 30 blocks/sec")
    void light_maxSpeed_is30() {
        assertEquals(30.0f, WeightCategory.LIGHT.getMaxSpeed());
    }

    @Test
    @DisplayName("MEDIUM maxSpeed = 20 blocks/sec")
    void medium_maxSpeed_is20() {
        assertEquals(20.0f, WeightCategory.MEDIUM.getMaxSpeed());
    }

    @Test
    @DisplayName("HEAVY maxSpeed = 10 blocks/sec")
    void heavy_maxSpeed_is10() {
        assertEquals(10.0f, WeightCategory.HEAVY.getMaxSpeed());
    }

    @Test
    @DisplayName("OVERLOADED maxSpeed = 0 (cannot fly)")
    void overloaded_maxSpeed_is0() {
        assertEquals(0.0f, WeightCategory.OVERLOADED.getMaxSpeed());
    }

    // ─── canFly ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LIGHT can fly")
    void light_canFly() {
        assertTrue(WeightCategory.LIGHT.canFly());
    }

    @Test
    @DisplayName("MEDIUM can fly")
    void medium_canFly() {
        assertTrue(WeightCategory.MEDIUM.canFly());
    }

    @Test
    @DisplayName("HEAVY can fly (slowly)")
    void heavy_canFly() {
        assertTrue(WeightCategory.HEAVY.canFly());
    }

    @Test
    @DisplayName("OVERLOADED cannot fly")
    void overloaded_cannotFly() {
        assertFalse(WeightCategory.OVERLOADED.canFly());
    }

    // ─── warning messages ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("LIGHT has no warning")
    void light_noWarning() {
        assertNull(WeightCategory.LIGHT.getWarning());
    }

    @Test
    @DisplayName("MEDIUM has no warning")
    void medium_noWarning() {
        assertNull(WeightCategory.MEDIUM.getWarning());
    }

    @Test
    @DisplayName("HEAVY has a warning message")
    void heavy_hasWarning() {
        assertNotNull(WeightCategory.HEAVY.getWarning());
        assertFalse(WeightCategory.HEAVY.getWarning().isBlank());
    }

    @Test
    @DisplayName("OVERLOADED has an error-level warning message")
    void overloaded_hasWarning() {
        assertNotNull(WeightCategory.OVERLOADED.getWarning());
        // Error-level messages use red §c formatting
        assertTrue(WeightCategory.OVERLOADED.getWarning().contains("§c"),
                "Overloaded warning should use red color code §c");
    }

    // ─── min/max range sanity ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Categories cover the full integer range without gaps (mass thresholds 120/240/360)")
    void categoriesCoversFullRange_noGaps() {
        assertEquals(WeightCategory.LIGHT,      WeightCategory.fromMass(120));
        assertEquals(WeightCategory.MEDIUM,     WeightCategory.fromMass(121));
        assertEquals(WeightCategory.MEDIUM,     WeightCategory.fromMass(240));
        assertEquals(WeightCategory.HEAVY,      WeightCategory.fromMass(241));
        assertEquals(WeightCategory.HEAVY,      WeightCategory.fromMass(360));
        assertEquals(WeightCategory.OVERLOADED, WeightCategory.fromMass(361));
    }
}
