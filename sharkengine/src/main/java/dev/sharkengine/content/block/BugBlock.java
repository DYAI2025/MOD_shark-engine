package dev.sharkengine.content.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The BUG (Bow/Bug) block is the mandatory front-marker of every vehicle.
 *
 * <p>Placement rules enforced at assembly time:</p>
 * <ul>
 *   <li>Exactly one BUG per vehicle</li>
 *   <li>Must be on the outer edge of the structure</li>
 *   <li>Its FACING direction defines the vehicle's forward direction</li>
 * </ul>
 *
 * <p>The block stores a horizontal FACING property so the player can
 * explicitly point it in the intended forward direction when placing it.</p>
 *
 * @author Shark Engine Team
 * @version 1.0
 */
public final class BugBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BugBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The bug faces the direction the player is looking at (= forward direction of vehicle)
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
}
