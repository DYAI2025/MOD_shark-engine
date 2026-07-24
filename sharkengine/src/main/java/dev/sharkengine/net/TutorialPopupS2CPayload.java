package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.ship.VehicleClass;
import dev.sharkengine.tutorial.TutorialPopupStage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * REQ-001 (AC-001): for {@link TutorialPopupStage#MODE_SELECTION} — the vehicle route-selection
 * popup — {@code routes} carries the exact set of route identifiers the popup offers, so the
 * trigger's correctness (does placing/interacting with a Steering Wheel actually offer AIR, LAND,
 * AND WATER — not a shallow subset) is verifiable server-side from the payload itself, without
 * needing a client screenshot. Stages other than {@code MODE_SELECTION} carry an empty list.
 */
public record TutorialPopupS2CPayload(TutorialPopupStage stage, List<VehicleClass> routes) implements CustomPacketPayload {
    public static final Type<TutorialPopupS2CPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "tutorial_popup")
    );

    public TutorialPopupS2CPayload {
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialPopupS2CPayload> CODEC =
            StreamCodec.of(TutorialPopupS2CPayload::write, TutorialPopupS2CPayload::read);

    /** Non-route stages (WELCOME, BUILD_GUIDE, READY_TO_LAUNCH, FLIGHT_TIPS): no routes attached. */
    public static TutorialPopupS2CPayload forStage(TutorialPopupStage stage) {
        return new TutorialPopupS2CPayload(stage, List.of());
    }

    /** REQ-001 (AC-001): the route-selection popup, carrying the routes it offers. */
    public static TutorialPopupS2CPayload forModeSelection(List<VehicleClass> routes) {
        return new TutorialPopupS2CPayload(TutorialPopupStage.MODE_SELECTION, routes);
    }

    private static void write(RegistryFriendlyByteBuf buf, TutorialPopupS2CPayload payload) {
        buf.writeUtf(payload.stage.id());
        buf.writeVarInt(payload.routes.size());
        for (VehicleClass route : payload.routes) {
            buf.writeEnum(route);
        }
    }

    private static TutorialPopupS2CPayload read(RegistryFriendlyByteBuf buf) {
        TutorialPopupStage stage = TutorialPopupStage.fromId(buf.readUtf());
        int routeCount = buf.readVarInt();
        List<VehicleClass> routes = new ArrayList<>(routeCount);
        for (int i = 0; i < routeCount; i++) {
            routes.add(buf.readEnum(VehicleClass.class));
        }
        return new TutorialPopupS2CPayload(stage, routes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
