package dev.sharkengine.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * AIR-030 datagen entrypoint. Registered under {@code fabric-datagen} in
 * {@code fabric.mod.json} and driven by {@code ./gradlew runDatagen}
 * (Fabric API's {@code fabricApi.configureDataGeneration()} in build.gradle).
 *
 * <p>Generates the resources that used to be hand-written under
 * {@code src/main/resources} for {@code thruster}, {@code steering_wheel}
 * and {@code bug} (recipe, loot table, blockstate, block/item model,
 * ship_eligible tag, en_us/de_de lang) into {@code src/main/generated},
 * which Loom adds as an extra resources source directory automatically.
 */
public final class SharkEngineDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(SharkEngineModelProvider::new);
        pack.addProvider(SharkEngineLootProvider::new);
        pack.addProvider(SharkEngineRecipeProvider::new);
        pack.addProvider(SharkEngineTagProvider::new);
        pack.addProvider(SharkEngineLangProvider.English::new);
        pack.addProvider(SharkEngineLangProvider.German::new);
    }
}
