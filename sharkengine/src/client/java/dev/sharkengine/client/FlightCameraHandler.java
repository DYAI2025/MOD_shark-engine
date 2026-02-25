package dev.sharkengine.client;

import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * Forces a cinematic third-person camera while piloting ships and restores the previous mode afterwards.
 */
public final class FlightCameraHandler {
    private static CameraType previousType;
    private static boolean forced;

    private FlightCameraHandler() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null) {
                return;
            }
            if (client.player == null) {
                if (forced) {
                    releaseCamera(client);
                }
                return;
            }

            boolean ridingShip = client.player.getVehicle() instanceof ShipEntity;
            if (ridingShip) {
                if (!forced) {
                    previousType = client.options.getCameraType();
                    client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    forced = true;
                }
            } else if (forced) {
                releaseCamera(client);
            }
        });
    }

    private static void releaseCamera(Minecraft client) {
        forced = false;
        CameraType fallback = previousType != null ? previousType : CameraType.FIRST_PERSON;
        client.options.setCameraType(fallback);
        previousType = null;
    }
}
