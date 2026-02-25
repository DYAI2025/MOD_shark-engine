package dev.sharkengine.ship;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Enum representing the 5 acceleration phases for air vehicles.
 * Each phase has distinct speed, duration, and visual effects.
 * 
 * <p>Acceleration works as follows:</p>
 * <ul>
 *   <li>Phase 1 (0-2s): 5 blocks/sec - Smoke particles (20% intensity)</li>
 *   <li>Phase 2 (2-4s): 15 blocks/sec - Smoke particles (40% intensity)</li>
 *   <li>Phase 3 (4-5s): 20 blocks/sec - Flame particles (60% intensity)</li>
 *   <li>Phase 4 (5-6s): 25 blocks/sec - Flame particles (80% intensity)</li>
 *   <li>Phase 5 (6s+): 30 blocks/sec - Flame particles (100% intensity)</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public enum AccelerationPhase {
    /**
     * Phase 1: Initial acceleration (0-2 seconds)
     * Speed: 5 blocks/sec, Particle Intensity: 20%
     */
    PHASE_1(0, 40, 5.0f, 0.2f, ParticleTypes.CAMPFIRE_COSY_SMOKE),
    
    /**
     * Phase 2: Building speed (2-4 seconds)
     * Speed: 15 blocks/sec, Particle Intensity: 40%
     */
    PHASE_2(40, 80, 15.0f, 0.4f, ParticleTypes.CAMPFIRE_COSY_SMOKE),
    
    /**
     * Phase 3: Moderate speed (4-5 seconds)
     * Speed: 20 blocks/sec, Particle Intensity: 60%
     * Switches to flame particles
     */
    PHASE_3(80, 100, 20.0f, 0.6f, ParticleTypes.FLAME),
    
    /**
     * Phase 4: High speed (5-6 seconds)
     * Speed: 25 blocks/sec, Particle Intensity: 80%
     */
    PHASE_4(100, 120, 25.0f, 0.8f, ParticleTypes.FLAME),
    
    /**
     * Phase 5: Maximum speed (6+ seconds)
     * Speed: 30 blocks/sec, Particle Intensity: 100%
     */
    PHASE_5(120, -1, 30.0f, 1.0f, ParticleTypes.FLAME);
    
    /**
     * Starting tick for this phase (20 ticks = 1 second)
     */
    private final int startTick;
    
    /**
     * Ending tick for this phase (-1 for infinite/last phase)
     */
    private final int endTick;
    
    /**
     * Maximum speed for this phase in blocks/sec
     */
    private final float speed;
    
    /**
     * Particle intensity multiplier (0.0 - 1.0)
     */
    private final float particleIntensity;
    
    /**
     * Type of particle to spawn
     */
    private final ParticleOptions particleType;
    
    /**
     * Constructor for AccelerationPhase
     * 
     * @param startTick Starting tick (20 ticks = 1 second)
     * @param endTick Ending tick (-1 for last phase)
     * @param speed Maximum speed in blocks/sec
     * @param particleIntensity Particle intensity (0.0 - 1.0)
     * @param particleType Type of particle to spawn
     */
    AccelerationPhase(int startTick, int endTick, float speed, float particleIntensity, ParticleOptions particleType) {
        this.startTick = startTick;
        this.endTick = endTick;
        this.speed = speed;
        this.particleIntensity = particleIntensity;
        this.particleType = particleType;
    }
    
    /**
     * Returns the starting tick for this phase
     * 
     * @return Starting tick (20 ticks = 1 second)
     */
    public int getStartTick() {
        return startTick;
    }
    
    /**
     * Returns the ending tick for this phase
     * 
     * @return Ending tick, or -1 if this is the last phase
     */
    public int getEndTick() {
        return endTick;
    }
    
    /**
     * Returns the maximum speed for this phase
     * 
     * @return Speed in blocks/sec
     */
    public float getSpeed() {
        return speed;
    }
    
    /**
     * Returns the particle intensity multiplier
     * 
     * @return Intensity from 0.0 to 1.0
     */
    public float getParticleIntensity() {
        return particleIntensity;
    }
    
    /**
     * Returns the particle type for this phase
     * 
     * @return ParticleOptions for rendering
     */
    public ParticleOptions getParticleType() {
        return particleType;
    }
    
    /**
     * Determines the acceleration phase from the current tick count
     * 
     * @param ticks Elapsed ticks since acceleration started
     * @return The appropriate AccelerationPhase
     */
    public static AccelerationPhase fromTick(int ticks) {
        if (ticks < PHASE_2.startTick) return PHASE_1;
        if (ticks < PHASE_3.startTick) return PHASE_2;
        if (ticks < PHASE_4.startTick) return PHASE_3;
        if (ticks < PHASE_5.startTick) return PHASE_4;
        return PHASE_5;
    }
}
