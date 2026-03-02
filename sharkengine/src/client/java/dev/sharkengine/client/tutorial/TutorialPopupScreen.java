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

import java.util.Locale;

public final class TutorialPopupScreen extends Screen {
    private final TutorialPopupStage stage;
    private VehicleClass selectedMode = VehicleClass.AIR;

    public TutorialPopupScreen(TutorialPopupStage stage) {
        super(Component.literal(""));
        this.stage = stage;
    }

    @Override
    protected void init() {
        super.init();
        switch (stage) {
            case WELCOME -> initWelcome();
            case MODE_SELECTION -> initModeSelection();
            case BUILD_GUIDE -> initBuildGuide();
            case READY_TO_LAUNCH -> initReadyPopup();
            case FLIGHT_TIPS -> initFlightTips();
        }
    }

    private void initWelcome() {
        int buttonWidth = 120;
        addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.continue"), button -> onClose())
                .bounds((width - buttonWidth) / 2, height - 40, buttonWidth, 20)
                .build());
    }

    private void initModeSelection() {
        int startY = height / 2 - 30;
        int buttonWidth = 160;

        for (VehicleClass mode : VehicleClass.values()) {
            int y = startY + mode.ordinal() * 25;
            boolean enabled = mode == VehicleClass.AIR;
            Component label = Component.translatable("screen.sharkengine.tutorial.mode." + mode.name().toLowerCase(java.util.Locale.ROOT));
            Button button = Button.builder(label, btn -> {
                if (enabled) {
                    selectedMode = mode;
                }
            }).bounds((width - buttonWidth) / 2, y, buttonWidth, 20).build();
            button.active = enabled;
            addRenderableWidget(button);
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.confirm"), button -> confirmModeSelection())
                .bounds((width - buttonWidth) / 2, height - 40, buttonWidth, 20)
                .build());
    }

    private void initBuildGuide() {
        int buttonWidth = 160;
        addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.continue"), button -> {
            ClientPlayNetworking.send(new TutorialAdvanceC2SPayload(TutorialPopupStage.BUILD_GUIDE));
            onClose();
        }).bounds((width - buttonWidth) / 2, height - 40, buttonWidth, 20).build());
    }

    private void initReadyPopup() {
        int buttonWidth = 160;
        addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.launch"), button -> onClose())
                .bounds((width - buttonWidth) / 2, height - 40, buttonWidth, 20)
                .build());
    }

    private void initFlightTips() {
        int buttonWidth = 160;
        addRenderableWidget(Button.builder(Component.translatable("screen.sharkengine.tutorial.button.continue"), button -> onClose())
                .bounds((width - buttonWidth) / 2, height - 40, buttonWidth, 20)
                .build());
    }

    private void confirmModeSelection() {
        ClientPlayNetworking.send(new TutorialModeSelectionC2SPayload(selectedMode));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, getStageTitle(), width / 2, height / 2 - 80, 0xFFFFFF);
        graphics.drawCenteredString(font, getBody(), width / 2, height / 2 - 60, 0xCCCCCC);
        if (stage == TutorialPopupStage.MODE_SELECTION) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.sharkengine.tutorial.mode.disabled_note"),
                    width / 2,
                    height / 2 + 40,
                    0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, delta);
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
