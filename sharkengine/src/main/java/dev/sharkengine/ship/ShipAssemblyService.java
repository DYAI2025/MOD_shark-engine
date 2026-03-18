package dev.sharkengine.ship;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.ModTags;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.net.BuilderPreviewS2CPayload;
import dev.sharkengine.tutorial.TutorialService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for scanning ship structures, validating BUG placement,
 * and assembling ship entities.
 *
 * <p>Architecture: The BUG block is the sole source of the vehicle's
 * forward direction. Its FACING property defines "forward". Thrusters
 * provide thrust power but have no directional authority.</p>
 *
 * @author Shark Engine Team
 * @version 3.0 (BUG-Frontsystem + Debug-Logs)
 */
public final class ShipAssemblyService {
    private static final Logger LOGGER = LoggerFactory.getLogger("SharkEngine-Assembly");
    
    public static final int MAX_BLOCKS = 512;
    public static final int MAX_RADIUS = 32;

    private ShipAssemblyService() {}

    public record AssembleResult(String translationKey, Object arg) {}

    public record StructureScan(BlockPos origin,
                                List<ShipBlueprint.ShipBlock> blocks,
                                List<BlockPos> invalidAttachments,
                                int contactPoints,
                                boolean hasThruster,
                                int thrusterCount,
                                int coreNeighbors,
                                int bugCount,
                                boolean bugOnEdge,
                                float bugYawDeg) {
        public boolean isEmpty() {
            return blocks.isEmpty();
        }

        public int blockCount() {
            return blocks.size();
        }

        public boolean hasBug() {
            return bugCount == 1;
        }

        public boolean canAssemble() {
            return !isEmpty()
                    && invalidAttachments.isEmpty()
                    && contactPoints == 0
                    && hasThruster
                    && coreNeighbors >= 4
                    && bugCount == 1
                    && bugOnEdge;
        }

        public ShipBlueprint toBlueprint() {
            return new ShipBlueprint(origin, blocks, blockCount());
        }
    }

    public static AssembleResult tryAssemble(ServerLevel level, BlockPos wheelPos, ServerPlayer pilot) {
        LOGGER.info("🔧 Assembly requested at {}", wheelPos);
        
        StructureScan scan = scanStructure(level, wheelPos);
        LOGGER.info("📊 Scan result: {} blocks, {} invalid, {} contacts, {} thrusters, {} core, {} bugs",
            scan.blockCount(), scan.invalidAttachments().size(), scan.contactPoints(),
            scan.thrusterCount(), scan.coreNeighbors(), scan.bugCount());

        if (scan.isEmpty()) {
            LOGGER.warn("❌ Assembly failed: empty structure");
            return new AssembleResult("message.sharkengine.assembly_fail_empty", "");
        }

        if (!scan.invalidAttachments().isEmpty()) {
            LOGGER.warn("❌ Assembly failed: {} invalid attachments", scan.invalidAttachments().size());
            return new AssembleResult("message.sharkengine.assembly_fail_invalid", scan.invalidAttachments().size());
        }

        if (scan.contactPoints() > 0) {
            LOGGER.warn("❌ Assembly failed: {} world contacts", scan.contactPoints());
            return new AssembleResult("message.sharkengine.assembly_fail_contact", scan.contactPoints());
        }

        if (!scan.hasThruster()) {
            LOGGER.warn("❌ Assembly failed: no thrusters (count: {})", scan.thrusterCount());
            return new AssembleResult("message.sharkengine.assembly_fail_thruster", scan.thrusterCount());
        }

        if (scan.coreNeighbors() < 4) {
            LOGGER.warn("❌ Assembly failed: only {} core neighbors (need 4)", scan.coreNeighbors());
            return new AssembleResult("message.sharkengine.assembly_fail_core", scan.coreNeighbors());
        }

        // ═══════════════════════════════════════════════════════════════════
        // BUG VALIDATION
        // ═══════════════════════════════════════════════════════════════════
        if (scan.bugCount() == 0) {
            LOGGER.warn("❌ Assembly failed: no bug block found");
            return new AssembleResult("message.sharkengine.assembly_fail_no_bug", "");
        }
        if (scan.bugCount() > 1) {
            LOGGER.warn("❌ Assembly failed: {} bug blocks (need exactly 1)", scan.bugCount());
            return new AssembleResult("message.sharkengine.assembly_fail_multi_bug", scan.bugCount());
        }
        if (!scan.bugOnEdge()) {
            LOGGER.warn("❌ Assembly failed: bug block not on edge");
            return new AssembleResult("message.sharkengine.assembly_fail_bug_inside", "");
        }

        LOGGER.info("✅ All validation passed! Assembling ship...");

        ShipBlueprint blueprint = scan.toBlueprint();

        // Remove scanned blocks
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            BlockPos target = wheelPos.offset(block.dx(), block.dy(), block.dz());
            level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
        }

        // Spawn ship entity
        ShipEntity shipEntity = new ShipEntity(ModEntities.SHIP, level);
        shipEntity.setPos(wheelPos.getX() + 0.5, wheelPos.getY() + 0.5, wheelPos.getZ() + 0.5);

        // Set blueprint (calculates block stats, thruster count, etc.)
        shipEntity.setBlueprint(blueprint);
        shipEntity.setPilot(pilot);

        // ═══════════════════════════════════════════════════════════════════
        // BUG DIRECTION: The BUG block's FACING determines forward yaw.
        // This is absolute: player look direction is NOT used.
        // ═══════════════════════════════════════════════════════════════════
        shipEntity.setBugYawDeg(scan.bugYawDeg());
        shipEntity.setYawDeg(scan.bugYawDeg());

        level.addFreshEntity(shipEntity);
        LOGGER.info("✅ Ship entity created and added to level");

        boolean mounted = pilot.startRiding(shipEntity, true);
        LOGGER.info("✅ Player mounted: {}", mounted);
        
        TutorialService.notifyFlightTips(pilot);

        LOGGER.info("🎉 Assembly complete! Ship has {} blocks", blueprint.blockCount());
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
                scan.thrusterCount(),
                scan.coreNeighbors(),
                scan.bugCount()
        );
        ServerPlayNetworking.send(player, payload);

        if (scan.canAssemble()) {
            TutorialService.notifyReady(player);
        }
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

        // BUG tracking
        int bugCount = 0;
        int bugDx = 0, bugDy = 0, bugDz = 0;
        Direction bugFacing = Direction.NORTH;

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
            int dx = current.getX() - wheelPos.getX();
            int dy = current.getY() - wheelPos.getY();
            int dz = current.getZ() - wheelPos.getZ();
            blocks.add(new ShipBlueprint.ShipBlock(dx, dy, dz, state));
            blockIds.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());

            // Track BUG blocks
            if (state.is(ModBlocks.BUG)) {
                bugCount++;
                bugDx = dx;
                bugDy = dy;
                bugDz = dz;
                if (state.hasProperty(BugBlock.FACING)) {
                    bugFacing = state.getValue(BugBlock.FACING);
                }
            }

            for (Direction d : Direction.values()) {
                queue.add(current.relative(d));
            }
        }

        int contactPoints = countWorldContacts(level, ship);
        int thrusterCount = ThrusterRequirements.countThrusters(blockIds);
        int coreNeighbors = countCoreNeighbors(wheelPos, ship);

        // ═══════════════════════════════════════════════════════════════════
        // BUG EDGE VALIDATION
        // A BUG is on the outer edge if at least one of its 6 neighbor
        // positions is NOT part of the ship structure.
        // ═══════════════════════════════════════════════════════════════════
        boolean bugOnEdge = false;
        if (bugCount == 1) {
            BlockPos bugWorldPos = wheelPos.offset(bugDx, bugDy, bugDz);
            bugOnEdge = isOnEdge(bugWorldPos, ship);
        }

        // ═══════════════════════════════════════════════════════════════════
        // BUG YAW CALCULATION
        // The BUG's FACING direction maps directly to yaw:
        //   NORTH = 180°, SOUTH = 0°, WEST = 90°, EAST = -90° (Minecraft convention)
        // ═══════════════════════════════════════════════════════════════════
        float bugYawDeg = directionToYaw(bugFacing);

        return new StructureScan(wheelPos, blocks, invalidAttachments, contactPoints,
                thrusterCount > 0, thrusterCount, coreNeighbors,
                bugCount, bugOnEdge, bugYawDeg);
    }

    /**
     * Checks whether the given position is on the outer edge of the structure.
     * A block is on the edge if at least one adjacent position is NOT part of the ship.
     */
    private static boolean isOnEdge(BlockPos pos, LongSet ship) {
        for (Direction d : Direction.values()) {
            if (!ship.contains(pos.relative(d).asLong())) {
                return true; // At least one neighbor is outside → edge
            }
        }
        return false; // All 6 neighbors are ship blocks → inside
    }

    /**
     * Converts a Minecraft Direction to entity yaw in degrees.
     * Minecraft yaw: SOUTH=0, WEST=90, NORTH=180, EAST=-90/270
     */
    private static float directionToYaw(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case NORTH -> 180.0f;
            case EAST -> -90.0f;
            default -> 0.0f; // UP/DOWN shouldn't happen
        };
    }

    private static int countWorldContacts(ServerLevel level, LongSet ship) {
        int contacts = 0;
        CollisionContext ctx = CollisionContext.empty();

        for (var it = ship.iterator(); it.hasNext(); ) {
            BlockPos p = BlockPos.of(it.nextLong());

            for (Direction d : Direction.values()) {
                // DOWN is not a contact: ships placed on the ground can always
                // take off vertically. Only horizontal and UP contacts block flight.
                if (d == Direction.DOWN) continue;

                BlockPos n = p.relative(d);
                if (ship.contains(n.asLong())) continue;

                BlockState ns = level.getBlockState(n);
                if (ns.isAir()) continue;

                if (!ns.getCollisionShape(level, n, ctx).isEmpty()) {
                    contacts++;
                }
            }
        }
        return contacts;
    }

    private static int countCoreNeighbors(BlockPos wheelPos, LongSet ship) {
        int neighbors = 0;
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            BlockPos adjacent = wheelPos.relative(direction);
            if (ship.contains(adjacent.asLong())) {
                neighbors++;
            }
        }
        return neighbors;
    }
}
