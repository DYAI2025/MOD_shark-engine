package dev.sharkengine.tutorial;

import dev.sharkengine.net.TutorialModeSelectionC2SPayload;
import dev.sharkengine.net.TutorialPopupS2CPayload;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.VehicleClass;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TutorialService {
    private static final Map<UUID, BlockPos> pendingBuilder = new HashMap<>();

    private TutorialService() {}

    public static void sendWelcomePopup(ServerPlayer player) {
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

        if (player.level() instanceof ServerLevel serverLevel) {
            ShipAssemblyService.openBuilderPreview(serverLevel, wheelPos, player);
            player.sendSystemMessage(Component.translatable("message.sharkengine.builder_open"));
        }
    }

    private static void sendModeLockedMessage(ServerPlayer player, VehicleClass mode) {
        player.sendSystemMessage(Component.translatable("message.sharkengine.mode_locked", mode.getDisplayName()));
    }
}
