package net.minecraft.util;

/**
 * Hand-written test stub (same pattern as the BlockPos/BlockState/VoxelShape stubs): unit tests
 * run without a real Minecraft runtime, and {@code TrailColor} implements this interface to be
 * usable as a BlockState property value. Only the single method the production enum implements.
 */
public interface StringRepresentable {
    String getSerializedName();
}
