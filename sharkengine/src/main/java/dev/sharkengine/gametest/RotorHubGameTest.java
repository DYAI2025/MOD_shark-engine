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
 * AIR-040 gametest for {@code rotor_hub} — the fifth of the six AIR-040 core
 * placeable parts (ROTOR_HUB role, {@code axis} blockstate).
 *
 * <p>Mirrors {@link FuselageFrameGameTest}/{@link HelicopterEngineGameTest}'s
 * layout/helper pattern exactly (same core structure: wheel + 4 planks + thruster +
 * edge-placed BUG). Unlike {@code helicopter_engine} (PROPULSION role), a
 * {@code rotor_hub} alone does NOT satisfy {@code ShipAssemblyService}'s "at least one
 * PROPULSION part" check — {@code ShipPartAnalyzer} counts propulsion by
 * {@code PartRole.PROPULSION} only, and {@code ROTOR_HUB} is a distinct role (concept
 * §3.3's {@code PartRole} enum) — so this test attaches the hub to an already-valid
 * (wheel+4 planks+thruster+bug) structure rather than trying to have it stand in for
 * the thruster, same as {@link FuselageFrameGameTest}/{@link AirframePanelGameTest}'s
 * STRUCTURE/SKIN parts do. Covers both a table-driven {@code ShipStats} delta
 * assertion sourced from {@link VehicleBalance#PARTS} and the loot-table roundtrip
 * (place, break, assert drop) in one method body.</p>
 */
public final class RotorHubGameTest implements FabricGameTest {

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
     * Assembles a minimal valid ship, attaches one {@code rotor_hub} to the structure
     * and asserts the {@code ShipStats} delta matches this part's exact
     * mass/lift/thrust/drag/fuelCapacity row in {@link VehicleBalance#PARTS} — a real
     * balance-table assertion, not just "assembly still succeeds" — then breaks the
     * placed hub and asserts its loot table drops the item back (dropSelf roundtrip).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void rotorHubAssemblesWithExactBalanceAndDropsOnBreak(GameTestHelper helper) {
        placeCoreStructure(helper);
        ShipAssemblyService.StructureScan baseline = scan(helper);
        if (!baseline.canAssemble()) {
            helper.fail("expected baseline canAssemble()=true before adding rotor_hub "
                    + "(blocks=" + baseline.blockCount()
                    + ", contactPoints=" + baseline.contactPoints()
                    + ", coreNeighbors=" + baseline.coreNeighbors()
                    + ", bugCount=" + baseline.bugCount() + ")");
            return;
        }
        ShipStats baseStats = baseline.stats();

        // Attach one rotor_hub two blocks east of the wheel — connected to the wheel
        // via BFS through the east-neighbor plank (already ship_eligible), at the same
        // height so it cannot introduce a terrain contact point.
        BlockPos hubPos = WHEEL_POS.east().east();
        helper.setBlock(hubPos, ModBlocks.ROTOR_HUB);

        ShipAssemblyService.StructureScan withHub = scan(helper);
        if (!withHub.canAssemble()) {
            helper.fail("expected canAssemble()=true after adding one connected rotor_hub "
                    + "(invalidAttachments=" + withHub.invalidAttachments().size()
                    + ", contactPoints=" + withHub.contactPoints() + ")");
            return;
        }
        ShipStats withHubStats = withHub.stats();

        VehiclePartDefinition expected = VehicleBalance.PARTS.get("sharkengine:rotor_hub");
        if (expected == null) {
            helper.fail("VehicleBalance.PARTS has no entry for sharkengine:rotor_hub");
            return;
        }

        int massDelta = withHubStats.mass() - baseStats.mass();
        int liftDelta = withHubStats.lift() - baseStats.lift();
        int thrustDelta = withHubStats.thrust() - baseStats.thrust();
        int dragDelta = withHubStats.drag() - baseStats.drag();
        int fuelCapDelta = withHubStats.fuelCapacity() - baseStats.fuelCapacity();

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

        // Loot table roundtrip: break the placed hub and assert it drops its item.
        // Deliberately NOT GameTestHelper#destroyBlock(BlockPos) — ground-truthed by
        // decompiling that method against the pinned Minecraft jar: it calls
        // ServerLevel.destroyBlock(pos, /* dropBlock= */ false, null), i.e. it clears
        // the block WITHOUT running its loot table at all. Calling
        // ServerLevel.destroyBlock directly with dropBlock=true reproduces what a
        // player actually does when mining the block.
        helper.getLevel().destroyBlock(helper.absolutePos(hubPos), true, null);
        helper.succeedWhen(() -> helper.assertItemEntityPresent(ModBlocks.ROTOR_HUB.asItem(), hubPos, 2.0));
    }
}
