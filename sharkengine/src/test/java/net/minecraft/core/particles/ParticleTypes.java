package net.minecraft.core.particles;

/**
 * Minimal stub for unit tests; replaces the actual particle registry during JVM-only tests.
 */
public final class ParticleTypes {
    public static final ParticleOptions CAMPFIRE_COSY_SMOKE = new SimpleParticle("campfire");
    public static final ParticleOptions FLAME = new SimpleParticle("flame");

    private ParticleTypes() {}

    private record SimpleParticle(String id) implements ParticleOptions {
        @Override
        public String toString() {
            return id;
        }
    }
}
