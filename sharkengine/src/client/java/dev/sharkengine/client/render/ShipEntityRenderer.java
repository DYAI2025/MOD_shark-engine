package dev.sharkengine.client.render;

import dev.sharkengine.ship.ShipEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

// Minimaler "no-op" Renderer (1.21.11 nutzt RenderStates).
public final class ShipEntityRenderer extends EntityRenderer<ShipEntity, EntityRenderState> {
    public ShipEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
