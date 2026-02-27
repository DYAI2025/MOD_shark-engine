package dev.sharkengine.client;

import dev.sharkengine.client.builder.BuilderModeClient;
import dev.sharkengine.client.render.FuelHudOverlay;
import dev.sharkengine.client.render.ShipEntityRenderer;
import dev.sharkengine.client.tutorial.TutorialPopupClient;
import dev.sharkengine.content.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * Client-side mod initializer for Shark Engine.
 * 
 * <p>Initializes:</p>
 * <ul>
 *   <li>HelmInputClient - Player input handler</li>
 *   <li>ShipBlueprintHandler - Client-side blueprint rendering</li>
 *   <li>ShipEntityRenderer - Ship entity renderer with particles</li>
 *   <li>FuelHudOverlay - Fuel and status HUD overlay</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public final class SharkEngineClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize input handler
        HelmInputClient.init();
        
        // Initialize blueprint handler
        ShipBlueprintHandler.init();

        // Builder overlay & automatic camera controls
        BuilderModeClient.init();
        FlightCameraHandler.init();
        TutorialPopupClient.init();
        
        // Register ship entity renderer
        EntityRendererRegistry.register(ModEntities.SHIP, ShipEntityRenderer::new);
        
        // ═══════════════════════════════════════════════════════════════════
        // FUEL HUD OVERLAY INTEGRATION (Task 4.4)
        // ═══════════════════════════════════════════════════════════════════
        // Register HUD overlay renderer
        // Renders fuel bar, height, speed, and weight warnings
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            FuelHudOverlay.render(guiGraphics, net.minecraft.client.Minecraft.getInstance());
        });
    }
}
