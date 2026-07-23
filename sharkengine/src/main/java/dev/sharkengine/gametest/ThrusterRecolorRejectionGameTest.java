package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModComponents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * REQ-018/AC-018 (T20) falsifying-test contract (test-plan {@code
 * docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-018 — Single Thruster
 * item with craft-time DyeColor component"):
 *
 * <ol>
 *   <li>{@link #craftingWithDyeYieldsComponentedThruster} — the CRAFT-TIME half: thruster + dye
 *       in a crafting grid matches exactly the single dye-agnostic coloring recipe and yields
 *       the SAME single thruster item carrying a {@code trail_color} component of that dye
 *       (never a second color-specific item id — the council-rejected 16-item design,
 *       LED-002/RISK-008).</li>
 *   <li>{@link #dyeOnPlacedThrusterIsNoOp} — the POST-PLACEMENT half: right-clicking an
 *       already-placed thruster with a dye is a complete no-op (block state unchanged, dye not
 *       consumed, no block entity materializing as hidden color storage) — craft-time-only
 *       coloring per the user's OQ-002 decision ("kein nachträgliches Färben"); the tester's
 *       subtler false positive is a leftover dye-on-block path that (1) and registry tests
 *       cannot see.</li>
 * </ol>
 *
 * <p>The registry/recipe-file halves of AC-018 (exactly one item id, exactly one recipe JSON)
 * are unit-locked in {@code ResourceValidationTest.ThrusterDyeComponentResourceTests}.</p>
 */
public final class ThrusterRecolorRejectionGameTest implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void craftingWithDyeYieldsComponentedThruster(GameTestHelper helper) {
        ItemStack thruster = new ItemStack(ModBlocks.THRUSTER);
        ItemStack dye = new ItemStack(Items.RED_DYE);
        CraftingInput input = CraftingInput.of(2, 1, List.of(thruster, dye));

        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        if (match.isEmpty()) {
            helper.fail("no crafting recipe matches thruster + red dye — the REQ-018 single "
                    + "coloring recipe does not exist");
            return;
        }
        ItemStack result = match.get().value().assemble(input, helper.getLevel().registryAccess());
        if (!result.is(ModBlocks.THRUSTER.asItem())) {
            helper.fail("coloring recipe produced " + result + " instead of the single thruster item "
                    + "— a color-specific item id is the rejected 16-item design");
        }
        if (result.get(ModComponents.TRAIL_COLOR) != DyeColor.RED) {
            helper.fail("crafted thruster carries trail_color=" + result.get(ModComponents.TRAIL_COLOR)
                    + " instead of RED");
        }
        if (result.getCount() != 1) {
            helper.fail("coloring must yield exactly 1 thruster, got " + result.getCount());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void dyeOnPlacedThrusterIsNoOp(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.THRUSTER);
        BlockState before = helper.getBlockState(pos);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos abs = helper.absolutePos(pos);
        player.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() - 1.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE, 4));

        helper.useBlock(pos, player);

        if (!helper.getBlockState(pos).equals(before)) {
            helper.fail("dye right-click mutated the placed thruster's block state — "
                    + "post-placement recoloring must not exist (OQ-002 decision)");
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getCount() != 4) {
            helper.fail("dye was consumed on a placed thruster — a hidden recolor path exists");
        }
        if (helper.getLevel().getBlockEntity(abs) != null) {
            helper.fail("a block entity appeared on the thruster after dye use — hidden color storage");
        }
        helper.succeed();
    }
}
