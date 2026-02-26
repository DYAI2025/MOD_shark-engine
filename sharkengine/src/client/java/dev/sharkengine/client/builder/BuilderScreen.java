package dev.sharkengine.client.builder;

import dev.sharkengine.net.BuilderAssembleC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BuilderScreen extends Screen {
    private final PreviewState state;
    private Button assembleButton;

    public BuilderScreen(PreviewState state) {
        super(Component.translatable("screen.sharkengine.builder.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 160;
        int buttonHeight = 20;
        int buttonX = (width - buttonWidth) / 2;
        int buttonY = height - 40;

        assembleButton = Button.builder(Component.translatable("screen.sharkengine.builder.action"), button -> {
            ClientPlayNetworking.send(new BuilderAssembleC2SPayload(state.wheelPos()));
        }).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();

        boolean canLaunch = state.canAssemble() && state.invalidBlocks().isEmpty() && state.contactPoints() == 0 && state.thrusterCount() > 0;
        assembleButton.active = canLaunch;
        if (!canLaunch) {
            assembleButton.setMessage(Component.translatable("screen.sharkengine.builder.action_disabled"));
        }

        addRenderableWidget(assembleButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);

        int centerX = width / 2;
        int y = 30;
        graphics.drawCenteredString(font, title, centerX, y, 0xFFFFFF);
        y += 20;

        graphics.drawCenteredString(font,
                Component.translatable("screen.sharkengine.builder.instructions"),
                centerX,
                y,
                0xAAAAAA);
        y += 20;

        graphics.drawCenteredString(font,
                Component.translatable("screen.sharkengine.builder.blocks", state.validBlocks().size()),
                centerX,
                y,
                0xFFFFFF);
        y += 15;

        graphics.drawCenteredString(font,
                Component.translatable("screen.sharkengine.builder.invalid", state.invalidBlocks().size()),
                centerX,
                y,
                state.invalidBlocks().isEmpty() ? 0x55FF55 : 0xFF5555);
        y += 15;

        graphics.drawCenteredString(font,
                Component.translatable("screen.sharkengine.builder.contacts", state.contactPoints()),
                centerX,
                y,
                state.contactPoints() == 0 ? 0x55FF55 : 0xFFAA00);
        y += 15;

        graphics.drawCenteredString(font,
                Component.translatable("screen.sharkengine.builder.thrusters", state.thrusterCount()),
                centerX,
                y,
                state.thrusterCount() > 0 ? 0x55FF55 : 0xFF5555);
    }

    @Override
    public void onClose() {
        super.onClose();
        BuilderModeClient.clearPreview();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
