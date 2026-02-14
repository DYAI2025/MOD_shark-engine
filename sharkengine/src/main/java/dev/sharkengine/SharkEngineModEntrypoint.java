package dev.sharkengine;

import net.fabricmc.api.ModInitializer;

public final class SharkEngineModEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        SharkEngineMod.init();
    }
}
