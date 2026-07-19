package dev.sharkengine.ship;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.ModTags;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.net.BuilderPreviewS2CPayload;
import dev.sharkengine.ship.part.AssemblyIssue;
import dev.sharkengine.ship.part.ShipPartAnalyzer;
import dev.sharkengine.ship.part.ShipStats;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for scanning ship structures, validating BUG placement,
 * and assembling ship entities.
 *
 * <p>Architecture: The BUG block is the sole source of the vehicle's
 * forward direction. Its FACING property defines "forward". Thrusters
 * provide thrust power but have no directional authority.</p>
 *
 * @author Shark Engine Team
 * @version 3.0 (BUG-Frontsystem)
 */
public final class ShipAssemblyService {
    public static final int MAX_BLOCKS = 512;
    public static final int MAX_RADIUS = 32;

    /**
     * REQ-003 test/inspection hook: the most recent {@link BuilderPreviewS2CPayload} sent to each
     * player via {@link #openBuilderPreview} — mirrors {@code TutorialService#lastPopupSent}'s
     * established pattern. Lets GameTests assert on exactly what a given player was actually sent
     * (in particular, whether a session id was echoed back to them) without intercepting the
     * network layer itself. See {@code BuildSessionGate#sessionIdForOwner}'s javadoc for the
     * security property this exists to prove.
     */
    private static final Map<UUID, BuilderPreviewS2CPayload> lastPreviewSent = new HashMap<>();

    private ShipAssemblyService() {}

    public record AssembleResult(String translationKey, Object arg) {
        /**
         * Whether this result represents an actual, structurally-successful assembly (as opposed
         * to any of the {@code assembly_fail_*} rejections). {@link
         * dev.sharkengine.ship.BuildSessionGate#tryAssemble} consumes the REQ-003 build session
         * only when this is {@code true} — see that method's javadoc for why.
         */
        public boolean isSuccess() {
            return "message.sharkengine.assembly_ok".equals(translationKey);
        }
    }

    public record StructureScan(BlockPos origin,
                                List<ShipBlueprint.ShipBlock> blocks,
                                List<BlockPos> invalidAttachments,
                                int contactPoints,
                                ShipStats stats,
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

        /** Role-based replacement for the old ID-comparison {@code hasThruster} (B4). */
        public boolean hasThruster() {
            return stats.hasPropulsion();
        }

        /** Role-based replacement for the old ID-comparison {@code thrusterCount} (B4). */
        public int thrusterCount() {
            return stats.propulsionCount();
        }

        public boolean canAssemble() {
            return !isEmpty()
                    && invalidAttachments.isEmpty()
                    && contactPoints == 0
                    && stats.hasPropulsion()
                    && coreNeighbors >= 4
                    && bugCount == 1
                    && bugOnEdge;
        }

        public ShipBlueprint toBlueprint() {
            // AIR-015: carry the BUG block's resolved yaw into the blueprint
            // so rendering/collision/disassembly (which only see the
            // blueprint, not this scan) can compute effective rotation.
            return new ShipBlueprint(origin, blocks, blockCount()).withAssemblyYaw(bugYawDeg);
        }

        /**
         * Every currently-failing assembly condition, structured (REQ-S3) — unlike
         * {@code tryAssemble()}'s chat-message chain, which stops at the first failing
         * condition, this reports ALL of them at once so the builder preview can list every
         * blocker simultaneously. Mirrors {@link #canAssemble()}'s conditions one-to-one;
         * {@code issues().isEmpty()} iff {@code canAssemble()} is {@code true}.
         */
        public List<AssemblyIssue> issues() {
            List<AssemblyIssue> issues = new ArrayList<>();
            if (isEmpty()) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.EMPTY_STRUCTURE));
                return issues;
            }
            if (!invalidAttachments.isEmpty()) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.INVALID_ATTACHMENTS, invalidAttachments.size()));
            }
            if (contactPoints > 0) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.TERRAIN_CONTACT, contactPoints));
            }
            if (!stats.hasPropulsion()) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.NO_PROPULSION));
            }
            if (coreNeighbors < 4) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.TOO_FEW_CORE_NEIGHBORS, coreNeighbors));
            }
            if (bugCount == 0) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.NO_BUG));
            } else if (bugCount > 1) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.MULTI_BUG, bugCount));
            } else if (!bugOnEdge) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.BUG_INSIDE));
            }
            return issues;
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

        if (scan.coreNeighbors() < 4) {
            return new AssembleResult("message.sharkengine.assembly_fail_core", scan.coreNeighbors());
        }

        // ═══════════════════════════════════════════════════════════════════
        // BUG VALIDATION
        // ═══════════════════════════════════════════════════════════════════
        if (scan.bugCount() == 0) {
            return new AssembleResult("message.sharkengine.assembly_fail_no_bug", "");
        }
        if (scan.bugCount() > 1) {
            return new AssembleResult("message.sharkengine.assembly_fail_multi_bug", scan.bugCount());
        }
        if (!scan.bugOnEdge()) {
            return new AssembleResult("message.sharkengine.assembly_fail_bug_inside", "");
        }

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

        pilot.startRiding(shipEntity, true);
        TutorialService.notifyFlightTips(pilot);

        return new AssembleResult("message.sharkengine.assembly_ok", blueprint.blockCount());
    }

    public static void openBuilderPreview(ServerLevel level, BlockPos wheelPos, ServerPlayer player) {
        StructureScan scan = scanStructure(level, wheelPos);
        ShipBlueprint blueprint = new ShipBlueprint(wheelPos, scan.blocks(), scan.blockCount())
                .withAssemblyYaw(scan.bugYawDeg());

        // REQ-003 (security fix, reviewer-reported): embed the session id bound to this wheel
        // ONLY if `player` is that session's own owner -- sessionIdForOwner returns null for
        // anyone else (or when no session exists), so a non-owner who merely reopens/advances the
        // builder UI at someone ELSE's wheel is never handed that owner's real, still-usable
        // session id. This call path is reached both from ModNetworking's assemble-failure branch
        // (where the caller is always already-authorized) AND from TutorialService's
        // handleAdvanceStage (which has no ownership check of its own) -- sessionIdForOwner is the
        // single choke point that protects both.
        UUID sessionId = BuildSessionGate.sessionIdForOwner(level, wheelPos, player);
        BuilderPreviewS2CPayload payload = BuilderPreviewS2CPayload.open(
                wheelPos,
                blueprint.toNbt(),
                scan.invalidAttachments(),
                scan.contactPoints(),
                scan.canAssemble(),
                scan.thrusterCount(),
                scan.coreNeighbors(),
                scan.bugCount(),
                scan.issues(),
                sessionId
        );
        lastPreviewSent.put(player.getUUID(), payload);
        ServerPlayNetworking.send(player, payload);

        if (scan.canAssemble()) {
            TutorialService.notifyReady(player);
        }
    }

    /**
     * REQ-003 test/inspection hook: see {@link #lastPreviewSent} for why this exists.
     */
    public static BuilderPreviewS2CPayload lastPreviewSent(UUID playerId) {
        return lastPreviewSent.get(playerId);
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
        ShipStats stats = ShipPartAnalyzer.analyze(blockIds);
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
                stats, coreNeighbors,
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
