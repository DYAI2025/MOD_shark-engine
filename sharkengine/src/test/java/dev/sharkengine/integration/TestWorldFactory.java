package dev.sharkengine.integration;

import dev.sharkengine.ship.ShipBlueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating test world structures for integration tests.
 * 
 * <p>Provides helper methods to build common ship structures for testing
 * the complete assembly and flight loop.</p>
 *
 * <p>Note: This factory uses null for BlockState since Minecraft classes
 * are not available in the test classpath.</p>
 *
 * @author Shark Engine Team
 * @version 1.0
 */
public final class TestWorldFactory {

    private TestWorldFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a minimal valid ship structure (7 blocks).
     */
    public static List<ShipBlueprint.ShipBlock> createMinimalValidStructure(int wheelX, int wheelY, int wheelZ) {
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();

        // Steering Wheel (center)
        blocks.add(createBlock(0, 0, 0));

        // North neighbor (core block)
        blocks.add(createBlock(0, 0, -1));

        // South neighbor (core block)
        blocks.add(createBlock(0, 0, 1));

        // East neighbor (core block)
        blocks.add(createBlock(1, 0, 0));

        // West neighbor (core block)
        blocks.add(createBlock(-1, 0, 0));

        // BUG block at front edge
        blocks.add(createBlock(0, 0, -2));

        // Thruster at back
        blocks.add(createBlock(0, 0, 2));

        return blocks;
    }

    /**
     * Creates a ship structure without a thruster (for failure tests).
     */
    public static List<ShipBlueprint.ShipBlock> createStructureWithoutThruster(int wheelX, int wheelY, int wheelZ) {
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();

        blocks.add(createBlock(0, 0, 0)); // Steering Wheel
        blocks.add(createBlock(0, 0, -1)); // North
        blocks.add(createBlock(0, 0, 1));  // South
        blocks.add(createBlock(1, 0, 0));  // East
        blocks.add(createBlock(-1, 0, 0)); // West
        blocks.add(createBlock(0, 0, -2)); // BUG

        return blocks;
    }

    /**
     * Creates a ship structure without a BUG block (for failure tests).
     */
    public static List<ShipBlueprint.ShipBlock> createStructureWithoutBug(int wheelX, int wheelY, int wheelZ) {
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();

        blocks.add(createBlock(0, 0, 0)); // Steering Wheel
        blocks.add(createBlock(0, 0, -1)); // North
        blocks.add(createBlock(0, 0, 1));  // South
        blocks.add(createBlock(1, 0, 0));  // East
        blocks.add(createBlock(-1, 0, 0)); // West
        blocks.add(createBlock(0, 0, 2));  // Thruster

        return blocks;
    }

    /**
     * Creates a ship structure with BUG in the interior (not on edge).
     */
    public static List<ShipBlueprint.ShipBlock> createStructureWithBugInside(int wheelX, int wheelY, int wheelZ) {
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();

        blocks.add(createBlock(0, 0, 0)); // BUG
        blocks.add(createBlock(0, 1, 0));
        blocks.add(createBlock(0, -1, 0));
        blocks.add(createBlock(1, 0, 0));
        blocks.add(createBlock(-1, 0, 0));
        blocks.add(createBlock(0, 0, 1));
        blocks.add(createBlock(0, 0, -1));
        blocks.add(createBlock(0, 2, 0)); // Thruster

        return blocks;
    }

    /**
     * Creates a larger ship structure for extended testing (13 blocks).
     */
    public static List<ShipBlueprint.ShipBlock> createLargeStructure(int wheelX, int wheelY, int wheelZ) {
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();

        blocks.add(createBlock(0, 0, 0));  // Steering Wheel
        blocks.add(createBlock(0, 0, -1)); // North
        blocks.add(createBlock(0, 0, 1));  // South
        blocks.add(createBlock(1, 0, 0));  // East
        blocks.add(createBlock(-1, 0, 0)); // West
        blocks.add(createBlock(0, 0, -2)); // Hull
        blocks.add(createBlock(0, 0, -3)); // Hull
        blocks.add(createBlock(1, 0, -1)); // Hull
        blocks.add(createBlock(-1, 0, -1));// Hull
        blocks.add(createBlock(0, 0, -4)); // BUG
        blocks.add(createBlock(0, 0, 1));  // Thruster
        blocks.add(createBlock(1, 0, 1));  // Thruster
        blocks.add(createBlock(-1, 0, 1)); // Thruster

        return blocks;
    }

    /**
     * Creates a structure with world contact (for failure tests).
     */
    public static List<ShipBlueprint.ShipBlock> createStructureWithWorldContact(int wheelX, int wheelY, int wheelZ) {
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();

        blocks.add(createBlock(0, 0, 0));  // Steering Wheel
        blocks.add(createBlock(0, 0, -1)); // North
        blocks.add(createBlock(0, 0, 1));  // South
        blocks.add(createBlock(1, 0, 0));  // East
        blocks.add(createBlock(-1, 0, 0)); // West
        blocks.add(createBlock(0, 0, -2)); // BUG
        blocks.add(createBlock(0, 0, 2));  // Thruster
        blocks.add(createBlock(0, -1, 0)); // Ground contact

        return blocks;
    }

    /**
     * Helper method to create a ShipBlock with relative coordinates.
     */
    private static ShipBlueprint.ShipBlock createBlock(int dx, int dy, int dz) {
        return new ShipBlueprint.ShipBlock(dx, dy, dz, null);
    }

    /**
     * Calculates expected fuel consumption for a given flight duration.
     */
    public static int calculateExpectedFuelConsumption(int ticks, boolean hasThrottle) {
        if (!hasThrottle) {
            return 0;
        }

        int seconds = ticks / 20;
        int consumption = 0;

        int phase12Seconds = Math.min(seconds, 4);
        consumption += phase12Seconds * 1;

        if (seconds > 4) {
            int phase34Seconds = Math.min(seconds - 4, 2);
            consumption += phase34Seconds * 2;
        }

        if (seconds > 6) {
            int phase5Seconds = seconds - 6;
            consumption += phase5Seconds * 3;
        }

        return consumption;
    }

    /**
     * Creates a simple block structure for basic tests.
     */
    public static List<ShipBlueprint.ShipBlock> createSimpleBlocks(int count) {
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            blocks.add(createBlock(i, 0, 0));
        }
        return blocks;
    }

    /**
     * Creates a position array for testing.
     */
    public static int[] createPos(int x, int y, int z) {
        return new int[]{x, y, z};
    }
}
