package dev.sharkengine.command;

import dev.sharkengine.command.ShipDebugCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;

/**
 * Command registration for Shark Engine.
 * Registers debug commands for ship testing.
 *
 * @author Shark Engine Team
 * @version 1.0
 */
public final class ModCommands {

    private ModCommands() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ShipDebugCommand.register(dispatcher);
        });
    }
}
