package dev.sharkengine.ship.part;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks every numeric vehicle-balance constant against
 * {@code docs/AIRCRAFT_CONCEPT_V2.md} §4 (part table) and §6 (rotor animation
 * constants) in one table-driven test (AIR-023).
 *
 * <p>Runs as plain JUnit — {@link VehicleBalance} has no Fabric/Minecraft
 * bootstrap dependency, same as {@link VehiclePartRegistry}. No
 * {@code junit-jupiter-params} dependency is declared in this project's
 * {@code build.gradle}, so the table is driven by a plain in-memory row list
 * rather than {@code @ParameterizedTest}.</p>
 */
@DisplayName("VehicleBalance Tests")
class VehicleBalanceTest {

    /** One row of the concept §4 balance table. */
    private record Row(String path, PartRole role, int mass, int lift, int thrust,
                        int drag, int fuelCapacity, VehiclePartDefinition.LiftMode liftMode) {
    }

    private static final List<Row> CONCEPT_TABLE = List.of(
            new Row("airframe_panel", PartRole.SKIN, 1, 0, 0, 0, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("fuselage_frame", PartRole.STRUCTURE, 2, 0, 0, 0, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("wing_root", PartRole.LIFT_SURFACE, 2, 3, 0, 1, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("wing_panel", PartRole.LIFT_SURFACE, 1, 4, 0, 1, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("wing_tip", PartRole.LIFT_SURFACE, 1, 2, 0, 0, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("tail_fin", PartRole.CONTROL_SURFACE, 1, 0, 0, 1, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("helicopter_engine", PartRole.PROPULSION, 6, 0, 40, 0, 0,
                    VehiclePartDefinition.LiftMode.ROTOR),
            new Row("rotor_hub", PartRole.ROTOR_HUB, 3, 0, 0, 0, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("rotor_blade", PartRole.ROTOR_BLADE, 1, 8, 0, 0, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("landing_skid", PartRole.LANDING_GEAR, 1, 0, 0, 0, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("fuel_tank", PartRole.FUEL_STORAGE, 3, 0, 0, 0, 100,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("thruster", PartRole.PROPULSION, 2, 0, 20, 0, 0,
                    VehiclePartDefinition.LiftMode.DIRECT),
            new Row("steering_wheel", PartRole.CONTROL, 2, 0, 0, 0, 0,
                    VehiclePartDefinition.LiftMode.NONE),
            new Row("bug", PartRole.CONTROL, 1, 0, 0, 0, 0,
                    VehiclePartDefinition.LiftMode.NONE)
    );

    @Test
    @DisplayName("every part in the concept §4 table matches VehicleBalance.PARTS exactly")
    void everyPartMatchesConceptTable() {
        for (Row row : CONCEPT_TABLE) {
            String id = "sharkengine:" + row.path();
            VehiclePartDefinition def = VehicleBalance.PARTS.get(id);
            assertNotNull(def, "concept §4 lists '" + row.path() + "' but VehicleBalance.PARTS has no entry for it");
            assertEquals(row.role(), def.role(), row.path() + ": role");
            assertEquals(row.mass(), def.mass(), row.path() + ": mass");
            assertEquals(row.lift(), def.lift(), row.path() + ": lift");
            assertEquals(row.thrust(), def.thrust(), row.path() + ": thrust");
            assertEquals(row.drag(), def.drag(), row.path() + ": drag");
            assertEquals(row.fuelCapacity(), def.fuelCapacity(), row.path() + ": fuelCapacity");
            assertEquals(row.liftMode(), def.liftMode(), row.path() + ": liftMode");
        }
    }

    @Test
    @DisplayName("PARTS has exactly the 14 named rows from concept §4 (the generic-block row is FALLBACK, not a table entry)")
    void partsTableHasExpectedSize() {
        assertEquals(CONCEPT_TABLE.size(), VehicleBalance.PARTS.size());
        assertEquals(14, VehicleBalance.PARTS.size());
    }

    // ─── weight-category thresholds (mass-based, concept §4) ──────────────────

    @Test
    @DisplayName("weight thresholds: LIGHT<=120, MEDIUM<=240, HEAVY<=360 (OVERLOADED > 360) — "
            + "4x raised 2026-07-13 from the original concept 30/60/90")
    void weightThresholdsMatchConcept() {
        assertEquals(120, VehicleBalance.LIGHT_MAX_MASS);
        assertEquals(240, VehicleBalance.MEDIUM_MAX_MASS);
        assertEquals(360, VehicleBalance.HEAVY_MAX_MASS);
    }

    // ─── rotor animation constants (concept §6) ────────────────────────────────

    @Test
    @DisplayName("rotor omega: idle 9deg/tick, full load 36deg/tick, spool over 40 ticks")
    void rotorConstantsMatchConcept() {
        assertEquals(9.0f, VehicleBalance.ROTOR_OMEGA_IDLE_DEG_PER_TICK);
        assertEquals(36.0f, VehicleBalance.ROTOR_OMEGA_FULL_DEG_PER_TICK);
        assertEquals(40, VehicleBalance.ROTOR_SPOOL_TICKS);
    }

    // ─── flight-feel bank/roll constants (FLR-004, docs/plans/flight-bank-roll.md) ──

    @Test
    @DisplayName("bank/roll: max 25deg, smoothing factor in (0,1]")
    void bankRollConstantsAreSane() {
        assertEquals(25.0f, VehicleBalance.MAX_BANK_DEG);
        assertEquals(0.15f, VehicleBalance.BANK_SMOOTHING_FACTOR);
        assertTrue(VehicleBalance.BANK_SMOOTHING_FACTOR > 0.0f && VehicleBalance.BANK_SMOOTHING_FACTOR <= 1.0f,
                "smoothing factor must be a valid lerp fraction");
    }

    // ─── flight-feel pitch constants (FLP-004, docs/plans/flight-pitch.md) ─────

    @Test
    @DisplayName("pitch: max 18deg (smaller than bank's 25deg), smoothing factor in (0,1]")
    void pitchConstantsAreSane() {
        assertEquals(18.0f, VehicleBalance.MAX_PITCH_DEG);
        assertEquals(0.15f, VehicleBalance.PITCH_SMOOTHING_FACTOR);
        assertTrue(VehicleBalance.MAX_PITCH_DEG < VehicleBalance.MAX_BANK_DEG,
                "pitch reads as a more extreme attitude change at the same angle than roll");
        assertTrue(VehicleBalance.PITCH_SMOOTHING_FACTOR > 0.0f && VehicleBalance.PITCH_SMOOTHING_FACTOR <= 1.0f,
                "smoothing factor must be a valid lerp fraction");
    }

    @Test
    @DisplayName("2-blade rotor lift (16) and 4-blade rotor lift (32) derive from rotor_blade lift=8, per concept §4")
    void rotorBladeLiftDerivesTwoAndFourBladeTotals() {
        int bladeLift = VehicleBalance.PARTS.get("sharkengine:rotor_blade").lift();
        assertEquals(16, 2 * bladeLift, "2-blade rotor should carry 16 mass of lift per concept §4");
        assertEquals(32, 4 * bladeLift, "4-blade rotor should carry 32 mass of lift per concept §4");
    }
}
