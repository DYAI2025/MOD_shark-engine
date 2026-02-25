package dev.sharkengine.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility class for ship physics calculations.
 * Provides static methods for calculating speed, height penalties, and collision detection.
 * 
 * <p>This class handles:</p>
 * <ul>
 *   <li>Maximum speed calculation based on weight (block count)</li>
 *   <li>Height penalty calculation (thinner air at high altitudes)</li>
 *   <li>Acceleration phase determination from tick count</li>
 *   <li>Fuel consumption calculation per phase</li>
 *   <li>Simple collision detection</li>
 * </ul>
 * 
 * <p>All methods are static - this class cannot be instantiated.</p>
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public final class ShipPhysics {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ShipPhysics() {
        throw new UnsupportedOperationException("ShipPhysics is a utility class and cannot be instantiated");
    }
    
    /**
     * Calculates the maximum speed based on the number of blocks in the ship.
     * 
     * <p>Weight categories:</p>
     * <ul>
     *   <li>1-20 blocks: 30 blocks/sec (LIGHT)</li>
     *   <li>21-40 blocks: 20 blocks/sec (MEDIUM)</li>
     *   <li>41-60 blocks: 10 blocks/sec (HEAVY)</li>
     *   <li>61+ blocks: 0 blocks/sec (OVERLOADED - cannot fly)</li>
     * </ul>
     * 
     * @param blockCount Number of blocks in the ship
     * @return Maximum speed in blocks/sec, or 0 if too heavy
     */
    public static float calculateMaxSpeed(int blockCount) {
        if (blockCount <= 20) {
            return 30.0f;
        } else if (blockCount <= 40) {
            return 20.0f;
        } else if (blockCount <= 60) {
            return 10.0f;
        } else {
            return 0.0f; // Too heavy to fly
        }
    }
    
    /**
     * Calculates the height penalty multiplier based on Y position.
     * Higher altitudes have thinner air, reducing maximum speed.
     * 
     * <p>Height ranges:</p>
     * <ul>
     *   <li>Y=0-100: 100% speed (no penalty)</li>
     *   <li>Y=100-150: 80% speed</li>
     *   <li>Y=150-200: 60% speed</li>
     *   <li>Y=200-256: 40% speed (thin air)</li>
     * </ul>
     * 
     * @param yPos Current Y position of the ship
     * @return Height penalty multiplier (0.4 to 1.0)
     */
    public static float calculateHeightPenalty(float yPos) {
        if (yPos < 100.0f) {
            return 1.0f;
        } else if (yPos < 150.0f) {
            return 0.8f;
        } else if (yPos < 200.0f) {
            return 0.6f;
        } else {
            return 0.4f; // Thin air at high altitudes
        }
    }
    
    /**
     * Determines the current acceleration phase from the elapsed tick count.
     * Delegates to {@link AccelerationPhase#fromTick(int)}.
     * 
     * @param ticks Elapsed ticks since acceleration started (20 ticks = 1 second)
     * @return The current AccelerationPhase
     */
    public static AccelerationPhase calculatePhase(int ticks) {
        return AccelerationPhase.fromTick(ticks);
    }
    
    /**
     * Calculates fuel consumption per second based on the current acceleration phase.
     * 
     * <p>Consumption rates:</p>
     * <ul>
     *   <li>Phase 1-2: 1 energy/sec (low consumption)</li>
     *   <li>Phase 3-4: 2 energy/sec (medium consumption)</li>
     *   <li>Phase 5: 3 energy/sec (high consumption)</li>
     * </ul>
     * 
     * @param phase Current acceleration phase
     * @return Energy consumption per second
     */
    public static int calculateFuelConsumption(AccelerationPhase phase) {
        return switch (phase) {
            case PHASE_1, PHASE_2 -> 1;
            case PHASE_3, PHASE_4 -> 2;
            case PHASE_5 -> 3;
        };
    }
    
    /**
     * Checks if the ship is colliding with any solid blocks at the given position.
     * 
     * @param level The world/level the ship is in
     * @param pos The position to check
     * @return true if collision detected, false otherwise
     */
    public static boolean checkCollision(Level level, BlockPos pos) {
        return checkCollision(level, pos, null);
    }

    /**
     * Checks collisions for the full blueprint footprint.
     * Falls back to a single-block probe when the blueprint is null.
     */
    public static boolean checkCollision(Level level, BlockPos pos, ShipBlueprint blueprint) {
        if (level == null || pos == null) {
            return false;
        }

        List<BlockVector> offsets = collectOffsets(blueprint);
        return hasCollision(
                BlockVector.from(pos),
                offsets,
                vector -> !level.getBlockState(vector.toBlockPos()).isAir()
        );
    }

    static boolean hasCollision(BlockVector origin, List<BlockVector> offsets,
                                Predicate<BlockVector> isSolid) {
        if (origin == null || isSolid == null) {
            return false;
        }

        if (offsets == null || offsets.isEmpty()) {
            return isSolid.test(origin);
        }

        for (BlockVector offset : offsets) {
            if (isSolid.test(origin.add(offset))) {
                return true;
            }
        }
        return false;
    }
    
    private static List<BlockVector> collectOffsets(ShipBlueprint blueprint) {
        if (blueprint == null || blueprint.blocks().isEmpty()) {
            return Collections.emptyList();
        }

        List<BlockVector> offsets = new ArrayList<>(blueprint.blocks().size());
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            offsets.add(new BlockVector(block.dx(), block.dy(), block.dz()));
        }
        return offsets;
    }
    
    /**
     * Calculates the effective speed after applying all modifiers.
     * 
     * @param baseSpeed Base speed from acceleration phase
     * @param heightPenalty Height multiplier (0.4-1.0)
     * @param weightPenalty Weight multiplier (0.0-1.0)
     * @return Final effective speed in blocks/sec
     */
    public static float calculateEffectiveSpeed(float baseSpeed, float heightPenalty, float weightPenalty) {
        return baseSpeed * heightPenalty * weightPenalty;
    }

    /**
     * Lightweight integer vector for collision math to keep tests free of Minecraft classes.
     */
    static final class BlockVector {
        private final int x;
        private final int y;
        private final int z;

        BlockVector(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        static BlockVector from(BlockPos pos) {
            return new BlockVector(pos.getX(), pos.getY(), pos.getZ());
        }

        BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }

        BlockVector add(BlockVector other) {
            if (other == null) {
                return this;
            }
            return new BlockVector(x + other.x, y + other.y, z + other.z);
        }

        int x() { return x; }
        int y() { return y; }
        int z() { return z; }
    }
}
