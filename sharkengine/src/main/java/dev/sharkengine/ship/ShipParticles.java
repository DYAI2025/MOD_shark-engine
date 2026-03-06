package dev.sharkengine.ship;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Resolves particle identifiers into actual ParticleOptions.
 */
final class ShipParticles {
    private ShipParticles() {}

    static ParticleOptions resolve(String key) {
        if (key == null) {
            return ParticleTypes.CAMPFIRE_COSY_SMOKE;
        }
        return switch (key) {
            case "flame" -> ParticleTypes.FLAME;
            case "campfire" -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
            default -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
        };
    }
}
