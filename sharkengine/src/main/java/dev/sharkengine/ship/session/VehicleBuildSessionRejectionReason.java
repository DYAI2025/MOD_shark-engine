package dev.sharkengine.ship.session;

/**
 * The six independent authorization axes from REQ-003/AC-003's test contract, one value each —
 * "Spieler, Dimension, Position, Distanz, Sessionstatus oder Ablaufzeit ungültig sind" plus the
 * session-id bearer-token check. {@link VehicleBuildSessionValidator#validate} evaluates every one
 * of these unconditionally (no short-circuiting), so a request can fail on more than one axis at
 * once and each axis is independently provable.
 */
public enum VehicleBuildSessionRejectionReason {
    /** {@code VehicleBuildSessionRequest#requestingPlayerId} does not match the session's owner. */
    NON_OWNER,
    /** The requester's current dimension does not match the session's dimension. */
    WRONG_DIMENSION,
    /** The requester is farther than {@link VehicleBuildSessionValidator#MAX_DISTANCE_BLOCKS} from the session's wheel position. */
    OUT_OF_RANGE_DISTANCE,
    /** {@code nowMillis} is at or past the session's {@code expiresAtMillis}. */
    EXPIRED,
    /** The client-supplied session id is null or does not match the session's own id. */
    INVALID_SESSION_ID,
    /** The session was already consumed by a prior successful request (duplicate/replay). */
    ALREADY_CONSUMED
}
