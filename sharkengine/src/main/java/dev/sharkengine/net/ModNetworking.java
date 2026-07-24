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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
     * REQ-008/AC-008 (T09 remediation, code-review finding): key for {@link #rejectionLogState},
     * a rejected helm-input attempt identified by the sending player and the ship they attempted
     * to control. {@code shipId} is {@code null} for the "not riding any ship at all" rejection
     * path, where there is no {@link ShipEntity} to key by -- distinct from the copilot/non-pilot
     * rejection path, which always has one. A record is used purely as a compound map key (auto
     * {@code equals}/{@code hashCode}), same idiom as elsewhere in this codebase (e.g. {@code
     * ship.session.BlockCoord}).
     */
    private record RejectionKey(UUID playerId, UUID shipId) {}

    /**
     * Per-(player, ship) rate-limit bookkeeping for logging rejected helm-input attempts
     * (AC-008: "der Versuch wird begrenzt protokolliert" / the attempt is rate-limited and
     * logged). Helm input can be sent every client tick (~20/sec), so a naive unconditional
     * {@code LOGGER.warn} on every rejection would flood the server log for as long as an
     * unauthorized client kept sending control input -- {@link #logRejectedHelmInput} only
     * actually logs once per key per {@link #REJECTION_LOG_COOLDOWN_MS} window.
     *
     * <p>Bounded like {@code ShipEntity#passengerMountCounts}: this is (by count) the fifth
     * per-player tracking map in this codebase, after {@code TutorialService#lastPopupSent},
     * {@code ShipAssemblyService#lastPreviewSent}, {@code TutorialService#lastModeLockedNotice}
     * (all unbounded, server-lifetime, keyed by every player who ever connects -- see {@code
     * passengerMountCounts}' javadoc for why that was flagged) and {@code
     * ShipEntity#passengerMountCounts} itself (bounded, but scoped to one ship instance's own
     * lifetime). This one is {@code static}/server-lifetime like the first three, so it gets the
     * same defensive cap as {@code passengerMountCounts}: {@link #MAX_TRACKED_REJECTIONS} entries,
     * cleared outright the moment a genuinely new key would exceed it. This map is pure
     * rate-limit/test-instrumentation bookkeeping -- never consulted by the actual
     * isPilot/isRider authorization checks -- so losing long-tail entries past the cap only
     * resets a distant rejection's cooldown early, never a security regression.</p>
     */
    private static final Map<RejectionKey, RejectionLogState> rejectionLogState = new HashMap<>();

    /** Minimum time between two actually-logged warnings for the same (player, ship) pair. */
    private static final long REJECTION_LOG_COOLDOWN_MS = 5000L;

    /** Defensive size cap for {@link #rejectionLogState} -- see its javadoc. */
    private static final int MAX_TRACKED_REJECTIONS = 64;

    /**
     * Mutable value held per {@link RejectionKey}: when a rejection for this pair was last
     * actually logged, and how many times logging has fired in total for it. {@code loggedCount}
     * exists purely so tests can prove the cooldown suppresses rapid repeats -- two timestamps
     * captured microseconds apart would look almost identical whether logging fired once or a
     * hundred times in that window, but the count cannot lie about how many log lines actually
     * happened.
     */
    private static final class RejectionLogState {
        long lastLoggedAtMs;
        int loggedCount;
    }

    /**
     * REQ-008/AC-008 (T09) test/inspection hook: mirrors the established pattern ({@code
     * TutorialService#lastPopupSent}, {@code ShipAssemblyService#lastPreviewSent}) of exposing an
     * otherwise-private tracking map read-only for GameTest assertions. Returns how many times a
     * rejected helm-input attempt for this exact (player, ship) pair has actually been logged (0
     * if never, including "rejected but suppressed by the rate limit").
     */
    public static int rejectionLogCount(UUID playerId, UUID shipId) {
        RejectionLogState state = rejectionLogState.get(new RejectionKey(playerId, shipId));
        return state == null ? 0 : state.loggedCount;
    }

    /**
     * REQ-008/AC-008 (T09) test/inspection hook: the epoch-millis timestamp of the most recent
     * actually-logged rejection for this (player, ship) pair, or {@code null} if none has been
     * logged yet. See {@link #rejectionLogCount} for the companion count-based hook.
     */
    public static Long lastRejectionLoggedAt(UUID playerId, UUID shipId) {
        RejectionLogState state = rejectionLogState.get(new RejectionKey(playerId, shipId));
        return state == null ? null : state.lastLoggedAtMs;
    }

    /**
     * Logs a rejected helm-input attempt, rate-limited to at most once per {@link
     * #REJECTION_LOG_COOLDOWN_MS} per (player, ship) pair -- see {@link #rejectionLogState}'s
     * javadoc. {@code ship} is {@code null} for the "not riding any ship" rejection path.
     */
    private static void logRejectedHelmInput(ServerPlayer sp, ShipEntity ship, String reason) {
        UUID shipId = ship == null ? null : ship.getUUID();
        RejectionKey key = new RejectionKey(sp.getUUID(), shipId);
        long now = System.currentTimeMillis();

        RejectionLogState state = rejectionLogState.get(key);
        if (state != null && now - state.lastLoggedAtMs < REJECTION_LOG_COOLDOWN_MS) {
            return; // within cooldown -- suppress this repeat, no log line emitted
        }

        if (state == null) {
            if (rejectionLogState.size() >= MAX_TRACKED_REJECTIONS) {
                rejectionLogState.clear();
            }
            state = new RejectionLogState();
            rejectionLogState.put(key, state);
        }
        state.lastLoggedAtMs = now;
        state.loggedCount++;

        SharkEngineMod.LOGGER.warn("Rejected helm input from {} ({}), ship={}",
                sp.getGameProfile().getName(), reason, shipId);
    }

    /**
     * REQ-008/T09: production authorization entry point for helm input. Only the
     * server-assigned pilot ({@link ShipEntity#isPilot}) may have their input actually applied
     * via {@link ShipEntity#setInputs} -- a copilot (a genuine rider, {@link
     * ShipEntity#isCopilot}) or any other non-pilot passenger/bystander is silently ignored, no
     * state change, but the rejected attempt is rate-limit-logged (AC-008, see {@link
     * #logRejectedHelmInput}). This check pre-dates T09 (it already gated the registered C2S
     * receiver below); it was pulled out into its own method purely so {@code
     * PilotControlAuthorityGameTest} can call the exact production logic directly without a live
     * network connection -- zero behavior change to the authorization checks themselves, same
     * extraction pattern {@code BuildSessionGate#tryAssemble} established for REQ-003's C2S
     * assemble handler.
     */
    public static void handleHelmInput(ServerPlayer sp, HelmInputC2SPayload payload) {
        if (!(sp.getVehicle() instanceof ShipEntity ship)) {
            logRejectedHelmInput(sp, null, "not riding a ship");
            return;
        }
        if (!ship.isPilot(sp)) {
            logRejectedHelmInput(sp, ship, "not the pilot");
            return;
        }

        // Set all 3 inputs: throttle (vertical), turn, forward
        ship.setInputs(payload.throttle(), payload.turn(), payload.forward());
    }

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
            ctx.server().execute(() -> handleHelmInput(sp, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(BuilderAssembleC2SPayload.TYPE, (payload, ctx) -> {
            ServerPlayer sp = ctx.player();
            ctx.server().execute(() -> {
                if (!(sp.level() instanceof ServerLevel serverLevel)) {
                    return;
                }

                // ═══════════════════════════════════════════════════════════════════
                // REQ-014/T14: this SAME C2S payload (wheelPos + sessionId) is also how the
                // BuilderScreen "Assemble" button commits an EDIT-MODE change on an
                // already-launched ship -- ShipAssemblyService#openEditModePreview hands the
                // client ship.blockPosition() as this payload's wheelPos, with sessionId always
                // null (no REQ-003 build session applies to an edit commit). Try that resolution
                // FIRST; only fall through to the ordinary pre-launch BuildSessionGate flow below
                // when wheelPos does not resolve to an edit-mode ship this player pilots.
                // ═══════════════════════════════════════════════════════════════════
                ShipEntity editingShip =
                        ShipAssemblyService.findEditModeShip(serverLevel, payload.wheelPos(), sp);
                if (editingShip != null) {
                    ShipAssemblyService.EditCommitResult commit =
                            ShipAssemblyService.commitEdit(serverLevel, editingShip, sp);
                    sp.sendSystemMessage(Component.translatable(commit.translationKey(), commit.arg()));
                    if (commit.isSuccess()) {
                        ServerPlayNetworking.send(sp, BuilderPreviewS2CPayload.close());
                    } else {
                        SharkEngineMod.LOGGER.warn("Rejected edit-mode commit from {} at {}: {}",
                                sp.getGameProfile().getName(), payload.wheelPos(), commit.translationKey());
                    }
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
