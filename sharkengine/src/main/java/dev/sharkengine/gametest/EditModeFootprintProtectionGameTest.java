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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * REQ-014/T14 remediation round 3 (security-review BLOCKER) falsifying-test contract for {@link
 * dev.sharkengine.ship.EditModeBlockProtection}. See that class's own javadoc for the full RISK-004
 * third-party gap this closes: once {@link ShipAssemblyService#materializeForEdit} places a ship's
 * blueprint into the world as real blocks, that footprint was, until this remediation, ordinary
 * UNPROTECTED world state for the entire Edit Mode window -- ANY nearby player, not just the
 * editing pilot, could break or place blocks in it.
 *
 * <p><b>Real production entry points, not a mock of the Fabric event system:</b> {@code
 * GameTestHelper}'s own {@code destroyBlock(BlockPos)}/{@code useBlock(...)}/{@code placeAt(...)}
 * convenience methods were investigated and found to BYPASS the exact mixin-driven event path this
 * remediation depends on -- {@code GameTestHelper#destroyBlock} calls {@code Level#destroyBlock}
 * directly (no player context, never reaches {@link
 * net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents#BEFORE} at all), and {@code
 * GameTestHelper#useBlock}/{@code #placeAt} call {@code BlockState#useItemOn}/{@code
 * ItemStack#useOn} directly, skipping {@code ServerPlayerGameMode} (and therefore {@link
 * net.fabricmc.fabric.api.event.player.UseBlockCallback#EVENT}) entirely. This suite instead calls
 * {@code ServerPlayer#gameMode}'s own {@code destroyBlock(BlockPos)}/{@code useItemOn(...)}
 * directly -- verified by decompiling {@code ServerPlayerGameMode} against Fabric API's own {@code
 * ServerPlayerInteractionManagerMixin} to confirm these are EXACTLY the two methods that mixin
 * injects into (immediately before {@code Block.playerWillDestroy}/{@code Block.destroy} for
 * breaking; at the HEAD, cancellable, for {@code useItemOn}) -- so every assertion below exercises
 * the REAL production wiring (mixin -&gt; Fabric event -&gt; {@code EditModeBlockProtection}'s
 * listener), not a shortcut around it.</p>
 *
 * <p><b>REQ-014/T14 remediation round 4 (security-review BLOCKER) addition:</b> {@link
 * #nonPilotUsingUnrelatedBlockNearFootprintIsNotBlocked} proves the round-3 fix's own overreach is
 * closed -- {@code onUseBlock} used to guard the CLICKED position with the same adjacent-shell
 * check meant for PLACEMENT targets, so any bystander right-click on their own, entirely unrelated,
 * pre-existing block (a chest, in this test) within one tile of an active edit session's footprint
 * got silently cancelled. {@link
 * #nonPilotCannotBridgeAdjacentToFootprintViaOwnScaffoldButPilotCan} (unchanged from round 3) is
 * re-run in this same round to prove the round-4 fix did not reopen that closed BFS-bridging gap
 * while narrowing the guard's scope.</p>
 *
 * <p><b>REQ-014/T14 remediation round 5 (2nd occurrence of the same bug class) addition:</b> round
 * 4's own {@code #nonPilotUsingUnrelatedBlockNearFootprintIsNotBlocked} above only used an EMPTY
 * held item, which already short-circuited {@code onUseBlock} before the round-4 bug's actual
 * mechanism (a computed hypothetical PLACEMENT position) was ever reached -- it could not have
 * caught round 5's bug even in principle. {@link
 * #nonPilotOpeningOwnChestWhoseHypotheticalPlacementWouldLandInFootprintShellIsNotBlocked} and
 * {@link #nonPilotTogglingOwnLeverWhoseHypotheticalPlacementWouldLandInFootprintShellIsNotBlocked}
 * close that gap: both hold an actual placeable {@code BlockItem} and click a genuinely
 * non-replaceable, pre-existing interactive block (a chest -- {@link
 * net.minecraft.world.level.block.state.BlockState#getMenuProvider} path; a lever -- {@code
 * instanceof LeverBlock} path) positioned so the OLD code's computed placement position (one step
 * off the clicked face) would land inside an active edit session's footprint shell. Before this
 * round's fix, {@code onUseBlock} cancelled the whole interaction based on that placement position
 * even though vanilla's own dispatch would have let the clicked block consume the click first (open
 * the chest / toggle the lever) and never have attempted placement at all -- see {@link
 * dev.sharkengine.ship.EditModeBlockProtection}'s own round-5 javadoc for the full mechanism. {@link
 * #nonPilotCannotBridgeAdjacentToFootprintViaOwnScaffoldButPilotCan} (unchanged) is re-run in this
 * same round to prove the round-5 fix did not reopen the BFS-bridging gap it narrows around -- a
 * plain, non-interactive scaffold block (oak planks) must still have its OWN placement guarded
 * exactly as before.</p>
 */
public final class EditModeFootprintProtectionGameTest implements FabricGameTest {

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

    /** Same helper shape as {@code AtomicEditReassemblyGameTest#assembleShip} -- see that class. */
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

    /** Same helper shape as {@code AtomicEditReassemblyGameTest#openEditModeOrFail} -- see that class. */
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
            helper.fail("test precondition: expected materializeForEdit to have placed the Steering "
                    + "Wheel at " + ship.blockPosition() + ", got " + wheelState);
            return false;
        }
        return true;
    }

    private static BlockHitResult replaceHit(BlockPos worldPos) {
        return new BlockHitResult(Vec3.atCenterOf(worldPos), Direction.UP, worldPos, false);
    }

    private static BlockHitResult adjacentHit(BlockPos anchorWorldPos, Direction towardTarget) {
        return new BlockHitResult(Vec3.atCenterOf(anchorWorldPos), towardTarget, anchorWorldPos, false);
    }

    /**
     * Core BLOCKER contract, break side: a bystander (not the pilot, not even aboard) attempting to
     * break a real block in the ship's currently-materialized footprint via the REAL production
     * path ({@code ServerPlayerGameMode#destroyBlock}, the exact method Fabric's own mixin injects
     * {@link net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents#BEFORE} into) must be
     * prevented -- the block must remain exactly as materialized. The SAME position, broken by the
     * actual pilot immediately afterward through the identical call, must succeed -- proving this
     * is a per-pilot exemption, not an accidental blanket freeze of the whole footprint.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nonPilotBreakBlockedButPilotBreakSucceeds(GameTestHelper helper) {
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
        BlockPos pilotSeatPos = shipPos.south();
        ServerLevel level = helper.getLevel();

        if (!level.getBlockState(pilotSeatPos).is(ModBlocks.PILOT_SEAT)) {
            helper.fail("test precondition: expected the materialized pilot seat at " + pilotSeatPos
                    + ", got " + level.getBlockState(pilotSeatPos));
            return;
        }

        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        bystander.setPos(pilotSeatPos.getX() + 0.5, pilotSeatPos.getY(), pilotSeatPos.getZ() + 0.5);

        bystander.gameMode.destroyBlock(pilotSeatPos);

        if (!level.getBlockState(pilotSeatPos).is(ModBlocks.PILOT_SEAT)) {
            helper.fail("expected a non-pilot's break attempt on the materialized footprint to be "
                    + "prevented (RISK-004 third-party case) -- pilot seat at " + pilotSeatPos
                    + " was removed, got " + level.getBlockState(pilotSeatPos));
            return;
        }

        pilot.gameMode.destroyBlock(pilotSeatPos);

        if (!level.getBlockState(pilotSeatPos).isAir()) {
            helper.fail("expected the ACTUAL pilot's own break within their own ship's footprint to "
                    + "still work unaffected, got " + level.getBlockState(pilotSeatPos)
                    + " at " + pilotSeatPos);
            return;
        }

        helper.succeed();
    }

    /**
     * Core BLOCKER contract, place side: a bystander attempting to place a foreign block directly
     * into a gap within the ship's materialized footprint (simulating a block having been removed
     * from the structure during the edit -- a legitimate mid-edit state, not itself part of this
     * assertion) via the REAL production path ({@code ServerPlayerGameMode#useItemOn}, the exact
     * method Fabric's own mixin injects {@link
     * net.fabricmc.fabric.api.event.player.UseBlockCallback#EVENT} into, cancellable at the HEAD)
     * must be prevented -- the gap must remain air. The actual pilot's own placement into the SAME
     * gap immediately afterward must succeed.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nonPilotPlaceIntoFootprintGapBlockedButPilotPlaceSucceeds(GameTestHelper helper) {
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
        BlockPos gapPos = shipPos.east(); // one of the materialized hull planks
        ServerLevel level = helper.getLevel();

        if (level.getBlockState(gapPos).isAir()) {
            helper.fail("test precondition: expected a materialized (non-air) block at " + gapPos);
            return;
        }
        // Simulate "a block was removed from the structure mid-edit" -- a legitimate prior state
        // this test does not itself assert on (the break side is EditModeFootprintProtectionGameTest's
        // OWN sibling test above); this test isolates the PLACE-side contract only.
        level.setBlock(gapPos, Blocks.AIR.defaultBlockState(), 3);

        // Standing one block AWAY from gapPos itself (not exactly inside its own 1x1x1 cube) --
        // useItemOn's manually-built BlockHitResult below targets gapPos regardless of the actor's
        // own position, but a player's real hitbox standing exactly ON the target block would
        // otherwise obstruct vanilla's own BlockPlaceContext#canPlace entity-collision check,
        // which is unrelated to (and would falsely confound) the protection contract under test.
        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        bystander.setPos(gapPos.getX() + 1.5, gapPos.getY(), gapPos.getZ() + 0.5);
        bystander.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));

        bystander.gameMode.useItemOn(bystander, level, bystander.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, replaceHit(gapPos));

        if (!level.getBlockState(gapPos).isAir()) {
            helper.fail("expected a non-pilot's placement into a gap within the materialized footprint "
                    + "to be prevented (RISK-004 third-party case), got " + level.getBlockState(gapPos)
                    + " at " + gapPos);
            return;
        }
        // Out of the way entirely before the pilot's own attempt -- the bystander's own hitbox
        // must not become a second, unrelated obstruction to what this test asserts next.
        bystander.discard();

        pilot.setPos(gapPos.getX() + 1.5, gapPos.getY(), gapPos.getZ() + 0.5);
        pilot.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
        pilot.gameMode.useItemOn(pilot, level, pilot.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, replaceHit(gapPos));

        if (level.getBlockState(gapPos).isAir()) {
            helper.fail("expected the ACTUAL pilot's own placement within their own ship's footprint "
                    + "to still work unaffected, got air at " + gapPos);
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-014/T14 "important finding" contract (security-review: BFS has no per-ship ownership
     * boundary during commit): proves the specific closure claim in {@link
     * dev.sharkengine.ship.EditModeBlockProtection}'s own javadoc -- placement is guarded against
     * the footprint's immediate neighbor shell, not just exact membership, so a bystander cannot
     * defeat the protection by approaching from an unrelated angle (using their OWN previously-
     * placed, unrelated scaffold block as the placement anchor, never clicking the footprint's own
     * face) and landing a new block immediately touching the footprint -- exactly the move that
     * would otherwise make {@link ShipAssemblyService#scanStructure}'s adjacency-based BFS treat
     * the new block (and anything attached beyond it) as connected to the ship's structure at the
     * next commit. The pilot performing the IDENTICAL scaffold-anchored placement must still
     * succeed.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nonPilotCannotBridgeAdjacentToFootprintViaOwnScaffoldButPilotCan(GameTestHelper helper) {
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
        BlockPos bugPos = shipPos.north().north(); // materialized footprint member (the BUG block)
        ServerLevel level = helper.getLevel();

        if (!level.getBlockState(bugPos).is(ModBlocks.BUG)) {
            helper.fail("test precondition: expected the materialized BUG at " + bugPos
                    + ", got " + level.getBlockState(bugPos));
            return;
        }

        // The bystander's OWN unrelated scaffold block, two steps beyond the BUG -- outside both the
        // footprint AND its neighbor shell, so placing it here is ordinary, unprotected world
        // building (not itself part of what this test asserts).
        BlockPos scaffoldPos = bugPos.north().north();
        level.setBlock(scaffoldPos, Blocks.OAK_PLANKS.defaultBlockState(), 3);

        // The bridging target: one step north of the BUG -- empty space that is NOT itself a
        // footprint member, but IS immediately touching one (the BUG). This is the exact position a
        // real BFS re-scan at commit time would treat as connected to the ship if a foreign block
        // ever lands here.
        BlockPos bridgeGapPos = bugPos.north();
        if (!level.getBlockState(bridgeGapPos).isAir()) {
            helper.fail("test precondition: expected empty space at " + bridgeGapPos);
            return;
        }

        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        bystander.setPos(scaffoldPos.getX() + 0.5, scaffoldPos.getY(), scaffoldPos.getZ() + 0.5);
        bystander.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));

        // Right-clicks the SCAFFOLD's face (never the BUG's own face) aiming SOUTH, toward the
        // bridge gap -- the "approach from an unrelated angle" attack this test exists to prove is
        // still caught.
        bystander.gameMode.useItemOn(bystander, level, bystander.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, adjacentHit(scaffoldPos, Direction.SOUTH));

        if (!level.getBlockState(bridgeGapPos).isAir()) {
            helper.fail("expected a non-pilot's scaffold-anchored placement immediately touching the "
                    + "materialized footprint to be prevented (BFS bridging gap), got "
                    + level.getBlockState(bridgeGapPos) + " at " + bridgeGapPos);
            return;
        }

        pilot.setPos(scaffoldPos.getX() + 0.5, scaffoldPos.getY(), scaffoldPos.getZ() + 0.5);
        pilot.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
        pilot.gameMode.useItemOn(pilot, level, pilot.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, adjacentHit(scaffoldPos, Direction.SOUTH));

        if (level.getBlockState(bridgeGapPos).isAir()) {
            helper.fail("expected the ACTUAL pilot's own scaffold-anchored placement touching their own "
                    + "ship's footprint to still work unaffected, got air at " + bridgeGapPos);
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-014/T14 remediation round 4 (security-review BLOCKER) falsifying-test contract: a
     * bystander's own, entirely unrelated, pre-existing block -- a chest, never a footprint member,
     * never placed by anyone as part of this ship -- sitting one tile away from a footprint member
     * (i.e. within the shell radius {@link dev.sharkengine.ship.EditModeBlockProtection} correctly
     * still uses for PLACEMENT checks) must remain fully usable by a non-pilot while that ship's
     * Edit Mode is active. Before this round's fix, {@code onUseBlock} guarded the CLICKED position
     * with that same shell check, so this exact right-click was wrongly cancelled -- the chest
     * never opened. Proven via the REAL production path ({@code ServerPlayerGameMode#useItemOn}
     * with an EMPTY held item -- an empty hand can never place a block, so this interaction can
     * only ever resolve as a pure "use", isolating the regression this test targets) and a genuine
     * vanilla side effect ({@code Player#hasContainerOpen()} flips true only once {@code ChestBlock}
     * itself has actually called {@code player.openMenu(...)} -- not merely "the event wasn't
     * cancelled").
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nonPilotUsingUnrelatedBlockNearFootprintIsNotBlocked(GameTestHelper helper) {
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
        // shipPos.east() is a materialized footprint member (an OAK_PLANKS hull block, see
        // placeValidStructure); one further step east is therefore inside the OLD buggy shell
        // radius (one tile from a footprint member) but is NOT itself a footprint member -- exactly
        // the "unrelated bystander block merely near the footprint" case this test isolates.
        BlockPos chestPos = shipPos.east().east();
        ServerLevel level = helper.getLevel();

        if (!level.getBlockState(shipPos.east()).is(Blocks.OAK_PLANKS)) {
            helper.fail("test precondition: expected a materialized footprint member at " + shipPos.east()
                    + ", got " + level.getBlockState(shipPos.east()));
            return;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        bystander.setPos(chestPos.getX() + 1.5, chestPos.getY(), chestPos.getZ() + 0.5);
        // Empty hand: cannot place a block under any circumstance, so a cancelled result here can
        // only ever be this class wrongly treating a pure "use" interaction as a placement attempt.
        InteractionResult result = bystander.gameMode.useItemOn(bystander, level, ItemStack.EMPTY,
                InteractionHand.MAIN_HAND, replaceHit(chestPos));

        if (result == InteractionResult.FAIL) {
            helper.fail("expected a non-pilot's use of their OWN unrelated chest merely near (not "
                    + "inside) the materialized footprint to be allowed, got InteractionResult.FAIL");
            return;
        }
        if (!bystander.hasContainerOpen()) {
            helper.fail("expected the chest to have actually opened (Player#hasContainerOpen()) for the "
                    + "bystander -- proximity to an active edit session's footprint must never block an "
                    + "unrelated block's own use interaction");
            return;
        }
        if (!level.getBlockState(chestPos).is(Blocks.CHEST)) {
            helper.fail("expected the bystander's own chest to remain untouched, got "
                    + level.getBlockState(chestPos) + " at " + chestPos);
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-014/T14 remediation round 5 (2nd occurrence of the same bug class) falsifying-test
     * contract, menu-provider path: a bystander HOLDING a placeable block item (unlike the round-4
     * test above, which used an empty hand and therefore never reached the round-5 bug's actual
     * mechanism) right-clicks their OWN, entirely unrelated, pre-existing chest. The chest is
     * positioned so the hypothetical placement position {@code onUseBlock} would have computed from
     * this click (one step off the clicked face, since a chest is not replaceable) lands inside an
     * active edit session's footprint SHELL -- exactly the position the OLD code guarded and
     * wrongly cancelled on, even though vanilla's real dispatch lets the chest consume the click
     * (open) before placement is ever attempted. Proven via the REAL production path ({@code
     * ServerPlayerGameMode#useItemOn}, the exact method Fabric's mixin injects {@link
     * net.fabricmc.fabric.api.event.player.UseBlockCallback#EVENT} into) and a genuine vanilla side
     * effect ({@code Player#hasContainerOpen()} flips true only once {@code ChestBlock} itself has
     * actually opened the menu).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nonPilotOpeningOwnChestWhoseHypotheticalPlacementWouldLandInFootprintShellIsNotBlocked(
            GameTestHelper helper) {
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

        if (!level.getBlockState(shipPos.east()).is(Blocks.OAK_PLANKS)) {
            helper.fail("test precondition: expected a materialized footprint member at " + shipPos.east()
                    + ", got " + level.getBlockState(shipPos.east()));
            return;
        }

        // Three steps east of the wheel: clicking its WEST face computes a hypothetical placement
        // one step further west (shipPos.east().east()) -- NOT itself a footprint member, but
        // immediately touching one (shipPos.east(), the materialized oak plank) -- i.e. inside the
        // guarded adjacent shell the round-3 fix still correctly protects for PLACEMENT. This is the
        // exact position the pre-round-5 code would have guarded and wrongly cancelled on.
        BlockPos chestPos = shipPos.east().east().east();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        BlockPos wouldBePlacedPos = chestPos.west();
        if (!level.getBlockState(wouldBePlacedPos).isAir()) {
            helper.fail("test precondition: expected empty space at the hypothetical placement "
                    + "position " + wouldBePlacedPos);
            return;
        }

        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        bystander.setPos(chestPos.getX() + 1.5, chestPos.getY(), chestPos.getZ() + 0.5);
        bystander.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));

        InteractionResult result = bystander.gameMode.useItemOn(bystander, level,
                bystander.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND,
                adjacentHit(chestPos, Direction.WEST));

        if (result == InteractionResult.FAIL) {
            helper.fail("expected a non-pilot's use of their OWN chest (held item merely a placeable "
                    + "block, never actually placed) to be allowed even though a hypothetical "
                    + "placement from this click would land in the footprint shell, got "
                    + "InteractionResult.FAIL");
            return;
        }
        if (!bystander.hasContainerOpen()) {
            helper.fail("expected the chest to have actually opened (Player#hasContainerOpen()) for "
                    + "the bystander -- a hypothetical placement position landing in the footprint "
                    + "shell must never block the CLICKED block's own use when vanilla would let that "
                    + "block consume the click first");
            return;
        }
        if (!level.getBlockState(chestPos).is(Blocks.CHEST)) {
            helper.fail("expected the bystander's own chest to remain untouched, got "
                    + level.getBlockState(chestPos) + " at " + chestPos);
            return;
        }
        if (!level.getBlockState(wouldBePlacedPos).isAir()) {
            helper.fail("expected no block to have actually been placed at the hypothetical position "
                    + wouldBePlacedPos + " (vanilla's chest-open never falls through to placement), "
                    + "got " + level.getBlockState(wouldBePlacedPos));
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-014/T14 remediation round 5 (2nd occurrence of the same bug class) falsifying-test
     * contract, non-menu-provider path: same shape as {@link
     * #nonPilotOpeningOwnChestWhoseHypotheticalPlacementWouldLandInFootprintShellIsNotBlocked} but
     * exercises the {@code instanceof LeverBlock} branch of {@code
     * EditModeBlockProtection#isKnownInteractiveBlock} instead of the menu-provider branch -- a
     * lever has no {@link net.minecraft.world.MenuProvider} at all, so this is the only way to prove
     * that branch is reached. A bystander holding a placeable block item right-clicks their OWN
     * lever, positioned so the hypothetical placement position lands in the footprint shell; the
     * lever must actually toggle ({@link LeverBlock#POWERED} flips), not get silently cancelled.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nonPilotTogglingOwnLeverWhoseHypotheticalPlacementWouldLandInFootprintShellIsNotBlocked(
            GameTestHelper helper) {
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

        if (!level.getBlockState(shipPos.east()).is(Blocks.OAK_PLANKS)) {
            helper.fail("test precondition: expected a materialized footprint member at " + shipPos.east()
                    + ", got " + level.getBlockState(shipPos.east()));
            return;
        }

        BlockPos leverPos = shipPos.east().east().east();
        level.setBlock(leverPos, Blocks.LEVER.defaultBlockState(), 3);
        if (level.getBlockState(leverPos).getValue(LeverBlock.POWERED)) {
            helper.fail("test precondition: expected the lever to start un-powered");
            return;
        }

        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        bystander.setPos(leverPos.getX() + 1.5, leverPos.getY(), leverPos.getZ() + 0.5);
        bystander.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));

        InteractionResult result = bystander.gameMode.useItemOn(bystander, level,
                bystander.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND,
                adjacentHit(leverPos, Direction.WEST));

        if (result == InteractionResult.FAIL) {
            helper.fail("expected a non-pilot's use of their OWN lever (held item merely a placeable "
                    + "block, never actually placed) to be allowed even though a hypothetical "
                    + "placement from this click would land in the footprint shell, got "
                    + "InteractionResult.FAIL");
            return;
        }
        if (!level.getBlockState(leverPos).getValue(LeverBlock.POWERED)) {
            helper.fail("expected the lever to have actually toggled on -- a hypothetical placement "
                    + "position landing in the footprint shell must never block the CLICKED block's "
                    + "own use when vanilla would let that block consume the click first");
            return;
        }

        helper.succeed();
    }
}
