package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-022/AC-022 (T19) — architecture-conformance gate, side (b), the NFR-003 regression guard:
 * {@code ShipEntity} must not grow {@code if (vehicleClass == VehicleClass.LAND)}/{@code WATER}-
 * shaped conditional branches. This closes the failure mode OPPOSITE to dead abstraction:
 * everything inlined into the entity, a growing class-switch instead of any seam extraction
 * (the tester's counter-thesis to AC-022's one-sided literal rule).
 *
 * <p><b>Documented baseline (2026-07-24, T19):</b> ZERO vehicle-class conditional branches in
 * all of {@code ShipEntity.java}. The scan is deliberately WHOLE-FILE, not tick()-only — NFR-003
 * names "tick() and direct callees", and every direct callee lives in this file; scanning the
 * whole file is the stricter, parse-free superset. If a legitimate class-conditional is ever
 * needed, it belongs in a policy/controller behind a seam (NFR-003, CAN-018), not here — and
 * whoever adds one must consciously confront this gate instead of silently growing a switch.</p>
 *
 * <p><b>Falsifiability note:</b> verified RED-capable by mutation during T19 development — a
 * temporary {@code if (vehicleClass == VehicleClass.LAND)} branch injected into the tick path
 * makes {@link #noVehicleClassComparisons} fail (see the T19 commit message for the recorded
 * run). Comment/string content is stripped before matching, so prose mentions can neither
 * trigger nor mask a finding.</p>
 */
@DisplayName("NFR-003/T19: ShipEntity grows no vehicle-class conditional branches")
class ShipEntityConditionalGrowthTest {

    private static final Path SHIP_ENTITY = Path.of("src/main/java/dev/sharkengine/ship/ShipEntity.java");

    private static String strippedSource() throws IOException {
        String source = Files.readString(SHIP_ENTITY);
        return source
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("//[^\n]*", " ")
                .replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"");
    }

    private static int count(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    @Test
    @DisplayName("Scanner sanity: ShipEntity source is actually read (no vacuous pass)")
    void scannerReadsShipEntity() throws IOException {
        assertTrue(Files.exists(SHIP_ENTITY), "ShipEntity.java not found — scanner path broken");
        String stripped = strippedSource();
        assertTrue(stripped.contains("class ShipEntity"), "stripped source lost the class declaration");
        assertTrue(stripped.length() > 10_000, "suspiciously small stripped source ("
                + stripped.length() + " chars) — stripping or read broken");
    }

    @Test
    @DisplayName("Baseline 0: no LAND/WATER literals reachable from ShipEntity code")
    void noLandWaterReferences() throws IOException {
        String stripped = strippedSource();
        Pattern landWater = Pattern.compile("VehicleClass\\s*\\.\\s*(LAND|WATER)\\b|case\\s+(LAND|WATER)\\b");
        assertEquals(0, count(landWater, stripped),
                "ShipEntity references VehicleClass.LAND/WATER — class-specific behavior belongs in a "
                        + "policy/controller behind a seam (NFR-003), not in the entity");
    }

    @Test
    @DisplayName("Baseline 0: no vehicleClass comparisons or switches in ShipEntity")
    void noVehicleClassComparisons() throws IOException {
        String stripped = strippedSource();
        Pattern comparison = Pattern.compile("vehicleClass\\s*[=!]=|switch\\s*\\(\\s*vehicleClass");
        assertEquals(0, count(comparison, stripped),
                "ShipEntity branches on vehicleClass — the documented T19 baseline is ZERO such "
                        + "branches; extract a seam instead of growing a class-switch (NFR-003, CAN-018)");
    }
}
