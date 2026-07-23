package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipEntity;
import dev.sharkengine.ship.EditModeDistanceGate;
import dev.sharkengine.ship.VehicleClass;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * REQ-017/AC-017 (T18) falsifying-test contract (test-plan {@code
 * docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-017 — Persistence and
 * restart"): a ship with both seats populated, non-round fuel spent, real damage taken and an
 * OPEN edit-mode session survives a full {@code saveWithoutId} → fresh-entity {@code load}
 * cycle with every field asserted INDIVIDUALLY (never an aggregate "vehicle count > 0"):
 * VehicleClass, blueprint (block count, schema version, assembly yaw, seat anchors incl.
 * roles), pilot seat/UUID, copilot seat/UUID, fuel (level AND fractional debt), damage, edit
 * state.
 *
 * <p><b>Why tag-level {@code contains(...)} asserts exist alongside the reload asserts:</b>
 * with only AIR in the {@link VehicleClass} enum, "reloaded class == AIR" passes vacuously even
 * if the field is never written (the fresh entity's default IS AIR) — the tag-presence assert
 * is what actually falsifies a missing write. Same reasoning for {@code EditModeActive=false}
 * defaults.</p>
 *
 * <p><b>Why edit state matters (found during T18):</b> without persisting {@code
 * editModeActive}, a server restart mid-edit reloads the ship as "not editing" while its
 * materialized edit-session blocks still stand in the world — the pilot can then fly off or
 * disassemble, leaving blueprint AND materialized blocks live simultaneously: RISK-004's
 * "Blöcke duplizieren" shape reachable by an ordinary restart.</p>
 *
 * <p><b>Damage-clamp hardening (same lost-in-recovery class as T16's NaN guard):</b> Health was
 * read from NBT unclamped; a corrupt/hand-edited tag could load 999 HP (or negative → instant
 * destruction). Load now clamps to {@code [0, MAX_HEALTH]} — asserted here against corrupt
 * tags directly.</p>
 *
 * <p><b>Trail-config slot (Preconditions §7, deliberately NOT DyeColor yet):</b> a reserved,
 * generic {@code TrailConfig} compound must survive load→save untouched (raw pass-through), so
 * T21 (REQ-019) can populate it without another schema change. Its DyeColor-specific round
 * trip is explicitly T21's scope — reserved here, not silently dropped between the tasks.</p>
 *
 * <p><b>Honestly-disclosed limit (per the planner's own T18 note):</b> this is an in-place
 * save-to-NBT-and-reload cycle — the closest GameTest-reachable proxy to a true OS-level
 * dedicated-server restart, which remains a manual REQ-024 release-evidence step.</p>
 */
public final class VehiclePersistenceRestartGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /**
     * The 7-block minimal valid structure shared with {@code AtomicEditReassemblyGameTest},
     * PLUS a copilot seat hanging off the east hull plank (orthogonally connected, so BFS
     * absorbs it; copilot seats carry no anchor-position rule the way the pilot seat does).
     */
    private static void placeStructureWithBothSeats(GameTestHelper helper, BlockPos wheelPos) {
        helper.setBlock(wheelPos, ModBlocks.STEERING_WHEEL);
        helper.setBlock(wheelPos.north(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.south(), ModBlocks.PILOT_SEAT);
        helper.setBlock(wheelPos.east(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.west(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.above(), ModBlocks.THRUSTER);
        BlockState bug = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.SOUTH);
        helper.setBlock(wheelPos.north().north(), bug);
        helper.setBlock(wheelPos.east().north(), ModBlocks.COPILOT_SEAT);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void fullVehicleStateSurvivesSaveLoadCycle(GameTestHelper helper) {
        placeStructureWithBothSeats(helper, WHEEL_POS);
        BlockPos wheelWorldPos = helper.absolutePos(WHEEL_POS);
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(wheelWorldPos.getX() + 0.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);

        ShipAssemblyService.AssembleResult result =
                ShipAssemblyService.tryAssemble(helper.getLevel(), wheelWorldPos, pilot);
        if (!result.isSuccess()) {
            helper.fail("test precondition: expected assembly to succeed, got " + result.translationKey());
            return;
        }
        List<ShipEntity> ships = helper.getLevel().getEntities(
                ModEntities.SHIP, new AABB(wheelWorldPos).inflate(8), e -> true);
        if (ships.size() != 1) {
            helper.fail("test precondition: expected exactly one spawned ShipEntity, got " + ships.size());
            return;
        }
        ShipEntity ship = ships.get(0);

        // Copilot mounts via the production interact path (empty hand, non-pilot player)
        ServerPlayer copilot = helper.makeMockServerPlayerInLevel();
        copilot.setPos(wheelWorldPos.getX() + 1.5, wheelWorldPos.getY(), wheelWorldPos.getZ() + 0.5);
        ship.interact(copilot, InteractionHand.MAIN_HAND);
        if (ship.getCopilot() == null || !ship.getCopilot().equals(copilot.getUUID())) {
            helper.fail("test precondition: copilot did not mount via interact()");
            return;
        }

        final double homeX = wheelWorldPos.getX() + 0.5;
        final double homeY = wheelWorldPos.getY() + 0.5;
        final double homeZ = wheelWorldPos.getZ() + 0.5;

        // Phase 0: burn fuel under real input until the debt accumulator is provably non-round
        ship.setInputs(0.0f, 0.0f, 1.0f);
        final int[] phase = {0};
        final boolean[] done = {false};

        helper.onEachTick(() -> {
            if (done[0]) {
                return;
            }
            ship.setPos(homeX, homeY, homeZ);

            if (phase[0] == 0) {
                ship.setInputs(0.0f, 0.0f, 1.0f);
                if (ship.getFuelLevel() < 100 && ship.getFuelDebt() > 0.0f) {
                    ship.setInputs(0.0f, 0.0f, 0.0f); // cut throttle, let speed decay
                    phase[0] = 1;
                }
                return;
            }

            if (phase[0] == 1) {
                if (ship.getCurrentSpeed() > 0.0f) {
                    return; // still decelerating — edit-mode gate requires stationary
                }
                runSaveLoadAssertions(helper, ship, pilot, copilot);
                done[0] = true;
                phase[0] = 2;
            }
        });

        helper.succeedWhen(() -> helper.assertTrue(done[0],
                "waiting for burn → stationary → save/load assertion chain (phase " + phase[0] + ")"));
    }

    private static void runSaveLoadAssertions(GameTestHelper helper, ShipEntity ship,
                                              ServerPlayer pilot, ServerPlayer copilot) {
        // Real damage via the production hurt() path: no-attacker source → flat 40
        int healthBefore = ship.getHealth();
        ship.hurt(helper.getLevel().damageSources().generic(), 1.0f);
        int damagedHealth = ship.getHealth();
        if (damagedHealth != healthBefore - 40) {
            helper.fail("test precondition: expected flat 40 no-attacker damage, health went "
                    + healthBefore + " -> " + damagedHealth);
        }

        // Open a REAL edit-mode session through the production gate (pilot is mounted at the
        // ship's own position → distance 0, stationary, undestroyed, same dimension)
        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(pilot);
        if (reason != EditModeDistanceGate.Reason.ACCEPTED) {
            helper.fail("test precondition: edit-mode gate rejected with " + reason);
        }
        if (!ship.isEditModeActive()) {
            helper.fail("test precondition: editModeActive not set after ACCEPTED");
        }

        ShipBlueprint originalBp = ship.getBlueprint();
        int fuelLevel = ship.getFuelLevel();
        float fuelDebt = ship.getFuelDebt();
        if (fuelDebt <= 0.0f) {
            helper.fail("test precondition: expected a non-round (debt > 0) fuel state, got " + fuelDebt);
        }

        CompoundTag tag = ship.saveWithoutId(new CompoundTag());

        // ── Tag-level writes (these falsify missing persistence even where reload defaults
        //    would mask it — see class javadoc) ──
        if (!tag.contains("VehicleClass")) {
            helper.fail("VehicleClass is not persisted — any future non-AIR vehicle would reload as AIR");
        }
        if (!tag.contains("EditModeActive")) {
            helper.fail("EditModeActive is not persisted — a restart mid-edit strands materialized "
                    + "blocks with a ship that no longer knows it is editing (RISK-004)");
        }

        // ── Individual field round-trips on a freshly-loaded entity ──
        ShipEntity reloaded = new ShipEntity(ModEntities.SHIP, helper.getLevel());
        reloaded.load(tag);

        if (reloaded.getVehicleClass() != VehicleClass.AIR) {
            helper.fail("VehicleClass drifted on reload: " + reloaded.getVehicleClass());
        }
        ShipBlueprint reloadedBp = reloaded.getBlueprint();
        if (reloadedBp == null) {
            helper.fail("blueprint lost on reload");
            return;
        }
        if (reloadedBp.blockCount() != originalBp.blockCount()) {
            helper.fail("blueprint blockCount drifted: " + reloadedBp.blockCount()
                    + " != " + originalBp.blockCount());
        }
        if (reloadedBp.schemaVersion() != originalBp.schemaVersion()) {
            helper.fail("blueprint schemaVersion drifted: " + reloadedBp.schemaVersion()
                    + " != " + originalBp.schemaVersion());
        }
        if (reloadedBp.assemblyYaw() != originalBp.assemblyYaw()) {
            helper.fail("blueprint assemblyYaw drifted: " + reloadedBp.assemblyYaw()
                    + " != " + originalBp.assemblyYaw());
        }
        if (!reloadedBp.seatAnchors().equals(originalBp.seatAnchors())) {
            helper.fail("seat anchors (incl. roles) drifted on reload: " + reloadedBp.seatAnchors()
                    + " != " + originalBp.seatAnchors());
        }
        if (!reloaded.isPilot(pilot)) {
            helper.fail("pilot seat authority lost on reload");
        }
        if (reloaded.getCopilot() == null || !reloaded.getCopilot().equals(copilot.getUUID())) {
            helper.fail("copilot occupancy lost on reload: " + reloaded.getCopilot());
        }
        if (reloaded.getFuelLevel() != fuelLevel) {
            helper.fail("fuel level drifted on reload: " + reloaded.getFuelLevel() + " != " + fuelLevel);
        }
        if (reloaded.getFuelDebt() != fuelDebt) {
            helper.fail("fuel debt drifted on reload: " + reloaded.getFuelDebt() + " != " + fuelDebt);
        }
        if (reloaded.getHealth() != damagedHealth) {
            helper.fail("damage state drifted on reload: " + reloaded.getHealth() + " != " + damagedHealth);
        }
        if (!reloaded.isEditModeActive()) {
            helper.fail("edit state lost on reload — restart mid-edit would strand materialized blocks");
        }
        reloaded.discard();

        // ── Corrupt-health clamp (lost-in-recovery hardening, NFR-009) ──
        CompoundTag corruptHigh = tag.copy();
        corruptHigh.putInt("Health", 999);
        ShipEntity loadedHigh = new ShipEntity(ModEntities.SHIP, helper.getLevel());
        loadedHigh.load(corruptHigh);
        if (loadedHigh.getHealth() != loadedHigh.getMaxHealth()) {
            helper.fail("corrupt Health=999 must clamp to MAX_HEALTH on load, got " + loadedHigh.getHealth());
        }
        loadedHigh.discard();

        CompoundTag corruptLow = tag.copy();
        corruptLow.putInt("Health", -50);
        ShipEntity loadedLow = new ShipEntity(ModEntities.SHIP, helper.getLevel());
        loadedLow.load(corruptLow);
        if (loadedLow.getHealth() != 0) {
            helper.fail("corrupt Health=-50 must clamp to 0 on load, got " + loadedLow.getHealth());
        }
        loadedLow.discard();

        // ── Reserved TrailConfig slot: raw pass-through load → save (T21 populates it) ──
        CompoundTag withTrail = tag.copy();
        CompoundTag trailConfig = new CompoundTag();
        trailConfig.putBoolean("Reserved", true);
        withTrail.put("TrailConfig", trailConfig);
        ShipEntity trailCarrier = new ShipEntity(ModEntities.SHIP, helper.getLevel());
        trailCarrier.load(withTrail);
        CompoundTag resaved = trailCarrier.saveWithoutId(new CompoundTag());
        if (!resaved.contains("TrailConfig") || !resaved.getCompound("TrailConfig").getBoolean("Reserved")) {
            helper.fail("reserved TrailConfig slot is not preserved across load→save — T21 (REQ-019) "
                    + "would need another schema change instead of populating the reserved slot");
        }
        trailCarrier.discard();
    }
}
