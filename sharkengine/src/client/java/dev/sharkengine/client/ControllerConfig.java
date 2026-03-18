package dev.sharkengine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration for controller/gamepad settings.
 * Stored in config/sharkengine-controller.properties
 *
 * <p>Configurable options:</p>
 * <ul>
 *   <li>deadzone - Stick deadzone threshold (0.0-0.5)</li>
 *   <li>invertYaw - Invert yaw/turn axis</li>
 *   <li>invertPitch - Invert pitch axis (for future use)</li>
 *   <li>vibrationEnabled - Enable controller vibration feedback</li>
 * </ul>
 *
 * @author Shark Engine Team
 * @version 1.0
 */
public final class ControllerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("SharkEngine-ControllerConfig");
    private static final String CONFIG_FILE = "config/sharkengine-controller.properties";

    private static ControllerConfig instance;

    /** Stick deadzone threshold (default: 0.15) */
    private float deadzone = 0.15f;

    /** Invert yaw/turn axis (default: false) */
    private boolean invertYaw = false;

    /** Invert pitch axis (default: false) */
    private boolean invertPitch = false;

    /** Enable controller vibration (default: true) */
    private boolean vibrationEnabled = true;

    /** Vibration intensity (0.0-1.0, default: 0.5) */
    private float vibrationIntensity = 0.5f;

    private ControllerConfig() {}

    /**
     * Gets the singleton instance, loading config if necessary.
     */
    public static ControllerConfig getInstance() {
        if (instance == null) {
            instance = new ControllerConfig();
            instance.load();
        }
        return instance;
    }

    /**
     * Loads configuration from file.
     * Creates default config if file doesn't exist.
     */
    public void load() {
        File configFile = new File(CONFIG_FILE);
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                LOGGER.info("Loaded controller config from {}", CONFIG_FILE);
            } catch (IOException e) {
                LOGGER.error("Failed to load controller config", e);
            }
        } else {
            // Create default config
            configFile.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                props.setProperty("deadzone", String.valueOf(deadzone));
                props.setProperty("invertYaw", String.valueOf(invertYaw));
                props.setProperty("invertPitch", String.valueOf(invertPitch));
                props.setProperty("vibrationEnabled", String.valueOf(vibrationEnabled));
                props.setProperty("vibrationIntensity", String.valueOf(vibrationIntensity));
                props.store(fos, "Shark Engine Controller Configuration");
                LOGGER.info("Created default controller config at {}", CONFIG_FILE);
            } catch (IOException e) {
                LOGGER.error("Failed to create default controller config", e);
            }
        }

        // Read values
        try {
            deadzone = Float.parseFloat(props.getProperty("deadzone", "0.15"));
            deadzone = Math.max(0.0f, Math.min(0.5f, deadzone));

            invertYaw = Boolean.parseBoolean(props.getProperty("invertYaw", "false"));
            invertPitch = Boolean.parseBoolean(props.getProperty("invertPitch", "false"));
            vibrationEnabled = Boolean.parseBoolean(props.getProperty("vibrationEnabled", "true"));
            vibrationIntensity = Float.parseFloat(props.getProperty("vibrationIntensity", "0.5"));
            vibrationIntensity = Math.max(0.0f, Math.min(1.0f, vibrationIntensity));

            LOGGER.info("Controller config: deadzone={}, invertYaw={}, vibration={}",
                    deadzone, invertYaw, vibrationEnabled);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid config value, using defaults", e);
        }
    }

    /**
     * Saves current configuration to file.
     */
    public void save() {
        File configFile = new File(CONFIG_FILE);
        Properties props = new Properties();

        props.setProperty("deadzone", String.valueOf(deadzone));
        props.setProperty("invertYaw", String.valueOf(invertYaw));
        props.setProperty("invertPitch", String.valueOf(invertPitch));
        props.setProperty("vibrationEnabled", String.valueOf(vibrationEnabled));
        props.setProperty("vibrationIntensity", String.valueOf(vibrationIntensity));

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Shark Engine Controller Configuration");
            LOGGER.info("Saved controller config to {}", CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.error("Failed to save controller config", e);
        }
    }

    /**
     * @return Stick deadzone threshold (0.0-0.5)
     */
    public float getDeadzone() {
        return deadzone;
    }

    /**
     * Sets deadzone and saves config.
     * @param value Deadzone value (0.0-0.5)
     */
    public void setDeadzone(float value) {
        this.deadzone = Math.max(0.0f, Math.min(0.5f, value));
        save();
    }

    /**
     * @return true if yaw axis is inverted
     */
    public boolean isInvertYaw() {
        return invertYaw;
    }

    /**
     * Sets yaw inversion and saves config.
     */
    public void setInvertYaw(boolean value) {
        this.invertYaw = value;
        save();
    }

    /**
     * @return true if pitch axis is inverted
     */
    public boolean isInvertPitch() {
        return invertPitch;
    }

    /**
     * Sets pitch inversion and saves config.
     */
    public void setInvertPitch(boolean value) {
        this.invertPitch = value;
        save();
    }

    /**
     * @return true if controller vibration is enabled
     */
    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    /**
     * Sets vibration enabled and saves config.
     */
    public void setVibrationEnabled(boolean value) {
        this.vibrationEnabled = value;
        save();
    }

    /**
     * @return Vibration intensity (0.0-1.0)
     */
    public float getVibrationIntensity() {
        return vibrationIntensity;
    }

    /**
     * Sets vibration intensity and saves config.
     */
    public void setVibrationIntensity(float value) {
        this.vibrationIntensity = Math.max(0.0f, Math.min(1.0f, value));
        save();
    }
}
