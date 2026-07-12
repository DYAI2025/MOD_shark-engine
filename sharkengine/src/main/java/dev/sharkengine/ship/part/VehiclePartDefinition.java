package dev.sharkengine.ship.part;

import java.util.Objects;

/**
 * The balancing kennwerte for a single vehicle part, keyed by role.
 *
 * <p>Values (mass/lift/thrust/drag/fuelCapacity) come from the balance table in
 * {@code docs/AIRCRAFT_CONCEPT_V2.md} §4 — that table, not this class, is the source
 * of truth; a future {@code VehicleBalance} (AIR-023) will centralize all such
 * constants in one place.</p>
 *
 * @param role          functional role this part plays in assembly/physics/rendering
 * @param mass          contribution to total ship mass (drives {@code WeightCategory})
 * @param lift          contribution to total lift (rotor blades, wing surfaces)
 * @param thrust        contribution to total thrust (propulsion parts)
 * @param drag          contribution to total drag (fixed-wing surfaces)
 * @param fuelCapacity  contribution to total fuel capacity (fuel storage parts)
 * @param liftMode      how a PROPULSION part lifts the craft; {@link LiftMode#NONE} for
 *                      every non-PROPULSION role
 */
public record VehiclePartDefinition(
        PartRole role,
        int mass,
        int lift,
        int thrust,
        int drag,
        int fuelCapacity,
        LiftMode liftMode
) {
    public VehiclePartDefinition {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(liftMode, "liftMode must not be null");
    }

    /**
     * How a PROPULSION part lifts the craft (REQ-S1: distinguished by definition,
     * never by block ID comparison).
     */
    public enum LiftMode {
        /** Not a propulsion source — every non-PROPULSION role uses this. */
        NONE,
        /** Engine lifts the craft directly (legacy thruster / jet path). */
        DIRECT,
        /** Engine drives rotors only; does not lift on its own (helicopter_engine). */
        ROTOR
    }
}
