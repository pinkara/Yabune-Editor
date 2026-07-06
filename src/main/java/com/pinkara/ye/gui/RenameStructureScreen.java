package com.pinkara.ye.gui;

import com.pinkara.ye.editor.StructureManager;
import com.pinkara.ye.network.PacketRenameStructure;
import com.pinkara.ye.network.YENetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RenameStructureScreen extends Screen {
    private final Screen parent;
    private final String oldName;
    private EditBox nameField;

    public RenameStructureScreen(Screen parent, String oldName) {
        super(Component.literal("Rename Structure"));
        this.parent = parent;
        this.oldName = oldName;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int top = this.height / 2 - 30;

        this.nameField = new EditBox(this.font, cx - 100, top, 200, 18, Component.literal("New name"));
        this.nameField.setMaxLength(128);
        this.nameField.setValue(oldName);
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);

        this.addRenderableWidget(Button.builder(Component.literal("Rename"), b -> rename())
                .bounds(cx - 102, top + 28, 100, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> close())
                .bounds(cx + 2, top + 28, 100, 18).build());
    }

    private void rename() {
        String newName = this.nameField.getValue();
        if (newName != null && !newName.isBlank()) {
            YENetwork.sendToServer(new PacketRenameStructure(oldName, newName));
        }
        close();
    }

    private void close() {
        if (this.minecraft != null && parent != null) {
            this.minecraft.setScreen(parent);
        } else {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 55, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, oldName, this.width / 2, this.height / 2 - 42, 0xFFAAAAAA);
    }
}
