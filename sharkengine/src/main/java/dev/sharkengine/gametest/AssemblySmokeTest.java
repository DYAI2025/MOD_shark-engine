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
 */
public final class AssemblySmokeTest implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void assemblesWithWheelBugAndThruster(GameTestHelper helper) {
        BlockPos wheelPos = new BlockPos(3, 1, 3);

        helper.setBlock(wheelPos, ModBlocks.STEERING_WHEEL);
        helper.setBlock(wheelPos.north(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.south(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.east(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.west(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.above(), ModBlocks.THRUSTER);

        BlockState bugState = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(wheelPos.north().north(), bugState);

        BlockPos absoluteWheelPos = helper.absolutePos(wheelPos);
        ShipAssemblyService.StructureScan scan =
                ShipAssemblyService.scanStructure(helper.getLevel(), absoluteWheelPos);

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
}
