package dev.sharkengine.ship;

/**
 * Utility class for fuel system calculations.
 * Provides methods for converting wood to energy, calculating flight time, and formatting fuel display.
 * 
 * <p>Fuel system basics:</p>
 * <ul>
 *   <li>1 wood block = 100 energy units</li>
 *   <li>Energy consumption: 1-3 units/sec (depending on acceleration phase)</li>
 *   <li>Maximum fuel tank: 100 energy (expandable in future)</li>
 * </ul>
 * 
 * <p>All methods are static - this class cannot be instantiated.</p>
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public final class FuelSystem {
    
    /**
     * Energy content of one wood block in energy units
     */
    public static final int ENERGY_PER_WOOD = 100;
    
    /**
     * Maximum fuel capacity in energy units
     */
    public static final int MAX_FUEL = 100;
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FuelSystem() {
        throw new UnsupportedOperationException("FuelSystem is a utility class and cannot be instantiated");
    }
    
    /**
     * Converts wood blocks to energy units.
     * 
     * @param woodCount Number of wood blocks
     * @return Energy units (woodCount * 100)
     */
    public static int woodToEnergy(int woodCount) {
        return woodCount * ENERGY_PER_WOOD;
    }
    
    /**
     * Converts energy units to wood blocks (for UI display).
     * 
     * @param energy Energy units
     * @return Equivalent wood blocks (float, may be fractional)
     */
    public static float energyToWood(float energy) {
        return energy / ENERGY_PER_WOOD;
    }
    
    /**
     * Calculates remaining flight time in seconds based on current fuel level and phase.
     * 
     * @param fuelLevel Current fuel level in energy units
     * @param phase Current acceleration phase
     * @return Remaining flight time in seconds
     */
    public static int calculateRemainingFlightTime(int fuelLevel, AccelerationPhase phase) {
        int consumption = ShipPhysics.calculateFuelConsumption(phase);
        if (consumption == 0) {
            return Integer.MAX_VALUE; // No consumption = infinite flight
        }
        return fuelLevel / consumption;
    }
    
    /**
     * Formats the fuel level as a display string with bar graph.
     * 
     * <p>Example output: "§eTreibstoff: [§a████████░░§e] 80%"</p>
     * 
     * @param fuelLevel Current fuel level (0-100)
     * @param maxFuel Maximum fuel capacity
     * @return Formatted display string with Minecraft formatting codes
     */
    public static String formatFuelDisplay(int fuelLevel, int maxFuel) {
        if (maxFuel <= 0) {
            return "§cTreibstoff: ERROR";
        }
        
        int percent = Math.min(100, Math.max(0, (fuelLevel * 100) / maxFuel));
        int bars = percent / 10;
        int emptyBars = 10 - bars;
        
        StringBuilder bar = new StringBuilder();
        bar.append("§eTreibstoff: [");
        
        if (percent > 50) {
            bar.append("§a"); // Green for good fuel
        } else if (percent > 20) {
            bar.append("§e"); // Yellow for warning
        } else {
            bar.append("§c"); // Red for critical
        }
        
        bar.append("█".repeat(bars));
        bar.append("§7");
        bar.append("░".repeat(emptyBars));
        bar.append("§e] ");
        bar.append(percent);
        bar.append("%");
        
        return bar.toString();
    }
    
    /**
     * Formats the remaining flight time as a display string.
     * 
     * @param fuelLevel Current fuel level
     * @param phase Current acceleration phase
     * @return Formatted time string (e.g., "33s" or "∞")
     */
    public static String formatRemainingTime(int fuelLevel, AccelerationPhase phase) {
        int seconds = calculateRemainingFlightTime(fuelLevel, phase);
        
        if (seconds >= 3600) {
            return "∞"; // Effectively infinite
        } else if (seconds >= 60) {
            int minutes = seconds / 60;
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Checks if the fuel level is critically low (below 20%).
     * 
     * @param fuelLevel Current fuel level
     * @param maxFuel Maximum fuel capacity
     * @return true if below 20%, false otherwise
     */
    public static boolean isCritical(int fuelLevel, int maxFuel) {
        if (maxFuel <= 0) return false;
        return (fuelLevel * 100) / maxFuel < 20;
    }
}
