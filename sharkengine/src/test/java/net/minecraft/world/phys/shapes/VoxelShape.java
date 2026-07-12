package net.minecraft.world.phys.shapes;

/**
 * Minimal stub for VoxelShape – exists only so that ShipPhysics.class
 * (which calls BlockState.getCollisionShape(...).isEmpty()) resolves during
 * JVM class verification. No unit test constructs a real VoxelShape; the
 * collision-detection behavior itself is exercised via
 * ShipPhysics.hasCollision(BlockVector, List, Predicate), which never
 * touches this class.
 */
public class VoxelShape {
    private final boolean empty;

    public VoxelShape() {
        this(true);
    }

    public VoxelShape(boolean empty) {
        this.empty = empty;
    }

    public boolean isEmpty() {
        return empty;
    }
}
