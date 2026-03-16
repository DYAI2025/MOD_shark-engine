package dev.sharkengine.client.render;

import com.mojang.blaze3d.platform.InputConstants;
import dev.sharkengine.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Version 4 onboarding HUD card.
 * Shows first-launch controls and flight basics when piloting a ship.
 */
public final class FirstLaunchOnboardingHud {
    private static final String CONFIG_FILE = "sharkengine_client.properties";
    private static final String FIRST_LAUNCH_KEY = "onboarding.dismissed.v4";

    private static final int CARD_WIDTH = 250;
    private static final int CARD_PADDING = 10;
    private static final int LINE_HEIGHT = 10;

    private static boolean initialized;
    private static boolean dismissed;
    private static boolean lastDismissKeyDown;

    private FirstLaunchOnboardingHud() {
        throw new UnsupportedOperationException("FirstLaunchOnboardingHud is a utility class");
    }

    public static void render(GuiGraphics graphics, Minecraft mc) {
        initializeIfNeeded(mc);
        if (dismissed) return;
        if (!(mc.getCameraEntity() instanceof LocalPlayer player)) return;
        if (!(player.getVehicle() instanceof ShipEntity)) return;

        boolean dismissKeyDown = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_X);
        if (dismissKeyDown && !lastDismissKeyDown) {
            dismissed = true;
            saveDismissedState(mc);
            return;
        }
        lastDismissKeyDown = dismissKeyDown;

        int cardX = Math.max(8, mc.getWindow().getGuiScaledWidth() - CARD_WIDTH - 10);
        int cardY = 10;
        int maxTextWidth = CARD_WIDTH - CARD_PADDING * 2;

        List<Component> content = List.of(
                Component.translatable("hud.sharkengine.onboarding.title"),
                Component.translatable("hud.sharkengine.onboarding.movement"),
                Component.translatable("hud.sharkengine.onboarding.vertical"),
                Component.translatable("hud.sharkengine.onboarding.fuel"),
                Component.translatable("hud.sharkengine.onboarding.dismiss")
        );

        int lineCount = 0;
        for (Component component : content) {
            lineCount += mc.font.split(component, maxTextWidth).size();
        }

        int cardHeight = CARD_PADDING * 2 + lineCount * LINE_HEIGHT + 4;
        graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + cardHeight, 0xCC101820);
        graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + 1, 0xFF3FA9F5);

        int y = cardY + CARD_PADDING;
        for (int i = 0; i < content.size(); i++) {
            int color = (i == 0) ? 0xFFFFFFFF : (i == content.size() - 1 ? 0xFF9ED0FF : 0xFFE8EDF2);
            for (FormattedCharSequence line : mc.font.split(content.get(i), maxTextWidth)) {
                graphics.drawString(mc.font, line, cardX + CARD_PADDING, y, color);
                y += LINE_HEIGHT;
            }
            if (i == 0) {
                y += 2;
            }
        }
    }

    private static void initializeIfNeeded(Minecraft mc) {
        if (initialized) return;
        dismissed = loadDismissedState(mc);
        initialized = true;
    }

    private static boolean loadDismissedState(Minecraft mc) {
        Path path = mc.gameDirectory.toPath().resolve(CONFIG_FILE);
        if (!Files.exists(path)) {
            return false;
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            return Boolean.parseBoolean(properties.getProperty(FIRST_LAUNCH_KEY, "false"));
        } catch (IOException e) {
            return false;
        }
    }

    private static void saveDismissedState(Minecraft mc) {
        Path path = mc.gameDirectory.toPath().resolve(CONFIG_FILE);
        Properties properties = new Properties();

        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException ignored) {
            }
        }

        properties.setProperty(FIRST_LAUNCH_KEY, "true");

        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "Shark Engine client settings");
        } catch (IOException ignored) {
        }
    }
}
