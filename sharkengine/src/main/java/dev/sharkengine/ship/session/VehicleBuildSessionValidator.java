package dev.sharkengine.ship.session;

import java.util.EnumSet;
import java.util.Set;

/**
 * Pure REQ-003/AC-003 authorization logic — no Fabric bootstrap needed to unit-test, same
 * discipline as {@code dev.sharkengine.ship.part.ShipPartAnalyzer}.
 *
 * <p><b>Counter-thesis this exists to close</b> (test-plan REQ-003): a shallow implementation
 * could validate one field (e.g. session status) carefully while silently trusting another (e.g.
 * client-supplied dimension), or use a short-circuiting if-return chain where a later check's code
 * never actually runs across the whole test suite. {@link #validate} evaluates all six axes —
 * owner, dimension, distance, expiry, session id, consumed-status — unconditionally against the
 * same {@link VehicleBuildSession}/{@link VehicleBuildSessionRequest} pair, so each axis is
 * independently provable (a request violating exactly one axis reports exactly one reason) and no
 * axis's check can be masked by an earlier one returning first.</p>
 */
public final class VehicleBuildSessionValidator {

    /**
     * Maximum allowed Euclidean distance (blocks) between the requester's current position and
     * the session's wheel position.
     *
     * <p><b>ASSUMPTION, not a PRD number:</b> REQ-003/AC-003 mandate a distance check but — unlike
     * REQ-012/OQ-001's explicit "≤5 Blöcke" Edit Mode rule — do not pin a value for the build
     * session. 8.0 is a deliberately documented engineering default (wider than vanilla's ~4.5–6
     * block interact reach, to tolerate a player stepping back to see a small build), not a figure
     * taken from any PRD/AC text. Revisit if playtest data suggests otherwise.</p>
     */
    public static final double MAX_DISTANCE_BLOCKS = 8.0;

    private VehicleBuildSessionValidator() {}

    /**
     * Validates {@code request} against {@code session}. A {@code null} session (no session found
     * for whatever the request targeted) is reported as {@link VehicleBuildSessionRejectionReason#INVALID_SESSION_ID}
     * — there is nothing else to independently check against.
     */
    public static VehicleBuildSessionValidation validate(VehicleBuildSession session, VehicleBuildSessionRequest request) {
        if (session == null) {
            return new VehicleBuildSessionValidation(EnumSet.of(VehicleBuildSessionRejectionReason.INVALID_SESSION_ID));
        }

        Set<VehicleBuildSessionRejectionReason> reasons = EnumSet.noneOf(VehicleBuildSessionRejectionReason.class);

        // Every check below runs unconditionally -- deliberately not an if-return chain -- so a
        // fix to one axis can never leave another axis's check unexercised.
        if (!session.playerId().equals(request.requestingPlayerId())) {
            reasons.add(VehicleBuildSessionRejectionReason.NON_OWNER);
        }
        if (!session.dimensionId().equals(request.requestingDimensionId())) {
            reasons.add(VehicleBuildSessionRejectionReason.WRONG_DIMENSION);
        }
        if (session.wheelPos().distanceTo(request.requestingPlayerPos()) > MAX_DISTANCE_BLOCKS) {
            reasons.add(VehicleBuildSessionRejectionReason.OUT_OF_RANGE_DISTANCE);
        }
        if (request.nowMillis() >= session.expiresAtMillis()) {
            reasons.add(VehicleBuildSessionRejectionReason.EXPIRED);
        }
        if (request.providedSessionId() == null || !request.providedSessionId().equals(session.sessionId())) {
            reasons.add(VehicleBuildSessionRejectionReason.INVALID_SESSION_ID);
        }
        if (session.status() != VehicleBuildSessionStatus.ACTIVE) {
            reasons.add(VehicleBuildSessionRejectionReason.ALREADY_CONSUMED);
        }

        return new VehicleBuildSessionValidation(reasons);
    }
}
