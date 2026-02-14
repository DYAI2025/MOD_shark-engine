package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HelmInputC2SPayload(float throttle, float turn) implements CustomPacketPayload {
    public static final Type<HelmInputC2SPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "helm_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HelmInputC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, HelmInputC2SPayload::throttle,
                    ByteBufCodecs.FLOAT, HelmInputC2SPayload::turn,
                    HelmInputC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
