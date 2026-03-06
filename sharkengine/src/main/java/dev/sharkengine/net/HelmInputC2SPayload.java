package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-Server payload for ship helm/steering input.
 * Sent when the pilot changes their input (throttle, turn, forward).
 * 
 * <p>Fields:</p>
 * <ul>
 *   <li>throttle: Vertical input (-1..+1) - Leertaste/Shift</li>
 *   <li>turn: Rotation input (-1..+1) - A/D-Tasten</li>
 *   <li>forward: Forward acceleration (0..1) - W-Taste</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public record HelmInputC2SPayload(
    float throttle,   // -1..+1 (vertical: Leertaste/Shift)
    float turn,       // -1..+1 (rotation: A/D-Tasten)
    float forward     // 0..1 (acceleration: W-Taste)
) implements CustomPacketPayload {
    
    /**
     * Packet type identifier
     */
    public static final Type<HelmInputC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "helm_input"));

    /**
     * Stream codec for encoding/decoding the payload
     * Now includes all 3 fields: throttle, turn, forward
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, HelmInputC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, HelmInputC2SPayload::throttle,
                    ByteBufCodecs.FLOAT, HelmInputC2SPayload::turn,
                    ByteBufCodecs.FLOAT, HelmInputC2SPayload::forward,  // NEW: forward field
                    HelmInputC2SPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
