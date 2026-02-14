package dev.sharkengine.client;

import dev.sharkengine.content.ModEntities;
import dev.sharkengine.client.render.ShipEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class SharkEngineClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HelmInputClient.init();
        ShipBlueprintHandler.init();

        EntityRendererRegistry.register(ModEntities.SHIP, ShipEntityRenderer::new);
    }
}
