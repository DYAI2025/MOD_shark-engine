package dev.sharkengine.ship.part;

import dev.sharkengine.SharkEngineMod;

import java.util.Map;

/**
 * Single authority for every numeric vehicle-balance constant (REQ-S4).
 *
 * <p>Source of truth is {@code docs/AIRCRAFT_CONCEPT_V2.md} §4 ("Bauteilsatz mit
 * konkreten Kennwerten") for {@link #PARTS} and the weight-category thresholds, and
 * §6 ("Rotor-Topologie & Animation") for the rotor spool/angular-velocity constants.
 * Both docs sections list the same numbers this class hard-codes — if the numbers
 * ever need to change, change them here and in the table-driven
 * {@code VehicleBalanceTest}, nowhere else (see the "Balance numbers feel wrong in
 * play" risk mitigation in {@code docs/plans/aircraft-extension-implementation.md}).</p>
 *
 * <p>{@link VehiclePartRegistry} resolves every block id through {@link #PARTS}
 * directly — there is deliberately no second, separately-maintained definitions map.
 * {@link #PARTS} includes rows for parts that are not registered as
 * {@code ModBlocks} yet (the Slice 3/4/5 helicopter and fixed-wing parts): resolving
 * an id that has no corresponding block simply never happens at runtime, but having
 * the full table available now lets balance numbers be locked by unit tests ahead of
 * the blocks that will use them (AIR-040+).</p>
 */
public final class VehicleBalance {

    private VehicleBalance() {}

    // ═══════════════════════════════════════════════════════════════════
    // WEIGHT-CATEGORY THRESHOLDS (mass-based, concept §4)
    //
    // Originally "LIGHT ≤ 30, MEDIUM ≤ 60, HEAVY ≤ 90, OVERLOADED > 90" per
    // the concept doc's AIR-023 mass migration. Raised 4x (2026-07-13, user
    // request: "lass uns das zulässige maximal Gewicht um das vierfache
    // anheben, so dass der Spieler auch große Flugzeuge bauen kann") so
    // larger builds stay flyable — docs/AIRCRAFT_CONCEPT_V2.md §4 updated to
    // match, these constants are the current source of truth, not the
    // original concept numbers. WeightCategory.fromMass is the only reader
    // of these three constants; ShipPhysics.calculateMaxSpeed derives its
    // speed from WeightCategory instead of re-declaring its own thresholds
    // (single authority, AIR-023).
    // ═══════════════════════════════════════════════════════════════════

    public static final int LIGHT_MAX_MASS = 120;
    public static final int MEDIUM_MAX_MASS = 240;
    public static final int HEAVY_MAX_MASS = 360;
    // OVERLOADED: mass > HEAVY_MAX_MASS (unbounded above)

    // ═══════════════════════════════════════════════════════════════════
    // ROTOR ANIMATION CONSTANTS (concept §6)
    //
    // Not yet consumed anywhere (that lands with the render/RPM-sync work
    // in AIR-051/AIR-052) — declared here now, ahead of use, so the
    // numbers are locked by VehicleBalanceTest instead of being
    // reintroduced/re-guessed when that later slice starts.
    // ═══════════════════════════════════════════════════════════════════

    /** Idle rotor angular velocity: 9°/tick (0.5 rotations/sec). */
    public static final float ROTOR_OMEGA_IDLE_DEG_PER_TICK = 9.0f;

    /** Full-load rotor angular velocity: 36°/tick (2 rotations/sec). */
    public static final float ROTOR_OMEGA_FULL_DEG_PER_TICK = 36.0f;

    /** Linear spool-up/spool-down duration between idle and full load, in ticks. */
    public static final int ROTOR_SPOOL_TICKS = 40;

    // ═══════════════════════════════════════════════════════════════════
    // FLIGHT-FEEL: BANK/ROLL (FLR-004, docs/plans/flight-bank-roll.md)
    //
    // Purely cosmetic (ShipEntityRenderer only) — see that plan doc's
    // Non-goals for why this deliberately does not touch collision/physics.
    // ═══════════════════════════════════════════════════════════════════

    /** Maximum visual bank angle at full turn input, in degrees. */
    public static final float MAX_BANK_DEG = 25.0f;

    /**
     * Per-tick interpolation fraction the renderer's smoothed roll state
     * moves toward its target each tick (0..1 — higher is snappier, lower is
     * floatier). Not a duration; matches the {@code Mth.lerp}-style factor
     * convention already used elsewhere in this codebase (e.g.
     * {@code ShipEntity.updatePhysics}'s speed lerp).
     */
    public static final float BANK_SMOOTHING_FACTOR = 0.15f;

    // ═══════════════════════════════════════════════════════════════════
    // FLIGHT-FEEL: PITCH (FLP-004, docs/plans/flight-pitch.md)
    //
    // Purely cosmetic (ShipEntityRenderer only), same as bank/roll above —
    // see that plan doc's Non-goals for why this deliberately does not
    // touch vertical-speed physics/collision.
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Maximum visual pitch angle at full vertical input, in degrees. Smaller
     * than {@link #MAX_BANK_DEG}'s 25° — pitch reads as a more extreme
     * attitude change at the same angle than roll does.
     */
    public static final float MAX_PITCH_DEG = 18.0f;

    /** Per-tick interpolation fraction for pitch, same convention as {@link #BANK_SMOOTHING_FACTOR}. */
    public static final float PITCH_SMOOTHING_FACTOR = 0.15f;

    // ═══════════════════════════════════════════════════════════════════
    // FUEL TUNING
    //
    // User feedback 2026-07-13: "Der Spritverbrauch ist sehr hoch aktuell.
    // Bitte den Verbrauch um das vierfache reduzieren, so dass er vier mal
    // länger hält." Deliberately a separate multiplier rather than changing
    // ShipPhysics.calculateFuelConsumption's per-phase 1/2/3 nominal values
    // directly — those are locked by ShipPhysicsTest as the documented
    // "energy/sec" balance-table numbers; this constant is the single place
    // to retune overall fuel duration without touching that contract.
    // Consumed by ShipEntity#tick() (via a fractional accumulator, since
    // fuelLevel is an int and 1/4 of "1 energy/sec" isn't a whole unit) and
    // FuelSystem#calculateRemainingFlightTime (so the displayed time matches
    // actual burn rate).
    // ═══════════════════════════════════════════════════════════════════

    /** Multiplies {@code ShipPhysics.calculateFuelConsumption}'s nominal per-phase rate. 0.25 = 4x flight time. */
    public static final float FUEL_CONSUMPTION_RATE = 0.25f;

    // ═══════════════════════════════════════════════════════════════════
    // PART BALANCE TABLE (concept §4)
    //
    // The concept table's dash ("–") cells mean "no contribution" (0), and
    // the generic *(genericher ship_eligible-Block)* row has no id of its
    // own — it is VehiclePartRegistry.FALLBACK, not an entry here.
    // ═══════════════════════════════════════════════════════════════════

    public static final Map<String, VehiclePartDefinition> PARTS = Map.ofEntries(
            // ─── Slice 3: structure/skin (no block yet — AIR-040) ─────────────
            Map.entry(id("airframe_panel"), new VehiclePartDefinition(
                    PartRole.SKIN, 1, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE)),
            Map.entry(id("fuselage_frame"), new VehiclePartDefinition(
                    PartRole.STRUCTURE, 2, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE)),

            // ─── Slice 5: fixed-wing (no block yet — AIR-041) ──────────────────
            Map.entry(id("wing_root"), new VehiclePartDefinition(
                    PartRole.LIFT_SURFACE, 2, 3, 0, 1, 0, VehiclePartDefinition.LiftMode.NONE)),
            Map.entry(id("wing_panel"), new VehiclePartDefinition(
                    PartRole.LIFT_SURFACE, 1, 4, 0, 1, 0, VehiclePartDefinition.LiftMode.NONE)),
            Map.entry(id("wing_tip"), new VehiclePartDefinition(
                    PartRole.LIFT_SURFACE, 1, 2, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE)),
            Map.entry(id("tail_fin"), new VehiclePartDefinition(
                    PartRole.CONTROL_SURFACE, 1, 0, 0, 1, 0, VehiclePartDefinition.LiftMode.NONE)),

            // ─── Slice 3/4: helicopter (no block yet — AIR-040) ────────────────
            Map.entry(id("helicopter_engine"), new VehiclePartDefinition(
                    PartRole.PROPULSION, 6, 0, 40, 0, 0, VehiclePartDefinition.LiftMode.ROTOR)),
            Map.entry(id("rotor_hub"), new VehiclePartDefinition(
                    PartRole.ROTOR_HUB, 3, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE)),
            Map.entry(id("rotor_blade"), new VehiclePartDefinition(
                    PartRole.ROTOR_BLADE, 1, 8, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE)),
            Map.entry(id("landing_skid"), new VehiclePartDefinition(
                    PartRole.LANDING_GEAR, 1, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE)),
            Map.entry(id("fuel_tank"), new VehiclePartDefinition(
                    PartRole.FUEL_STORAGE, 3, 0, 0, 0, 100, VehiclePartDefinition.LiftMode.NONE)),

            // ─── Legacy parts (already registered blocks, AIR-002/AIR-020) ─────
            Map.entry(id("thruster"), new VehiclePartDefinition(
                    PartRole.PROPULSION, 2, 0, 20, 0, 0, VehiclePartDefinition.LiftMode.DIRECT)),
            Map.entry(id("steering_wheel"), new VehiclePartDefinition(
                    PartRole.CONTROL, 2, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE)),
            Map.entry(id("bug"), new VehiclePartDefinition(
                    PartRole.CONTROL, 1, 0, 0, 0, 0, VehiclePartDefinition.LiftMode.NONE))
    );

    private static String id(String path) {
        return SharkEngineMod.MOD_ID + ":" + path;
    }
}
