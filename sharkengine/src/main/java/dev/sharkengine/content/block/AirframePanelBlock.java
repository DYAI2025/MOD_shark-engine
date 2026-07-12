package dev.sharkengine.content.block;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Outer hull skin plate (SKIN role, concept doc §4). First of the six AIR-040 core
 * placeable parts — establishes the pattern the other five (fuselage_frame,
 * helicopter_engine, rotor_hub, rotor_blade, landing_skid) follow: a full
 * six-direction {@code facing} blockstate (concept doc's "Blockstate" column for this
 * part), a thin flush-mounted {@link VoxelShape} on the facing side, and
 * datagen-only resources (no hand-written JSON).
 *
 * <p>Placement picks the clicked face as the outward-facing normal (like a
 * wall-mounted plate you attach to whichever face of the neighboring block you
 * click), matching how a hull skin panel is applied to any side of a fuselage —
 * floor, ceiling, or wall — not just horizontally (unlike {@link BugBlock}, whose
 * FACING is deliberately horizontal-only because it encodes the vehicle's forward
 * direction, not an attachment side).</p>
 */
public final class AirframePanelBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public AirframePanelBlock(Properties properties) {
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /**
     * A 2px-thick plate flush against the block face the {@code FACING} direction
     * points to (e.g. {@code UP} → thin plate at the top of the block, {@code NORTH}
     * → thin plate on the north side). Rigid-transform-consistent with the
     * blockstate model rotation table in {@code SharkEngineModelProvider}
     * (default model oriented {@code facing=up}, rotated per-direction the same
     * way vanilla's lightning_rod rotates its default up-pointing model).
     */
    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.UP, Shapes.box(0, 14.0 / 16.0, 0, 1, 1, 1));
        shapes.put(Direction.DOWN, Shapes.box(0, 0, 0, 1, 2.0 / 16.0, 1));
        shapes.put(Direction.NORTH, Shapes.box(0, 0, 0, 1, 1, 2.0 / 16.0));
        shapes.put(Direction.SOUTH, Shapes.box(0, 0, 14.0 / 16.0, 1, 1, 1));
        shapes.put(Direction.EAST, Shapes.box(14.0 / 16.0, 0, 0, 1, 1, 1));
        shapes.put(Direction.WEST, Shapes.box(0, 0, 0, 2.0 / 16.0, 1, 1));
        return shapes;
    }
}
