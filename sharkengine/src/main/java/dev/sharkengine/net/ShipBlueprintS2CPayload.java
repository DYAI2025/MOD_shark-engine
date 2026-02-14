package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.ship.ShipBlueprint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShipBlueprintS2CPayload(int shipEntityId, CompoundTag blueprintNbt) implements CustomPacketPayload {
    public static final Type<ShipBlueprintS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "ship_blueprint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipBlueprintS2CPayload> CODEC =
            StreamCodec.of(ShipBlueprintS2CPayload::write, ShipBlueprintS2CPayload::read);

    public ShipBlueprintS2CPayload(int shipEntityId, ShipBlueprint blueprint) {
        this(shipEntityId, blueprint.toNbt());
    }

    private static void write(RegistryFriendlyByteBuf buf, ShipBlueprintS2CPayload payload) {
        buf.writeVarInt(payload.shipEntityId);
        buf.writeNbt(payload.blueprintNbt);
    }

    private static ShipBlueprintS2CPayload read(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CompoundTag tag = buf.readNbt();
        return new ShipBlueprintS2CPayload(entityId, tag);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
