package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.EditModeDistanceGate;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/**
 * REQ-012/AC-012 (T12) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-012 — Safe edit-mode
 * gate", the plan's own "sharpest counter-thesis in the whole plan"): the pure-logic case for
 * Euclidean-vs-Manhattan is already covered by {@code EditModeDistanceTest}; this class exercises
 * the SAME two discriminating offsets against a REAL, assembled {@link ShipEntity} and REAL
 * {@link ServerPlayer} positions, then separately proves the stationary/not-destroyed/
 * conflict-free/pilot-only preconditions each independently gate the same production entry point
 * ({@link ShipEntity#tryEnterEditMode}) regardless of distance.
 *
 * <p><b>Control Anchor:</b> {@link ShipAssemblyService#tryAssemble} spawns every {@link
 * ShipEntity} at exactly the Steering Wheel's world position, and the whole entity translates
 * uniformly in flight -- so the ship's own {@code getX()/getY()/getZ()} is the "Control Anchor"
 * OQ-001's five-block rule measures from, in flight or at rest. Every offset below is applied
 * directly against the live {@code ship.getX()/getY()/getZ()} for that reason, not against the
 * original block-placement coordinates.</p>
 *
 * <p>Scope note (explicitly out of scope for this task, per T12's DoD): this class does NOT open
 * an actual builder-menu payload or assert on any block-list content -- that is REQ-013/T13's job.
 * It asserts only the gate's own accept/reject decision and {@link
 * ShipEntity#isEditModeActive()}'s resulting state (or lack of state change on rejection).</p>
 *
 * <p><b>Dimension-mismatch precondition (security fix, attempt-1 review finding):</b> unlike {@link
 * dev.sharkengine.ship.BuildSessionGate}'s analogous dimension check (verified in {@code
 * BuildSessionAuthorizationGameTest} only by spoofing {@code VehicleBuildSessionRegistry}'s stored,
 * mutable {@code dimensionId} string, since that suite's own comment notes the GameTest server was
 * assumed to expose no real second dimension), {@link ShipEntity#tryEnterEditMode}'s {@code
 * sameDimension} check is a live {@code player.level() == this.level()} reference comparison with
 * nothing stored to spoof. Verifying it therefore requires genuinely moving a player into a
 * different real {@link net.minecraft.world.level.Level} instance -- which, verified directly
 * (see {@code editModeRejectedWhenPilotIsInADifferentRealDimensionAtMatchingCoordinates} below),
 * the GameTest server's {@code minecraft:the_nether} level does provide via {@code
 * ServerPlayer#teleportTo(ServerLevel, ...)}, contrary to that older assumption. The pure-logic
 * boolean-threading case is additionally covered at the unit level by {@code
 * EditModeDistanceTest#gateRejectsWrongDimensionEvenAtNumericallyMatchingOffsetOfZero} and {@code
 * EditModeDistanceTest#gateRejectsWrongDimensionRegardlessOfOtherwiseInRangeOffset}.</p>
 */
public final class EditModeDistanceGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /** Same forward-input tick budget style established by {@code VehicleReentryGameTest#FLIGHT_TICKS}, scaled down -- only enough ticks are needed to move {@code currentSpeed} measurably above {@link EditModeDistanceGate#STATIONARY_SPEED_EPSILON}, not to burn a full fuel bar. */
    private static final int MOVING_TICKS = 20;

    private static void placeStructure(GameTestHelper helper) {
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), ModBlocks.PILOT_SEAT);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bug);
    }

    private record AssembledShip(ShipEntity ship, ServerPlayer pilot) {}

    /**
     * Places the structure, assembles for real via the same production entry point every other
     * REQ-012-adjacent GameTest in this package uses, and returns the spawned {@link ShipEntity}
     * (pilot already mounted by {@code tryAssemble} itself) paired with that pilot -- or {@code
     * null} if a precondition failed (in which case {@code helper.fail} was already called and the
     * caller must return immediately).
     */
    private static AssembledShip assembleShip(GameTestHelper helper) {
        placeStructure(helper);
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail("test precondition: expected assembly to succeed, got " + result.translationKey());
            return null;
        }

        List<ShipEntity> ships = helper.getLevel().getEntities(
                ModEntities.SHIP, new AABB(wheelWorldPos).inflate(8), e -> true);
        if (ships.size() != 1) {
            helper.fail("test precondition: expected exactly one spawned ShipEntity, got " + ships.size());
            return null;
        }
        return new AssembledShip(ships.get(0), pilot);
    }

    private static void placePilotAtOffset(ShipEntity ship, ServerPlayer pilot, double dx, double dy, double dz) {
        pilot.setPos(ship.getX() + dx, ship.getY() + dy, ship.getZ() + dz);
    }

    /**
     * REQ-012 authorization axis (security fix, reviewer-reported attempt-1 finding): a requester
     * physically present in a REAL different dimension (the Nether, via a genuine {@link
     * ServerPlayer#teleportTo(ServerLevel, double, double, double, java.util.Set, float, float)}
     * cross-dimension teleport -- not a stubbed field) must be rejected even when their raw
     * numeric coordinates exactly match the ship's own, on an otherwise fully valid
     * (stationary/safe/conflict-free, assigned pilot) vehicle. This is the exact scenario the
     * review flagged: before the fix, {@link ShipEntity#tryEnterEditMode} computed {@code
     * player.getX/Y/Z() - this.getX/Y/Z()} without ever checking {@code player.level() ==
     * this.level()}, so a numerically-matching position in an unrelated dimension resolved to
     * offset (0,0,0) and was wrongly ACCEPTED.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void editModeRejectedWhenPilotIsInADifferentRealDimensionAtMatchingCoordinates(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        if (nether == null || nether == ship.level()) {
            helper.fail("test precondition: expected a real, distinct Nether ServerLevel instance to be available");
            return;
        }

        boolean teleported = pilot.teleportTo(nether, ship.getX(), ship.getY(), ship.getZ(),
                Set.of(), pilot.getYRot(), pilot.getXRot());
        if (!teleported || pilot.level() != nether) {
            helper.fail("test precondition: expected the pilot to genuinely be relocated into the Nether level");
            return;
        }

        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(pilot);

        if (reason != EditModeDistanceGate.Reason.REJECTED_WRONG_DIMENSION) {
            helper.fail("expected REJECTED_WRONG_DIMENSION for a requester in a different real "
                    + "dimension at numerically-matching coordinates, got " + reason);
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected NO state change on rejection -- isEditModeActive() must remain false");
            return;
        }
        helper.succeed();
    }

    /**
     * REQ-012 (accept case): offset (3,3,0) -- the exact diagonal offset the test-plan names as
     * the case that alone falsifies a Manhattan-based implementation (Euclidean ~4.24 <=5 accepts;
     * Manhattan=6 would wrongly reject). Vehicle is freshly assembled (stationary, full health, no
     * prior Edit Mode open) and the requester is the assigned pilot -- every precondition holds, so
     * this must ACCEPT and flip {@link ShipEntity#isEditModeActive()} to {@code true}.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void editModeOpensWhenWithinEuclideanDistanceAndAllPreconditionsHold(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        if (ship.isEditModeActive()) {
            helper.fail("test precondition: expected a freshly assembled ship to not already have Edit Mode active");
            return;
        }

        placePilotAtOffset(ship, pilot, 3, 3, 0);
        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(pilot);

        if (reason != EditModeDistanceGate.Reason.ACCEPTED) {
            helper.fail("expected ACCEPTED for offset (3,3,0) (Euclidean ~4.24, within the <=5 "
                    + "boundary) with every other precondition satisfied, got " + reason);
            return;
        }
        if (!ship.isEditModeActive()) {
            helper.fail("expected isEditModeActive() to be true after an ACCEPTED gate result");
            return;
        }
        helper.succeed();
    }

    /**
     * REQ-012 (reject case, distance): offset (3,4,1) -- Euclidean ~5.099, just past the <=5
     * boundary -- must REJECT even though the vehicle is otherwise stationary/safe/conflict-free
     * and the requester is the pilot, and must leave {@link ShipEntity#isEditModeActive()}
     * unchanged (AC-012: "erfolgt keine Zustandsänderung").
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void editModeRejectedWhenJustBeyondFiveBlockEuclideanDistance(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        placePilotAtOffset(ship, pilot, 3, 4, 1);
        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(pilot);

        if (reason != EditModeDistanceGate.Reason.REJECTED_TOO_FAR) {
            helper.fail("expected REJECTED_TOO_FAR for offset (3,4,1) (Euclidean ~5.099, just over "
                    + "the <=5 boundary), got " + reason);
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected NO state change on rejection -- isEditModeActive() must remain false");
            return;
        }
        helper.succeed();
    }

    /**
     * REQ-012 (reject case, moving): a genuinely non-stationary ship (real forward input actually
     * ticked, not merely a flag) must reject an in-range, otherwise-valid request. Distance is 0
     * (player placed exactly at the Control Anchor) specifically to prove the moving precondition
     * gates independently of distance, matching the DoD's "damaged/moving vehicle -> rejected
     * regardless of distance."
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void editModeRejectedWhenVehicleIsMovingRegardlessOfDistance(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        ship.setInputForward(1.0f);
        for (int i = 0; i < MOVING_TICKS; i++) {
            ship.tick();
        }
        if (ship.getCurrentSpeed() <= EditModeDistanceGate.STATIONARY_SPEED_EPSILON) {
            helper.fail("test precondition: expected measurably nonzero currentSpeed after "
                    + MOVING_TICKS + " ticks of forward input (ship must be genuinely moving), got "
                    + ship.getCurrentSpeed());
            return;
        }

        placePilotAtOffset(ship, pilot, 0, 0, 0);
        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(pilot);

        if (reason != EditModeDistanceGate.Reason.REJECTED_MOVING) {
            helper.fail("expected REJECTED_MOVING for a genuinely non-stationary ship even at "
                    + "distance 0, got " + reason);
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected NO state change on rejection -- isEditModeActive() must remain false");
            return;
        }
        helper.succeed();
    }

    /**
     * REQ-012 (reject case, destroyed): a ship reduced to 0 health via the real {@link
     * ShipEntity#hurt} damage path (three real hits with real cooldown ticks elapsed between them,
     * not a hand-set field) must reject an in-range, stationary, otherwise-valid request. Distance
     * is 0 to isolate the destroyed precondition, matching the DoD's "damaged ... vehicle ->
     * rejected regardless of distance."
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void editModeRejectedWhenVehicleIsDestroyedRegardlessOfDistance(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        // MAX_HEALTH=100, each generic() hit deals a fixed EXPLOSION_DAMAGE=40, and hurt() enforces
        // a 10-tick cooldown between hits -- three real hits with cooldown actually elapsed between
        // them (100 -> 60 -> 20 -> 0) is what genuinely destroys the ship, not a shortcut field set.
        for (int hit = 0; hit < 3 && !ship.isDestroyed(); hit++) {
            boolean applied = ship.hurt(helper.getLevel().damageSources().generic(), 0.0f);
            if (!applied) {
                helper.fail("test precondition: expected ship.hurt() to apply damage on hit " + hit);
                return;
            }
            for (int i = 0; i < 10; i++) {
                ship.tick();
            }
        }
        if (!ship.isDestroyed()) {
            helper.fail("test precondition: expected the ship to be destroyed (health<=0) after three "
                    + "real hits with cooldown elapsed, got health=" + ship.getHealth());
            return;
        }

        placePilotAtOffset(ship, pilot, 0, 0, 0);
        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(pilot);

        if (reason != EditModeDistanceGate.Reason.REJECTED_DESTROYED) {
            helper.fail("expected REJECTED_DESTROYED for a destroyed ship even at distance 0, got " + reason);
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected NO state change on rejection -- isEditModeActive() must remain false");
            return;
        }
        helper.succeed();
    }

    /**
     * REQ-012 (reject case, conflict): a SECOND concurrent open attempt on an already-open Edit
     * Mode must reject rather than silently succeeding again -- otherwise-valid (in-range,
     * stationary, safe, pilot) but not conflict-free. Proves the gate's conflict axis is checked
     * independently, not merely inferred from the accept case never being called twice.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void editModeRejectedWhenAlreadyActiveRegardlessOfDistance(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        placePilotAtOffset(ship, pilot, 0, 0, 0);
        EditModeDistanceGate.Reason firstAttempt = ship.tryEnterEditMode(pilot);
        if (firstAttempt != EditModeDistanceGate.Reason.ACCEPTED || !ship.isEditModeActive()) {
            helper.fail("test precondition: expected the first open attempt to succeed, got "
                    + firstAttempt + " isEditModeActive=" + ship.isEditModeActive());
            return;
        }

        EditModeDistanceGate.Reason secondAttempt = ship.tryEnterEditMode(pilot);
        if (secondAttempt != EditModeDistanceGate.Reason.REJECTED_CONFLICT) {
            helper.fail("expected REJECTED_CONFLICT for a second concurrent open attempt on an "
                    + "already-active Edit Mode, got " + secondAttempt);
            return;
        }
        if (!ship.isEditModeActive()) {
            helper.fail("expected isEditModeActive() to remain true (the FIRST successful open is "
                    + "not undone by a rejected second attempt)");
            return;
        }
        helper.succeed();
    }

    /**
     * REQ-012 authorization axis (REQ-008 cross-reference: "Edit" is explicitly one of the
     * pilot-exclusive command classes): a non-pilot bystander must be rejected even at distance 0
     * on an otherwise fully valid (stationary/safe/conflict-free) vehicle.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void editModeRejectedForNonPilotRegardlessOfDistance(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();

        placePilotAtOffset(ship, bystander, 0, 0, 0);
        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(bystander);

        if (reason != EditModeDistanceGate.Reason.REJECTED_NOT_PILOT) {
            helper.fail("expected REJECTED_NOT_PILOT for a non-pilot requester even at distance 0, got " + reason);
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected NO state change on rejection -- isEditModeActive() must remain false");
            return;
        }
        helper.succeed();
    }
}
