package dev.sharkengine.client.tutorial;

import dev.sharkengine.net.TutorialPopupS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class TutorialPopupClient {
    private TutorialPopupClient() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TutorialPopupS2CPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> showPopup(payload))
        );
    }

    private static void showPopup(TutorialPopupS2CPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new TutorialPopupScreen(payload.stage()));
    }
}
