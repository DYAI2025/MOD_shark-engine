package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModComponents;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.content.block.ThrusterBlock;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import dev.sharkengine.ship.TrailColor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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

import java.util.ArrayList;
import java.util.List;

/**
 * REQ-020/AC-020 (T22) falsifying-test contract (test-plan {@code
 * docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-020 — Trail isolation
 * and bounded rendering"):
 *
 * <ol>
 *   <li>{@link #coloredAndPlainTwinShipsHaveIdenticalStats} — two otherwise-identical ships,
 *       one with a plain thruster and one with a RED-componented thruster placed through the
 *       real item path, must produce BIT-IDENTICAL gameplay stats (mass, block count, max
 *       speed, propulsion presence). This is the end-to-end proof that color never leaks into
 *       {@code VehiclePartRegistry} resolution — the unit-level half (state-decorated ids fall
 *       back to a mass-1, no-propulsion definition, which is exactly the silent stats change
 *       the tester names) lives in {@code VehiclePartRegistryTest}.</li>
 *   <li>{@link #coloredFleetStressSmoke} — QUALITATIVE-ONLY (per OQ-005/NFR-006 the user
 *       explicitly declined a hard numeric performance gate): a fleet of colored-thruster ships
 *       ticks for 100 ticks without a throw and with every entity still alive.</li>
 * </ol>
 *
 * <p><b>Honestly-disclosed limit:</b> the trail PARTICLE path itself is client-only
 * ({@code ShipEntityRenderer.spawnThrusterParticles}) and cannot execute in a headless
 * GameTest server — this stress smoke proves server-side fleet stability with colored states;
 * visual particle behavior remains a manual REQ-024 client-smoke item. Neither gap is claimed
 * as covered.</p>
 */
public final class TrailParticleStressGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    private static void placeStructureSansThruster(GameTestHelper helper, BlockPos wheelPos) {
        helper.setBlock(wheelPos, ModBlocks.STEERING_WHEEL);
        helper.setBlock(wheelPos.north(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.south(), ModBlocks.PILOT_SEAT);
        helper.setBlock(wheelPos.east(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.west(), Blocks.OAK_PLANKS);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(wheelPos.north().north(), bug);
    }

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

    private record ShipStatsSnapshot(int mass, int blockCount, float maxSpeed, boolean hasThrusters) {}

    private static ShipStatsSnapshot assembleAndSnapshot(GameTestHelper helper, ServerPlayer pilot,
                                                         BlockPos wheelWorldPos) {
        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail("test precondition: expected assembly to succeed, got " + result.translationKey());
            return null;
        }
        List<ShipEntity> ships = helper.getLevel().getEntities(
                ModEntities.SHIP, new AABB(wheelWorldPos).inflate(8), e -> true);
        if (ships.size() != 1) {
            helper.fail("test precondition: expected exactly one ShipEntity, got " + ships.size());
            return null;
        }
        ShipEntity ship = ships.get(0);
        ShipStatsSnapshot snapshot = new ShipStatsSnapshot(
                ship.getMass(), ship.getBlockCount(), ship.getMaxSpeed(), ship.hasThrusters());
        // Restore the structure for the next round and remove the entity
        ship.disassemble();
        return snapshot;
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void coloredAndPlainTwinShipsHaveIdenticalStats(GameTestHelper helper) {
        placeStructureSansThruster(helper, WHEEL_POS);
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();

        // Round 1: PLAIN thruster via the real item path (standing clear of the target)
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() - 3.0);
        placeViaItem(helper, pilot, new ItemStack(ModBlocks.THRUSTER), WHEEL_POS);
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        ShipStatsSnapshot plain = assembleAndSnapshot(helper, pilot, wheelWorldPos);
        if (plain == null) {
            return;
        }

        // Round 2: identical structure, but the thruster carries the RED craft-time component.
        // disassemble() restored the plain structure — swap only the thruster.
        helper.setBlock(WHEEL_POS.above(), Blocks.AIR.defaultBlockState());
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() - 3.0);
        ItemStack colored = new ItemStack(ModBlocks.THRUSTER);
        colored.set(ModComponents.TRAIL_COLOR, DyeColor.RED);
        placeViaItem(helper, pilot, colored, WHEEL_POS);
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        if (!helper.getBlockState(WHEEL_POS.above()).toString().contains("trail_color=red")) {
            helper.fail("test precondition: swapped thruster is not RED-colored");
            return;
        }
        ShipStatsSnapshot coloredStats = assembleAndSnapshot(helper, pilot, wheelWorldPos);
        if (coloredStats == null) {
            return;
        }

        if (!coloredStats.equals(plain)) {
            helper.fail("trail color leaked into gameplay stats: plain=" + plain
                    + " vs colored=" + coloredStats + " — resolution must key on block identity "
                    + "(AC-020: identical mass/speed/propulsion regardless of color)");
        }
        helper.succeed();
    }

    @GameTest(template = "sharkengine:gametest_large_empty", timeoutTicks = 300)
    public void coloredFleetStressSmoke(GameTestHelper helper) {
        // QUALITATIVE-ONLY (OQ-005/NFR-006): no numeric gate — the fleet must tick without a
        // throw and stay alive. 5 ships, 3 blocks each, every thruster colored differently.
        BlockState coloredThruster = ModBlocks.THRUSTER.defaultBlockState();
        TrailColor[] palette = {TrailColor.RED, TrailColor.BLUE, TrailColor.LIME,
                TrailColor.MAGENTA, TrailColor.CYAN};
        List<ShipEntity> fleet = new ArrayList<>();
        for (int i = 0; i < palette.length; i++) {
            BlockPos centerRel = new BlockPos(6 + i * 7, 4, 20);
            BlockPos centerAbs = helper.absolutePos(centerRel);
            List<ShipBlueprint.ShipBlock> blocks = List.of(
                    new ShipBlueprint.ShipBlock(0, 0, 0, Blocks.OAK_PLANKS.defaultBlockState()),
                    new ShipBlueprint.ShipBlock(1, 0, 0,
                            coloredThruster.setValue(ThrusterBlock.TRAIL_COLOR, palette[i])),
                    new ShipBlueprint.ShipBlock(-1, 0, 0, Blocks.OAK_PLANKS.defaultBlockState()));
            ShipEntity ship = new ShipEntity(ModEntities.SHIP, helper.getLevel());
            ship.setPos(centerAbs.getX() + 0.5, centerAbs.getY() + 0.5, centerAbs.getZ() + 0.5);
            ship.setBlueprint(new ShipBlueprint(centerAbs, blocks));
            helper.getLevel().addFreshEntity(ship);
            fleet.add(ship);
        }

        helper.runAfterDelay(100, () -> {
            long alive = fleet.stream().filter(ship -> !ship.isRemoved()).count();
            if (alive != fleet.size()) {
                helper.fail("colored fleet lost ships during the stress window: "
                        + alive + "/" + fleet.size() + " alive after 100 ticks");
            }
            helper.succeed();
        });
    }
}
