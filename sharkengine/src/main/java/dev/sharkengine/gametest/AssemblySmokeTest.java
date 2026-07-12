package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.ShipAssemblyService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AIR-003 smoke test: proves the Fabric GameTest infrastructure is wired
 * correctly by assembling a minimal valid ship (steering wheel, 4 core
 * neighbors, thruster, edge-placed BUG block) and asserting
 * {@link ShipAssemblyService.StructureScan#canAssemble()}.
 *
 * <p>Layout (relative to the empty 8x8x8 test structure origin):</p>
 * <pre>
 *   wheel:  (3,1,3)
 *   planks: (3,1,2) (3,1,4) (2,1,3) (4,1,3)  -- the 4 required core neighbors
 *   thruster: (3,2,3)                        -- above the wheel
 *   bug:    (3,1,1)                          -- 2 north of the wheel, on the
 *                                                structure edge, facing SOUTH
 * </pre>
 *
 * <p><b>Bidirectional coverage:</b> {@link #assemblesWithWheelBugAndThruster}
 * covers the positive case; {@link #rejectsAssemblyWithoutBug} covers the
 * negative case (BUG block omitted). Both are permanent regression gates —
 * added 2026-07-12 after an ultrathink-craftsmanship review found the
 * negative case had only ever been checked ad hoc during AIR-003's original
 * development (temporarily edited then reverted), never committed as a
 * test, despite the plan's "verified bidirectionally" claim.</p>
 */
public final class AssemblySmokeTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    private static void placeCoreStructure(GameTestHelper helper) {
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
    }

    private static ShipAssemblyService.StructureScan scan(GameTestHelper helper) {
        BlockPos absoluteWheelPos = helper.absolutePos(WHEEL_POS);
        return ShipAssemblyService.scanStructure(helper.getLevel(), absoluteWheelPos);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void assemblesWithWheelBugAndThruster(GameTestHelper helper) {
        placeCoreStructure(helper);
        BlockState bugState = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bugState);

        ShipAssemblyService.StructureScan scan = scan(helper);
        if (!scan.canAssemble()) {
            helper.fail("expected canAssemble()=true (blocks=" + scan.blockCount()
                    + ", contactPoints=" + scan.contactPoints()
                    + ", hasThruster=" + scan.hasThruster()
                    + ", coreNeighbors=" + scan.coreNeighbors()
                    + ", bugCount=" + scan.bugCount()
                    + ", bugOnEdge=" + scan.bugOnEdge()
                    + ", invalidAttachments=" + scan.invalidAttachments().size() + ")");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rejectsAssemblyWithoutBug(GameTestHelper helper) {
        placeCoreStructure(helper);
        // Deliberately no BUG block placed — everything else is identical to
        // the positive-case layout above.

        ShipAssemblyService.StructureScan scan = scan(helper);
        if (scan.canAssemble()) {
            helper.fail("expected canAssemble()=false with no BUG block present, but assembly was accepted "
                    + "(bugCount=" + scan.bugCount() + ", bugOnEdge=" + scan.bugOnEdge() + ")");
            return;
        }
        if (scan.bugCount() != 0) {
            helper.fail("expected bugCount()=0 with no BUG block placed, got " + scan.bugCount());
            return;
        }
        helper.succeed();
    }
}
