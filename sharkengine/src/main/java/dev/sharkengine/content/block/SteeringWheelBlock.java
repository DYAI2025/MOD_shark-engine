package dev.sharkengine.content.block;

import dev.sharkengine.ship.ShipAssemblyService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class SteeringWheelBlock extends Block {
    public SteeringWheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        ShipAssemblyService.AssembleResult res = ShipAssemblyService.tryAssemble(serverLevel, pos, sp);
        sp.sendSystemMessage(Component.translatable(res.translationKey(), res.arg()));

        return InteractionResult.CONSUME;
    }
}
