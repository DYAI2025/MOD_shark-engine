package dev.sharkengine.client.tutorial;

import dev.sharkengine.net.TutorialAdvanceC2SPayload;
import dev.sharkengine.net.TutorialModeSelectionC2SPayload;
import dev.sharkengine.ship.VehicleClass;
import dev.sharkengine.tutorial.TutorialPopupStage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

public final class TutorialPopupScreen extends Screen {
    private static final int PANEL_WIDTH_MAX = 380;
    private static final int PANEL_PADDING = 12;
    private static final int BODY_LINE_HEIGHT = 11;

    private final TutorialPopupStage stage;
    private VehicleClass selectedMode = VehicleClass.AIR;

    public TutorialPopupScreen(TutorialPopupStage stage) {
        super(Component.literal(""));
        this.stage = stage;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = stage == TutorialPopupStage.WELCOME ? 120 : 160;
        int buttonY = Math.min(height - 34, computePanelBottom() + 8);

        switch (stage) {
            case WELCOME -> addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.continue"), button -> onClose())
                    .bounds((width - buttonWidth) / 2, buttonY, buttonWidth, 20)
                    .build());
            case MODE_SELECTION -> {
                initModeSelection();
                addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.confirm"), button -> confirmModeSelection())
                        .bounds((width - buttonWidth) / 2, buttonY, buttonWidth, 20)
                        .build());
            }
            case BUILD_GUIDE -> addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.continue"), button -> {
                ClientPlayNetworking.send(new TutorialAdvanceC2SPayload(TutorialPopupStage.BUILD_GUIDE));
                onClose();
            }).bounds((width - buttonWidth) / 2, buttonY, buttonWidth, 20).build());
            case READY_TO_LAUNCH -> addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.launch"), button -> onClose())
                    .bounds((width - buttonWidth) / 2, buttonY, buttonWidth, 20)
                    .build());
            case FLIGHT_TIPS -> addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.continue"), button -> onClose())
                    .bounds((width - buttonWidth) / 2, buttonY, buttonWidth, 20)
                    .build());
        }
    }

    private void initModeSelection() {
        int startY = computePanelTop() + 55;
        int buttonWidth = 160;

        for (VehicleClass mode : VehicleClass.values()) {
            int y = startY + mode.ordinal() * 25;
            boolean enabled = mode == VehicleClass.AIR;
            Component label = Component.translatable("screen.sharkengine.tutorial.mode." + mode.name().toLowerCase(Locale.ROOT));
            Button button = Button.builder(label, btn -> {
                if (enabled) {
                    selectedMode = mode;
                }
            }).bounds((width - buttonWidth) / 2, y, buttonWidth, 20).build();
            button.active = enabled;
            addRenderableWidget(button);
        }
    }

    private void confirmModeSelection() {
        ClientPlayNetworking.send(new TutorialModeSelectionC2SPayload(selectedMode));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);

        int panelW = Math.min(width - 40, PANEL_WIDTH_MAX);
        int panelX = (width - panelW) / 2;
        int panelTop = computePanelTop();
        int panelBottom = computePanelBottom();

        graphics.fill(panelX, panelTop, panelX + panelW, panelBottom, 0xCC000000);

        int currentY = panelTop + PANEL_PADDING;
        graphics.drawCenteredString(font, getStageTitle(), width / 2, currentY, 0xFFFFFF);
        currentY += 20;

        List<FormattedCharSequence> bodyLines = font.split(getBody(), panelW - PANEL_PADDING * 2);
        for (FormattedCharSequence line : bodyLines) {
            graphics.drawCenteredString(font, line, width / 2, currentY, 0xDDDDDD);
            currentY += BODY_LINE_HEIGHT;
        }

        if (stage == TutorialPopupStage.MODE_SELECTION) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.sharkengine.tutorial.mode.disabled_note"),
                    width / 2,
                    computePanelBottom() - 26,
                    0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private int computePanelTop() {
        int panelHeight = computePanelHeight();
        return Math.max(20, (height - panelHeight) / 2 - 10);
    }

    private int computePanelBottom() {
        return computePanelTop() + computePanelHeight();
    }

    private int computePanelHeight() {
        int panelW = Math.min(width - 40, PANEL_WIDTH_MAX);
        int bodyHeight = font.split(getBody(), panelW - PANEL_PADDING * 2).size() * BODY_LINE_HEIGHT;
        int baseHeight = PANEL_PADDING * 2 + 20 + bodyHeight;
        if (stage == TutorialPopupStage.MODE_SELECTION) {
            baseHeight = Math.max(baseHeight, 180);
        }
        return baseHeight;
    }

    private Component getStageTitle() {
        return Component.translatable("screen.sharkengine.tutorial." + stage.id() + ".title");
    }

    private Component getBody() {
        return Component.translatable("screen.sharkengine.tutorial." + stage.id() + ".body");
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }
}
