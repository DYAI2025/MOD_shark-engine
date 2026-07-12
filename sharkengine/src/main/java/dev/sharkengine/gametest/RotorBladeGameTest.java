package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.part.ShipStats;
import dev.sharkengine.ship.part.VehicleBalance;
import dev.sharkengine.ship.part.VehiclePartDefinition;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AIR-040 gametest for {@code rotor_blade} — the sixth and last of the six AIR-040
 * core placeable parts (ROTOR_BLADE role, {@code facing} blockstate).
 *
 * <p>Mirrors {@link RotorHubGameTest}'s layout/helper pattern exactly (same core
 * structure: wheel + 4 planks + thruster + edge-placed BUG). Like {@code rotor_hub}
 * (ROTOR_HUB role) and unlike {@code helicopter_engine} (PROPULSION role), a
 * {@code rotor_blade} alone does NOT satisfy {@code ShipAssemblyService}'s "at least
 * one PROPULSION part" check — {@code ShipPartAnalyzer} counts propulsion by
 * {@code PartRole.PROPULSION} only, and {@code ROTOR_BLADE} is a distinct role
 * (concept §3.3's {@code PartRole} enum) — so this test attaches the blade to an
 * already-valid (wheel+4 planks+thruster+bug) structure rather than trying to have it
 * stand in for the thruster, same as {@link RotorHubGameTest}/{@link
 * AirframePanelGameTest}'s ROTOR_HUB/SKIN parts do. Covers both a table-driven
 * {@code ShipStats} delta assertion sourced from {@link VehicleBalance#PARTS} and the
 * loot-table roundtrip (place, break, assert drop) in one method body.</p>
 */
public final class RotorBladeGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    private static void placeCoreStructure(GameTestHelper helper) {
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
        BlockState bugState = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bugState);
    }

    private static ShipAssemblyService.StructureScan scan(GameTestHelper helper) {
        BlockPos absoluteWheelPos = helper.absolutePos(WHEEL_POS);
        return ShipAssemblyService.scanStructure(helper.getLevel(), absoluteWheelPos);
    }

    /**
     * Assembles a minimal valid ship, attaches one {@code rotor_blade} to the
     * structure and asserts the {@code ShipStats} delta matches this part's exact
     * mass/lift/thrust/drag/fuelCapacity row in {@link VehicleBalance#PARTS} — a real
     * balance-table assertion (in particular {@code lift=8}, the single highest
     * per-block lift figure in the whole table), not just "assembly still succeeds" —
     * then breaks the placed blade and asserts its loot table drops the item back
     * (dropSelf roundtrip).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void rotorBladeAssemblesWithExactBalanceAndDropsOnBreak(GameTestHelper helper) {
        placeCoreStructure(helper);
        ShipAssemblyService.StructureScan baseline = scan(helper);
        if (!baseline.canAssemble()) {
            helper.fail("expected baseline canAssemble()=true before adding rotor_blade "
                    + "(blocks=" + baseline.blockCount()
                    + ", contactPoints=" + baseline.contactPoints()
                    + ", coreNeighbors=" + baseline.coreNeighbors()
                    + ", bugCount=" + baseline.bugCount() + ")");
            return;
        }
        ShipStats baseStats = baseline.stats();

        // Attach one rotor_blade two blocks east of the wheel — connected to the
        // wheel via BFS through the east-neighbor plank (already ship_eligible), at
        // the same height so it cannot introduce a terrain contact point.
        BlockPos bladePos = WHEEL_POS.east().east();
        helper.setBlock(bladePos, ModBlocks.ROTOR_BLADE);

        ShipAssemblyService.StructureScan withBlade = scan(helper);
        if (!withBlade.canAssemble()) {
            helper.fail("expected canAssemble()=true after adding one connected rotor_blade "
                    + "(invalidAttachments=" + withBlade.invalidAttachments().size()
                    + ", contactPoints=" + withBlade.contactPoints() + ")");
            return;
        }
        ShipStats withBladeStats = withBlade.stats();

        VehiclePartDefinition expected = VehicleBalance.PARTS.get("sharkengine:rotor_blade");
        if (expected == null) {
            helper.fail("VehicleBalance.PARTS has no entry for sharkengine:rotor_blade");
            return;
        }

        int massDelta = withBladeStats.mass() - baseStats.mass();
        int liftDelta = withBladeStats.lift() - baseStats.lift();
        int thrustDelta = withBladeStats.thrust() - baseStats.thrust();
        int dragDelta = withBladeStats.drag() - baseStats.drag();
        int fuelCapDelta = withBladeStats.fuelCapacity() - baseStats.fuelCapacity();

        if (massDelta != expected.mass()) {
            helper.fail("expected mass delta=" + expected.mass() + " (VehicleBalance), got " + massDelta);
            return;
        }
        if (liftDelta != expected.lift()) {
            helper.fail("expected lift delta=" + expected.lift() + " (VehicleBalance), got " + liftDelta);
            return;
        }
        if (thrustDelta != expected.thrust()) {
            helper.fail("expected thrust delta=" + expected.thrust() + " (VehicleBalance), got " + thrustDelta);
            return;
        }
        if (dragDelta != expected.drag()) {
            helper.fail("expected drag delta=" + expected.drag() + " (VehicleBalance), got " + dragDelta);
            return;
        }
        if (fuelCapDelta != expected.fuelCapacity()) {
            helper.fail("expected fuelCapacity delta=" + expected.fuelCapacity() + " (VehicleBalance), got " + fuelCapDelta);
            return;
        }

        // Loot table roundtrip: break the placed blade and assert it drops its item.
        // Deliberately NOT GameTestHelper#destroyBlock(BlockPos) — ground-truthed by
        // decompiling that method against the pinned Minecraft jar: it calls
        // ServerLevel.destroyBlock(pos, /* dropBlock= */ false, null), i.e. it clears
        // the block WITHOUT running its loot table at all. Calling
        // ServerLevel.destroyBlock directly with dropBlock=true reproduces what a
        // player actually does when mining the block.
        helper.getLevel().destroyBlock(helper.absolutePos(bladePos), true, null);
        helper.succeedWhen(() -> helper.assertItemEntityPresent(ModBlocks.ROTOR_BLADE.asItem(), bladePos, 2.0));
    }
}
