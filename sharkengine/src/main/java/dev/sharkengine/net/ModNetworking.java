package dev.sharkengine.net;

import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking initialization class for Shark Engine.
 * Registers all packet payloads and their handlers.
 * 
 * <p>Registered Payloads:</p>
 * <ul>
 *   <li>C2S: {@link HelmInputC2SPayload} - Client to Server helm/steering input</li>
 *   <li>S2C: {@link ShipBlueprintS2CPayload} - Server to Client blueprint sync</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public final class ModNetworking {
    private ModNetworking() {}

    /**
     * Initializes all networking handlers.
     * Call this during mod initialization.
     */
    public static void init() {
        // ═══════════════════════════════════════════════════════════════════
        // C2S: Helm Input (Client → Server)
        // ═══════════════════════════════════════════════════════════════════
        PayloadTypeRegistry.playC2S().register(HelmInputC2SPayload.TYPE, HelmInputC2SPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HelmInputC2SPayload.TYPE, (payload, ctx) -> {
            ServerPlayer sp = ctx.player();
            ctx.server().execute(() -> {
                if (!(sp.getVehicle() instanceof ShipEntity ship)) return;
                if (!ship.isPilot(sp)) return;
                
                // Set all 3 inputs: throttle (vertical), turn, forward
                ship.setInputs(payload.throttle(), payload.turn(), payload.forward());
            });
        });

        // ═══════════════════════════════════════════════════════════════════
        // S2C: Blueprint Sync (Server → Client)
        // ═══════════════════════════════════════════════════════════════════
        PayloadTypeRegistry.playS2C().register(ShipBlueprintS2CPayload.TYPE, ShipBlueprintS2CPayload.CODEC);
    }
}
