package dev.sharkengine.content.block;

import net.minecraft.world.level.block.Block;

/**
 * Generic pilot seat (REQ-005: {@code PartRole.PILOT_SEAT}).
 *
 * <p>Deliberately a plain full-cube block with no custom {@code VoxelShape} or
 * blockstate property — registered the same minimal way
 * {@link dev.sharkengine.content.ModBlocks#THRUSTER} is (see that class's
 * {@code registerBlock} call for this block). REQ-005 requires the seat's own
 * model/data to carry no AIR-specific naming so it is directly reusable by future
 * LAND/WATER vehicle profiles ("architektonisch auch für spätere LAND/WATER-Profile
 * wiederverwendbar" — see {@code docs/prd/shark-engine-air-release-1.prd.md}
 * REQ-005): this class has no AIR/ship-specific fields, properties, or behavior of
 * its own, and its id ({@code pilot_seat}) and translation keys are vehicle-class
 * neutral. A dedicated class (rather than reusing {@code Block} directly, the way
 * {@link dev.sharkengine.content.ModBlocks#THRUSTER} does) exists so future seat-
 * specific behavior (e.g. REQ-006's front-of-wheel anchor resolution, which is
 * {@code ShipAssemblyService}'s concern, not this block's) has an obvious, single
 * place to attach without touching the registration call site again.</p>
 *
 * <p>Assembly's seat-COUNT invariant (exactly one {@code PILOT_SEAT}-role part
 * required, REQ-005/AC-005) is enforced entirely by
 * {@link dev.sharkengine.ship.ShipAssemblyService} via the role-based
 * {@code ShipPartAnalyzer}/{@code ShipStats} aggregation — not by this class, which
 * carries no assembly logic of its own (same separation {@link LandingSkidBlock}/
 * {@link RotorBladeBlock} already follow for their own roles).</p>
 */
public final class PilotSeatBlock extends Block {

    public PilotSeatBlock(Properties properties) {
        super(properties);
    }
}
