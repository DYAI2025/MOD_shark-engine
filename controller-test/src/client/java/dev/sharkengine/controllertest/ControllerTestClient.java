package dev.sharkengine.controllertest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller Test Mod - Independent gamepad/controller debugging tool.
 * 
 * Features:
 * - Detects controllers on startup
 * - Shows live input values (sticks, triggers, buttons)
 * - Configurable deadzone and inversion
 * - Works independently of Shark Engine
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public final class ControllerTestClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ControllerTest");
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Controller Test Mod initialized");
        LOGGER.info("Connect a controller and watch the logs for input data");
        
        // Initialize controller handler
        GamepadInputHandler.init();
        
        // Initialize HUD overlay
        ControllerHudOverlay.init();
        
        // Register tick handler for polling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GamepadInputHandler.poll();
        });
    }
}
