package net.minecraft.world.level.block.state;

/**
 * Minimal stub for BlockState – used in unit tests to avoid loading the full
 * Minecraft block registry. None of the assembly-validation tests inspect
 * the actual block state; they only care about counts and positions.
 */
public class BlockState {
    // Intentionally empty – only the type reference is needed in test scope.
}
