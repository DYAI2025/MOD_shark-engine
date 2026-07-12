package dev.sharkengine.datagen;

import dev.sharkengine.content.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

/**
 * AIR-030: loot tables for thruster/steering_wheel/bug.
 *
 * <p>{@link #dropSelf} is vanilla's {@code BlockLootSubProvider} helper and
 * produces exactly the loot table shape the hand-written AIR-002 files use:
 * a single pool, {@code rolls: 1}, one item entry for the block itself,
 * gated by a {@code survives_explosion} condition (see
 * {@code BlockLootSubProvider.createSingleItemTable}/{@code dropSelf}) —
 * verified by decompiling the pinned Loom/Minecraft classes, not assumed
 * from general Fabric docs.
 */
final class SharkEngineLootProvider extends FabricBlockLootTableProvider {

    SharkEngineLootProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.THRUSTER);
        dropSelf(ModBlocks.STEERING_WHEEL);
        dropSelf(ModBlocks.BUG);
        dropSelf(ModBlocks.AIRFRAME_PANEL);
        dropSelf(ModBlocks.FUSELAGE_FRAME);
        dropSelf(ModBlocks.HELICOPTER_ENGINE);
        dropSelf(ModBlocks.ROTOR_HUB);
        dropSelf(ModBlocks.ROTOR_BLADE);
        dropSelf(ModBlocks.LANDING_SKID);
    }
}
