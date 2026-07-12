package dev.sharkengine;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.content.ModItems;
import dev.sharkengine.net.ModNetworking;
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
        LOGGER.info("Shark Engine initialized");
    }
}
