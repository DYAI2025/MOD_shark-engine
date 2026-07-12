package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.content.block.AirframePanelBlock;
import dev.sharkengine.content.block.BugBlock;
import dev.sharkengine.content.block.HelicopterEngineBlock;
import dev.sharkengine.content.block.LandingSkidBlock;
import dev.sharkengine.content.block.RotorBladeBlock;
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

    /**
     * AIR-040: rotor_hub (ROTOR_HUB role, concept §4 "Blockstate" column: {@code axis}
     * — same "placed along X/Y/Z like a log/pillar" semantics as {@link #FUSELAGE_FRAME}).
     * Reuses vanilla's {@link RotatedPillarBlock} directly for the identical reason
     * documented on {@link #FUSELAGE_FRAME}: it already implements the {@code AXIS}
     * blockstate property and full-cube {@code VoxelShape} this part needs, with no
     * custom geometry required. Strength/sound match {@link #FUSELAGE_FRAME}'s (a
     * metal structural/mechanical part, not a light hull-skin plate) rather than
     * {@link #HELICOPTER_ENGINE}'s heavier-industrial profile — a hub is a bearing
     * fixture, not an engine block.
     */
    public static final Block ROTOR_HUB = registerBlock(
            "rotor_hub",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.METAL),
            block -> new BlockItem(block, new Item.Properties())
    );

    /**
     * AIR-040: rotor_blade (ROTOR_BLADE role, concept §4 "Blockstate" column:
     * {@code facing}, full six-direction — see {@link RotorBladeBlock}'s javadoc for
     * why full six-direction rather than {@link dev.sharkengine.content.block.BugBlock}'s
     * horizontal-only variant was chosen). Sixth and last of the six AIR-040 core
     * placeable parts. Strength/sound match {@link #AIRFRAME_PANEL}/{@link
     * #FUSELAGE_FRAME}/{@link #ROTOR_HUB}'s (a light sheet-metal/mechanical part, not
     * a heavy engine block) rather than {@link #HELICOPTER_ENGINE}'s industrial
     * profile — a blade is thin stamped metal, not a solid engine casting.
     */
    public static final Block ROTOR_BLADE = registerBlock(
            "rotor_blade",
            RotorBladeBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.METAL),
            block -> new BlockItem(block, new Item.Properties())
    );

    /**
     * AIR-040: landing_skid (LANDING_GEAR role, concept §4 "Blockstate" column:
     * {@code facing}, full six-direction — see {@link LandingSkidBlock}'s javadoc).
     * Seventh AIR-040 core placeable part, following {@link #ROTOR_BLADE}. Strength/
     * sound match {@link #AIRFRAME_PANEL}/{@link #FUSELAGE_FRAME}/{@link #ROTOR_HUB}/
     * {@link #ROTOR_BLADE}'s (a light sheet-metal/mechanical part, not a heavy engine
     * block) rather than {@link #HELICOPTER_ENGINE}'s industrial profile — a skid is
     * stamped/welded undercarriage metal, not a solid engine casting.
     */
    public static final Block LANDING_SKID = registerBlock(
            "landing_skid",
            LandingSkidBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.METAL),
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
            entries.accept(ROTOR_HUB.asItem());
            entries.accept(ROTOR_BLADE.asItem());
            entries.accept(LANDING_SKID.asItem());
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
