package dev.sharkengine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sharkengine.ship.AccelerationPhase;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import dev.sharkengine.ship.ShipTransform;
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
        // BUG FIX 1+4 (AIR-011, 2026-07-12): Rotate entire ship structure with
        // interpolated yaw, MINUS the yaw the BUG block resolved to at
        // assembly time (blueprint.assemblyYaw(), AIR-015). Blueprint offsets
        // are captured in raw world-space, and the entity spawns with
        // yRot == assemblyYaw — rotating by raw entity yaw alone (the
        // pre-AIR-011 behavior) double-counts that initial orientation for
        // any BUG facing other than SOUTH (assemblyYaw=0), causing a visible
        // snap-rotation the instant the ship launches. Uses
        // ShipTransform.effectiveYaw (AIR-010) rather than a bare subtraction
        // so this is the one rotation authority, not a second formula.
        // Using the dispatcher-supplied entityYaw (already interpolated —
        // see EntityRenderDispatcher, matches vanilla's own BoatRenderer
        // pattern of using its received yaw parameter directly) instead of
        // recomputing Mth.lerp(partialTick, entity.yRotO, entity.getYRot())
        // by hand. The two are normally equal, but the manual recompute was
        // redundant with what the engine already computed for us and is one
        // fewer thing to keep in sync if that computation ever changes.
        // ═══════════════════════════════════════════════════════════════════
        float smoothYaw = ShipTransform.effectiveYaw(entityYaw, blueprint.assemblyYaw());
        poseStack.mulPose(Axis.YN.rotationDegrees(smoothYaw));

        // ═══════════════════════════════════════════════════════════════════
        // FLR-003 (flight-feel bank/roll, docs/plans/flight-bank-roll.md):
        // second rotation, composed AFTER yaw so it turns around the ship's
        // own local forward axis (Z, per how blueprint offsets place blocks
        // via dz) in the already-yaw-rotated frame, not world-space Z.
        // entity.getClientRoll() is a smoothed, client-only value (never
        // synced itself — derived every client tick from the synced
        // SYNC_TURN, see ShipEntity.tick()'s client branch) so this stays
        // purely cosmetic and identical for every observer.
        //
        // Axis.ZP below is a first guess, NOT yet empirically confirmed —
        // ShipTransform.rollFromTurnInput's contract only guarantees
        // "positive return value = bank in the direction that turns the
        // ship left"; it does NOT decide which PoseStack axis/sign that
        // corresponds to visually. Verify in runClient (turn left, confirm
        // the hull actually banks left — left wing/edge dips) and flip to
        // Axis.ZN here if it rolls the wrong way. Do not "fix" this by
        // flipping the sign in rollFromTurnInput instead — that function's
        // sign convention is deliberately anchored to the already-proven
        // turn-direction physics (see its javadoc), this axis choice is the
        // only thing that should change.
        // ═══════════════════════════════════════════════════════════════════
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getClientRoll()));

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
