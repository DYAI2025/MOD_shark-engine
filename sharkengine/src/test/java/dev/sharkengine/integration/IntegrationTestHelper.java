package dev.sharkengine.integration;

import dev.sharkengine.ship.ShipBlueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for integration test setup and teardown.
 * 
 * <p>Provides common functionality for:</p>
 * <ul>
 *   <li>Creating test structures</li>
 *   <li>Setting up ship data</li>
 *   <li>Validating test results</li>
 * </ul>
 *
 * @author Shark Engine Team
 * @version 1.0
 */
public class IntegrationTestHelper {

    /** Test ship data (mock) */
    private MockShipData testShip;

    /** Default wheel position coordinates */
    private int wheelX = 0;
    private int wheelY = 10;
    private int wheelZ = 0;

    /**
     * Creates a new integration test helper.
     */
    public IntegrationTestHelper() {
    }

    /**
     * Sets up the test world.
     */
    public IntegrationTestHelper setupTestWorld() {
        this.wheelX = 0;
        this.wheelY = 10;
        this.wheelZ = 0;
        return this;
    }

    /**
     * Sets the wheel position.
     */
    public IntegrationTestHelper setWheelPos(int x, int y, int z) {
        this.wheelX = x;
        this.wheelY = y;
        this.wheelZ = z;
        return this;
    }

    /**
     * Gets the wheel X coordinate.
     */
    public int getWheelX() {
        return wheelX;
    }

    /**
     * Gets the wheel Y coordinate.
     */
    public int getWheelY() {
        return wheelY;
    }

    /**
     * Gets the wheel Z coordinate.
     */
    public int getWheelZ() {
        return wheelZ;
    }

    /**
     * Builds a minimal valid ship structure around the wheel position.
     */
    public void buildMinimalShipStructure() {
        // Structure is tracked conceptually, no actual block placement
    }

    /**
     * Builds a ship structure without a thruster (for failure tests).
     */
    public void buildStructureWithoutThruster() {
        // Structure is tracked conceptually
    }

    /**
     * Builds a ship structure without a BUG block (for failure tests).
     */
    public void buildStructureWithoutBug() {
        // Structure is tracked conceptually
    }

    /**
     * Builds a structure with world contact (for failure tests).
     */
    public void buildStructureWithWorldContact() {
        // Structure is tracked conceptually
    }

    /**
     * Sets the test ship data.
     */
    public void setTestShip(MockShipData ship) {
        this.testShip = ship;
    }

    /**
     * Gets the test ship data.
     */
    public MockShipData getTestShip() {
        return testShip;
    }

    /**
     * Validates that a ship entity was created correctly.
     */
    public boolean validateShipEntity(MockShipData ship, int expectedBlockCount, int expectedFuel) {
        if (ship == null) {
            return false;
        }

        if (ship.getBlockCount() != expectedBlockCount) {
            return false;
        }

        if (ship.getFuelLevel() != expectedFuel) {
            return false;
        }

        return true;
    }

    /**
     * Validates ship movement after physics update.
     */
    public boolean validateShipMovement(MockShipData ship, float expectedMinSpeed, float expectedMaxSpeed) {
        if (ship == null) {
            return false;
        }

        float currentSpeed = ship.getCurrentSpeed();
        
        return currentSpeed >= expectedMinSpeed && currentSpeed <= expectedMaxSpeed;
    }

    /**
     * Validates fuel level.
     */
    public boolean validateFuelLevel(MockShipData ship, int expectedMinFuel, int expectedMaxFuel) {
        if (ship == null) {
            return false;
        }

        int fuelLevel = ship.getFuelLevel();
        
        return fuelLevel >= expectedMinFuel && fuelLevel <= expectedMaxFuel;
    }

    /**
     * Simulates adding fuel to the ship.
     */
    public int addFuel(MockShipData ship, int woodCount) {
        if (ship == null) {
            return 0;
        }
        return ship.addFuel(woodCount);
    }

    /**
     * Simulates mounting the ship.
     */
    public boolean mountShip(MockShipData ship) {
        if (ship == null) {
            return false;
        }
        ship.setHasPassenger(true);
        return true;
    }

    /**
     * Simulates dismounting the ship.
     */
    public void dismountShip(MockShipData ship) {
        if (ship != null) {
            ship.setHasPassenger(false);
        }
    }

    /**
     * Toggles the ship anchor.
     */
    public void toggleAnchor(MockShipData ship) {
        if (ship != null) {
            ship.toggleAnchor();
        }
    }

    /**
     * Disassembles the ship.
     */
    public void disassemble(MockShipData ship) {
        if (ship != null) {
            ship.discard();
        }
    }

    /**
     * Cleans up test resources.
     */
    public void cleanup() {
        testShip = null;
    }

    /**
     * Creates a simple position representation for testing.
     */
    public static int[] createPos(int x, int y, int z) {
        return new int[]{x, y, z};
    }

    /**
     * Mock ship data class for testing.
     * Simulates ShipEntity behavior without Minecraft dependencies.
     */
    public static class MockShipData {
        private int blockCount = 7;
        private int fuelLevel = 100;
        private float currentSpeed = 0.0f;
        private int accelerationTicks = 0;
        private boolean anchored = false;
        private boolean engineOut = false;
        private float inputForward = 0.0f;
        private boolean removed = false;
        private boolean hasPassenger = false;
        private UUID pilot;

        public MockShipData(int blockCount) {
            this.blockCount = blockCount;
        }

        public int getBlockCount() {
            return blockCount;
        }

        public int getFuelLevel() {
            return fuelLevel;
        }

        public float getCurrentSpeed() {
            return currentSpeed;
        }

        public int getAccelerationTicks() {
            return accelerationTicks;
        }

        public void setAccelerationTicks(int ticks) {
            this.accelerationTicks = ticks;
        }

        public dev.sharkengine.ship.AccelerationPhase getPhase() {
            return dev.sharkengine.ship.AccelerationPhase.fromTick(accelerationTicks);
        }

        public boolean isAnchored() {
            return anchored;
        }

        public boolean isEngineOut() {
            return engineOut;
        }

        public float getInputForward() {
            return inputForward;
        }

        public boolean isRemoved() {
            return removed;
        }

        public void setInputs(float throttle, float turn, float forward) {
            this.inputForward = Math.max(0, Math.min(1, forward));
        }

        public void setCurrentSpeed(float speed) {
            this.currentSpeed = speed;
        }

        public int addFuel(int woodCount) {
            // woodCount in Energy umrechnen (1 Holz = 100 Energy)
            int energyToAdd = woodCount * dev.sharkengine.ship.FuelSystem.ENERGY_PER_WOOD;
            
            if (woodCount < 0) {
                fuelLevel = Math.max(0, fuelLevel + woodCount);
                if (fuelLevel <= 0) {
                    engineOut = true;
                }
                return woodCount;
            } else {
                int oldFuel = fuelLevel;
                fuelLevel = Math.min(dev.sharkengine.ship.FuelSystem.MAX_FUEL, fuelLevel + energyToAdd);
                if (fuelLevel > 0) {
                    engineOut = false;
                }
                return fuelLevel - oldFuel;
            }
        }

        public boolean hasPassenger() {
            return hasPassenger;
        }

        public void setHasPassenger(boolean has) {
            this.hasPassenger = has;
        }

        public void setPilot(UUID pilot) {
            this.pilot = pilot;
        }

        public boolean isPilot(UUID playerUuid) {
            return pilot != null && pilot.equals(playerUuid);
        }

        public void discard() {
            this.removed = true;
        }

        public void toggleAnchor() {
            this.anchored = !this.anchored;
        }
    }
}
