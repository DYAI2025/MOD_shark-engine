package dev.sharkengine.client;

import dev.sharkengine.net.HovercraftInputC2SPayload;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-side input handler for hovercraft flight control.
 * Captures keyboard and controller input, normalizes to [-1..1],
 * applies deadzone, and sends HovercraftInputC2SPayload to server.
 *
 * <p>Keyboard Controls:</p>
 * <ul>
 *   <li>W: Forward (+1)</li>
 *   <li>S: Backward (-1)</li>
 *   <li>A: Strafe left (-1)</li>
 *   <li>D: Strafe right (+1)</li>
 *   <li>Space: Ascend (+1)</li>
 *   <li>Shift: Descend (-1)</li>
 * </ul>
 *
 * <p>Controller Controls (Xbox-style):</p>
 * <ul>
 *   <li>Left Stick Y: Forward/backward</li>
 *   <li>Left Stick X: Strafe left/right</li>
 *   <li>Right Trigger: Ascend</li>
 *   <li>Left Trigger: Descend</li>
 * </ul>
 */
public final class HelmInputClient {
    private static int cooldown = 0;

    private HelmInputClient() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cooldown > 0) cooldown--;

            ControllerInput.pollController();

            LocalPlayer player = client.player;
            if (player == null) return;

            // Controller buttons (work outside ship for mounting)
            if (ControllerInput.isConnected()) {
                if (ControllerInput.isDismountPressed() && player.isPassenger()) {
                    player.input.shiftKeyDown = true;
                }
            }

            if (!(player.getVehicle() instanceof ShipEntity ship)) return;

            // ─── Keyboard input ──────────────────────────────────────
            float kbForward = 0f;
            if (client.options.keyUp.isDown()) kbForward += 1.0f;
            if (client.options.keyDown.isDown()) kbForward -= 1.0f;

            float kbStrafe = 0f;
            if (client.options.keyRight.isDown()) kbStrafe += 1.0f;
            if (client.options.keyLeft.isDown()) kbStrafe -= 1.0f;

            float kbVertical = 0f;
            if (client.options.keyJump.isDown()) kbVertical += 1.0f;
            if (client.options.keyShift.isDown()) kbVertical -= 1.0f;

            // ─── Controller input (merged: strongest signal wins) ────
            float ctrlForward = 0f;
            float ctrlStrafe = 0f;
            float ctrlVertical = 0f;

            if (ControllerInput.isConnected()) {
                ctrlForward = ControllerInput.getForward();
                ctrlStrafe = ControllerInput.getStrafe();
                ctrlVertical = ControllerInput.getVertical();
            }

            float moveForward = maxAbs(kbForward, ctrlForward);
            float moveStrafe = maxAbs(kbStrafe, ctrlStrafe);
            float moveVertical = maxAbs(kbVertical, ctrlVertical);
            float playerYaw = player.getYRot();

            // Send at ~10 Hz (every 2 ticks)
            if (cooldown == 0) {
                cooldown = 2;
                ClientPlayNetworking.send(
                        new HovercraftInputC2SPayload(moveForward, moveStrafe, moveVertical, playerYaw));
            }
        });
    }

    private static float maxAbs(float a, float b) {
        return Math.abs(a) >= Math.abs(b) ? a : b;
    }
}
