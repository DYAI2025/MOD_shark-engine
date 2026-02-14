package dev.sharkengine.net;

import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
    private ModNetworking() {}

    public static void init() {
        PayloadTypeRegistry.playC2S().register(HelmInputC2SPayload.TYPE, HelmInputC2SPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HelmInputC2SPayload.TYPE, (payload, ctx) -> {
            ServerPlayer sp = ctx.player();
            ctx.server().execute(() -> {
                if (!(sp.getVehicle() instanceof ShipEntity ship)) return;
                if (!ship.isPilot(sp)) return;
                ship.setInputs(payload.throttle(), payload.turn());
            });
        });
    }
}
