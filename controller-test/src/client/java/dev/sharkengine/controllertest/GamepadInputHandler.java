package dev.sharkengine.controllertest;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Gamepad input handler using GLFW.
 * Detects controllers and reads all input values.
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public final class GamepadInputHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ControllerTest");
    
    /** Connected joystick ID, or -1 if none */
    private static int connectedJoystick = -1;
    
    /** Whether we've already shown the connected message */
    private static boolean wasConnected = false;
    
    // Current input state
    private static float leftStickX = 0f;
    private static float leftStickY = 0f;
    private static float rightStickX = 0f;
    private static float rightStickY = 0f;
    private static float leftTrigger = 0f;
    private static float rightTrigger = 0f;
    private static boolean[] buttons = new boolean[16];
    
    // Deadzone config
    private static float deadzone = 0.15f;
    
    // Last update time for rate limiting
    private static long lastUpdateTime = 0;
    private static long lastConnectTime = 0;

    private GamepadInputHandler() {}

    /**
     * Initialize the handler. Called once at client startup.
     */
    public static void init() {
        LOGGER.info("GamepadInputHandler initialized");
        LOGGER.info("Scanning for controllers...");
        
        // Initial scan
        scanForControllers();
    }

    /**
     * Scan for connected controllers. Call periodically.
     */
    public static void poll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Rate limit polling to 10 Hz
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < 100) return;
        lastUpdateTime = currentTime;
        
        // Find connected joystick
        if (connectedJoystick < 0) {
            scanForControllers();
            return;
        }
        
        // Check if still connected
        if (!GLFW.glfwJoystickPresent(connectedJoystick)) {
            LOGGER.info("❌ Controller disconnected (slot {})", connectedJoystick);
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("❌ Controller disconnected"));
            }
            connectedJoystick = -1;
            wasConnected = false;
            resetInputs();
            return;
        }
        
        // Read inputs
        readInputs(mc);
    }
    
    /**
     * Scan for any connected controller.
     */
    private static void scanForControllers() {
        Minecraft mc = Minecraft.getInstance();
        
        // Rate limit scan to once per second
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConnectTime < 1000) return;
        lastConnectTime = currentTime;
        
        for (int i = GLFW.GLFW_JOYSTICK_1; i <= GLFW.GLFW_JOYSTICK_LAST; i++) {
            if (GLFW.glfwJoystickPresent(i)) {
                connectedJoystick = i;
                String name = GLFW.glfwGetJoystickName(i);
                
                if (!wasConnected) {
                    LOGGER.info("✅ Controller connected: {} (slot {})", name, i);
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("✅ Controller connected: " + name));
                    }
                    wasConnected = true;
                }
                return;
            }
        }
    }
    
    /**
     * Read all input values from the controller.
     */
    private static void readInputs(Minecraft mc) {
        if (connectedJoystick < 0) return;
        
        // Read axes
        FloatBuffer axes = GLFW.glfwGetJoystickAxes(connectedJoystick);
        if (axes != null && axes.limit() >= 6) {
            leftStickX = applyDeadzone(axes.get(0));
            leftStickY = applyDeadzone(-axes.get(1)); // Invert Y
            rightStickX = applyDeadzone(axes.get(2));
            rightStickY = applyDeadzone(-axes.get(3));
            
            // Triggers (range -1 to 1, convert to 0 to 1)
            leftTrigger = Math.max(0, (axes.get(4) + 1) / 2);
            rightTrigger = Math.max(0, (axes.get(5) + 1) / 2);
        }
        
        // Read buttons
        ByteBuffer buttons = GLFW.glfwGetJoystickButtons(connectedJoystick);
        if (buttons != null) {
            for (int i = 0; i < Math.min(16, buttons.limit()); i++) {
                GamepadInputHandler.buttons[i] = buttons.get(i) == GLFW.GLFW_PRESS;
            }
        }
        
        // Log input every 500ms
        if (System.currentTimeMillis() % 500 < 100) {
            logInputs(mc);
        }
    }
    
    /**
     * Log current input state.
     */
    private static void logInputs(Minecraft mc) {
        // Only log if there's actual input
        boolean hasInput = Math.abs(leftStickX) > 0.01 || Math.abs(leftStickY) > 0.01
                || Math.abs(rightStickX) > 0.01 || Math.abs(rightStickY) > 0.01
                || leftTrigger > 0.01 || rightTrigger > 0.01
                || anyButtonPressed();
        
        if (hasInput) {
            String msg = String.format(
                "🎮 L:[%.2f,%.2f] R:[%.2f,%.2f] LT:%.2f RT:%.2f",
                leftStickX, leftStickY, rightStickX, rightStickY,
                leftTrigger, rightTrigger
            );
            LOGGER.info(msg);
            
            // Show in action bar (less intrusive than chat)
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(msg), true);
            }
        }
    }
    
    /**
     * Check if any button is pressed.
     */
    private static boolean anyButtonPressed() {
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i]) return true;
        }
        return false;
    }
    
    /**
     * Apply deadzone to axis value.
     */
    private static float applyDeadzone(float value) {
        if (Math.abs(value) < deadzone) return 0f;
        float sign = Math.signum(value);
        return sign * (Math.abs(value) - deadzone) / (1f - deadzone);
    }
    
    /**
     * Reset all inputs to zero.
     */
    private static void resetInputs() {
        leftStickX = 0f;
        leftStickY = 0f;
        rightStickX = 0f;
        rightStickY = 0f;
        leftTrigger = 0f;
        rightTrigger = 0f;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = false;
        }
    }
    
    // Getters for HUD
    public static float getLeftStickX() { return leftStickX; }
    public static float getLeftStickY() { return leftStickY; }
    public static float getRightStickX() { return rightStickX; }
    public static float getRightStickY() { return rightStickY; }
    public static float getLeftTrigger() { return leftTrigger; }
    public static float getRightTrigger() { return rightTrigger; }
    public static boolean isButtonPressed(int index) { 
        return index >= 0 && index < buttons.length && buttons[index]; 
    }
    public static boolean isConnected() { return connectedJoystick >= 0; }
}
