package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REQ-021/AC-021 (T15) falsifying-test contract (test-plan {@code
 * docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-021 — Transactional world
 * mutation"): assembly failure paths must leave the world byte-identical (zero mutation), and
 * disassembly must restore EXACTLY the blueprint's block count — "not '≥1 restored'" — at both a
 * small smoke scale and near {@link ShipAssemblyService#MAX_BLOCKS}.
 *
 * <p><b>Scenario coverage:</b></p>
 * <ul>
 *   <li>{@link #assemblyFailurePathNeverMutatesWorld}: (1) the TOCTOU race — a structure that was
 *       previewable/valid is invalidated (thruster broken) before the commit arrives; {@link
 *       ShipAssemblyService#tryAssemble} re-scans and must reject with zero mutation (this is
 *       T14's preflight-before-mutation pattern: every validation sits textually before the first
 *       {@code setBlock}). (2) the spawn-blocked path — a second, parked {@link ShipEntity}
 *       overlaps the assembly footprint; committing would spawn a ship inside a ship (RISK-004's
 *       duplication shape), so the commit must be refused with zero mutation.</li>
 *   <li>{@link #disassemblyRestoresExactBlockCountAtSmallScale}: the same 7-block structure used
 *       by {@code AtomicEditReassemblyGameTest} round-trips assemble → disassemble with every
 *       block restored to its exact pre-assembly {@link BlockState} and position.</li>
 *   <li>{@link #disassemblyRestoresExactBlockCountAtMaxScale}: a programmatically-built blueprint
 *       of exactly {@link ShipAssemblyService#MAX_BLOCKS} (512) blocks restores all 512.</li>
 * </ul>
 *
 * <p><b>Honestly-disclosed coverage limits (per the planner's own T15 notes):</b> true
 * chunk-unload-mid-disassembly is not exercisable inside a GameTest template. Additionally,
 * {@link ShipAssemblyService#MAX_RADIUS} (32, Manhattan) is not literally reachable either: a
 * radius-32 structure spans 65 blocks, beyond any loadable test template, so the max-scale test
 * stresses the BLOCK-COUNT cap exactly (512/512) inside a custom 40×14×40 template
 * ({@code sharkengine:gametest_large_empty}) with Manhattan extent up to 22. Neither gap is
 * silently claimed as covered.</p>
 *
 * <p><b>Why the max-scale blueprint is built programmatically</b> instead of via {@link
 * ShipAssemblyService#tryAssemble}: a 512-block structure satisfying every assembly rule (seat
 * anchor, cockpit visibility, BUG edge…) would be enormous test scaffolding, and the contract
 * under test — {@link ShipEntity#disassemble} restoring blueprint → world — takes the blueprint
 * as its input regardless of how assembly produced it. The small-scale test covers the real
 * production assemble→disassemble round trip end-to-end.</p>
 */
public final class AssemblyDisassemblyRollbackGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /** Same 7-block minimal valid structure as {@code AtomicEditReassemblyGameTest}. */
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

    private static List<BlockPos> structureRelPositions(BlockPos wheelPos) {
        return List.of(wheelPos, wheelPos.north(), wheelPos.south(), wheelPos.east(),
                wheelPos.west(), wheelPos.above(), wheelPos.north().north());
    }

    private static Map<BlockPos, BlockState> snapshotWorld(GameTestHelper helper, List<BlockPos> relPositions) {
        Map<BlockPos, BlockState> snapshot = new HashMap<>();
        for (BlockPos rel : relPositions) {
            snapshot.put(rel, helper.getBlockState(rel));
        }
        return snapshot;
    }

    private static void assertWorldUnchanged(GameTestHelper helper, Map<BlockPos, BlockState> snapshot, String scenario) {
        for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
            BlockState now = helper.getBlockState(entry.getKey());
            if (!now.equals(entry.getValue())) {
                helper.fail(scenario + ": world mutated at " + entry.getKey()
                        + " — expected " + entry.getValue() + ", found " + now);
            }
        }
    }

    private static List<ShipEntity> shipsNear(GameTestHelper helper, BlockPos wheelWorldPos) {
        return helper.getLevel().getEntities(ModEntities.SHIP, new AABB(wheelWorldPos).inflate(16), e -> true);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void assemblyFailurePathNeverMutatesWorld(GameTestHelper helper) {
        placeValidStructure(helper, WHEEL_POS);
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);

        // ── Scenario 1: TOCTOU — the structure was valid (the small-scale test below assembles
        // this exact layout), then the only thruster is broken before the commit arrives.
        helper.setBlock(WHEEL_POS.above(), Blocks.AIR.defaultBlockState());
        List<BlockPos> remaining = new ArrayList<>(structureRelPositions(WHEEL_POS));
        remaining.remove(WHEEL_POS.above());
        Map<BlockPos, BlockState> snapshotToctou = snapshotWorld(helper, remaining);

        ShipAssemblyService.AssembleResult toctou =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (toctou.isSuccess()) {
            helper.fail("TOCTOU: assembly succeeded on a structure whose thruster was broken after preview");
        }
        assertWorldUnchanged(helper, snapshotToctou, "TOCTOU");
        if (!shipsNear(helper, wheelWorldPos).isEmpty()) {
            helper.fail("TOCTOU: rejected assembly still spawned a ShipEntity");
        }

        // ── Scenario 2: spawn position blocked by another (parked) ShipEntity overlapping the
        // footprint. Structure is made valid again first, so the ONLY failing condition is the
        // blocker — otherwise the earlier validation chain would mask the missing preflight.
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
        ShipEntity blocker = new ShipEntity(ModEntities.SHIP, helper.getLevel());
        blocker.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY() + 0.5, wheelWorldPos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(blocker);

        Map<BlockPos, BlockState> snapshotBlocked = snapshotWorld(helper, structureRelPositions(WHEEL_POS));
        ShipAssemblyService.AssembleResult blocked =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (blocked.isSuccess()) {
            helper.fail("spawn-blocked: assembly succeeded although a parked ShipEntity overlaps the footprint");
        }
        assertWorldUnchanged(helper, snapshotBlocked, "spawn-blocked");
        List<ShipEntity> ships = shipsNear(helper, wheelWorldPos);
        if (ships.size() != 1) {
            helper.fail("spawn-blocked: expected only the parked blocker to remain, found " + ships.size() + " ships");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void disassemblyRestoresExactBlockCountAtSmallScale(GameTestHelper helper) {
        placeValidStructure(helper, WHEEL_POS);
        Map<BlockPos, BlockState> preAssembly = snapshotWorld(helper, structureRelPositions(WHEEL_POS));
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);

        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail("test precondition: expected assembly to succeed, got " + result.translationKey());
            return;
        }
        List<ShipEntity> ships = shipsNear(helper, wheelWorldPos);
        if (ships.size() != 1) {
            helper.fail("test precondition: expected exactly one spawned ShipEntity, got " + ships.size());
            return;
        }
        ShipEntity ship = ships.get(0);
        int blueprintCount = ship.getBlueprint().blockCount();
        if (blueprintCount != preAssembly.size()) {
            helper.fail("test precondition: expected a " + preAssembly.size() + "-block blueprint, got " + blueprintCount);
        }

        ship.disassemble();

        // Exact restore: every pre-assembly position holds its exact pre-assembly BlockState
        // again (position AND state, not just "some block") — AC-021's "exactly equal", plus
        // the count contract made explicit against the blueprint.
        int restored = 0;
        for (Map.Entry<BlockPos, BlockState> entry : preAssembly.entrySet()) {
            BlockState now = helper.getBlockState(entry.getKey());
            if (!now.equals(entry.getValue())) {
                helper.fail("disassembly did not restore " + entry.getValue() + " at "
                        + entry.getKey() + " — found " + now);
            }
            restored++;
        }
        if (restored != blueprintCount) {
            helper.fail("disassembly restored " + restored + " blocks, blueprint has " + blueprintCount);
        }
        if (!ship.isRemoved()) {
            helper.fail("disassembled ship entity was not discarded");
        }
        helper.succeed();
    }

    @GameTest(template = "sharkengine:gametest_large_empty")
    public void disassemblyRestoresExactBlockCountAtMaxScale(GameTestHelper helper) {
        BlockPos centerRel = new BlockPos(20, 3, 20);
        BlockPos centerAbs = helper.absolutePos(centerRel);

        // Exactly MAX_BLOCKS offsets: a 23×23 single-layer slab (529 candidates) ordered by
        // Manhattan distance, first 512 kept — max extent ±11 stays inside the 40×14×40 template.
        List<int[]> candidates = new ArrayList<>();
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -11; dz <= 11; dz++) {
                candidates.add(new int[]{dx, dz});
            }
        }
        candidates.sort(Comparator.comparingInt(o -> Math.abs(o[0]) + Math.abs(o[1])));
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>(ShipAssemblyService.MAX_BLOCKS);
        for (int i = 0; i < ShipAssemblyService.MAX_BLOCKS; i++) {
            int[] offset = candidates.get(i);
            blocks.add(new ShipBlueprint.ShipBlock(offset[0], 0, offset[1], plank));
        }
        ShipBlueprint blueprint = new ShipBlueprint(centerAbs, blocks);
        if (blueprint.blockCount() != ShipAssemblyService.MAX_BLOCKS) {
            helper.fail("test precondition: blueprint has " + blueprint.blockCount()
                    + " blocks, wanted MAX_BLOCKS=" + ShipAssemblyService.MAX_BLOCKS);
        }

        ShipEntity ship = new ShipEntity(ModEntities.SHIP, helper.getLevel());
        ship.setPos(centerAbs.getX() + 0.5, centerAbs.getY() + 0.5, centerAbs.getZ() + 0.5);
        ship.setBlueprint(blueprint);
        helper.getLevel().addFreshEntity(ship);

        ship.disassemble();

        int restored = 0;
        for (ShipBlueprint.ShipBlock block : blocks) {
            BlockPos rel = centerRel.offset(block.dx(), block.dy(), block.dz());
            if (helper.getBlockState(rel).is(Blocks.OAK_PLANKS)) {
                restored++;
            }
        }
        if (restored != ShipAssemblyService.MAX_BLOCKS) {
            helper.fail("max-scale disassembly restored " + restored + "/"
                    + ShipAssemblyService.MAX_BLOCKS + " blocks — partial restore");
        }
        if (!ship.isRemoved()) {
            helper.fail("disassembled ship entity was not discarded");
        }
        helper.succeed();
    }
}
