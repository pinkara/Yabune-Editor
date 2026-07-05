package com.pinkara.ye.gui;

import com.pinkara.ye.network.YENetwork;
import com.pinkara.ye.network.PacketLoadStructure;
import com.pinkara.ye.network.PacketRequestStructureList;
import com.pinkara.ye.network.PacketSaveStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class StructureBrowserScreen extends Screen {
    public enum Mode {
        LOAD,
        SAVE
    }

    private StructureList list;
    private EditBox nameField;
    private Button saveButton;
    private Button loadButton;
    private static List<String> serverNames = new ArrayList<>();
    private int refreshCooldown = 0;

    private final Screen parent;
    private final Mode mode;

    public StructureBrowserScreen(Screen parent, Mode mode) {
        super(Component.translatable(mode == Mode.SAVE
                ? "gui.ye.structure.save_title" : "gui.ye.structure.load_title"));
        this.parent = parent;
        this.mode = mode;
    }

    @Override
    protected void init() {
        super.init();
        YENetwork.sendToServer(new PacketRequestStructureList());

        int cx = this.width / 2;
        int top = 40;

        this.nameField = new EditBox(this.font, cx - 150, top, 200, 20, Component.translatable("gui.ye.structure.name"));
        this.nameField.setMaxLength(128);
        this.nameField.setValue("my_structure");
        this.nameField.setVisible(mode == Mode.SAVE);
        this.nameField.setEditable(mode == Mode.SAVE);
        this.addRenderableWidget(this.nameField);

        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.structure.save"), b -> save())
                .bounds(cx + 60, top, 90, 20).build());
        this.saveButton.visible = mode == Mode.SAVE;

        this.list = new StructureList(this.minecraft, 300, this.height - top - 70, top + 30, 18);
        this.addWidget(this.list);
        this.list.update(serverNames);

        this.loadButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.structure.load"), b -> load())
                .bounds(cx - 100, this.height - 60, 90, 20).build());
        this.loadButton.visible = mode == Mode.LOAD;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.structure.close"), b -> closeToParent())
                .bounds(cx + 10, this.height - 60, 90, 20).build());

        this.setInitialFocus(this.nameField);
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
        if (mode != Mode.SAVE) return;
        YENetwork.sendToServer(new PacketSaveStructure(this.nameField.getValue()));
        YENetwork.sendToServer(new PacketRequestStructureList());
        refreshCooldown = 10; // request again after ~0.5s in case the first list packet beat the file write
    }

    private void load() {
        if (mode != Mode.LOAD) return;
        StructureList.Entry e = this.list.getSelected();
        if (e == null) return;
        YENetwork.sendToServer(new PacketLoadStructure(e.name));
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
        if (Minecraft.getInstance().screen instanceof StructureBrowserScreen screen) {
            if (screen.list != null) {
                screen.list.update(serverNames);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.list.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    private class StructureList extends ObjectSelectionList<StructureList.Entry> {
        public StructureList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
            this.setX(StructureBrowserScreen.this.width / 2 - width / 2);
        }

        public void update(List<String> names) {
            this.clearEntries();
            for (String name : names) {
                this.addEntry(new Entry(name));
            }
        }

        @Override
        public int getRowWidth() { return this.width - 20; }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            final String name;
            Entry(String name) { this.name = name; }

            @Override
            public Component getNarration() { return Component.literal(name); }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                graphics.drawString(StructureBrowserScreen.this.font, name, left + 5, top + 4, hovered ? 0xFFFF55 : 0xFFFFFF);
            }
        }
    }
}
