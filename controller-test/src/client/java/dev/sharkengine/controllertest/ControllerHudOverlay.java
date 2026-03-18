package dev.sharkengine.controllertest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * HUD overlay showing live controller input data.
 * 
 * @author Shark Engine Team
 * @version 1.0
 */
public final class ControllerHudOverlay {

    private static final int HUD_X = 10;
    private static final int HUD_Y = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ACTIVE_COLOR = 0xFF00FF00;
    private static final int INACTIVE_COLOR = 0xFF888888;

    private ControllerHudOverlay() {}

    public static void init() {
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            render(guiGraphics, Minecraft.getInstance());
        });
    }

    private static void render(GuiGraphics g, Minecraft mc) {
        if (!GamepadInputHandler.isConnected()) {
            renderNotConnected(g, mc);
            return;
        }

        renderConnected(g, mc);
    }

    private static void renderNotConnected(GuiGraphics g, Minecraft mc) {
        String text = "🎮 No controller connected";
        g.drawString(mc.font, text, HUD_X, HUD_Y, INACTIVE_COLOR);
    }

    private static void renderConnected(GuiGraphics g, Minecraft mc) {
        // Background
        g.fill(HUD_X - 2, HUD_Y - 2, HUD_X + 200, HUD_Y + 80, 0x80000000);

        // Header
        g.drawString(mc.font, "🎮 Controller Connected", HUD_X, HUD_Y, ACTIVE_COLOR);

        // Left Stick
        float lx = GamepadInputHandler.getLeftStickX();
        float ly = GamepadInputHandler.getLeftStickY();
        g.drawString(mc.font, String.format("Left Stick:  [%.2f, %.2f]", lx, ly), 
                     HUD_X, HUD_Y + LINE_HEIGHT, TEXT_COLOR);

        // Right Stick
        float rx = GamepadInputHandler.getRightStickX();
        float ry = GamepadInputHandler.getRightStickY();
        g.drawString(mc.font, String.format("Right Stick: [%.2f, %.2f]", rx, ry), 
                     HUD_X, HUD_Y + LINE_HEIGHT * 2, TEXT_COLOR);

        // Triggers
        float lt = GamepadInputHandler.getLeftTrigger();
        float rt = GamepadInputHandler.getRightTrigger();
        g.drawString(mc.font, String.format("Triggers:    LT=%.2f RT=%.2f", lt, rt), 
                     HUD_X, HUD_Y + LINE_HEIGHT * 3, TEXT_COLOR);

        // Buttons (first 4: A, B, X, Y)
        String buttons = String.format("Buttons:     A=%s B=%s X=%s Y=%s",
            btn(GamepadInputHandler.isButtonPressed(0)),
            btn(GamepadInputHandler.isButtonPressed(1)),
            btn(GamepadInputHandler.isButtonPressed(2)),
            btn(GamepadInputHandler.isButtonPressed(3))
        );
        g.drawString(mc.font, buttons, HUD_X, HUD_Y + LINE_HEIGHT * 4, TEXT_COLOR);

        // Visual stick representation
        renderStickVisual(g, mc, HUD_X + 100, HUD_Y + LINE_HEIGHT, lx, ly, "L");
        renderStickVisual(g, mc, HUD_X + 150, HUD_Y + LINE_HEIGHT, rx, ry, "R");
    }

    private static void renderStickVisual(GuiGraphics g, Minecraft mc, int x, int y, 
                                          float stickX, float stickY, String label) {
        // Center
        int centerX = x;
        int centerY = y;
        int radius = 15;

        // Background circle
        g.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 0xFF444444);

        // Stick position (scaled)
        int stickPx = centerX + (int)(stickX * radius);
        int stickPy = centerY + (int)(stickY * radius);

        // Stick dot
        g.fill(stickPx - 4, stickPy - 4, stickPx + 4, stickPy + 4, 0xFF00FF00);

        // Label
        g.drawString(mc.font, label, x - 2, y - 2, 0xFFFFFFFF);
    }

    private static String btn(boolean pressed) {
        return pressed ? "■" : "□";
    }
}
