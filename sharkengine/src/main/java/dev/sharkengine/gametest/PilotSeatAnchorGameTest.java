package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import dev.sharkengine.ship.ShipTransform;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REQ-006/AC-006 (T06) falsifying-test contract: the pilot seat's SeatAnchor is
 * deterministically resolved as the single block directly in front of the BUG's resolved
 * facing. Occupying that exact position with a non-seat/invalid block must fail assembly
 * explicitly, leaving the blueprint with ZERO SeatAnchor entries (never a silently-chosen
 * alternate position); with the front slot free and valid, the anchor offset must be
 * exactly correct across all 4 BUG facings (N/E/S/W) and survive a save/load NBT round-trip.
 *
 * <p><b>Counter-thesis this class exists to close</b> (per the test-plan's REQ-006 section,
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md} lines 184-208): the
 * riskiest false positive here is invisible from the happy path alone — an implementation
 * with a hidden fallback branch that searches for ANY nearby valid seat position when the
 * front slot is occupied would still pass a naive "seat anchor got set to SOME position"
 * test whenever the front slot happens to be free, because that fallback branch would simply
 * never be exercised by such a test. {@link
 * #occupiedFrontSlotFailsExplicitlyWithZeroSeatAnchorEntries} deliberately places a valid,
 * count-satisfying (REQ-005: exactly one) pilot seat AWAY from the front slot, with the front
 * slot itself occupied by an ordinary ship_eligible block — only a test built this way can
 * distinguish "deterministic front-slot resolution" from "fallback search that happens to
 * land elsewhere." The production implementation (see {@code
 * ShipAssemblyService#frontOffset}/{@code #scanStructure}) deliberately contains no such
 * fallback: this test proves it by construction, not by inspection.</p>
 *
 * <p>Pure rotation math for the per-facing offset (the SOUTH-built-offset-rotated-by-yaw
 * relationship) is covered exhaustively by {@code ShipTransformTest}'s {@code
 * SeatAnchorOffsetTests} nested class — this class instead exercises the full production
 * pipeline that math feeds into: a real {@code ServerLevel} block-state lookup at a computed
 * world position, the actual {@code ShipAssemblyService.tryAssemble} entry point, a real
 * spawned {@code ShipEntity}, and a real NBT save/load round-trip.</p>
 */
public final class PilotSeatAnchorGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    private static ShipAssemblyService.StructureScan scan(GameTestHelper helper) {
        return ShipAssemblyService.scanStructure(helper.getLevel(), helper.absolutePos(WHEEL_POS));
    }

    private static int shipEntityCount(GameTestHelper helper) {
        AABB region = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(64);
        return helper.getLevel().getEntities(ModEntities.SHIP, region, e -> true).size();
    }

    private static Map<BlockPos, BlockState> snapshot(GameTestHelper helper, BlockPos... relativePositions) {
        Map<BlockPos, BlockState> snap = new LinkedHashMap<>();
        for (BlockPos relative : relativePositions) {
            snap.put(relative, helper.getBlockState(relative));
        }
        return snap;
    }

    // ─── occupied-front-slot failure (the counter-thesis case) ────────────────────────────

    /**
     * Base structure with the pilot seat placed AWAY from the front slot (west core neighbor
     * doubling as the seat), and the actual front slot (south, matching the BUG's SOUTH facing
     * below) occupied by an ordinary plank — REQ-005's "exactly one pilot seat" count
     * invariant is satisfied, but REQ-006's anchor-POSITION invariant is not.
     */
    private static void placeSeatAwayFromFrontSlot(GameTestHelper helper) {
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        // Front slot for a SOUTH-facing BUG (see bug placement below) — deliberately an
        // ordinary block, NOT the pilot seat.
        helper.setBlock(WHEEL_POS.south(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        // The one real pilot seat — deliberately NOT at the front slot.
        helper.setBlock(WHEEL_POS.west(), ModBlocks.PILOT_SEAT);
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
        BlockState bugState = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bugState);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void occupiedFrontSlotFailsExplicitlyWithZeroSeatAnchorEntries(GameTestHelper helper) {
        placeSeatAwayFromFrontSlot(helper);

        ShipAssemblyService.StructureScan preflight = scan(helper);
        if (preflight.pilotSeatCount() != 1) {
            helper.fail("test precondition: expected exactly one pilot seat present (REQ-005 "
                    + "satisfied), got " + preflight.pilotSeatCount());
            return;
        }
        if (preflight.seatAnchorValid()) {
            helper.fail("expected seatAnchorValid()=false: the one pilot seat is NOT at the "
                    + "front-of-wheel slot");
            return;
        }
        if (preflight.canAssemble()) {
            helper.fail("expected canAssemble()=false when the front slot is occupied by a non-seat block");
            return;
        }

        // The sharpest assertion this whole class exists for: the raw scan's blueprint (what a
        // fallback-search implementation would have populated with SOME position) must show
        // ZERO SeatAnchor entries — not the misplaced west seat, not any other position.
        ShipBlueprint previewBlueprint = preflight.toBlueprint();
        if (!previewBlueprint.seatAnchors().isEmpty()) {
            helper.fail("expected ZERO SeatAnchor entries when the front slot is invalid — a "
                    + "fallback search would have silently populated one anyway; got "
                    + previewBlueprint.seatAnchors());
            return;
        }

        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        BlockPos[] tracked = {
                WHEEL_POS, WHEEL_POS.north(), WHEEL_POS.south(), WHEEL_POS.east(),
                WHEEL_POS.west(), WHEEL_POS.above(), WHEEL_POS.north().north()
        };
        Map<BlockPos, BlockState> before = snapshot(helper, tracked);
        int entitiesBefore = shipEntityCount(helper);

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);

        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);

        if (result.isSuccess()) {
            helper.fail("expected assembly to be explicitly rejected when the front slot holds a "
                    + "non-seat block, but it succeeded");
            return;
        }
        if (!"message.sharkengine.assembly_fail_seat_anchor".equals(result.translationKey())) {
            helper.fail("expected the explicit seat-anchor rejection message, got " + result.translationKey());
            return;
        }

        Map<BlockPos, BlockState> after = snapshot(helper, tracked);
        if (!after.equals(before)) {
            helper.fail("expected zero block-diff after a rejected assembly attempt, but the "
                    + "structure changed. before=" + before + " after=" + after);
            return;
        }
        int entitiesAfter = shipEntityCount(helper);
        if (entitiesAfter != entitiesBefore) {
            helper.fail("expected zero entity-count diff after a rejected assembly attempt, before="
                    + entitiesBefore + " after=" + entitiesAfter);
            return;
        }
        helper.succeed();
    }

    // ─── all 4 facings: front slot free/valid -> correct offset, full assembly, save/load ──

    private record FacingCase(Direction facing, int expectedDx, int expectedDz) {}

    /**
     * Places wheel + 4 core neighbors + thruster + edge BUG (facing {@code facing}), with the
     * ONE core-neighbor plank matching {@code facing}'s own front-of-wheel side replaced by the
     * pilot seat — the seat doubles as that core neighbor, same as any other ship_eligible
     * block could. The BUG itself always sits at {@code WHEEL_POS.north().north()} (its own
     * position only needs to be on the structure's outer edge — independent of its FACING
     * property), matching the pattern already established by
     * {@code BlueprintPersistenceGameTest#assemblyPlumbsWestFacingBugYawIntoBlueprint}.
     */
    private static void placeStructureWithSeatAtFront(GameTestHelper helper, Direction facing) {
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), facing == Direction.NORTH ? ModBlocks.PILOT_SEAT : Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), facing == Direction.SOUTH ? ModBlocks.PILOT_SEAT : Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.east(), facing == Direction.EAST ? ModBlocks.PILOT_SEAT : Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), facing == Direction.WEST ? ModBlocks.PILOT_SEAT : Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
        BlockState bugState = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, facing);
        helper.setBlock(WHEEL_POS.north().north(), bugState);
    }

    private static void assertFacingResolvesToFrontOffset(GameTestHelper helper, FacingCase testCase) {
        placeStructureWithSeatAtFront(helper, testCase.facing());

        ShipAssemblyService.StructureScan preflight = scan(helper);
        if (!preflight.seatAnchorValid()) {
            helper.fail(testCase.facing() + ": expected seatAnchorValid()=true with the seat "
                    + "correctly placed at the front-of-wheel slot");
            return;
        }
        if (!preflight.canAssemble()) {
            helper.fail(testCase.facing() + ": expected canAssemble()=true (coreNeighbors="
                    + preflight.coreNeighbors() + ", bugCount=" + preflight.bugCount()
                    + ", bugOnEdge=" + preflight.bugOnEdge() + ")");
            return;
        }

        ShipBlueprint previewBlueprint = preflight.toBlueprint();
        if (previewBlueprint.seatAnchors().size() != 1) {
            helper.fail(testCase.facing() + ": expected exactly one SeatAnchor entry, got "
                    + previewBlueprint.seatAnchors().size());
            return;
        }
        ShipBlueprint.SeatAnchor anchor = previewBlueprint.seatAnchors().get(0);
        if (anchor.dx() != testCase.expectedDx() || anchor.dy() != 0 || anchor.dz() != testCase.expectedDz()) {
            helper.fail(testCase.facing() + ": expected SeatAnchor offset (" + testCase.expectedDx()
                    + ",0," + testCase.expectedDz() + "), got (" + anchor.dx() + "," + anchor.dy()
                    + "," + anchor.dz() + ")");
            return;
        }

        // Full assembly flow (not just a read-only scan) -- proves the anchor is carried all the
        // way through a real ShipEntity spawn, matching AC-006's explicit "assembliert, gedreht,
        // gespeichert und geladen" (assembled, rotated, saved, loaded) contract.
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail(testCase.facing() + ": expected assembly to succeed, got " + result.translationKey());
            return;
        }

        List<ShipEntity> ships = helper.getLevel().getEntities(
                ModEntities.SHIP, new AABB(wheelWorldPos).inflate(8), e -> true);
        if (ships.size() != 1) {
            helper.fail(testCase.facing() + ": expected exactly one spawned ShipEntity, got " + ships.size());
            return;
        }
        ShipBlueprint spawnedBlueprint = ships.get(0).getBlueprint();
        if (spawnedBlueprint.seatAnchors().size() != 1) {
            helper.fail(testCase.facing() + ": expected the spawned entity's own blueprint to carry "
                    + "exactly one SeatAnchor, got " + spawnedBlueprint.seatAnchors().size());
            return;
        }

        // Save/load round-trip (AC-006's explicit "gespeichert und geladen"): the anchor must
        // survive a real toNbt()/fromNbt() cycle, not just live in memory on the
        // freshly-spawned entity.
        CompoundTag nbt = spawnedBlueprint.toNbt();
        ShipBlueprint reloaded = ShipBlueprint.fromNbt(nbt, helper.getLevel().registryAccess());
        if (reloaded.seatAnchors().size() != 1) {
            helper.fail(testCase.facing() + ": expected the SeatAnchor to survive a save/load "
                    + "round-trip, got " + reloaded.seatAnchors().size() + " entries after reload");
            return;
        }
        ShipBlueprint.SeatAnchor reloadedAnchor = reloaded.seatAnchors().get(0);
        if (reloadedAnchor.dx() != testCase.expectedDx() || reloadedAnchor.dy() != 0
                || reloadedAnchor.dz() != testCase.expectedDz()) {
            helper.fail(testCase.facing() + ": expected the reloaded SeatAnchor offset to still be ("
                    + testCase.expectedDx() + ",0," + testCase.expectedDz() + "), got ("
                    + reloadedAnchor.dx() + "," + reloadedAnchor.dy() + "," + reloadedAnchor.dz() + ")");
            return;
        }

        // ShipTransform.rotateOffset() wiring sanity (AIR-010 single rotation authority; the
        // exhaustive per-facing math itself is covered by ShipTransformTest's
        // SeatAnchorOffsetTests): at the moment of assembly, effectiveYaw is always 0 (entity
        // yaw == assembly yaw, AIR-015), so rotating the anchor by that effective yaw must be
        // the identity -- proving this class's offsets and ShipTransform's rotation authority
        // agree, not a second/divergent computation.
        float effectiveYaw = ShipTransform.effectiveYaw(ships.get(0).getYRot(), spawnedBlueprint.assemblyYaw());
        double[] rotated = ShipTransform.rotateOffset(anchor.dx(), anchor.dz(), effectiveYaw);
        if (Math.abs(rotated[0] - anchor.dx()) > 1e-6 || Math.abs(rotated[1] - anchor.dz()) > 1e-6) {
            helper.fail(testCase.facing() + ": expected rotateOffset() at rest (effectiveYaw=0) to "
                    + "be the identity, got (" + rotated[0] + "," + rotated[1] + ")");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void southFacingSeatAnchorResolvesToFrontOffset(GameTestHelper helper) {
        assertFacingResolvesToFrontOffset(helper, new FacingCase(Direction.SOUTH, 0, 1));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void westFacingSeatAnchorResolvesToFrontOffset(GameTestHelper helper) {
        assertFacingResolvesToFrontOffset(helper, new FacingCase(Direction.WEST, -1, 0));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void northFacingSeatAnchorResolvesToFrontOffset(GameTestHelper helper) {
        assertFacingResolvesToFrontOffset(helper, new FacingCase(Direction.NORTH, 0, -1));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void eastFacingSeatAnchorResolvesToFrontOffset(GameTestHelper helper) {
        assertFacingResolvesToFrontOffset(helper, new FacingCase(Direction.EAST, 1, 0));
    }
}
