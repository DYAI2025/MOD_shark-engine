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
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public record ShipBlueprint(
    BlockPos origin, 
    List<ShipBlock> blocks,
    int blockCount  // NEW: Cached block count for performance
) {
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
     * Convenience constructor that calculates blockCount automatically
     */
    public ShipBlueprint(BlockPos origin, List<ShipBlock> blocks) {
        this(origin, blocks, blocks.size());
    }

    /**
     * Serializes the blueprint to NBT format.
     * 
     * @return CompoundTag containing origin, blocks, and blockCount
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("OriginX", origin.getX());
        tag.putInt("OriginY", origin.getY());
        tag.putInt("OriginZ", origin.getZ());
        tag.putInt("BlockCount", blockCount);  // NEW: Save block count

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
        
        // NEW: Read blockCount from NBT (with fallback to list size)
        int blockCount = tag.contains("BlockCount") ? tag.getInt("BlockCount") : blocks.size();
        return new ShipBlueprint(origin, blocks, blockCount);
    }
}
