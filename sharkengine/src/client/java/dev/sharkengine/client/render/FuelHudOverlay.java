package dev.sharkengine.client.render;

import dev.sharkengine.ship.FuelSystem;
import dev.sharkengine.ship.ShipEntity;
import dev.sharkengine.ship.WeightCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Client-side HUD overlay for ship fuel and status display.
 * Renders fuel bar, height, speed, and weight warnings.
 * 
 * <p>Display layout:</p>
 * <pre>
 * ┌─────────────────────────────────┐
 * │ Treibstoff: [████████░░] 80%   │
 * │ Höhe: Y=127                     │
 * │ Geschwindigkeit: 25 Blöcke/sec │
 * │ Gewicht: 34 Blöcke → 20 max    │
 * │ [Warnung bei HEAVY/OVERLOADED] │
 * └─────────────────────────────────┘
 * </pre>
 * 
 * @author Shark Engine Team
 * @version 1.0 (Luftfahrzeug-MVP)
 */
public final class FuelHudOverlay {
    
    /** HUD position (top-left corner) */
    private static final int HUD_X = 10;
    private static final int HUD_Y = 10;
    
    /** Line height in pixels */
    private static final int LINE_HEIGHT = 12;
    
    /** Text color (white) */
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    
    /** Warning color (yellow) */
    private static final int WARNING_COLOR = 0xFFFFFF00;
    
    /** Error color (red) */
    private static final int ERROR_COLOR = 0xFFFF0000;
    
    private FuelHudOverlay() {
        throw new UnsupportedOperationException("FuelHudOverlay is a utility class");
    }
    
    /**
     * Renders the fuel HUD overlay.
     * Called every frame when player is piloting a ship.
     * 
     * @param graphics GuiGraphics for rendering
     * @param mc Minecraft instance
     */
    public static void render(GuiGraphics graphics, Minecraft mc) {
        if (!(mc.getCameraEntity() instanceof LocalPlayer player)) return;
        if (!(player.getVehicle() instanceof ShipEntity ship)) return;
        
        // Get ship data
        int fuel = ship.getFuelLevel();
        int blocks = ship.getBlockCount();
        float speed = ship.getCurrentSpeed();
        double height = player.getY();
        WeightCategory weight = ship.getWeightCategory();
        
        // Render HUD background (semi-transparent black)
        int hudWidth = 220;
        int hudHeight = 60;
        graphics.fill(HUD_X - 2, HUD_Y - 2, HUD_X + hudWidth + 2, HUD_Y + hudHeight + 2, 0x80000000);
        
        // Render fuel bar
        renderFuelBar(graphics, mc, HUD_X, HUD_Y, fuel, FuelSystem.MAX_FUEL);
        
        // Render stats
        renderStats(graphics, mc, HUD_X, HUD_Y + LINE_HEIGHT, blocks, speed, height);
        
        // Render warning if applicable
        if (weight.getWarning() != null) {
            renderWarning(graphics, mc, HUD_X, HUD_Y + LINE_HEIGHT * 3, weight);
        }
    }
    
    /**
     * Renders the fuel bar with colored segments.
     * 
     * @param g GuiGraphics
     * @param mc Minecraft instance
     * @param x X position
     * @param y Y position
     * @param fuel Current fuel level
     * @param maxFuel Maximum fuel capacity
     */
    private static void renderFuelBar(GuiGraphics g, Minecraft mc, int x, int y, int fuel, int maxFuel) {
        if (maxFuel <= 0) return;
        
        int percent = Math.min(100, Math.max(0, (fuel * 100) / maxFuel));
        int bars = percent / 10;
        int emptyBars = 10 - bars;
        
        // Determine color based on fuel level
        int barColor;
        if (percent > 50) {
            barColor = 0xFF00FF00; // Green for good fuel
        } else if (percent > 20) {
            barColor = 0xFFFFFF00; // Yellow for warning
        } else {
            barColor = 0xFFFF0000; // Red for critical
        }
        
        // Render label
        String label = "Treibstoff: ";
        g.drawString(mc.font, label, x, y, TEXT_COLOR);
        
        // Calculate bar start position
        int barX = x + mc.font.width(label);
        
        // Render filled bars
        g.drawString(mc.font, "█".repeat(bars), barX, y, barColor);
        
        // Render empty bars
        int emptyX = barX + mc.font.width("█".repeat(bars));
        g.drawString(mc.font, "░".repeat(emptyBars), emptyX, y, 0xFF888888);
        
        // Render percentage
        String percentText = " " + percent + "%";
        g.drawString(mc.font, percentText, emptyX + mc.font.width("░".repeat(emptyBars)), y, TEXT_COLOR);
    }
    
    /**
     * Renders ship statistics (height, speed, weight).
     * 
     * @param g GuiGraphics
     * @param mc Minecraft instance
     * @param x X position
     * @param y Y position
     * @param blocks Number of blocks
     * @param speed Current speed
     * @param height Current height (Y position)
     */
    private static void renderStats(GuiGraphics g, Minecraft mc, int x, int y, int blocks, float speed, double height) {
        // Height
        String heightText = String.format("Höhe: Y=%.0f", height);
        g.drawString(mc.font, heightText, x, y, TEXT_COLOR);
        
        // Speed
        String speedText = String.format("Geschwindigkeit: %.1f Blöcke/sec", speed);
        g.drawString(mc.font, speedText, x, y + LINE_HEIGHT, TEXT_COLOR);
        
        // Weight
        WeightCategory category = WeightCategory.fromBlockCount(blocks);
        String weightText = String.format("Gewicht: %d Blöcke → %.0f max", blocks, category.getMaxSpeed());
        g.drawString(mc.font, weightText, x, y + LINE_HEIGHT * 2, TEXT_COLOR);
    }
    
    /**
     * Renders warning message for heavy/overloaded ships.
     * 
     * @param g GuiGraphics
     * @param mc Minecraft instance
     * @param x X position
     * @param y Y position
     * @param category Weight category
     */
    private static void renderWarning(GuiGraphics g, Minecraft mc, int x, int y, WeightCategory category) {
        if (category.getWarning() == null) return;
        
        int color = category == WeightCategory.OVERLOADED ? ERROR_COLOR : WARNING_COLOR;
        g.drawString(mc.font, category.getWarning(), x, y, color);
    }
}
