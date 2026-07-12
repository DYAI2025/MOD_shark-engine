package dev.sharkengine.datagen;

import dev.sharkengine.content.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

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
    }

    private void shapedRecipe(
            RecipeOutput exporter,
            String path,
            net.minecraft.world.item.Item result,
            Map<Character, Ingredient> key,
            String... pattern
    ) {
        ShapedRecipePattern shapedPattern = ShapedRecipePattern.of(key, pattern);
        ShapedRecipe recipe = new ShapedRecipe(
                "",
                CraftingBookCategory.MISC,
                shapedPattern,
                new ItemStack(result, 1)
        );
        exporter.accept(ResourceLocation.fromNamespaceAndPath(output.getModId(), path), recipe, null);
    }
}
