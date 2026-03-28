package dev.sharkengine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Gamepad / Controller input handler using LWJGL/GLFW.
 *
 * <p>Mapping (Xbox-style layout):</p>
 * <ul>
 *   <li>Left Stick Y-axis → Forward/backward</li>
 *   <li>Left Stick X-axis → Strafe left/right</li>
 *   <li>Right Trigger (axis 5) → Climb up</li>
 *   <li>Left Trigger (axis 4) → Descend</li>
 *   <li>A-Button (0) → Toggle anchor</li>
 *   <li>B-Button (1) → Dismount</li>
 *   <li>Y-Button (3) → Interact (mount/builder)</li>
 * </ul>
 *
 * <p>Configuration is loaded from config/sharkengine-controller.properties</p>
 *
 * @author Shark Engine Team
 * @version 2.0 (Config-Support + Chat-Nachrichten)
 */
public final class ControllerInput {
    private static final Logger LOGGER = LoggerFactory.getLogger("SharkEngine-Controller");

    /** Connected joystick ID (GLFW_JOYSTICK_1 .. _15), or -1 if none */
    private static int connectedJoystick = -1;

    /** Whether a gamepad was ever detected (for log spam prevention) */
    private static boolean wasConnected = false;

    // Current input state (hovercraft 3-axis model)
    private static float forward = 0f;
    private static float strafe = 0f;
    private static float vertical = 0f;
    private static boolean anchorPressed = false;
    private static boolean dismountPressed = false;
    private static boolean interactPressed = false;

    // Button edge detection (prevent repeat-fire)
    private static boolean lastAnchorButton = false;
    private static boolean lastDismountButton = false;
    private static boolean lastInteractButton = false;

    // Vibration state
    private static long lastVibrationTime = 0;

    private ControllerInput() {}

    /**
     * @return Config instance for controller settings
     */
    private static ControllerConfig getConfig() {
        return ControllerConfig.getInstance();
    }

    /**
     * Initializes controller detection and sends chat message.
     * Call once during client initialization.
     */
    public static void init() {
        // Check for controllers immediately on init
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            scanForControllers(mc);
        }
    }

    /**
     * Scans for connected controllers and sends chat notification.
     * @param mc Minecraft instance for chat messages
     */
    private static void scanForControllers(Minecraft mc) {
        for (int i = GLFW.GLFW_JOYSTICK_1; i <= GLFW.GLFW_JOYSTICK_LAST; i++) {
            if (GLFW.glfwJoystickPresent(i)) {
                connectedJoystick = i;
                String name = GLFW.glfwGetJoystickName(i);
                if (!wasConnected) {
                    LOGGER.info("🎮 Controller connected: {} (slot {})", name, i);
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.translatable("message.sharkengine.controller_connected"));
                    }
                    wasConnected = true;
                }
                return;
            }
        }
    }

    /**
     * Scans for connected controllers. Call once per tick.
     */
    public static void pollController() {
        Minecraft mc = Minecraft.getInstance();
        
        // Find connected joystick
        if (connectedJoystick < 0) {
            for (int i = GLFW.GLFW_JOYSTICK_1; i <= GLFW.GLFW_JOYSTICK_LAST; i++) {
                if (GLFW.glfwJoystickPresent(i)) {
                    connectedJoystick = i;
                    String name = GLFW.glfwGetJoystickName(i);
                    if (!wasConnected) {
                        LOGGER.info("🎮 Controller connected: {} (slot {})", name, i);
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.translatable("message.sharkengine.controller_connected"));
                        }
                        wasConnected = true;
                    }
                    break;
                }
            }
        }

        if (connectedJoystick < 0) {
            resetInputs();
            return;
        }

        // Check still connected
        if (!GLFW.glfwJoystickPresent(connectedJoystick)) {
            LOGGER.info("Controller disconnected (slot {})", connectedJoystick);
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.translatable("message.sharkengine.controller_disconnected"));
            }
            connectedJoystick = -1;
            wasConnected = false;
            resetInputs();
            return;
        }

        // Read axes
        FloatBuffer axes = GLFW.glfwGetJoystickAxes(connectedJoystick);
        if (axes == null || axes.limit() < 4) {
            resetInputs();
            return;
        }

        // Standard gamepad mapping:
        // Axis 0: Left Stick X
        // Axis 1: Left Stick Y (inverted: -1 = push up)
        // Axis 2: Right Stick X
        // Axis 3: Right Stick Y
        // Axis 4: Left Trigger (-1 = released, +1 = fully pressed)
        // Axis 5: Right Trigger (-1 = released, +1 = fully pressed)

        float leftStickY = axes.get(1);    // Forward/back
        float leftStickX = axes.get(0);    // Strafe

        // Forward/backward: left stick Y (negative Y = forward)
        forward = applyDeadzone(-leftStickY);

        // Strafe: left stick X (positive X = right)
        strafe = applyDeadzone(leftStickX);

        // Vertical: triggers
        float vertUp = 0f;
        float vertDown = 0f;
        if (axes.limit() > 5) {
            // Triggers range from -1 (released) to +1 (pressed)
            vertUp = (axes.get(5) + 1f) / 2f;   // Right Trigger → climb
            vertDown = (axes.get(4) + 1f) / 2f;  // Left Trigger → descend
        }
        vertical = vertUp - vertDown;
        vertical = Math.max(-1f, Math.min(1f, vertical));

        // Read buttons
        ByteBuffer buttons = GLFW.glfwGetJoystickButtons(connectedJoystick);
        if (buttons != null && buttons.limit() > 3) {
            boolean aButton = buttons.get(0) == GLFW.GLFW_PRESS;
            boolean bButton = buttons.get(1) == GLFW.GLFW_PRESS;
            boolean yButton = buttons.get(3) == GLFW.GLFW_PRESS;

            // Edge detection: fire only on press, not hold
            anchorPressed = aButton && !lastAnchorButton;
            dismountPressed = bButton && !lastDismountButton;
            interactPressed = yButton && !lastInteractButton;

            lastAnchorButton = aButton;
            lastDismountButton = bButton;
            lastInteractButton = yButton;
        }
    }

    /** @return true if any controller is connected */
    public static boolean isConnected() {
        return connectedJoystick >= 0;
    }

    /** @return Forward/backward input -1..+1 (left stick Y) */
    public static float getForward() { return forward; }

    /** @return Strafe input -1..+1 (left stick X, positive = right) */
    public static float getStrafe() { return strafe; }

    /** @return Vertical input -1..+1 (triggers: RT=up, LT=down) */
    public static float getVertical() { return vertical; }

    /** @return true on A-button press edge (toggle anchor) */
    public static boolean isAnchorPressed() { return anchorPressed; }

    /** @return true on B-button press edge (dismount) */
    public static boolean isDismountPressed() { return dismountPressed; }

    /** @return true on Y-button press edge (interact) */
    public static boolean isInteractPressed() { return interactPressed; }

    /**
     * Applies deadzone and rescales output.
     * Uses configurable deadzone from ControllerConfig.
     * @param value Input value (-1..+1)
     * @return Processed value with deadzone applied
     */
    private static float applyDeadzone(float value) {
        float deadzone = getConfig().getDeadzone();
        if (Math.abs(value) < deadzone) return 0f;
        // Rescale so that output starts at 0 right outside deadzone
        float sign = Math.signum(value);
        return sign * (Math.abs(value) - deadzone) / (1f - deadzone);
    }

    private static void resetInputs() {
        forward = 0f;
        strafe = 0f;
        vertical = 0f;
        anchorPressed = false;
        dismountPressed = false;
        interactPressed = false;
    }
}
