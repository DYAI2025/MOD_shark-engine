package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * REQ-018/T20: custom data components.
 *
 * <p>{@link #TRAIL_COLOR} is the craft-time {@link DyeColor} carried by a colored Thruster
 * ITEM STACK — the council-approved single-item design (LED-002/RISK-008 resolution: one
 * {@code sharkengine:thruster} id with an optional component, explicitly NOT sixteen
 * {@code thruster_<color>} ids). It is set exclusively by {@code ThrusterColoringRecipe} at
 * craft time; no interaction path may mutate it after placement
 * ({@code ThrusterRecolorRejectionGameTest}). Absent component = default trail (REQ-019).</p>
 */
public final class ModComponents {

    public static final DataComponentType<DyeColor> TRAIL_COLOR = DataComponentType.<DyeColor>builder()
            .persistent(DyeColor.CODEC)
            .networkSynchronized(ByteBufCodecs.idMapper(DyeColor::byId, DyeColor::getId))
            .build();

    private ModComponents() {}

    public static void init() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "trail_color"),
                TRAIL_COLOR);
    }
}
