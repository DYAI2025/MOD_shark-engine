package dev.sharkengine.ship;

/**
 * Utility class for fuel system calculations.
 * Provides methods for converting wood to energy, calculating flight time, and formatting fuel display.
 * 
 * <p>Fuel system basics:</p>
 * <ul>
 *   <li>1 wood block = 100 energy units</li>
 *   <li>Energy consumption: nominal 1-3 units/sec by acceleration phase, multiplied by
 *       {@code VehicleBalance.FUEL_CONSUMPTION_RATE} (0.25) — effective 0.25-0.75 units/sec</li>
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
     * Negative wood counts are treated as zero (cannot remove fuel via conversion).
     *
     * @param woodCount Number of wood blocks (clamped to &ge;0)
     * @return Energy units (max(0, woodCount) * 100)
     */
    public static int woodToEnergy(int woodCount) {
        return Math.max(0, woodCount) * ENERGY_PER_WOOD;
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
     * <p>Applies {@link dev.sharkengine.ship.part.VehicleBalance#FUEL_CONSUMPTION_RATE}
     * (2026-07-13 fuel-duration tuning) to {@link ShipPhysics#calculateFuelConsumption}'s
     * nominal per-second rate, so the displayed time matches {@code ShipEntity#tick()}'s
     * actual (now 4x slower) burn rate rather than the un-tuned nominal one.</p>
     *
     * @param fuelLevel Current fuel level in energy units
     * @param phase Current acceleration phase
     * @return Remaining flight time in seconds
     */
    public static int calculateRemainingFlightTime(int fuelLevel, AccelerationPhase phase) {
        int nominalConsumption = ShipPhysics.calculateFuelConsumption(phase);
        if (nominalConsumption == 0) {
            return Integer.MAX_VALUE; // No consumption = infinite flight
        }
        float effectiveConsumption = nominalConsumption * dev.sharkengine.ship.part.VehicleBalance.FUEL_CONSUMPTION_RATE;
        return (int) (fuelLevel / effectiveConsumption);
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
    /**
     * Result of one whole "consumption second" step (REQ-016/T17).
     *
     * @param fuelLevel RAW post-step fuel level — may be {@code <= 0}; the caller owns the
     *                  engine-out decision and zero-clamping (entity concerns, not fuel math)
     * @param fuelDebt  remaining fractional debt, always {@code [0,1)} after extraction
     */
    public record FuelTick(int fuelLevel, float fuelDebt) {}

    /**
     * Applies one consumption second: accumulates {@code nominalPerSecond ×
     * VehicleBalance.FUEL_CONSUMPTION_RATE} into the fractional debt and extracts whole fuel
     * units. Pure mirror of the formerly-inline {@code ShipEntity.updatePhysics()} math —
     * extracted for REQ-016/T17 so drift-freeness is unit-provable without a Fabric bootstrap.
     *
     * <p><b>Zero float drift by construction:</b> {@code FUEL_CONSUMPTION_RATE} is 0.25 (a power
     * of two), so every per-second increment (0.25/0.5/0.75) is exactly representable and the
     * debt accumulator never collects rounding error — locked by {@code FuelSystemTest}.</p>
     */
    public static FuelTick applyConsumptionSecond(int fuelLevel, float fuelDebt, int nominalPerSecond) {
        fuelDebt += nominalPerSecond * dev.sharkengine.ship.part.VehicleBalance.FUEL_CONSUMPTION_RATE;
        int wholeUnits = (int) fuelDebt;
        if (wholeUnits > 0) {
            fuelDebt -= wholeUnits;
            fuelLevel -= wholeUnits;
        }
        return new FuelTick(fuelLevel, fuelDebt);
    }

    /**
     * Sanitizes a fuel-debt value read from NBT (REQ-016/T17). Valid live values are always in
     * {@code [0,1)}; anything else — NaN, ±Infinity, negative, or {@code >= 1} (impossible from
     * our own writer, i.e. corrupt or hand-edited) — conservatively resets to {@code 0}: never
     * grants extra pending burn, never poisons the accumulator.
     */
    public static float sanitizeFuelDebt(float debt) {
        if (!Float.isFinite(debt) || debt < 0.0f || debt >= 1.0f) {
            return 0.0f;
        }
        return debt;
    }

    public static boolean isCritical(int fuelLevel, int maxFuel) {
        if (maxFuel <= 0) return false;
        return (fuelLevel * 100) / maxFuel < 20;
    }
}
