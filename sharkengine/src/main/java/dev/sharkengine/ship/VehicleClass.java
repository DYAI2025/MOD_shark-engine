package dev.sharkengine.ship;

/**
 * Enum representing the different vehicle classes in Shark Engine.
 * Each class has unique movement characteristics and physics.
 * 
 * <p>Vehicle classes are used to determine:</p>
 * <ul>
 *   <li>Movement type (water, air, land)</li>
 *   <li>Required components (e.g., thrusters for AIR)</li>
 *   <li>Physics behavior (e.g., height penalty for AIR)</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public enum VehicleClass {
    /**
     * Water vehicles: boats, submarines
     * Moves on/under water, requires water-based components
     */
    WATER("Wasser"),
    
    /**
     * Air vehicles: aircraft, spaceships, UFOs
     * Moves in 3D airspace, requires thrusters or wings
     */
    AIR("Luft"),
    
    /**
     * Land vehicles: cars, motorcycles, trucks
     * Moves on ground, requires wheels or tracks
     */
    LAND("Land");
    
    /**
     * Display name for this vehicle class in German
     */
    private final String displayName;
    
    /**
     * Constructor for VehicleClass
     * 
     * @param displayName The display name for this class
     */
    VehicleClass(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Returns the display name for this vehicle class
     * 
     * @return The German display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
