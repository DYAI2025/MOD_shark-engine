package dev.sharkengine.content.block;

import dev.sharkengine.content.ModComponents;
import dev.sharkengine.ship.TrailColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * REQ-019/T21: the single thruster block, carrying the trail color as a BLOCKSTATE property.
 * Placement copies the craft-time {@code trail_color} item component (T20) into the state —
 * from there the color rides the existing blueprint/NBT pipelines for free: assembly stores
 * BlockStates verbatim, save/load round-trips them (TrailColorPersistenceGameTest), and even
 * disassembly restores the colored state. No block entity (T20's no-hidden-storage guard), no
 * second block/item id (LED-002). Pre-T21 saved states simply lack the property and
 * deserialize to the default ({@code none}) — conservative migration, NFR-004.
 *
 * <p><b>Known, deliberate limit (disclosed):</b> breaking a placed colored thruster drops the
 * PLAIN item — a loot table cannot map a state property back into a data component without a
 * custom loot function. Color is recovered by re-crafting (OQ-002: coloring is craft-time
 * only). The blockstate model uses the {@code ""} catch-all variant, so no per-state models or
 * textures exist (TrailTextureResourceTests).</p>
 */
public class ThrusterBlock extends Block {

    public static final EnumProperty<TrailColor> TRAIL_COLOR =
            EnumProperty.create("trail_color", TrailColor.class);

    public ThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(TRAIL_COLOR, TrailColor.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRAIL_COLOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        DyeColor dye = context.getItemInHand().get(ModComponents.TRAIL_COLOR);
        return defaultBlockState().setValue(TRAIL_COLOR,
                dye == null ? TrailColor.NONE : TrailColor.fromDyeName(dye.getName()));
    }
}
