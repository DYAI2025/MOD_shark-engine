package dev.sharkengine.client.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

record PreviewState(BlockPos wheelPos,
                    ResourceKey<Level> dimension,
                    List<BlockPos> validBlocks,
                    List<BlockPos> invalidBlocks,
                    boolean canAssemble,
                    int contactPoints,
                    int thrusterCount,
                    int coreNeighbors) {
}
