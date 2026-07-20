package dev.sharkengine;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.ModItems;
import dev.sharkengine.net.ModNetworking;
import dev.sharkengine.ship.BuildSessionGate;
import dev.sharkengine.ship.EditModeBlockProtection;
import dev.sharkengine.ship.part.VehiclePartRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SharkEngineMod {
    public static final String MOD_ID = "sharkengine";
    public static final Logger LOGGER = LoggerFactory.getLogger("SharkEngine");

    public static void init() {
        ModBlocks.init();
        ModItems.init();
        ModEntities.init();
        ModNetworking.init();
        VehiclePartRegistry.init();
        // REQ-003: server/world-lifecycle cleanup for VehicleBuildSessionRegistry (reviewer-
        // reported: previously never wired to anything in production -- see
        // BuildSessionGate#registerLifecycleHooks).
        BuildSessionGate.registerLifecycleHooks();
        // REQ-014/T14 remediation round 3 (security-review BLOCKER): protects a ship's
        // materialized Edit Mode footprint against break/place by anyone other than the
        // editing pilot -- see EditModeBlockProtection's own class javadoc.
        EditModeBlockProtection.register();
        LOGGER.info("Shark Engine initialized");
    }
}
