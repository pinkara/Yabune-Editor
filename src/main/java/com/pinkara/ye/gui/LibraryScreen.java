package com.pinkara.ye.gui;

import com.pinkara.ye.network.YENetwork;
import com.pinkara.ye.network.PacketLoadStructure;
import com.pinkara.ye.network.PacketRequestStructureList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class LibraryScreen extends Screen {
    private final Screen parent;
    private static List<String> serverNames = new ArrayList<>();

    private final List<String> names = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int listX, listY, listW, listH;
    private static final int ITEM_H = 14;

    public LibraryScreen(Screen parent) {
        super(Component.literal("Library"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        YENetwork.sendToServer(new PacketRequestStructureList());

        int cx = this.width / 2;
        int top = 36;
        this.listX = cx - 150;
        this.listW = 300;
        this.listY = top;
        this.listH = Math.max(40, this.height - top - 55);

        update(serverNames);

        this.addRenderableWidget(Button.builder(Component.literal("Load"), b -> load())
                .bounds(cx - 100, this.height - 28, 90, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> closeToParent())
                .bounds(cx + 10, this.height - 28, 90, 18).build());
    }

    private void update(List<String> incoming) {
        this.names.clear();
        this.names.addAll(incoming);
        this.selectedIndex = -1;
        this.scrollOffset = 0;
    }

    private void load() {
        if (selectedIndex < 0 || selectedIndex >= names.size()) return;
        YENetwork.sendToServer(new PacketLoadStructure(names.get(selectedIndex)));
        this.onClose();
    }

    private void closeToParent() {
        if (this.minecraft != null && parent != null) {
            this.minecraft.setScreen(parent);
        } else {
            this.onClose();
        }
    }

    public static void setServerList(List<String> names) {
        serverNames = new ArrayList<>(names);
        if (Minecraft.getInstance().screen instanceof LibraryScreen screen) {
            screen.update(serverNames);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "Library entries: " + names.size(), this.width / 2, 22, 0xFFAAAAAA);

        graphics.fill(listX, listY, listX + listW, listY + listH, 0xFF111111);
        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < names.size(); i++) {
            int y = listY + i * ITEM_H - scrollOffset;
            if (y + ITEM_H < listY || y > listY + listH) continue;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY < y + ITEM_H;
            int bg = (i == selectedIndex) ? 0xFF5555AA : hovered ? 0xFF666666 : 0xFF111111;
            graphics.fill(listX, y, listX + listW, y + ITEM_H, bg);
            String name = names.get(i);
            int textX = listX + 4;
            if (name.startsWith("favorite_")) {
                graphics.drawString(this.font, "★", textX, y + 3, 0xFFFFFF55, true);
                textX += 10;
            }
            graphics.drawString(this.font, name, textX, y + 3, 0xFFFFFFFF, true);
        }
        graphics.disableScissor();

        if (names.isEmpty()) {
            graphics.drawCenteredString(this.font, "Library is empty. Save structures first.", this.width / 2, listY + listH / 2, 0xFFFF5555);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int idx = ((int) mouseY - listY + scrollOffset) / ITEM_H;
            if (idx >= 0 && idx < names.size()) {
                selectedIndex = idx;
            }
            return true;
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int max = Math.max(0, names.size() * ITEM_H - listH);
            scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * ITEM_H), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
