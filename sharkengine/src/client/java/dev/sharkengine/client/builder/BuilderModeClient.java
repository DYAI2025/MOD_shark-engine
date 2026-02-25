package dev.sharkengine.client.builder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sharkengine.net.BuilderPreviewS2CPayload;
import dev.sharkengine.ship.ShipBlueprint;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Client-side controller for the builder preview overlay and screen.
 */
public final class BuilderModeClient {
    private static PreviewState preview;

    private BuilderModeClient() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(BuilderPreviewS2CPayload.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> handlePreview(payload));
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            PreviewState state = preview;
            if (state == null) {
                return;
            }
            if (context == null || context.matrixStack() == null || context.consumers() == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !Objects.equals(mc.level.dimension(), state.dimension())) {
                return;
            }

            renderHighlights(state, context.matrixStack(), context.consumers().getBuffer(RenderType.lines()), context.camera().getPosition());
        });
    }

    private static void handlePreview(BuilderPreviewS2CPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!payload.active()) {
            clearPreview();
            if (mc.screen instanceof BuilderScreen) {
                mc.setScreen(null);
            }
            return;
        }

        if (mc.level == null || payload.blueprintNbt() == null) {
            return;
        }

        ShipBlueprint blueprint = ShipBlueprint.fromNbt(payload.blueprintNbt(), mc.level.registryAccess());
        List<BlockPos> validBlocks = new ArrayList<>(blueprint.blocks().size());
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            validBlocks.add(blueprint.origin().offset(block.dx(), block.dy(), block.dz()));
        }

        PreviewState state = new PreviewState(
                blueprint.origin(),
                mc.level.dimension(),
                Collections.unmodifiableList(validBlocks),
                List.copyOf(payload.invalidBlocks()),
                payload.canAssemble(),
                payload.contactPoints()
        );
        preview = state;
        mc.setScreen(new BuilderScreen(state));
    }

    public static void clearPreview() {
        preview = null;
    }

    static PreviewState getPreview() {
        return preview;
    }

    private static void renderHighlights(PreviewState state, PoseStack poseStack, VertexConsumer consumer, Vec3 camera) {
        if (consumer == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos valid : state.validBlocks()) {
            drawBox(poseStack, consumer, valid, 1.0F, 1.0F, 1.0F);
        }
        for (BlockPos invalid : state.invalidBlocks()) {
            drawBox(poseStack, consumer, invalid, 1.0F, 0.2F, 0.2F);
        }
        poseStack.popPose();
    }

    private static void drawBox(PoseStack poseStack, VertexConsumer consumer, BlockPos pos, float r, float g, float b) {
        AABB box = new AABB(pos).inflate(0.002D);
        LevelRenderer.renderLineBox(poseStack, consumer,
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                r, g, b, 0.8F);
    }
}
