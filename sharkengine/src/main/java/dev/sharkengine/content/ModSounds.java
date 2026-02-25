package dev.sharkengine.content;

import dev.sharkengine.SharkEngineMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Registry class for custom sound events in Shark Engine.
 * 
 * <p>Registered sounds:</p>
 * <ul>
 *   <li>{@link #THRUSTER_IDLE} - Quiet thruster hum when hovering</li>
 *   <li>{@link #THRUSTER_ACTIVE} - Loud thruster noise during acceleration</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>{@code
 * // In mod initializer
 * ModSounds.init();
 * 
 * // Play sound
 * level.playSound(null, x, y, z, ModSounds.THRUSTER_ACTIVE.value(), ...);
 * }</pre>
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public final class ModSounds {
    
    /**
     * Thruster idle sound - quiet hum when hovering in place
     * Resource location: {@code sharkengine:entity.ship.thruster_idle}
     */
    public static final Holder<SoundEvent> THRUSTER_IDLE = 
        registerSound("entity.ship.thruster_idle");
    
    /**
     * Thruster active sound - loud noise during acceleration
     * Resource location: {@code sharkengine:entity.ship.thruster_active}
     */
    public static final Holder<SoundEvent> THRUSTER_ACTIVE = 
        registerSound("entity.ship.thruster_active");
    
    /**
     * Private constructor to prevent instantiation.
     * Use {@link #init()} for initialization.
     */
    private ModSounds() {
        throw new UnsupportedOperationException("ModSounds is a utility class");
    }
    
    /**
     * Registers a sound event in the built-in registry.
     * 
     * @param name Sound name (will be prefixed with {@code sharkengine:})
     * @return Holder reference to the registered SoundEvent
     */
    private static Holder<SoundEvent> registerSound(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        return BuiltInRegistries.SOUND_EVENT.register(id, () -> event);
    }
    
    /**
     * Initializes the sound registry.
     * Call this during mod initialization.
     */
    public static void init() {
        // Sounds are registered via static field initialization
        SharkEngineMod.LOGGER.debug("ModSounds initialized: {} sounds registered", 2);
    }
}
