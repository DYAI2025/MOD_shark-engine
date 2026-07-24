package dev.sharkengine.content.block;

import net.minecraft.world.level.block.Block;

/**
 * Craftable copilot seat (REQ-009: {@code PartRole.COPILOT_SEAT}).
 *
 * <p>Deliberately a plain full-cube block with no custom {@code VoxelShape} or blockstate
 * property, same minimal registration pattern as {@link PilotSeatBlock} (see that class's
 * javadoc for why a dedicated class exists despite carrying no behavior of its own). Unlike
 * {@link PilotSeatBlock}'s single deterministic front-of-wheel anchor position (REQ-006/T06,
 * wheel-facing-derived), a copilot seat's {@code SeatAnchor} offset is simply wherever the
 * block was actually placed in the structure — see {@code
 * ShipAssemblyService#scanStructure}'s copilot-seat tracking. Seat-occupancy semantics (accept
 * an empty seat, reject an already-occupied one without displacing the existing occupant) live
 * on {@code ShipEntity}, not here — this class carries no assembly or occupancy logic of its
 * own, same separation {@link PilotSeatBlock} already follows.</p>
 */
public final class CopilotSeatBlock extends Block {

    public CopilotSeatBlock(Properties properties) {
        super(properties);
    }
}
