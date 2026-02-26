package dev.sharkengine.ship;

import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.ModTags;
import dev.sharkengine.net.BuilderPreviewS2CPayload;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ShipAssemblyService {
    // MVP defaults
    public static final int MAX_BLOCKS = 512;
    public static final int MAX_RADIUS = 32; // Manhattan / BFS depth-ish via distManhattan

    private ShipAssemblyService() {}

    public record AssembleResult(String translationKey, Object arg) {}

    public record StructureScan(BlockPos origin,
                                List<ShipBlueprint.ShipBlock> blocks,
                                List<BlockPos> invalidAttachments,
                                int contactPoints,
                                boolean hasThruster,
                                int thrusterCount) {
        public boolean isEmpty() {
            return blocks.isEmpty();
        }

        public int blockCount() {
            return blocks.size();
        }

        public boolean canAssemble() {
            return !isEmpty() && invalidAttachments.isEmpty() && contactPoints == 0 && hasThruster;
        }

        public ShipBlueprint toBlueprint() {
            return new ShipBlueprint(origin, blocks, blockCount());
        }
    }

    public static AssembleResult tryAssemble(ServerLevel level, BlockPos wheelPos, ServerPlayer pilot) {
        StructureScan scan = scanStructure(level, wheelPos);

        if (scan.isEmpty()) {
            return new AssembleResult("message.sharkengine.assembly_fail_empty", "");
        }

        if (!scan.invalidAttachments().isEmpty()) {
            return new AssembleResult("message.sharkengine.assembly_fail_invalid", scan.invalidAttachments().size());
        }

        if (scan.contactPoints() > 0) {
            return new AssembleResult("message.sharkengine.assembly_fail_contact", scan.contactPoints());
        }

        if (!scan.hasThruster()) {
            return new AssembleResult("message.sharkengine.assembly_fail_thruster", scan.thrusterCount());
        }

        ShipBlueprint blueprint = scan.toBlueprint();

        // Remove scanned blocks
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            BlockPos target = wheelPos.offset(block.dx(), block.dy(), block.dz());
            level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
        }

        // 4) Spawn ship entity
        ShipEntity shipEntity = new ShipEntity(ModEntities.SHIP, level);
        shipEntity.setPos(wheelPos.getX() + 0.5, wheelPos.getY() + 0.5, wheelPos.getZ() + 0.5);
        shipEntity.setYawDeg(pilot.getYRot());

        shipEntity.setBlueprint(blueprint);
        shipEntity.setPilot(pilot);
        level.addFreshEntity(shipEntity);

        pilot.startRiding(shipEntity, true);

        return new AssembleResult("message.sharkengine.assembly_ok", blueprint.blockCount());
    }

    public static void openBuilderPreview(ServerLevel level, BlockPos wheelPos, ServerPlayer player) {
        StructureScan scan = scanStructure(level, wheelPos);
        ShipBlueprint blueprint = new ShipBlueprint(wheelPos, scan.blocks(), scan.blockCount());

        BuilderPreviewS2CPayload payload = BuilderPreviewS2CPayload.open(
                wheelPos,
                blueprint.toNbt(),
                scan.invalidAttachments(),
                scan.contactPoints(),
                scan.canAssemble(),
                scan.thrusterCount()
        );
        ServerPlayNetworking.send(player, payload);
    }

    public static StructureScan scanStructure(ServerLevel level, BlockPos wheelPos) {
        LongSet visited = new LongOpenHashSet();
        LongSet ship = new LongOpenHashSet();
        LongSet invalid = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(wheelPos);

        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();
        List<BlockPos> invalidAttachments = new ArrayList<>();
        List<String> blockIds = new ArrayList<>();

        while (!queue.isEmpty() && ship.size() < MAX_BLOCKS) {
            BlockPos current = queue.poll();
            if (current.distManhattan(wheelPos) > MAX_RADIUS) continue;

            long key = current.asLong();
            if (visited.contains(key)) continue;
            visited.add(key);

            BlockState state = level.getBlockState(current);
            if (state.isAir()) continue;

            if (!state.is(ModTags.SHIP_ELIGIBLE)) {
                if (invalid.add(key)) {
                    invalidAttachments.add(current.immutable());
                }
                continue;
            }

            ship.add(key);
            blocks.add(new ShipBlueprint.ShipBlock(
                    current.getX() - wheelPos.getX(),
                    current.getY() - wheelPos.getY(),
                    current.getZ() - wheelPos.getZ(),
                    state
            ));
            blockIds.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());

            for (Direction d : Direction.values()) {
                queue.add(current.relative(d));
            }
        }

        int contactPoints = countWorldContacts(level, ship);
        int thrusterCount = ThrusterRequirements.countThrusters(blockIds);
        return new StructureScan(wheelPos, blocks, invalidAttachments, contactPoints, thrusterCount > 0, thrusterCount);
    }

    private static int countWorldContacts(ServerLevel level, LongSet ship) {
        int contacts = 0;
        CollisionContext ctx = CollisionContext.empty();

        for (var it = ship.iterator(); it.hasNext(); ) {
            BlockPos p = BlockPos.of(it.nextLong());

            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (ship.contains(n.asLong())) continue;

                BlockState ns = level.getBlockState(n);
                if (ns.isAir()) continue;

                // solid-ish world contact
                if (!ns.getCollisionShape(level, n, ctx).isEmpty()) {
                    contacts++;
                }
            }
        }
        return contacts;
    }
}
