package dev.sharkengine.tutorial;

import dev.sharkengine.net.TutorialAdvanceC2SPayload;
import dev.sharkengine.net.TutorialPopupS2CPayload;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.VehicleClass;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TutorialService {
    private static final Map<UUID, BlockPos> pendingBuilder = new HashMap<>();
    private static final Map<UUID, EnumSet<TutorialPopupStage>> stageHistory = new HashMap<>();

    private TutorialService() {}

    public static void sendWelcomePopup(ServerPlayer player) {
        resetStages(player);
        ServerPlayNetworking.send(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.WELCOME));
    }

    public static void startModeSelection(ServerPlayer player, BlockPos wheelPos) {
        pendingBuilder.put(player.getUUID(), wheelPos.immutable());
        ServerPlayNetworking.send(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.MODE_SELECTION));
    }

    public static void handleModeSelection(ServerPlayer player, VehicleClass mode) {
        BlockPos wheelPos = pendingBuilder.remove(player.getUUID());
        if (wheelPos == null) {
            return;
        }
        if (mode != VehicleClass.AIR) {
            sendModeLockedMessage(player, mode);
            pendingBuilder.put(player.getUUID(), wheelPos);
            ServerPlayNetworking.send(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.MODE_SELECTION));
            return;
        }
        pendingBuilder.put(player.getUUID(), wheelPos);
        ServerPlayNetworking.send(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.BUILD_GUIDE));
    }

    private static void sendModeLockedMessage(ServerPlayer player, VehicleClass mode) {
        player.sendSystemMessage(Component.translatable("message.sharkengine.mode_locked", mode.getDisplayName()));
    }

    public static void handleAdvanceStage(ServerPlayer player, TutorialPopupStage stage) {
        if (stage == TutorialPopupStage.BUILD_GUIDE) {
            BlockPos wheelPos = pendingBuilder.get(player.getUUID());
            if (wheelPos == null) {
                return;
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                ShipAssemblyService.openBuilderPreview(serverLevel, wheelPos, player);
                player.sendSystemMessage(Component.translatable("message.sharkengine.builder_open"));
            }
        } else if (stage == TutorialPopupStage.READY_TO_LAUNCH || stage == TutorialPopupStage.FLIGHT_TIPS) {
            markStageAsShown(player, stage);
        }
    }

    public static void notifyReady(ServerPlayer player) {
        if (markStageAsShown(player, TutorialPopupStage.READY_TO_LAUNCH)) {
            ServerPlayNetworking.send(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.READY_TO_LAUNCH));
        }
    }

    public static void notifyFlightTips(ServerPlayer player) {
        if (markStageAsShown(player, TutorialPopupStage.FLIGHT_TIPS)) {
            ServerPlayNetworking.send(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.FLIGHT_TIPS));
        }
    }

    private static void resetStages(ServerPlayer player) {
        stageHistory.remove(player.getUUID());
    }

    private static boolean markStageAsShown(ServerPlayer player, TutorialPopupStage stage) {
        EnumSet<TutorialPopupStage> stages = stageHistory.computeIfAbsent(player.getUUID(), uuid -> EnumSet.noneOf(TutorialPopupStage.class));
        if (stages.contains(stage)) {
            return false;
        }
        stages.add(stage);
        return true;
    }
}
