package dev.sharkengine.ship;

import dev.sharkengine.ship.part.VehicleBalance;
import org.jetbrains.annotations.Nullable;

/**
 * Enum representing weight categories for air vehicles based on total mass
 * (AIR-023: switched from block count to mass, per
 * {@code docs/AIRCRAFT_CONCEPT_V2.md} §4 — different parts weigh different
 * amounts, so a ship with few heavy parts and one with many light parts can
 * carry the same block count but very different real weight).
 * Each category has a maximum speed limit and optional warning message.
 *
 * <p>Weight categories affect flight capability. Thresholds are mass values,
 * sourced from {@link VehicleBalance} (single authority — not duplicated here
 * or in {@link dev.sharkengine.ship.ShipPhysics}). Raised 4x on 2026-07-13
 * (originally 30/60/90) so larger builds stay flyable:</p>
 * <ul>
 *   <li>LIGHT (mass 0-120): Full speed (30 blocks/sec)</li>
 *   <li>MEDIUM (mass 121-240): Reduced speed (20 blocks/sec)</li>
 *   <li>HEAVY (mass 241-360): Slow speed (10 blocks/sec) + warning</li>
 *   <li>OVERLOADED (mass 361+): Cannot fly (0 blocks/sec) + error</li>
 * </ul>
 *
 * @author Shark Engine Team
 * @version 2.1 (2026-07-13: thresholds raised 4x for larger builds)
 */
public enum WeightCategory {
    /**
     * Light vehicles: mass 0-120
     * Maximum speed: 30 blocks/sec
     * No warning displayed
     */
    LIGHT(0, VehicleBalance.LIGHT_MAX_MASS, 30.0f, null),

    /**
     * Medium vehicles: mass 121-240
     * Maximum speed: 20 blocks/sec
     * No warning displayed
     */
    MEDIUM(VehicleBalance.LIGHT_MAX_MASS + 1, VehicleBalance.MEDIUM_MAX_MASS, 20.0f, null),

    /**
     * Heavy vehicles: mass 241-360
     * Maximum speed: 10 blocks/sec
     * Warning: "Achtung: Schiff wird langsam"
     */
    HEAVY(VehicleBalance.MEDIUM_MAX_MASS + 1, VehicleBalance.HEAVY_MAX_MASS, 10.0f, "§eAchtung: Schiff wird langsam"),

    /**
     * Overloaded vehicles: mass 361+
     * Maximum speed: 0 blocks/sec (cannot fly)
     * Error: "⚠️ Zu schwer zum Fliegen!"
     */
    OVERLOADED(VehicleBalance.HEAVY_MAX_MASS + 1, Integer.MAX_VALUE, 0.0f, "§c⚠️ Zu schwer zum Fliegen!");

    /**
     * Minimum mass for this category
     */
    private final int min;

    /**
     * Maximum mass for this category
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
     * @param min Minimum mass for this category
     * @param max Maximum mass for this category
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
     * Returns the minimum mass for this category
     *
     * @return Minimum mass
     */
    public int getMin() {
        return min;
    }

    /**
     * Returns the maximum mass for this category
     *
     * @return Maximum mass
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
     * Determines the weight category from total ship mass (AIR-023).
     *
     * <p>Mass is the sum of every part's {@code VehiclePartDefinition.mass()}
     * (see {@link dev.sharkengine.ship.part.ShipPartAnalyzer}), not the raw
     * block count — a ship built from a few heavy parts can be OVERLOADED
     * with far fewer blocks than one built from many light parts.</p>
     *
     * @param mass Total mass of the ship (summed part masses)
     * @return The appropriate WeightCategory
     */
    public static WeightCategory fromMass(int mass) {
        if (mass <= LIGHT.max) return LIGHT;
        if (mass <= MEDIUM.max) return MEDIUM;
        if (mass <= HEAVY.max) return HEAVY;
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
