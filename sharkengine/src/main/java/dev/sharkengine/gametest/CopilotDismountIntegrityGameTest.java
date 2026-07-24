package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.net.HelmInputC2SPayload;
import dev.sharkengine.net.ModNetworking;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * REQ-010/AC-010 (T10) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-010 — Passive
 * copilot behavior"): input-rejection while a copilot is mounted is already the discriminating
 * case covered by {@code PilotControlAuthorityGameTest#copilotInputRejectedDespiteBeingARider}
 * (REQ-008/T09). REQ-010's own distinct, not-yet-covered failure mode is
 * <b>dismount-triggered corruption</b>: a copilot exiting mid-flight, through a poorly-scoped
 * dismount handler, could accidentally clear the PILOT's seat reference too (e.g. a shared
 * "occupants" list cleared on any dismount), or leave the copilot seat's own internal state
 * dangling so it appears occupied forever afterward -- silently breaking REQ-011's re-entry
 * guarantee even though nobody is actually in the seat. A test that only re-checks "copilot
 * input has no effect" never exercises the dismount code path at all, so this class exists
 * purely to drive a REAL dismount (via {@link ServerPlayer#stopRiding()}, the same path a
 * player's normal sneak-to-exit takes -- {@code Entity#stopRiding} calling
 * {@code vehicle#removePassenger}) and observe its side effects.
 *
 * <p>Both methods put the ship genuinely mid-flight -- non-zero {@code getCurrentSpeed()} AND
 * measurably burned fuel (not merely "input is set" while the ship still sits still) -- via
 * {@link #driveIntoFlight}, which calls {@link ShipEntity#tick()} directly and repeatedly (the
 * same method the real server game loop calls every tick) with full forward input applied,
 * before the copilot ever dismounts. This matters because a dismount handler that happens to
 * only misbehave once the ship has real velocity/fuel-debt state (e.g. by touching fields a
 * stationary ship never populates) would not be caught by a dismount test run against a
 * freshly-assembled, still-stationary ship.</p>
 *
 * <p>{@link #midFlightDismountLeavesPilotUnaffected} covers assertions (a) and (b) of the
 * sharpened test: the pilot remains mounted and demonstrably retains full control authority
 * (a real {@link ModNetworking#handleHelmInput} call is proven to still land, not just a
 * riding-status check) after the copilot's dismount, and the ship's persisted NBT is
 * byte-identical before vs. after the dismount except the {@code Copilot} occupant tag itself,
 * which must immediately reflect the vacancy (not remain dangling until some other player's
 * next mount attempt happens to lazily heal it).</p>
 *
 * <p>{@link #vacatedSeatIsImmediatelyRemountable} covers assertion (c): a DIFFERENT player can
 * claim the just-vacated copilot seat immediately, within the same test run, directly feeding
 * REQ-011/T11's re-entry test as a regression guard per the tester's plan.</p>
 */
public final class CopilotDismountIntegrityGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /**
     * Ticks of full forward input applied via direct {@link ShipEntity#tick()} calls before a
     * dismount is exercised. {@code VehicleBalance.FUEL_CONSUMPTION_RATE} (0.25) means even the
     * slowest phase (1 nominal unit/sec, phase 1-2) accumulates a whole burned fuel unit within 4
     * twenty-tick consumption cycles (80 ticks); 200 ticks leaves a wide, phase-independent
     * margin so the mid-flight precondition never flakes on phase-transition timing.
     */
    private static final int FLIGHT_TICKS = 200;

    /**
     * Wheel + 4 core neighbors (south = pilot seat, sitting exactly at the SOUTH-facing BUG's
     * deterministic front-of-wheel anchor, T06) + thruster + edge BUG, plus a {@code
     * copilot_seat} block attached to the east core-neighbor plank -- identical layout to
     * {@code CopilotSeatOccupancyGameTest} (T07) and {@code PilotControlAuthorityGameTest} (T09)
     * so a genuine copilot rider can be mounted here too.
     */
    private static void placeStructureWithCopilotSeat(GameTestHelper helper) {
        helper.setBlock(WHEEL_POS, ModBlocks.STEERING_WHEEL);
        helper.setBlock(WHEEL_POS.north(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.south(), ModBlocks.PILOT_SEAT);
        helper.setBlock(WHEEL_POS.east(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.west(), Blocks.OAK_PLANKS);
        helper.setBlock(WHEEL_POS.east().east(), ModBlocks.COPILOT_SEAT);
        helper.setBlock(WHEEL_POS.above(), ModBlocks.THRUSTER);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(WHEEL_POS.north().north(), bug);
    }

    /** Pairs the spawned ship with the {@link ServerPlayer} assembly assigned as its pilot. */
    private record AssembledShip(ShipEntity ship, ServerPlayer pilot) {}

    /**
     * Places the structure, assembles for real via the same production entry point every other
     * seat GameTest uses, and returns the spawned {@link ShipEntity} (with its pilot already
     * mounted, done by {@code tryAssemble} itself) paired with that pilot -- or {@code null} if a
     * precondition failed (in which case {@code helper.fail} was already called and the caller
     * must return immediately).
     */
    private static AssembledShip assembleShipWithMountedPilotAndCopilotSeat(GameTestHelper helper) {
        placeStructureWithCopilotSeat(helper);
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);

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
        return new AssembledShip(ships.get(0), pilot);
    }

    /**
     * Drives {@code ship} into a genuinely non-stationary state -- full forward input applied and
     * then {@link ShipEntity#tick()} called directly and repeatedly (the exact method the real
     * server game loop invokes every tick; {@code ShipEntity} is unanchored by default post-
     * assembly, see {@code ShipAssemblyService#tryAssemble}) -- rather than merely setting input
     * and leaving the ship stationary. Fails the test via {@code helper.fail} and returns
     * {@code false} if the ship isn't demonstrably moving (non-zero speed) AND demonstrably
     * burning fuel (fuel below max) after {@link #FLIGHT_TICKS} ticks.
     */
    private static boolean driveIntoFlight(GameTestHelper helper, ShipEntity ship) {
        ship.setInputForward(1.0f);
        for (int i = 0; i < FLIGHT_TICKS; i++) {
            ship.tick();
        }
        if (ship.getCurrentSpeed() <= 0.0f) {
            helper.fail("test precondition: expected non-zero speed after " + FLIGHT_TICKS
                    + " ticks of forward input (ship must be genuinely mid-flight, not "
                    + "stationary), got " + ship.getCurrentSpeed());
            return false;
        }
        if (ship.getFuelLevel() >= 100) {
            helper.fail("test precondition: expected measurably burned fuel after " + FLIGHT_TICKS
                    + " ticks of forward input (ship must be genuinely mid-flight, not "
                    + "stationary), fuel is still " + ship.getFuelLevel());
            return false;
        }
        return true;
    }

    /** Mounts a fresh player into the copilot seat and confirms it actually landed. */
    private static ServerPlayer mountCopilotOrFail(GameTestHelper helper, ShipEntity ship, String label) {
        ServerPlayer copilot = helper.makeMockServerPlayerInLevel();
        copilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult mountResult = ship.interact(copilot, InteractionHand.MAIN_HAND);
        if (mountResult != InteractionResult.CONSUME || !ship.isCopilot(copilot) || copilot.getVehicle() != ship) {
            helper.fail("test precondition: expected " + label + " to actually be mounted as "
                    + "copilot before flight, got interact()=" + mountResult
                    + " isCopilot=" + ship.isCopilot(copilot) + " vehicle=" + copilot.getVehicle());
            return null;
        }
        return copilot;
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void midFlightDismountLeavesPilotUnaffected(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        ServerPlayer copilot = mountCopilotOrFail(helper, ship, "the copilot");
        if (copilot == null) {
            return;
        }

        if (!driveIntoFlight(helper, ship)) {
            return;
        }

        CompoundTag before = ship.saveWithoutId(new CompoundTag());
        Vec3 velocityBefore = ship.getDeltaMovement();
        if (!before.hasUUID("Copilot") || !before.getUUID("Copilot").equals(copilot.getUUID())) {
            helper.fail("test precondition: expected pre-dismount NBT to record the copilot's UUID, got "
                    + (before.hasUUID("Copilot") ? before.getUUID("Copilot") : "absent"));
            return;
        }

        // ━━━ THE ACTUAL DISMOUNT ━━━ a normal player exit (the same path a real sneak-to-dismount
        // takes: Entity#stopRiding -> vehicle#removePassenger), not a hand-rolled shortcut around
        // ShipEntity's own logic.
        copilot.stopRiding();

        if (copilot.getVehicle() == ship || ship.getPassengers().contains(copilot)) {
            helper.fail("test precondition: expected the copilot to have actually dismounted");
            return;
        }

        // ─── (a) pilot remains mounted, retains full control authority, unaffected ───
        if (pilot.getVehicle() != ship || !ship.isPilot(pilot)) {
            helper.fail("expected the pilot to remain mounted and tracked as pilot after the "
                    + "copilot's mid-flight dismount, vehicle=" + pilot.getVehicle()
                    + " isPilot=" + ship.isPilot(pilot));
            return;
        }
        HelmInputC2SPayload controlInput = new HelmInputC2SPayload(0.7f, 0.3f, 1.0f);
        ModNetworking.handleHelmInput(pilot, controlInput);
        if (ship.getInputForward() != controlInput.forward()
                || ship.getSyncedTurn() != controlInput.turn()
                || ship.getInputVertical() != controlInput.throttle()) {
            helper.fail("expected the pilot's control input to still be applied after the "
                    + "copilot's mid-flight dismount (pilot must retain full control authority), "
                    + "expected forward=" + controlInput.forward() + " turn=" + controlInput.turn()
                    + " throttle=" + controlInput.throttle() + " got forward=" + ship.getInputForward()
                    + " turn=" + ship.getSyncedTurn() + " vertical=" + ship.getInputVertical());
            return;
        }

        // ─── (b) fuel/velocity/vehicle NBT identical before/after except the copilot slot ───
        CompoundTag after = ship.saveWithoutId(new CompoundTag());
        Vec3 velocityAfter = ship.getDeltaMovement();
        if (!velocityBefore.equals(velocityAfter)) {
            helper.fail("expected velocity to be unaffected by the copilot's dismount, before="
                    + velocityBefore + " after=" + velocityAfter);
            return;
        }
        CompoundTag beforeMinusCopilot = before.copy();
        CompoundTag afterMinusCopilot = after.copy();
        beforeMinusCopilot.remove("Copilot");
        afterMinusCopilot.remove("Copilot");
        if (!beforeMinusCopilot.equals(afterMinusCopilot)) {
            helper.fail("expected vehicle NBT to be identical before/after the copilot's dismount "
                    + "except the Copilot occupant tag, before=" + beforeMinusCopilot
                    + " after=" + afterMinusCopilot);
            return;
        }
        // The sharpest assertion this test exists for (test-plan counter-thesis): the copilot
        // slot must reflect the vacancy IMMEDIATELY, not remain a dangling stale reference that
        // only gets lazily healed whenever some other player's next mount attempt happens to
        // notice it -- that dangling window is exactly what would silently break REQ-011's
        // "the seat is free, right now" promise even though nobody is actually in it.
        if (after.hasUUID("Copilot") || ship.getCopilot() != null) {
            helper.fail("expected the copilot seat to be recorded as vacated immediately after "
                    + "dismount (not left dangling until a future mount attempt lazily heals it), "
                    + "got NBT Copilot=" + (after.hasUUID("Copilot") ? after.getUUID("Copilot") : "absent")
                    + " ship.getCopilot()=" + ship.getCopilot());
            return;
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void vacatedSeatIsImmediatelyRemountable(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        ServerPlayer copilotA = mountCopilotOrFail(helper, ship, "copilot A");
        if (copilotA == null) {
            return;
        }

        if (!driveIntoFlight(helper, ship)) {
            return;
        }

        copilotA.stopRiding();
        if (copilotA.getVehicle() == ship || ship.getPassengers().contains(copilotA)) {
            helper.fail("test precondition: expected copilot A to have actually dismounted");
            return;
        }

        // ─── (c) the vacated seat is immediately re-mountable by a DIFFERENT player ───
        ServerPlayer copilotB = helper.makeMockServerPlayerInLevel();
        copilotB.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult mountResultB = ship.interact(copilotB, InteractionHand.MAIN_HAND);

        if (mountResultB != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME (mounted) for a different player on the just-vacated "
                    + "copilot seat, got " + mountResultB);
            return;
        }
        if (!ship.isCopilot(copilotB) || copilotB.getVehicle() != ship) {
            helper.fail("expected the new player to be tracked and actually mounted as copilot "
                    + "immediately after A vacated the seat mid-flight, isCopilot="
                    + ship.isCopilot(copilotB) + " vehicle=" + copilotB.getVehicle());
            return;
        }
        if (ship.isCopilot(copilotA)) {
            helper.fail("expected player A to no longer be tracked as copilot once B took the "
                    + "vacated seat");
            return;
        }
        if (ship.getPassengers().size() != 2
                || !ship.getPassengers().contains(pilot)
                || !ship.getPassengers().contains(copilotB)) {
            helper.fail("expected exactly pilot + the new copilot as tracked passengers after the "
                    + "seat swap, got " + ship.getPassengers());
            return;
        }

        helper.succeed();
    }
}
