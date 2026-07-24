package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModComponents;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * REQ-019/AC-019 (T21) falsifying-test contract (test-plan {@code
 * docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-019 — Persistent colored
 * trail via single render path"): the craft-time {@code trail_color} component (T20) must
 * survive the full world round trip — placement as a block, assembly into a blueprint, and the
 * entity's NBT save/load — so the flying ship still knows each thruster's color.
 *
 * <p><b>Design under test (T21 decision):</b> the color rides as a {@code trail_color}
 * BLOCKSTATE property on the single thruster block (never a second block/item id, never a
 * block entity — T20's no-hidden-storage GameTest stays green). Placement copies the item
 * component into the state; the blueprint stores BlockStates verbatim, so persistence needs no
 * new schema — which also closes T18's deferred trail-persistence slot: the reserved generic
 * {@code TrailConfig} entity tag deliberately stays unused, because the blueprint IS the single
 * source of truth (no second color store to drift). Assertions are string-based on
 * {@code BlockState.toString()} so this test compiles and runs (RED) even before the property
 * exists.</p>
 */
public final class TrailColorPersistenceGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /** Places {@code stack} onto the UP face of {@code floorRel} via the real BlockItem path. */
    private static void placeViaItem(GameTestHelper helper, ServerPlayer player,
                                     ItemStack stack, BlockPos floorRel) {
        BlockPos floorAbs = helper.absolutePos(floorRel);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(floorAbs).add(0, 0.5, 0), Direction.UP, floorAbs, false);
        InteractionResult result = stack.useOn(
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        if (!result.consumesAction()) {
            helper.fail("test precondition: BlockItem placement was rejected (" + result + ")");
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void placedColoredThrusterKeepsItsColor(GameTestHelper helper) {
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos floorAbs = helper.absolutePos(floor);
        player.setPos(floorAbs.getX() + 0.5, floorAbs.getY() + 1, floorAbs.getZ() - 2.0);

        ItemStack colored = new ItemStack(ModBlocks.THRUSTER);
        colored.set(ModComponents.TRAIL_COLOR, DyeColor.RED);
        placeViaItem(helper, player, colored, floor);

        BlockState placed = helper.getBlockState(floor.above());
        if (!placed.is(ModBlocks.THRUSTER)) {
            helper.fail("test precondition: expected a thruster block above the floor, got " + placed);
        }
        if (!placed.toString().contains("trail_color=red")) {
            helper.fail("placed colored thruster lost its craft-time color — state is " + placed
                    + " (no trail_color=red): the component does not survive placement");
        }
        // And an UNCOLORED item must place the default state (existing standard trail)
        ItemStack plain = new ItemStack(ModBlocks.THRUSTER);
        BlockPos floor2 = new BlockPos(3, 1, 1);
        helper.setBlock(floor2, Blocks.STONE);
        placeViaItem(helper, player, plain, floor2);
        BlockState placedPlain = helper.getBlockState(floor2.above());
        if (placedPlain.toString().contains("trail_color=")
                && !placedPlain.toString().contains("trail_color=none")) {
            helper.fail("uncolored thruster placed with a non-default trail color: " + placedPlain);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void coloredThrusterColorSurvivesBlueprintSaveLoad(GameTestHelper helper) {
        // 7-block structure, but the thruster is placed via the REAL item path with a RED component
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), ModBlocks.PILOT_SEAT);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), Blocks.OAK_PLANKS);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bug);

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        // Stand clear while placing: a player whose collision box overlaps the target position
        // (wheel.above()) makes vanilla reject the placement — found in the first GREEN run.
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() - 3.0);

        ItemStack colored = new ItemStack(ModBlocks.THRUSTER);
        colored.set(ModComponents.TRAIL_COLOR, DyeColor.RED);
        placeViaItem(helper, pilot, colored, WHEEL_POS); // onto the wheel's UP face → wheel.above()
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        if (!helper.getBlockState(WHEEL_POS.above()).is(ModBlocks.THRUSTER)) {
            helper.fail("test precondition: colored thruster did not land at wheel.above()");
            return;
        }

        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail("test precondition: expected assembly to succeed, got " + result.translationKey());
            return;
        }
        List<ShipEntity> ships = helper.getLevel().getEntities(
                ModEntities.SHIP, new AABB(wheelWorldPos).inflate(8), e -> true);
        if (ships.size() != 1) {
            helper.fail("test precondition: expected exactly one ShipEntity, got " + ships.size());
            return;
        }
        ShipEntity ship = ships.get(0);

        CompoundTag tag = ship.saveWithoutId(new CompoundTag());
        ShipEntity reloaded = new ShipEntity(ModEntities.SHIP, helper.getLevel());
        reloaded.load(tag);
        ShipBlueprint bp = reloaded.getBlueprint();
        if (bp == null) {
            helper.fail("blueprint lost on reload");
            return;
        }
        boolean foundColored = bp.blocks().stream().anyMatch(b ->
                b.state().is(ModBlocks.THRUSTER) && b.state().toString().contains("trail_color=red"));
        if (!foundColored) {
            helper.fail("reloaded blueprint has no RED-colored thruster state — trail color does not "
                    + "survive the assembly + NBT round trip (AC-019's persistence half)");
        }
        reloaded.discard();
        helper.succeed();
    }
}
