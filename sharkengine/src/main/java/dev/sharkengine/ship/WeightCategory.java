package dev.sharkengine.ship;

import org.jetbrains.annotations.Nullable;

/**
 * Enum representing weight categories for air vehicles based on block count.
 * Each category has a maximum speed limit and optional warning message.
 * 
 * <p>Weight categories affect flight capability:</p>
 * <ul>
 *   <li>LIGHT (1-20 blocks): Full speed (30 blocks/sec)</li>
 *   <li>MEDIUM (21-40 blocks): Reduced speed (20 blocks/sec)</li>
 *   <li>HEAVY (41-60 blocks): Slow speed (10 blocks/sec) + warning</li>
 *   <li>OVERLOADED (61+ blocks): Cannot fly (0 blocks/sec) + error</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public enum WeightCategory {
    /**
     * Light vehicles: 1-20 blocks
     * Maximum speed: 30 blocks/sec
     * No warning displayed
     */
    LIGHT(0, 20, 30.0f, null),
    
    /**
     * Medium vehicles: 21-40 blocks
     * Maximum speed: 20 blocks/sec
     * No warning displayed
     */
    MEDIUM(21, 40, 20.0f, null),
    
    /**
     * Heavy vehicles: 41-60 blocks
     * Maximum speed: 10 blocks/sec
     * Warning: "Achtung: Schiff wird langsam"
     */
    HEAVY(41, 60, 10.0f, "§eAchtung: Schiff wird langsam"),
    
    /**
     * Overloaded vehicles: 61+ blocks
     * Maximum speed: 0 blocks/sec (cannot fly)
     * Error: "⚠️ Zu schwer zum Fliegen!"
     */
    OVERLOADED(61, Integer.MAX_VALUE, 0.0f, "§c⚠️ Zu schwer zum Fliegen!");
    
    /**
     * Minimum block count for this category
     */
    private final int min;
    
    /**
     * Maximum block count for this category
     */
    private final int max;
    
    /**
     * Maximum speed allowed for this category in blocks/sec
     */
    private final float maxSpeed;
    
    /**
     * Warning message to display (null if none)
     */
    @Nullable
    private final String warning;
    
    /**
     * Constructor for WeightCategory
     * 
     * @param min Minimum block count
     * @param max Maximum block count
     * @param maxSpeed Maximum speed in blocks/sec
     * @param warning Warning message or null
     */
    WeightCategory(int min, int max, float maxSpeed, @Nullable String warning) {
        this.min = min;
        this.max = max;
        this.maxSpeed = maxSpeed;
        this.warning = warning;
    }
    
    /**
     * Returns the minimum block count for this category
     * 
     * @return Minimum blocks
     */
    public int getMin() {
        return min;
    }
    
    /**
     * Returns the maximum block count for this category
     * 
     * @return Maximum blocks
     */
    public int getMax() {
        return max;
    }
    
    /**
     * Returns the maximum speed for this category
     * 
     * @return Speed in blocks/sec
     */
    public float getMaxSpeed() {
        return maxSpeed;
    }
    
    /**
     * Returns the warning message for this category
     * 
     * @return Warning message or null if none
     */
    @Nullable
    public String getWarning() {
        return warning;
    }
    
    /**
     * Determines the weight category from the block count
     * 
     * @param blockCount Number of blocks in the ship
     * @return The appropriate WeightCategory
     */
    public static WeightCategory fromBlockCount(int blockCount) {
        if (blockCount <= LIGHT.max) return LIGHT;
        if (blockCount <= MEDIUM.max) return MEDIUM;
        if (blockCount <= HEAVY.max) return HEAVY;
        return OVERLOADED;
    }
    
    /**
     * Checks if a vehicle in this category can fly
     * 
     * @return true if maxSpeed > 0, false if overloaded
     */
    public boolean canFly() {
        return maxSpeed > 0.0f;
    }
}
