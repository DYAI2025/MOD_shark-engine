package dev.sharkengine.content.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Helicopter engine (PROPULSION role, {@code liftMode=ROTOR} per
 * {@link dev.sharkengine.ship.part.VehicleBalance#PARTS}). Fourth of the six AIR-040
 * core placeable parts (concept doc §4 "Blockstate" column: {@code facing}, full
 * six-direction — mirrors {@link AirframePanelBlock}'s established full-six-direction
 * {@code facing} pattern rather than {@link BugBlock}'s horizontal-only one, since an
 * engine can be bolted onto any face of the fuselage — ceiling, floor, or wall — the
 * same way a hull skin panel can, not restricted to a horizontal forward direction
 * like the BUG block's vehicle-orientation marker).
 *
 * <p>Unlike {@link AirframePanelBlock} (a thin flush-mounted plate), this is a solid
 * engine block — no custom {@link net.minecraft.world.phys.shapes.VoxelShape} is
 * needed; {@link Block}'s default {@code getShape} already returns a full cube, which
 * is the correct collision/selection shape for every {@code FACING} value here (the
 * single {@code helicopter_engine.png} texture is applied uniformly to all six faces
 * via {@code minecraft:block/cube_all} in the datagen block model — same reasoning
 * {@code fuselage_frame}'s own class javadoc gives for its {@code axis} property:
 * kept for blockstate correctness and forward-consistency with the rest of the part
 * system, even though today's single texture makes every facing look identical).</p>
 */
public final class HelicopterEngineBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public HelicopterEngineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }
}
