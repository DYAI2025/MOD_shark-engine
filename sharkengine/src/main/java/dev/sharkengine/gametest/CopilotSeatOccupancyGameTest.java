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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * REQ-009/AC-009 (T07) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-009 — Craftable
 * copilot seat"): the sharpest named risk is an occupancy check that OVERWRITES the copilot
 * seat's occupant reference instead of rejecting a second interact outright — so a second
 * player would silently displace the first, with no dismount event, desyncing the first
 * player's client. A shallow test that only checks "B ends up riding" cannot distinguish this
 * from a correctly-additive design.
 *
 * <p>{@link #secondPlayerCannotDisplaceFirstCopilot} occupies the copilot seat with player A,
 * asserts A is riding, then has player B interact with the SAME already-occupied seat and
 * asserts B's mount is rejected, A remains mounted (no silent displacement, no dismount event),
 * and the copilot occupancy stays exactly A throughout (never overwritten). The "not even
 * internally via a dismount-and-remount cycle" half of that claim is backed by an actual
 * instrumented mount-event counter ({@link ShipEntity#getMountCount(java.util.UUID)}, fed by
 * {@code ShipEntity#addPassenger}) asserted unchanged for A across B's attempt — not just a
 * post-hoc end-state snapshot, which a hypothetical internal displace-then-remount that restored
 * A's final state would pass right through.</p>
 *
 * <p>{@link #occupancyIsObserverConsistent} asserts the server-authoritative occupant reference
 * ({@link ShipEntity#getCopilot()}) matches what an observer's tracked passenger list would
 * reflect ({@link ShipEntity#getPassengers()}, the standard Entity passenger list every
 * observer client's own entity tracking is populated from). <b>Flag (test-plan, explicit):</b>
 * true dual-real-client rendering desync is NOT GameTest-testable (single server instance) —
 * this covers server-tracked state only, not actual rendered client frames. Full cross-client
 * visual proof remains a manual EV-009 smoke item, not claimed as covered here.</p>
 *
 * <p>{@link #playerAfterPilotDismountBecomesTrackedCopilotNotStowaway} closes a separate,
 * later-discovered gap (Watcher review-required finding on the T07 remediation pass): {@link
 * ShipEntity#interact} used to pick its copilot-mount branch by checking whether ANY {@code
 * Player} currently rides the ship, which went false the instant the assigned pilot normally
 * dismounted (REQ-011) even though the copilot seat was still empty — letting the next
 * right-click fall through to a bare, completely untracked mount. This proves that exact
 * sequence (pilot mounts, pilot dismounts, a different player interacts) now leaves the new
 * player properly tracked as copilot, never as an untracked passenger.</p>
 */
public final class CopilotSeatOccupancyGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /**
     * Wheel + 4 core neighbors (south = pilot seat, sitting exactly at the SOUTH-facing BUG's
     * deterministic front-of-wheel anchor, T06) + thruster + edge BUG, plus a {@code
     * copilot_seat} block attached to the east core-neighbor plank (REQ-009: the copilot
     * seat's anchor is NOT wheel-facing-derived like the pilot seat's — it is simply wherever
     * the block was actually placed, connected into the structure via ordinary ship_eligible
     * adjacency).
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
     * Places the structure, asserts the {@code copilot_seat} block is genuinely wired into the
     * blueprint's {@code SeatAnchor} representation (REQ-009's "muss ... als SeatAnchor im
     * Blueprint repräsentiert werden" — a copilot mount that worked despite this being false
     * would be undetectable without this precondition check), assembles for real via the same
     * production entry point every other seat GameTest uses, and returns the spawned {@link
     * ShipEntity} (with its pilot already mounted, done by {@code tryAssemble} itself) paired
     * with that pilot — or {@code null} if a precondition failed (in which case {@code
     * helper.fail} was already called and the caller must return immediately).
     */
    private static AssembledShip assembleShipWithMountedPilotAndCopilotSeat(GameTestHelper helper) {
        placeStructureWithCopilotSeat(helper);
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);

        ShipAssemblyService.StructureScan preflight =
                ShipAssemblyService.scanStructure(helper.getLevel(), wheelWorldPos);
        ShipBlueprint previewBlueprint = preflight.toBlueprint();
        long copilotAnchors = previewBlueprint.seatAnchors().stream()
                .filter(a -> a.role() == ShipBlueprint.SeatRole.COPILOT)
                .count();
        if (copilotAnchors != 1) {
            helper.fail("test precondition: expected exactly one COPILOT SeatAnchor from the "
                    + "placed copilot_seat block, got " + copilotAnchors);
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
        return new AssembledShip(ships.get(0), pilot);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void secondPlayerCannotDisplaceFirstCopilot(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return; // precondition already failed via helper.fail above
        }
        ShipEntity ship = assembled.ship();

        ServerPlayer playerA = helper.makeMockServerPlayerInLevel();
        playerA.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult resultA = ship.interact(playerA, InteractionHand.MAIN_HAND);

        if (resultA != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME when player A occupies the empty copilot seat, got " + resultA);
            return;
        }
        if (!ship.isCopilot(playerA) || playerA.getVehicle() != ship) {
            helper.fail("expected player A to be mounted as the ship's copilot after the first interact");
            return;
        }
        int passengerCountAfterA = ship.getPassengers().size();
        // Instrumented, not just a post-hoc end-state snapshot: ShipEntity#addPassenger fires
        // on every real mount event for A regardless of path, so this baseline lets the
        // assertion below prove no internal dismount-and-remount cycle happened for A during
        // B's attempt, not merely that A's final state happens to look untouched.
        int mountCountAfterA = ship.getMountCount(playerA.getUUID());

        ServerPlayer playerB = helper.makeMockServerPlayerInLevel();
        playerB.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult resultB = ship.interact(playerB, InteractionHand.MAIN_HAND);

        // The sharpest assertion this whole class exists for (test-plan counter-thesis): B's
        // attempt on the already-occupied copilot seat must be REJECTED outright, not silently
        // overwrite A's occupant reference.
        if (resultB != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME (rejected) for B's attempt on an already-occupied "
                    + "copilot seat, got " + resultB);
            return;
        }
        if (playerB.getVehicle() == ship) {
            helper.fail("player B must NOT be mounted -- the copilot seat was already occupied by A");
            return;
        }
        if (!ship.isCopilot(playerA)) {
            helper.fail("player A must STILL be the copilot after B's rejected attempt -- the "
                    + "occupant reference must never have been overwritten");
            return;
        }
        if (playerA.getVehicle() != ship) {
            helper.fail("player A must remain mounted -- no silent dismount as a side effect of "
                    + "B's rejected mount attempt");
            return;
        }
        if (ship.getPassengers().size() != passengerCountAfterA) {
            helper.fail("expected server-tracked occupancy to stay exactly unchanged after B's "
                    + "rejected attempt, before=" + passengerCountAfterA
                    + " after=" + ship.getPassengers().size());
            return;
        }
        // The event-level guarantee the end-state checks above cannot make on their own: A's
        // real mount-event count (ShipEntity#addPassenger, instrumented, fires on every actual
        // mount via any path) must be untouched by B's attempt. A hypothetical internal
        // displace-then-remount of A that happened to restore the exact same end state would
        // still increment this counter, so this is what actually rules out "not even an
        // internal dismount-and-remount cycle" -- not just a snapshot of where A ended up.
        if (ship.getMountCount(playerA.getUUID()) != mountCountAfterA) {
            helper.fail("expected player A's real mount-event count to stay unchanged by B's "
                    + "rejected attempt (a hypothetical internal dismount-and-remount cycle that "
                    + "preserved A's final state would still have incremented this), before="
                    + mountCountAfterA + " after=" + ship.getMountCount(playerA.getUUID()));
            return;
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void occupancyIsObserverConsistent(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();

        ServerPlayer playerA = helper.makeMockServerPlayerInLevel();
        playerA.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ship.interact(playerA, InteractionHand.MAIN_HAND);

        // Two independent code paths must agree on who the copilot is: (1) the stored
        // server-authoritative occupant reference, and (2) the standard Entity passenger list
        // every observer client's own entity tracking is populated from (test-plan: "assert the
        // server-authoritative occupant id matches what an observer connection's tracked
        // passenger list reports" -- limited here to server-tracked state, per that section's
        // explicit flag that true dual-real-client rendering desync is not GameTest-testable).
        if (ship.getCopilot() == null || !ship.getCopilot().equals(playerA.getUUID())) {
            helper.fail("expected ship.getCopilot() to report player A's UUID, got " + ship.getCopilot());
            return;
        }
        if (!ship.getPassengers().contains(playerA)) {
            helper.fail("expected player A to be present in ship.getPassengers() (what an "
                    + "observer client's tracked passenger list would reflect), got "
                    + ship.getPassengers());
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-009/T07 remediation (Watcher, review-required finding: the "stowaway" mount gap).
     * {@link ShipEntity#interact} used to select the copilot-mount branch by checking whether
     * ANY {@code Player} currently rides the ship, as a proxy for "the assigned pilot is
     * aboard". That proxy was safe before copilots existed (only the pilot could ever be a
     * {@code Player} passenger) but wrong afterward: the moment the assigned pilot normally
     * dismounts (REQ-011 -- "a ship can exist with no pilot aboard" is documented, expected
     * behavior) while the copilot seat is still empty, the proxy goes false and the very next
     * right-click fell through to the bare {@code player.startRiding(this, true)} mount
     * fallback -- force-mounting the interacting player with ZERO registration as pilot or
     * copilot. That untracked "stowaway" left the copilot field still {@code null}, so a
     * SECOND player arriving afterward could legitimately claim the copilot seat too --
     * producing two simultaneous {@code Player} passengers for a seat AC-009 promises holds
     * "genau ein zusätzlicher Passagier" (exactly one additional passenger).
     *
     * <p>This reproduces exactly that sequence -- pilot mounts via real assembly, pilot
     * dismounts, a different player X right-clicks -- and asserts X is never an untracked bare
     * passenger. This structure has a copilot seat (see {@link
     * #assembleShipWithMountedPilotAndCopilotSeat}'s precondition check), so the correct
     * outcome for X is to become the properly-tracked copilot, not the pilot (X is not the
     * player assembly assigned as pilot, and REQ-011 restricts pilot re-entry to that specific
     * player) and not an unmounted rejection either.</p>
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void playerAfterPilotDismountBecomesTrackedCopilotNotStowaway(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        if (pilot.getVehicle() != ship) {
            helper.fail("test precondition: expected the assembled pilot to already be riding "
                    + "the ship before dismounting");
            return;
        }
        // REQ-011's documented normal case: the pilot leaves; the ship persists, still with its
        // permanent pilot assignment on record (ShipEntity#isPilot is unaffected by riding
        // state by design, see ShipEntity#pilot's javadoc), just nobody currently aboard it.
        pilot.stopRiding();
        if (pilot.getVehicle() == ship || ship.getPassengers().contains(pilot)) {
            helper.fail("test precondition: expected the pilot to have actually dismounted");
            return;
        }

        ServerPlayer playerX = helper.makeMockServerPlayerInLevel();
        playerX.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult resultX = ship.interact(playerX, InteractionHand.MAIN_HAND);

        if (resultX != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME for X's interact against a ship whose pilot just "
                    + "dismounted, got " + resultX);
            return;
        }
        // The sharpest assertion this test exists for: X must be a PROPERLY TRACKED occupant,
        // never a bare untracked passenger. Before the fix, X would have been mounted via the
        // player.startRiding(this, true) fallback with neither ship.isPilot(X) nor
        // ship.isCopilot(X) true -- an untracked stowaway, exactly the bug this closes.
        boolean trackedAsPilot = ship.isPilot(playerX);
        boolean trackedAsCopilot = ship.isCopilot(playerX);
        if (!trackedAsPilot && !trackedAsCopilot) {
            helper.fail("player X must be tracked as pilot or copilot if mounted at all -- got "
                    + "neither, meaning X is an untracked stowaway (the exact bug this test "
                    + "guards against)");
            return;
        }
        if (trackedAsPilot) {
            helper.fail("player X must NOT be tracked as pilot -- X is not this ship's assigned "
                    + "pilot (REQ-011 restricts pilot re-entry to that specific player), only "
                    + "the copilot seat was ever X's to legitimately claim");
            return;
        }
        if (playerX.getVehicle() != ship) {
            helper.fail("expected player X to actually be mounted on the ship (tracked as "
                    + "copilot but not riding would itself be a desync)");
            return;
        }
        if (ship.getPassengers().size() != 1 || !ship.getPassengers().contains(playerX)) {
            helper.fail("expected exactly one tracked passenger (X) after the pilot dismounted "
                    + "and X mounted, got " + ship.getPassengers());
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-009/T07 remediation round 2 (security-reviewer BLOCKING finding): {@link
     * ShipEntity#copilot} used to be the sole in-memory guard {@link ShipEntity#mountCopilot}
     * relies on to reject a second occupant, yet -- unlike {@link ShipEntity#pilot}, persisted
     * right next to it via an explicit "Pilot" UUID tag -- it was never written in {@code
     * addAdditionalSaveData} nor read back in {@code readAdditionalSaveData}. Both mount call
     * sites use {@code player.startRiding(this, true)} (force=true), which bypasses vanilla
     * {@code Entity}'s own single-passenger {@code canAddPassenger()} cap entirely, so the
     * unpersisted field was the ONLY thing enforcing AC-009's "exactly one additional
     * passenger" invariant. A live server-restart cycle can't be reproduced inside a single
     * GameTest server session (one continuous in-memory session, no real serialize/deserialize
     * across a process boundary) -- this gets as close as the GameTest framework allows: a
     * direct {@code addAdditionalSaveData}/{@code readAdditionalSaveData} round-trip via the
     * same public entry points ({@link ShipEntity#saveWithoutId} / {@link ShipEntity#load},
     * confirmed public via {@code javap} against the real mapped {@code Entity.class} --
     * both simply delegate to the protected pair this class overrides) on a real, assembled
     * {@code ShipEntity} with a copilot actually mounted, asserting the copilot's UUID comes
     * back out the other side unchanged. Before the fix, {@code restored.getCopilot()} would
     * be {@code null} here even though {@code original.getCopilot()} was a real player UUID --
     * exactly the desync that lets a second player force-mount past the reset guard after a
     * reload.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void copilotSurvivesSaveAndLoadRoundTrip(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity original = assembled.ship();

        ServerPlayer copilotPlayer = helper.makeMockServerPlayerInLevel();
        copilotPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult mountResult = original.interact(copilotPlayer, InteractionHand.MAIN_HAND);
        if (mountResult != InteractionResult.CONSUME || !original.isCopilot(copilotPlayer)) {
            helper.fail("test precondition: expected the copilot to actually be mounted before "
                    + "the save/load round-trip, got interact()=" + mountResult
                    + " isCopilot=" + original.isCopilot(copilotPlayer));
            return;
        }

        // Real save: the same public entry point the world's own save-on-tick/save-on-unload
        // path uses, which internally calls addAdditionalSaveData(compound) -- not a
        // hand-rolled shortcut that could drift from what actually gets persisted.
        CompoundTag saved = original.saveWithoutId(new CompoundTag());

        // Simulate the "reload" half of a server restart: a FRESH ShipEntity instance (never
        // added to the level, so this can't collide with `original`'s own UUID/entity tracking)
        // reconstructed purely from the saved NBT via the same public load() entry point, which
        // internally calls readAdditionalSaveData(compound) -- exactly the path a real chunk
        // reload would take.
        ShipEntity restored = new ShipEntity(ModEntities.SHIP, helper.getLevel());
        restored.load(saved);

        if (restored.getCopilot() == null) {
            helper.fail("expected the copilot UUID to survive a save/load round-trip, got "
                    + "null -- this is the exact BLOCKING finding: copilot resets across "
                    + "reload while startRiding(force=true) means nothing else enforces "
                    + "AC-009's single-additional-passenger invariant");
            return;
        }
        if (!restored.getCopilot().equals(copilotPlayer.getUUID())) {
            helper.fail("expected restored copilot UUID to equal the mounted player's UUID "
                    + copilotPlayer.getUUID() + ", got " + restored.getCopilot());
            return;
        }

        helper.succeed();
    }
}
