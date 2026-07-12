package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> SHIP_ELIGIBLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "ship_eligible"));

    // AIR-040 (concept doc §5.4): the five new aircraft-part role tags, introduced
    // here once for the whole task family. Only AIRCRAFT_STRUCTURE is populated by
    // this task's own scope (airframe_panel); the other four are declared ahead of
    // the parts that will populate them (LIFT_SURFACES: wing_root/wing_panel/wing_tip,
    // AIR-041; PROPULSION: helicopter_engine; ROTOR_HUBS: rotor_hub; ROTOR_BLADES:
    // rotor_blade — all later in this same AIR-040 task's remaining scope /
    // AIR-041/AIR-050). Declared once, here — do not re-add if a later stage already
    // introduced these.
    public static final TagKey<Block> AIRCRAFT_STRUCTURE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "aircraft_structure"));
    public static final TagKey<Block> LIFT_SURFACES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "lift_surfaces"));
    public static final TagKey<Block> PROPULSION =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "propulsion"));
    public static final TagKey<Block> ROTOR_HUBS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "rotor_hubs"));
    public static final TagKey<Block> ROTOR_BLADES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "rotor_blades"));

    private ModTags() {}
}
