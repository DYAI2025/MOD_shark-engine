package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload for hovercraft flight input.
 * Replaces HelmInputC2SPayload with 3-axis translation model + player yaw.
 *
 * @param moveForward  forward/backward [-1..1]
 * @param moveStrafe   left/right [-1..1]
 * @param moveVertical up/down [-1..1]
 * @param playerYaw    player's horizontal look direction in degrees
 */
public record HovercraftInputC2SPayload(
        float moveForward,
        float moveStrafe,
        float moveVertical,
        float playerYaw
) implements CustomPacketPayload {

    public static final Type<HovercraftInputC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "hovercraft_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HovercraftInputC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, HovercraftInputC2SPayload::moveForward,
                    ByteBufCodecs.FLOAT, HovercraftInputC2SPayload::moveStrafe,
                    ByteBufCodecs.FLOAT, HovercraftInputC2SPayload::moveVertical,
                    ByteBufCodecs.FLOAT, HovercraftInputC2SPayload::playerYaw,
                    HovercraftInputC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
