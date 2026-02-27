package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.ship.VehicleClass;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TutorialModeSelectionC2SPayload(VehicleClass vehicleClass) implements CustomPacketPayload {
    public static final Type<TutorialModeSelectionC2SPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "tutorial_mode_selection")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialModeSelectionC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    payload -> payload.vehicleClass.name(),
                    name -> new TutorialModeSelectionC2SPayload(VehicleClass.valueOf(name))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
