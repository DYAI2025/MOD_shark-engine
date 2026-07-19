package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.VehicleClass;
import dev.sharkengine.ship.session.BlockCoord;
import dev.sharkengine.ship.session.VehicleBuildSession;
import dev.sharkengine.ship.session.VehicleBuildSessionRegistry;
import dev.sharkengine.ship.session.VehicleBuildSessionStatus;
import dev.sharkengine.tutorial.TutorialPopupStage;
import dev.sharkengine.tutorial.TutorialService;
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

/**
 * REQ-002/AC-002: Release 1 activates only AIR as an executable route; LAND and WATER stay
 * selectable but never create a build session or mutate the world.
 *
 * <p>Counter-thesis this test kills (test-plan REQ-002 section): "no world mutation" is trivially
 * satisfiable either by disabling/greying out LAND/WATER (not "sichtbar" as a route per CAN-006 --
 * a disabled button looks broken, not "future") or by silently swallowing the click with zero
 * feedback (technically non-mutating, but indistinguishable from a bug). A test that only asserts
 * "world unchanged" cannot tell a designed non-goal from a silently broken button -- so this test
 * additionally asserts on {@link TutorialService#lastModeLockedNotice}, the production hook that
 * records the explicit "coming soon" notice {@code TutorialService#handleModeSelection} sends for
 * every non-AIR selection, proving the selection was actively processed and answered rather than
 * dropped.</p>
 *
 * <p>All three tests call the exact production entry point,
 * {@link TutorialService#handleModeSelection}, directly -- mirroring
 * {@link VehicleRoutePopupGameTest}'s and {@link BuildSessionAuthorizationGameTest}'s discipline of
 * exercising real production hooks instead of re-implementing them.</p>
 */
public final class VehicleRouteAvailabilityGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 40);

    /** A structurally-valid AIR ship, so "no mutation" is proven against something mutation-capable. */
    private static final BlockPos[] STRUCTURE_OFFSETS = {
            BlockPos.ZERO,
            new BlockPos(0, 0, -1),
            new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(0, 0, -2),
    };

    private static void placeValidStructure(GameTestHelper helper, BlockPos wheelPos) {
        helper.setBlock(wheelPos, ModBlocks.STEERING_WHEEL);
        helper.setBlock(wheelPos.north(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.south(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.east(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.west(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.above(), ModBlocks.THRUSTER);
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

    /**
     * Shared body for LAND/WATER: selection must (a) be actively processed -- not silently
     * dropped -- (b) answer with an explicit, distinguishable "coming soon" notice naming exactly
     * that mode, (c) never create a {@link VehicleBuildSession} at this wheel, and (d) leave the
     * world (blocks + entity count) byte-for-byte unchanged.
     */
    private static void assertNonAirRouteIsInteractableButNonMutating(GameTestHelper helper, VehicleClass mode) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        placeValidStructure(helper, WHEEL_POS);
        Map<BlockPos, BlockState> before = snapshot(helper, WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);
        String dimensionId = helper.getLevel().dimension().location().toString();
        BlockCoord coord = new BlockCoord(wheelPos.getX(), wheelPos.getY(), wheelPos.getZ());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);

        // Real trigger path: open the route popup first (as placing/interacting with the wheel
        // does), exactly as a player who then picks a non-AIR route would experience it.
        TutorialService.startModeSelection(player, wheelPos);
        TutorialService.handleModeSelection(player, mode);

        // (a)+(b): the selection was actively answered with an explicit, mode-specific notice --
        // not silence, and not a rejection indistinguishable from "nothing happened".
        VehicleClass notice = TutorialService.lastModeLockedNotice(player.getUUID());
        if (notice == null) {
            helper.fail(mode + ": expected an explicit 'coming soon' notice after selecting a non-AIR "
                    + "route, but none was recorded -- selection was silently swallowed (exactly the "
                    + "counter-thesis's named failure: indistinguishable from a broken/disabled button)");
            return;
        }
        if (notice != mode) {
            helper.fail(mode + ": expected the notice to name the selected mode, got " + notice);
            return;
        }

        // (a): the route-selection popup is re-shown with all 3 routes still available -- proving
        // the player can still interact with LAND/WATER again, not that they were locked out.
        var popup = TutorialService.lastPopupSent(player.getUUID());
        if (popup == null || popup.stage() != TutorialPopupStage.MODE_SELECTION || popup.routes().size() != 3) {
            helper.fail(mode + ": expected the mode-selection popup (with all 3 routes) to be re-shown "
                    + "after a non-AIR selection, got " + (popup == null ? "no popup" : popup.stage() + "/" + popup.routes()));
            return;
        }

        // (c): REQ-002's "nicht fahrzeugerzeugend" -- no VehicleBuildSession may exist at this wheel.
        VehicleBuildSession session = VehicleBuildSessionRegistry.findByWheel(dimensionId, coord);
        if (session != null) {
            helper.fail(mode + ": expected no VehicleBuildSession to be created for a non-AIR route, "
                    + "but found one: " + session);
            return;
        }

        // (d): full block-state + entity-count diff of the interaction area is empty.
        assertZeroWorldDiff(helper, WHEEL_POS, before, entitiesBefore, mode.toString());

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void landSelectionIsInteractableButNonMutating(GameTestHelper helper) {
        assertNonAirRouteIsInteractableButNonMutating(helper, VehicleClass.LAND);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void waterSelectionIsInteractableButNonMutating(GameTestHelper helper) {
        assertNonAirRouteIsInteractableButNonMutating(helper, VehicleClass.WATER);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void airSelectionCreatesExactlyOneSession(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        String dimensionId = helper.getLevel().dimension().location().toString();
        BlockCoord coord = new BlockCoord(wheelPos.getX(), wheelPos.getY(), wheelPos.getZ());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(wheelPos.getX() + 0.5, wheelPos.getY(), wheelPos.getZ() + 0.5);

        if (VehicleBuildSessionRegistry.findByWheel(dimensionId, coord) != null) {
            helper.fail("test precondition violated: a session already exists at this wheel before selection");
            return;
        }

        TutorialService.startModeSelection(player, wheelPos);
        TutorialService.handleModeSelection(player, VehicleClass.AIR);

        VehicleBuildSession session = VehicleBuildSessionRegistry.findByWheel(dimensionId, coord);
        if (session == null) {
            helper.fail("expected AIR selection to create exactly one VehicleBuildSession, but none was found");
            return;
        }
        if (!session.playerId().equals(player.getUUID())) {
            helper.fail("expected the session to be tied to the selecting player, got playerId=" + session.playerId());
            return;
        }
        if (!session.wheelPos().equals(coord)) {
            helper.fail("expected the session to be tied to the selected wheel position, got wheelPos=" + session.wheelPos());
            return;
        }
        if (session.vehicleClass() != VehicleClass.AIR) {
            helper.fail("expected the session's vehicle class to be AIR, got " + session.vehicleClass());
            return;
        }
        if (session.status() != VehicleBuildSessionStatus.ACTIVE) {
            helper.fail("expected a freshly-created session to be ACTIVE, got " + session.status());
            return;
        }

        // "Exactly one": re-selecting AIR must not leave a second, separate session lying around --
        // the same (dimension, wheel) key must still resolve to a single session afterward.
        TutorialService.startModeSelection(player, wheelPos);
        TutorialService.handleModeSelection(player, VehicleClass.AIR);
        VehicleBuildSession afterReselect = VehicleBuildSessionRegistry.findByWheel(dimensionId, coord);
        if (afterReselect == null) {
            helper.fail("expected exactly one session to still exist after re-selecting AIR");
            return;
        }
        if (!afterReselect.playerId().equals(player.getUUID())) {
            helper.fail("expected the single session to remain tied to the same player after re-selection, got "
                    + afterReselect.playerId());
            return;
        }

        helper.succeed();
    }
}
