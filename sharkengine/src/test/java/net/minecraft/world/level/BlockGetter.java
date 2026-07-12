package net.minecraft.world.level;

/**
 * Minimal stub for BlockGetter – exists only so that ShipPhysics.class
 * (which references it as a parameter type of
 * BlockState.getCollisionShape(BlockGetter, BlockPos)) resolves during JVM
 * class verification. No test constructs or calls through this type
 * directly; it only needs to be present on the test classpath.
 */
public interface BlockGetter {
}
