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
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public record ShipBlueprint(
    BlockPos origin,
    List<ShipBlock> blocks,
    int blockCount,  // NEW: Cached block count for performance
    int schemaVersion,
    float assemblyYaw
) {
    /** Current NBT schema version written by {@link #toNbt}. */
    public static final int CURRENT_SCHEMA_VERSION = 2;

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
     * Convenience constructor that calculates blockCount automatically and
     * defaults schemaVersion/assemblyYaw for callers that don't care about
     * orientation (e.g. tests). Use {@link #withAssemblyYaw} to set a real
     * assembly yaw after construction.
     */
    public ShipBlueprint(BlockPos origin, List<ShipBlock> blocks) {
        this(origin, blocks, blocks.size(), CURRENT_SCHEMA_VERSION, 0f);
    }

    /**
     * Convenience constructor with an explicit blockCount (existing callers)
     * that defaults schemaVersion/assemblyYaw the same way as the 2-arg form.
     */
    public ShipBlueprint(BlockPos origin, List<ShipBlock> blocks, int blockCount) {
        this(origin, blocks, blockCount, CURRENT_SCHEMA_VERSION, 0f);
    }

    /**
     * Returns a copy of this blueprint with only {@code assemblyYaw} changed.
     * This is the only supported way to set a non-default assembly yaw —
     * kept separate from the constructors so existing 2-/3-arg call sites
     * never need to change, and so the legacy-fallback patch in
     * {@code ShipEntity.readAdditionalSaveData} reads as an explicit,
     * intentional override rather than a fifth constructor argument easy to
     * pass in the wrong order.
     */
    public ShipBlueprint withAssemblyYaw(float newAssemblyYaw) {
        return new ShipBlueprint(origin, blocks, blockCount, schemaVersion, newAssemblyYaw);
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

        return new ShipBlueprint(origin, blocks, blockCount, schemaVersion, assemblyYaw);
    }
}
