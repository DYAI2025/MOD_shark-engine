package dev.sharkengine.content;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * REQ-018/T20: the single, dye-agnostic thruster-coloring recipe — one Thruster plus one dye
 * anywhere in the crafting grid yields the SAME single thruster item carrying a craft-time
 * {@link ModComponents#TRAIL_COLOR} component of that dye.
 *
 * <p><b>Why a custom recipe class:</b> a static recipe JSON cannot derive the output component
 * from the input dye — the alternative would be 16 per-dye JSON files producing 16 outputs,
 * which is exactly the council-REJECTED design (LED-002/RISK-008). One custom matcher = one
 * recipe id ({@code sharkengine:thruster_coloring}) for all 16 dyes, guarded by
 * {@code ResourceValidationTest.ThrusterDyeComponentResourceTests}.</p>
 *
 * <p>Re-crafting an already-colored thruster with a different dye is still craft-time coloring
 * and overwrites the component. POST-placement recoloring does not exist anywhere
 * ({@code ThrusterRecolorRejectionGameTest}, OQ-002 user decision: "ausschließlich Crafting").</p>
 */
public class ThrusterColoringRecipe extends CustomRecipe {

    public ThrusterColoringRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack thruster = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModBlocks.THRUSTER.asItem())) {
                if (!thruster.isEmpty()) {
                    return false; // exactly one thruster
                }
                thruster = stack;
            } else if (stack.getItem() instanceof DyeItem) {
                if (!dye.isEmpty()) {
                    return false; // exactly one dye
                }
                dye = stack;
            } else {
                return false; // nothing else in the grid
            }
        }
        return !thruster.isEmpty() && !dye.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack thruster = ItemStack.EMPTY;
        DyeColor color = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModBlocks.THRUSTER.asItem())) {
                thruster = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem) {
                color = dyeItem.getDyeColor();
            }
        }
        if (thruster.isEmpty() || color == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = thruster.copyWithCount(1);
        result.set(ModComponents.TRAIL_COLOR, color);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.THRUSTER_COLORING;
    }
}
