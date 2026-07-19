package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.ship.BuildSessionGate;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipEntity;
import dev.sharkengine.tutorial.TutorialService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Networking initialization class for Shark Engine.
 * Registers all packet payloads and their handlers.
 * 
 * <p>Registered Payloads:</p>
 * <ul>
 *   <li>C2S: {@link HelmInputC2SPayload} - Client to Server helm/steering input</li>
 *   <li>S2C: {@link ShipBlueprintS2CPayload} - Server to Client blueprint sync</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public final class ModNetworking {
    private ModNetworking() {}

    /**
     * Initializes all networking handlers.
     * Call this during mod initialization.
     */
    public static void init() {
        // ═══════════════════════════════════════════════════════════════════
        // C2S: Helm Input (Client → Server)
        // ═══════════════════════════════════════════════════════════════════
        PayloadTypeRegistry.playC2S().register(HelmInputC2SPayload.TYPE, HelmInputC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuilderAssembleC2SPayload.TYPE, BuilderAssembleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TutorialModeSelectionC2SPayload.TYPE, TutorialModeSelectionC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TutorialAdvanceC2SPayload.TYPE, TutorialAdvanceC2SPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HelmInputC2SPayload.TYPE, (payload, ctx) -> {
            ServerPlayer sp = ctx.player();
            ctx.server().execute(() -> {
                if (!(sp.getVehicle() instanceof ShipEntity ship)) return;
                if (!ship.isPilot(sp)) return;
                
                // Set all 3 inputs: throttle (vertical), turn, forward
                ship.setInputs(payload.throttle(), payload.turn(), payload.forward());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(BuilderAssembleC2SPayload.TYPE, (payload, ctx) -> {
            ServerPlayer sp = ctx.player();
            ctx.server().execute(() -> {
                if (!(sp.level() instanceof ServerLevel serverLevel)) {
                    return;
                }
                // REQ-003: every assemble request must pass the server-owned VehicleBuildSession
                // gate (owner, dimension, distance, expiry, session id, not-already-consumed)
                // BEFORE ShipAssemblyService ever scans/removes a single block. See
                // BuildSessionGate#tryAssemble.
                BuildSessionGate.AssembleAttemptResult attempt =
                        BuildSessionGate.tryAssemble(serverLevel, payload.wheelPos(), sp, payload.sessionId());
                if (!attempt.isAuthorized()) {
                    SharkEngineMod.LOGGER.warn("Rejected assembly request from {} at {}: {}",
                            sp.getGameProfile().getName(), payload.wheelPos(), attempt.authorization().reasons());
                    sp.sendSystemMessage(Component.translatable("message.sharkengine.session_invalid"));
                    return;
                }

                ShipAssemblyService.AssembleResult result = attempt.assembleResult();
                sp.sendSystemMessage(Component.translatable(result.translationKey(), result.arg()));

                boolean success = result.isSuccess();
                if (success) {
                    ServerPlayNetworking.send(sp, BuilderPreviewS2CPayload.close());
                } else {
                    ShipAssemblyService.openBuilderPreview(serverLevel, payload.wheelPos(), sp);
                    if ("message.sharkengine.assembly_fail_invalid".equals(result.translationKey())) {
                        sp.sendSystemMessage(Component.translatable("message.sharkengine.builder_invalid", result.arg()));
                    } else if ("message.sharkengine.assembly_fail_contact".equals(result.translationKey())) {
                        sp.sendSystemMessage(Component.translatable("message.sharkengine.builder_contacts", result.arg()));
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TutorialModeSelectionC2SPayload.TYPE, (payload, ctx) -> {
            ServerPlayer sp = ctx.player();
            ctx.server().execute(() -> TutorialService.handleModeSelection(sp, payload.vehicleClass()));
        });

        ServerPlayNetworking.registerGlobalReceiver(TutorialAdvanceC2SPayload.TYPE, (payload, ctx) -> {
            ServerPlayer sp = ctx.player();
            ctx.server().execute(() -> TutorialService.handleAdvanceStage(sp, payload.stage()));
        });

        // ═══════════════════════════════════════════════════════════════════
        // S2C: Blueprint Sync (Server → Client)
        // ═══════════════════════════════════════════════════════════════════
        PayloadTypeRegistry.playS2C().register(ShipBlueprintS2CPayload.TYPE, ShipBlueprintS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BuilderPreviewS2CPayload.TYPE, BuilderPreviewS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TutorialPopupS2CPayload.TYPE, TutorialPopupS2CPayload.CODEC);
    }
}
