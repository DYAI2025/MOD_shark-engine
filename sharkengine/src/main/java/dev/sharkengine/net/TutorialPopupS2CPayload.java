package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.tutorial.TutorialPopupStage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TutorialPopupS2CPayload(TutorialPopupStage stage) implements CustomPacketPayload {
    public static final Type<TutorialPopupS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "tutorial_popup")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialPopupS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, payload -> payload.stage.id(),
                    id -> new TutorialPopupS2CPayload(TutorialPopupStage.fromId(id))
            );

    public static TutorialPopupS2CPayload forStage(TutorialPopupStage stage) {
        return new TutorialPopupS2CPayload(stage);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
