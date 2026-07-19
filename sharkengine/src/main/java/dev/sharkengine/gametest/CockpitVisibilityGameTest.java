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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REQ-007/AC-007 (T08, remediated after a review-required Watcher gate) falsifying-test contract
 * (test-plan {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-007 —
 * Cockpit visibility", sharpened test (b)): exercises the real production pipeline -- an actual
 * placed pilot seat block, a real {@code ServerLevel} block-state scan, {@code
 * ShipAssemblyService#tryAssemble}, and (only on the compliant path) a real spawned {@link
 * ShipEntity} -- to confirm assembly-time enforcement of AC-007's promise: a pilot seat that
 * would leave the pilot fully exposed above the hull is rejected explicitly, not merely logged.
 *
 * <p><b>T08 remediation (this pass):</b> the original T08 pass wired {@code
 * CockpitVisibility#isFullyExposedAboveHull} into exactly one place -- a server-console {@code
 * LOGGER.warn()} inside {@code ShipEntity#addPassenger} -- with nothing rejecting assembly,
 * repositioning the seat, or reaching the player/client. A ship with insufficient hull around the
 * pilot seat was exactly as non-compliant as before that pass, the only difference being an
 * invisible log line. Per the user's explicit decision (mirroring T06's {@code seatAnchorValid}
 * gate, not the higher-risk {@code getPassengerAttachmentPoint} alternative), enforcement now
 * lives in {@code ShipAssemblyService.StructureScan#cockpitVisibilityCompliant}: a non-compliant
 * structure fails {@code tryAssemble} explicitly (translation key {@code
 * message.sharkengine.assembly_fail_cockpit_visibility}) with zero world mutation. This means a
 * ship that successfully assembles automatically satisfies AC-007 -- non-compliant configurations
 * simply cannot be built.</p>
 *
 * <p><b>Counter-thesis this class exists to close</b> (test-plan, sharpened for the remediation):
 * a client-only cosmetic fix (e.g. a local render-layer trick that hides the player model on one
 * developer's own screen) would never touch {@code ShipAssemblyService}, and a hardcoded-{@code
 * true}/hardcoded-{@code false} production stub would pass a single directional assertion
 * trivially. {@link #tallHullWallConcealsMountedPilot} (adequate hull -> assembly succeeds) is
 * therefore paired with {@link #shortAdjacentBlockLeavesPilotExposedAssemblyRejectedExplicitly}
 * (insufficient hull -> assembly REJECTED, with an explicit message and a byte-for-byte
 * before/after world snapshot proving zero mutation) -- proving the gate is genuinely
 * height-sensitive AND actually blocks assembly, not a fixed answer or a log-only side effect.</p>
 */
public final class CockpitVisibilityGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /**
     * Wheel + 4 core neighbors (south = pilot seat, sitting exactly at the SOUTH-facing BUG's
     * deterministic front-of-wheel anchor, T06) + thruster + edge BUG. The pilot seat's only
     * horizontal neighbor column after this base layout is the wheel itself (north, 1 block
     * tall) plus whichever wall a caller adds south/east/west of it.
     *
     * <p>The thruster is deliberately placed above the WEST core neighbor, not above the wheel
     * itself: the wheel sits directly in the seat's own north-neighbor column, so a thruster
     * stacked there would silently turn that column into a 2-tall obstruction (wheel dy=0 +
     * thruster dy=1) regardless of any wall this test places elsewhere -- contaminating the
     * "short adjacent block" contrast case. The west core neighbor's column is not adjacent to
     * the seat's column at all (seat is south of the wheel; west-of-wheel is not west-of-seat),
     * so stacking the thruster there cannot affect the visibility computation either way.</p>
     */
    private static void placeBaseStructure(GameTestHelper helper) {
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), ModBlocks.PILOT_SEAT);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west().above(), ModBlocks.THRUSTER);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bug);
    }

    /** The pilot seat sits at {@code WHEEL_POS.south()} -- one block further south is adjacent. */
    private static BlockPos wallColumnBase() {
        return WHEEL_POS.south().south();
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

    /**
     * Every relative position {@link #placeBaseStructure} (plus a wall, when present) ever
     * touches -- used by the rejection test's before/after world-mutation snapshot.
     */
    private static BlockPos[] trackedPositions() {
        BlockPos wallBase = wallColumnBase();
        return new BlockPos[] {
                WHEEL_POS, WHEEL_POS.north(), WHEEL_POS.south(), WHEEL_POS.east(), WHEEL_POS.west(),
                WHEEL_POS.west().above(), WHEEL_POS.north().north(), wallBase, wallBase.above()
        };
    }

    /**
     * Assembles the base structure (plus whatever wall the caller already placed) via the real
     * production entry point, spawns and returns the {@link ShipEntity} together with the
     * mounted pilot -- or {@code null} with {@code helper.fail} already called if any
     * precondition failed.
     */
    private record AssembledShip(ShipEntity ship, ServerPlayer pilot) {}

    private static AssembledShip assembleAndGetShip(GameTestHelper helper) {
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);

        ShipAssemblyService.StructureScan preflight =
                ShipAssemblyService.scanStructure(helper.getLevel(), wheelWorldPos);
        if (!preflight.canAssemble()) {
            helper.fail("test precondition: expected canAssemble()=true, issues=" + preflight.issues());
            return null;
        }
        ShipBlueprint previewBlueprint = preflight.toBlueprint();
        long pilotAnchors = previewBlueprint.seatAnchors().stream()
                .filter(a -> a.role() == ShipBlueprint.SeatRole.PILOT)
                .count();
        if (pilotAnchors != 1) {
            helper.fail("test precondition: expected exactly one PILOT SeatAnchor, got " + pilotAnchors);
            return null;
        }

        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail("test precondition: expected assembly to succeed, got " + result.translationKey());
            return null;
        }

        List<ShipEntity> ships = helper.getLevel().getEntities(
                ModEntities.SHIP, new AABB(wheelWorldPos).inflate(8), e -> true);
        if (ships.size() != 1) {
            helper.fail("test precondition: expected exactly one spawned ShipEntity, got " + ships.size());
            return null;
        }
        if (pilot.getVehicle() != ships.get(0)) {
            helper.fail("test precondition: expected the pilot to be mounted on the spawned ship "
                    + "(assembly's own auto-mount) before checking cockpit visibility");
            return null;
        }
        return new AssembledShip(ships.get(0), pilot);
    }

    // ─── tall (2+ block) hull wall -> assembly succeeds, mounted pilot concealed ───────────

    @GameTest(template = EMPTY_STRUCTURE)
    public void tallHullWallConcealsMountedPilot(GameTestHelper helper) {
        placeBaseStructure(helper);
        // A genuinely tall (2-block) hull wall directly adjacent (south) of the pilot seat --
        // adequate hull, so REQ-007/AC-007's enforcement gate must let assembly through.
        BlockPos wallBase = wallColumnBase();
        helper.setBlock(wallBase, Blocks.OAK_PLANKS);
        helper.setBlock(wallBase.above(), Blocks.OAK_PLANKS);

        AssembledShip assembled = assembleAndGetShip(helper);
        if (assembled == null) {
            return; // precondition already failed via helper.fail above
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        double eyeHeight = pilot.getEyeHeight();
        boolean compliant = ship.isSeatVisibilityCompliant(ShipBlueprint.SeatRole.PILOT, eyeHeight);

        if (!compliant) {
            helper.fail("expected the mounted pilot's eye-height point to sit below the top face "
                    + "of the tallest adjacent (2-block) hull wall, but isSeatVisibilityCompliant "
                    + "reported non-compliant (fully exposed) for eyeHeight=" + eyeHeight);
            return;
        }

        helper.succeed();
    }

    // ─── contrast case: only a 1-block-tall adjacent surface -> assembly REJECTED ─────────

    /**
     * Deliberately omits the extra wall blocks {@link #tallHullWallConcealsMountedPilot} adds --
     * the pilot seat's only horizontal neighbor left is the wheel itself (1 block tall, same Y
     * level as everything else). Post-remediation, this must no longer assemble at all: {@code
     * ShipAssemblyService#tryAssemble} must reject explicitly with {@code
     * message.sharkengine.assembly_fail_cockpit_visibility}, and the world must show zero
     * mutation (no blocks removed, no {@link ShipEntity} spawned) -- same discipline as {@code
     * PilotSeatAnchorGameTest#occupiedFrontSlotFailsExplicitlyWithZeroSeatAnchorEntries} (T06)
     * and {@code CopilotSeatOccupancyGameTest}'s (T14) other assembly-rejection tests. This is
     * the sharpest assertion in this class: an implementation that reverted to T08's original
     * log-only behavior (assemble anyway, warn on the console) would pass a naive "assembly
     * eventually produces a ship" check but fail every assertion here.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void shortAdjacentBlockLeavesPilotExposedAssemblyRejectedExplicitly(GameTestHelper helper) {
        placeBaseStructure(helper);
        // No extra wall placed -- the seat's only adjacent block is the 1-block-tall wheel.

        ShipAssemblyService.StructureScan preflight =
                ShipAssemblyService.scanStructure(helper.getLevel(), helper.absolutePos(WHEEL_POS));
        if (!preflight.seatAnchorValid()) {
            helper.fail("test precondition: expected seatAnchorValid()=true (only the visibility "
                    + "condition should be under test here), got false. issues=" + preflight.issues());
            return;
        }
        if (preflight.cockpitVisibilityCompliant()) {
            helper.fail("test precondition: expected cockpitVisibilityCompliant()=false with only a "
                    + "1-block-tall adjacent surface -- otherwise this test cannot distinguish real "
                    + "enforcement from a hardcoded-compliant stub");
            return;
        }
        if (preflight.canAssemble()) {
            helper.fail("expected canAssemble()=false when the pilot seat leaves the pilot fully "
                    + "exposed above the hull (REQ-007/AC-007)");
            return;
        }

        BlockPos[] tracked = trackedPositions();
        Map<BlockPos, BlockState> before = snapshot(helper, tracked);
        int entitiesBefore = shipEntityCount(helper);

        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);

        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);

        if (result.isSuccess()) {
            helper.fail("expected assembly to be explicitly rejected when the pilot seat leaves the "
                    + "pilot fully exposed above the hull, but it succeeded -- REQ-007/AC-007 requires "
                    + "assembly-time enforcement, not merely a server-log warning after the fact");
            return;
        }
        if (!"message.sharkengine.assembly_fail_cockpit_visibility".equals(result.translationKey())) {
            helper.fail("expected the explicit cockpit-visibility rejection message, got "
                    + result.translationKey());
            return;
        }

        // Zero world mutation (same discipline as T06/T14's other assembly-rejection tests):
        // no block in the structure (or the tested wall column) may have changed, and no
        // ShipEntity may have been spawned, purely from a rejected assembly attempt.
        Map<BlockPos, BlockState> after = snapshot(helper, tracked);
        if (!after.equals(before)) {
            helper.fail("expected zero block-diff after a rejected assembly attempt, but the "
                    + "structure changed. before=" + before + " after=" + after);
            return;
        }
        if (pilot.getVehicle() != null) {
            helper.fail("expected the pilot to remain unmounted after a rejected assembly attempt, "
                    + "but they are riding " + pilot.getVehicle());
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
}
