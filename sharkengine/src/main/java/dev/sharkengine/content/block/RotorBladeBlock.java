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
 * Rotor blade (ROTOR_BLADE role, concept doc §4: mass 1, lift 8 — the highest
 * per-block lift figure in the whole balance table). Sixth and last of the six
 * AIR-040 core placeable parts.
 *
 * <p>Concept doc §5.3: "{@code rotor_blade} flach (~3 px hoch)" — a flat blade, about
 * 3 texture-pixels thick — so this follows {@link AirframePanelBlock}'s established
 * thin-plate pattern (full six-direction {@code facing} blockstate, a custom flush
 * {@link VoxelShape} per facing) rather than {@link HelicopterEngineBlock}/{@code
 * fuselage_frame}/{@code rotor_hub}'s solid full-cube pattern, with a thinner profile
 * (3/16 instead of {@code airframe_panel}'s 2/16) to read visually as a blade rather
 * than a hull skin panel.</p>
 *
 * <p>Full six-direction facing (not {@link BugBlock}'s horizontal-only variant) is
 * needed because rotor blade chains extend from a {@code rotor_hub} along whichever
 * horizontal plane the hub's own {@code axis} implies (§6: main rotor —
 * hub {@code axis=y}, blades extend horizontally; tail rotor — hub {@code axis=x} or
 * {@code axis=z}, blades extend from a vertical hub orientation). A blade must
 * therefore be placeable pointing in any of the six directions, exactly like an
 * engine or hull panel can be bolted onto any face.</p>
 */
public final class RotorBladeBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public RotorBladeBlock(Properties properties) {
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
     * A 3px-thick blade flush against the block face the {@code FACING} direction
     * points to — thinner than {@link AirframePanelBlock}'s 2px hull-skin plate,
     * matching concept doc §5.3's "~3 px hoch" blade profile. Rigid-transform
     * consistent with the blockstate model rotation table in
     * {@code SharkEngineModelProvider} (default model oriented {@code facing=up},
     * rotated per-direction the same default-up + rotation-table idiom as
     * {@link AirframePanelBlock}/{@link HelicopterEngineBlock}).
     */
    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.UP, Shapes.box(0, 13.0 / 16.0, 0, 1, 1, 1));
        shapes.put(Direction.DOWN, Shapes.box(0, 0, 0, 1, 3.0 / 16.0, 1));
        shapes.put(Direction.NORTH, Shapes.box(0, 0, 0, 1, 1, 3.0 / 16.0));
        shapes.put(Direction.SOUTH, Shapes.box(0, 0, 13.0 / 16.0, 1, 1, 1));
        shapes.put(Direction.EAST, Shapes.box(13.0 / 16.0, 0, 0, 1, 1, 1));
        shapes.put(Direction.WEST, Shapes.box(0, 0, 0, 3.0 / 16.0, 1, 1));
        return shapes;
    }
}
