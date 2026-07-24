package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipBlueprint;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * AIR-015 tests for {@link ShipBlueprint}'s SchemaVersion/AssemblyYaw NBT
 * fields and the legacy-save fallback.
 *
 * <p>Run as GameTests, not plain unit tests: {@code ShipBlueprint} has
 * {@code toNbt()}/{@code fromNbt()} methods whose signatures reference real
 * Minecraft NBT classes ({@code CompoundTag}, {@code NbtUtils}, ...), which
 * have no test-classpath stub. Attempting to unit-test even the
 * NBT-independent parts of the class (constructors, {@code withAssemblyYaw})
 * fails with {@code NoClassDefFoundError} at class-verification time, before
 * any NBT method is actually called — Java resolves every method signature
 * on a class when it's linked, not lazily per call. GameTests run against
 * the real Minecraft classpath, so this isn't a limitation here.</p>
 */
public final class BlueprintPersistenceGameTest implements FabricGameTest {

    private static ShipBlueprint oneBlockBlueprint(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        List<ShipBlueprint.ShipBlock> blocks = List.of(
                new ShipBlueprint.ShipBlock(1, 0, 0, Blocks.OAK_PLANKS.defaultBlockState())
        );
        return new ShipBlueprint(origin, blocks, blocks.size());
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void roundtripsSchemaVersionAndAssemblyYaw(GameTestHelper helper) {
        ShipBlueprint original = oneBlockBlueprint(helper).withAssemblyYaw(90f);

        CompoundTag tag = original.toNbt();
        ShipBlueprint restored = ShipBlueprint.fromNbt(tag, helper.getLevel().registryAccess());

        if (restored.schemaVersion() != ShipBlueprint.CURRENT_SCHEMA_VERSION) {
            helper.fail("expected schemaVersion=" + ShipBlueprint.CURRENT_SCHEMA_VERSION
                    + ", got " + restored.schemaVersion());
            return;
        }
        if (Math.abs(restored.assemblyYaw() - 90f) > 1e-6) {
            helper.fail("expected assemblyYaw=90, got " + restored.assemblyYaw());
            return;
        }
        if (restored.blockCount() != original.blockCount()) {
            helper.fail("expected blockCount=" + original.blockCount() + ", got " + restored.blockCount());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void legacyV1NbtDefaultsAssemblyYawToZero(GameTestHelper helper) {
        ShipBlueprint original = oneBlockBlueprint(helper);
        CompoundTag tag = original.toNbt();
        // Simulate a pre-AIR-015 (v1) save: strip the fields v1 never wrote.
        tag.remove("SchemaVersion");
        tag.remove("AssemblyYaw");

        ShipBlueprint restored = ShipBlueprint.fromNbt(tag, helper.getLevel().registryAccess());

        if (Math.abs(restored.assemblyYaw()) > 1e-6) {
            helper.fail("expected assemblyYaw=0 as the v1 default, got " + restored.assemblyYaw());
            return;
        }
        if (restored.schemaVersion() != 1) {
            helper.fail("expected schemaVersion=1 (implicit, no SchemaVersion key), got " + restored.schemaVersion());
            return;
        }
        // The caller (ShipEntity.readAdditionalSaveData) is responsible for
        // patching this to the entity's persisted BugYaw; verify the
        // patch mechanism itself works as intended.
        ShipBlueprint patched = restored.withAssemblyYaw(180f);
        if (Math.abs(patched.assemblyYaw() - 180f) > 1e-6) {
            helper.fail("withAssemblyYaw patch did not apply, got " + patched.assemblyYaw());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void roundtripsSeatAnchors(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        List<ShipBlueprint.ShipBlock> blocks = List.of(
                new ShipBlueprint.ShipBlock(1, 0, 0, Blocks.OAK_PLANKS.defaultBlockState())
        );
        ShipBlueprint original = new ShipBlueprint(origin, blocks, blocks.size())
                .withSeatAnchors(List.of(new ShipBlueprint.SeatAnchor(0, 0, 1)));

        CompoundTag tag = original.toNbt();
        ShipBlueprint restored = ShipBlueprint.fromNbt(tag, helper.getLevel().registryAccess());

        if (restored.seatAnchors().size() != 1) {
            helper.fail("expected exactly one SeatAnchor to roundtrip, got " + restored.seatAnchors().size());
            return;
        }
        ShipBlueprint.SeatAnchor anchor = restored.seatAnchors().get(0);
        if (anchor.dx() != 0 || anchor.dy() != 0 || anchor.dz() != 1) {
            helper.fail("expected SeatAnchor offset (0,0,1) to roundtrip unchanged, got ("
                    + anchor.dx() + "," + anchor.dy() + "," + anchor.dz() + ")");
            return;
        }
        if (restored.schemaVersion() != ShipBlueprint.CURRENT_SCHEMA_VERSION) {
            helper.fail("expected schemaVersion=" + ShipBlueprint.CURRENT_SCHEMA_VERSION
                    + ", got " + restored.schemaVersion());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void legacyPreV3NbtDefaultsSeatAnchorsToEmpty(GameTestHelper helper) {
        ShipBlueprint original = oneBlockBlueprint(helper)
                .withSeatAnchors(List.of(new ShipBlueprint.SeatAnchor(0, 0, 1)));
        CompoundTag tag = original.toNbt();
        // Simulate a pre-REQ-006 (v1/v2) save: strip the field v1/v2 never wrote, exactly
        // like legacyV1NbtDefaultsAssemblyYawToZero does for AssemblyYaw above (NFR-004:
        // conservative migration, never a fabricated/guessed anchor position).
        tag.remove("SeatAnchors");

        ShipBlueprint restored = ShipBlueprint.fromNbt(tag, helper.getLevel().registryAccess());

        if (!restored.seatAnchors().isEmpty()) {
            helper.fail("expected seatAnchors=[] as the pre-v3 default when the NBT tag is "
                    + "absent, got " + restored.seatAnchors());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fromNbtIgnoresCorruptStoredBlockCount(GameTestHelper helper) {
        ShipBlueprint original = oneBlockBlueprint(helper); // 1 real block
        CompoundTag tag = original.toNbt();
        tag.putInt("BlockCount", 999); // corrupt: doesn't match the actual block list

        ShipBlueprint restored = ShipBlueprint.fromNbt(tag, helper.getLevel().registryAccess());

        if (restored.blockCount() != 1) {
            helper.fail("expected blockCount derived from the real block list (1), "
                    + "got " + restored.blockCount() + " (trusted the corrupt stored value)");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void assemblyPlumbsWestFacingBugYawIntoBlueprint(GameTestHelper helper) {
        BlockPos wheelPos = new BlockPos(3, 1, 3);
        helper.setBlock(wheelPos, ModBlocks.STEERING_WHEEL);
        helper.setBlock(wheelPos.north(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.south(), Blocks.OAK_PLANKS);
        helper.setBlock(wheelPos.east(), Blocks.OAK_PLANKS);
        // REQ-006: the BUG below faces WEST, so the pilot seat must sit exactly one block
        // west of the wheel (the deterministic front-of-wheel anchor) -- it doubles as the
        // west core-neighbor, same as any other ship_eligible block would.
        helper.setBlock(wheelPos.west(), ModBlocks.PILOT_SEAT);
        helper.setBlock(wheelPos.above(), ModBlocks.THRUSTER);
        // BUG facing WEST (directionToYaw: WEST=90) instead of the usual SOUTH.
        BlockState bugState = ModBlocks.BUG.defaultBlockState().setValue(BugBlock.FACING, Direction.WEST);
        helper.setBlock(wheelPos.north().north(), bugState);

        BlockPos absoluteWheelPos = helper.absolutePos(wheelPos);
        ShipAssemblyService.StructureScan scan =
                ShipAssemblyService.scanStructure(helper.getLevel(), absoluteWheelPos);

        if (!scan.canAssemble()) {
            helper.fail("expected canAssemble()=true, scan rejected the structure");
            return;
        }
        ShipBlueprint blueprint = scan.toBlueprint();
        if (Math.abs(blueprint.assemblyYaw() - 90f) > 1e-6) {
            helper.fail("expected blueprint.assemblyYaw()=90 for a WEST-facing BUG, got "
                    + blueprint.assemblyYaw());
            return;
        }
        // REQ-006: the seat anchor must be exactly the WEST-facing front offset (-1, 0, 0),
        // matching Direction.WEST's own normal vector -- not a fallback/alternate position.
        if (blueprint.seatAnchors().size() != 1) {
            helper.fail("expected exactly one SeatAnchor for a WEST-facing BUG with a "
                    + "correctly-placed seat, got " + blueprint.seatAnchors().size());
            return;
        }
        ShipBlueprint.SeatAnchor anchor = blueprint.seatAnchors().get(0);
        if (anchor.dx() != -1 || anchor.dy() != 0 || anchor.dz() != 0) {
            helper.fail("expected SeatAnchor offset (-1,0,0) for a WEST-facing BUG, got ("
                    + anchor.dx() + "," + anchor.dy() + "," + anchor.dz() + ")");
            return;
        }
        helper.succeed();
    }
}
