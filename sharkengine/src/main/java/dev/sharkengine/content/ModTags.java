package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> SHIP_ELIGIBLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "ship_eligible"));

    private ModTags() {}
}
