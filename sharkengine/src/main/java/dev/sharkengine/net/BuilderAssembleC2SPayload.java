package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BuilderAssembleC2SPayload(BlockPos wheelPos) implements CustomPacketPayload {
    public static final Type<BuilderAssembleC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "builder_assemble"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuilderAssembleC2SPayload> CODEC =
            StreamCodec.of(BuilderAssembleC2SPayload::write, BuilderAssembleC2SPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, BuilderAssembleC2SPayload payload) {
        buf.writeBlockPos(payload.wheelPos);
    }

    private static BuilderAssembleC2SPayload read(RegistryFriendlyByteBuf buf) {
        return new BuilderAssembleC2SPayload(buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
