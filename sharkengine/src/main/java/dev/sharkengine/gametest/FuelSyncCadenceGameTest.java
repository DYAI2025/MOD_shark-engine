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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * REQ-016/AC-016 (T17) falsifying-test contract (test-plan {@code
 * docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-016 — Fuel and speed
 * loop"). One integrated flight scenario proving two contracts:
 *
 * <ol>
 *   <li><b>Sync cadence</b> — on EVERY tick the authoritative server-side fuel level changes,
 *       the client-synced value ({@link ShipEntity#getSyncedFuel()}) matches it in that same
 *       tick. This falsifies a mount/dismount-only (or slower-interval) sync: the HUD reads
 *       {@code getFuelLevel()} → SYNC_FUEL live each frame, so synced == authoritative per
 *       change IS the HUD-cadence guarantee (the tester's "testing sync cadence, not merely
 *       that a sync happens once").</li>
 *   <li><b>Fractional-debt persistence round-trip</b> — at a moment where the fuel-debt
 *       accumulator is provably non-zero (a genuinely non-round mid-flight fuel state), a full
 *       {@code saveWithoutId} → fresh-entity {@code load} cycle preserves BOTH the integer fuel
 *       level and the fractional {@code FuelDebt} bit-exactly. Without persistence, every
 *       save/load quietly refunds up to one fuel unit of already-burned debt (the DoD's
 *       "rounding/truncation drift").</li>
 * </ol>
 *
 * <p><b>Mechanics notes:</b> consumption only runs under sustained forward input
 * ({@code setInputs(0,0,1)} — the same production method the C2S payload handler calls), gated
 * to once per 20 ticks, at {@code nominal × VehicleBalance.FUEL_CONSUMPTION_RATE} (0.25) — so
 * the first whole-unit drop needs several seconds; {@code timeoutTicks} is sized accordingly.
 * The ship is re-pinned to its spawn position every tick so real physics keep running while the
 * entity stays inside the test cell. The pure step math itself (exact power-of-two debt
 * accumulation, NaN sanitization on load) is unit-locked in {@code FuelSystemTest}; this test
 * proves the live entity wiring.</p>
 */
public final class FuelSyncCadenceGameTest implements FabricGameTest {

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

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void fuelSyncCadenceAndDebtPersistence(GameTestHelper helper) {
        placeValidStructure(helper, WHEEL_POS);
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

        final double homeX = wheelWorldPos.getX() + 0.5;
        final double homeY = wheelWorldPos.getY() + 0.5;
        final double homeZ = wheelWorldPos.getZ() + 0.5;

        // Sustained full throttle via the production input path (what the C2S handler calls)
        ship.setInputs(0.0f, 0.0f, 1.0f);

        final int[] lastFuel = {ship.getFuelLevel()};
        final int[] dropsSeen = {0};
        final boolean[] roundTripDone = {false};

        helper.onEachTick(() -> {
            // Keep physics running but the entity inside the test cell
            ship.setPos(homeX, homeY, homeZ);
            ship.setInputs(0.0f, 0.0f, 1.0f);

            int fuelNow = ship.getFuelLevel();
            if (fuelNow != lastFuel[0]) {
                if (fuelNow > lastFuel[0]) {
                    helper.fail("fuel increased mid-flight: " + lastFuel[0] + " -> " + fuelNow);
                }
                // AC-016 cadence: synced value matches the authoritative one in the SAME tick
                if (ship.getSyncedFuel() != fuelNow) {
                    helper.fail("sync cadence broken: authoritative fuel changed to " + fuelNow
                            + " but client-synced value lags at " + ship.getSyncedFuel());
                }
                lastFuel[0] = fuelNow;
                dropsSeen[0]++;

                // Persistence round-trip at a genuinely non-round moment (debt strictly > 0)
                if (!roundTripDone[0] && ship.getFuelDebt() > 0.0f) {
                    float liveDebt = ship.getFuelDebt();
                    CompoundTag tag = ship.saveWithoutId(new CompoundTag());
                    if (!tag.contains("FuelDebt")) {
                        helper.fail("FuelDebt is not persisted — a mid-flight save/load refunds "
                                + liveDebt + " units of already-burned fuel debt");
                    }
                    float savedDebt = tag.getFloat("FuelDebt");
                    if (savedDebt != liveDebt) {
                        helper.fail("saved FuelDebt " + savedDebt + " != live debt " + liveDebt);
                    }
                    ShipEntity reloaded = new ShipEntity(ModEntities.SHIP, helper.getLevel());
                    reloaded.load(tag);
                    if (reloaded.getFuelDebt() != liveDebt) {
                        helper.fail("reloaded FuelDebt drifted: " + reloaded.getFuelDebt()
                                + " != " + liveDebt);
                    }
                    if (reloaded.getFuelLevel() != fuelNow) {
                        helper.fail("reloaded FuelLevel drifted: " + reloaded.getFuelLevel()
                                + " != " + fuelNow);
                    }
                    reloaded.discard();
                    roundTripDone[0] = true;
                }
            }
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(dropsSeen[0] >= 2,
                    "waiting for at least two whole-unit fuel drops (saw " + dropsSeen[0] + ")");
            helper.assertTrue(roundTripDone[0],
                    "waiting for a non-zero FuelDebt moment to round-trip through NBT");
        });
    }
}
