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
 * REQ-008/AC-008 (T09) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-008 — Pilot control
 * authority"): the load-bearing false positive named there is an authorization check that tests
 * "is this player riding the ship at all" instead of "is this player *specifically* the pilot, not
 * the copilot" -- since the copilot IS a rider (T07), a test suite that only checks a *non-rider*
 * sending control input would never catch a guard that conflates "is riding" with "is the pilot".
 *
 * <p>This class exercises {@link ModNetworking#handleHelmInput}, the exact production
 * authorization entry point {@code ModNetworking}'s registered C2S {@code HelmInputC2SPayload}
 * receiver calls (extracted verbatim from that receiver's lambda body purely so it can be invoked
 * directly here without a live network connection -- zero behavior change, same pattern {@code
 * BuildSessionGate#tryAssemble} established for REQ-003's C2S assemble handler, see {@code
 * BuildSessionAuthorizationGameTest}'s javadoc for the precedent).</p>
 *
 * <p><b>Investigation finding (read before assuming a fix is needed):</b> {@code
 * ModNetworking}'s handler already gated helm input with {@code if (!ship.isPilot(sp)) return;}
 * (not a broader "is riding" check), and {@link ShipEntity#isPilot} and {@link
 * ShipEntity#isCopilot} are backed by two independent UUID fields ({@code pilot}, {@code
 * copilot}) rather than a shared "is a rider" flag -- so the pilot/copilot discrimination this
 * test proves already existed correctly in production code before this task. The only production
 * change made for T09 was extracting the handler's authorization body into {@link
 * ModNetworking#handleHelmInput} so this GameTest could call it directly; the authorization logic
 * itself (the two {@code if} checks) is byte-identical to what shipped before T09.</p>
 *
 * <p>Scope note: AC-008/the DoD name "velocity/yaw/fuel/anchor/edit-state" as the state that must
 * stay byte-identical for a rejected input. This class snapshots velocity ({@code
 * getDeltaMovement()}), yaw ({@code getYRot()}), fuel ({@code getFuelLevel()}), anchor ({@code
 * isAnchored()}), and the raw input fields {@link ModNetworking#handleHelmInput} is the sole
 * production writer of ({@code getInputForward}/{@code getSyncedTurn}/{@code getInputVertical}) --
 * the most direct evidence of whether the handler mutated anything. "Edit-state" is intentionally
 * NOT asserted: no edit-mode concept exists anywhere in {@code ShipEntity} yet (confirmed via
 * repo-wide search for {@code EditMode}/{@code editMode}, zero hits) -- that is REQ-012/T12's
 * scope, not yet implemented, so there is nothing to snapshot.</p>
 */
public final class PilotControlAuthorityGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /** A single, fixed non-trivial payload reused across all three tests for direct comparability. */
    private static final HelmInputC2SPayload CONTROL_INPUT = new HelmInputC2SPayload(0.6f, -0.4f, 0.9f);

    /**
     * Wheel + 4 core neighbors (south = pilot seat, sitting exactly at the SOUTH-facing BUG's
     * deterministic front-of-wheel anchor, T06) + thruster + edge BUG, plus a {@code
     * copilot_seat} block attached to the east core-neighbor plank -- identical layout to {@code
     * CopilotSeatOccupancyGameTest} (T07) so a genuine copilot rider can be mounted here too.
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
     * Every piece of state AC-008 names (minus edit-state, see class javadoc) plus the raw input
     * fields {@link ModNetworking#handleHelmInput} actually writes -- the most direct evidence a
     * rejected call mutated nothing.
     */
    private record ControlStateSnapshot(
            Vec3 velocity, float yaw, int fuel, boolean anchored,
            float inputForward, float inputTurn, float inputVertical) {

        static ControlStateSnapshot of(ShipEntity ship) {
            return new ControlStateSnapshot(
                    ship.getDeltaMovement(), ship.getYRot(), ship.getFuelLevel(), ship.isAnchored(),
                    ship.getInputForward(), ship.getSyncedTurn(), ship.getInputVertical());
        }
    }

    private static void assertByteIdentical(GameTestHelper helper, ControlStateSnapshot before,
                                             ControlStateSnapshot after, String context) {
        if (!before.equals(after)) {
            helper.fail(context + ": expected velocity/yaw/fuel/anchor/input state to stay "
                    + "byte-identical before vs. after the rejected input, before=" + before
                    + " after=" + after);
        }
    }

    /** (a) The easy, obvious case: a player who never mounted the ship at all. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nonRiderInputRejected(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();

        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        if (bystander.getVehicle() == ship) {
            helper.fail("test precondition: bystander must not be riding the ship");
            return;
        }

        ControlStateSnapshot before = ControlStateSnapshot.of(ship);
        ModNetworking.handleHelmInput(bystander, CONTROL_INPUT);
        ControlStateSnapshot after = ControlStateSnapshot.of(ship);

        assertByteIdentical(helper, before, after, "nonRiderInputRejected");
        helper.succeed();
    }

    /**
     * (d) AC-008's "der Versuch wird begrenzt protokolliert" (the attempt is rate-limited and
     * logged) half of the acceptance criterion -- previously entirely untested (code-review
     * finding). Verifies a rejected attempt is actually logged at least once via {@link
     * ModNetworking#rejectionLogCount}, the same test-inspection-hook pattern established by
     * {@code TutorialService#lastPopupSent}/{@code ShipAssemblyService#lastPreviewSent}, for both
     * rejection paths handled by {@link ModNetworking#handleHelmInput}: the non-rider bystander
     * (no {@link ShipEntity} to key by, {@code shipId=null}) and the genuine-rider copilot.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void rejectedAttemptIsLogged(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();

        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        if (ModNetworking.rejectionLogCount(bystander.getUUID(), null) != 0) {
            helper.fail("test precondition: fresh bystander must start with zero logged rejections");
            return;
        }
        ModNetworking.handleHelmInput(bystander, CONTROL_INPUT);
        if (ModNetworking.rejectionLogCount(bystander.getUUID(), null) != 1) {
            helper.fail("expected the non-rider's rejected attempt to be logged exactly once, got count="
                    + ModNetworking.rejectionLogCount(bystander.getUUID(), null));
            return;
        }
        if (ModNetworking.lastRejectionLoggedAt(bystander.getUUID(), null) == null) {
            helper.fail("expected lastRejectionLoggedAt to be set after a logged rejection");
            return;
        }

        ServerPlayer copilot = helper.makeMockServerPlayerInLevel();
        copilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult mountResult = ship.interact(copilot, InteractionHand.MAIN_HAND);
        if (mountResult != InteractionResult.CONSUME || !ship.isCopilot(copilot)) {
            helper.fail("test precondition: expected the copilot to actually be mounted before "
                    + "sending control input, got interact()=" + mountResult);
            return;
        }
        ModNetworking.handleHelmInput(copilot, CONTROL_INPUT);
        if (ModNetworking.rejectionLogCount(copilot.getUUID(), ship.getUUID()) != 1) {
            helper.fail("expected the copilot's rejected attempt to be logged exactly once, got count="
                    + ModNetworking.rejectionLogCount(copilot.getUUID(), ship.getUUID()));
            return;
        }

        helper.succeed();
    }

    /**
     * (e) Proves the rate-limiting is real, not just a single always-log call: a rejected
     * bystander sending RAPID repeated helm-input attempts (simulating a client that keeps
     * sending control input every tick while unauthorized, AC-008's named spam scenario) must
     * produce exactly ONE logged rejection, not N -- {@link ModNetworking#rejectionLogCount}
     * would read N if the cooldown were absent or a no-op.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void rapidRepeatedRejectionsDoNotSpamLog(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }

        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        if (ModNetworking.rejectionLogCount(bystander.getUUID(), null) != 0) {
            helper.fail("test precondition: fresh bystander must start with zero logged rejections");
            return;
        }

        final int ATTEMPTS = 50;
        for (int i = 0; i < ATTEMPTS; i++) {
            ModNetworking.handleHelmInput(bystander, CONTROL_INPUT);
        }

        int count = ModNetworking.rejectionLogCount(bystander.getUUID(), null);
        if (count != 1) {
            helper.fail("expected " + ATTEMPTS + " rapid rejected attempts to produce exactly 1 "
                    + "logged rejection (rate-limited), got count=" + count);
            return;
        }

        helper.succeed();
    }

    /**
     * (b) The discriminating case per the test-plan's counter-thesis: the copilot IS a genuine
     * rider (mounted via T07's real {@link ShipEntity#interact} copilot-mount path, not a stand-in)
     * yet must still be rejected -- proving the guard checks "is specifically the pilot", not
     * merely "is riding at all".
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void copilotInputRejectedDespiteBeingARider(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();

        ServerPlayer copilot = helper.makeMockServerPlayerInLevel();
        copilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult mountResult = ship.interact(copilot, InteractionHand.MAIN_HAND);
        if (mountResult != InteractionResult.CONSUME || !ship.isCopilot(copilot) || copilot.getVehicle() != ship) {
            helper.fail("test precondition: expected the copilot to actually be mounted and "
                    + "riding before sending control input, got interact()=" + mountResult
                    + " isCopilot=" + ship.isCopilot(copilot) + " vehicle=" + copilot.getVehicle());
            return;
        }
        if (ship.isPilot(copilot)) {
            helper.fail("test precondition violated: the mounted copilot must not also read as pilot");
            return;
        }

        ControlStateSnapshot before = ControlStateSnapshot.of(ship);
        ModNetworking.handleHelmInput(copilot, CONTROL_INPUT);
        ControlStateSnapshot after = ControlStateSnapshot.of(ship);

        assertByteIdentical(helper, before, after, "copilotInputRejectedDespiteBeingARider");
        // Re-assert post-attempt: the copilot must not have been silently promoted to pilot by
        // the rejected call either.
        if (ship.isPilot(copilot)) {
            helper.fail("expected the copilot to still not read as pilot after the rejected "
                    + "control input, but isPilot(copilot) is now true");
            return;
        }
        if (!copilot.getVehicle().equals(ship) || !ship.isCopilot(copilot)) {
            helper.fail("expected the copilot to remain mounted and tracked as copilot after "
                    + "their rejected control input (no side-effect dismount)");
            return;
        }
        helper.succeed();
    }

    /** (c) Positive control: the actual assigned pilot's input is applied. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void pilotInputAccepted(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        if (!ship.isPilot(pilot) || pilot.getVehicle() != ship) {
            helper.fail("test precondition: expected the assembled pilot to be mounted and "
                    + "tracked as pilot before sending control input");
            return;
        }

        ModNetworking.handleHelmInput(pilot, CONTROL_INPUT);

        // Proves the matrix isn't vacuously "always rejects": the pilot's input actually landed.
        if (ship.getInputForward() != CONTROL_INPUT.forward()) {
            helper.fail("expected the pilot's forward input to be applied, expected="
                    + CONTROL_INPUT.forward() + " got=" + ship.getInputForward());
            return;
        }
        if (ship.getSyncedTurn() != CONTROL_INPUT.turn()) {
            helper.fail("expected the pilot's turn input to be applied, expected="
                    + CONTROL_INPUT.turn() + " got=" + ship.getSyncedTurn());
            return;
        }
        if (ship.getInputVertical() != CONTROL_INPUT.throttle()) {
            helper.fail("expected the pilot's throttle input to be applied, expected="
                    + CONTROL_INPUT.throttle() + " got=" + ship.getInputVertical());
            return;
        }
        helper.succeed();
    }
}
