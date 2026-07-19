package dev.sharkengine.ship;

import dev.sharkengine.ship.session.BlockCoord;
import dev.sharkengine.ship.session.VehicleBuildSession;
import dev.sharkengine.ship.session.VehicleBuildSessionRegistry;
import dev.sharkengine.ship.session.VehicleBuildSessionRequest;
import dev.sharkengine.ship.session.VehicleBuildSessionValidation;
import dev.sharkengine.ship.session.VehicleBuildSessionValidator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Fabric-facing adapter binding {@code dev.sharkengine.ship.session}'s pure session/validation
 * logic (REQ-003) to real {@link ServerPlayer}/{@link ServerLevel}/{@link BlockPos} objects.
 *
 * <p>The {@code ship.session} package never imports {@code net.minecraft.*} (same discipline as
 * {@code ship.part}/{@code ShipTransform}); this class is the single seam where that pure logic
 * meets server authority — {@link dev.sharkengine.tutorial.TutorialService} calls {@link
 * #createAirSession} when a player confirms the AIR route, and {@link dev.sharkengine.net.ModNetworking}'s
 * assemble handler calls {@link #tryAssemble} instead of {@link ShipAssemblyService#tryAssemble}
 * directly, so no assembly (and therefore no block removal / entity spawn) can happen for a
 * request that fails any of the six independent authorization axes.</p>
 *
 * <p><b>Session lookups always pass the requester's own current dimension</b> — never the wheel
 * coordinate alone — to {@link VehicleBuildSessionRegistry}'s composite-keyed store, so a session
 * belonging to an unrelated wheel that happens to share raw {@code x/y/z} coordinates in a
 * different dimension can never be confused for "the" session at this wheel (regression fix — see
 * {@link VehicleBuildSessionRegistry}'s javadoc).</p>
 */
public final class BuildSessionGate {

    /**
     * How often ({@link net.minecraft.server.MinecraftServer#getTickCount()} modulo this value)
     * the REQ-003 expired-session reaper runs — see {@link #registerLifecycleHooks}. 200 ticks =
     * ~10 seconds at the vanilla 20 ticks/sec rate. Deliberately stateless (gated on the server's
     * own monotonic tick counter rather than a separately-tracked counter field) so there is
     * nothing extra to reset/leak across a hot mod reload.
     */
    private static final long SWEEP_INTERVAL_TICKS = 200L;

    private BuildSessionGate() {}

    /**
     * Creates (or replaces the SAME owner's prior) AIR build session bound to {@code wheelPos}
     * for {@code player}. Returns {@code null} if a DIFFERENT player's session is still ACTIVE
     * and unexpired at this wheel — REQ-003 session-theft fix, see {@link
     * VehicleBuildSessionRegistry#create}. Callers MUST handle a {@code null} result explicitly
     * (e.g. {@code TutorialService#handleModeSelection}) rather than assuming creation always
     * succeeds.
     */
    public static VehicleBuildSession createAirSession(ServerPlayer player, BlockPos wheelPos) {
        return VehicleBuildSessionRegistry.create(
                player.getUUID(),
                dimensionId(player.level()),
                toCoord(wheelPos),
                VehicleClass.AIR,
                System.currentTimeMillis());
    }

    /**
     * The session id bound to {@code wheelPos} in {@code level}'s dimension, but ONLY when {@code
     * requester} is that session's own owner — {@code null} otherwise (including when no session
     * exists at all).
     *
     * <p><b>REQ-003 security fix (reviewer-reported):</b> a session id is a secret, server-owned
     * bearer token (see {@link VehicleBuildSession}'s javadoc) — {@link
     * ShipAssemblyService#openBuilderPreview} embeds whatever this method returns into the {@code
     * BuilderPreviewS2CPayload} sent to {@code requester}, and that payload is the exact
     * client-visible channel a later {@code BuilderAssembleC2SPayload} attempt reads its {@code
     * sessionId} field from. {@code openBuilderPreview} is called from more than one C2S path
     * (both {@code ModNetworking}'s assemble-failure branch and {@code
     * TutorialService#handleAdvanceStage}), and the latter has no session-ownership check of its
     * own — it only requires that {@code requester} previously interacted with this exact wheel at
     * all (which ANY player can do, including a non-owner who never selected AIR). This method is
     * the single choke point that stops a non-owner's request from having someone else's real,
     * still-usable session id echoed back to them, regardless of which call site is used. This
     * replaces the previous {@code currentSessionId} lookup, which returned "whatever session
     * currently exists at this wheel" unconditionally, with no check on who was asking.</p>
     */
    public static UUID sessionIdForOwner(ServerLevel level, BlockPos wheelPos, ServerPlayer requester) {
        VehicleBuildSession session = VehicleBuildSessionRegistry.findByWheel(dimensionId(level), toCoord(wheelPos));
        if (session == null || !session.playerId().equals(requester.getUUID())) {
            return null;
        }
        return session.sessionId();
    }

    /**
     * Wires REQ-003's session-lifecycle cleanup into the running server (reviewer-reported: the
     * registry previously had no server/world-lifecycle hook at all — entries never got evicted
     * after expiry, unbounded growth, and a same-player stale session could survive a
     * singleplayer world switch within the same JVM). Call exactly once, from {@code
     * SharkEngineMod#init}.
     *
     * <p>Two hooks:</p>
     * <ul>
     *   <li>{@link ServerLifecycleEvents#SERVER_STOPPED} clears every tracked session outright —
     *   a stopped server (including an integrated singleplayer server about to be replaced by a
     *   different world in the same JVM, e.g. via "Save and Quit to Title" then loading another
     *   world) has no player who could still legitimately hold one.</li>
     *   <li>A periodic {@link ServerTickEvents#END_SERVER_TICK} sweep, gated on {@code
     *   server.getTickCount() % SWEEP_INTERVAL_TICKS} so it needs no separate mutable counter,
     *   evicts individually-expired sessions on a schedule instead of only reactively whenever
     *   something happens to look up that exact key — bounding the map's size even for wheels
     *   nobody ever revisits.</li>
     * </ul>
     */
    public static void registerLifecycleHooks() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> VehicleBuildSessionRegistry.clearAll());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % SWEEP_INTERVAL_TICKS == 0) {
                VehicleBuildSessionRegistry.evictExpired(System.currentTimeMillis());
            }
        });
    }

    /**
     * Validates {@code requester}'s claim against the session bound to {@code wheelPos} in the
     * requester's own current dimension, on all six independent axes (REQ-003/AC-003). Performs no
     * world mutation of any kind and, deliberately, does <b>not</b> consume the session even when
     * fully valid — see {@link #tryAssemble} for why that decision belongs one layer up.
     */
    public static VehicleBuildSessionValidation authorizeAssembly(ServerPlayer requester, BlockPos wheelPos, UUID providedSessionId) {
        String requesterDimensionId = dimensionId(requester.level());
        BlockCoord coord = toCoord(wheelPos);
        VehicleBuildSession session = VehicleBuildSessionRegistry.findByWheel(requesterDimensionId, coord);

        VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                requester.getUUID(),
                requesterDimensionId,
                toCoord(requester.blockPosition()),
                providedSessionId,
                System.currentTimeMillis());

        return VehicleBuildSessionValidator.validate(session, request);
    }

    /** Outcome of a session-gated assembly attempt: {@code assembleResult} is {@code null} unless {@code authorization} is valid. */
    public record AssembleAttemptResult(VehicleBuildSessionValidation authorization, ShipAssemblyService.AssembleResult assembleResult) {
        public boolean isAuthorized() {
            return authorization.isValid();
        }
    }

    /**
     * The single production entry point tying REQ-003's session gate to the actual assembly
     * pipeline. Callers (both {@code ModNetworking} and {@code BuildSessionAuthorizationGameTest})
     * must call this instead of {@link ShipAssemblyService#tryAssemble} directly.
     *
     * <p>The session is consumed (one-shot, REQ-003's "duplicate/replayed request" axis) only once
     * {@link ShipAssemblyService#tryAssemble} itself reports a structurally-successful assembly —
     * never merely because the six REQ-003 authorization axes passed. An authorized-but-structurally
     * -invalid attempt (e.g. an owner whose structure is still missing a thruster) therefore leaves
     * the session {@code ACTIVE}, so the player's very next, now-complete attempt is still honored
     * instead of being permanently burned by a purely structural rejection unrelated to
     * authorization.</p>
     */
    public static AssembleAttemptResult tryAssemble(ServerLevel level, BlockPos wheelPos, ServerPlayer pilot, UUID providedSessionId) {
        VehicleBuildSessionValidation validation = authorizeAssembly(pilot, wheelPos, providedSessionId);
        if (!validation.isValid()) {
            return new AssembleAttemptResult(validation, null);
        }

        ShipAssemblyService.AssembleResult result = ShipAssemblyService.tryAssemble(level, wheelPos, pilot);
        if (result.isSuccess()) {
            VehicleBuildSessionRegistry.consume(dimensionId(pilot.level()), toCoord(wheelPos));
        }
        return new AssembleAttemptResult(validation, result);
    }

    private static String dimensionId(Level level) {
        return level.dimension().location().toString();
    }

    private static BlockCoord toCoord(BlockPos pos) {
        return new BlockCoord(pos.getX(), pos.getY(), pos.getZ());
    }
}
