package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.content.block.AirframePanelBlock;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.content.block.HelicopterEngineBlock;
import dev.sharkengine.content.block.SteeringWheelBlock;
import dev.sharkengine.content.block.SteeringWheelItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class ModBlocks {
    public static final Block STEERING_WHEEL = registerBlock(
            "steering_wheel",
            SteeringWheelBlock::new,
            BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD),
            block -> new SteeringWheelItem(block, new Item.Properties())
    );

    public static final Block THRUSTER = registerBlock(
            "thruster",
            Block::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops(),
            block -> new BlockItem(block, new Item.Properties())
    );

    public static final Block BUG = registerBlock(
            "bug",
            BugBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.METAL),
            block -> new BlockItem(block, new Item.Properties())
    );

    public static final Block AIRFRAME_PANEL = registerBlock(
            "airframe_panel",
            AirframePanelBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.METAL),
            block -> new BlockItem(block, new Item.Properties())
    );

    /**
     * AIR-040: fuselage_frame (STRUCTURE role, concept §4 "Blockstate" column:
     * {@code axis} — "placed along X/Y/Z like a log/pillar"). Vanilla's own
     * {@link RotatedPillarBlock} already implements exactly this behavior
     * ({@code AXIS} blockstate property, {@code getStateForPlacement} derived from
     * the clicked face's axis, full-cube {@code VoxelShape}) — reused directly rather
     * than hand-rolling a new {@code content.block} class, unlike {@link
     * AirframePanelBlock} (which needed a custom thin-plate VoxelShape per facing
     * that no vanilla base class provides).
     */
    public static final Block FUSELAGE_FRAME = registerBlock(
            "fuselage_frame",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.METAL),
            block -> new BlockItem(block, new Item.Properties())
    );

    /**
     * AIR-040: helicopter_engine (PROPULSION role, {@code liftMode=ROTOR} —
     * {@code dev.sharkengine.ship.part.VehicleBalance#PARTS}). Strength/sound match
     * {@link #THRUSTER}'s (an engine block, not a light hull-skin/frame part like
     * {@link #AIRFRAME_PANEL}/{@link #FUSELAGE_FRAME}), including
     * {@code requiresCorrectToolForDrops()} for the same reason: an industrial engine
     * block is intentionally harder to casually punch out than sheet-metal hull parts.
     */
    public static final Block HELICOPTER_ENGINE = registerBlock(
            "helicopter_engine",
            HelicopterEngineBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops(),
            block -> new BlockItem(block, new Item.Properties())
    );

    private ModBlocks() {}

    public static void init() {
        // creative tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(STEERING_WHEEL.asItem());
            entries.accept(THRUSTER.asItem());
            entries.accept(BUG.asItem());
            entries.accept(AIRFRAME_PANEL.asItem());
            entries.accept(FUSELAGE_FRAME.asItem());
            entries.accept(HELICOPTER_ENGINE.asItem());
        });
    }

    private static Block registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties props,
            Function<Block, BlockItem> itemFactory
    ) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, name);
        Block block = factory.apply(props);

        Registry.register(BuiltInRegistries.BLOCK, id, block);

        if (itemFactory != null) {
            Registry.register(BuiltInRegistries.ITEM, id, itemFactory.apply(block));
        }

        return block;
    }
}
