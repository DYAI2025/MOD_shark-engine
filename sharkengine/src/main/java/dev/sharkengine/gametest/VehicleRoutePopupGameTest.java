package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.net.TutorialPopupS2CPayload;
import dev.sharkengine.ship.VehicleClass;
import dev.sharkengine.tutorial.TutorialPopupStage;
import dev.sharkengine.tutorial.TutorialService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

/**
 * REQ-001 (AC-001): placing a Steering Wheel AND right-clicking an already-placed,
 * previously-untouched Steering Wheel must both trigger the same route-selection popup path
 * server-side, and the resulting popup state must carry exactly 3 route identifiers — AIR, LAND,
 * WATER. Verified programmatically via {@link TutorialService#lastPopupSent}, not by screenshot,
 * per the tester's counter-thesis that a screenshot proves nothing about which server event
 * actually opened the popup.
 *
 * <p>Both tests call the exact production hooks directly —
 * {@link dev.sharkengine.content.block.SteeringWheelBlock#setPlacedBy} for placement and
 * {@link BlockState#useWithoutItem} (which dispatches to
 * {@code SteeringWheelBlock#useWithoutItem}) for the interact path — mirroring
 * {@link ShipEntityMountGameTest}'s style of exercising production interaction methods directly
 * instead of fully simulating client-side click machinery.</p>
 */
public final class VehicleRoutePopupGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(1, 1, 1);

    @GameTest(template = EMPTY_STRUCTURE)
    public void popupOpensOnPlacement(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // "Placing a Steering Wheel": the block enters the world, then vanilla's
        // BlockItem#place calls Block#setPlacedBy with the placing player — reproduce exactly
        // that sequence (helper.setBlock() alone only fires Block#onPlace, which has no player
        // reference at all, so it cannot be the trigger point for a per-player popup).
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        BlockState state = helper.getLevel().getBlockState(wheelPos);
        ModBlocks.STEERING_WHEEL.setPlacedBy(helper.getLevel(), wheelPos, state, player,
                new ItemStack(ModBlocks.STEERING_WHEEL.asItem()));

        assertRouteSelectionPopup(helper, player, "placing a Steering Wheel");
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void popupOpensOnInteractWithExistingWheel(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(WHEEL_POS);
        // "Already-placed, previously-untouched": the wheel enters the world with no placing
        // player involved at all (no setPlacedBy call here) — the only trigger under test is the
        // right-click below.
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockState state = helper.getLevel().getBlockState(wheelPos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(wheelPos), Direction.UP, wheelPos, false);
        state.useWithoutItem(helper.getLevel(), player, hit);

        assertRouteSelectionPopup(helper, player, "right-clicking an already-placed Steering Wheel");
    }

    private static void assertRouteSelectionPopup(GameTestHelper helper, ServerPlayer player, String triggerDescription) {
        TutorialPopupS2CPayload popup = TutorialService.lastPopupSent(player.getUUID());
        if (popup == null) {
            helper.fail("expected " + triggerDescription + " to send a route-selection popup, but no popup was recorded");
            return;
        }
        if (popup.stage() != TutorialPopupStage.MODE_SELECTION) {
            helper.fail("expected " + triggerDescription + " to open the route-selection (MODE_SELECTION) popup, got stage="
                    + popup.stage());
            return;
        }
        if (popup.routes().size() != 3) {
            helper.fail("expected exactly 3 routes, got " + popup.routes().size() + ": " + popup.routes());
            return;
        }
        Set<VehicleClass> routes = EnumSet.copyOf(popup.routes());
        Set<VehicleClass> expected = EnumSet.of(VehicleClass.AIR, VehicleClass.LAND, VehicleClass.WATER);
        if (!routes.equals(expected)) {
            helper.fail("expected routes AIR/LAND/WATER, got " + popup.routes());
            return;
        }
        helper.succeed();
    }
}
