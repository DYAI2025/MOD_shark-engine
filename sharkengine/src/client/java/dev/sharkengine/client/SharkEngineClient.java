package dev.sharkengine.client;

import dev.sharkengine.content.ModEntities;
import dev.sharkengine.client.render.ShipEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class SharkEngineClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HelmInputClient.init();

        // Minimal renderer: rendert nichts, verhindert aber "missing renderer"-Probleme.
        EntityRenderers.register(ModEntities.SHIP, ShipEntityRenderer::new);
    }
}
