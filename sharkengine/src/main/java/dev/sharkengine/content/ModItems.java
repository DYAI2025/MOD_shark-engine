package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * AIR-040: crafting-intermediate items.
 *
 * <p>These are the 4 intermediates from the concept doc's §4 "Zwischenprodukte &
 * Rezepte" recipe table — {@code metal_sheet}, {@code rotor_shaft}, {@code engine_core},
 * {@code bearing_assembly} — every downstream helicopter/fixed-wing part recipe
 * references one or more of them. They are plain crafting reagents, never placed in
 * the world: no {@code Block}/blockstate/VoxelShape, no {@code ModTags.SHIP_ELIGIBLE}
 * membership, no {@code VehiclePartDefinition} (AIR-020's registry only resolves
 * placed/registered blocks). Registered the same way {@link ModBlocks} registers its
 * items, minus the {@code Block} half.
 */
public final class ModItems {
    public static final Item METAL_SHEET = registerItem("metal_sheet");
    public static final Item ROTOR_SHAFT = registerItem("rotor_shaft");
    public static final Item ENGINE_CORE = registerItem("engine_core");
    public static final Item BEARING_ASSEMBLY = registerItem("bearing_assembly");

    private ModItems() {}

    public static void init() {
        // creative tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(METAL_SHEET);
            entries.accept(ROTOR_SHAFT);
            entries.accept(ENGINE_CORE);
            entries.accept(BEARING_ASSEMBLY);
        });
    }

    private static Item registerItem(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, name);
        Item item = new Item(new Item.Properties());
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }
}
