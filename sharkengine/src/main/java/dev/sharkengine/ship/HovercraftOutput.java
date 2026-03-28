package dev.sharkengine.ship;

/**
 * Result of one tick's flight computation, applied by ShipEntity to entity velocity.
 *
 * @param newVelX new X velocity for next tick
 * @param newVelY new Y velocity for next tick
 * @param newVelZ new Z velocity for next tick
 */
public record HovercraftOutput(
        float newVelX,
        float newVelY,
        float newVelZ
) {
}
