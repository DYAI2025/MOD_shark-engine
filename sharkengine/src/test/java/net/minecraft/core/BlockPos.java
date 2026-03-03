package net.minecraft.core;

/**
 * Minimal stub for BlockPos – used in unit tests to avoid loading the full
 * Minecraft registry. Only the constructor and the fields needed by
 * StructureScan / ShipBlueprint are provided.
 */
public class BlockPos {
    private final int x;
    private final int y;
    private final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    @Override
    public String toString() {
        return "BlockPos{" + x + ", " + y + ", " + z + "}";
    }
}
