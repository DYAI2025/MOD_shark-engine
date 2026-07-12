package dev.sharkengine.datagen;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

/**
 * AIR-030: {@code data/sharkengine/tags/block/ship_eligible.json}.
 *
 * <p>Uses {@link TagsProvider.TagAppender} directly rather than
 * {@code FabricTagProvider.FabricTagBuilder}: empirically (the compiler,
 * not the sources jar, is ground truth here) {@code tag(TagKey)} as
 * resolved against this project's actual dependency bytecode returns the
 * raw {@code TagAppender}, not the covariant {@code FabricTagBuilder} the
 * (slightly newer) sources jar suggests — so block elements are added via
 * {@code ResourceKey<Block>} (matching {@code BlockTagProvider#reverseLookup},
 * {@code block.builtInRegistryHolder().key()}) instead of the {@code add(Block)}
 * convenience overload.
 *
 * <p><b>Known, understood diff from the hand-written file:</b> the five
 * vanilla tag references ({@code #minecraft:planks} etc.) use
 * {@link TagsProvider.TagAppender#addTag}, which datagen serializes as
 * {@code required: true} — but a single-mod datagen run has no visibility
 * into vanilla's own tag data (only into providers it registers itself), so
 * {@code TagsProvider.run()}'s own build-time completeness check rejects it
 * ("Couldn't define tag ... missing following references", reproduced and
 * confirmed by decompiling {@code TagsProvider.run()} against the pinned
 * Minecraft jar: the {@code required} check for a referenced *tag* only
 * consults this run's own builders + an empty parent {@code TagLookup},
 * never the actual registry contents). {@link TagsProvider.TagAppender#addOptionalTag}
 * is Fabric's documented, intended workaround for referencing another mod's
 * (or vanilla's) tag from datagen — it serializes as
 * {@code {"id": "#minecraft:planks", "required": false}} instead of the bare
 * string the hand file used. Behaviourally identical here: these are core
 * vanilla tags that always exist on a real MC 1.21.1 install, so
 * required-vs-optional never actually changes which blocks satisfy
 * {@code ship_eligible} at runtime.
 */
final class SharkEngineTagProvider extends FabricTagProvider.BlockTagProvider {

    SharkEngineTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        TagsProvider.TagAppender<Block> builder = tag(ModTags.SHIP_ELIGIBLE);
        builder.add(key(ModBlocks.STEERING_WHEEL));
        builder.addOptionalTag(BlockTags.PLANKS.location());
        builder.addOptionalTag(BlockTags.LOGS.location());
        builder.addOptionalTag(BlockTags.WOODEN_SLABS.location());
        builder.addOptionalTag(BlockTags.WOODEN_STAIRS.location());
        builder.addOptionalTag(BlockTags.WOOL.location());
        builder.add(key(Blocks.GLASS));
        builder.add(key(ModBlocks.THRUSTER));
        builder.add(key(ModBlocks.BUG));
        builder.add(key(ModBlocks.AIRFRAME_PANEL));
        builder.add(key(ModBlocks.FUSELAGE_FRAME));

        // AIR-040 (concept doc §5.4): role tags, populated as each part lands.
        // airframe_panel's and fuselage_frame's own scope notes explicitly require
        // "ship_eligible + aircraft_structure tag membership" — added here.
        tag(ModTags.AIRCRAFT_STRUCTURE).add(key(ModBlocks.AIRFRAME_PANEL));
        tag(ModTags.AIRCRAFT_STRUCTURE).add(key(ModBlocks.FUSELAGE_FRAME));
    }

    private static ResourceKey<Block> key(Block block) {
        return block.builtInRegistryHolder().key();
    }
}
