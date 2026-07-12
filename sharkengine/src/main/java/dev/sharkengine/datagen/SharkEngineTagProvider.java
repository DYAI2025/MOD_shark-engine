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
        builder.add(key(ModBlocks.HELICOPTER_ENGINE));
        builder.add(key(ModBlocks.ROTOR_HUB));
        builder.add(key(ModBlocks.ROTOR_BLADE));
        builder.add(key(ModBlocks.LANDING_SKID));

        // AIR-040 (concept doc §5.4): role tags, populated as each part lands.
        // airframe_panel's and fuselage_frame's own scope notes explicitly require
        // "ship_eligible + aircraft_structure tag membership" — added here.
        tag(ModTags.AIRCRAFT_STRUCTURE).add(key(ModBlocks.AIRFRAME_PANEL));
        tag(ModTags.AIRCRAFT_STRUCTURE).add(key(ModBlocks.FUSELAGE_FRAME));

        // helicopter_engine's own scope note explicitly requires
        // "ship_eligible + propulsion tags" — added here.
        tag(ModTags.PROPULSION).add(key(ModBlocks.HELICOPTER_ENGINE));

        // rotor_hub's own scope note explicitly requires
        // "ship_eligible + rotor_hubs tags" — added here.
        tag(ModTags.ROTOR_HUBS).add(key(ModBlocks.ROTOR_HUB));

        // rotor_blade's own scope note explicitly requires "ship_eligible +
        // rotor_blades + lift_surfaces tags" — added here.
        tag(ModTags.ROTOR_BLADES).add(key(ModBlocks.ROTOR_BLADE));

        // Deliberate choice, documented: LIFT_SURFACES membership is lift>0-based,
        // not strictly PartRole==LIFT_SURFACE-based. rotor_blade's own PartRole is
        // ROTOR_BLADE (concept §3.3's PartRole enum), a distinct role from
        // LIFT_SURFACE (wing_root/wing_panel/wing_tip, AIR-041) — but concept §4's
        // balance table gives it lift=8, the single highest per-block lift figure in
        // the entire table, strictly greater than any dedicated LIFT_SURFACE part
        // (wing_panel's 4 is the next highest). A content/data-file tag meant to let
        // players and future tooling ("what generates lift in this mod?") discover
        // every lift-contributing block by tag would be actively misleading if it
        // silently excluded the single biggest lift contributor. The concept doc's §4
        // dash ("–") cells mean "no contribution" for every non-lift part; rotor_blade
        // is unambiguously NOT a dash cell. Tag membership here is therefore driven by
        // "does this block have lift > 0 in VehicleBalance.PARTS", not by a strict
        // PartRole match — unlike AIRCRAFT_STRUCTURE/PROPULSION/ROTOR_HUBS above,
        // which line up 1:1 with their PartRole today because no part yet has a
        // nonzero value in one of those columns under a *different* role.
        // Flight-rule *logic* (AIR-054/AIR-060) is unaffected by this: those rules are
        // required (REQ-S1) to be role-based via VehiclePartRegistry/ShipPartAnalyzer,
        // never tag-based — this tag is a content-classification convenience only, not
        // a gameplay-rule input.
        tag(ModTags.LIFT_SURFACES).add(key(ModBlocks.ROTOR_BLADE));

        // landing_skid (LANDING_GEAR role) deliberately gets no role tag beyond
        // ship_eligible above: concept §5.4 lists exactly five new role tags
        // (aircraft_structure, lift_surfaces, propulsion, rotor_hubs, rotor_blades) and
        // LANDING_GEAR is not one of them — landing gear is neither structure, lift,
        // propulsion, nor rotor hardware, so it stays ship_eligible-only, same as
        // steering_wheel/bug/thruster before AIR-040 introduced any role tags at all.
    }

    private static ResourceKey<Block> key(Block block) {
        return block.builtInRegistryHolder().key();
    }
}
