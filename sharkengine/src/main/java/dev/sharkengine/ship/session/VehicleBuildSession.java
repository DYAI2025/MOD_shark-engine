package dev.sharkengine.ship.session;

import dev.sharkengine.ship.VehicleClass;

import java.util.UUID;

/**
 * A server-owned vehicle build session (REQ-003, PRD "Auswahl und Assembly müssen an eine
 * serverseitig validierte VehicleBuildSession ... gebunden sein").
 *
 * <p>Carries exactly the fields the PRD names for REQ-003 — Spieler ({@link #playerId}), Dimension
 * ({@link #dimensionId}), Steering-Wheel-Position ({@link #wheelPos}), Fahrzeugklasse
 * ({@link #vehicleClass}), Status ({@link #status}), and Ablaufzeit ({@link #expiresAtMillis}) —
 * plus a {@link #sessionId} bearer token so a request can be checked against "wrong/absent session
 * id" as its own independent axis (see {@link VehicleBuildSessionValidator}).</p>
 *
 * <p>{@link #dimensionId} is a plain string (a dimension's {@code ResourceLocation.toString()},
 * e.g. {@code "minecraft:overworld"}) rather than {@code net.minecraft.resources.ResourceKey<Level>}
 * — this package has zero {@code net.minecraft.*}/Fabric imports (mirrors {@code ship.part} and
 * {@code ShipTransform}'s discipline), so it never needs a Fabric bootstrap to unit-test. Fabric
 * adapters live in {@code dev.sharkengine.ship.BuildSessionGate}.</p>
 */
public record VehicleBuildSession(
        UUID sessionId,
        UUID playerId,
        String dimensionId,
        BlockCoord wheelPos,
        VehicleClass vehicleClass,
        VehicleBuildSessionStatus status,
        long expiresAtMillis
) {
    /** Returns a copy of this session marked {@link VehicleBuildSessionStatus#CONSUMED}. */
    public VehicleBuildSession consumed() {
        return new VehicleBuildSession(sessionId, playerId, dimensionId, wheelPos, vehicleClass,
                VehicleBuildSessionStatus.CONSUMED, expiresAtMillis);
    }
}
