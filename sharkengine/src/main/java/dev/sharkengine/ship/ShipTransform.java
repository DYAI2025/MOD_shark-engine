package dev.sharkengine.ship;

/**
 * Single rotation authority for ship blueprint offsets, shared by rendering,
 * collision, and disassembly so all three agree on orientation (AIR-010,
 * fixes B1/B2 — see docs/AIRCRAFT_CONCEPT_V2.md §3.1/§3.2).
 *
 * <p><b>World truth</b> (ground-truthed against real MC 1.21.1 bytecode, not
 * assumed): Minecraft yaw is clockwise-positive when viewed from above, with
 * yaw 0 = south (+Z), matching {@code ShipAssemblyService.directionToYaw()}
 * (SOUTH=0, WEST=90, NORTH=180, EAST=-90) and {@code ShipEntity}'s forward
 * vector ({@code fx=-sin(rad), fz=cos(rad)}). {@link #rotateOffset} at +90
 * degrees is mathematically identical to vanilla
 * {@code net.minecraft.core.BlockPos.rotate(Rotation.CLOCKWISE_90)}, which
 * decompiles to {@code (x,z) -> (-z, x)} — verified by direct bytecode
 * inspection of the pinned dependency, not the plan's original assumption.</p>
 *
 * <p>Pure math only — no Minecraft classes referenced here so this class is
 * trivially unit-testable. Thin adapters (BlockPos/Rotation conversions)
 * belong in callers, not here.</p>
 */
public final class ShipTransform {

    private ShipTransform() {}

    /**
     * Rotates a local (dx, dz) offset by {@code yawDeg} around the Y axis,
     * using the standard rotation matrix that reproduces vanilla's forward
     * vector convention (see class doc for the ground-truthed derivation).
     *
     * @return a 2-element array {@code [worldX, worldZ]}
     */
    public static double[] rotateOffset(double dx, double dz, float yawDeg) {
        double rad = Math.toRadians(yawDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double worldX = dx * cos - dz * sin;
        double worldZ = dx * sin + dz * cos;
        return new double[] { worldX, worldZ };
    }

    /**
     * Wraps a yaw value into the canonical range {@code (-180, 180]}, matching
     * vanilla {@code Mth.wrapDegrees}.
     */
    public static float wrapDegrees(float yawDeg) {
        float wrapped = yawDeg % 360.0f;
        if (wrapped > 180.0f) {
            wrapped -= 360.0f;
        } else if (wrapped <= -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    /**
     * The effective rotation between the ship's current yaw and the yaw it
     * was assembled at (its BUG block's facing, {@code assemblyYaw} —
     * AIR-015). This is what rendering, collision, and disassembly must
     * apply to raw world-space blueprint offsets; using the raw entity yaw
     * instead is exactly bug B1.
     */
    public static float effectiveYaw(float entityYawDeg, float assemblyYawDeg) {
        return wrapDegrees(entityYawDeg - assemblyYawDeg);
    }

    /**
     * Snaps a yaw value to the nearest cardinal direction (0/90/180/270),
     * for selecting a discrete {@code Rotation} enum value (disassembly).
     * Ties round toward the higher cardinal (matches {@link Math#round}).
     *
     * @return a value in {@code [0, 360)}
     */
    public static int snapToCardinal(float yawDeg) {
        float wrapped = wrapDegrees(yawDeg);
        int snapped = Math.round(wrapped / 90.0f) * 90;
        return ((snapped % 360) + 360) % 360;
    }

    /**
     * Rotates a local (dx, dy, dz) offset by {@code yawDeg} and rounds to the
     * nearest integer world block position, packed the same way as vanilla
     * {@code BlockPos.asLong()} (see {@link #packBlockPos}). Deterministic:
     * identical inputs always produce identical output, so callers can dedupe
     * rounding collisions (e.g. from a non-cardinal rotation) via a Set
     * instead of silently losing or duplicating blocks.
     */
    public static long worldBlock(int dx, int dy, int dz, float yawDeg) {
        double[] rotated = rotateOffset(dx, dz, yawDeg);
        long rx = Math.round(rotated[0]);
        long rz = Math.round(rotated[1]);
        return packBlockPos(rx, dy, rz);
    }

    /**
     * Packs (x, y, z) into a single long for use as a Set/Map key. Not
     * claimed to match vanilla {@code BlockPos.asLong()}'s exact bit layout
     * (that wasn't verified) — this is an internal, deterministic packing
     * only: identical coordinates always produce the identical long, which
     * is the only property {@link #worldBlock} callers need for dedupe.
     */
    public static long packBlockPos(long x, long y, long z) {
        long result = 0L;
        result |= (x & 0x3FFFFFFL) << 38;
        result |= (y & 0xFFFL);
        result |= (z & 0x3FFFFFFL) << 12;
        return result;
    }

    /**
     * Bank/roll angle for the flight-feel feature (FLR-002,
     * docs/plans/flight-bank-roll.md) — a pure, deterministic mapping from
     * turn input to a visual roll angle. Linear and clamped: {@code turnInput}
     * is expected in {@code [-1, 1]} (callers should already clamp, e.g.
     * {@code ShipEntity.setInputs}) but out-of-range input is defensively
     * clamped here too rather than trusted.
     *
     * <p><b>Sign convention:</b> the return value has the SAME sign as
     * {@code turnInput}. Positive {@code turnInput} is the value that turns
     * the ship left (the P0-verified convention in {@code ShipEntity.tick()}:
     * {@code yaw = getYRot() - inputTurn * 3.0f} — proven via forward/right
     * cross product, see that method's own javadoc), so a positive return
     * here means "bank left". This function does NOT decide which
     * {@code PoseStack} rotation axis/direction corresponds to a left bank —
     * that mapping is empirically verified in {@code ShipEntityRenderer}
     * (FLR-003) against a real {@code runClient} render, not assumed here.</p>
     *
     * @param turnInput  current turn input, expected {@code [-1, 1]}
     * @param maxBankDeg maximum bank angle in degrees at full turn input
     * @return bank angle in degrees, same sign as {@code turnInput}, clamped
     *         to {@code [-maxBankDeg, maxBankDeg]}
     */
    public static float rollFromTurnInput(float turnInput, float maxBankDeg) {
        float clamped = Math.max(-1.0f, Math.min(1.0f, turnInput));
        return clamped * maxBankDeg;
    }

    /**
     * Pitch angle for the flight-feel feature (FLP-002, docs/plans/flight-pitch.md) —
     * a pure, deterministic mapping from vertical input to a visual pitch angle.
     * Structurally identical to {@link #rollFromTurnInput}: linear and clamped,
     * {@code verticalInput} is expected in {@code [-1, 1]} (callers should already
     * clamp, e.g. {@code ShipEntity.setInputVertical}) but out-of-range input is
     * defensively clamped here too rather than trusted.
     *
     * <p><b>Sign convention:</b> the return value has the SAME sign as
     * {@code verticalInput}. Positive {@code verticalInput} is Space/climb
     * (see {@code HelmInputClient}: Space=+1, Shift=-1), and per the user's own
     * stated convention for this feature a climb should tip the nose UP — so a
     * positive return here means "pitch nose up". This function does NOT decide
     * which {@code PoseStack} rotation axis/direction corresponds to nose-up —
     * that mapping is empirically verified in {@code ShipEntityRenderer} (FLP-003)
     * against a real {@code runClient} render, not assumed here (though the axis
     * itself is very likely already known from the FLR-003 bug hunt — see that
     * task's notes in the plan doc).</p>
     *
     * @param verticalInput current vertical input, expected {@code [-1, 1]}
     * @param maxPitchDeg   maximum pitch angle in degrees at full vertical input
     * @return pitch angle in degrees, same sign as {@code verticalInput}, clamped
     *         to {@code [-maxPitchDeg, maxPitchDeg]}
     */
    public static float pitchFromVerticalInput(float verticalInput, float maxPitchDeg) {
        float clamped = Math.max(-1.0f, Math.min(1.0f, verticalInput));
        return clamped * maxPitchDeg;
    }
}
