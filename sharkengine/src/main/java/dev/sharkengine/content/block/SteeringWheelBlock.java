package dev.sharkengine.content.block;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.tutorial.TutorialService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;

public final class SteeringWheelBlock extends Block {
    public SteeringWheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        // Drop starter kit: 2 thrusters + 1 bug (front marker)
        ItemStack thrusters = new ItemStack(ModBlocks.THRUSTER.asItem(), 2);
        ItemEntity dropThrusters = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, thrusters);
        dropThrusters.setDefaultPickUpDelay();
        level.addFreshEntity(dropThrusters);

        ItemStack bug = new ItemStack(ModBlocks.BUG.asItem(), 1);
        ItemEntity dropBug = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, bug);
        dropBug.setDefaultPickUpDelay();
        level.addFreshEntity(dropBug);
    }

    /**
     * REQ-001 (AC-001): the placement half of the vehicle route-popup trigger. Vanilla's
     * {@code BlockItem#place} always calls this after successfully placing the block, with the
     * placing entity — this is the only placement hook that actually has a player reference
     * ({@link #onPlace} above does not), so it is the correct place to open the route-selection
     * popup on placement, matching {@link #useWithoutItem}'s interact-path trigger below (both
     * call {@link TutorialService#startModeSelection}).
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof ServerPlayer sp)) {
            return;
        }

        TutorialService.startModeSelection(sp, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        TutorialService.startModeSelection(sp, pos);
        return InteractionResult.CONSUME;
    }
}
