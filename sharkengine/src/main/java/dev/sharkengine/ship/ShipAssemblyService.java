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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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

        // ═══════════════════════════════════════════════════════════════════
        // SPAWN PREFLIGHT (REQ-021/T15): if an already-spawned ship intersects this
        // structure's footprint, committing would nest one ship inside another and the
        // block-removal below would mutate the world for a doomed spawn (RISK-004's
        // duplication shape). Checked READ-ONLY before the first setBlock, same
        // preflight-before-mutation discipline as REQ-014's materializeForEdit. Scoped
        // to ShipEntity deliberately: players/mobs standing on the structure are
        // ordinary bystanders (a waiting copilot must not block assembly).
        // ═══════════════════════════════════════════════════════════════════
        AABB footprint = null;
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            AABB cell = new AABB(wheelPos.offset(block.dx(), block.dy(), block.dz()));
            footprint = footprint == null ? cell : footprint.minmax(cell);
        }
        if (footprint != null
                && !level.getEntities(ModEntities.SHIP, footprint.inflate(0.5), e -> true).isEmpty()) {
            return new AssembleResult("message.sharkengine.assembly_fail_spawn_blocked", "");
        }

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
     * <p><b>Close/reset path (REQ-014/T14):</b> a ship whose Edit Mode is already open rejects a
     * concurrent second open attempt with {@link EditModeDistanceGate.Reason#REJECTED_CONFLICT},
     * exactly as intended ("conflict-free" precondition, see {@link ShipEntity#editModeActive}'s
     * javadoc) — this is no longer a permanent lockout: {@link #commitEdit} calls {@link
     * ShipEntity#exitEditMode()} at the end of every commit attempt (success or rejection), after
     * which this method can open Edit Mode again.</p>
     *
     * <p><b>REQ-014/T14 remediation — materialization ("nothing to actually edit" gap):</b> on
     * {@link EditModeDistanceGate.Reason#ACCEPTED}, this method now ALSO calls {@link
     * #materializeForEdit} before sending the preview — the missing counterpart to pre-launch
     * building (a player places blocks, {@link #tryAssemble} scans/clears them) that makes {@link
     * #commitEdit}'s "valid" path reachable by an actual player action for the first time. See
     * {@link #materializeForEdit}'s own javadoc for exactly what gets placed and how rotation is
     * handled.</p>
     *
     * <p><b>REQ-014/T14 remediation round 6 (IMPORTANT finding, fixed): a footprint position
     * obstructed since the ship parked is a REJECTION, not a silent overwrite.</b> {@link
     * #materializeForEdit} returns {@code false} (having placed nothing at all — see its own
     * javadoc) when something occupies a target position that should be air. On that outcome this
     * method rolls back the {@code editModeActive} flag {@link ShipEntity#tryEnterEditMode} already
     * flipped {@code true} (via {@link ShipEntity#exitEditMode()} — the same close/reset path
     * {@link #commitEdit} uses, so Edit Mode is never left "stuck" open), skips the builder preview
     * entirely, and reports {@link EditModeDistanceGate.Reason#REJECTED_FOOTPRINT_OBSTRUCTED} —
     * which both of {@code ShipEntity#interact}'s edit-mode-requesting branches already handle
     * generically (any non-{@code ACCEPTED} reason gets echoed to the player as {@code
     * message.sharkengine.edit_mode_rejected}), so no new client/message plumbing was needed for
     * this to be player-visible.</p>
     */
    public static EditModeDistanceGate.Reason openEditMode(ShipEntity ship, ServerPlayer player) {
        EditModeDistanceGate.Reason reason = ship.tryEnterEditMode(player);
        if (EditModeDistanceGate.isAccepted(reason)) {
            if (ship.level() instanceof ServerLevel level) {
                if (!materializeForEdit(level, ship)) {
                    ship.exitEditMode();
                    return EditModeDistanceGate.Reason.REJECTED_FOOTPRINT_OBSTRUCTED;
                }
            }
            openEditModePreview(ship, player);
        }
        return reason;
    }

    /**
     * REQ-013/REQ-014 (T14 remediation): materializes {@code ship}'s CURRENT blueprint into real
     * world blocks at its CURRENT position/orientation. Called exactly once, by {@link
     * #openEditMode} on every {@link EditModeDistanceGate.Reason#ACCEPTED} open — both of T13's
     * player-reachable entry gestures ({@code ShipEntity#interact}'s mounted-pilot and
     * dismounted-but-anchored-and-nearby branches) route through that one method, so both get this
     * for free from a single choke point, not two independently-maintained call sites.
     *
     * <p><b>The gap this closes:</b> {@link #tryAssemble} clears EVERY scanned block, including
     * the Steering Wheel itself, to {@link Blocks#AIR} at launch — an already-launched ship's
     * structure exists ONLY as {@link ShipEntity#getBlueprint()} data, never as real world blocks.
     * Without this method, a player entering Edit Mode saw an empty area with nothing to build
     * against; {@link #commitEdit}'s "valid" path could only be exercised by test-only {@code
     * level.setBlock} calls placing an entire replica structure, something no real player action
     * produced. Once materialized, ORDINARY vanilla block placement/breaking by the player near
     * the ship IS the editing mechanism — no custom in-game block-editing UI is needed, mirroring
     * this mod's existing pre-launch-build precedent exactly (REQ-013's own text: "...eine
     * Erweiterung erlauben").</p>
     *
     * <p><b>Rotation (AIR-010, single authority — no second rotation path):</b> a ship's live
     * {@code getYRot()} can differ from the yaw it was originally assembled at ({@code
     * blueprint.assemblyYaw()}) — it turns freely in flight and can land facing any direction, and
     * {@link EditModeDistanceGate} only requires "stationary" (speed ~0), never "facing its
     * original assembly direction." Placing the blueprint's raw, un-rotated {@code (dx, dy, dz)}
     * offsets here would materialize the OLD, pre-turn footprint at the ship's CURRENT position —
     * visibly disagreeing with the ALREADY-rotated footprint {@link ShipPhysics}/{@code
     * ShipEntityRenderer} use for this exact ship (the B1/B2 class of bug AIR-010 exists to
     * prevent). Offsets are therefore rotated by {@code
     * ShipTransform.effectiveYaw(ship.getYRot(), blueprint.assemblyYaw())} through {@link
     * ShipTransform#rotateOffset} via {@link #rotatedWorldPositions} — the SAME computation
     * {@link ShipEntity#updatePhysics()} already feeds {@link ShipPhysics#checkCollision}, not a
     * second, independently-invented formula.</p>
     *
     * <p><b>Snapped to the nearest cardinal ({@link ShipTransform#snapToCardinal}):</b> real
     * Minecraft blocks are axis-aligned; a raw, non-cardinal {@code effectiveYaw} (the ship can
     * stop mid-turn at any float degree — nothing snaps {@code getYRot()} itself) cannot rotate an
     * integer block lattice without rounding gaps/overlaps that would break the BFS connectivity
     * {@link #scanStructure} depends on the moment materialization ran. Snapping first, then
     * rotating, guarantees every offset lands on an exact grid cell — see {@link
     * #snappedEffectiveYaw}.</p>
     *
     * <p><b>Only the BUG's {@code FACING} is corrected for rotation; every OTHER directional block
     * is placed with its ORIGINAL stored {@link BlockState} verbatim (disclosed, not a new gap):
     * </b> {@link #scanStructure} derives the NEXT {@code assemblyYaw} solely from whatever {@code
     * FACING} the BUG block reads back as on commit. Leaving the BUG's facing un-rotated while its
     * POSITION is rotated would silently double-apply the rotation on every future render (the
     * freshly re-scanned blueprint's "raw" offsets are already the CURRENT, rotated layout, so an
     * {@code assemblyYaw} that still claims the OLD pre-turn facing would make {@link
     * ShipTransform#effectiveYaw} rotate an already-rotated footprint a second time) — correcting
     * it (via {@link Rotation#rotate(Direction)}, vanilla's own directional-rotation primitive, not
     * a hand-rolled formula) is required for internal consistency, not optional polish. Every OTHER
     * directional block (e.g. a thruster's nozzle) keeps its stored facing unrotated — purely
     * cosmetic (nothing in assembly/physics reads it), and the EXACT SAME already-disclosed gap
     * {@code ShipEntity#disassemble()} has always had (see {@code
     * docs/plans/aircraft-extension-implementation.md}'s AIR-012 entry: "disassembly rotation...
     * NOT done") — not a new one introduced here, and out of this task's scope to fix everywhere
     * at once.</p>
     *
     * <p><b>REQ-014/T14 remediation round 6 (IMPORTANT finding, fixed): preflight-checked, not
     * blind, against a footprint position that stopped being air while the ship sat parked.</b> A
     * parked ship's footprint is ordinary real-world air from the moment {@link #tryAssemble}
     * cleared it (at launch, or at the end of any prior {@link #commitEdit}) until Edit Mode is
     * next opened — nothing in this codebase prevents SURVIVAL gameplay from placing something into
     * that space in the meantime (a player building nearby, a falling block landing, sand/gravel
     * settling, in principle even a player-placed chest full of items). This method used to call
     * {@code level.setBlock} unconditionally for every position with no {@code isAir()}/
     * {@code canBeReplaced()} check first — {@code setBlock} never drops items, so that would
     * silently destroy whatever was there, without warning or compensation. This method now runs a
     * READ-ONLY preflight over every target position first (mirroring this class's own existing "is
     * this position safely available" idiom — the {@code state.isAir()} checks {@link
     * #scanStructure}/{@link #countWorldContacts} already use, not a new pattern): if ANY target
     * position is not air, NOTHING is placed at all (not even the positions that WERE clear — a
     * partial materialization would desync the world from {@code preEditBlueprint}, breaking
     * {@link #commitEdit}'s own footprint-clearing union) and this method returns {@code false}.
     * {@link #openEditMode} treats {@code false} as a rejection — see its own javadoc for the
     * close/reset handling. Returns {@code true} on an outright missing blueprint too (nothing to
     * materialize is not an obstruction) and on ordinary successful placement.</p>
     */
    public static boolean materializeForEdit(ServerLevel level, ShipEntity ship) {
        ShipBlueprint blueprint = ship.getBlueprint();
        if (blueprint == null) {
            return true;
        }
        BlockPos shipPos = ship.blockPosition();
        float shipYawDeg = ship.getYRot();
        List<BlockPos> positions = rotatedWorldPositions(blueprint, shipPos, shipYawDeg);

        // REQ-014/T14 remediation round 6 (IMPORTANT finding, fixed): a parked ship's footprint is
        // real-world AIR from the moment #tryAssemble cleared it until Edit Mode is next opened (see
        // this method's own "gap this closes" paragraph below) -- ordinary survival gameplay can
        // place something into that space in the meantime (a player builds nearby, water/sand
        // flows in, a falling block lands, ...). Blindly overwriting every position with
        // level.setBlock (as this method used to, unconditionally) would silently destroy whatever
        // is there first, with zero item drops (setBlock never drops) -- in the worst case, a
        // chest full of a player's items, gone without a trace or a warning. Mirrors this class's
        // OWN existing "is this position safely available" discipline (#countWorldContacts /
        // #scanStructure's own state.isAir() checks below) rather than inventing a new one: every
        // target position is checked for isAir() FIRST, as a read-only preflight over the WHOLE
        // list, before a single level.setBlock call runs -- so a blocked position aborts the ENTIRE
        // materialization, not just skips that one block (a partial materialization would silently
        // desync the world from preEditBlueprint, breaking #commitEdit's own footprint-clearing
        // union). {@link #openEditMode} treats a {@code false} return as a rejection (rolls back
        // the {@code editModeActive} flag {@link ShipEntity#tryEnterEditMode} already flipped, and
        // never sends the builder preview) rather than silently proceeding into an inconsistent
        // state.
        for (BlockPos pos : positions) {
            if (!level.getBlockState(pos).isAir()) {
                return false;
            }
        }

        Rotation rotation = rotationForYaw(snappedEffectiveYaw(blueprint, shipYawDeg));

        List<ShipBlueprint.ShipBlock> blocks = blueprint.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            BlockState state = blocks.get(i).state();
            if (state.hasProperty(BugBlock.FACING)) {
                state = state.setValue(BugBlock.FACING, rotation.rotate(state.getValue(BugBlock.FACING)));
            }
            level.setBlock(positions.get(i), state, 3);
        }
        return true;
    }

    /**
     * REQ-014/T14: {@code blueprint}'s own {@code assemblyYaw} vs. {@code shipYawDeg}'s effective
     * rotation ({@link ShipTransform#effectiveYaw}), snapped to the nearest cardinal ({@link
     * ShipTransform#snapToCardinal}) — the one shared computation {@link #materializeForEdit} and
     * {@link #rotatedWorldPositions} both use, so the two are never able to independently drift
     * apart into two different rotation answers for the same (blueprint, position, yaw) triple.
     */
    private static int snappedEffectiveYaw(ShipBlueprint blueprint, float shipYawDeg) {
        float effectiveYaw = ShipTransform.effectiveYaw(shipYawDeg, blueprint.assemblyYaw());
        return ShipTransform.snapToCardinal(effectiveYaw);
    }

    /**
     * REQ-014/T14: the world position each of {@code blueprint.blocks()} currently occupies (or
     * would occupy if (re-)materialized) given {@code shipPos}/{@code shipYawDeg} — same order,
     * same size as {@code blueprint.blocks()}, so callers may zip the two lists by index. Shared by
     * {@link #materializeForEdit} (placement) and {@link #commitEdit}'s rejection path (knowing
     * exactly what to clear again) — one computation, not two independently-maintained copies of
     * the rotation math.
     */
    private static List<BlockPos> rotatedWorldPositions(ShipBlueprint blueprint, BlockPos shipPos, float shipYawDeg) {
        int snappedYaw = snappedEffectiveYaw(blueprint, shipYawDeg);
        List<BlockPos> positions = new ArrayList<>(blueprint.blocks().size());
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            double[] rotated = ShipTransform.rotateOffset(block.dx(), block.dz(), snappedYaw);
            int rx = (int) Math.round(rotated[0]);
            int rz = (int) Math.round(rotated[1]);
            positions.add(shipPos.offset(rx, block.dy(), rz));
        }
        return positions;
    }

    /**
     * REQ-014/T14 remediation round 3 (security-review BLOCKER, RISK-004 third-party case): the
     * world positions {@code ship}'s CURRENT materialized footprint occupies right now — the exact
     * same {@link #rotatedWorldPositions} computation {@link #materializeForEdit} used to place
     * those blocks and {@link #commitEdit}'s rejection path uses to clear them, reused here
     * (not duplicated) so {@link EditModeBlockProtection} can look up exactly what a ship's active
     * Edit Mode session covers without inventing a second, independently-computed footprint
     * definition. Returns an empty list when {@code ship} has no blueprint (nothing materialized)
     * — callers should treat that as "protects nothing," not an error.
     *
     * <p>Public (unlike {@link #rotatedWorldPositions} itself) specifically so a caller outside
     * this class — a block-break/place event listener — can reuse it; still takes only {@code
     * ship} (deriving blueprint/position/yaw from it), so callers never risk passing a stale or
     * mismatched triple of those three values by hand.</p>
     */
    public static List<BlockPos> materializedFootprint(ShipEntity ship) {
        ShipBlueprint blueprint = ship.getBlueprint();
        if (blueprint == null) {
            return List.of();
        }
        return rotatedWorldPositions(blueprint, ship.blockPosition(), ship.getYRot());
    }

    /**
     * REQ-014/T14: maps a snapped-to-cardinal degree value (0/90/180/270, see {@link
     * #snappedEffectiveYaw}) to vanilla's {@link Rotation} enum — ground-truthed the same way
     * {@link ShipTransform}'s own class javadoc documents ({@code rotateOffset(+90)} is
     * bytecode-verified identical to {@code BlockPos.rotate(Rotation.CLOCKWISE_90)}), so this
     * mapping and {@link ShipTransform#rotateOffset} agree on which direction is "positive."
     */
    private static Rotation rotationForYaw(int normalizedDeg) {
        return switch (normalizedDeg) {
            case 90 -> Rotation.CLOCKWISE_90;
            case 180 -> Rotation.CLOCKWISE_180;
            case 270 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
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
     * <p><b>REQ-014/T14 wiring:</b> the payload's {@code wheelPos} is {@code ship.blockPosition()}
     * and {@code sessionId} is always {@code null} (no REQ-003 build-session concept applies to an
     * edit-mode commit). {@link #findEditModeShip} is what turns that {@code wheelPos} back into
     * WHICH ship entity a commit should apply to — {@code ModNetworking}'s
     * {@code BuilderAssembleC2SPayload} handler tries {@link #findEditModeShip} first and routes to
     * {@link #commitEdit} on a hit, falling back to the pre-launch {@link
     * dev.sharkengine.ship.BuildSessionGate#tryAssemble} flow (which still safely no-ops/rejects a
     * null session) only when no edit-mode ship matches.</p>
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
     * REQ-014/T14 (AC-014): result of a {@link #commitEdit} attempt — the same {@code
     * (translationKey, arg)} shape {@link AssembleResult} already established for {@link
     * #tryAssemble}, not a second, differently-shaped result type.
     */
    public record EditCommitResult(String translationKey, Object arg) {
        /** Whether this result represents a genuinely committed structural change. */
        public boolean isSuccess() {
            return "message.sharkengine.edit_commit_ok".equals(translationKey);
        }
    }

    /**
     * REQ-014/T14: resolves the {@link ShipEntity} (if any) that {@code player}'s {@code wheelPos}
     * refers to for the purpose of committing an Edit Mode change. {@code wheelPos} here is exactly
     * what {@link #openEditModePreview} handed the client as the preview's echo-back anchor ({@code
     * ship.blockPosition()} at the moment Edit Mode opened) — see that method's javadoc.
     *
     * <p>Fails closed on every axis: returns {@code null} (not some other player's ship) unless a
     * {@link ShipEntity} exists whose OWN current {@code blockPosition()} exactly equals {@code
     * wheelPos} (not merely "nearby" — an edit commit must target the exact ship the preview was
     * opened against), whose {@link ShipEntity#isEditModeActive()} is {@code true} (no ship with a
     * closed/never-opened edit session is ever returned), AND whose {@link ShipEntity#isPilot}
     * accepts {@code player} (a bystander's {@code BuilderAssembleC2SPayload} sharing someone else's
     * wheelPos coordinate can never resolve to that other player's active edit session). {@link
     * #commitEdit} re-checks {@code isEditModeActive}/{@code isPilot} again on the returned ship
     * regardless — this lookup's own filtering is defense-in-depth, not the sole gate.</p>
     *
     * @return the matching edit-mode ship, or {@code null} if this is not a REQ-014 edit commit at
     *         all (the caller should fall back to the ordinary pre-launch assembly flow)
     */
    public static ShipEntity findEditModeShip(ServerLevel level, BlockPos wheelPos, ServerPlayer player) {
        List<ShipEntity> nearby = level.getEntities(ModEntities.SHIP, new AABB(wheelPos).inflate(1), e -> true);
        for (ShipEntity candidate : nearby) {
            if (candidate.blockPosition().equals(wheelPos)
                    && candidate.isEditModeActive()
                    && candidate.isPilot(player)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * REQ-014/AC-014 (T14): the real "Beenden des Edit-Modus" (ending Edit Mode) production entry
     * point — validates the structure {@code player} has been editing against the SAME AIR-policy
     * criteria {@link #tryAssemble} itself enforces ({@link StructureScan#canAssemble()}), commits
     * it atomically on success, or rejects and rolls the WORLD back to a clean baseline on
     * failure — {@code ship.getBlueprint()} itself is provably untouched either way until success
     * is certain. Reuses {@link #scanStructure}'s existing BFS scan verbatim — the SAME
     * preflight-validate-then-mutate discipline {@link #tryAssemble} already established in this
     * class — rather than inventing a second validation mechanism.
     *
     * <p><b>What "atomic" means here, updated for materialization (T14 remediation): the
     * BLUEPRINT, not the world, is the atomic unit.</b> Before {@link #materializeForEdit} existed,
     * an Edit Mode session had no real world footprint at all, so "zero {@code level.setBlock}
     * calls on rejection" and "the ship's blueprint is untouched on rejection" were the SAME
     * falsifiable claim. They no longer are: {@link #openEditMode} now places real blocks into the
     * world the moment Edit Mode opens, so by the time a player commits, the world already
     * necessarily differs from its pre-edit state (that is the whole point — real blocks are what
     * the player edited). Demanding "the world is byte-identical to before" on rejection would
     * therefore be demanding the IMPOSSIBLE (materialization itself is a mutation), not a
     * meaningful atomicity guarantee. What actually matters for AC-014 ("die vorherige Struktur
     * vollständig erhalten") and RISK-004 ("Blöcke duplizieren oder verlieren") is: (1) {@code
     * ship.setBlueprint} — the ship's own persisted source of truth — is reached ONLY past {@code
     * scan.canAssemble()}, exactly as before (still provably true: every {@code
     * ship.setBlueprint} call in this method is textually and control-flow-reachably below that
     * check); and (2) a REJECTED commit leaves NO dangling world state that matches neither the
     * old nor the new structure — see the next paragraph for how.</p>
     *
     * <p><b>Rejection now clears the world instead of leaving it untouched (design decision, see
     * this task's own instructions to make this explicit rather than silently pick one):</b> the
     * counter-thesis test that used to prove "zero world mutation" on rejection ({@code
     * AtomicEditReassemblyGameTest}) has been rewritten to prove the NEW invariant instead: on
     * rejection, this method clears every block {@link #materializeForEdit} originally placed
     * (recomputed from {@code ship}'s still-pre-edit blueprint/position/yaw — see {@link
     * #rotatedWorldPositions}) UNION whatever {@code scan.blocks()} just found genuinely CONNECTED
     * to {@code shipPos} (covers additions the player made elsewhere in the structure that the
     * original footprint alone wouldn't include). {@code ship.getBlueprint()} itself is left
     * COMPLETELY UNCHANGED (the pre-edit value) — never patched, never partially applied. The
     * alternative considered and REJECTED: restoring the OLD valid blocks into the world on a
     * rejected commit (instead of clearing to air). That would leave a non-editing ship with real
     * world blocks sitting at its position — a state that, outside of an active Edit Mode session,
     * has never existed anywhere else in this codebase (the entire premise this task's gap report
     * opens with is "an already-launched ship's structure exists ONLY as data ... never as real
     * world blocks") — and would require {@link #openEditMode}'s NEXT invocation to somehow detect
     * and clear that leftover restoration before re-materializing, a second bespoke path this
     * class doesn't need. Clearing to air on rejection returns the world to the EXACT SAME
     * baseline every other non-editing ship already has; the player must re-request Edit Mode
     * (T13's real triggers) to try again, which re-materializes the SAME still-unchanged blueprint
     * fresh — a rejected edit is therefore not resumable in-place by design, not by omission.</p>
     *
     * <p><b>SUCCESS clears the exact same union, not just {@code newBlueprint.blocks()} (T14
     * remediation round 6, BLOCKER fix — RISK-004 block-duplication exploit): a committed structure
     * can legitimately be a strict SUBSET of what was materialized.</b> A player can break the one
     * connector joining a non-load-bearing branch to the rest of the (already-materialized)
     * structure without breaking anything else: {@link #scanStructure}'s BFS from {@code shipPos}
     * then simply never reaches that now-disconnected branch, so it is silently absent from {@code
     * scan.blocks()}/{@code newBlueprint.blocks()} — while the wheel-connected remainder can still
     * independently satisfy {@code scan.canAssemble()} and commit successfully. If the success
     * branch cleared only {@code newBlueprint.blocks()}'s world positions (as it used to, the bug
     * this remediation fixes), the detached branch's real blocks would never be reached by the
     * clearing loop at all — they would stand in the world FOREVER as free, ownerless material,
     * while {@code ship.setBlueprint(newBlueprint)} simultaneously drops their mass/parts from the
     * ship's data for good: a real, reproducible duplication bug, not a hypothetical one. The fix is
     * to clear the SAME union the rejection branch above already computes correctly — {@link
     * #rotatedWorldPositions} of the still-pre-edit {@code preEditBlueprint} (every position
     * materialization originally touched) UNION {@code newBlueprint.blocks()}'s world positions
     * (covers anything newly added that ended up connected) — before {@code ship.setBlueprint} runs.
     * Falsifying-test contract: {@code AtomicEditReassemblyGameTest#detachedBranchClearedOnSuccessfulCommit}.</p>
     *
     * <p><b>{@link ShipEntity#editModeActive} close/reset path (T14's other job, per that field's
     * own javadoc):</b> {@link ShipEntity#exitEditMode()} is called UNCONDITIONALLY at the end of
     * this method for both outcomes — success AND rejection — never leaving Edit Mode "stuck"
     * open. A rejected commit does NOT get a special "still open, keep trying" state: the player
     * must re-request Edit Mode (walk back within OQ-001's 5-block range, right-click again, or
     * remount) before their next attempt. This is a deliberate choice over "leave it active so the
     * player can silently retry without re-requesting": leaving it active on rejection would
     * resurrect T13's originally-disclosed "permanently stuck" failure mode for the specific case
     * of a player who abandons a broken edit without ever producing a valid structure — {@link
     * ShipEntity#tryEnterEditMode}'s own conflict check ({@code editModeActive} already {@code
     * true}) would then reject every future open attempt forever on that one ship, with no other
     * path to clear it. Resetting on every outcome guarantees Edit Mode can always be re-requested,
     * at the (minor, disclosed) UX cost of one extra right-click to retry after a rejected edit —
     * the same "try again from scratch" shape every other rejected gate in this codebase already
     * has (a rejected {@link #tryAssemble} or {@code BuildSessionGate} attempt doesn't leave any
     * special half-open state behind either).</p>
     *
     * <p>Early precondition failures (not editing / not the pilot) return immediately WITHOUT
     * calling {@link ShipEntity#exitEditMode()} — a bystander sending a bogus commit must not be
     * able to terminate the ACTUAL pilot's still-open edit session out from under them.</p>
     *
     * @param level  the ship's current level
     * @param ship   the edit-mode-active ship being committed
     * @param player the requester; must be {@code ship}'s assigned pilot
     */
    public static EditCommitResult commitEdit(ServerLevel level, ShipEntity ship, ServerPlayer player) {
        if (!ship.isEditModeActive()) {
            return new EditCommitResult("message.sharkengine.edit_commit_not_active", "");
        }
        if (!ship.isPilot(player)) {
            return new EditCommitResult("message.sharkengine.edit_commit_not_pilot", "");
        }

        BlockPos shipPos = ship.blockPosition();
        // Captured BEFORE any scan/mutation below -- this is the ship's genuinely pre-edit
        // blueprint, the one #materializeForEdit placed into the world when Edit Mode opened
        // (the ship cannot have moved/turned since then; ShipEntity#tick() freezes movement and
        // turning whenever editModeActive is true -- see that method's own comment).
        ShipBlueprint preEditBlueprint = ship.getBlueprint();

        // ═══════════════════════════════════════════════════════════════════
        // PREFLIGHT (READ-ONLY): scanStructure() never calls level.setBlock -- see its own body.
        // Everything up to and including this call only reads world state; nothing has been
        // mutated yet.
        // ═══════════════════════════════════════════════════════════════════
        StructureScan scan = scanStructure(level, shipPos);

        if (!scan.canAssemble()) {
            int issueCount = scan.issues().size();

            // ROLLBACK (REQ-014/T14 remediation, RISK-004 mitigation): clear every real world
            // block this abandoned edit attempt could have touched -- see this method's own
            // class-level javadoc ("Rejection now clears the world...") for the full reasoning.
            // ship.setBlueprint is NEVER called on this branch -- preEditBlueprint remains the
            // ship's live blueprint, completely unchanged.
            Set<BlockPos> toClear = new HashSet<>(
                    rotatedWorldPositions(preEditBlueprint, shipPos, ship.getYRot()));
            for (ShipBlueprint.ShipBlock block : scan.blocks()) {
                toClear.add(shipPos.offset(block.dx(), block.dy(), block.dz()));
            }
            for (BlockPos pos : toClear) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }

            ship.exitEditMode();
            return new EditCommitResult("message.sharkengine.edit_commit_invalid", issueCount);
        }

        ShipBlueprint newBlueprint = scan.toBlueprint();

        // ═══════════════════════════════════════════════════════════════════
        // MUTATION -- only reached once every AIR-policy condition above has passed. Mirrors
        // #tryAssemble's own clear-scanned-blocks-then-adopt-blueprint order exactly (same idiom,
        // not a second one).
        //
        // REQ-014/T14 remediation round 6 (BLOCKER, RISK-004 block-duplication exploit): clearing
        // ONLY newBlueprint.blocks()'s world positions is WRONG on its own -- a player can break the
        // single connector joining a non-load-bearing branch to the rest of the materialized
        // structure, leaving that branch BFS-disconnected (scanStructure never reaches it, so it is
        // simply absent from newBlueprint.blocks()) while the WHEEL-connected remainder still
        // satisfies scan.canAssemble(). The branch's real blocks are still standing in the world at
        // this point; ship.setBlueprint(newBlueprint) below permanently drops their mass/parts from
        // the ship's own data. Clearing only newBlueprint.blocks() would leave those branch blocks
        // in the world FOREVER -- free, ownerless, duplicated material, exactly the "Blöcke
        // duplizieren" failure AC-014/RISK-004 exist to rule out. The fix mirrors the REJECTION
        // branch above EXACTLY: clear the UNION of (1) every position #materializeForEdit originally
        // placed for the pre-edit structure (recomputed from preEditBlueprint/shipPos/ship.getYRot()
        // via #rotatedWorldPositions -- preEditBlueprint itself is never mutated) and (2) every
        // position newBlueprint.blocks() now occupies (already covered by (1) for anything the
        // player didn't touch, but also covers any NEW block the player added elsewhere that ended
        // up connected). This is the same union computation as the rejection branch, not a second,
        // independently-invented one.
        // ═══════════════════════════════════════════════════════════════════
        Set<BlockPos> toClear = new HashSet<>(
                rotatedWorldPositions(preEditBlueprint, shipPos, ship.getYRot()));
        for (ShipBlueprint.ShipBlock block : newBlueprint.blocks()) {
            toClear.add(shipPos.offset(block.dx(), block.dy(), block.dz()));
        }
        for (BlockPos pos : toClear) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        ship.setBlueprint(newBlueprint);
        ship.exitEditMode();

        return new EditCommitResult("message.sharkengine.edit_commit_ok", newBlueprint.blockCount());
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
