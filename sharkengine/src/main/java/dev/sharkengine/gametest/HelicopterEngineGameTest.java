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
 * AIR-040 gametest for {@code helicopter_engine} — the fourth of the six AIR-040 core
 * placeable parts (PROPULSION role, {@code liftMode=ROTOR} per
 * {@link VehicleBalance#PARTS}).
 *
 * <p>Two methods, mirroring {@link FuselageFrameGameTest}/{@link AirframePanelGameTest}'s
 * table-driven-{@code ShipStats}-delta + loot-roundtrip pattern, plus one method specific
 * to this part's role:</p>
 * <ul>
 *   <li>{@link #helicopterEngineAssemblesWithExactBalanceAndDropsOnBreak}: same pattern as
 *       the other core parts — attach one {@code helicopter_engine} to an already-valid
 *       (wheel+4 planks+thruster+bug) structure, assert the exact {@code ShipStats} delta,
 *       then break it and assert the loot table drops the item.</li>
 *   <li>{@link #helicopterEngineAloneSatisfiesPropulsionRequirement}: the part-specific
 *       property this task's own scope note calls out — {@code ShipPartAnalyzer} counts
 *       {@code propulsionCount} by {@code PartRole.PROPULSION} alone, never by
 *       {@code liftMode} (that distinction is AIR-054's job, not yet implemented), so a
 *       structure with a {@code helicopter_engine} and <b>no thruster at all</b> already
 *       satisfies {@code ShipAssemblyService}'s "at least one PROPULSION part" check today.
 *       This is a transitional state, explicitly called out in the plan doc's AIR-040
 *       evidence line ("until then assembly uses the plain PROPULSION check... so
 *       helicopter_engine qualifies transitionally") — this test proves it, and will need
 *       to be revisited (not necessarily deleted — a helicopter_engine + valid rotor is
 *       still supposed to fly) once AIR-054 lands the strict rotor/lift rule.</li>
 * </ul>
 */
public final class HelicopterEngineGameTest implements FabricGameTest {

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
     * Assembles a minimal valid ship, attaches one {@code helicopter_engine} to the
     * structure and asserts the {@code ShipStats} delta matches this part's exact
     * mass/lift/thrust/drag/fuelCapacity row in {@link VehicleBalance#PARTS} — a real
     * balance-table assertion, not just "assembly still succeeds" — then breaks the
     * placed engine and asserts its loot table drops the item back (dropSelf roundtrip).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void helicopterEngineAssemblesWithExactBalanceAndDropsOnBreak(GameTestHelper helper) {
        placeCoreStructure(helper);
        ShipAssemblyService.StructureScan baseline = scan(helper);
        if (!baseline.canAssemble()) {
            helper.fail("expected baseline canAssemble()=true before adding helicopter_engine "
                    + "(blocks=" + baseline.blockCount()
                    + ", contactPoints=" + baseline.contactPoints()
                    + ", coreNeighbors=" + baseline.coreNeighbors()
                    + ", bugCount=" + baseline.bugCount() + ")");
            return;
        }
        ShipStats baseStats = baseline.stats();

        // Attach one helicopter_engine two blocks east of the wheel — connected to the
        // wheel via BFS through the east-neighbor plank (already ship_eligible), at the
        // same height so it cannot introduce a terrain contact point.
        BlockPos enginePos = WHEEL_POS.east().east();
        helper.setBlock(enginePos, ModBlocks.HELICOPTER_ENGINE);

        ShipAssemblyService.StructureScan withEngine = scan(helper);
        if (!withEngine.canAssemble()) {
            helper.fail("expected canAssemble()=true after adding one connected helicopter_engine "
                    + "(invalidAttachments=" + withEngine.invalidAttachments().size()
                    + ", contactPoints=" + withEngine.contactPoints() + ")");
            return;
        }
        ShipStats withEngineStats = withEngine.stats();

        VehiclePartDefinition expected = VehicleBalance.PARTS.get("sharkengine:helicopter_engine");
        if (expected == null) {
            helper.fail("VehicleBalance.PARTS has no entry for sharkengine:helicopter_engine");
            return;
        }

        int massDelta = withEngineStats.mass() - baseStats.mass();
        int liftDelta = withEngineStats.lift() - baseStats.lift();
        int thrustDelta = withEngineStats.thrust() - baseStats.thrust();
        int dragDelta = withEngineStats.drag() - baseStats.drag();
        int fuelCapDelta = withEngineStats.fuelCapacity() - baseStats.fuelCapacity();

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

        // Loot table roundtrip: break the placed engine and assert it drops its item.
        // Deliberately NOT GameTestHelper#destroyBlock(BlockPos) — ground-truthed by
        // decompiling that method against the pinned Minecraft jar: it calls
        // ServerLevel.destroyBlock(pos, /* dropBlock= */ false, null), i.e. it clears
        // the block WITHOUT running its loot table at all. Calling
        // ServerLevel.destroyBlock directly with dropBlock=true reproduces what a
        // player actually does when mining the block.
        helper.getLevel().destroyBlock(helper.absolutePos(enginePos), true, null);
        helper.succeedWhen(() -> helper.assertItemEntityPresent(ModBlocks.HELICOPTER_ENGINE.asItem(), enginePos, 2.0));
    }

    /**
     * A structure with a {@code helicopter_engine} and NO thruster at all still
     * satisfies {@code canAssemble()}'s propulsion check today, because
     * {@code ShipPartAnalyzer} counts {@code propulsionCount} by role
     * ({@code PartRole.PROPULSION}) alone — {@code liftMode} (DIRECT vs ROTOR) is not
     * consulted until AIR-054. This is the exact "transitional" behavior this task's own
     * scope note calls out: "once this exists, a ship with only a helicopter_engine and
     * no thruster should be assemblable".
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void helicopterEngineAloneSatisfiesPropulsionRequirement(GameTestHelper helper) {
        // Same core structure as placeCoreStructure(), but with helicopter_engine
        // replacing the thruster above the wheel — no thruster anywhere in this layout.
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.above(), ModBlocks.HELICOPTER_ENGINE);
        BlockState bugState = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bugState);

        ShipAssemblyService.StructureScan result = scan(helper);
        if (!result.canAssemble()) {
            helper.fail("expected canAssemble()=true with only a helicopter_engine (no thruster) providing "
                    + "propulsion (blocks=" + result.blockCount()
                    + ", hasThruster/hasPropulsion=" + result.hasThruster()
                    + ", coreNeighbors=" + result.coreNeighbors()
                    + ", bugCount=" + result.bugCount() + ")");
            return;
        }
        if (result.stats().propulsionCount() != 1) {
            helper.fail("expected exactly 1 PROPULSION part (the helicopter_engine), got "
                    + result.stats().propulsionCount());
            return;
        }
        helper.succeed();
    }
}
