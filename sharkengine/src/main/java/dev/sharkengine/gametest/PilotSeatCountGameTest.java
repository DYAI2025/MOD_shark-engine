package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.ShipAssemblyService;
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
 * REQ-005/AC-005 (T05) three-way partition GameTest: assembly requires EXACTLY one
 * {@code PILOT_SEAT}-role part — zero and more-than-one are both rejected explicitly,
 * with the world left unchanged; exactly one succeeds.
 *
 * <p>Per the test-plan's counter-thesis for REQ-005 (see
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}'s REQ-005
 * section), the sharpest false positive here is an off-by-one "found the first pilot
 * seat, stop looking" counting bug that never checks for a SECOND seat elsewhere in
 * the structure. {@link #twoSeatsRejectedEvenWhenFarApart} deliberately places its two
 * pilot seats far apart — one directly south of the wheel (REQ-006's front-of-wheel
 * position for this class's SOUTH-facing BUG), the other at the end of an
 * L-shaped plank corridor (east, then south) — not adjacent, specifically to defeat a
 * scan that only checks immediate neighbors for a duplicate rather than tallying every
 * matching part in the structure (see {@code ShipPartAnalyzer#analyze}, which sums
 * every resolved part with no early-break short-circuit).</p>
 *
 * <p><b>GameTest world-bounds note:</b> the {@code EMPTY_STRUCTURE} template
 * ({@code fabric-gametest-api-v1:empty}) is a fixed 8x8x8 pocket of air (local
 * coordinates 0..7 in every axis) carved into the surrounding GameTest world, which is
 * NOT air outside that pocket. Every block this test places (and every block whose
 * NEIGHBORS get inspected by {@link ShipAssemblyService}'s BFS scan) is therefore kept
 * within local coordinates 1..6 in every axis — one cell clear of the 0/7 boundary — so
 * that even a placed block's outward neighbor lookup (coordinate 0-1 or 7+1) never steps
 * outside the guaranteed-air pocket into real, non-{@code ship_eligible} terrain (which
 * would otherwise spuriously trip the "invalid attachments" check, not the pilot-seat
 * check this test targets).</p>
 *
 * <p>All three methods exercise {@link ShipAssemblyService#tryAssemble} directly (the
 * production entry point the seat-count validation is wired into), not just {@link
 * ShipAssemblyService#scanStructure}, so the negative cases also prove "world
 * unchanged" (no blocks removed, no {@code ShipEntity} spawned) — not merely that
 * {@code canAssemble()} returns false on a read-only scan.</p>
 */
public final class PilotSeatCountGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /**
     * L-shaped corridor beyond the existing east core-neighbor plank: two more blocks
     * east (to local x=6, the safe margin's limit), then three blocks south (to local
     * z=6) — see the class javadoc's "GameTest world-bounds note" for why it stops at
     * 6, not 7.
     */
    private static final BlockPos[] FAR_CORRIDOR = {
            WHEEL_POS.offset(2, 0, 0),  // (5,1,3)
            WHEEL_POS.offset(3, 0, 0),  // (6,1,3)
            WHEEL_POS.offset(3, 0, 1),  // (6,1,4)
            WHEEL_POS.offset(3, 0, 2),  // (6,1,5)
    };

    /** Far seat: at the end of {@link #FAR_CORRIDOR}, still within the safe 1..6 margin. */
    private static final BlockPos FAR_SEAT_POS = WHEEL_POS.offset(3, 0, 3); // (6,1,6)

    /** Wheel + 4 core-neighbor planks + thruster (above) + edge BUG — no seat yet. */
    private static void placeBaseStructure(GameTestHelper helper) {
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
        BlockState bugState = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bugState);
    }

    /**
     * Close seat: exactly the front-of-wheel position for the base structure's SOUTH-facing
     * BUG (REQ-006) -- {@code oneSeatAccepted} needs the single seat to actually be here for
     * assembly to succeed; it overwrites the south core-neighbor plank {@link
     * #placeBaseStructure} already set, same as any other ship_eligible block could.
     * {@code twoSeatsRejectedEvenWhenFarApart} also reuses this position for its "close" seat
     * -- that test expects rejection regardless of position (MULTI_PILOT_SEAT fires before
     * REQ-006's anchor check is ever reached), so the exact position doesn't affect its outcome.
     */
    private static BlockPos closeSeatPos() {
        return WHEEL_POS.south();
    }

    /** Lays {@link #FAR_CORRIDOR} and places a pilot seat at {@link #FAR_SEAT_POS}. */
    private static void placeFarSeat(GameTestHelper helper) {
        for (BlockPos corridorPos : FAR_CORRIDOR) {
            helper.setBlock(corridorPos, Blocks.OAK_PLANKS);
        }
        helper.setBlock(FAR_SEAT_POS, ModBlocks.PILOT_SEAT);
    }

    private static ShipAssemblyService.StructureScan scan(GameTestHelper helper) {
        return ShipAssemblyService.scanStructure(helper.getLevel(), helper.absolutePos(WHEEL_POS));
    }

    private static int shipEntityCount(GameTestHelper helper) {
        AABB region = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(64);
        return helper.getLevel().getEntities(ModEntities.SHIP, region, e -> true).size();
    }

    /** Snapshots every position this test family might touch, relative to {@code WHEEL_POS}. */
    private static Map<BlockPos, BlockState> snapshot(GameTestHelper helper, BlockPos... relativePositions) {
        Map<BlockPos, BlockState> snap = new LinkedHashMap<>();
        for (BlockPos relative : relativePositions) {
            snap.put(relative, helper.getBlockState(relative));
        }
        return snap;
    }

    private static void assertWorldUnchanged(GameTestHelper helper, Map<BlockPos, BlockState> before,
                                              int entitiesBefore, BlockPos... relativePositions) {
        Map<BlockPos, BlockState> after = snapshot(helper, relativePositions);
        if (!after.equals(before)) {
            helper.fail("expected zero block-diff after a rejected assembly attempt, but the "
                    + "structure changed. before=" + before + " after=" + after);
            return;
        }
        int entitiesAfter = shipEntityCount(helper);
        if (entitiesAfter != entitiesBefore) {
            helper.fail("expected zero entity-count diff after a rejected assembly attempt, "
                    + "before=" + entitiesBefore + " after=" + entitiesAfter);
        }
    }

    private static BlockPos[] baseStructurePositions() {
        return new BlockPos[] {
                WHEEL_POS, WHEEL_POS.north(), WHEEL_POS.south(), WHEEL_POS.east(),
                WHEEL_POS.west(), WHEEL_POS.above(), WHEEL_POS.north().north()
        };
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void zeroSeatsRejected(GameTestHelper helper) {
        placeBaseStructure(helper);
        // Deliberately no pilot seat placed at all — everything else is a valid structure.

        ShipAssemblyService.StructureScan preflight = scan(helper);
        if (preflight.pilotSeatCount() != 0) {
            helper.fail("expected pilotSeatCount()=0 with no seat placed, got " + preflight.pilotSeatCount());
            return;
        }
        if (preflight.canAssemble()) {
            helper.fail("expected canAssemble()=false with zero pilot seats, but the scan accepted it");
            return;
        }

        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        BlockPos[] tracked = baseStructurePositions();
        Map<BlockPos, BlockState> before = snapshot(helper, tracked);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);

        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);

        if (result.isSuccess()) {
            helper.fail("expected assembly to be explicitly rejected with zero pilot seats, but it succeeded");
            return;
        }
        if (!"message.sharkengine.assembly_fail_no_pilot_seat".equals(result.translationKey())) {
            helper.fail("expected the explicit no-pilot-seat rejection message, got " + result.translationKey());
            return;
        }
        assertWorldUnchanged(helper, before, entitiesBefore, tracked);
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void oneSeatAccepted(GameTestHelper helper) {
        placeBaseStructure(helper);
        BlockPos seatPos = closeSeatPos();
        helper.setBlock(seatPos, ModBlocks.PILOT_SEAT);

        ShipAssemblyService.StructureScan preflight = scan(helper);
        if (preflight.pilotSeatCount() != 1) {
            helper.fail("expected pilotSeatCount()=1 with exactly one seat placed, got "
                    + preflight.pilotSeatCount());
            return;
        }
        if (!preflight.canAssemble()) {
            helper.fail("expected canAssemble()=true with exactly one pilot seat and every other "
                    + "condition met (blocks=" + preflight.blockCount()
                    + ", contactPoints=" + preflight.contactPoints()
                    + ", hasThruster=" + preflight.hasThruster()
                    + ", coreNeighbors=" + preflight.coreNeighbors()
                    + ", bugCount=" + preflight.bugCount()
                    + ", invalidAttachments=" + preflight.invalidAttachments().size() + ")");
            return;
        }

        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);

        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);

        if (!result.isSuccess()) {
            helper.fail("expected assembly to succeed with exactly one pilot seat, got "
                    + result.translationKey());
            return;
        }
        int entitiesAfter = shipEntityCount(helper);
        if (entitiesAfter != entitiesBefore + 1) {
            helper.fail("expected exactly one new ShipEntity after a successful assembly, before="
                    + entitiesBefore + " after=" + entitiesAfter);
            return;
        }
        if (pilot.getVehicle() == null) {
            helper.fail("expected the requesting player to be mounted as pilot after a successful assembly");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void twoSeatsRejectedEvenWhenFarApart(GameTestHelper helper) {
        placeBaseStructure(helper);
        BlockPos closeSeat = closeSeatPos();
        helper.setBlock(closeSeat, ModBlocks.PILOT_SEAT);
        placeFarSeat(helper);

        ShipAssemblyService.StructureScan preflight = scan(helper);
        if (preflight.pilotSeatCount() != 2) {
            helper.fail("expected pilotSeatCount()=2 with two seats placed far apart, got "
                    + preflight.pilotSeatCount() + " -- a scan that only checks immediate "
                    + "duplicates would silently under-count this");
            return;
        }
        if (preflight.canAssemble()) {
            helper.fail("expected canAssemble()=false with two pilot seats, but the scan accepted it "
                    + "(invalidAttachments=" + preflight.invalidAttachments().size() + ")");
            return;
        }

        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        BlockPos[] tracked = new BlockPos[baseStructurePositions().length + 1 + FAR_CORRIDOR.length + 1];
        int idx = 0;
        for (BlockPos p : baseStructurePositions()) {
            tracked[idx++] = p;
        }
        tracked[idx++] = closeSeat;
        for (BlockPos p : FAR_CORRIDOR) {
            tracked[idx++] = p;
        }
        tracked[idx] = FAR_SEAT_POS;

        Map<BlockPos, BlockState> before = snapshot(helper, tracked);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);

        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);

        if (result.isSuccess()) {
            helper.fail("expected assembly to be explicitly rejected with two pilot seats "
                    + "placed far apart, but it succeeded");
            return;
        }
        if (!"message.sharkengine.assembly_fail_multi_pilot_seat".equals(result.translationKey())) {
            helper.fail("expected the explicit multi-pilot-seat rejection message, got "
                    + result.translationKey());
            return;
        }
        if (!Integer.valueOf(2).equals(result.arg())) {
            helper.fail("expected the rejection message arg to carry the actual seat count (2), got "
                    + result.arg());
            return;
        }
        assertWorldUnchanged(helper, before, entitiesBefore, tracked);
        helper.succeed();
    }
}
