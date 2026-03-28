package dev.sharkengine.ship;

/**
 * Immutable input for one tick of hovercraft flight computation.
 * All axes range from -1.0 to 1.0. Player yaw is in degrees.
 *
 * @param moveForward  forward (+) / backward (-) along player yaw
 * @param moveStrafe   left (-) / right (+) orthogonal to player yaw
 * @param moveVertical up (+) / down (-) on Y axis
 * @param playerYawDeg player's horizontal look direction in degrees
 */
public record HovercraftInput(
        float moveForward,
        float moveStrafe,
        float moveVertical,
        float playerYawDeg
) {
    public boolean isZero() {
        return moveForward == 0.0f && moveStrafe == 0.0f && moveVertical == 0.0f;
    }
}
