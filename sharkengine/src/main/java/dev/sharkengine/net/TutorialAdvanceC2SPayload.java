package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.tutorial.TutorialPopupStage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TutorialAdvanceC2SPayload(TutorialPopupStage stage) implements CustomPacketPayload {
    public static final Type<TutorialAdvanceC2SPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "tutorial_advance")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialAdvanceC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    payload -> payload.stage().id(),
                    id -> new TutorialAdvanceC2SPayload(TutorialPopupStage.fromId(id))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
