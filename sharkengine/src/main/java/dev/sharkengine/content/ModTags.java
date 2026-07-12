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
    // here once for the whole task family. AIRCRAFT_STRUCTURE (airframe_panel,
    // fuselage_frame), PROPULSION (helicopter_engine), ROTOR_HUBS (rotor_hub), and
    // ROTOR_BLADES (rotor_blade) are all populated by AIR-040. LIFT_SURFACES is
    // populated ahead of its "natural" owner (wing_root/wing_panel/wing_tip, AIR-041)
    // by rotor_blade — a lift>0-based membership choice, not a strict PartRole match;
    // see SharkEngineTagProvider's addTags() for the full rationale. Declared once,
    // here — do not re-add if a later stage already introduced these.
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
