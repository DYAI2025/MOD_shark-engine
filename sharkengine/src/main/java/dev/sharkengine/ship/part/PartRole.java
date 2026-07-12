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
    CONTROL
}
