package dev.sharkengine.content.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Custom item for the steering wheel block.
 *
 * <p>REQ-001 (AC-001): placement itself — including triggering the vehicle route-selection popup
 * — is handled by {@link SteeringWheelBlock#setPlacedBy}, which vanilla's default
 * {@link BlockItem#place} already invokes (with the placing player) after a successful placement.
 * No override is needed here; this class exists purely as the registered {@code Item} type for
 * the steering wheel block.</p>
 */
public final class SteeringWheelItem extends BlockItem {
    public SteeringWheelItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
}
