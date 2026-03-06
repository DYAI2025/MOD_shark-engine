package dev.sharkengine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sharkengine.ship.AccelerationPhase;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renderer for ship entities.
 * Renders all blocks rotated with the entity yaw and spawns thruster particles.
 *
 * <p>BUG FIX 1+4: Blocks now rotate with the entity so the visual matches
 * the actual movement direction. Uses partialTick interpolation for smooth rendering.</p>
 *
 * @author Shark Engine Team
 * @version 3.0 (Bug-Fix)
 */
public final class ShipEntityRenderer extends EntityRenderer<ShipEntity> {
    private final BlockRenderDispatcher blockRenderer;

    private static final int MAX_PARTICLES_PER_TICK = 50;

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

        poseStack.pushPose();

        // ═══════════════════════════════════════════════════════════════════
        // BUG FIX 1+4: Rotate entire ship structure with interpolated yaw
        // This ensures the visual ship orientation matches movement direction.
        // Using Mth.lerp for smooth rotation between ticks (fixes jitter).
        // ═══════════════════════════════════════════════════════════════════
        float smoothYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YN.rotationDegrees(smoothYaw));

        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            BlockState blockState = block.state();
            if (blockState.getRenderShape() != RenderShape.MODEL) continue;

            poseStack.pushPose();
            poseStack.translate(block.dx() - 0.5f, block.dy(), block.dz() - 0.5f);
            blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        poseStack.popPose();

        // Particles
        if (entity.level().isClientSide) {
            spawnThrusterParticles(entity);
        }
    }

    /**
     * Spawns thruster particles at the positions of thruster blocks,
     * rotated to match the ship's current orientation.
     */
    private void spawnThrusterParticles(ShipEntity entity) {
        if (!entity.hasThrusters()) return;

        AccelerationPhase phase = entity.getPhase();
        ParticleOptions particleType = phase.getParticleType();
        float intensity = phase.getParticleIntensity();

        int particleCount = Math.min(MAX_PARTICLES_PER_TICK,
                (phase.ordinal() + 1) * 10);
        if (particleCount <= 0) return;

        Level level = entity.level();
        net.minecraft.util.RandomSource random = level.random;

        // BUG FIX 1: Spawn particles at rotated positions matching ship orientation
        float yawRad = (float) Math.toRadians(entity.getYRot());
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);

        double cx = entity.getX();
        double cy = entity.getY() - 0.5;
        double cz = entity.getZ();

        for (int i = 0; i < particleCount; i++) {
            // Random offset relative to ship, then rotate
            double localX = (random.nextDouble() - 0.5) * 2.0;
            double localZ = (random.nextDouble() - 0.5) * 2.0;

            // Rotate by yaw
            double worldX = localX * cosYaw - localZ * sinYaw;
            double worldZ = localX * sinYaw + localZ * cosYaw;

            double velY = 0.05 * intensity;

            level.addParticle(particleType,
                    cx + worldX, cy, cz + worldZ,
                    0, velY, 0);
        }

        // Sound effects
        if (random.nextInt(20) == 0 && entity.getFuelLevel() > 0) {
            float volume = 0.3f * intensity;
            float pitch = 0.8f + (random.nextFloat() * 0.4f);

            level.playLocalSound(
                    entity.getX(), entity.getY(), entity.getZ(),
                    dev.sharkengine.content.ModSounds.THRUSTER_ACTIVE,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    volume, pitch, false);
        }
    }
}
