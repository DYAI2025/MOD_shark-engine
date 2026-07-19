package dev.sharkengine.ship.session;

import dev.sharkengine.ship.VehicleClass;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side, in-memory store of active {@link VehicleBuildSession}s, keyed by the wheel
 * position <b>and dimension</b> they're bound to (REQ-003).
 *
 * <p><b>Why keyed by (dimension, wheel position), not by player or by session id:</b> the wheel is
 * the resource being protected (a specific place in a specific world), which is what lets all six
 * authorization axes stay independently testable from a single fixture — {@link #findByWheel}
 * always finds "the session for this wheel in this dimension" regardless of what the requester
 * claims about themselves, their distance, or their session id, and {@link
 * VehicleBuildSessionValidator} then cross-checks every one of those claims against it. Keying by
 * session id instead would make "wrong/absent session id" degenerate into "session not found" and
 * short-circuit the other five checks; keying by player would make "non-owner player" untestable
 * (a lookup by the requester's own id can never return someone else's session).</p>
 *
 * <p><b>Dimension is part of the key, not just a field on the session (regression fix):</b> an
 * earlier version of this registry keyed solely by {@link BlockCoord}, which meant two entirely
 * unrelated wheels at the same raw {@code x/y/z} in two different dimensions (Overworld/Nether/End
 * coordinate coincidence near spawn or portals is common) silently overwrote each other's session
 * the moment either player created one — the victim's own still-active, still-valid session simply
 * vanished, and their next legitimate assemble attempt was rejected as NON_OWNER/WRONG_DIMENSION
 * even though they had done nothing wrong. See {@code VehicleBuildSessionValidationTest}'s
 * {@code createInDifferentDimensionAtSameCoordDoesNotEvictOtherDimensionsSession} regression test
 * and {@code BuildSessionAuthorizationGameTest#crossDimensionCoordinateCollisionDoesNotEvictSession}
 * for direct reproductions of the fixed bug. Every lookup/mutation method below therefore requires
 * an explicit {@code dimensionId} alongside the {@link BlockCoord}.</p>
 *
 * <p><b>Session-theft prevention (reviewer-reported regression fix):</b> {@link #create} refuses
 * to evict an ACTIVE, not-yet-expired session that belongs to a DIFFERENT player — see that
 * method's javadoc. Without this guard, any player who merely reached another player's Steering
 * Wheel could overwrite the owner's real session with their own, then legitimately (per REQ-003's
 * own six axes, which only check "does this request match the CURRENT session," never "was the
 * current session rightfully created") assemble/steal the owner's already-built structure.</p>
 *
 * <p><b>Lifecycle cleanup (reviewer-reported regression fix):</b> this map is a JVM-static
 * singleton with no size bound of its own — {@link #evictExpired} must be called periodically
 * (wired from {@code dev.sharkengine.ship.BuildSessionGate#registerLifecycleHooks}) or expired
 * sessions for wheels nobody ever looks up again accumulate forever, and {@link #clearAll} must
 * be called on server stop (also wired from {@code registerLifecycleHooks}) so a stale session
 * can't survive into a different world loaded later in the same JVM (e.g. a singleplayer world
 * switch).</p>
 *
 * <p>Pure Java (only {@code java.util}/{@code java.util.concurrent} plus this package's own
 * types) — no Fabric bootstrap needed to unit-test, same discipline as the rest of this
 * package.</p>
 */
public final class VehicleBuildSessionRegistry {

    /**
     * Default session lifetime.
     *
     * <p><b>ASSUMPTION, not a PRD number:</b> REQ-003/AC-003 require an "Ablaufzeit" (expiry) to
     * exist and be enforced but do not pin a duration. 10 minutes is a deliberately generous
     * engineering default — long enough that a player who opens the builder screen, walks the
     * partial structure, and comes back to click Assemble doesn't spuriously expire — not a value
     * derived from any PRD/AC text. Revisit if playtest data suggests otherwise.</p>
     */
    public static final long DEFAULT_TTL_MILLIS = 10L * 60L * 1000L;

    /** Composite registry key: a session is scoped to one wheel position in one dimension. */
    private record SessionKey(String dimensionId, BlockCoord wheelPos) {
        private SessionKey {
            Objects.requireNonNull(dimensionId, "dimensionId");
            Objects.requireNonNull(wheelPos, "wheelPos");
        }
    }

    private static final Map<SessionKey, VehicleBuildSession> SESSIONS_BY_WHEEL = new ConcurrentHashMap<>();

    private VehicleBuildSessionRegistry() {}

    /**
     * Creates a session for {@code playerId} bound to {@code wheelPos}/{@code dimensionId} with
     * the default TTL. See the 6-arg overload for the session-theft-prevention contract (in
     * particular: this can return {@code null}).
     */
    public static VehicleBuildSession create(UUID playerId, String dimensionId, BlockCoord wheelPos,
                                              VehicleClass vehicleClass, long nowMillis) {
        return create(playerId, dimensionId, wheelPos, vehicleClass, nowMillis, DEFAULT_TTL_MILLIS);
    }

    /**
     * Creates a session for {@code playerId} bound to {@code wheelPos}/{@code dimensionId}, with
     * an explicit TTL (test hook for the 5-arg overload above).
     *
     * <p><b>REQ-003 session-theft fix (reviewer-reported):</b> refuses — returns {@code null},
     * leaving whatever session was already there completely untouched — when an ACTIVE,
     * not-yet-expired session already exists at this exact {@code (dimensionId, wheelPos)} key
     * for a DIFFERENT {@code playerId}. Without this guard, any player who merely reached another
     * player's Steering Wheel could evict the owner's real, still-in-progress session and then
     * legitimately (per this package's own six authorization axes, which only ever check "does
     * this request match the CURRENT session," never "was the current session rightfully
     * created") assemble/steal the owner's already-built structure for themselves.</p>
     *
     * <p>Succeeds (creates a brand-new session — new random {@link VehicleBuildSession#sessionId()},
     * fresh expiry — replacing whatever was previously stored) in every other case: no prior
     * session at this key, a prior session that has since expired, a prior session already {@link
     * VehicleBuildSessionStatus#CONSUMED consumed}, or a prior session belonging to this SAME
     * {@code playerId} (a player re-selecting AIR legitimately replaces only their OWN prior
     * session, e.g. to restart after walking away and coming back).</p>
     */
    public static VehicleBuildSession create(UUID playerId, String dimensionId, BlockCoord wheelPos,
                                              VehicleClass vehicleClass, long nowMillis, long ttlMillis) {
        SessionKey key = new SessionKey(dimensionId, wheelPos);
        VehicleBuildSession candidate = new VehicleBuildSession(
                UUID.randomUUID(), playerId, dimensionId, wheelPos, vehicleClass,
                VehicleBuildSessionStatus.ACTIVE, nowMillis + ttlMillis);

        // Atomic compute (not get-then-put) so the "is there a live different-owner session"
        // check and the write happen as one indivisible step -- no TOCTOU window for a second
        // create() call to slip in between them.
        VehicleBuildSession[] outcome = new VehicleBuildSession[1];
        SESSIONS_BY_WHEEL.compute(key, (k, existing) -> {
            if (existing != null && isActiveAndUnexpired(existing, nowMillis) && !existing.playerId().equals(playerId)) {
                outcome[0] = null; // refused -- existing session left in place, unmodified
                return existing;
            }
            outcome[0] = candidate;
            return candidate;
        });
        return outcome[0];
    }

    private static boolean isActiveAndUnexpired(VehicleBuildSession session, long nowMillis) {
        return session.status() == VehicleBuildSessionStatus.ACTIVE && session.expiresAtMillis() > nowMillis;
    }

    /**
     * Stores an already-constructed session verbatim, keyed by its own {@link
     * VehicleBuildSession#dimensionId()}/{@link VehicleBuildSession#wheelPos()} fields (test hook
     * — e.g. to force a specific sessionId/status).
     */
    public static void put(VehicleBuildSession session) {
        putAt(session.dimensionId(), session.wheelPos(), session);
    }

    /**
     * Stores {@code session} under an explicitly-given key, independent of the session's own
     * {@link VehicleBuildSession#dimensionId()}/{@link VehicleBuildSession#wheelPos()} fields (test
     * hook). Lets a test construct a deliberately-inconsistent fixture — a session that's
     * <em>findable</em> under one key but whose own recorded fields claim something else — to prove
     * {@link VehicleBuildSessionValidator} independently re-checks those fields rather than
     * trusting "found at this key, so it must be fine" (defense in depth; see
     * {@code BuildSessionAuthorizationGameTest#axis2WrongDimensionRejected}).
     */
    public static void putAt(String dimensionId, BlockCoord wheelPos, VehicleBuildSession session) {
        SESSIONS_BY_WHEEL.put(new SessionKey(dimensionId, wheelPos), session);
    }

    /** The session currently bound to {@code wheelPos} in {@code dimensionId}, or {@code null} if none exists. */
    public static VehicleBuildSession findByWheel(String dimensionId, BlockCoord wheelPos) {
        return SESSIONS_BY_WHEEL.get(new SessionKey(dimensionId, wheelPos));
    }

    /** Atomically marks the session at {@code (dimensionId, wheelPos)} {@link VehicleBuildSessionStatus#CONSUMED}, if present. */
    public static void consume(String dimensionId, BlockCoord wheelPos) {
        SESSIONS_BY_WHEEL.computeIfPresent(new SessionKey(dimensionId, wheelPos), (key, session) -> session.consumed());
    }

    /**
     * Test isolation hook AND production lifecycle hook: clears every tracked session
     * unconditionally. Wired to {@code ServerLifecycleEvents.SERVER_STOPPED} in production (see
     * {@code dev.sharkengine.ship.BuildSessionGate#registerLifecycleHooks}) so a session can never
     * survive into a different world/server instance loaded later in the same JVM.
     */
    public static void clearAll() {
        SESSIONS_BY_WHEEL.clear();
    }

    /**
     * Removes every session whose {@link VehicleBuildSession#expiresAtMillis()} is at or before
     * {@code nowMillis} (REQ-003 lifecycle-cleanup fix, reviewer-reported). Without a periodic
     * reaper, a session for a wheel nobody ever looks up again (e.g. the player abandoned the
     * build and never returned) would sit in this JVM-static map forever — {@link #findByWheel}
     * only ever removes an entry when something actively queries or overwrites that exact key.
     * Wired to a periodic {@code ServerTickEvents.END_SERVER_TICK} sweep in production (see {@code
     * BuildSessionGate#registerLifecycleHooks}).
     */
    public static void evictExpired(long nowMillis) {
        SESSIONS_BY_WHEEL.values().removeIf(session -> session.expiresAtMillis() <= nowMillis);
    }
}
