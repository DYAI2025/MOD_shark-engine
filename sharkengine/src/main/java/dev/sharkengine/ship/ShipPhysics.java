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
 *   <li>Maximum speed calculation based on weight (total ship mass, AIR-023)</li>
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
     * Calculates the maximum speed based on the ship's total mass.
     *
     * <p>AIR-023: previously took a raw block count and independently
     * hardcoded the 20/40/60 weight-category thresholds here, duplicating
     * (and risking drift from) {@link WeightCategory}'s own thresholds. Now
     * derives entirely from {@link WeightCategory#fromMass(int)} — one
     * authority for the mass→speed mapping, consumed identically by physics
     * here, {@code ShipEntity.getWeightCategory()}, and the client HUD
     * ({@code FuelHudOverlay}), so they cannot disagree.</p>
     *
     * <p>Mass is the sum of every part's {@code VehiclePartDefinition.mass()}
     * ({@link dev.sharkengine.ship.part.ShipPartAnalyzer}), not block count —
     * see {@link WeightCategory} for the current mass thresholds.</p>
     *
     * @param mass Total mass of the ship (summed part masses)
     * @return Maximum speed in blocks/sec, or 0 if there is no ship or it is too heavy
     */
    public static float calculateMaxSpeed(int mass) {
        if (mass <= 0) {
            return 0.0f; // No mass = no ship
        }
        return WeightCategory.fromMass(mass).getMaxSpeed();
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
        return checkCollision(level, pos, null, 0f);
    }

    /**
     * Checks collisions for the full blueprint footprint at yaw 0 (no rotation).
     * Kept for callers that don't track an effective yaw; prefer the 4-arg
     * overload for anything that can turn.
     */
    public static boolean checkCollision(Level level, BlockPos pos, ShipBlueprint blueprint) {
        return checkCollision(level, pos, blueprint, 0f);
    }

    /**
     * Checks collisions for the full blueprint footprint, rotated by the
     * ship's current effective yaw.
     *
     * <p>P0 hotfix (2026-07-12, live playtest of AIR-011 found the mod
     * effectively unflyable): two independent bugs made this check both
     * over- and under-sensitive. (1) The old solidity test was
     * {@code !isAir()}, which is true for grass, flowers, torches, signs,
     * carpets — none of which have a real hitbox — so a freshly-launched
     * ship near ground level (guaranteed to be near such decoration)
     * false-triggered a "collision" almost immediately, and the response
     * ({@code setDeltaMovement(Vec3.ZERO)}, no escape) left it permanently
     * stuck. Fixed: test the block's actual collision shape.
     * (2) Offsets were never rotated by the ship's live yaw, only by its
     * frozen build-time orientation — so the probed volume silently
     * diverged from the ship's real, visually-rotated shape (B2) the
     * moment it turned, which the "GTA-style" continuous A/D turning makes
     * happen almost immediately after launch. Fixed: rotate every offset
     * via {@link ShipTransform#rotateOffset} before testing it, using the
     * same {@link ShipTransform#effectiveYaw} the renderer (AIR-011) uses,
     * so collision finally matches what the player sees.
     *
     * @param effectiveYawDeg {@code ShipTransform.effectiveYaw(entity yaw, blueprint.assemblyYaw())}
     */
    public static boolean checkCollision(Level level, BlockPos pos, ShipBlueprint blueprint, float effectiveYawDeg) {
        if (level == null || pos == null) {
            return false;
        }

        List<BlockVector> offsets = collectRotatedOffsets(blueprint, effectiveYawDeg);
        return hasCollision(
                BlockVector.from(pos),
                offsets,
                vector -> {
                    BlockPos blockPos = vector.toBlockPos();
                    return !level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty();
                }
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
    
    private static List<BlockVector> collectRotatedOffsets(ShipBlueprint blueprint, float effectiveYawDeg) {
        if (blueprint == null || blueprint.blocks().isEmpty()) {
            return Collections.emptyList();
        }

        List<BlockVector> offsets = new ArrayList<>(blueprint.blocks().size());
        for (ShipBlueprint.ShipBlock block : blueprint.blocks()) {
            double[] rotated = ShipTransform.rotateOffset(block.dx(), block.dz(), effectiveYawDeg);
            int rx = (int) Math.round(rotated[0]);
            int rz = (int) Math.round(rotated[1]);
            offsets.add(new BlockVector(rx, block.dy(), rz));
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
