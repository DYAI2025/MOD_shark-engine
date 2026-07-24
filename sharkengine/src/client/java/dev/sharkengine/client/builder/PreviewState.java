package dev.sharkengine.client.builder;

import dev.sharkengine.ship.part.AssemblyIssue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public record PreviewState(BlockPos wheelPos,
                    ResourceKey<Level> dimension,
                    List<BlockPos> validBlocks,
                    List<BlockPos> invalidBlocks,
                    boolean canAssemble,
                    int contactPoints,
                    int thrusterCount,
                    int coreNeighbors,
                    int bugCount,
                    List<AssemblyIssue> issues,
                    UUID sessionId) {
}
