package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.ship.ShipEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<ShipEntity> SHIP = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "ship"),
            EntityType.Builder.<ShipEntity>of(ShipEntity::new, MobCategory.MISC)
                    .sized(2.5f, 1.5f)
                    .build("sharkengine:ship")
    );

    private ModEntities() {}

    public static void init() {
        // nothing yet
    }
}
