package dev.sharkengine.client;

import dev.sharkengine.net.HelmInputC2SPayload;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-side input handler for ship control.
 * Captures player input and sends it to the server via HelmInputC2SPayload.
 * 
 * <p>Controls:</p>
 * <ul>
 *   <li>W: Forward acceleration (0..1)</li>
 *   <li>A/D: Turn left/right (-1..+1)</li>
 *   <li>Leertaste: Climb up/vertical up (-1..+1)</li>
 *   <li>Shift: Climb down/vertical down (-1..+1)</li>
 * </ul>
 * 
 * <p>Network optimization: Sends updates at ~10Hz (every 2 ticks) or when input changes.</p>
 * 
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public final class HelmInputClient {
    private static float lastT = 0;      // Last throttle value
    private static float lastR = 0;      // Last turn value
    private static float lastF = 0;      // Last forward value (NEW)
    private static float lastV = 0;      // Last vertical value (NEW)
    private static int cooldown = 0;

    private HelmInputClient() {}

    /**
     * Initializes the input handler.
     * Registers tick event to capture player input and send to server.
     */
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cooldown > 0) cooldown--;

            LocalPlayer player = client.player;
            if (player == null) return;
            if (!(player.getVehicle() instanceof ShipEntity)) return;

            // ═══════════════════════════════════════════════════════════════════
            // FORWARD INPUT (W-Taste for acceleration)
            // ═══════════════════════════════════════════════════════════════════
            float forward = 0;
            if (client.options.keyUp.isDown()) {
                forward = 1.0f;  // W-Taste = volle Beschleunigung
            }

            // ═══════════════════════════════════════════════════════════════════
            // TURN INPUT (A/D-Tasten for rotation)
            // ═══════════════════════════════════════════════════════════════════
            float turn = 0;
            if (client.options.keyLeft.isDown()) turn += 1;  // A-Taste
            if (client.options.keyRight.isDown()) turn -= 1; // D-Taste

            // ═══════════════════════════════════════════════════════════════════
            // VERTICAL INPUT (Leertaste/Shift for climb)
            // ═══════════════════════════════════════════════════════════════════
            float vertical = 0;
            if (client.options.keyJump.isDown()) {
                vertical = 1.0f;   // Leertaste = aufsteigen
            }
            if (client.options.keyShift.isDown()) {
                vertical = -1.0f;  // Shift = absteigen
            }

            // Send only if changed or every few ticks (network optimization)
            boolean inputChanged = (vertical != lastT) || (turn != lastR) || 
                                   (forward != lastF) || (vertical != lastV);
            
            if (cooldown == 0 && inputChanged) {
                lastT = vertical;  // throttle = vertical input
                lastR = turn;
                lastF = forward;
                lastV = vertical;
                cooldown = 2; // ~10Hz at 20tps
                
                // Send payload with all 3 parameters
                ClientPlayNetworking.send(new HelmInputC2SPayload(vertical, turn, forward));
            }
        });
    }
}
