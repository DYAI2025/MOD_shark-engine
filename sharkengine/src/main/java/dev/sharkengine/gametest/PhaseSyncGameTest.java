package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.AccelerationPhase;
import dev.sharkengine.ship.ShipAssemblyService;
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

import java.util.List;

/**
 * Falsifying-test contract for the acceleration-phase sync.
 *
 * <p><b>The defect this locks out.</b> {@code ShipEntity.phase} was a plain field, assigned
 * only inside {@code updatePhysics()}, which early-returns on the client. It was NOT in
 * {@link net.minecraft.network.syncher.SynchedEntityData} — unlike speed/mass/fuel/turn/vertical,
 * all of which have {@code isClientSide ? entityData.get(...) : field} getters. Consequence:
 * {@code ShipEntityRenderer} calls {@code getPhase()} on the client and therefore ALWAYS saw
 * {@code PHASE_1} — particles were permanently campfire smoke at intensity 0.2 and never
 * escalated to flame, no matter how fast the ship actually flew.</p>
 *
 * <p><b>Why this is a GameTest and not a unit test.</b> The test source set has no Minecraft
 * jar on its classpath (see CLAUDE.md, "Testing"), so {@code SynchedEntityData} cannot be
 * exercised there at all.</p>
 *
 * <p><b>Why "phase advances" alone would be a vacuous test.</b> The server-side field always
 * advanced correctly — that was never broken. The contract that actually falsifies the bug is
 * that the SYNCED value tracks the authoritative one in the SAME tick it changes, which is what
 * the renderer reads. Modelled on {@code FuelSyncCadenceGameTest}'s cadence assertion.</p>
 */
public final class PhaseSyncGameTest implements FabricGameTest {

    private static final BlockPos WHEEL_POS = new BlockPos(3, 1, 3);

    /** Same 7-block minimal valid structure as {@code FuelSyncCadenceGameTest}. */
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
    public void phaseIsSyncedToClientOnEveryChange(GameTestHelper helper) {
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

        // Full forward thrust every tick — the same method the C2S handler calls. Without a
        // held forward input, accelerationTicks never advances and the phase never changes.
        ship.setInputs(0.0f, 0.0f, 1.0f);

        final AccelerationPhase[] lastPhase = { ship.getPhase() };
        final boolean[] sawChange = { false };

        helper.onEachTick(() -> {
            // Re-pin to the spawn point every tick (same as FuelSyncCadenceGameTest). At up to
            // 30 blocks/sec the ship leaves the 8x8x8 cell within ~2 ticks, hits the non-air
            // world outside it, takes collision damage and becomes destroyed — and
            // updatePhysics() returns on isDestroyed() BEFORE the accelerationTicks++ line,
            // so the phase would never advance and this test would fail for a reason that has
            // nothing to do with syncing.
            ship.setPos(homeX, homeY, homeZ);
            ship.setInputs(0.0f, 0.0f, 1.0f);

            AccelerationPhase authoritative = ship.getPhase();
            AccelerationPhase synced = ship.getSyncedPhase();

            // The cadence contract: whenever the authoritative phase changes, the synced value
            // must already match in that same tick — the renderer reads it every frame.
            if (authoritative != lastPhase[0]) {
                sawChange[0] = true;
                if (synced != authoritative) {
                    helper.fail("phase sync broken: authoritative phase changed to " + authoritative
                            + " but client-synced value lags at " + synced);
                }
                lastPhase[0] = authoritative;
            }

            // Holds on every tick, not just on change — a write that only happens on mount
            // would pass a change-only assertion but still starve the renderer.
            if (synced != authoritative) {
                helper.fail("phase sync drift: authoritative=" + authoritative + " synced=" + synced);
            }
        });

        // PHASE_1 -> PHASE_2 happens at accelerationTicks 40. Assert well past that so a
        // never-advancing phase fails loudly instead of passing vacuously.
        helper.runAfterDelay(120, () -> {
            if (!sawChange[0]) {
                helper.fail("phase never advanced past " + lastPhase[0]
                        + " in 120 ticks of full forward thrust — expected at least PHASE_2");
            }
            if (ship.getSyncedPhase() != ship.getPhase()) {
                helper.fail("final phase mismatch: authoritative=" + ship.getPhase()
                        + " synced=" + ship.getSyncedPhase());
            }
            helper.succeed();
        });
    }

    /**
     * Particle emission must be rate-limited to ONE burst per tick, however often the caller
     * asks.
     *
     * <p><b>The defect this locks out.</b> {@code ShipEntityRenderer.spawnThrusterParticles} is
     * invoked from {@code EntityRenderer.render()} — once per FRAME, not per tick. The count
     * constant is even named {@code MAX_PARTICLES_PER_TICK}, which it never was: at 60 fps the
     * ship emitted its whole per-tick budget 60 times a second (and rolled the
     * {@code random.nextInt(20)} sound gate 60 times, firing ~3 sounds/sec instead of ~1).
     * Emission scaled with framerate, so the same ship looked different on different machines.</p>
     *
     * <p>The gate lives on the ENTITY, not the renderer: {@code EntityRenderer} is a single
     * instance shared by every {@code ShipEntity}, so a renderer-side counter would let one ship
     * suppress another's particles.</p>
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void particleEmissionIsGatedToOnePerTick(GameTestHelper helper) {
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

        final int[] grantsThisTick = { 0 };

        helper.onEachTick(() -> {
            // Simulate a high frame rate: ask many times within a single tick, exactly as
            // render() would at 60+ fps. Only the first ask may be granted.
            int granted = 0;
            for (int frame = 0; frame < 8; frame++) {
                if (ship.shouldEmitParticlesThisTick()) granted++;
            }
            if (granted > 1) {
                helper.fail("particle gate let " + granted + " emissions through in ONE tick across 8 "
                        + "simulated frames — emission is still framerate-bound");
            }
            grantsThisTick[0] += granted;
        });

        helper.runAfterDelay(60, () -> {
            // Over 60 ticks it must have granted at least once per tick's worth of asks, i.e. it
            // must not be starved either — a gate that never opens would also "pass" the cap check.
            if (grantsThisTick[0] == 0) {
                helper.fail("particle gate never opened in 60 ticks — emission would be dead, not rate-limited");
            }
            helper.succeed();
        });
    }
}
