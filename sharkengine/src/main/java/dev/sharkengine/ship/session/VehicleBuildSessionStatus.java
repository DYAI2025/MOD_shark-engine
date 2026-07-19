package dev.sharkengine.ship.session;

/**
 * Lifecycle status of a {@link VehicleBuildSession} (REQ-003 "Status" field, PRD §REQ-003).
 * Deliberately separate from expiry: {@code ACTIVE}/{@code CONSUMED} tracks whether the session
 * has already been spent on a successful assembly (closes the "duplicate/replayed request" axis
 * of AC-003's authorization matrix), while expiry is a time-based check evaluated independently
 * by {@link VehicleBuildSessionValidator}.
 */
public enum VehicleBuildSessionStatus {
    /** Session has not yet been successfully used to assemble a vehicle. */
    ACTIVE,
    /** Session was already used for a successful assembly; any further use is a replay. */
    CONSUMED
}
