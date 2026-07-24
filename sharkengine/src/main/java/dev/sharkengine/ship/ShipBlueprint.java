package dev.sharkengine.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Blueprint of a ship structure.
 * Contains the origin position and all blocks with their relative positions.
 * Used for rendering, serialization, and block count calculation.
 *
 * <p><b>{@code assemblyYaw}</b> (AIR-015, schema v2): the yaw the BUG block's
 * {@code FACING} resolved to at scan time (see
 * {@code ShipAssemblyService.directionToYaw}: SOUTH=0, WEST=90, NORTH=180,
 * EAST=-90), captured here so rendering/collision/disassembly — which only
 * see the blueprint, not the entity — can compute the effective rotation
 * ({@code entityYaw - assemblyYaw}, {@link ShipTransform#effectiveYaw}) that
 * fixes B1/B2. Blueprints loaded from pre-AIR-015 (v1) NBT have no persisted
 * {@code AssemblyYaw}; {@link #fromNbt} defaults it to 0 in that case, and
 * the caller ({@code ShipEntity.readAdditionalSaveData}) is responsible for
 * patching it to the entity's own persisted {@code BugYaw} via
 * {@link #withAssemblyYaw} immediately afterward — see that method's design
 * note for why 0 alone would be wrong for non-SOUTH-facing legacy ships.</p>
 *
 * <p><b>{@code seatAnchors}</b> (REQ-006, schema v3+): the resolved seat anchor
 * position(s), each a single block offset relative to {@code origin} — for AIR release 1
 * this is always empty, exactly one entry (the pilot seat only), or the pilot seat entry
 * plus one or more copilot entries (REQ-009/T07). Deliberately a {@code List}, not a
 * nullable single field: {@code T07} (copilot seat) extends this same representation with
 * additional entries tagged by {@link SeatRole} rather than introducing a second, parallel
 * data structure. Like {@code blocks}, these are raw (unrotated) offsets captured at
 * assembly time — {@code ShipTransform.rotateOffset} (AIR-010's single rotation authority)
 * is how callers recover the current world position after the ship has since rotated in
 * flight, exactly like {@link ShipBlock} offsets.</p>
 *
 * @author Shark Engine Team
 * @version 4.0 (Copilotensitz)
 */
public record ShipBlueprint(
    BlockPos origin,
    List<ShipBlock> blocks,
    int blockCount,  // NEW: Cached block count for performance
    int schemaVersion,
    float assemblyYaw,
    List<SeatAnchor> seatAnchors
) {
    /** Current NBT schema version written by {@link #toNbt}. */
    public static final int CURRENT_SCHEMA_VERSION = 4;

    /**
     * Represents a single block in the ship structure
     *
     * @param dx X offset from origin
     * @param dy Y offset from origin
     * @param dz Z offset from origin
     * @param state Block state
     */
    public record ShipBlock(int dx, int dy, int dz, BlockState state) {}

    /**
     * Which seat a {@link SeatAnchor} represents (REQ-009/T07). {@code PILOT} is the sole
     * role that existed through schema v3 (T06); {@code COPILOT} is new in schema v4.
     */
    public enum SeatRole { PILOT, COPILOT }

    /**
     * A single seat's anchor position, relative to {@code origin} (REQ-006), tagged with
     * which seat it represents (REQ-009/T07). Raw (unrotated) offset, captured at assembly
     * time — same convention as {@link ShipBlock}.
     *
     * @param dx X offset from origin
     * @param dy Y offset from origin
     * @param dz Z offset from origin
     * @param role which seat this anchor represents
     */
    public record SeatAnchor(int dx, int dy, int dz, SeatRole role) {
        /**
         * Convenience constructor for every pre-T07 call site (production and the
         * already-committed T06 GameTests/production code), all of which predate the
         * copilot seat and always mean the pilot's own anchor — defaults {@code role} to
         * {@link SeatRole#PILOT} so those 3-arg call sites keep compiling with their
         * original meaning unchanged, rather than requiring every one of them to be
         * rewritten just to add an always-PILOT role argument.
         */
        public SeatAnchor(int dx, int dy, int dz) {
            this(dx, dy, dz, SeatRole.PILOT);
        }
    }

    /** Normalizes {@code seatAnchors} to a non-null, immutable list — never {@code null}. */
    public ShipBlueprint {
        seatAnchors = seatAnchors == null ? List.of() : List.copyOf(seatAnchors);
    }

    /**
     * Convenience constructor that calculates blockCount automatically and
     * defaults schemaVersion/assemblyYaw/seatAnchors for callers that don't care about
     * orientation or seating (e.g. tests). Use {@link #withAssemblyYaw}/
     * {@link #withSeatAnchors} to set real values after construction.
     */
    public ShipBlueprint(BlockPos origin, List<ShipBlock> blocks) {
        this(origin, blocks, blocks.size(), CURRENT_SCHEMA_VERSION, 0f, List.of());
    }

    /**
     * Convenience constructor with an explicit blockCount (existing callers)
     * that defaults schemaVersion/assemblyYaw/seatAnchors the same way as the 2-arg form.
     */
    public ShipBlueprint(BlockPos origin, List<ShipBlock> blocks, int blockCount) {
        this(origin, blocks, blockCount, CURRENT_SCHEMA_VERSION, 0f, List.of());
    }

    /**
     * Returns a copy of this blueprint with only {@code assemblyYaw} changed.
     * This is the only supported way to set a non-default assembly yaw —
     * kept separate from the constructors so existing 2-/3-arg call sites
     * never need to change, and so the legacy-fallback patch in
     * {@code ShipEntity.readAdditionalSaveData} reads as an explicit,
     * intentional override rather than a sixth constructor argument easy to
     * pass in the wrong order.
     */
    public ShipBlueprint withAssemblyYaw(float newAssemblyYaw) {
        return new ShipBlueprint(origin, blocks, blockCount, schemaVersion, newAssemblyYaw, seatAnchors);
    }

    /**
     * Returns a copy of this blueprint with only {@code seatAnchors} changed (REQ-006).
     * Kept separate from the constructors for the same reason {@link #withAssemblyYaw} is —
     * an explicit, intentional override rather than another positional constructor argument.
     */
    public ShipBlueprint withSeatAnchors(List<SeatAnchor> newSeatAnchors) {
        return new ShipBlueprint(origin, blocks, blockCount, schemaVersion, assemblyYaw, newSeatAnchors);
    }

    /**
     * Serializes the blueprint to NBT format.
     * 
     * @return CompoundTag containing origin, blocks, and blockCount
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", schemaVersion);
        tag.putFloat("AssemblyYaw", assemblyYaw);
        tag.putInt("OriginX", origin.getX());
        tag.putInt("OriginY", origin.getY());
        tag.putInt("OriginZ", origin.getZ());
        tag.putInt("BlockCount", blockCount);  // informational only — fromNbt no longer trusts this, see below

        ListTag blockList = new ListTag();
        for (ShipBlock block : blocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("dx", block.dx());
            blockTag.putInt("dy", block.dy());
            blockTag.putInt("dz", block.dz());
            blockTag.put("State", NbtUtils.writeBlockState(block.state()));
            blockList.add(blockTag);
        }
        tag.put("Blocks", blockList);

        // REQ-006 (schema v3): seat anchors. Always written (possibly empty) so a v3+
        // reader never has to guess between "v3 blueprint with zero anchors" and
        // "pre-v3 blueprint that never had the concept" — fromNbt still handles a
        // missing tag defensively for true legacy (v1/v2) data, see below.
        ListTag seatAnchorList = new ListTag();
        for (SeatAnchor anchor : seatAnchors) {
            CompoundTag anchorTag = new CompoundTag();
            anchorTag.putInt("dx", anchor.dx());
            anchorTag.putInt("dy", anchor.dy());
            anchorTag.putInt("dz", anchor.dz());
            // REQ-009/T07 (schema v4): always written explicitly (never relying on the
            // enum's default) so a v4+ reader never has to guess a role for an anchor.
            anchorTag.putString("Role", anchor.role().name());
            seatAnchorList.add(anchorTag);
        }
        tag.put("SeatAnchors", seatAnchorList);
        return tag;
    }

    /**
     * Deserializes a blueprint from NBT format.
     * 
     * @param tag NBT tag containing blueprint data
     * @param registries Registry lookup provider
     * @return Deserialized ShipBlueprint
     */
    public static ShipBlueprint fromNbt(CompoundTag tag, HolderLookup.Provider registries) {
        BlockPos origin = new BlockPos(
                tag.getInt("OriginX"),
                tag.getInt("OriginY"),
                tag.getInt("OriginZ")
        );

        HolderGetter<Block> blockGetter = registries.lookupOrThrow(Registries.BLOCK);
        ListTag blockList = tag.getList("Blocks", Tag.TAG_COMPOUND);
        List<ShipBlock> blocks = new ArrayList<>(blockList.size());
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag blockTag = blockList.getCompound(i);
            blocks.add(new ShipBlock(
                    blockTag.getInt("dx"),
                    blockTag.getInt("dy"),
                    blockTag.getInt("dz"),
                    NbtUtils.readBlockState(blockGetter, blockTag.getCompound("State"))
            ));
        }
        
        // AIR-015 fix: ALWAYS derive blockCount from the actual block list.
        // Previously this trusted a stored "BlockCount" NBT value over the
        // real list size, so a stale/corrupt tag could feed wrong weight/
        // speed stats into ShipEntity.applyBlueprintStats() without any
        // error — the list itself is the only source of truth.
        int blockCount = blocks.size();

        int schemaVersion = tag.contains("SchemaVersion") ? tag.getInt("SchemaVersion") : 1;
        // v1 NBT has no AssemblyYaw; default to 0. This is only correct for
        // a SOUTH-facing BUG (directionToYaw() == 0) — the caller
        // (ShipEntity.readAdditionalSaveData) MUST patch this via
        // withAssemblyYaw(bugYawDeg) for every other facing, immediately
        // after this call, before the blueprint is used for rendering.
        float assemblyYaw = tag.contains("AssemblyYaw") ? tag.getFloat("AssemblyYaw") : 0f;

        // REQ-006 (schema v3, NFR-004 conservative migration): pre-v3 (v1/v2) NBT never
        // wrote "SeatAnchors" at all. Missing tag defaults to an empty list rather than
        // failing to load — the same conservative-default treatment AIR-015 already gave
        // AssemblyYaw for pre-v2 saves — never a fabricated/guessed anchor position.
        List<SeatAnchor> seatAnchors = new ArrayList<>();
        if (tag.contains("SeatAnchors")) {
            ListTag seatAnchorList = tag.getList("SeatAnchors", Tag.TAG_COMPOUND);
            for (int i = 0; i < seatAnchorList.size(); i++) {
                CompoundTag anchorTag = seatAnchorList.getCompound(i);
                // REQ-009/T07 (schema v4): pre-v4 (v3) NBT never wrote "Role" -- every anchor
                // written before the copilot seat existed always meant the pilot's own anchor,
                // so a missing tag defaults to PILOT, the same conservative-default treatment
                // AssemblyYaw/SeatAnchors themselves already got for their own pre-existing
                // schema bumps above. Never a fabricated/guessed COPILOT role.
                SeatRole role = anchorTag.contains("Role")
                        ? SeatRole.valueOf(anchorTag.getString("Role"))
                        : SeatRole.PILOT;
                seatAnchors.add(new SeatAnchor(
                        anchorTag.getInt("dx"),
                        anchorTag.getInt("dy"),
                        anchorTag.getInt("dz"),
                        role
                ));
            }
        }

        return new ShipBlueprint(origin, blocks, blockCount, schemaVersion, assemblyYaw, seatAnchors);
    }
}
