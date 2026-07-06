package com.pinkara.ye.gui;

import com.pinkara.ye.network.YENetwork;
import com.pinkara.ye.network.PacketLoadStructure;
import com.pinkara.ye.network.PacketRequestStructureList;
import com.pinkara.ye.network.PacketSaveStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.pinkara.ye.network.PacketDeleteStructure;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class StructureBrowserScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    public enum Mode {
        LOAD,
        SAVE
    }

    private EditBox nameField;
    private Button saveButton;
    private Button loadButton;
    private static List<String> serverNames = new ArrayList<>();
    private int refreshCooldown = 0;

    private final List<String> names = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int listX, listY, listW, listH;
    private static final int ITEM_H = 14;
    private static final int ICON_W = 12;

    private final Screen parent;
    private final Mode mode;

    public StructureBrowserScreen(Screen parent, Mode mode) {
        super(Component.translatable(mode == Mode.SAVE
                ? "gui.ye.structure.save_title" : "gui.ye.structure.load_title"));
        this.parent = parent;
        this.mode = mode;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("StructureBrowserScreen init mode=" + mode + " size=" + this.width + "x" + this.height);
        YENetwork.sendToServer(new PacketRequestStructureList());

        int cx = this.width / 2;
        int top = 36;
        int fieldW = 200;
        int fieldX = cx - fieldW - 10;
        int btnW = 90;
        int btnX = cx + 10;

        this.nameField = new EditBox(this.font, fieldX, top, fieldW, 18, Component.translatable("gui.ye.structure.name"));
        this.nameField.setMaxLength(128);
        this.nameField.setValue("my_structure");
        this.nameField.setVisible(mode == Mode.SAVE);
        this.nameField.setEditable(mode == Mode.SAVE);
        this.addRenderableWidget(this.nameField);

        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.structure.save"), b -> save())
                .bounds(btnX, top, btnW, 18).build());
        this.saveButton.visible = mode == Mode.SAVE;

        this.addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> refresh())
                .bounds(btnX, top + 22, btnW, 18).build());

        this.listX = cx - 150;
        this.listW = 300;
        this.listY = top + 50;
        this.listH = Math.max(40, this.height - this.listY - 55);

        update(serverNames);

        int bottomY = this.height - 28;
        if (mode == Mode.LOAD) {
            this.loadButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.structure.load"), b -> load())
                    .bounds(cx - 100, bottomY, 90, 18).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.structure.close"), b -> closeToParent())
                    .bounds(cx + 10, bottomY, 90, 18).build());
        } else {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.structure.close"), b -> closeToParent())
                    .bounds(cx - 45, bottomY, 90, 18).build());
        }

        this.setInitialFocus(this.nameField);
    }

    private void update(List<String> incoming) {
        this.names.clear();
        this.names.addAll(incoming);
        this.selectedIndex = -1;
        this.scrollOffset = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (refreshCooldown > 0) {
            if (--refreshCooldown == 0) {
                YENetwork.sendToServer(new PacketRequestStructureList());
            }
        }
    }

    private void save() {
        LOGGER.info("StructureBrowserScreen save clicked");
        if (mode != Mode.SAVE) return;
        String name = this.nameField.getValue();
        if (name == null || name.isBlank()) return;
        YENetwork.sendToServer(new PacketSaveStructure(name));
        refresh();
    }

    private void refresh() {
        LOGGER.info("StructureBrowserScreen refresh");
        YENetwork.sendToServer(new PacketRequestStructureList());
        refreshCooldown = 10;
    }

    private void load() {
        LOGGER.info("StructureBrowserScreen load clicked");
        if (mode != Mode.LOAD) return;
        if (selectedIndex < 0 || selectedIndex >= names.size()) return;
        YENetwork.sendToServer(new PacketLoadStructure(names.get(selectedIndex)));
        this.onClose();
    }

    private void closeToParent() {
        LOGGER.info("StructureBrowserScreen close clicked");
        if (this.minecraft != null && parent != null) {
            this.minecraft.setScreen(parent);
        } else {
            this.onClose();
        }
    }

    public static void setServerList(List<String> names) {
        LOGGER.info("StructureBrowserScreen setServerList: " + names);
        serverNames = new ArrayList<>(names);
        if (Minecraft.getInstance().screen instanceof StructureBrowserScreen screen) {
            screen.update(serverNames);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "Saved structures: " + names.size(), this.width / 2, 22, 0xFFAAAAAA);

        // List background
        graphics.fill(listX, listY, listX + listW, listY + listH, 0xFF111111);
        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < names.size(); i++) {
            int y = listY + i * ITEM_H - scrollOffset;
            if (y + ITEM_H < listY || y > listY + listH) continue;
            boolean rowHovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY < y + ITEM_H;
            boolean renameHovered = rowHovered && mouseX >= listX && mouseX < listX + ICON_W;
            boolean deleteHovered = rowHovered && mouseX >= listX + ICON_W && mouseX < listX + ICON_W * 2;
            int bg = (i == selectedIndex) ? 0xFF5555AA : rowHovered ? 0xFF666666 : 0xFF111111;
            graphics.fill(listX, y, listX + listW, y + ITEM_H, bg);
            // Rename icon background
            graphics.fill(listX + 1, y + 1, listX + ICON_W - 1, y + ITEM_H - 1, renameHovered ? 0xFF4488AA : 0xFF333333);
            graphics.drawString(this.font, "✎", listX + 3, y + 3, 0xFFAAFFFF, true);
            // Delete icon background
            graphics.fill(listX + ICON_W + 1, y + 1, listX + ICON_W * 2 - 1, y + ITEM_H - 1, deleteHovered ? 0xFFAA4444 : 0xFF333333);
            graphics.drawString(this.font, "✕", listX + ICON_W + 3, y + 3, 0xFFFFAAAA, true);
            // Structure name
            graphics.drawString(this.font, names.get(i), listX + ICON_W * 2 + 4, y + 3, 0xFFFFFFFF, true);
        }
        graphics.disableScissor();

        if (names.isEmpty()) {
            graphics.drawCenteredString(this.font, "No saved structures. Copy a selection, then Save.", this.width / 2, listY + listH / 2, 0xFFFF5555);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int idx = ((int) mouseY - listY + scrollOffset) / ITEM_H;
            if (idx >= 0 && idx < names.size()) {
                if (mouseX >= listX && mouseX < listX + ICON_W) {
                    // Rename icon clicked
                    this.minecraft.setScreen(new RenameStructureScreen(this, names.get(idx)));
                    return true;
                }
                if (mouseX >= listX + ICON_W && mouseX < listX + ICON_W * 2) {
                    // Delete icon clicked
                    YENetwork.sendToServer(new PacketDeleteStructure(names.get(idx)));
                    return true;
                }
                selectedIndex = idx;
                if (mode == Mode.LOAD) {
                    this.nameField.setValue(names.get(idx));
                }
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
