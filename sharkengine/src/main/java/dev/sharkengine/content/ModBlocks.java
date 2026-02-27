package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
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

    private ModBlocks() {}

    public static void init() {
        // creative tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(STEERING_WHEEL.asItem());
            entries.accept(THRUSTER.asItem());
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
