package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.net.BuilderPreviewS2CPayload;
import dev.sharkengine.ship.BuildSessionGate;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.VehicleClass;
import dev.sharkengine.ship.session.BlockCoord;
import dev.sharkengine.ship.session.VehicleBuildSession;
import dev.sharkengine.ship.session.VehicleBuildSessionRegistry;
import dev.sharkengine.ship.session.VehicleBuildSessionRejectionReason;
import dev.sharkengine.ship.session.VehicleBuildSessionStatus;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REQ-003/AC-003: full server-authoritative flow with a real {@code ServerPlayer} and dimension,
 * exercising the exact production entry point ({@link BuildSessionGate#tryAssemble}) that {@code
 * ModNetworking}'s C2S assemble handler calls — mirrors {@code VehicleRoutePopupGameTest}'s
 * discipline of calling production hooks directly rather than re-implementing them.
 *
 * <p>Six independent invalid axes (non-owner player, wrong dimension, out-of-range distance,
 * expired session, wrong/absent session id, duplicate/replayed request) + one positive control +
 * a two-player session-isolation test. Every negative test asserts a full block-diff (the entire
 * placed structure, snapshotted before/after) AND an entity-count diff of zero, not just
 * "no ship spawned" — per the test-plan counter-thesis that a partial-mutation-before-rejection
 * bug would otherwise slip through.</p>
 */
public final class BuildSessionAuthorizationGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);
    private static final BlockPos WHEEL_POS_B = new BlockPos(3, 1, 20);

    /** All positions the valid structure at {@code wheelPos} occupies, relative to it. */
    private static final BlockPos[] STRUCTURE_OFFSETS = {
            BlockPos.ZERO,
            new BlockPos(0, 0, -1),
            new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(0, 0, -2),
            new BlockPos(-2, 0, 0),
    };

    private static void placeValidStructure(GameTestHelper helper, BlockPos wheelPos) {
        helper.setBlock(wheelPos, ModBlocks.STEERING_WHEEL);
        helper.setBlock(wheelPos.north(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.south(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.east(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.west(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.above(), ModBlocks.THRUSTER);
        // REQ-005: assembly now also requires exactly one pilot seat.
        helper.setBlock(wheelPos.west().west(), ModBlocks.PILOT_SEAT);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(wheelPos.north().north(), bug);
    }

    private static Map<BlockPos, BlockState> snapshot(GameTestHelper helper, BlockPos wheelPos) {
        Map<BlockPos, BlockState> snap = new LinkedHashMap<>();
        for (BlockPos offset : STRUCTURE_OFFSETS) {
            BlockPos p = wheelPos.offset(offset);
            snap.put(p, helper.getBlockState(p));
        }
        return snap;
    }

    private static int shipEntityCount(GameTestHelper helper) {
        AABB region = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48);
        return helper.getLevel().getEntities(ModEntities.SHIP, region, e -> true).size();
    }

    private static void assertZeroWorldDiff(GameTestHelper helper, BlockPos wheelPos,
                                             Map<BlockPos, BlockState> before, int entitiesBefore, String context) {
        Map<BlockPos, BlockState> after = snapshot(helper, wheelPos);
        if (!after.equals(before)) {
            helper.fail(context + ": expected zero block-diff, but the structure changed. before=" + before + " after=" + after);
        }
        int entitiesAfter = shipEntityCount(helper);
        if (entitiesAfter != entitiesBefore) {
            helper.fail(context + ": expected zero entity-count diff, before=" + entitiesBefore + " after=" + entitiesAfter);
        }
    }

    private static void assertExactly(GameTestHelper helper, VehicleBuildSessionRejectionReason expected,
                                       Set<VehicleBuildSessionRejectionReason> actual, String context) {
        if (!actual.equals(Set.of(expected))) {
            helper.fail(context + ": expected reasons={" + expected + "} only, got " + actual);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void axis1NonOwnerPlayerRejected(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);
        Map<BlockPos, BlockState> before = snapshot(helper, WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession session = BuildSessionGate.createAirSession(owner, wheelPos);

        ServerPlayer intruder = helper.makeMockServerPlayerInLevel();
        intruder.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);

        BuildSessionGate.AssembleAttemptResult attempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, intruder, session.sessionId());

        if (attempt.isAuthorized()) {
            helper.fail("expected a non-owner player's assembly request to be rejected");
            return;
        }
        assertExactly(helper, VehicleBuildSessionRejectionReason.NON_OWNER, attempt.authorization().reasons(), "axis1");
        assertZeroWorldDiff(helper, WHEEL_POS, before, entitiesBefore, "axis1");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void axis2WrongDimensionRejected(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);
        Map<BlockPos, BlockState> before = snapshot(helper, WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer requester = helper.makeMockServerPlayerInLevel();
        requester.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);

        // The GameTest server only ever runs one real dimension (minecraft:overworld), so a real
        // second-dimension level isn't available to instantiate here. VehicleBuildSessionRegistry
        // is now keyed by (dimension, wheelPos) -- see its javadoc -- so a session hand-inserted
        // under a mismatched dimension key at this same coordinate would simply never be FOUND by
        // BuildSessionGate.tryAssemble's own lookup (which correctly scopes to the requester's
        // real current dimension), producing INVALID_SESSION_ID rather than WRONG_DIMENSION.
        //
        // Instead: create a REAL session through the production entry point (createAirSession), so
        // it's stored under exactly the composite key the real lookup will query -- genuinely
        // findable -- then overwrite ONLY its own recorded dimensionId field to a mismatched value
        // via the registry's putAt() test hook, re-filing it at that SAME (findable) key. This
        // simulates a session that became inconsistent with its own storage key (e.g. stale data)
        // and proves VehicleBuildSessionValidator independently re-checks the dimension field
        // rather than trusting "found at this key, so it must be fine" -- exactly the kind of gap
        // the test-plan's counter-thesis warns about ("validates one field carefully while silently
        // trusting another").
        VehicleBuildSession real = BuildSessionGate.createAirSession(requester, wheelPos);
        VehicleBuildSession spoofedDimension = new VehicleBuildSession(
                real.sessionId(), real.playerId(), "minecraft:the_nether", real.wheelPos(),
                real.vehicleClass(), real.status(), real.expiresAtMillis());
        VehicleBuildSessionRegistry.putAt(real.dimensionId(), real.wheelPos(), spoofedDimension);

        BuildSessionGate.AssembleAttemptResult attempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, requester, real.sessionId());

        if (attempt.isAuthorized()) {
            helper.fail("expected a session whose recorded dimension mismatches the requester's actual dimension to be rejected");
            return;
        }
        assertExactly(helper, VehicleBuildSessionRejectionReason.WRONG_DIMENSION, attempt.authorization().reasons(), "axis2");
        assertZeroWorldDiff(helper, WHEEL_POS, before, entitiesBefore, "axis2");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void crossDimensionCoordinateCollisionDoesNotEvictSession(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession ownerSession = BuildSessionGate.createAirSession(owner, wheelPos);

        // A completely unrelated player creates a session for an unrelated wheel that happens to
        // sit at the exact same raw block coordinate, but in a different dimension (Overworld/
        // Nether/End coordinate coincidence near spawn/portals is common in real play).
        // Regression test for the reviewer-reported bug: this must NOT evict/replace the owner's
        // own, unrelated, still-active session (the registry previously keyed by BlockCoord alone,
        // so the second create() silently clobbered the first).
        BlockCoord coord = new BlockCoord(wheelPos.getX(), wheelPos.getY(), wheelPos.getZ());
        VehicleBuildSessionRegistry.create(UUID.randomUUID(), "minecraft:the_nether", coord,
                VehicleClass.AIR, System.currentTimeMillis());

        BuildSessionGate.AssembleAttemptResult ownAttempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, ownerSession.sessionId());

        if (!ownAttempt.isAuthorized()) {
            helper.fail("expected the owner's own session to survive an unrelated same-coordinate "
                    + "session created in a different dimension, got " + ownAttempt.authorization().reasons());
            return;
        }
        if (!ownAttempt.assembleResult().isSuccess()) {
            helper.fail("expected the owner's assembly to actually succeed, got "
                    + ownAttempt.assembleResult().translationKey());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void axis3OutOfRangeDistanceRejected(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);
        Map<BlockPos, BlockState> before = snapshot(helper, WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession session = BuildSessionGate.createAirSession(owner, wheelPos);

        // Walk far away before sending the assembly request.
        owner.setPos(wheelPos.getX() + 500.5, wheelPos.getY(), wheelPos.getZ() + 0.5);

        BuildSessionGate.AssembleAttemptResult attempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, session.sessionId());

        if (attempt.isAuthorized()) {
            helper.fail("expected an out-of-range player's assembly request to be rejected");
            return;
        }
        assertExactly(helper, VehicleBuildSessionRejectionReason.OUT_OF_RANGE_DISTANCE, attempt.authorization().reasons(), "axis3");
        assertZeroWorldDiff(helper, WHEEL_POS, before, entitiesBefore, "axis3");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void axis4ExpiredSessionRejected(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);
        Map<BlockPos, BlockState> before = snapshot(helper, WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);

        BlockCoord coord = new BlockCoord(wheelPos.getX(), wheelPos.getY(), wheelPos.getZ());
        // Created far in the past with a 1ms TTL: already expired relative to real wall-clock time.
        VehicleBuildSession session = VehicleBuildSessionRegistry.create(
                owner.getUUID(), "minecraft:overworld", coord, VehicleClass.AIR, 0L, 1L);

        BuildSessionGate.AssembleAttemptResult attempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, session.sessionId());

        if (attempt.isAuthorized()) {
            helper.fail("expected an expired session's assembly request to be rejected");
            return;
        }
        assertExactly(helper, VehicleBuildSessionRejectionReason.EXPIRED, attempt.authorization().reasons(), "axis4");
        assertZeroWorldDiff(helper, WHEEL_POS, before, entitiesBefore, "axis4");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void axis5WrongOrAbsentSessionIdRejected(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);
        Map<BlockPos, BlockState> before = snapshot(helper, WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        BuildSessionGate.createAirSession(owner, wheelPos);

        // Wrong id.
        BuildSessionGate.AssembleAttemptResult wrongIdAttempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, UUID.randomUUID());
        if (wrongIdAttempt.isAuthorized()) {
            helper.fail("expected a wrong session id to be rejected");
            return;
        }
        assertExactly(helper, VehicleBuildSessionRejectionReason.INVALID_SESSION_ID, wrongIdAttempt.authorization().reasons(), "axis5-wrong");
        assertZeroWorldDiff(helper, WHEEL_POS, before, entitiesBefore, "axis5-wrong");

        // Absent (null) id -- session must still be ACTIVE (the wrong-id attempt above must not
        // have consumed it), so this is independently exercised too.
        BuildSessionGate.AssembleAttemptResult absentIdAttempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, null);
        if (absentIdAttempt.isAuthorized()) {
            helper.fail("expected an absent session id to be rejected");
            return;
        }
        assertExactly(helper, VehicleBuildSessionRejectionReason.INVALID_SESSION_ID, absentIdAttempt.authorization().reasons(), "axis5-absent");
        assertZeroWorldDiff(helper, WHEEL_POS, before, entitiesBefore, "axis5-absent");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void axis6DuplicateReplayedRequestRejected(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession session = BuildSessionGate.createAirSession(owner, wheelPos);

        BuildSessionGate.AssembleAttemptResult first =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, session.sessionId());
        if (!first.isAuthorized()) {
            helper.fail("expected the FIRST request to succeed (positive control precondition), got reasons="
                    + first.authorization().reasons());
            return;
        }

        // Snapshot the post-assembly world state, then replay the exact same request.
        Map<BlockPos, BlockState> afterFirst = snapshot(helper, WHEEL_POS);
        int entitiesAfterFirst = shipEntityCount(helper);

        BuildSessionGate.AssembleAttemptResult replay =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, session.sessionId());

        if (replay.isAuthorized()) {
            helper.fail("expected a replayed (duplicate) request against an already-consumed session to be rejected");
            return;
        }
        assertExactly(helper, VehicleBuildSessionRejectionReason.ALREADY_CONSUMED, replay.authorization().reasons(), "axis6");
        assertZeroWorldDiff(helper, WHEEL_POS, afterFirst, entitiesAfterFirst, "axis6 (post-first-assembly state must not change again)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void authorizedButStructurallyInvalidAttemptDoesNotConsumeSession(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        // Start from the same valid structure every other test uses, then remove ONLY the
        // Thruster -- passes REQ-003's own six authorization axes, but ShipAssemblyService's own
        // structural check ("at least 1 Thruster") must still reject it. (placeValidStructure
        // takes RELATIVE test-structure coordinates, same as every other test in this class --
        // helper.setBlock must never be called with the ABSOLUTE wheelPos.)
        placeValidStructure(helper, WHEEL_POS);
        helper.setBlock(WHEEL_POS.above(), Blocks.AIR);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession session = BuildSessionGate.createAirSession(owner, wheelPos);

        // First attempt: authorized (all six REQ-003 axes pass -- owner, dimension, distance,
        // expiry, session id all valid, session ACTIVE) but structurally invalid (missing
        // Thruster). REGRESSION for the reviewer-reported bug: this must NOT permanently consume
        // the session -- a purely structural rejection is not a REQ-003 authorization failure.
        BuildSessionGate.AssembleAttemptResult firstAttempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, session.sessionId());
        if (!firstAttempt.isAuthorized()) {
            helper.fail("expected the session gate to authorize this attempt (all six REQ-003 axes "
                    + "are valid); got " + firstAttempt.authorization().reasons());
            return;
        }
        if (firstAttempt.assembleResult().isSuccess()) {
            helper.fail("expected assembly to fail structurally (missing Thruster) as test precondition, got "
                    + firstAttempt.assembleResult().translationKey());
            return;
        }

        VehicleBuildSession afterFirst = VehicleBuildSessionRegistry.findByWheel(
                "minecraft:overworld", new BlockCoord(wheelPos.getX(), wheelPos.getY(), wheelPos.getZ()));
        if (afterFirst == null || afterFirst.status() != VehicleBuildSessionStatus.ACTIVE) {
            helper.fail("expected the session to remain ACTIVE after a purely structural (non-REQ-003) "
                    + "rejection, got " + (afterFirst == null ? "null (session vanished)" : afterFirst.status()));
            return;
        }

        // Fix the structure (add the missing Thruster back) and retry with the SAME session id --
        // must now succeed, proving the session survived the earlier structural failure instead
        // of being permanently burned by it.
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
        BuildSessionGate.AssembleAttemptResult secondAttempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, session.sessionId());
        if (!secondAttempt.isAuthorized()) {
            helper.fail("expected the SAME session to still authorize the now-complete retry, got "
                    + secondAttempt.authorization().reasons());
            return;
        }
        if (!secondAttempt.assembleResult().isSuccess()) {
            helper.fail("expected the retry (now structurally complete) to actually succeed, got "
                    + secondAttempt.assembleResult().translationKey());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void positiveControlAllValidCreatesAndAssembles(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession session = BuildSessionGate.createAirSession(owner, wheelPos);
        if (session.status() != VehicleBuildSessionStatus.ACTIVE) {
            helper.fail("expected a freshly-created session to be ACTIVE");
            return;
        }

        BuildSessionGate.AssembleAttemptResult attempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, session.sessionId());

        if (!attempt.isAuthorized()) {
            helper.fail("expected the fully-valid request to be authorized, got reasons=" + attempt.authorization().reasons());
            return;
        }
        if (!attempt.assembleResult().isSuccess()) {
            helper.fail("expected assembly to actually succeed once authorized, got " + attempt.assembleResult().translationKey());
            return;
        }
        // Proves the matrix isn't vacuously "always rejects": the world DID mutate (wheel block
        // gone) and a ship entity DID spawn.
        if (!helper.getBlockState(WHEEL_POS).isAir()) {
            helper.fail("expected the wheel block to be removed after a successful, authorized assembly");
            return;
        }
        int entitiesAfter = shipEntityCount(helper);
        if (entitiesAfter != entitiesBefore + 1) {
            helper.fail("expected exactly one new ship entity, before=" + entitiesBefore + " after=" + entitiesAfter);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void twoPlayerSessionIsolation(GameTestHelper helper) {
        BlockPos wheelPosA = helper.absolutePos(WHEEL_POS);
        BlockPos wheelPosB = helper.absolutePos(WHEEL_POS_B);
        placeValidStructure(helper, WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS_B);
        Map<BlockPos, BlockState> beforeA = snapshot(helper, WHEEL_POS);
        Map<BlockPos, BlockState> beforeB = snapshot(helper, WHEEL_POS_B);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer playerA = helper.makeMockServerPlayerInLevel();
        playerA.setPos(wheelPosA.getX() + 0.5, wheelPosA.getY(), wheelPosA.getZ() + 0.5);
        VehicleBuildSession sessionA = BuildSessionGate.createAirSession(playerA, wheelPosA);

        ServerPlayer playerB = helper.makeMockServerPlayerInLevel();
        playerB.setPos(wheelPosB.getX() + 0.5, wheelPosB.getY(), wheelPosB.getZ() + 0.5);
        VehicleBuildSession sessionB = BuildSessionGate.createAirSession(playerB, wheelPosB);

        // Player A walks over to wheel B (so distance/dimension are both genuinely valid there)
        // and tries to use player B's session id against player B's own wheel -- must fail purely
        // on ownership (A is not B's session owner), isolating that axis from distance, and must
        // NOT touch wheel B's structure.
        playerA.setPos(wheelPosB.getX() + 0.5, wheelPosB.getY(), wheelPosB.getZ() + 0.5);
        BuildSessionGate.AssembleAttemptResult crossAttempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPosB, playerA, sessionB.sessionId());
        if (crossAttempt.isAuthorized()) {
            helper.fail("expected player A to be rejected when using player B's wheel/session");
            return;
        }
        assertExactly(helper, VehicleBuildSessionRejectionReason.NON_OWNER, crossAttempt.authorization().reasons(), "isolation-cross");
        assertZeroWorldDiff(helper, WHEEL_POS_B, beforeB, entitiesBefore, "isolation-cross (wheel B)");
        assertZeroWorldDiff(helper, WHEEL_POS, beforeA, entitiesBefore, "isolation-cross (wheel A must be untouched too)");

        // Each player's own session against their own wheel remains valid and independent of the
        // other's -- consuming would prove real cross-contamination if the registry mixed them up.
        playerA.setPos(wheelPosA.getX() + 0.5, wheelPosA.getY(), wheelPosA.getZ() + 0.5);
        BuildSessionGate.AssembleAttemptResult ownAttemptA =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPosA, playerA, sessionA.sessionId());
        if (!ownAttemptA.isAuthorized()) {
            helper.fail("expected player A's own request against their own session to succeed, got "
                    + ownAttemptA.authorization().reasons());
            return;
        }
        BuildSessionGate.AssembleAttemptResult ownAttemptB =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPosB, playerB, sessionB.sessionId());
        if (!ownAttemptB.isAuthorized()) {
            helper.fail("expected player B's own request against their own session to succeed (unaffected by A's activity), got "
                    + ownAttemptB.authorization().reasons());
            return;
        }
        helper.succeed();
    }

    /**
     * REGRESSION (reviewer-reported session theft): a stranger reaching another player's Steering
     * Wheel and selecting AIR there must NOT be able to evict the owner's still-ACTIVE,
     * unexpired session -- {@link BuildSessionGate#createAirSession} must refuse (return {@code
     * null}), and the owner's original session must remain fully usable to actually assemble
     * their own already-built structure.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void sessionTheftViaOverwriteIsRejected(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);
        Map<BlockPos, BlockState> before = snapshot(helper, WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession ownerSession = BuildSessionGate.createAirSession(owner, wheelPos);
        if (ownerSession == null) {
            helper.fail("expected the owner's initial session creation to succeed (test precondition)");
            return;
        }

        // A stranger reaches the SAME wheel (e.g. by walking up and right-clicking it -- any
        // player can do this, it's just a placed block) and selects AIR there too, exactly like
        // TutorialService.handleModeSelection does on the production path.
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
        stranger.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession strangerAttempt = BuildSessionGate.createAirSession(stranger, wheelPos);

        if (strangerAttempt != null) {
            helper.fail("SECURITY: a stranger's createAirSession evicted the owner's still-active "
                    + "session instead of being refused");
            return;
        }

        // The owner's ORIGINAL session id must still authorize and successfully assemble the
        // owner's own already-built structure -- proving it truly survived untouched, not just
        // that the registry happens to still hold *some* session at that key.
        BuildSessionGate.AssembleAttemptResult ownerAttempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, owner, ownerSession.sessionId());
        if (!ownerAttempt.isAuthorized() || !ownerAttempt.assembleResult().isSuccess()) {
            helper.fail("expected the owner's original session to still authorize and assemble "
                    + "their own structure after the rejected theft attempt, got authorized="
                    + ownerAttempt.isAuthorized() + " reasons=" + ownerAttempt.authorization().reasons());
            return;
        }

        // The stranger, having never obtained a real session, cannot assemble the (now-owner's)
        // structure using any id of their own devising either.
        BuildSessionGate.AssembleAttemptResult strangerAssembleAttempt =
                BuildSessionGate.tryAssemble(helper.getLevel(), wheelPos, stranger, ownerSession.sessionId());
        if (strangerAssembleAttempt.isAuthorized()) {
            helper.fail("expected the stranger to remain unauthorized even against the owner's own "
                    + "(never-leaked-to-them) session id");
            return;
        }
        helper.succeed();
    }

    /**
     * REGRESSION (reviewer-reported secret leak): {@link ShipAssemblyService#openBuilderPreview}
     * must never echo a DIFFERENT player's real, still-usable session id back to a requester who
     * isn't that session's owner -- a session id is a secret bearer token, and this call path is
     * reachable both from {@code ModNetworking}'s assemble-failure branch and from {@code
     * TutorialService#handleAdvanceStage}, the latter having no ownership check of its own.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void openBuilderPreviewDoesNotLeakSessionIdToNonOwner(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);

        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);
        VehicleBuildSession session = BuildSessionGate.createAirSession(owner, wheelPos);
        if (session == null) {
            helper.fail("expected the owner's session creation to succeed (test precondition)");
            return;
        }

        ServerPlayer intruder = helper.makeMockServerPlayerInLevel();
        intruder.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);

        // Exactly the vulnerable call: any player who reaches this wheel can trigger
        // openBuilderPreview for themselves (TutorialService.handleAdvanceStage has no ownership
        // gate at all), regardless of whether they hold a session here.
        ShipAssemblyService.openBuilderPreview(helper.getLevel(), wheelPos, intruder);

        BuilderPreviewS2CPayload intruderPayload = ShipAssemblyService.lastPreviewSent(intruder.getUUID());
        if (intruderPayload == null) {
            helper.fail("expected a preview payload to have been sent to the intruder");
            return;
        }
        if (intruderPayload.sessionId() != null) {
            helper.fail("SECURITY: owner's real session id (" + session.sessionId()
                    + ") was leaked to a non-owner via openBuilderPreview, got " + intruderPayload.sessionId());
            return;
        }

        // Positive-control half: the OWNER, through the exact same call path, still legitimately
        // receives their own real session id -- proving this isn't a blanket "always null"
        // regression that would silently break the normal builder-reopen flow (T13/REQ-013).
        ShipAssemblyService.openBuilderPreview(helper.getLevel(), wheelPos, owner);
        BuilderPreviewS2CPayload ownerPayload = ShipAssemblyService.lastPreviewSent(owner.getUUID());
        if (ownerPayload == null || !session.sessionId().equals(ownerPayload.sessionId())) {
            helper.fail("expected the owner to still receive their own real session id through the "
                    + "same call path, got " + (ownerPayload == null ? "null payload" : ownerPayload.sessionId()));
            return;
        }
        helper.succeed();
    }
}
