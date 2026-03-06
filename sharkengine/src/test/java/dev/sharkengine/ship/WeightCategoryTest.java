package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WeightCategory enum.
 * Covers all four weight categories, boundary values, canFly(), and warnings.
 */
@DisplayName("WeightCategory Tests")
class WeightCategoryTest {

    // ─── fromBlockCount ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("fromBlockCount: 0 blocks → LIGHT (lower boundary)")
    void zeroBlocks_isLight() {
        assertEquals(WeightCategory.LIGHT, WeightCategory.fromBlockCount(0));
    }

    @Test
    @DisplayName("fromBlockCount: 1 block → LIGHT")
    void oneBlock_isLight() {
        assertEquals(WeightCategory.LIGHT, WeightCategory.fromBlockCount(1));
    }

    @Test
    @DisplayName("fromBlockCount: 20 blocks → LIGHT (upper boundary)")
    void twentyBlocks_isLight() {
        assertEquals(WeightCategory.LIGHT, WeightCategory.fromBlockCount(20));
    }

    @Test
    @DisplayName("fromBlockCount: 21 blocks → MEDIUM (lower boundary)")
    void twentyOneBlocks_isMedium() {
        assertEquals(WeightCategory.MEDIUM, WeightCategory.fromBlockCount(21));
    }

    @Test
    @DisplayName("fromBlockCount: 30 blocks → MEDIUM (midpoint)")
    void thirtyBlocks_isMedium() {
        assertEquals(WeightCategory.MEDIUM, WeightCategory.fromBlockCount(30));
    }

    @Test
    @DisplayName("fromBlockCount: 40 blocks → MEDIUM (upper boundary)")
    void fortyBlocks_isMedium() {
        assertEquals(WeightCategory.MEDIUM, WeightCategory.fromBlockCount(40));
    }

    @Test
    @DisplayName("fromBlockCount: 41 blocks → HEAVY (lower boundary)")
    void fortyOneBlocks_isHeavy() {
        assertEquals(WeightCategory.HEAVY, WeightCategory.fromBlockCount(41));
    }

    @Test
    @DisplayName("fromBlockCount: 50 blocks → HEAVY (midpoint)")
    void fiftyBlocks_isHeavy() {
        assertEquals(WeightCategory.HEAVY, WeightCategory.fromBlockCount(50));
    }

    @Test
    @DisplayName("fromBlockCount: 60 blocks → HEAVY (upper boundary)")
    void sixtyBlocks_isHeavy() {
        assertEquals(WeightCategory.HEAVY, WeightCategory.fromBlockCount(60));
    }

    @Test
    @DisplayName("fromBlockCount: 61 blocks → OVERLOADED (lower boundary)")
    void sixtyOneBlocks_isOverloaded() {
        assertEquals(WeightCategory.OVERLOADED, WeightCategory.fromBlockCount(61));
    }

    @Test
    @DisplayName("fromBlockCount: 512 blocks → OVERLOADED (max BFS limit)")
    void maxBfsBlocks_isOverloaded() {
        assertEquals(WeightCategory.OVERLOADED, WeightCategory.fromBlockCount(512));
    }

    @Test
    @DisplayName("fromBlockCount: Integer.MAX_VALUE → OVERLOADED")
    void maxInt_isOverloaded() {
        assertEquals(WeightCategory.OVERLOADED, WeightCategory.fromBlockCount(Integer.MAX_VALUE));
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
    @DisplayName("Categories cover the full integer range without gaps")
    void categoriesCoversFullRange_noGaps() {
        // Boundaries: LIGHT 0-20, MEDIUM 21-40, HEAVY 41-60, OVERLOADED 61+
        assertEquals(WeightCategory.LIGHT,     WeightCategory.fromBlockCount(20));
        assertEquals(WeightCategory.MEDIUM,    WeightCategory.fromBlockCount(21));
        assertEquals(WeightCategory.MEDIUM,    WeightCategory.fromBlockCount(40));
        assertEquals(WeightCategory.HEAVY,     WeightCategory.fromBlockCount(41));
        assertEquals(WeightCategory.HEAVY,     WeightCategory.fromBlockCount(60));
        assertEquals(WeightCategory.OVERLOADED, WeightCategory.fromBlockCount(61));
    }
}
