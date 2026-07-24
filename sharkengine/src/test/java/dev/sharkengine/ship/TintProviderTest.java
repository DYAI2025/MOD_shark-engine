package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-019/AC-019 (T21): the tint provider — ONE function path ({@link TrailColor#fromDyeName}
 * + {@link TrailColor#rgb()}) exercised across all 16 dye values PLUS the no-component default,
 * proving one code path handles 17 cases rather than 17 separate paths (the test-plan's
 * falsifier for a per-color implementation). Pure logic, no Fabric bootstrap — resolution is
 * keyed by the dye's serialized name (primitive String), the T20 Preconditions-§4 decision,
 * so no {@code DyeColor} shim is needed (only the trivial {@code StringRepresentable} stub).
 *
 * <p>The RGB table is locked to the vanilla dye diffuse colors so a drive-by "adjustment" of a
 * single color (or a copy-paste duplicate) fails loudly.</p>
 */
@DisplayName("REQ-019/T21: trail tint provider — one path for 16 dyes + default")
class TintProviderTest {

    private static final Map<String, Integer> EXPECTED_DYE_RGB = Map.ofEntries(
            Map.entry("white", 0xF9FFFE),
            Map.entry("orange", 0xF9801D),
            Map.entry("magenta", 0xC74EBD),
            Map.entry("light_blue", 0x3AB3DA),
            Map.entry("yellow", 0xFED83D),
            Map.entry("lime", 0x80C71F),
            Map.entry("pink", 0xF38BAA),
            Map.entry("gray", 0x474F52),
            Map.entry("light_gray", 0x9D9D97),
            Map.entry("cyan", 0x169C9C),
            Map.entry("purple", 0x8932B8),
            Map.entry("blue", 0x3C44AA),
            Map.entry("brown", 0x835432),
            Map.entry("green", 0x5E7C16),
            Map.entry("red", 0xB02E26),
            Map.entry("black", 0x1D1D21));

    @Test
    @DisplayName("All 16 dye names resolve through the SAME path to distinct, colored values with the locked RGB")
    void allSixteenDyesResolveThroughOnePath() {
        Set<TrailColor> resolved = new HashSet<>();
        for (Map.Entry<String, Integer> expected : EXPECTED_DYE_RGB.entrySet()) {
            TrailColor color = TrailColor.fromDyeName(expected.getKey());
            assertTrue(color.isColored(), expected.getKey() + " must resolve to a colored value");
            assertEquals(expected.getValue().intValue(), color.rgb(),
                    "locked RGB drifted for " + expected.getKey());
            assertEquals(expected.getKey(), color.getSerializedName(),
                    "serialized name must round-trip for the blockstate property");
            resolved.add(color);
        }
        assertEquals(16, resolved.size(), "the 16 dyes must map to 16 DISTINCT trail colors");
        assertFalse(resolved.contains(TrailColor.NONE), "no dye may resolve to the default");
    }

    @Test
    @DisplayName("The 17th case: null/unknown/'none' conservatively resolve to NONE (default trail)")
    void defaultCaseResolvesToNone() {
        assertSame(TrailColor.NONE, TrailColor.fromDyeName(null),
                "no component (null) must mean the existing default trail");
        assertSame(TrailColor.NONE, TrailColor.fromDyeName("none"));
        assertSame(TrailColor.NONE, TrailColor.fromDyeName("chartreuse"),
                "an unknown dye name must never guess a color");
        assertFalse(TrailColor.NONE.isColored());
    }

    @Test
    @DisplayName("RGB float components stay in [0,1] for every value (renderer contract)")
    void rgbComponentsAreNormalized() {
        for (TrailColor color : TrailColor.values()) {
            assertTrue(color.red() >= 0.0f && color.red() <= 1.0f, color + " red out of range");
            assertTrue(color.green() >= 0.0f && color.green() <= 1.0f, color + " green out of range");
            assertTrue(color.blue() >= 0.0f && color.blue() <= 1.0f, color + " blue out of range");
        }
        assertEquals(17, TrailColor.values().length,
                "exactly 17 cases: 16 dyes + NONE — a new value means a design change, not a drive-by");
    }
}
