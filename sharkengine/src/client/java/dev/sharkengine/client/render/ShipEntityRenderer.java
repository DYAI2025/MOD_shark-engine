package dev.sharkengine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sharkengine.content.block.ThrusterBlock;
import dev.sharkengine.ship.AccelerationPhase;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import dev.sharkengine.ship.TrailColor;
import net.minecraft.core.particles.DustParticleOptions;
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
        // WHY assemblyYaw IS SUBTRACTED AT ALL (AIR-011/AIR-015): blueprint
        // offsets are captured in raw world space and the entity spawns with
        // yRot == assemblyYaw, so rotating by raw entity yaw alone double-counts
        // the initial orientation for any BUG facing other than SOUTH, producing
        // a visible snap-rotation the instant the ship launches. entityYaw comes
        // from the dispatcher already interpolated (same as vanilla BoatRenderer);
        // do not recompute it by hand.
        // ═══════════════════════════════════════════════════════════════════
        // ORIENTATION-INDEPENDENT ROLL/PITCH (corrected 2026-07-26 after a live
        // report: Space banked the ship LEFT and D tipped the NOSE DOWN — roll
        // and pitch were acting on each other's axes).
        //
        // ROOT CAUSE, and why two earlier "empirically confirmed" fixes both
        // stuck: ShipAssemblyService stores blueprint offsets as RAW WORLD
        // deltas (dx = pos.getX() - wheelPos.getX()), never normalised to the
        // BUG's facing. So the model's local FORWARD axis is whatever world
        // axis the BUG pointed along AT ASSEMBLY TIME: +Z for a SOUTH bug, +X
        // for EAST, -X for WEST, -Z for NORTH. There is therefore NO single
        // hardcoded axis pair that is right for every ship — the older fixes
        // (roll ZP->XP->XN, pitch ZP->ZN) were each measured on a differently
        // oriented test ship and were correct only for that one. Working the
        // geometry back: roll=XN is right iff forward is +X (EAST build),
        // pitch=ZN iff forward is -X (WEST build) — no build satisfies both,
        // which is the tell that the mapping was never orientation-independent.
        //
        // FIX: split the yaw so roll/pitch happen in the CANONICAL frame
        // (nose = +Z, up = +Y, left wing = +X). The inner YN(-assemblyYaw)
        // rotates the raw blueprint out of its assembly-time world frame into
        // that canonical frame; roll then goes about the longitudinal axis (Z)
        // and pitch about the lateral axis (X) for EVERY bug facing.
        //
        // Safe refactor: with roll = pitch = 0 the two YN terms compose to
        // YN(entityYaw - assemblyYaw) = YN(effectiveYaw), i.e. byte-identical
        // to the previous single-rotation yaw handling.
        //
        // Do NOT "fix" a future regression by flipping signs in
        // ShipTransform.rollFromTurnInput / pitchFromVerticalInput — those are
        // anchored to the proven turn/climb physics. If something looks wrong,
        // check the BUG facing of the ship you are testing FIRST: that is the
        // variable two previous debugging sessions missed.
        // ═══════════════════════════════════════════════════════════════════
        poseStack.mulPose(Axis.YN.rotationDegrees(entityYaw));
        poseStack.mulPose(Axis.ZN.rotationDegrees(entity.getClientRoll()));    // bank, about the longitudinal axis
        poseStack.mulPose(Axis.XN.rotationDegrees(entity.getClientPitch()));   // pitch, about the lateral axis
        poseStack.mulPose(Axis.YN.rotationDegrees(-blueprint.assemblyYaw()));

        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            BlockState blockState = block.state();
            if (blockState.getRenderShape() != RenderShape.MODEL) continue;

            poseStack.pushPose();
            poseStack.translate(block.dx() - 0.5f, block.dy(), block.dz() - 0.5f);
            blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        poseStack.popPose();

        // Particles — render() runs once per FRAME, so this must be gated to one burst per
        // TICK or emission (and the sound roll below) scales with framerate. The gate is
        // stateful and consumes the tick's slot, hence the call order: ask last, emit once.
        // The old `entity.level().isClientSide` guard here was dead code — an EntityRenderer
        // only ever runs client-side.
        if (entity.shouldEmitParticlesThisTick()) {
            spawnThrusterParticles(entity);
        }
    }

    /**
     * Spawns thruster particles for this tick, rotated to match the ship's current orientation.
     *
     * <p>Call ONLY behind {@link ShipEntity#shouldEmitParticlesThisTick()} — see the call site.</p>
     *
     * <p>Note the particles are emitted around the entity origin with a random horizontal
     * offset, NOT at the individual thruster block positions; that mismatch with this method's
     * historical name is a separate, still-open issue recorded in CLAUDE.md.</p>
     */
    private void spawnThrusterParticles(ShipEntity entity) {
        if (!entity.hasThrusters()) return;

        AccelerationPhase phase = entity.getPhase();
        ParticleOptions particleType = phase.getParticleType();
        float intensity = phase.getParticleIntensity();

        int particleCount = Math.min(MAX_PARTICLES_PER_TICK,
                (phase.ordinal() + 1) * 10);
        if (particleCount <= 0) return;

        // REQ-019/T21: one render path for all 17 color cases — a colored thruster emits the
        // SAME dust particle tinted via TrailColor.red()/green()/blue(); NONE keeps the phase's
        // existing default trail. Never a per-color texture (TrailTextureResourceTests guards
        // that). Pre-T21 blueprint states lack the property and simply contribute no color.
        java.util.List<TrailColor> trailColors = new java.util.ArrayList<>();
        ShipBlueprint blueprint = entity.getBlueprint();
        if (blueprint != null) {
            for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
                if (block.state().hasProperty(ThrusterBlock.TRAIL_COLOR)) {
                    trailColors.add(block.state().getValue(ThrusterBlock.TRAIL_COLOR));
                }
            }
        }

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

            ParticleOptions options = particleType;
            if (!trailColors.isEmpty()) {
                TrailColor trail = trailColors.get(random.nextInt(trailColors.size()));
                if (trail.isColored()) {
                    options = new DustParticleOptions(
                            new org.joml.Vector3f(trail.red(), trail.green(), trail.blue()), 1.0f);
                }
            }
            level.addParticle(options,
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
