package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

/**
 * REQ-018/T20: custom recipe serializers. {@code sharkengine:thruster_coloring} is the single
 * serializer behind the one dye-agnostic coloring recipe file — see
 * {@link ThrusterColoringRecipe} for why it must be a custom matcher.
 */
public final class ModRecipeSerializers {

    public static final RecipeSerializer<ThrusterColoringRecipe> THRUSTER_COLORING =
            new SimpleCraftingRecipeSerializer<>(ThrusterColoringRecipe::new);

    private ModRecipeSerializers() {}

    public static void init() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "thruster_coloring"),
                THRUSTER_COLORING);
    }
}
