package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * @param sessionId the REQ-003 {@code VehicleBuildSession} id the client was handed via {@link
 *                   BuilderPreviewS2CPayload#sessionId()}, or {@code null} if it never received
 *                   one. The server never derives authority from {@code wheelPos} alone — see
 *                   {@code dev.sharkengine.ship.BuildSessionGate#tryAssemble}.
 */
public record BuilderAssembleC2SPayload(BlockPos wheelPos, UUID sessionId) implements CustomPacketPayload {
    public static final Type<BuilderAssembleC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "builder_assemble"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuilderAssembleC2SPayload> CODEC =
            StreamCodec.of(BuilderAssembleC2SPayload::write, BuilderAssembleC2SPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, BuilderAssembleC2SPayload payload) {
        buf.writeBlockPos(payload.wheelPos);
        boolean hasSessionId = payload.sessionId != null;
        buf.writeBoolean(hasSessionId);
        if (hasSessionId) {
            buf.writeUUID(payload.sessionId);
        }
    }

    private static BuilderAssembleC2SPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos wheelPos = buf.readBlockPos();
        UUID sessionId = buf.readBoolean() ? buf.readUUID() : null;
        return new BuilderAssembleC2SPayload(wheelPos, sessionId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
