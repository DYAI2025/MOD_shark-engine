package dev.sharkengine.ship.session;

/**
 * Block-granularity 3D coordinate, deliberately independent of
 * {@code net.minecraft.core.BlockPos} (REQ-003).
 *
 * <p>This package mirrors the "pure functions, Fabric-free" discipline of
 * {@code dev.sharkengine.ship.part} (see {@code VehiclePartRegistry}) and, even more strictly,
 * {@code ShipTransform} (see that class's javadoc: "no Minecraft classes referenced here") — no
 * {@code net.minecraft.*} import appears anywhere in this package, so it needs no Fabric bootstrap
 * and no test-classpath shim to unit-test. Fabric-touching callers ({@code
 * dev.sharkengine.ship.BuildSessionGate}) adapt real {@code BlockPos} instances to/from this type
 * at the one seam where server authority meets this pure logic.</p>
 */
public record BlockCoord(int x, int y, int z) {

    /** Euclidean (not Manhattan) distance to another coordinate, in blocks. */
    public double distanceTo(BlockCoord other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
