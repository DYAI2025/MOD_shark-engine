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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * REQ-014/AC-014 (T14) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-014 — Atomic
 * edit/reassembly"), UPDATED for the T14 remediation pass (Watcher review-required: "nothing to
 * actually edit" — see {@link ShipAssemblyService#materializeForEdit}'s own javadoc for the full
 * gap this closes).
 *
 * <p><b>What changed and why (read before touching this class again):</b> before this
 * remediation, {@link ShipAssemblyService#openEditMode} never placed any real world blocks, so
 * this suite's ONLY way to exercise {@link ShipAssemblyService#commitEdit}'s "valid" path was
 * test-only {@code level.setBlock} calls placing an entire replica structure — something no real
 * player action produced (a disclosed, honestly-reported gap; see this task's own remediation
 * brief). {@link ShipAssemblyService#openEditMode} now ALSO calls {@link
 * ShipAssemblyService#materializeForEdit} on every accepted open, so {@link #openEditModeOrFail}
 * below drives Edit Mode open through the REAL production entry point (not {@code
 * ShipEntity#tryEnterEditMode} directly, which would skip materialization) and asserts the
 * Steering Wheel genuinely reappeared in the world before any test proceeds.</p>
 *
 * <p><b>The old "zero world mutation on rejection" thesis no longer holds, and that is
 * INTENTIONAL, not a regression (see {@link ShipAssemblyService#commitEdit}'s own updated
 * javadoc, "What 'atomic' means here..." for the full reasoning):</b> materialization itself is a
 * world mutation the moment Edit Mode opens, so "the world is byte-identical to before" is no
 * longer an achievable (or meaningful) claim on rejection — the achievable, meaningful claim is
 * that {@code ship.getBlueprint()} (the ship's real source of truth) is left completely
 * unchanged, AND the world is deterministically cleared back to the same "no materialized blocks"
 * baseline every non-editing ship already has (never left in a dangling state that matches
 * neither the old nor the new structure — RISK-004's "Blöcke duplizieren oder verlieren"). {@link
 * #invalidEditRollsBackAndClearsMaterializedWorld} proves exactly that, replacing the old
 * zero-mutation snapshot-diff assertion with a "world is air again, blueprint is the pre-edit
 * value" one.</p>
 *
 * <p><b>Companion positive-path test</b> ({@link #validEditAddsBlockAndCommitsFully}): simulates a
 * real edit — the player ADDS exactly one new real block near the already-materialized structure
 * (GameTest cannot simulate actual client placement input, but this is the closest honest
 * approximation: everything else needed for the edit is already physically present from
 * materialization, not re-placed by the test) — then commits: the scanned world region is cleared
 * (mirroring {@link ShipAssemblyService#tryAssemble}'s own clear-scanned-blocks behavior) and the
 * ship's own live block count changes to the new structure's count.</p>
 *
 * <p><b>REQ-014/T14's other job — {@code editModeActive}'s close/reset path:</b> both tests also
 * assert {@link ShipEntity#isEditModeActive()} is {@code false} after {@link
 * ShipAssemblyService#commitEdit} returns, for BOTH outcomes — see that method's javadoc and
 * {@link ShipEntity#exitEditMode()}'s javadoc for why a rejected commit still ends the session
 * (avoids resurrecting T13's originally-disclosed "permanently stuck" edit-mode bug for a player
 * who abandons a broken edit).</p>
 *
 * <p><b>{@link #materializationRotatesToShipsCurrentOrientation}:</b> a ship can turn freely in
 * flight and land facing any direction before Edit Mode opens ({@link EditModeDistanceGate} only
 * requires "stationary," never "facing its original assembly direction") — this test proves
 * {@link ShipAssemblyService#materializeForEdit} places the structure at the ship's CURRENT
 * (rotated) orientation, not its original assembly-time footprint, and rotates the BUG's own
 * {@code FACING} to match (see that method's javadoc for why the BUG specifically, and not every
 * directional block, is corrected).</p>
 *
 * <p><b>Scope note (honestly disclosed, not silently absorbed):</b> this suite exercises {@link
 * ShipAssemblyService#commitEdit} as the real, production commit method directly (the same
 * method {@code ModNetworking}'s {@code BuilderAssembleC2SPayload} handler now calls via {@link
 * ShipAssemblyService#findEditModeShip} — see that class for the wiring) rather than driving a
 * full client-side BuilderScreen block-editing UI, which does not exist yet (BuilderScreen today
 * is a read-only status/preview screen with no block-placement grid — see its own source).
 * Ordinary vanilla block placement/breaking near the now-materialized ship IS the real editing
 * mechanism (REQ-013's own text: "...eine Erweiterung erlauben") — this suite proves the
 * SERVER-side materialize/validate/commit-or-rollback contract those real player actions feed
 * into, simulating the placement/breaking itself via direct world mutation since GameTest has no
 * way to drive actual client input.</p>
 */
public final class AtomicEditReassemblyGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    private static void placeValidStructure(GameTestHelper helper, BlockPos wheelPos) {
        helper.setBlock(wheelPos, ModBlocks.STEERING_WHEEL);
        helper.setBlock(wheelPos.north(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.south(), ModBlocks.PILOT_SEAT);
        helper.setBlock(wheelPos.east(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.west(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.above(), ModBlocks.THRUSTER);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(wheelPos.north().north(), bug);
    }

    private record AssembledShip(ShipEntity ship, ServerPlayer pilot) {}

    /**
     * Places a 7-block valid structure, assembles for real via the same production entry point
     * every other REQ-012/013/014-adjacent GameTest in this package uses, and returns the spawned
     * {@link ShipEntity} (pilot already mounted by {@code tryAssemble} itself) paired with that
     * pilot — or {@code null} if a precondition failed (a {@code helper.fail} was already issued;
     * the caller must return immediately).
     */
    private static AssembledShip assembleShip(GameTestHelper helper) {
        placeValidStructure(helper, WHEEL_POS);
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

    /**
     * REQ-014/T14 remediation round 6 (BLOCKER falsifying-test contract, RISK-004 block-duplication
     * exploit): places the SAME 7-block {@link #placeValidStructure} core PLUS two extra
     * ship-eligible blocks forming a non-load-bearing "branch" hanging off the east hull plank —
     * {@code wheelPos.east().east()} (the connector) and {@code wheelPos.east().east().east()} (the
     * branch's own leaf block) — then assembles the full 9-block structure for real via {@link
     * ShipAssemblyService#tryAssemble}. The branch is deliberately NOT load-bearing (removing it
     * leaves the original 7-block core exactly as valid as {@link #assembleShip}'s own baseline) —
     * that is the whole point: a player can later break just the ONE connector and leave the branch
     * standing but BFS-disconnected, while the remainder still independently satisfies {@code
     * canAssemble()}.
     */
    private static AssembledShip assembleShipWithDetachableBranch(GameTestHelper helper) {
        placeValidStructure(helper, WHEEL_POS);
        helper.setBlock(WHEEL_POS.east().east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.east().east().east(), Blocks.OAK_PLANKS);
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail("test precondition: expected assembly (7-block core + 2-block branch) to "
                    + "succeed, got " + result.translationKey());
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

    /**
     * Opens Edit Mode on {@code ship} for real via {@link ShipAssemblyService#openEditMode} — the
     * REAL production entry point both of T13's player-reachable gestures ({@code
     * ShipEntity#interact}'s mounted and dismounted-but-anchored-and-nearby branches) route
     * through, NOT {@code ShipEntity#tryEnterEditMode} directly (which would skip {@link
     * ShipAssemblyService#materializeForEdit} entirely and silently defeat this remediation's own
     * point). The pilot is placed exactly at the Control Anchor (distance 0, well within OQ-001's
     * 5-block range) so only this test's OWN structure/commit assertions are under test, not the
     * distance gate itself (already covered by {@code EditModeDistanceGameTest}).
     *
     * <p>Also asserts the Steering Wheel (local offset (0,0,0), rotation-invariant — every test in
     * this class, rotated or not, can rely on this one check) genuinely reappeared in the real
     * world at {@code ship.blockPosition()} — the direct, falsifiable proof that materialization
     * actually ran, not merely that {@code isEditModeActive()} flipped true. Individual tests
     * assert the REST of the (possibly-rotated) footprint themselves where relevant.</p>
     *
     * @return {@code false} if the open attempt did not ACCEPT, or materialization did not
     *         demonstrably run (a {@code helper.fail} was already issued in either case)
     */
    private static boolean openEditModeOrFail(GameTestHelper helper, ShipEntity ship, ServerPlayer pilot) {
        pilot.setPos(ship.getX(), ship.getY(), ship.getZ());
        EditModeDistanceGate.Reason reason = ShipAssemblyService.openEditMode(ship, pilot);
        if (reason != EditModeDistanceGate.Reason.ACCEPTED || !ship.isEditModeActive()) {
            helper.fail("test precondition: expected Edit Mode to open, got " + reason
                    + " isEditModeActive=" + ship.isEditModeActive());
            return false;
        }
        BlockState wheelState = helper.getLevel().getBlockState(ship.blockPosition());
        if (!wheelState.is(ModBlocks.STEERING_WHEEL)) {
            helper.fail("expected ShipAssemblyService.openEditMode to materialize the Steering Wheel "
                    + "into the real world at " + ship.blockPosition() + " (ShipAssemblyService#"
                    + "materializeForEdit) -- got " + wheelState);
            return false;
        }
        return true;
    }

    /**
     * Simulates the player BREAKING enough of the (already-materialized) structure to make it
     * genuinely invalid: removes the pilot seat, both hull planks flanking the wheel on the
     * east/west, the thruster, and the BUG — leaving only the Steering Wheel and its north
     * neighbor connected. No propulsion, no pilot seat, no BUG, and only 1 of the required 4 core
     * neighbors: multiple simultaneous {@link dev.sharkengine.ship.part.AssemblyIssue}s, not a
     * degenerate empty structure.
     *
     * <p>Done by REMOVING blocks (not overwriting the wheel position with a different-but-still-
     * ship-eligible block, which is how this suite used to "corrupt" the structure before
     * materialization existed): now that {@link ShipAssemblyService#materializeForEdit} has placed
     * the REST of the original 7-block structure into the world too, overwriting only the wheel
     * itself would leave the other 5 blocks (pilot seat, thruster, BUG, both hull planks) still
     * standing and still connected — {@code scanStructure} would just re-absorb them into an
     * otherwise-still-fully-valid structure, and the "corruption" would silently corrupt nothing.
     * Removing them for real is also a more faithful simulation of "a player breaks blocks."</p>
     */
    private static void breakStructureIntoInvalidRemnant(ServerLevel level, BlockPos shipPos) {
        level.setBlock(shipPos.south(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(shipPos.east(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(shipPos.west(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(shipPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(shipPos.north().north(), Blocks.AIR.defaultBlockState(), 3);
    }

    /**
     * REQ-014/AC-014 core falsifying contract, updated for materialization (see class javadoc):
     * a deliberately-invalid final edit (only the Steering Wheel and its north neighbor remain
     * connected — no thruster, no pilot seat, no BUG, too few core neighbors) must roll back to a
     * clean world (every position the original materialized structure occupied cleared to air) and
     * leave the ship's own live blueprint/block count completely untouched. {@link
     * ShipEntity#isEditModeActive()} must be {@code false} afterward (T14's close/reset path,
     * exercised on the REJECTION outcome).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void invalidEditRollsBackAndClearsMaterializedWorld(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();
        int originalBlockCount = ship.getBlockCount();
        if (originalBlockCount != 7) {
            helper.fail("test precondition: expected the freshly assembled ship to have 7 blocks, got "
                    + originalBlockCount);
            return;
        }

        if (!openEditModeOrFail(helper, ship, pilot)) {
            return;
        }

        BlockPos shipPos = ship.blockPosition();
        ServerLevel level = helper.getLevel();

        BlockPos[] originalFootprint = {
                shipPos, shipPos.north(), shipPos.south(), shipPos.east(), shipPos.west(),
                shipPos.above(), shipPos.north().north()
        };

        breakStructureIntoInvalidRemnant(level, shipPos);

        ShipAssemblyService.EditCommitResult result = ShipAssemblyService.commitEdit(level, ship, pilot);

        if (result.isSuccess()) {
            helper.fail("expected the deliberately-invalid edit to be REJECTED, got success (arg="
                    + result.arg() + ")");
            return;
        }
        if (!"message.sharkengine.edit_commit_invalid".equals(result.translationKey())) {
            helper.fail("expected rejection reason 'edit_commit_invalid', got " + result.translationKey());
            return;
        }

        for (BlockPos pos : originalFootprint) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                helper.fail("expected the rejected commit to clear " + pos + " back to air (the "
                        + "materialized-then-abandoned edit must not leave dangling world state -- "
                        + "RISK-004), got " + state);
                return;
            }
        }

        if (ship.getBlockCount() != originalBlockCount) {
            helper.fail("expected the ship's own block count to remain unchanged (" + originalBlockCount
                    + ") after a rejected commit, got " + ship.getBlockCount());
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected isEditModeActive()=false after a rejected commit (T14's close/reset "
                    + "path must run on rejection too, not only on success)");
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-014/AC-014 companion positive-path contract, updated for materialization (see class
     * javadoc): the player adds exactly ONE new real block near the already-materialized 7-block
     * structure (everything else needed for the edit is already physically present from {@link
     * ShipAssemblyService#materializeForEdit} — not re-placed by this test), extending it to 8
     * blocks, still fully valid. Commits fully: the scanned world region is cleared (mirroring
     * {@link ShipAssemblyService#tryAssemble}'s own post-assembly world state) and the ship's own
     * live block count changes from 7 to 8. {@link ShipEntity#isEditModeActive()} must be
     * {@code false} afterward (T14's close/reset path, exercised on the SUCCESS outcome this time).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void validEditAddsBlockAndCommitsFully(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();
        int originalBlockCount = ship.getBlockCount();
        if (originalBlockCount != 7) {
            helper.fail("test precondition: expected the freshly assembled ship to have 7 blocks, got "
                    + originalBlockCount);
            return;
        }

        if (!openEditModeOrFail(helper, ship, pilot)) {
            return;
        }

        BlockPos shipPos = ship.blockPosition();
        ServerLevel level = helper.getLevel();

        // The player's ACTUAL edit: everything else (wheel, hull, pilot seat, thruster, BUG) is
        // already physically present from materializeForEdit (openEditModeOrFail already asserted
        // the wheel specifically) -- GameTest cannot simulate real client placement input, but
        // adding exactly this ONE new block via direct world mutation, without re-placing anything
        // materialization already provided, is the closest honest simulation of it.
        level.setBlock(shipPos.east().east(), Blocks.OAK_PLANKS.defaultBlockState(), 3);

        ShipAssemblyService.EditCommitResult result = ShipAssemblyService.commitEdit(level, ship, pilot);

        if (!result.isSuccess()) {
            helper.fail("expected the valid edit to be COMMITTED, got rejection " + result.translationKey()
                    + " (arg=" + result.arg() + ")");
            return;
        }
        if (!Integer.valueOf(8).equals(result.arg())) {
            helper.fail("expected commit result arg to be the new block count 8, got " + result.arg());
            return;
        }

        // World blocks in the committed region are cleared, mirroring tryAssemble's own
        // post-assembly world state.
        BlockPos[] committedPositions = {
                shipPos, shipPos.north(), shipPos.south(), shipPos.east(), shipPos.west(),
                shipPos.above(), shipPos.north().north(), shipPos.east().east()
        };
        for (BlockPos pos : committedPositions) {
            if (!level.getBlockState(pos).isAir()) {
                helper.fail("expected " + pos + " to be cleared to air after a successful edit commit, got "
                        + level.getBlockState(pos));
                return;
            }
        }

        if (ship.getBlockCount() != 8) {
            helper.fail("expected the ship's own block count to change from 7 to 8 after a successful "
                    + "commit, got " + ship.getBlockCount());
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected isEditModeActive()=false after a successful commit (T14's close/reset "
                    + "path must run on the success outcome)");
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-014/T14 close/reset path, second half of the contract that {@link
     * #invalidEditRollsBackAndClearsMaterializedWorld}/{@link #validEditAddsBlockAndCommitsFully}
     * leave unproven: those two only assert {@link ShipEntity#isEditModeActive()} reads back
     * {@code false} after {@link ShipAssemblyService#commitEdit} -- a flag read-back that a
     * stale/shadowed field or a getter bug could satisfy without the ship's Edit Mode actually
     * being reusable. This test proves genuine reusability the only way that is falsifiable: drive
     * a full open -&gt; reject-commit -&gt; open-AGAIN cycle through the real production entry
     * points ({@link ShipAssemblyService#openEditMode}, {@link ShipAssemblyService#commitEdit})
     * and assert the SECOND open call returns {@link EditModeDistanceGate.Reason#ACCEPTED} (not
     * {@link EditModeDistanceGate.Reason#REJECTED_CONFLICT}, which is exactly what {@link
     * EditModeDistanceGameTest#editModeRejectedWhenAlreadyActiveRegardlessOfDistance} proves for
     * the DIFFERENT case of a second attempt while still genuinely open, with no commit in
     * between). The commit here deliberately uses the REJECTED path -- the historically fragile
     * case this class's own javadoc discusses (T13's originally-disclosed "permanently stuck" bug
     * was a rejected/abandoned edit leaving {@code editModeActive} stuck {@code true} forever) --
     * rather than the accepted path, so this is not redundant with a hypothetical "reopen after a
     * successful edit" variant.
     *
     * <p>The second open call goes through {@link ShipAssemblyService#openEditMode} too (not raw
     * {@code tryEnterEditMode}), so it ALSO re-materializes the (still pre-edit, unchanged)
     * blueprint fresh -- proving the whole cycle, not just the gate flag, is genuinely reusable.</p>
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void editModeReopensAfterRejectedCommitExitCycle(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        if (!openEditModeOrFail(helper, ship, pilot)) {
            return;
        }

        BlockPos shipPos = ship.blockPosition();
        ServerLevel level = helper.getLevel();

        breakStructureIntoInvalidRemnant(level, shipPos);

        ShipAssemblyService.EditCommitResult firstCommit =
                ShipAssemblyService.commitEdit(level, ship, pilot);
        if (firstCommit.isSuccess()) {
            helper.fail("test precondition: expected the deliberately-invalid edit to be REJECTED, "
                    + "got success (arg=" + firstCommit.arg() + ")");
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("test precondition: expected isEditModeActive()=false immediately after the "
                    + "rejected commit -- cannot test reopening if the close/reset path didn't run");
            return;
        }

        // The SECOND open attempt: pilot has not moved (still at distance 0, the same position
        // openEditModeOrFail placed them at), ship is still stationary/undestroyed/same-dimension --
        // every precondition BUT the conflict-freedom one is identical to the first, successful,
        // open attempt. If editModeActive is genuinely false (not merely read back as false through
        // a bug), this must ACCEPT exactly like the first attempt did -- and, being routed through
        // the real ShipAssemblyService#openEditMode again, re-materializes the ship's (unchanged)
        // blueprint into the (now-cleared) world once more.
        if (!openEditModeOrFail(helper, ship, pilot)) {
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-014/T14 remediation, rotation correctness (see {@link
     * ShipAssemblyService#materializeForEdit}'s own javadoc): a ship can turn freely in flight and
     * land facing any direction before Edit Mode opens -- {@link EditModeDistanceGate} only
     * requires "stationary," never "facing its original assembly direction." This test simulates
     * that (a real forward-flight turn is not needed to prove the MATH; directly setting yaw is
     * the same state a real turn would leave the ship in) by setting the ship's yaw to 90 degrees
     * after assembly (assembled with the BUG facing SOUTH, i.e. {@code assemblyYaw=0}, so this is
     * a genuine 90-degree effective rotation, not a no-op).
     *
     * <p>Under {@code ShipTransform}'s ground-truthed rotation convention ({@code (dx,dz) ->
     * (-dz,dx)} at +90 degrees, identical to vanilla {@code Rotation.CLOCKWISE_90}): the pilot
     * seat (originally south-of-wheel) must materialize west-of-wheel, and the BUG (originally
     * north-north-of-wheel, facing SOUTH) must materialize east-east-of-wheel with its FACING
     * rotated to WEST -- proving both the POSITION rotation ({@link
     * ShipAssemblyService#rotatedWorldPositions}) and the BUG-FACING correction ({@code
     * Rotation.rotate(Direction)}) actually run, not merely that materialization places SOMETHING
     * at the ship's position.</p>
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void materializationRotatesToShipsCurrentOrientation(GameTestHelper helper) {
        AssembledShip assembled = assembleShip(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        ship.setYawDeg(90.0f);

        if (!openEditModeOrFail(helper, ship, pilot)) {
            return;
        }

        BlockPos shipPos = ship.blockPosition();
        ServerLevel level = helper.getLevel();

        BlockState pilotSeatState = level.getBlockState(shipPos.west());
        if (!pilotSeatState.is(ModBlocks.PILOT_SEAT)) {
            helper.fail("expected the pilot seat (originally south-of-wheel) rotated 90deg to "
                    + "materialize west-of-wheel, got " + pilotSeatState + " at " + shipPos.west());
            return;
        }

        BlockPos bugPos = shipPos.east().east();
        BlockState bugState = level.getBlockState(bugPos);
        if (!bugState.is(ModBlocks.BUG)) {
            helper.fail("expected the BUG block (originally north-north-of-wheel) rotated 90deg to "
                    + "materialize east-east-of-wheel, got " + bugState + " at " + bugPos);
            return;
        }
        Direction facing = bugState.getValue(BugBlock.FACING);
        if (facing != Direction.WEST) {
            helper.fail("expected the materialized BUG's FACING rotated from SOUTH to WEST (matching "
                    + "the ship's current 90deg orientation, avoiding a double-rotation on the next "
                    + "commit's re-scan), got " + facing);
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-014/T14 remediation round 6 (BLOCKER falsifying-test contract, RISK-004 block-duplication
     * exploit — the test that would have caught the bug, per this task's own instructions): {@link
     * #assembleShipWithDetachableBranch} materializes a 9-block structure (the ordinary 7-block
     * {@link #placeValidStructure} core, plus a 2-block branch hanging off the east hull plank —
     * connector at {@code shipPos.east().east()}, leaf at {@code shipPos.east().east().east()}).
     * The player then breaks ONLY the connector, leaving the branch's leaf block standing but
     * BFS-disconnected from the wheel — {@link ShipAssemblyService#scanStructure} never reaches an
     * air gap's far side, so the leaf is silently absent from the re-scanned {@code
     * newBlueprint.blocks()} — while the wheel-connected 7-block remainder still independently
     * satisfies {@code canAssemble()} (it is byte-for-byte {@link #assembleShip}'s own valid
     * baseline). The commit must therefore SUCCEED, not reject.
     *
     * <p><b>What this proves that {@link #validEditAddsBlockAndCommitsFully} does not:</b> that test
     * only ever grows the committed footprint (7 to 8 blocks, {@code newBlueprint.blocks()} is a
     * superset of what was materialized) — clearing {@code newBlueprint.blocks()} alone happens to
     * be correct there by coincidence, because it already covers everything materialized. This test
     * is the case where a successful commit SHRINKS relative to what was materialized (9 down to 7)
     * — the one case where the old buggy success branch (clearing only {@code
     * newBlueprint.blocks()}, never the pre-edit footprint) provably left real blocks behind. Both
     * assertions below are the falsifiable core of the fix: (1) the branch leaf's ORIGINAL world
     * position must be air, not still holding a real block a player could pick up as duplicated,
     * free material; (2) {@link ShipEntity#getBlueprint()} (via {@link ShipEntity#getBlockCount()},
     * the ship's real mass/parts source per {@code ShipPartAnalyzer}) must correctly exclude the
     * detached branch's 2 blocks, not silently retain their mass.</p>
     *
     * <p><b>TDD verification (per this task's own instructions, actually performed, not just
     * claimed):</b> with {@link ShipAssemblyService#commitEdit}'s success branch temporarily
     * reverted to its old buggy form (clearing only {@code newBlueprint.blocks()}'s world
     * positions), this test fails at the branch-leaf-is-air assertion below — {@code
     * level.getBlockState(branchPos)} reads back {@code minecraft:oak_planks}, not air, because the
     * old clearing loop never iterated over a position outside {@code newBlueprint.blocks()}. With
     * the fix restored (clearing the union of {@code rotatedWorldPositions(preEditBlueprint, ...)}
     * and {@code newBlueprint.blocks()}, mirroring the rejection branch), this test passes.</p>
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void detachedBranchClearedOnSuccessfulCommit(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithDetachableBranch(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();
        int originalBlockCount = ship.getBlockCount();
        if (originalBlockCount != 9) {
            helper.fail("test precondition: expected the freshly assembled ship (7-block core + "
                    + "2-block branch) to have 9 blocks, got " + originalBlockCount);
            return;
        }

        if (!openEditModeOrFail(helper, ship, pilot)) {
            return;
        }

        BlockPos shipPos = ship.blockPosition();
        ServerLevel level = helper.getLevel();

        // Sanity: materializeForEdit placed the connector AND the branch leaf for real too (not
        // just the 7-block core) -- confirms this exercises the same real-world footprint a real
        // player edit would see, not a test-only shortcut.
        BlockPos connectorPos = shipPos.east().east();
        BlockPos branchPos = shipPos.east().east().east();
        if (!level.getBlockState(connectorPos).is(Blocks.OAK_PLANKS)
                || !level.getBlockState(branchPos).is(Blocks.OAK_PLANKS)) {
            helper.fail("test precondition: expected materializeForEdit to have placed the connector "
                    + "at " + connectorPos + " (" + level.getBlockState(connectorPos) + ") and the "
                    + "branch leaf at " + branchPos + " (" + level.getBlockState(branchPos) + ")");
            return;
        }

        // THE EXPLOIT MOVE: break ONLY the connector. The branch leaf is left standing, but is now
        // BFS-unreachable from the wheel -- exactly the "non-load-bearing branch detached by
        // breaking its one connector" scenario this task's BLOCKER report describes.
        level.setBlock(connectorPos, Blocks.AIR.defaultBlockState(), 3);

        ShipAssemblyService.EditCommitResult result = ShipAssemblyService.commitEdit(level, ship, pilot);

        if (!result.isSuccess()) {
            helper.fail("expected the commit to SUCCEED (the wheel-connected 7-block remainder still "
                    + "independently satisfies canAssemble(), identical to assembleShip()'s own valid "
                    + "baseline) -- got rejection " + result.translationKey() + " (arg=" + result.arg()
                    + ")");
            return;
        }
        if (!Integer.valueOf(7).equals(result.arg())) {
            helper.fail("expected the committed block count to be 7 (the detached branch's 2 blocks "
                    + "correctly excluded from the new blueprint), got " + result.arg());
            return;
        }

        // THE BLOCKER ASSERTION: the detached branch leaf's ORIGINAL world position must now be
        // air. The OLD buggy success branch (clearing only newBlueprint.blocks(), never the
        // pre-edit footprint) never reached this position at all -- it would still read back
        // OAK_PLANKS here, a real, ownerless, duplicated block the player could simply pick up,
        // while the ship's own blueprint has already permanently dropped its mass.
        BlockState branchStateAfterCommit = level.getBlockState(branchPos);
        if (!branchStateAfterCommit.isAir()) {
            helper.fail("BLOCKER (RISK-004 block duplication): expected the detached branch leaf at "
                    + branchPos + " to be cleared to air by the successful commit (it was excluded "
                    + "from the new blueprint, so it must not survive in the world as a real, "
                    + "ownerless block) -- got " + branchStateAfterCommit);
            return;
        }
        // The connector position (already broken by the player, pre-commit) stays air too --
        // included here for completeness, not load-bearing for the BLOCKER claim above.
        if (!level.getBlockState(connectorPos).isAir()) {
            helper.fail("expected the connector position " + connectorPos + " to remain air after "
                    + "commit, got " + level.getBlockState(connectorPos));
            return;
        }

        // The committed 7-block core itself is cleared too, mirroring every other successful-commit
        // assertion in this class (validEditAddsBlockAndCommitsFully).
        BlockPos[] committedPositions = {
                shipPos, shipPos.north(), shipPos.south(), shipPos.east(), shipPos.west(),
                shipPos.above(), shipPos.north().north()
        };
        for (BlockPos pos : committedPositions) {
            if (!level.getBlockState(pos).isAir()) {
                helper.fail("expected " + pos + " to be cleared to air after a successful edit commit, "
                        + "got " + level.getBlockState(pos));
                return;
            }
        }

        // The ship's own live blueprint (ShipEntity#getBlockCount(), the real mass/parts source
        // per ShipPartAnalyzer -- AIR-023) must correctly exclude the detached branch's 2 blocks,
        // not silently retain their mass.
        if (ship.getBlockCount() != 7) {
            helper.fail("expected the ship's own live block count (mass source) to correctly exclude "
                    + "the detached branch's 2 blocks (9 - 2 = 7), got " + ship.getBlockCount());
            return;
        }
        if (ship.isEditModeActive()) {
            helper.fail("expected isEditModeActive()=false after a successful commit (T14's "
                    + "close/reset path must run on the success outcome)");
            return;
        }

        helper.succeed();
    }
}
