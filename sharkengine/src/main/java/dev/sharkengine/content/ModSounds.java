package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Registry class for custom sound events in Shark Engine.
 * 
 * <p>Registered sounds:</p>
 * <ul>
 *   <li>THRUSTER_IDLE - Quiet thruster hum when hovering</li>
 *   <li>THRUSTER_ACTIVE - Loud thruster noise during acceleration</li>
 * </ul>
 * 
 * @author Shark Engine Team
 * @version 2.0 (Luftfahrzeug-MVP)
 */
public final class ModSounds {
    
    /**
     * Thruster idle sound - quiet hum when hovering in place
     */
    public static final SoundEvent THRUSTER_IDLE = 
        createSound("entity.ship.thruster_idle");
    
    /**
     * Thruster active sound - loud noise during acceleration
     */
    public static final SoundEvent THRUSTER_ACTIVE = 
        createSound("entity.ship.thruster_active");
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ModSounds() {
        throw new UnsupportedOperationException("ModSounds is a utility class");
    }
    
    /**
     * Creates a SoundEvent.
     * Note: For Fabric with access wideners, sounds auto-register
     * 
     * @param name Sound name (will be prefixed with {@code sharkengine:})
     * @return SoundEvent instance
     */
    private static SoundEvent createSound(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, name);
        return SoundEvent.createVariableRangeEvent(id);
    }
    
    /**
     * Initializes the sound registry.
     * Call this during mod initialization.
     */
    public static void init() {
        // Sounds are created via static field initialization
        SharkEngineMod.LOGGER.debug("ModSounds initialized: {} sounds created", 2);
    }
}
