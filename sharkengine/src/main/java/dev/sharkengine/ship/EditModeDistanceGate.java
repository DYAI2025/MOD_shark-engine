package dev.sharkengine.ship;

/**
 * REQ-012/AC-012 (T12): pure, server-side-only "safe edit-mode gate" — decides whether an
 * already-assembled, already-launched {@link ShipEntity} may transition into AIR "Edit Mode".
 *
 * <p><b>OQ-001 (PRD, User decision 2026-07-18) resolved the "five-block rule" as EUCLIDEAN 3D
 * distance</b> from the requesting player to the vehicle's Control Anchor, &lt;=5 blocks in every
 * direction — <b>not</b> Manhattan and <b>not</b> Chebyshev. This matters because {@link
 * ShipAssemblyService#scanStructure} — the existing, shipped BFS assembly scan — already uses
 * MANHATTAN distance for its own, unrelated radius check ({@code current.distManhattan(wheelPos)
 * > MAX_RADIUS}), which is this codebase's existing idiom for "how far is this block" checks. That
 * idiom answers a different question (how far can a candidate BLOCK sit from the wheel during BFS
 * scanning) than this one (how far can a PLAYER stand from the vehicle to open Edit Mode) and is
 * deliberately NOT reused here: {@link #euclideanDistance} computes the genuine straight-line
 * {@code sqrt(dx^2+dy^2+dz^2)}. The test-plan's own sharpened case proves the two metrics disagree
 * at offset (3,3,0) — Euclidean ~4.24 (accepts) vs. Manhattan 6 (would wrongly reject) — see
 * {@code EditModeDistanceTest}.</p>
 *
 * <p>No Minecraft/Fabric imports — every method takes and returns only primitives/enums, matching
 * the {@code dev.sharkengine.ship.session}/{@link CockpitVisibility} pattern of running as a plain
 * JUnit test with zero game bootstrap. Turning a real {@link ShipEntity}/{@code ServerPlayer} pair
 * into the primitives this class needs (and applying its verdict) is {@link
 * ShipEntity#tryEnterEditMode}'s job, not this class's — including resolving the {@code
 * sameDimension} boolean {@link #evaluate} takes, since raw {@code dx}/{@code dy}/{@code dz} offsets
 * alone cannot distinguish "physically adjacent" from "numerically-matching coordinates in an
 * unrelated dimension" (see {@link Reason#REJECTED_WRONG_DIMENSION}).</p>
 */
public final class EditModeDistanceGate {

    /** OQ-001: the "five-block rule" boundary, inclusive ("&lt;=5 Blöcke in jede Richtung"). */
    public static final double MAX_DISTANCE_BLOCKS = 5.0;

    /**
     * Below this speed (blocks/sec), a ship counts as "stationary" for the edit-mode gate. Matches
     * the same literal threshold {@link ShipEntity#updatePhysics()} already uses to snap residual
     * speed to exactly zero when decelerating ({@code currentSpeed < 0.01f}), rather than inventing
     * a second, unrelated tolerance.
     */
    public static final float STATIONARY_SPEED_EPSILON = 0.01f;

    private EditModeDistanceGate() {
    }

    /**
     * Straight-line Euclidean distance for the given per-axis offsets — {@code
     * sqrt(dx^2+dy^2+dz^2)}. Deliberately NOT Manhattan ({@code |dx|+|dy|+|dz|}) and NOT Chebyshev
     * ({@code max(|dx|,|dy|,|dz|)}) — see this class's javadoc for why that distinction is the
     * entire point of this method existing separately from {@code BlockPos#distManhattan}.
     */
    public static double euclideanDistance(double dx, double dy, double dz) {
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Whether the given per-axis offset sits within the OQ-001 boundary ({@code &lt;=5}, inclusive). */
    public static boolean withinRange(double dx, double dy, double dz) {
        return euclideanDistance(dx, dy, dz) <= MAX_DISTANCE_BLOCKS;
    }

    /** Why {@link #evaluate} accepted or rejected an edit-mode open attempt. */
    public enum Reason {
        ACCEPTED,
        /** REQ-008 cross-reference: "Edit" is a pilot-exclusive command class; a non-pilot requester is rejected outright. */
        REJECTED_NOT_PILOT,
        /** The vehicle is destroyed (health &lt;= 0) — not a "safe" state. */
        REJECTED_DESTROYED,
        /** The vehicle is not stationary. */
        REJECTED_MOVING,
        /** Edit Mode is already open — not conflict-free. */
        REJECTED_CONFLICT,
        /**
         * The requester is not in the same dimension as the vehicle. Security fix (reviewer-reported,
         * attempt 1): raw per-axis offsets ({@code player.getX() - ship.getX()}, etc.) are computed from
         * numeric world coordinates alone, which repeat identically across every dimension (Overworld,
         * Nether, End all use the same coordinate space) — without this check first, a player standing
         * at a numerically-matching position in a completely different dimension from the vehicle would
         * evaluate to offset (0,0,0) and be wrongly ACCEPTED as "physically next to the ship." Mirrors
         * the same axis {@code VehicleBuildSessionRejectionReason#WRONG_DIMENSION} already checks for
         * the analogous build-session Control Anchor proximity gate (REQ-003/T02).
         */
        REJECTED_WRONG_DIMENSION,
        /** The requester is farther than {@link #MAX_DISTANCE_BLOCKS} (Euclidean) from the Control Anchor. */
        REJECTED_TOO_FAR
    }

    /**
     * The full REQ-012/AC-012 gate: combines the OQ-001 Euclidean distance check with the
     * stationary/not-destroyed/conflict-free/pilot-only preconditions named by AC-012 ("stationär,
     * sicher und konfliktfrei"). Any failing precondition rejects regardless of distance — a
     * damaged or moving vehicle (or a non-pilot, or an already-open Edit Mode) is rejected even at
     * distance 0, matching the test-plan's DoD.
     *
     * @param isPilot                requester is the vehicle's assigned pilot (REQ-008)
     * @param destroyed              vehicle health &lt;= 0
     * @param stationary             vehicle speed is below {@link #STATIONARY_SPEED_EPSILON}
     * @param editModeAlreadyActive  Edit Mode is already open on this vehicle
     * @param sameDimension          requester is in the same dimension/level as the vehicle — checked
     *                               before distance because {@code dx}/{@code dy}/{@code dz} are raw
     *                               numeric coordinate offsets that carry no dimension information of
     *                               their own (see {@link Reason#REJECTED_WRONG_DIMENSION})
     * @param dx                     requester X offset from the Control Anchor
     * @param dy                     requester Y offset from the Control Anchor
     * @param dz                     requester Z offset from the Control Anchor
     */
    public static Reason evaluate(boolean isPilot, boolean destroyed, boolean stationary,
                                   boolean editModeAlreadyActive, boolean sameDimension,
                                   double dx, double dy, double dz) {
        if (!isPilot) {
            return Reason.REJECTED_NOT_PILOT;
        }
        if (destroyed) {
            return Reason.REJECTED_DESTROYED;
        }
        if (!stationary) {
            return Reason.REJECTED_MOVING;
        }
        if (editModeAlreadyActive) {
            return Reason.REJECTED_CONFLICT;
        }
        if (!sameDimension) {
            return Reason.REJECTED_WRONG_DIMENSION;
        }
        if (!withinRange(dx, dy, dz)) {
            return Reason.REJECTED_TOO_FAR;
        }
        return Reason.ACCEPTED;
    }

    public static boolean isAccepted(Reason reason) {
        return reason == Reason.ACCEPTED;
    }
}
