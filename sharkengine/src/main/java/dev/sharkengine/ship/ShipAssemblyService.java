package dev.sharkengine.ship;

import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.ModTags;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ShipAssemblyService {
    // MVP defaults
    public static final int MAX_BLOCKS = 512;
    public static final int MAX_RADIUS = 32; // Manhattan / BFS depth-ish via distManhattan

    private ShipAssemblyService() {}

    public record AssembleResult(String translationKey, Object arg) {}

    public static AssembleResult tryAssemble(ServerLevel level, BlockPos wheelPos, ServerPlayer pilot) {
        // 1) BFS collect
        LongSet ship = new LongOpenHashSet();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(wheelPos);

        while (!q.isEmpty() && ship.size() < MAX_BLOCKS) {
            BlockPos p = q.poll();
            if (p.distManhattan(wheelPos) > MAX_RADIUS) continue;

            long key = p.asLong();
            if (ship.contains(key)) continue;

            BlockState st = level.getBlockState(p);
            if (st.isAir()) continue;

            // eligibility gate
            if (!st.is(ModTags.SHIP_ELIGIBLE)) continue;

            ship.add(key);

            for (Direction d : Direction.values()) {
                q.add(p.relative(d));
            }
        }

        if (ship.isEmpty()) {
            return new AssembleResult("message.sharkengine.assembly_fail_empty", "");
        }

        // 2) World-contact gate (pragmatic MVP)
        int contactPoints = countWorldContacts(level, ship);
        if (contactPoints > 0) {
            return new AssembleResult("message.sharkengine.assembly_fail_contact", contactPoints);
        }

        // 3) Snapshot blueprint + remove blocks
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>(ship.size());
        for (var it = ship.iterator(); it.hasNext(); ) {
            long key = it.nextLong();
            BlockPos p = BlockPos.of(key);
            BlockState st = level.getBlockState(p);
            blocks.add(new ShipBlueprint.ShipBlock(
                    p.getX() - wheelPos.getX(),
                    p.getY() - wheelPos.getY(),
                    p.getZ() - wheelPos.getZ(),
                    st
            ));
        }

        // Remove (NOTE: no block-entity/NBT yet in this MVP base)
        for (var it = ship.iterator(); it.hasNext(); ) {
            BlockPos p = BlockPos.of(it.nextLong());
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }

        // 4) Spawn ship entity
        ShipEntity shipEntity = new ShipEntity(ModEntities.SHIP, level);
        shipEntity.setPos(wheelPos.getX() + 0.5, wheelPos.getY() + 0.5, wheelPos.getZ() + 0.5);
        shipEntity.setYawDeg(pilot.getYRot());
        shipEntity.setBlueprint(new ShipBlueprint(wheelPos, blocks));
        shipEntity.setPilot(pilot);
        level.addFreshEntity(shipEntity);

        pilot.startRiding(shipEntity, true);

        return new AssembleResult("message.sharkengine.assembly_ok", blocks.size());
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
