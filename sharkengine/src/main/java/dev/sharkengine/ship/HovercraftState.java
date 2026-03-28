package dev.sharkengine.ship;

/**
 * Snapshot of the vehicle's current physical state, read by the controller.
 *
 * @param velX           current X velocity (blocks/tick)
 * @param velY           current Y velocity (blocks/tick)
 * @param velZ           current Z velocity (blocks/tick)
 * @param weightCategory determines max speed cap
 * @param fuelLevel      current fuel (0.0–100.0); zero means no acceleration
 */
public record HovercraftState(
        float velX,
        float velY,
        float velZ,
        WeightCategory weightCategory,
        float fuelLevel
) {
    public float speed() {
        return (float) Math.sqrt(velX * velX + velY * velY + velZ * velZ);
    }
}
