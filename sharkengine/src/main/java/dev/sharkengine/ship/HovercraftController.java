package dev.sharkengine.ship;

/**
 * Pure flight controller with no Minecraft/Fabric dependencies.
 * Computes the vehicle's new velocity from player input and current state.
 */
public final class HovercraftController {

    private static final float FRICTION_MULTIPLIER = 0.4f;
    private static final float VELOCITY_EPSILON = 0.001f;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    public HovercraftOutput tick(HovercraftInput input, HovercraftState state) {
        float newVelX;
        float newVelY;
        float newVelZ;

        if (input.isZero() || state.fuelLevel() <= 0.0f) {
            // Deceleration: apply friction to existing velocity
            newVelX = applyFriction(state.velX());
            newVelY = applyFriction(state.velY());
            newVelZ = applyFriction(state.velZ());
        } else {
            // Compute movement vector from player yaw
            float yawRad = input.playerYawDeg() * DEG_TO_RAD;
            float forwardX = -((float) Math.sin(yawRad));
            float forwardZ = (float) Math.cos(yawRad);
            float strafeX = (float) Math.cos(yawRad);
            float strafeZ = (float) Math.sin(yawRad);

            // Combine horizontal axes
            float hx = input.moveForward() * forwardX + input.moveStrafe() * strafeX;
            float hz = input.moveForward() * forwardZ + input.moveStrafe() * strafeZ;

            // Normalize if combined magnitude > 1.0 (DEC-diagonal-normalization)
            float hMag = (float) Math.sqrt(hx * hx + hz * hz);
            if (hMag > 1.0f) {
                hx /= hMag;
                hz /= hMag;
            }

            // Convert max speed from blocks/sec to blocks/tick (20 TPS)
            float maxSpeed = state.weightCategory().getMaxSpeed() / 20.0f;

            // Apply acceleration: blend toward target velocity
            newVelX = hx * maxSpeed;
            newVelY = input.moveVertical() * maxSpeed;
            newVelZ = hz * maxSpeed;
        }

        return new HovercraftOutput(newVelX, newVelY, newVelZ);
    }

    private float applyFriction(float vel) {
        vel *= FRICTION_MULTIPLIER;
        if (Math.abs(vel) < VELOCITY_EPSILON) {
            return 0.0f;
        }
        return vel;
    }
}
