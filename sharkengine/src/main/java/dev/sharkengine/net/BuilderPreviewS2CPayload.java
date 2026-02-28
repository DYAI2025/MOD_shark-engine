package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record BuilderPreviewS2CPayload(BlockPos wheelPos,
                                       CompoundTag blueprintNbt,
                                       List<BlockPos> invalidBlocks,
                                       int contactPoints,
                                       boolean canAssemble,
                                       int thrusterCount,
                                       int coreNeighbors,
                                       boolean active) implements CustomPacketPayload {

    public static final Type<BuilderPreviewS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "builder_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuilderPreviewS2CPayload> CODEC =
            StreamCodec.of(BuilderPreviewS2CPayload::write, BuilderPreviewS2CPayload::read);

    public static BuilderPreviewS2CPayload open(BlockPos wheelPos,
                                                CompoundTag blueprintNbt,
                                                List<BlockPos> invalidBlocks,
                                                int contactPoints,
                                                boolean canAssemble,
                                                int thrusterCount,
                                                int coreNeighbors) {
        return new BuilderPreviewS2CPayload(wheelPos, blueprintNbt, List.copyOf(invalidBlocks), contactPoints, canAssemble, thrusterCount, coreNeighbors, true);
    }

    public static BuilderPreviewS2CPayload close() {
        return new BuilderPreviewS2CPayload(BlockPos.ZERO, null, List.of(), 0, false, 0, 0, false);
    }

    private static void write(RegistryFriendlyByteBuf buf, BuilderPreviewS2CPayload payload) {
        buf.writeBlockPos(payload.wheelPos);
        boolean hasBlueprint = payload.blueprintNbt != null;
        buf.writeBoolean(hasBlueprint);
        if (hasBlueprint) {
            buf.writeNbt(payload.blueprintNbt);
        }
        ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC)
                .encode(buf, new ArrayList<>(payload.invalidBlocks()));
        buf.writeInt(payload.contactPoints());
        buf.writeBoolean(payload.canAssemble());
        buf.writeInt(payload.thrusterCount());
        buf.writeInt(payload.coreNeighbors());
        buf.writeBoolean(payload.active());
    }

    private static BuilderPreviewS2CPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos wheelPos = buf.readBlockPos();
        CompoundTag blueprint = null;
        boolean hasBlueprint = buf.readBoolean();
        if (hasBlueprint) {
            blueprint = buf.readNbt();
        }
        List<BlockPos> invalidBlocks = ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC).decode(buf);
        int contacts = buf.readInt();
        boolean canAssemble = buf.readBoolean();
        int thrusterCount = buf.readInt();
        int coreNeighbors = buf.readInt();
        boolean active = buf.readBoolean();
        return new BuilderPreviewS2CPayload(wheelPos, blueprint, invalidBlocks, contacts, canAssemble, thrusterCount, coreNeighbors, active);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
