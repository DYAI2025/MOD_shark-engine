package dev.sharkengine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renderer for ship entities.
 * Renders all blocks in the ship blueprint and spawns thruster particles.
 * 
 * <p>Particle effects:</p>
 * <ul>
 *   <li>Phase 1-2: Smoke particles (low intensity)</li>
 *   <li>Phase 3-5: Flame particles (high intensity)</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public final class ShipEntityRenderer extends EntityRenderer<ShipEntity> {
    private final BlockRenderDispatcher blockRenderer;
    
    /** Maximum particles per tick (performance limit) */
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

        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            BlockState blockState = block.state();
            if (blockState.getRenderShape() != RenderShape.MODEL) continue;

            poseStack.pushPose();
            // Offset by -0.5 to center on entity position, then add block offset
            poseStack.translate(block.dx() - 0.5f, block.dy(), block.dz() - 0.5f);
            blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // PARTICLE EFFECTS (Task 4.1)
        // ═══════════════════════════════════════════════════════════════════
        if (entity.level().isClientSide) {
            spawnThrusterParticles(entity);
        }
    }
    
    /**
     * Spawns thruster particles based on acceleration phase.
     * 
     * <p>Particle behavior:</p>
     * <ul>
     *   <li>Phase 1-2: CAMPFIRE_COSY_SMOKE (20-40% intensity)</li>
     *   <li>Phase 3-5: FLAME particles (60-100% intensity)</li>
     * </ul>
     * 
     * @param entity The ship entity to spawn particles for
     */
    private void spawnThrusterParticles(ShipEntity entity) {
        AccelerationPhase phase = entity.getPhase();
        ParticleOptions particleType = phase.getParticleType();
        float intensity = phase.getParticleIntensity();
        
        // Performance: Limit particles per tick
        int particleCount = Math.min(
            MAX_PARTICLES_PER_TICK,
            (int)(phase.ordinal() + 1) * 10  // More particles at higher phases
        );
        
        if (particleCount <= 0) return;
        
        Level level = entity.level();
        net.minecraft.util.RandomSource random = level.random;
        
        // Spawn particles below the ship (thruster location)
        double x = entity.getX();
        double y = entity.getY() - 0.5;
        double z = entity.getZ();
        
        for (int i = 0; i < particleCount; i++) {
            // Random offset around ship center
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;
            
            // Particle velocity (upward for thruster effect)
            double velY = 0.05 * intensity;
            
            level.addParticle(
                particleType,
                x + offsetX, y, z + offsetZ,
                0, velY, 0  // Slowly rising
            );
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // SOUND EFFECTS (Task 4.2)
        // ═══════════════════════════════════════════════════════════════════
        // Play thruster sound every ~1 second (20 ticks), throttled by intensity
        if (random.nextInt(20) == 0 && entity.getFuelLevel() > 0) {
            float volume = 0.3f * intensity;  // Louder at higher phases
            float pitch = 0.8f + (random.nextFloat() * 0.4f);  // 0.8-1.2
            
            level.playSound(
                entity.getX(), entity.getY(), entity.getZ(),
                dev.sharkengine.content.ModSounds.THRUSTER_ACTIVE.value(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                volume,
                pitch
            );
        }
    }
}
