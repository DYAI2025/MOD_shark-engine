package dev.sharkengine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class ShipEntityRenderer extends EntityRenderer<ShipEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public ShipEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(ShipEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        ShipBlueprint blueprint = entity.getBlueprint();
        if (blueprint == null || blueprint.blocks().isEmpty()) return;

        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            BlockState blockState = block.state();
            if (blockState.getRenderShape() != RenderShape.MODEL) continue;

            poseStack.pushPose();
            // Offset by -0.5 to center on entity position, then add block offset
            poseStack.translate(block.dx() - 0.5f, block.dy(), block.dz() - 0.5f);
            blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
