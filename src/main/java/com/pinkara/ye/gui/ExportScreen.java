package com.pinkara.ye.gui;

import com.pinkara.ye.network.PacketExportMesh;
import com.pinkara.ye.network.YENetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ExportScreen extends Screen {
    private byte format = 0; // 0=obj, 1=stl
    private EditBox fileName;

    public ExportScreen() {
        super(Component.translatable("gui.ye.export.title"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.fileName = new EditBox(this.font, cx - 100, cy - 30, 200, 20, Component.translatable("gui.ye.export.filename"));
        this.fileName.setMaxLength(128);
        this.fileName.setValue("structure");
        this.addRenderableWidget(this.fileName);

        this.addRenderableWidget(Button.builder(Component.literal(".obj"), b -> format = 0)
                .bounds(cx - 100, cy, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(".stl"), b -> format = 1)
                .bounds(cx + 5, cy, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.export.export"), b -> export())
                .bounds(cx - 100, cy + 35, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.export.cancel"), b -> this.onClose())
                .bounds(cx + 5, cy + 35, 95, 20).build());

        this.setInitialFocus(this.fileName);
    }

    private void export() {
        YENetwork.sendToServer(new PacketExportMesh(this.fileName.getValue(), format));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, this.height / 2 - 60, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.ye.export.filename"), cx - 100, this.height / 2 - 45, 0xAAAAAA);
        graphics.drawString(this.font, Component.translatable("gui.ye.export.format"), cx - 100, this.height / 2 - 10, format == 0 ? 0x55FF55 : 0xAAAAAA);
    }
}
