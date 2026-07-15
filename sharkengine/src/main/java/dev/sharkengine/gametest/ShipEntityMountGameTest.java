package dev.sharkengine.gametest;

import dev.sharkengine.content.ModBlocks;
import dev.sharkengine.content.ModEntities;
import dev.sharkengine.ship.ShipEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Regression coverage for the "helicopter_engine/rotor_shaft/metal_sheet können nicht
 * angebaut werden, es passiert einfach nichts" live playtest bug (2026-07-13):
 * {@link ShipEntity#interact} used to unconditionally mount the player (returning
 * {@code CONSUME}, swallowing the interaction) on ANY right-click that wasn't
 * shift-held or holding wood/planks — including one holding a block the player meant
 * to place. Since the entity's hitbox spans the whole launched ship's footprint, this
 * silently defeated every attempt to attach a new part to an already-assembled ship.
 *
 * <p>Three cases, all against a freshly-spawned {@link ShipEntity} with no pilot yet:</p>
 * <ul>
 *   <li>{@link #emptyHandRightClickStillMounts}: the pre-existing, still-required
 *       behavior — right-clicking with an empty hand mounts the player as pilot.</li>
 *   <li>{@link #heldBlockRightClickPassesThroughInsteadOfMounting}: the fix — holding a
 *       placeable block (here {@code helicopter_engine}) makes {@code interact} return
 *       {@code PASS} and leaves the player unmounted, so vanilla's normal block-placement
 *       path gets a chance to run instead.</li>
 *   <li>{@link #heldNonBlockItemRightClickStillMounts}: regression guard for a bug this
 *       same fix introduced and then corrected same-day (caught by an
 *       ultrathink-craftsmanship gate, not a live report) — the first version of the fix
 *       checked "any non-empty hand" instead of "holding a block", which silently broke
 *       mounting while holding a sword/torch/food/anything non-block. Holding a plain
 *       non-block item (here a stick) must still mount, exactly like the empty-hand
 *       case.</li>
 * </ul>
 */
public final class ShipEntityMountGameTest implements FabricGameTest {

    private static final BlockPos SHIP_POS = new BlockPos(3, 2, 3);

    @GameTest(template = EMPTY_STRUCTURE)
    public void emptyHandRightClickStillMounts(GameTestHelper helper) {
        ShipEntity ship = helper.spawn(ModEntities.SHIP, SHIP_POS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        InteractionResult result = ship.interact(player, InteractionHand.MAIN_HAND);

        if (result != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME (mount) for an empty-hand right-click, got " + result);
            return;
        }
        if (player.getVehicle() != ship) {
            helper.fail("expected player to be mounted on the ship after an empty-hand right-click");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void heldBlockRightClickPassesThroughInsteadOfMounting(GameTestHelper helper) {
        ShipEntity ship = helper.spawn(ModEntities.SHIP, SHIP_POS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.HELICOPTER_ENGINE.asItem()));

        InteractionResult result = ship.interact(player, InteractionHand.MAIN_HAND);

        if (result != InteractionResult.PASS) {
            helper.fail("expected PASS (let vanilla try placement) when holding a block, got " + result);
            return;
        }
        if (player.getVehicle() == ship) {
            helper.fail("player must NOT be mounted when the right-click was holding a placeable block");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void heldNonBlockItemRightClickStillMounts(GameTestHelper helper) {
        ShipEntity ship = helper.spawn(ModEntities.SHIP, SHIP_POS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));

        InteractionResult result = ship.interact(player, InteractionHand.MAIN_HAND);

        if (result != InteractionResult.CONSUME) {
            helper.fail("expected CONSUME (mount) when holding a non-block item, got " + result);
            return;
        }
        if (player.getVehicle() != ship) {
            helper.fail("expected player to be mounted on the ship even while holding a non-block item "
                    + "(a stick can't be placed, so it must not block mounting)");
            return;
        }
        helper.succeed();
    }
}
