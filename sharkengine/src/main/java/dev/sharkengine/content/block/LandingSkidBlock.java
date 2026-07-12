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
 * Landing skid (LANDING_GEAR role, concept doc §4: mass 1, no lift/thrust/drag/fuel
 * contribution — a purely structural undercarriage part). Seventh AIR-040 core
 * placeable part, following the six already registered (airframe_panel,
 * fuselage_frame, helicopter_engine, rotor_hub, rotor_blade).
 *
 * <p>Concept doc §4 "Blockstate" column: {@code facing}, full six-direction — same
 * idiom as {@link AirframePanelBlock}/{@link HelicopterEngineBlock}/{@link
 * RotorBladeBlock} (default model oriented {@code facing=up}, reached by rotation for
 * every other facing; a skid can be bolted to any face of the fuselage it supports,
 * not just the underside). Concept doc §5.3: "{@code landing_skid} Kufenprofil" (a
 * skid/runner profile) — distinguished from {@link AirframePanelBlock}'s 2px hull-skin
 * plate and {@link RotorBladeBlock}'s 3px blade by a chunkier 4px rail thickness,
 * matching a landing skid's role as a load-bearing touchdown structure rather than a
 * thin cosmetic plate. Collision {@link VoxelShape} is a single flush box per facing
 * (same simplification precedent as {@link BugBlock}, whose full-cube default shape
 * likewise does not trace its wedge-shaped visual model exactly) — a landing skid is
 * ship_eligible-only (no ROTOR_HUB/ROTOR_BLADE/PROPULSION/AIRCRAFT_STRUCTURE tag; see
 * {@code SharkEngineTagProvider}), so its collision shape carries no gameplay-rule
 * weight beyond player/entity walk collision on a placed, unassembled block.</p>
 */
public final class LandingSkidBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public LandingSkidBlock(Properties properties) {
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
     * A 4px-thick skid rail flush against the block face the {@code FACING} direction
     * points to — thicker than {@link AirframePanelBlock}'s 2px hull-skin plate and
     * {@link RotorBladeBlock}'s 3px blade, matching concept doc §5.3's "Kufenprofil"
     * (a load-bearing runner, not a thin cosmetic panel). Rigid-transform consistent
     * with the blockstate model rotation table in {@code SharkEngineModelProvider}
     * (default model oriented {@code facing=up}, rotated per-direction the same
     * default-up + rotation-table idiom as {@link AirframePanelBlock}/{@link
     * RotorBladeBlock}/{@link HelicopterEngineBlock}).
     */
    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.UP, Shapes.box(0, 12.0 / 16.0, 0, 1, 1, 1));
        shapes.put(Direction.DOWN, Shapes.box(0, 0, 0, 1, 4.0 / 16.0, 1));
        shapes.put(Direction.NORTH, Shapes.box(0, 0, 0, 1, 1, 4.0 / 16.0));
        shapes.put(Direction.SOUTH, Shapes.box(0, 0, 12.0 / 16.0, 1, 1, 1));
        shapes.put(Direction.EAST, Shapes.box(12.0 / 16.0, 0, 0, 1, 1, 1));
        shapes.put(Direction.WEST, Shapes.box(0, 0, 0, 4.0 / 16.0, 1, 1));
        return shapes;
    }
}
