package net.minecraft.world.level.block.state;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Minimal stub for BlockState – used in unit tests to avoid loading the full
 * Minecraft block registry. None of the assembly-validation tests inspect
 * the actual block state; they only care about counts and positions.
 *
 * <p>getCollisionShape exists only so that ShipPhysics.class (which calls
 * it in checkCollision's solidity predicate) resolves during JVM class
 * verification – see the sibling BlockGetter/VoxelShape stubs. No unit
 * test invokes it directly.</p>
 */
public class BlockState {
    public VoxelShape getCollisionShape(BlockGetter level, BlockPos pos) {
        return new VoxelShape(true);
    }
}
