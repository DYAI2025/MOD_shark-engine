package dev.sharkengine.ship.session;

import dev.sharkengine.ship.VehicleClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JUnit coverage for REQ-003/AC-003 — no Fabric bootstrap needed (see
 * {@link VehicleBuildSessionValidator}'s javadoc).
 *
 * <p><b>Sharpened test contract</b> (test-plan REQ-003, not redesigned here): six independent
 * invalid axes — non-owner player, wrong dimension, out-of-range distance, expired session,
 * wrong/absent session id, duplicate/replayed request — each verified in isolation (all other
 * fields held valid), plus one positive control proving the matrix isn't vacuously
 * "always rejects," plus a two-session isolation check. Every axis test asserts the reason set is
 * EXACTLY the one expected reason (not just "contains it"), which is what actually proves the
 * other five checks ran and passed rather than being masked by a short-circuiting implementation.</p>
 */
@DisplayName("VehicleBuildSessionValidator (REQ-003/AC-003)")
class VehicleBuildSessionValidationTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final BlockCoord WHEEL_POS = new BlockCoord(100, 64, 100);
    private static final long NOW = 1_000_000L;

    private static VehicleBuildSession validSession() {
        return new VehicleBuildSession(UUID.randomUUID(), OWNER, OVERWORLD, WHEEL_POS,
                VehicleClass.AIR, VehicleBuildSessionStatus.ACTIVE, NOW + 60_000L);
    }

    private static VehicleBuildSessionRequest validRequestFor(VehicleBuildSession session) {
        return new VehicleBuildSessionRequest(session.playerId(), session.dimensionId(),
                session.wheelPos(), session.sessionId(), NOW);
    }

    @Nested
    @DisplayName("Positive control")
    class PositiveControl {
        @Test
        @DisplayName("all six fields valid -> accepted (matrix is not vacuously all-reject)")
        void allValidIsAccepted() {
            VehicleBuildSession session = validSession();
            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, validRequestFor(session));

            assertTrue(result.isValid(), "expected acceptance, got reasons=" + result.reasons());
            assertTrue(result.reasons().isEmpty());
        }
    }

    @Nested
    @DisplayName("Six independent invalid axes")
    class IndependentAxes {

        @Test
        @DisplayName("AXIS 1: non-owner player -> NON_OWNER only")
        void nonOwnerPlayerRejectedOnlyForThatAxis() {
            VehicleBuildSession session = validSession();
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    STRANGER, session.dimensionId(), session.wheelPos(), session.sessionId(), NOW);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertFalse(result.isValid());
            assertEquals(Set.of(VehicleBuildSessionRejectionReason.NON_OWNER), result.reasons());
        }

        @Test
        @DisplayName("AXIS 2: wrong dimension -> WRONG_DIMENSION only")
        void wrongDimensionRejectedOnlyForThatAxis() {
            VehicleBuildSession session = validSession();
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    session.playerId(), NETHER, session.wheelPos(), session.sessionId(), NOW);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertFalse(result.isValid());
            assertEquals(Set.of(VehicleBuildSessionRejectionReason.WRONG_DIMENSION), result.reasons());
        }

        @Test
        @DisplayName("AXIS 3: out-of-range distance -> OUT_OF_RANGE_DISTANCE only")
        void outOfRangeDistanceRejectedOnlyForThatAxis() {
            VehicleBuildSession session = validSession();
            BlockCoord farAway = new BlockCoord(
                    session.wheelPos().x() + 1000, session.wheelPos().y(), session.wheelPos().z());
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    session.playerId(), session.dimensionId(), farAway, session.sessionId(), NOW);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertFalse(result.isValid());
            assertEquals(Set.of(VehicleBuildSessionRejectionReason.OUT_OF_RANGE_DISTANCE), result.reasons());
        }

        @Test
        @DisplayName("boundary: exactly at MAX_DISTANCE_BLOCKS is still in range")
        void exactlyAtMaxDistanceIsInRange() {
            VehicleBuildSession session = validSession();
            BlockCoord atLimit = new BlockCoord(
                    session.wheelPos().x() + (int) VehicleBuildSessionValidator.MAX_DISTANCE_BLOCKS,
                    session.wheelPos().y(), session.wheelPos().z());
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    session.playerId(), session.dimensionId(), atLimit, session.sessionId(), NOW);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertTrue(result.isValid(), "expected exactly-at-limit distance to be in range, got " + result.reasons());
        }

        @Test
        @DisplayName("AXIS 4: expired session -> EXPIRED only")
        void expiredSessionRejectedOnlyForThatAxis() {
            VehicleBuildSession session = validSession();
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    session.playerId(), session.dimensionId(), session.wheelPos(), session.sessionId(),
                    session.expiresAtMillis() + 1);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertFalse(result.isValid());
            assertEquals(Set.of(VehicleBuildSessionRejectionReason.EXPIRED), result.reasons());
        }

        @Test
        @DisplayName("AXIS 5a: wrong session id -> INVALID_SESSION_ID only")
        void wrongSessionIdRejectedOnlyForThatAxis() {
            VehicleBuildSession session = validSession();
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    session.playerId(), session.dimensionId(), session.wheelPos(), UUID.randomUUID(), NOW);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertFalse(result.isValid());
            assertEquals(Set.of(VehicleBuildSessionRejectionReason.INVALID_SESSION_ID), result.reasons());
        }

        @Test
        @DisplayName("AXIS 5b: absent (null) session id -> INVALID_SESSION_ID only")
        void absentSessionIdRejectedOnlyForThatAxis() {
            VehicleBuildSession session = validSession();
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    session.playerId(), session.dimensionId(), session.wheelPos(), null, NOW);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertFalse(result.isValid());
            assertEquals(Set.of(VehicleBuildSessionRejectionReason.INVALID_SESSION_ID), result.reasons());
        }

        @Test
        @DisplayName("no session found at all -> INVALID_SESSION_ID (nothing else to check against)")
        void nullSessionRejectedAsInvalidSessionId() {
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    OWNER, OVERWORLD, WHEEL_POS, UUID.randomUUID(), NOW);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(null, request);

            assertFalse(result.isValid());
            assertEquals(Set.of(VehicleBuildSessionRejectionReason.INVALID_SESSION_ID), result.reasons());
        }

        @Test
        @DisplayName("AXIS 6: duplicate/replayed request (already-consumed session) -> ALREADY_CONSUMED only")
        void alreadyConsumedSessionRejectedOnlyForThatAxis() {
            VehicleBuildSession session = validSession().consumed();
            VehicleBuildSessionRequest request = validRequestFor(session);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertFalse(result.isValid());
            assertEquals(Set.of(VehicleBuildSessionRejectionReason.ALREADY_CONSUMED), result.reasons());
        }

        @Test
        @DisplayName("multiple simultaneously-broken axes are all reported at once, not masked")
        void multipleBrokenAxesAllReported() {
            VehicleBuildSession session = validSession().consumed();
            VehicleBuildSessionRequest request = new VehicleBuildSessionRequest(
                    STRANGER, NETHER, session.wheelPos(), null, session.expiresAtMillis() + 1);

            VehicleBuildSessionValidation result = VehicleBuildSessionValidator.validate(session, request);

            assertEquals(Set.of(
                    VehicleBuildSessionRejectionReason.NON_OWNER,
                    VehicleBuildSessionRejectionReason.WRONG_DIMENSION,
                    VehicleBuildSessionRejectionReason.EXPIRED,
                    VehicleBuildSessionRejectionReason.INVALID_SESSION_ID,
                    VehicleBuildSessionRejectionReason.ALREADY_CONSUMED
            ), result.reasons());
        }
    }

    @Nested
    @DisplayName("Two-session isolation")
    class SessionIsolation {
        @Test
        @DisplayName("concurrent sessions on two different wheels/players don't cross-validate")
        void concurrentSessionsDoNotLeak() {
            UUID playerA = UUID.randomUUID();
            UUID playerB = UUID.randomUUID();
            VehicleBuildSession sessionA = new VehicleBuildSession(UUID.randomUUID(), playerA, OVERWORLD,
                    new BlockCoord(0, 1, 0), VehicleClass.AIR, VehicleBuildSessionStatus.ACTIVE, NOW + 60_000L);
            VehicleBuildSession sessionB = new VehicleBuildSession(UUID.randomUUID(), playerB, OVERWORLD,
                    new BlockCoord(50, 1, 50), VehicleClass.AIR, VehicleBuildSessionStatus.ACTIVE, NOW + 60_000L);

            // Each session validates fine against its own owner's request.
            assertTrue(VehicleBuildSessionValidator.validate(sessionA, validRequestFor(sessionA)).isValid());
            assertTrue(VehicleBuildSessionValidator.validate(sessionB, validRequestFor(sessionB)).isValid());

            // Player B's request checked against session A is rejected (non-owner, wrong session id).
            VehicleBuildSessionValidation crossA = VehicleBuildSessionValidator.validate(sessionA, validRequestFor(sessionB));
            assertFalse(crossA.isValid());
            assertTrue(crossA.rejectedFor(VehicleBuildSessionRejectionReason.NON_OWNER));
            assertTrue(crossA.rejectedFor(VehicleBuildSessionRejectionReason.INVALID_SESSION_ID));

            // Player A's request checked against session B is rejected the same way, symmetrically.
            VehicleBuildSessionValidation crossB = VehicleBuildSessionValidator.validate(sessionB, validRequestFor(sessionA));
            assertFalse(crossB.isValid());
            assertTrue(crossB.rejectedFor(VehicleBuildSessionRejectionReason.NON_OWNER));

            // Consuming session A must not affect session B's independent status.
            VehicleBuildSession consumedA = sessionA.consumed();
            assertTrue(VehicleBuildSessionValidator.validate(sessionB, validRequestFor(sessionB)).isValid());
            assertFalse(VehicleBuildSessionValidator.validate(consumedA, validRequestFor(consumedA)).isValid());
        }
    }

    @Nested
    @DisplayName("VehicleBuildSessionRegistry (wheel-keyed store backing the gate)")
    class RegistryTests {

        @AfterEach
        void clearRegistry() {
            VehicleBuildSessionRegistry.clearAll();
        }

        @Test
        @DisplayName("create() stores an ACTIVE session findable by (dimension, wheel position)")
        void createStoresActiveSession() {
            BlockCoord wheel = new BlockCoord(1, 2, 3);
            VehicleBuildSession created = VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW);

            VehicleBuildSession found = VehicleBuildSessionRegistry.findByWheel(OVERWORLD, wheel);
            assertEquals(created, found);
            assertEquals(VehicleBuildSessionStatus.ACTIVE, found.status());
        }

        @Test
        @DisplayName("findByWheel() returns null for an unknown wheel position")
        void findByWheelReturnsNullWhenAbsent() {
            assertNull(VehicleBuildSessionRegistry.findByWheel(OVERWORLD, new BlockCoord(999, 999, 999)));
        }

        @Test
        @DisplayName("consume() flips status to CONSUMED in place")
        void consumeFlipsStatus() {
            BlockCoord wheel = new BlockCoord(4, 5, 6);
            VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW);

            VehicleBuildSessionRegistry.consume(OVERWORLD, wheel);

            assertEquals(VehicleBuildSessionStatus.CONSUMED, VehicleBuildSessionRegistry.findByWheel(OVERWORLD, wheel).status());
        }

        @Test
        @DisplayName("create() by the SAME owner at the same wheel/dimension replaces their own prior session")
        void createBySameOwnerReplacesTheirOwnPriorSession() {
            BlockCoord wheel = new BlockCoord(7, 8, 9);
            VehicleBuildSession first = VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW);
            VehicleBuildSession second = VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW + 1);

            assertTrue(second != null && !first.sessionId().equals(second.sessionId()),
                    "expected the same owner's re-selection to produce a fresh session");
            VehicleBuildSession found = VehicleBuildSessionRegistry.findByWheel(OVERWORLD, wheel);
            assertEquals(second.sessionId(), found.sessionId());
            assertEquals(OWNER, found.playerId());
        }

        @Test
        @DisplayName("REGRESSION (session theft): create() by a DIFFERENT owner is refused while the "
                + "existing session is ACTIVE and unexpired -- reviewer-reported")
        void createByDifferentOwnerIsRefusedWhileExistingSessionIsActiveAndUnexpired() {
            BlockCoord wheel = new BlockCoord(10, 11, 12);
            VehicleBuildSession original = VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW);

            VehicleBuildSession stolen = VehicleBuildSessionRegistry.create(STRANGER, OVERWORLD, wheel, VehicleClass.AIR, NOW + 1);

            assertNull(stolen, "expected a different, still-live owner's session to refuse eviction");
            VehicleBuildSession found = VehicleBuildSessionRegistry.findByWheel(OVERWORLD, wheel);
            assertEquals(original.sessionId(), found.sessionId(), "the original owner's session must survive untouched");
            assertEquals(OWNER, found.playerId());
            assertEquals(VehicleBuildSessionStatus.ACTIVE, found.status());
        }

        @Test
        @DisplayName("create() by a DIFFERENT owner succeeds once the existing session has EXPIRED "
                + "(the guard isn't a blanket never-overwrite)")
        void createByDifferentOwnerSucceedsOnceExistingSessionExpired() {
            BlockCoord wheel = new BlockCoord(13, 14, 15);
            VehicleBuildSession original = VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW, 1L);

            // nowMillis for the second create() is past the first session's 1ms-TTL expiry.
            VehicleBuildSession replacement = VehicleBuildSessionRegistry.create(STRANGER, OVERWORLD, wheel, VehicleClass.AIR, NOW + 100L);

            assertTrue(replacement != null, "expected create() to succeed once the prior session had expired");
            assertFalse(original.sessionId().equals(replacement.sessionId()));
            VehicleBuildSession found = VehicleBuildSessionRegistry.findByWheel(OVERWORLD, wheel);
            assertEquals(STRANGER, found.playerId());
        }

        @Test
        @DisplayName("create() by a DIFFERENT owner succeeds once the existing session was CONSUMED "
                + "(a completed/assembled session no longer blocks a fresh wheel there)")
        void createByDifferentOwnerSucceedsOnceExistingSessionConsumed() {
            BlockCoord wheel = new BlockCoord(16, 17, 18);
            VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW);
            VehicleBuildSessionRegistry.consume(OVERWORLD, wheel);

            VehicleBuildSession replacement = VehicleBuildSessionRegistry.create(STRANGER, OVERWORLD, wheel, VehicleClass.AIR, NOW + 1);

            assertTrue(replacement != null, "expected create() to succeed once the prior session had been consumed");
            assertEquals(STRANGER, VehicleBuildSessionRegistry.findByWheel(OVERWORLD, wheel).playerId());
        }

        @Test
        @DisplayName("REGRESSION: create() at the same raw coordinates in a DIFFERENT dimension does "
                + "NOT evict the other dimension's session (reviewer-reported cross-dimension collision)")
        void createInDifferentDimensionAtSameCoordDoesNotEvictOtherDimensionsSession() {
            BlockCoord wheel = new BlockCoord(0, 64, 0);

            VehicleBuildSession overworldSession =
                    VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW);
            // A completely unrelated player creates a session for an unrelated wheel that happens
            // to sit at the exact same raw block coordinate, but in a different dimension.
            VehicleBuildSession netherSession =
                    VehicleBuildSessionRegistry.create(STRANGER, NETHER, wheel, VehicleClass.AIR, NOW);

            VehicleBuildSession foundOverworld = VehicleBuildSessionRegistry.findByWheel(OVERWORLD, wheel);
            VehicleBuildSession foundNether = VehicleBuildSessionRegistry.findByWheel(NETHER, wheel);

            assertEquals(overworldSession.sessionId(), foundOverworld.sessionId(),
                    "creating a Nether session at the same raw coordinate must not evict the Overworld session");
            assertEquals(OWNER, foundOverworld.playerId());
            assertEquals(VehicleBuildSessionStatus.ACTIVE, foundOverworld.status());
            assertEquals(netherSession.sessionId(), foundNether.sessionId());
            assertEquals(STRANGER, foundNether.playerId());

            // The owner's own subsequent, legitimate request against their own session still
            // validates cleanly -- unaffected by the unrelated Nether session's existence.
            VehicleBuildSessionValidation ownerCheck =
                    VehicleBuildSessionValidator.validate(foundOverworld, validRequestFor(foundOverworld));
            assertTrue(ownerCheck.isValid(), "expected owner's session to remain valid, got " + ownerCheck.reasons());
        }
    }

    @Nested
    @DisplayName("REGRESSION (lifecycle cleanup): evictExpired -- reviewer-reported unbounded growth")
    class EvictExpiredTests {

        @AfterEach
        void clearRegistry() {
            VehicleBuildSessionRegistry.clearAll();
        }

        @Test
        @DisplayName("evictExpired() removes only sessions whose expiry has passed, leaving unexpired ones intact")
        void evictExpiredRemovesOnlyExpiredSessions() {
            BlockCoord expiredWheel = new BlockCoord(20, 20, 20);
            BlockCoord liveWheel = new BlockCoord(30, 30, 30);
            VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, expiredWheel, VehicleClass.AIR, NOW, 1L);
            VehicleBuildSession live = VehicleBuildSessionRegistry.create(STRANGER, OVERWORLD, liveWheel, VehicleClass.AIR, NOW, 60_000L);

            VehicleBuildSessionRegistry.evictExpired(NOW + 100L);

            assertNull(VehicleBuildSessionRegistry.findByWheel(OVERWORLD, expiredWheel),
                    "expected the expired session to have been reaped");
            VehicleBuildSession stillFound = VehicleBuildSessionRegistry.findByWheel(OVERWORLD, liveWheel);
            assertEquals(live.sessionId(), stillFound.sessionId(), "expected the unexpired session to survive the sweep");
        }

        @Test
        @DisplayName("evictExpired() at a time before any expiry leaves everything untouched")
        void evictExpiredBeforeAnyExpiryIsANoop() {
            BlockCoord wheel = new BlockCoord(40, 40, 40);
            VehicleBuildSession session = VehicleBuildSessionRegistry.create(OWNER, OVERWORLD, wheel, VehicleClass.AIR, NOW, 60_000L);

            VehicleBuildSessionRegistry.evictExpired(NOW);

            assertEquals(session.sessionId(), VehicleBuildSessionRegistry.findByWheel(OVERWORLD, wheel).sessionId());
        }
    }
}
