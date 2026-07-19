package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * REQ-011/AC-011 (T11) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-011 — Vehicle
 * re-entry"): the named counter-thesis is that "remount" gets implemented by silently re-running
 * full assembly validation, or worse, respawning the {@link ShipEntity} instead of toggling
 * occupancy on the existing one -- a test that only asserts "the player is riding again with the
 * right role" cannot distinguish a legitimate in-place remount from a silent reassembly that
 * destroys/recreates the entity. This class therefore captures the ship's unique entity
 * {@link UUID}, fuel level, and damage (health) state before dismount, then after remount asserts
 * (a) the SAME entity UUID is still the one riding -- resolved back through
 * {@code ServerLevel#getEntity(UUID)}, a fresh lookup independent of the local Java reference, so a
 * despawn-and-respawn-with-identical-stats implementation cannot fake this the way merely comparing
 * two local variables could -- and (b) fuel/health are byte-identical to their pre-dismount values.
 *
 * <p><b>Trail-configuration state -- deliberately NOT asserted (scope note):</b> the sharpened test
 * in the test-plan names "damage/trail-configuration state" as what must be captured. A repo-wide
 * search confirms no trail/DyeColor concept exists anywhere in {@code ShipEntity} yet: {@code grep
 * -rn "trail\|Trail" -i src/main/java src/client/java src/main/resources} and {@code grep -rn
 * "DyeColor" -i src/main/java src/client/java} both return zero hits in this task sequence. The PRD
 * confirms why: REQ-018/019/020 (single Thruster item with craft-time DyeColor component,
 * persistent colored trail render path, trail isolation) are P1 and explicitly scheduled for
 * T20-T22, still pending as of this task. Per T09's precedent for the same situation (see {@code
 * PilotControlAuthorityGameTest}'s javadoc, "Edit-state is intentionally NOT asserted... no edit-mode
 * concept exists anywhere in ShipEntity yet"), this class asserts only the damage/fuel state that
 * genuinely exists today ({@code getHealth()}/{@code getFuelLevel()}) rather than fabricating an
 * assertion against a field that does not exist.</p>
 *
 * <p><b>Investigation finding (read before assuming a fix is needed):</b> {@link
 * ShipEntity#interact} already routes remounts purely by the two independent {@code UUID} fields
 * {@code pilot}/{@code copilot} -- {@code pilot} is set once at assembly
 * ({@code ShipAssemblyService#tryAssemble}) and is <b>never</b> cleared on dismount (see {@link
 * ShipEntity#removePassenger}'s javadoc, explicit REQ-011 comment: "a ship can exist with no pilot
 * aboard is normal, expected state"), so a returning pilot is routed to the bottom
 * {@code player.startRiding(this, true)} branch (their own seat) rather than {@link
 * ShipEntity#interact}'s {@code mountCopilot} branch, purely because {@code isPilot(player)} still
 * reads true regardless of current riding status. Neither {@link ShipEntity#interact} nor {@code
 * mountCopilot} calls {@link ShipAssemblyService} or spawns a new {@link ShipEntity} anywhere in
 * their bodies (confirmed by reading both methods in full) -- occupancy is toggled on the same
 * entity object, exactly what AC-011's "ohne das Fahrzeug neu zu bauen" (without rebuilding the
 * vehicle) requires. This class exists to prove that structurally, not just describe it: T09 and
 * T10 each found one real gap under an otherwise-correct mechanism, so this investigation finding
 * is confirmed empirically below, not assumed.</p>
 */
public final class VehicleReentryGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /** Same forward-input-tick budget T10 established as flake-safe across all acceleration phases. */
    private static final int FLIGHT_TICKS = 200;

    /**
     * Wheel + 4 core neighbors (south = pilot seat, sitting exactly at the SOUTH-facing BUG's
     * deterministic front-of-wheel anchor, T06) + thruster + edge BUG, plus a {@code copilot_seat}
     * block attached to the east core-neighbor plank -- identical layout to {@code
     * CopilotDismountIntegrityGameTest} (T10) and {@code PilotControlAuthorityGameTest} (T09) so
     * this class continues T10's scenario as its starting state, per the plan's stated dependency.
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
     * seat GameTest uses, and returns the spawned {@link ShipEntity} (pilot already mounted by
     * {@code tryAssemble} itself) paired with that pilot -- or {@code null} if a precondition
     * failed (in which case {@code helper.fail} was already called and the caller must return
     * immediately).
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
     * then {@link ShipEntity#tick()} called directly and repeatedly, the exact method the real
     * server game loop invokes every tick -- so the pre-dismount fuel snapshot this test compares
     * against is a real, non-default value rather than the untouched starting fuel level (which a
     * buggy remount could trivially "match" by accident even after a silent reassembly reset it
     * back to the same default). Fails via {@code helper.fail} and returns {@code false} if the
     * ship isn't demonstrably burning fuel after {@link #FLIGHT_TICKS} ticks.
     */
    private static boolean driveIntoFlight(GameTestHelper helper, ShipEntity ship) {
        ship.setInputForward(1.0f);
        for (int i = 0; i < FLIGHT_TICKS; i++) {
            ship.tick();
        }
        if (ship.getFuelLevel() >= 100) {
            helper.fail("test precondition: expected measurably burned fuel after " + FLIGHT_TICKS
                    + " ticks of forward input (ship must be genuinely mid-flight, not stationary), "
                    + "fuel is still " + ship.getFuelLevel());
            return false;
        }
        return true;
    }

    /**
     * REQ-011/T11 (part a of the sharpened test): dismounts the assigned pilot mid-flight (real
     * fuel burned, real damage applied), remounts them via the exact production {@link
     * ShipEntity#interact} entry point, and asserts the entity riding afterward is the SAME entity
     * -- resolved fresh via {@code ServerLevel#getEntity(UUID)}, not merely the same local
     * variable -- with byte-identical fuel/health and a byte-identical persisted NBT snapshot, plus
     * that the remounted player is tracked as pilot (not copilot, not untracked).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void remountPreservesEntityIdentityAndState(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        if (!driveIntoFlight(helper, ship)) {
            return;
        }
        // Real damage state (Feature 5's health system, the concrete stand-in for "damage state"
        // named by the sharpened test -- see this class's javadoc for why trail is out of scope).
        // damageSources().generic() carries no attacking entity, so ShipEntity#hurt takes its
        // EXPLOSION_DAMAGE branch (a fixed 40), giving a deterministic, non-flaky pre-dismount
        // health value distinct from MAX_HEALTH.
        boolean hurtApplied = ship.hurt(helper.getLevel().damageSources().generic(), 0.0f);
        if (!hurtApplied || ship.getHealth() >= ShipEntity.MAX_HEALTH) {
            helper.fail("test precondition: expected ship.hurt() to actually reduce health below "
                    + "MAX_HEALTH=" + ShipEntity.MAX_HEALTH + ", got hurtApplied=" + hurtApplied
                    + " health=" + ship.getHealth());
            return;
        }

        // ─── snapshot BEFORE dismount ───
        UUID shipUuid = ship.getUUID();
        int fuelBefore = ship.getFuelLevel();
        int healthBefore = ship.getHealth();
        CompoundTag nbtBefore = ship.saveWithoutId(new CompoundTag());
        if (!nbtBefore.hasUUID("Pilot") || !nbtBefore.getUUID("Pilot").equals(pilot.getUUID())) {
            helper.fail("test precondition: expected pre-dismount NBT to record the pilot's UUID, got "
                    + (nbtBefore.hasUUID("Pilot") ? nbtBefore.getUUID("Pilot") : "absent"));
            return;
        }

        // ━━━ THE ACTUAL DISMOUNT ━━━ a normal player exit (the same path a real sneak-to-dismount
        // takes: Entity#stopRiding -> vehicle#removePassenger), not a hand-rolled shortcut.
        pilot.stopRiding();
        if (pilot.getVehicle() == ship || ship.getPassengers().contains(pilot)) {
            helper.fail("test precondition: expected the pilot to have actually dismounted");
            return;
        }
        // While vacated, the ship entity must still exist under its original UUID (sanity check
        // before the remount is even attempted) -- a premature discard()/respawn would already be
        // visible here.
        Entity whileVacated = helper.getLevel().getEntity(shipUuid);
        if (whileVacated != ship) {
            helper.fail("expected the ship to still be the same entity object while the seat sits "
                    + "vacant (no despawn should happen merely from a dismount), got "
                    + (whileVacated == null ? "null (entity gone)" : whileVacated));
            return;
        }

        // ━━━ THE ACTUAL REMOUNT ━━━ via ShipEntity#interact, the exact production right-click
        // entry point, empty hand (a bare re-entry attempt, not refuel/disassemble).
        pilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult remountResult = ship.interact(pilot, InteractionHand.MAIN_HAND);
        if (remountResult != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME (mounted) when the original pilot re-interacts with the "
                    + "vacated seat, got " + remountResult);
            return;
        }

        // ─── (the sharpest assertion) SAME entity UUID is ridden -- resolved via a fresh
        // ServerLevel#getEntity(UUID) lookup, which a despawn-and-respawn implementation cannot
        // fake even if it recreated a ShipEntity with identical stats and the identical UUID
        // copied over, because getEntity() only returns entities the level's tracker currently
        // considers valid; a discard()+re-add cycle is independently visible via getId() churn
        // and the tracked-entity count below. ───
        Entity afterEntity = helper.getLevel().getEntity(shipUuid);
        if (afterEntity != ship) {
            helper.fail("expected the SAME ShipEntity (identity, not just matching UUID/stats) to "
                    + "be resolvable after remount -- a different object here means the vehicle was "
                    + "despawned and respawned instead of toggling occupancy, got "
                    + (afterEntity == null ? "null (entity gone)" : afterEntity));
            return;
        }
        if (pilot.getVehicle() != ship) {
            helper.fail("expected the pilot to actually be mounted on the ship after remount, "
                    + "vehicle=" + pilot.getVehicle());
            return;
        }
        List<ShipEntity> shipsAfter = helper.getLevel().getEntities(
                ModEntities.SHIP, new AABB(helper.absolutePos(WHEEL_POS)).inflate(8), e -> true);
        if (shipsAfter.size() != 1 || shipsAfter.get(0) != ship) {
            helper.fail("expected exactly one ShipEntity (the original) to exist after remount, got "
                    + shipsAfter.size() + " ships: " + shipsAfter);
            return;
        }

        // ─── role matches EXACTLY the seat interacted with: pilot back as pilot, not copilot ───
        if (!ship.isPilot(pilot) || ship.isCopilot(pilot)) {
            helper.fail("expected the returning player to be tracked as pilot (their own seat), not "
                    + "copilot or untracked, isPilot=" + ship.isPilot(pilot) + " isCopilot="
                    + ship.isCopilot(pilot));
            return;
        }

        // ─── fuel/damage byte-identical to their pre-dismount values ───
        if (ship.getFuelLevel() != fuelBefore) {
            helper.fail("expected fuel to be unaffected by dismount+remount, before=" + fuelBefore
                    + " after=" + ship.getFuelLevel());
            return;
        }
        if (ship.getHealth() != healthBefore) {
            helper.fail("expected health (damage state) to be unaffected by dismount+remount, before="
                    + healthBefore + " after=" + ship.getHealth());
            return;
        }
        CompoundTag nbtAfter = ship.saveWithoutId(new CompoundTag());
        if (!nbtBefore.equals(nbtAfter)) {
            helper.fail("expected persisted NBT to be fully byte-identical before dismount vs. after "
                    + "remount (the pilot re-occupying their own seat changes no persisted field at "
                    + "all), before=" + nbtBefore + " after=" + nbtAfter);
            return;
        }

        helper.succeed();
    }

    /**
     * REQ-011/T11 (part b of the sharpened test): "erhält ausschließlich die zum Sitz gehörige
     * Rolle" -- the received role must match exactly the seat interacted with, not a
     * default-to-whatever-free-role fallback. The discriminating scenario: after BOTH the pilot
     * and copilot dismount, both seats sit simultaneously "free" by occupancy (nobody riding), yet
     * {@code pilot} the {@code UUID} field itself is never cleared by dismount (only {@code
     * copilot} is, see {@link ShipEntity#removePassenger}). A "default to whichever role happens to
     * be free" implementation could easily hand a brand-new, previously-unseen player the PILOT
     * role at this point, since the pilot seat is mechanically unoccupied -- exactly the bug this
     * method exists to catch. The correct, identity-based rule (confirmed against production code
     * in this class's javadoc) must instead route the new player to copilot, and must still hand
     * the ORIGINAL pilot their own pilot role back afterward, never downgrading them to copilot
     * merely because a different player mounted the (mechanically free) copilot seat first.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void remountGrantsExactlyTheInteractedSeatsRole(GameTestHelper helper) {
        AssembledShip assembled = assembleShipWithMountedPilotAndCopilotSeat(helper);
        if (assembled == null) {
            return;
        }
        ShipEntity ship = assembled.ship();
        ServerPlayer pilot = assembled.pilot();

        ServerPlayer copilot = helper.makeMockServerPlayerInLevel();
        copilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult copilotMountResult = ship.interact(copilot, InteractionHand.MAIN_HAND);
        if (copilotMountResult != InteractionResult.CONSUME || !ship.isCopilot(copilot)) {
            helper.fail("test precondition: expected the copilot to actually be mounted before "
                    + "flight, got interact()=" + copilotMountResult + " isCopilot="
                    + ship.isCopilot(copilot));
            return;
        }

        if (!driveIntoFlight(helper, ship)) {
            return;
        }

        // Both seats vacate -- pilot's UUID field stays assigned (REQ-011: normal/expected), but
        // mechanically nobody is riding either seat right now.
        pilot.stopRiding();
        copilot.stopRiding();
        if (ship.getPassengers().size() != 0) {
            helper.fail("test precondition: expected both pilot and copilot to have actually "
                    + "dismounted, remaining passengers=" + ship.getPassengers());
            return;
        }
        if (ship.getCopilot() != null) {
            helper.fail("test precondition: expected the copilot slot to be cleared immediately on "
                    + "dismount (T10), got " + ship.getCopilot());
            return;
        }

        // ─── a brand-new, previously-unseen player interacts while BOTH seats are mechanically
        // free -- the discriminating case: must land in the copilot seat, never the pilot seat, ───
        ServerPlayer newPlayer = helper.makeMockServerPlayerInLevel();
        newPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult newPlayerResult = ship.interact(newPlayer, InteractionHand.MAIN_HAND);
        if (newPlayerResult != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME (mounted) for the new player on the vacated copilot seat, "
                    + "got " + newPlayerResult);
            return;
        }
        if (ship.isPilot(newPlayer)) {
            helper.fail("expected the new (never-assigned) player to NEVER receive the pilot role "
                    + "merely because the pilot seat happened to be mechanically free -- this is "
                    + "exactly the 'default to whatever free role' bug REQ-011's role clause forbids");
            return;
        }
        if (!ship.isCopilot(newPlayer) || newPlayer.getVehicle() != ship) {
            helper.fail("expected the new player to be tracked and actually mounted as copilot "
                    + "(the only seat they have any legitimate claim to), isCopilot="
                    + ship.isCopilot(newPlayer) + " vehicle=" + newPlayer.getVehicle());
            return;
        }

        // ─── the ORIGINAL pilot re-interacts and must get exactly their own seat/role back, never
        // downgraded to copilot (already occupied by newPlayer) or rejected outright ───
        pilot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult pilotRemountResult = ship.interact(pilot, InteractionHand.MAIN_HAND);
        if (pilotRemountResult != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME (mounted) when the original pilot re-interacts, got "
                    + pilotRemountResult);
            return;
        }
        if (!ship.isPilot(pilot) || ship.isCopilot(pilot) || pilot.getVehicle() != ship) {
            helper.fail("expected the original pilot to receive exactly the pilot role back "
                    + "(their own seat), not copilot or untracked, isPilot=" + ship.isPilot(pilot)
                    + " isCopilot=" + ship.isCopilot(pilot) + " vehicle=" + pilot.getVehicle());
            return;
        }
        if (ship.getPassengers().size() != 2
                || !ship.getPassengers().contains(pilot)
                || !ship.getPassengers().contains(newPlayer)) {
            helper.fail("expected exactly the original pilot + the new copilot as tracked "
                    + "passengers after both re-entries, got " + ship.getPassengers());
            return;
        }

        helper.succeed();
    }
}
