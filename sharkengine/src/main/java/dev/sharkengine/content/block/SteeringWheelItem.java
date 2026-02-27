package dev.sharkengine.content.block;

import dev.sharkengine.tutorial.TutorialService;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;

/**
 * Custom item for the steering wheel that triggers tutorial popups when placed.
 */
public final class SteeringWheelItem extends BlockItem {
    public SteeringWheelItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result.consumesAction() && context.getPlayer() instanceof ServerPlayer serverPlayer) {
            TutorialService.sendWelcomePopup(serverPlayer);
        }
        return result;
    }
}
