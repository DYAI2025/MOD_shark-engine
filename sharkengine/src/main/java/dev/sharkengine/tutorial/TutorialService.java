package dev.sharkengine.tutorial;

import dev.sharkengine.net.TutorialAdvanceC2SPayload;
import dev.sharkengine.net.TutorialPopupS2CPayload;
import dev.sharkengine.ship.BuildSessionGate;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.VehicleClass;
import dev.sharkengine.ship.session.VehicleBuildSession;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TutorialService {
    private static final Map<UUID, BlockPos> pendingBuilder = new HashMap<>();
    private static final Map<UUID, EnumSet<TutorialPopupStage>> stageHistory = new HashMap<>();

    /**
     * REQ-001 (AC-001) test/inspection hook: the most recent {@link TutorialPopupS2CPayload} sent
     * to each player. This is the single point every {@code ServerPlayNetworking.send(player,
     * TutorialPopupS2CPayload...)} call site routes through (see {@link #sendPopup}), so GameTests
     * can assert on the resulting server-side popup state (stage + routes) without needing to
     * intercept the network layer itself.
     */
    private static final Map<UUID, TutorialPopupS2CPayload> lastPopupSent = new HashMap<>();

    private TutorialService() {}

    public static void sendWelcomePopup(ServerPlayer player) {
        resetStages(player);
        sendPopup(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.WELCOME));
    }

    /**
     * REQ-001 (AC-001): the vehicle route-selection popup path. Both placing a Steering Wheel
     * ({@link dev.sharkengine.content.block.SteeringWheelBlock#setPlacedBy}) and right-clicking an
     * already-placed one ({@link dev.sharkengine.content.block.SteeringWheelBlock#useWithoutItem})
     * call this exact method, so both trigger paths produce the same popup state.
     */
    public static void startModeSelection(ServerPlayer player, BlockPos wheelPos) {
        pendingBuilder.put(player.getUUID(), wheelPos.immutable());
        // Feature 6: Reset stages so build mode can be re-entered after disassembly
        resetStages(player);
        sendPopup(player, TutorialPopupS2CPayload.forModeSelection(List.of(VehicleClass.values())));
    }

    public static void handleModeSelection(ServerPlayer player, VehicleClass mode) {
        BlockPos wheelPos = pendingBuilder.remove(player.getUUID());
        if (wheelPos == null) {
            return;
        }
        if (mode != VehicleClass.AIR) {
            sendModeLockedMessage(player, mode);
            pendingBuilder.put(player.getUUID(), wheelPos);
            sendPopup(player, TutorialPopupS2CPayload.forModeSelection(List.of(VehicleClass.values())));
            return;
        }
        pendingBuilder.put(player.getUUID(), wheelPos);
        // REQ-003: AIR is the only route that ever creates a server-owned VehicleBuildSession
        // (REQ-002/T03's LAND/WATER paths above return early and never reach here).
        VehicleBuildSession created = BuildSessionGate.createAirSession(player, wheelPos);
        if (created == null) {
            // REQ-003 session-theft fix (reviewer-reported): another player's session is still
            // ACTIVE and unexpired at this exact wheel -- BuildSessionGate.createAirSession
            // refused to evict it. Do NOT silently hand this player a "build guide" flow for a
            // session/structure they don't actually own (the eventual openBuilderPreview call
            // would correctly withhold the real session id per sessionIdForOwner, but this player
            // would still be looking at a builder screen for someone else's in-progress build
            // with no way to ever successfully assemble it). Tell them plainly and let them pick
            // again instead.
            pendingBuilder.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("message.sharkengine.session_invalid"));
            sendPopup(player, TutorialPopupS2CPayload.forModeSelection(List.of(VehicleClass.values())));
            return;
        }
        sendPopup(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.BUILD_GUIDE));
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
            sendPopup(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.READY_TO_LAUNCH));
        }
    }

    public static void notifyFlightTips(ServerPlayer player) {
        if (markStageAsShown(player, TutorialPopupStage.FLIGHT_TIPS)) {
            sendPopup(player, TutorialPopupS2CPayload.forStage(TutorialPopupStage.FLIGHT_TIPS));
        }
    }

    private static void sendPopup(ServerPlayer player, TutorialPopupS2CPayload payload) {
        lastPopupSent.put(player.getUUID(), payload);
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * REQ-001 (AC-001) test/inspection hook: the most recent popup payload sent to this player, or
     * {@code null} if none has been sent yet this session. See {@link #lastPopupSent} for why this
     * exists.
     */
    public static TutorialPopupS2CPayload lastPopupSent(UUID playerId) {
        return lastPopupSent.get(playerId);
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
