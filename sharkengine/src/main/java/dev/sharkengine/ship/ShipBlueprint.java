package dev.sharkengine.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record ShipBlueprint(BlockPos origin, List<ShipBlock> blocks) {
    public record ShipBlock(int dx, int dy, int dz, BlockState state) {}
}
