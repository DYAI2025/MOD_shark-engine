package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.content.block.SteeringWheelBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
            true
    );

    private ModBlocks() {}

    public static void init() {
        // creative tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(STEERING_WHEEL.asItem());
        });
    }

    private static Block registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties props,
            boolean registerItem
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(SharkEngineMod.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        props.setId(blockKey);
        Block block = factory.apply(props);

        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        if (registerItem) {
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
            Item.Properties itemProps = new Item.Properties().useBlockDescriptionPrefix().setId(itemKey);
            Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProps));
        }

        return block;
    }
}
