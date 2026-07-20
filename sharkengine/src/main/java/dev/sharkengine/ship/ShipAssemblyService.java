package dev.sharkengine.ship;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.ModTags;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.net.BuilderPreviewS2CPayload;
import dev.sharkengine.ship.part.AssemblyIssue;
import dev.sharkengine.ship.part.PartRole;
import dev.sharkengine.ship.part.ShipPartAnalyzer;
import dev.sharkengine.ship.part.ShipStats;
import dev.sharkengine.ship.part.VehiclePartRegistry;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                                float bugYawDeg,
                                boolean seatAnchorValid,
                                boolean cockpitVisibilityCompliant,
                                List<ShipBlueprint.SeatAnchor> copilotSeatAnchors) {
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

        /**
         * REQ-005: number of parts whose role is {@link dev.sharkengine.ship.part.PartRole#PILOT_SEAT}
         * in this structure — generalizes {@link #thrusterCount()}'s role-based counting pattern to a
         * DIFFERENT multiplicity rule ("exactly one", not "at least one"). Every matching part is
         * tallied by {@code ShipPartAnalyzer.analyze} (no early "found the first one, stop looking"
         * short-circuit), so two pilot seats anywhere in the structure — adjacent or far apart —
         * are both counted here, not just the first one found.
         */
        public int pilotSeatCount() {
            return stats.pilotSeatCount();
        }

        public boolean canAssemble() {
            return !isEmpty()
                    && invalidAttachments.isEmpty()
                    && contactPoints == 0
                    && stats.hasPropulsion()
                    && stats.pilotSeatCount() == 1
                    && coreNeighbors >= 4
                    && bugCount == 1
                    && bugOnEdge
                    && seatAnchorValid
                    && cockpitVisibilityCompliant;
        }

        public ShipBlueprint toBlueprint() {
            // AIR-015: carry the BUG block's resolved yaw into the blueprint
            // so rendering/collision/disassembly (which only see the
            // blueprint, not this scan) can compute effective rotation.
            ShipBlueprint blueprint = new ShipBlueprint(origin, blocks, blockCount()).withAssemblyYaw(bugYawDeg);
            // REQ-006: only ever populate the ONE deterministic front-of-wheel PILOT anchor
            // computed during the scan (see ShipAssemblyService#frontOffset/#scanStructure) —
            // never a fallback/alternate position. When it's invalid, no PILOT entry is added,
            // exactly the "zero SeatAnchor entries" contract AC-006 requires for the pilot seat
            // (not a silently-chosen alternate position).
            List<ShipBlueprint.SeatAnchor> anchors = new ArrayList<>();
            if (seatAnchorValid) {
                int[] offset = frontOffset(bugYawDeg);
                anchors.add(new ShipBlueprint.SeatAnchor(
                        offset[0], 0, offset[1], ShipBlueprint.SeatRole.PILOT));
            }
            // REQ-009/T07: additive to the pilot's own entry (or to an empty list if the pilot
            // anchor is invalid) — every COPILOT_SEAT-role part found during the scan, each at
            // its own placed position, not a second/parallel seat-tracking structure.
            anchors.addAll(copilotSeatAnchors);
            blueprint = blueprint.withSeatAnchors(anchors);
            return blueprint;
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
            if (stats.pilotSeatCount() == 0) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.NO_PILOT_SEAT));
            } else if (stats.pilotSeatCount() > 1) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.MULTI_PILOT_SEAT, stats.pilotSeatCount()));
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
            // REQ-006: only meaningful once exactly one BUG resolves an unambiguous facing —
            // with zero/multiple BUGs the facing itself is undefined, so NO_BUG/MULTI_BUG above
            // already cover that case and this would just be redundant noise.
            if (bugCount == 1 && !seatAnchorValid) {
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.SEAT_ANCHOR_INVALID));
            } else if (bugCount == 1 && !cockpitVisibilityCompliant) {
                // REQ-007/AC-007 (T08 remediation): only meaningful once the seat anchor itself
                // is valid -- SEAT_ANCHOR_INVALID above already covers "no coherent seat position
                // exists yet", so reporting visibility on top of that would be redundant noise.
                issues.add(AssemblyIssue.of(AssemblyIssue.Code.COCKPIT_VISIBILITY_INSUFFICIENT));
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

        // ═══════════════════════════════════════════════════════════════════
        // PILOT SEAT VALIDATION (REQ-005): exactly one PILOT_SEAT-role part
        // required — zero and more-than-one are both rejected explicitly,
        // with the world left unchanged (no blocks removed, no entity
        // spawned before this point). Role-based count, generalizing the
        // hasThruster()/thrusterCount() pattern above rather than reintroducing
        // ID-comparison counting (see ShipPartAnalyzer/ShipStats).
        // ═══════════════════════════════════════════════════════════════════
        if (scan.pilotSeatCount() == 0) {
            return new AssembleResult("message.sharkengine.assembly_fail_no_pilot_seat", "");
        }
        if (scan.pilotSeatCount() > 1) {
            return new AssembleResult("message.sharkengine.assembly_fail_multi_pilot_seat", scan.pilotSeatCount());
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

        // ═══════════════════════════════════════════════════════════════════
        // SEAT ANCHOR VALIDATION (REQ-006): the pilot seat must occupy the single
        // deterministic block directly in front of the BUG's resolved facing. If that
        // exact position is occupied by a non-seat block, or is otherwise invalid (empty,
        // outside the structure, etc.), assembly fails explicitly here — there is no
        // fallback search for a nearby alternate position anywhere in this class; only
        // this one position (computed by #frontOffset, reusing ShipTransform.rotateOffset,
        // AIR-010's single rotation authority) is ever consulted. World is still
        // unchanged at this point (no blocks removed, no entity spawned).
        // ═══════════════════════════════════════════════════════════════════
        if (!scan.seatAnchorValid()) {
            return new AssembleResult("message.sharkengine.assembly_fail_seat_anchor", "");
        }

        // ═══════════════════════════════════════════════════════════════════
        // COCKPIT VISIBILITY VALIDATION (REQ-007/AC-007, T08 remediation): the pilot seat's
        // resolved position must keep a standard-eye-height occupant concealed below the
        // tallest hull block adjacent to the seat -- otherwise the pilot would be permanently,
        // fully exposed above the hull (AC-007's promise). Rejected explicitly here, same
        // discipline as SEAT_ANCHOR_INVALID above: world is still unchanged at this point (no
        // blocks removed, no entity spawned) -- no fallback reposition, no silent bypass.
        // ═══════════════════════════════════════════════════════════════════
        if (!scan.cockpitVisibilityCompliant()) {
            return new AssembleResult("message.sharkengine.assembly_fail_cockpit_visibility", "");
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
        // REQ-006: scan.toBlueprint() (rather than reconstructing the same fields by hand here)
        // so the preview blueprint carries the exact same SeatAnchor entries (zero or one, never
        // a fallback position) that a real assembly attempt against this same scan would produce.
        ShipBlueprint blueprint = scan.toBlueprint();

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

    /**
     * REQ-013/AC-013 (T13): server-side production entry point for reopening the builder menu on
     * an already-assembled, already-launched {@link ShipEntity} once REQ-012's edit-mode gate
     * ({@link ShipEntity#tryEnterEditMode}) accepts {@code player}'s request. Delegates the
     * accept/reject decision entirely to that gate — this method adds exactly one thing on top of
     * it: on {@link EditModeDistanceGate.Reason#ACCEPTED}, it opens the builder preview populated
     * with the ship's structure (see {@link #openEditModePreview}). {@link
     * ShipEntity#tryEnterEditMode} itself deliberately stays free of any network/UI concern (see
     * its own javadoc: "opening the actual builder menu... is REQ-013/T13's scope, not this
     * method's") — this method is that scope, mirroring how {@link
     * dev.sharkengine.ship.BuildSessionGate#tryAssemble} layers session authorization on top of
     * {@link #tryAssemble} rather than folding it into that method itself.
     *
     * <p><b>No close/reset path exists yet:</b> see {@link ShipEntity#editModeActive}'s and {@link
     * ShipEntity#tryEnterEditMode}'s javadoc. This method inherits that limitation unchanged — a
     * ship whose Edit Mode was already opened once (by any caller, through any of the entry
     * points wired to this method) permanently rejects every later call with {@link
     * EditModeDistanceGate.Reason#REJECTED_CONFLICT}, until REQ-014/T14 ships a real close path.
     * This is disclosed, not silently absorbed — do not add a workaround reset here.</p>
     */
    public static EditModeDistanceGate.Reason openEditMode(ShipEntity ship, ServerPlayer player) {
        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(player);
        if (EditModeDistanceGate.isAccepted(reason)) {
            openEditModePreview(ship, player);
        }
        return reason;
    }

    /**
     * REQ-013/AC-013 (T13): sends {@code player} a builder preview of {@code ship}'s CURRENT
     * structure. Unlike {@link #openBuilderPreview} (which re-scans raw world blocks via BFS for a
     * not-yet-launched structure), an already-launched ship has no world blocks left to scan — its
     * own {@link ShipEntity#getBlueprint()} field IS the single live source of truth for "what does
     * this vehicle currently look like", the same field {@link #tryAssemble} itself populates and
     * every other legitimate mutator ({@code ShipEntity#readAdditionalSaveData}'s NBT load, and any
     * future REQ-014 edit-commit) fully REPLACES via {@link ShipEntity#setBlueprint} — never a
     * second, independently-tracked copy that could drift out of sync with it.
     *
     * <p><b>Falsifying-test contract (test-plan REQ-013 counter-thesis, {@code
     * dev.sharkengine.gametest.BuilderReopenGameTest}):</b> this method reads {@code
     * ship.getBlueprint()} fresh on EVERY call — never memoized/cached anywhere in this class or on
     * {@code ship} itself beyond that one field — so a reopen always reflects whatever the ship's
     * structure is AT THE MOMENT of reopening, never a stale snapshot from whenever Edit Mode (or
     * the ship itself) was first opened. Do not "optimize" this by caching the built {@link
     * BuilderPreviewS2CPayload} or the resolved {@link ShipBlueprint} across calls — that is exactly
     * the false positive this REQ exists to rule out.</p>
     *
     * <p><b>Known scope gap (T14, not this method's job):</b> the resulting payload's {@code
     * wheelPos} is {@code ship.blockPosition()} and {@code sessionId} is always {@code null} — there
     * is no REQ-014 "edit session" id concept yet, and this payload's {@code wheelPos} field
     * (designed for the pre-launch, world-block flow) does not by itself uniquely identify WHICH
     * ship entity a future commit should apply to. {@code BuilderScreen}'s existing "Assemble" button
     * will need real REQ-014 wiring (a distinct commit path, not {@link
     * dev.sharkengine.ship.BuildSessionGate#tryAssemble}, which safely no-ops/rejects a null session
     * today rather than crashing) before it does anything meaningful against an edit-mode-reopened
     * ship — out of scope here.</p>
     */
    public static void openEditModePreview(ShipEntity ship, ServerPlayer player) {
        ShipBlueprint blueprint = ship.getBlueprint();
        if (blueprint == null) {
            return;
        }

        List<String> blockIds = new ArrayList<>(blueprint.blocks().size());
        int bugCount = 0;
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            blockIds.add(BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).toString());
            if (block.state().is(ModBlocks.BUG)) {
                bugCount++;
            }
        }
        ShipStats stats = ShipPartAnalyzer.analyze(blockIds);
        int coreNeighbors = countCoreNeighborsInBlueprint(blueprint);

        // invalidBlocks/contactPoints: N/A for an already-flying structure — nothing here comes
        // from a world scan, so there is nothing to report as "invalid attachment" or "terrain
        // contact". canAssemble=true reflects that this blueprint, by construction, already passed
        // full structural validation once (either via a real #tryAssemble scan, or an NBT load of
        // previously-valid data) — this is NOT a live re-validation of an in-progress edit, which is
        // REQ-014's job. sessionId=null: see this method's javadoc "Known scope gap".
        BuilderPreviewS2CPayload payload = BuilderPreviewS2CPayload.open(
                ship.blockPosition(),
                blueprint.toNbt(),
                List.of(),
                0,
                true,
                stats.propulsionCount(),
                coreNeighbors,
                bugCount,
                List.of(),
                null
        );
        lastPreviewSent.put(player.getUUID(), payload);
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * REQ-013 helper: how many of {@code blueprint}'s origin's own 4 cardinal neighbors are
     * themselves part of the blueprint — computed purely from the blueprint's own local (dx, dy,
     * dz) offsets (the origin is always local (0,0,0), see {@link #scanStructure}), with no
     * world/level access needed. Mirrors {@link #countCoreNeighbors(BlockPos, LongSet)}'s semantics
     * exactly, but operates on a live {@link ShipBlueprint} instead of an in-progress BFS scan's
     * world-position set — there is no world position left to scan against for an already-launched,
     * currently-flying ship.
     */
    private static int countCoreNeighborsInBlueprint(ShipBlueprint blueprint) {
        Set<BlockPos> localOffsets = new HashSet<>();
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            localOffsets.add(new BlockPos(block.dx(), block.dy(), block.dz()));
        }
        int neighbors = 0;
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            if (localOffsets.contains(BlockPos.ZERO.relative(direction))) {
                neighbors++;
            }
        }
        return neighbors;
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
        // REQ-009/T07: every COPILOT_SEAT-role part's own offset, collected as they're found —
        // unlike the pilot seat's single deterministic front-of-wheel position (T06), the
        // copilot seat's anchor is simply wherever it was actually placed.
        List<ShipBlueprint.SeatAnchor> copilotSeatAnchors = new ArrayList<>();

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
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            blockIds.add(blockId);

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

            // REQ-009/T07: role-based (VehiclePartRegistry), not ID comparison (REQ-S1) —
            // every COPILOT_SEAT-role part becomes its own SeatAnchor, at its own offset.
            if (VehiclePartRegistry.resolve(blockId).role() == PartRole.COPILOT_SEAT) {
                copilotSeatAnchors.add(new ShipBlueprint.SeatAnchor(dx, dy, dz, ShipBlueprint.SeatRole.COPILOT));
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

        // ═══════════════════════════════════════════════════════════════════
        // SEAT ANCHOR VALIDATION (REQ-006): the pilot seat's anchor is deterministically
        // the single block directly in front of the BUG's resolved facing. Only meaningful
        // once exactly one BUG resolves an unambiguous facing (bugCount == 1) -- with
        // zero/multiple BUGs there is no well-defined "front" to check, and assembly
        // already fails separately on NO_BUG/MULTI_BUG in that case.
        //
        // Deliberately reads the ACTUAL world block state at exactly ONE position
        // (wheelPos + frontOffset) and nothing else -- no search, no scan of nearby
        // positions, no "if occupied, try the next side" branch. This is the single
        // source-of-truth check the "no silent fallback" requirement (REQ-006/AC-006)
        // depends on: an occupied-or-invalid front slot must be rejected here, not
        // silently reinterpreted as "look for a seat somewhere else nearby".
        // ═══════════════════════════════════════════════════════════════════
        boolean seatAnchorValid = false;
        // REQ-007/AC-007 (T08 remediation): defaults to true (compliant) exactly like
        // seatAnchorValid's own "nothing to check yet" baseline -- only ever computed below
        // once the seat anchor itself resolves to a real PILOT_SEAT part.
        boolean cockpitVisibilityCompliant = true;
        if (bugCount == 1) {
            int[] frontOffset = frontOffset(bugYawDeg);
            BlockPos seatAnchorPos = wheelPos.offset(frontOffset[0], 0, frontOffset[1]);
            BlockState seatAnchorState = level.getBlockState(seatAnchorPos);
            String seatAnchorId = BuiltInRegistries.BLOCK.getKey(seatAnchorState.getBlock()).toString();
            seatAnchorValid = VehiclePartRegistry.resolve(seatAnchorId).role() == PartRole.PILOT_SEAT;
            // REQ-007/AC-007 (T08 remediation): only meaningful once the pilot seat anchor
            // itself resolves to a real position -- an invalid/missing anchor is already
            // rejected by SEAT_ANCHOR_INVALID, so there is nothing coherent to check yet.
            // Uses CockpitVisibility.STANDARD_PLAYER_EYE_HEIGHT (a fixed constant), NOT the
            // attempting player's live eye height/pose (OQ-003) -- whether a structure can
            // assemble must not depend on who is attempting it or their momentary stance.
            if (seatAnchorValid) {
                double tallestAdjacentHullTopY =
                        tallestAdjacentHullTopY(blocks, frontOffset[0], frontOffset[1]);
                cockpitVisibilityCompliant = !CockpitVisibility.isFullyExposedAboveHull(
                        0.0, CockpitVisibility.STANDARD_PLAYER_EYE_HEIGHT, tallestAdjacentHullTopY);
            }
        }

        return new StructureScan(wheelPos, blocks, invalidAttachments, contactPoints,
                stats, coreNeighbors,
                bugCount, bugOnEdge, bugYawDeg, seatAnchorValid, cockpitVisibilityCompliant,
                copilotSeatAnchors);
    }

    /**
     * REQ-007/AC-007 (T08 remediation): the Y-offset of the top face of the tallest block in
     * {@code blocks} adjacent (one of the 4 horizontal neighbor columns, at any height) to the
     * pilot seat anchor's {@code (anchorDx, anchorDz)} column -- the "hull wall" that could
     * conceal a seated pilot. Falls back to the seat anchor's own level (dy=0, since the pilot
     * SeatAnchor's dy is always 0 -- see {@link StructureScan#toBlueprint()}) when no adjacent
     * block exists, so an unwalled seat is correctly treated as having nothing to hide behind.
     * Mirrors {@code ShipEntity#tallestAdjacentHullTopY} exactly, but operates on this scan's raw
     * {@code ShipBlueprint.ShipBlock} list (pre-blueprint) instead of an assembled {@code
     * ShipBlueprint} -- there is deliberately only ONE such computation shared in spirit between
     * assembly-time gating and mount-time logging, not two independently-maintained copies of
     * the adjacency math.
     */
    private static double tallestAdjacentHullTopY(List<ShipBlueprint.ShipBlock> blocks, int anchorDx, int anchorDz) {
        int tallestAdjacentDy = Integer.MIN_VALUE;
        for (ShipBlueprint.ShipBlock block : blocks) {
            boolean northSouthNeighbor = block.dx() == anchorDx && Math.abs(block.dz() - anchorDz) == 1;
            boolean eastWestNeighbor = block.dz() == anchorDz && Math.abs(block.dx() - anchorDx) == 1;
            if (northSouthNeighbor || eastWestNeighbor) {
                tallestAdjacentDy = Math.max(tallestAdjacentDy, block.dy());
            }
        }
        int effectiveDy = tallestAdjacentDy == Integer.MIN_VALUE ? 0 : tallestAdjacentDy;
        return effectiveDy + 1.0;
    }

    /**
     * REQ-006: the local (dx, dz) offset of "one block directly in front of the BUG's
     * resolved facing", derived by rotating the canonical SOUTH-facing offset (0, 1) through
     * {@link ShipTransform#rotateOffset} by {@code yawDeg} -- the SAME single rotation
     * authority (AIR-010) collision/disassembly/rendering already share, not a second,
     * independently-maintained direction-to-offset mapping. Pure and deterministic: the
     * sole computation of "where is the seat anchor" anywhere in this class -- there is no
     * fallback/alternate-position search calling this (or anything else) in a loop.
     *
     * @return a 2-element array {@code [dx, dz]}, rounded to the nearest integer block delta
     */
    private static int[] frontOffset(float yawDeg) {
        double[] rotated = ShipTransform.rotateOffset(0, 1, yawDeg);
        return new int[] { (int) Math.round(rotated[0]), (int) Math.round(rotated[1]) };
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
