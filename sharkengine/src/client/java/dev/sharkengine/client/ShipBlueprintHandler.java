package dev.sharkengine.client;

import dev.sharkengine.net.ShipBlueprintS2CPayload;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ShipBlueprintHandler {
    private ShipBlueprintHandler() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ShipBlueprintS2CPayload.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) return;

                Entity entity = mc.level.getEntity(payload.shipEntityId());
                if (entity instanceof ShipEntity ship && payload.blueprintNbt() != null) {
                    ShipBlueprint blueprint = ShipBlueprint.fromNbt(
                            payload.blueprintNbt(),
                            mc.level.registryAccess()
                    );
                    ship.setBlueprint(blueprint);
                }
            });
        });
    }
}
