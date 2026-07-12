package dev.sharkengine.datagen;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AIR-030: crafting recipes for thruster/steering_wheel/bug.
 *
 * <p>Deliberately does NOT use {@link net.minecraft.data.recipes.ShapedRecipeBuilder}:
 * {@code ShapedRecipeBuilder.save()} unconditionally synthesizes a
 * {@code has_the_recipe}/{@code RecipeUnlockedTrigger} advancement (verified
 * by decompiling {@code ShapedRecipeBuilder.save()} against the pinned
 * Minecraft jar — it calls {@code recipeOutput.advancement()...} even with
 * zero explicit {@code unlockedBy} calls, and throws if none are added at
 * all). None of the hand-written AIR-002 recipe files have a companion
 * advancement, and adding one would be a genuinely new file/behavior, not a
 * reformat — out of scope for a "semantically equal" migration. Building the
 * {@link ShapedRecipe} directly and calling the lower-level
 * {@link RecipeOutput#accept(ResourceLocation, net.minecraft.world.item.crafting.Recipe, net.minecraft.advancements.AdvancementHolder)}
 * with a {@code null} advancement reproduces the original files without
 * introducing one.
 */
final class SharkEngineRecipeProvider extends FabricRecipeProvider {

    SharkEngineRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void buildRecipes(RecipeOutput exporter) {
        // thruster: I=iron_ingot, F=fire_charge, B=blaze_rod, R=redstone
        // "IFI" / "BRB" / "IFI"
        shapedRecipe(
                exporter,
                "thruster",
                ModBlocks.THRUSTER.asItem(),
                Map.of(
                        'I', Ingredient.of(Items.IRON_INGOT),
                        'F', Ingredient.of(Items.FIRE_CHARGE),
                        'B', Ingredient.of(Items.BLAZE_ROD),
                        'R', Ingredient.of(Items.REDSTONE)
                ),
                "IFI", "BRB", "IFI"
        );

        // steering_wheel: P=#minecraft:planks, C=copper_ingot
        // " P " / "PCP" / " P "
        shapedRecipe(
                exporter,
                "steering_wheel",
                ModBlocks.STEERING_WHEEL.asItem(),
                Map.of(
                        'P', Ingredient.of(ItemTags.PLANKS),
                        'C', Ingredient.of(Items.COPPER_INGOT)
                ),
                " P ", "PCP", " P "
        );

        // bug: G=gold_ingot, R=redstone
        // " G " / "GRG" / " G "
        shapedRecipe(
                exporter,
                "bug",
                ModBlocks.BUG.asItem(),
                Map.of(
                        'G', Ingredient.of(Items.GOLD_INGOT),
                        'R', Ingredient.of(Items.REDSTONE)
                ),
                " G ", "GRG", " G "
        );

        // ─── AIR-040: crafting intermediates (concept doc §4 recipe table) ─────
        // Registered FIRST in this method's read order, matching the plan's own
        // "intermediates FIRST" requirement — every downstream part recipe
        // references one of these four ingredient ids.

        // metal_sheet: I=iron_ingot, C=copper_ingot — "II" / "CC", yield 4
        shapedRecipe(
                exporter,
                "metal_sheet",
                ModItems.METAL_SHEET,
                4,
                Map.of(
                        'I', Ingredient.of(Items.IRON_INGOT),
                        'C', Ingredient.of(Items.COPPER_INGOT)
                ),
                "II", "CC"
        );

        // rotor_shaft: I=iron_ingot, C=copper_ingot — column "I" / "C" / "I", yield 2
        shapedRecipe(
                exporter,
                "rotor_shaft",
                ModItems.ROTOR_SHAFT,
                2,
                Map.of(
                        'I', Ingredient.of(Items.IRON_INGOT),
                        'C', Ingredient.of(Items.COPPER_INGOT)
                ),
                "I", "C", "I"
        );

        // engine_core: I=iron_ingot, C=copper_ingot, R=redstone_block —
        // "ICI" / "CRC" / "ICI", yield 1
        shapedRecipe(
                exporter,
                "engine_core",
                ModItems.ENGINE_CORE,
                1,
                Map.of(
                        'I', Ingredient.of(Items.IRON_INGOT),
                        'C', Ingredient.of(Items.COPPER_INGOT),
                        'R', Ingredient.of(Items.REDSTONE_BLOCK)
                ),
                "ICI", "CRC", "ICI"
        );

        // bearing_assembly: shapeless — 1 iron_ingot + 2 copper_ingot, yield 2
        shapelessRecipe(
                exporter,
                "bearing_assembly",
                ModItems.BEARING_ASSEMBLY,
                2,
                Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(Items.COPPER_INGOT),
                Ingredient.of(Items.COPPER_INGOT)
        );

        // ─── AIR-040: airframe_panel (first core placeable part) ────────────────
        // airframe_panel: M=metal_sheet — "MM" (single row), yield 4
        shapedRecipe(
                exporter,
                "airframe_panel",
                ModBlocks.AIRFRAME_PANEL.asItem(),
                4,
                Map.of('M', Ingredient.of(ModItems.METAL_SHEET)),
                "MM"
        );

        // ─── AIR-040: fuselage_frame (second core placeable part) ───────────────
        // fuselage_frame: M=metal_sheet, I=iron_ingot — "MIM" (single row of 3),
        // yield 4. Concept doc §4 recipe table gives the shorthand "MIM" with no
        // row separators; unlike some other rows in that table (e.g. rotor_hub's
        // "B / S", which references a bearing_assembly not spelled out in the row
        // itself), both symbols here are already defined by the table's own legend
        // (M=metal_sheet, I=iron_ingot) — nothing is missing or ambiguous, this is
        // a plain 1x3 horizontal strip: metal_sheet, iron_ingot, metal_sheet.
        shapedRecipe(
                exporter,
                "fuselage_frame",
                ModBlocks.FUSELAGE_FRAME.asItem(),
                4,
                Map.of(
                        'M', Ingredient.of(ModItems.METAL_SHEET),
                        'I', Ingredient.of(Items.IRON_INGOT)
                ),
                "MIM"
        );

        // ─── AIR-040: helicopter_engine (fourth core placeable part) ────────────
        // helicopter_engine: M=metal_sheet, E=engine_core, S=rotor_shaft —
        // column "M" / "E" / "S", yield 1.
        shapedRecipe(
                exporter,
                "helicopter_engine",
                ModBlocks.HELICOPTER_ENGINE.asItem(),
                1,
                Map.of(
                        'M', Ingredient.of(ModItems.METAL_SHEET),
                        'E', Ingredient.of(ModItems.ENGINE_CORE),
                        'S', Ingredient.of(ModItems.ROTOR_SHAFT)
                ),
                "M", "E", "S"
        );

        // ─── AIR-040: rotor_hub (fifth core placeable part) ─────────────────────
        // rotor_hub: B=bearing_assembly, S=rotor_shaft — column "B" / "S", yield 1.
        shapedRecipe(
                exporter,
                "rotor_hub",
                ModBlocks.ROTOR_HUB.asItem(),
                1,
                Map.of(
                        'B', Ingredient.of(ModItems.BEARING_ASSEMBLY),
                        'S', Ingredient.of(ModItems.ROTOR_SHAFT)
                ),
                "B", "S"
        );

        // ─── AIR-040: rotor_blade (sixth and last core placeable part) ──────────
        // rotor_blade: S=rotor_shaft, M=metal_sheet — single row "SMM", yield 2.
        shapedRecipe(
                exporter,
                "rotor_blade",
                ModBlocks.ROTOR_BLADE.asItem(),
                2,
                Map.of(
                        'S', Ingredient.of(ModItems.ROTOR_SHAFT),
                        'M', Ingredient.of(ModItems.METAL_SHEET)
                ),
                "SMM"
        );

        // ─── AIR-040: landing_skid (seventh and last core placeable part) ───────
        // landing_skid: I=iron_ingot, M=metal_sheet — "I I" (iron, gap, iron) /
        // "MMM", yield 2. Concept doc §4 recipe table gives this as "I I / MMM";
        // the middle space in the top row is a genuine empty crafting-grid slot
        // (ShapedRecipePattern.of treats an unmapped space character as empty, the
        // same convention already used by steering_wheel's " P " / "PCP" / " P "
        // and bug's " G " / "GRG" / " G " rows above), not a typo to collapse away.
        shapedRecipe(
                exporter,
                "landing_skid",
                ModBlocks.LANDING_SKID.asItem(),
                2,
                Map.of(
                        'I', Ingredient.of(Items.IRON_INGOT),
                        'M', Ingredient.of(ModItems.METAL_SHEET)
                ),
                "I I", "MMM"
        );
    }

    private void shapedRecipe(
            RecipeOutput exporter,
            String path,
            Item result,
            Map<Character, Ingredient> key,
            String... pattern
    ) {
        shapedRecipe(exporter, path, result, 1, key, pattern);
    }

    private void shapedRecipe(
            RecipeOutput exporter,
            String path,
            Item result,
            int count,
            Map<Character, Ingredient> key,
            String... pattern
    ) {
        ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, pattern);
        ShapedRecipe recipe = new ShapedRecipe(
                "",
                CraftingBookCategory.MISC,
                shapedPattern,
                new ItemStack(result, count)
        );
        exporter.accept(ResourceLocation.fromNamespaceAndPath(output.getModId(), path), recipe, null);
    }

    private void shapelessRecipe(
            RecipeOutput exporter,
            String path,
            Item result,
            int count,
            Ingredient... ingredients
    ) {
        ShapelessRecipe recipe = new ShapelessRecipe(
                "",
                CraftingBookCategory.MISC,
                new ItemStack(result, count),
                NonNullList.of(Ingredient.EMPTY, ingredients)
        );
        exporter.accept(ResourceLocation.fromNamespaceAndPath(output.getModId(), path), recipe, null);
    }
}
