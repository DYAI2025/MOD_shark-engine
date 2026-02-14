package dev.sharkengine.client;

import dev.sharkengine.net.HelmInputC2SPayload;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class HelmInputClient {
    private static float lastT = 0;
    private static float lastR = 0;
    private static int cooldown = 0;

    private HelmInputClient() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cooldown > 0) cooldown--;

            LocalPlayer player = client.player;
            if (player == null) return;
            if (!(player.getVehicle() instanceof ShipEntity)) return;

            // Use existing movement keys (WASD)
            float throttle = 0;
            if (client.options.keyUp.isDown()) throttle += 1;
            if (client.options.keyDown.isDown()) throttle -= 1;

            float turn = 0;
            if (client.options.keyLeft.isDown()) turn += 1;
            if (client.options.keyRight.isDown()) turn -= 1;

            // Send only if changed or every few ticks
            if (cooldown == 0 && (throttle != lastT || turn != lastR)) {
                lastT = throttle;
                lastR = turn;
                cooldown = 2; // ~10Hz at 20tps
                ClientPlayNetworking.send(new HelmInputC2SPayload(throttle, turn));
            }
        });
    }
}
