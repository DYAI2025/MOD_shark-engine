package dev.sharkengine.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Debug commands for ship assembly testing.
 *
 * <p>Commands:</p>
 * <ul>
 *   <li>/shipdebug assemble - Instantly assembles ship at player position</li>
 *   <li>/shipdebug builder - Opens builder preview at player position</li>
 *   <li>/shipdebug giveall - Gives all ship blocks</li>
 *   <li>/shipdebug disassemble - Disassembles current ship</li>
 * </ul>
 *
 * @author Shark Engine Team
 * @version 1.0
 */
public final class ShipDebugCommand {

    private ShipDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shipdebug")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("assemble")
                .executes(ctx -> assembleShip(ctx.getSource())))
            .then(Commands.literal("builder")
                .executes(ctx -> openBuilder(ctx.getSource())))
            .then(Commands.literal("giveall")
                .executes(ctx -> giveAllBlocks(ctx.getSource())))
            .then(Commands.literal("disassemble")
                .executes(ctx -> disassembleShip(ctx.getSource())))
        );
    }

    private static int assembleShip(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        BlockPos playerPos = player.blockPosition();
        var result = ShipAssemblyService.tryAssemble(player.serverLevel(), playerPos, player);

        player.sendSystemMessage(Component.translatable(result.translationKey(), result.arg()));
        return 1;
    }

    private static int openBuilder(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        BlockPos playerPos = player.blockPosition();
        ShipAssemblyService.openBuilderPreview(player.serverLevel(), playerPos, player);

        player.sendSystemMessage(Component.literal("Builder preview opened at " + playerPos));
        return 1;
    }

    private static int giveAllBlocks(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        player.addItem(ModBlocks.STEERING_WHEEL.asItem().getDefaultInstance());
        player.addItem(ModBlocks.THRUSTER.asItem().getDefaultInstance());
        player.addItem(ModBlocks.BUG.asItem().getDefaultInstance());

        player.sendSystemMessage(Component.literal("§aReceived: Steering Wheel, Thruster, Bug"));
        return 1;
    }

    private static int disassembleShip(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        if (!(player.getVehicle() instanceof ShipEntity ship)) {
            source.sendFailure(Component.literal("You are not riding a ship!"));
            return 0;
        }

        ship.disassemble();
        player.sendSystemMessage(Component.literal("§eShip disassembled"));
        return 1;
    }
}
