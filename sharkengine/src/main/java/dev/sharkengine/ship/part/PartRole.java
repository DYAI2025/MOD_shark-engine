package dev.sharkengine.ship.part;

/**
 * The functional role a vehicle part plays in a ship's assembly.
 *
 * <p>Roles are the basis for property-based vehicle semantics (REQ-S1): assembly
 * validation, physics, and rendering reason about a block's role and its
 * {@link VehiclePartDefinition} kennwerte, never about the block's ID directly.
 * See {@code docs/AIRCRAFT_CONCEPT_V2.md} §3.3 for the design rationale.</p>
 */
public enum PartRole {
    /** Generic hull/frame block; also the fallback role for unregistered parts. */
    STRUCTURE,
    /** Outer skin panel; cosmetic/structural, no lift or thrust contribution. */
    SKIN,
    /** Fixed-wing lift-generating surface (wing root/panel/tip). */
    LIFT_SURFACE,
    /** Fixed-wing control surface (tail fin). */
    CONTROL_SURFACE,
    /** Thrust source; see {@link VehiclePartDefinition.LiftMode} for how it lifts. */
    PROPULSION,
    /** Rotor hub — pivot point for a rotor blade chain, requires an adjacent PROPULSION part. */
    ROTOR_HUB,
    /** Rotor blade — contributes rotor lift when part of a valid rotor topology. */
    ROTOR_BLADE,
    /** Landing gear/skid. */
    LANDING_GEAR,
    /** Fuel storage; extends a ship's fuel capacity. */
    FUEL_STORAGE,
    /** Steering/control input block (steering wheel, bug/bow marker). */
    CONTROL,
    /**
     * Generic crew seat that a rider can pilot from (REQ-005). Deliberately vehicle-class
     * agnostic: the role, not the block's id, is what {@code ShipAssemblyService}'s
     * seat-count validation checks (see that class's {@code StructureScan#canAssemble()}),
     * so this same role is directly reusable by future LAND/WATER vehicle profiles without
     * introducing a parallel AIR-specific seat concept. REQ-006 (seat *anchor* position
     * resolution) and REQ-009 (copilot seat, additional occupant) build on this role but are
     * out of scope for the role definition itself.
     */
    PILOT_SEAT,
    /**
     * Generic additional-passenger seat (REQ-009/T07: craftable copilot seat). A distinct
     * role from {@link #PILOT_SEAT}, deliberately: assembly's pilot-seat validation requires
     * exactly one {@code PILOT_SEAT}-role part ({@code StructureScan#canAssemble()}), and
     * reusing that same role for the copilot seat would make placing both a pilot seat and a
     * copilot seat in the same structure fail assembly as "multiple pilot seats" — the exact
     * opposite of what a craftable copilot seat needs. Unlike {@code PILOT_SEAT}'s single
     * deterministic front-of-wheel anchor (T06), a {@code COPILOT_SEAT} part's {@code
     * SeatAnchor} offset is simply wherever it was actually placed in the structure — see
     * {@code ShipAssemblyService#scanStructure}.
     */
    COPILOT_SEAT
}
