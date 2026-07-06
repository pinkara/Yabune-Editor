package com.pinkara.ye.gui;

import com.pinkara.ye.editor.ChangeEntry;
import com.pinkara.ye.network.PacketChangeBlocks;
import com.pinkara.ye.network.YENetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChangeScreen extends NonBlockingScreen {
    private final List<ChangeEntry> entries = new ArrayList<>();
    private final List<ChangeRow> rows = new ArrayList<>();

    private static final int SLOT_SIZE = 20;
    private static final int ROW_HEIGHT = 26;

    public ChangeScreen() {
        super(Component.literal("Change Blocks"));
        entries.add(new ChangeEntry());
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();

        int startY = 40;
        int cx = this.width / 2;

        for (int i = 0; i < entries.size(); ++i) {
            final int index = i;
            int y = startY + i * ROW_HEIGHT;
            ChangeRow row = new ChangeRow();
            row.fromButton = Button.builder(Component.literal("?"), b -> openPicker(index, true))
                    .pos(cx - 70, y).size(SLOT_SIZE, SLOT_SIZE).build();
            row.toButton = Button.builder(Component.literal("?"), b -> openPicker(index, false))
                    .pos(cx + 10, y).size(SLOT_SIZE, SLOT_SIZE).build();
            row.removeButton = Button.builder(Component.literal("X"), b -> removeRow(index))
                    .pos(cx + 50, y).size(20, 20).build();
            addRenderableWidget(row.fromButton);
            addRenderableWidget(row.toButton);
            addRenderableWidget(row.removeButton);
            rows.add(row);
        }

        int bottom = startY + entries.size() * ROW_HEIGHT + 10;
        addRenderableWidget(Button.builder(Component.literal("+ Add"), b -> addRow())
                .pos(cx - 80, bottom).size(70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> apply())
                .pos(cx + 10, bottom).size(70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .pos(cx - 35, bottom + 28).size(70, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Transparent overlay instead of pausing background
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, "From", this.width / 2 - 70, 28, 0xFFFF0000);
        guiGraphics.drawString(this.font, "To", this.width / 2 + 10, 28, 0xFF00FF00);

        int cx = this.width / 2;
        for (int i = 0; i < entries.size(); ++i) {
            ChangeEntry entry = entries.get(i);
            int y = 40 + i * ROW_HEIGHT;
            renderItemSlot(guiGraphics, entry.fromItem, cx - 70, y);
            renderItemSlot(guiGraphics, entry.toItem, cx + 10, y);
        }

        guiGraphics.drawWordWrap(this.font, Component.literal("Click a slot to choose a block from the picker."), this.width / 2 - 100, this.height - 60, 200, 0xFFAAAAAA);
    }

    private void renderItemSlot(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF373737);
        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, x + 2, y + 2);
        }
    }

    private void openPicker(int index, boolean from) {
        Minecraft.getInstance().setScreen(new BlockPickerScreen(this, stack -> setSlot(index, from, stack)));
    }

    private void setSlot(int index, boolean from, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack single = stack.copy();
        single.setCount(1);

        ChangeEntry entry = entries.get(index);
        if (from) {
            entry.fromItem = single;
        } else {
            entry.toItem = single;
        }
    }

    private void addRow() {
        entries.add(new ChangeEntry());
        this.rebuildWidgets();
    }

    private void removeRow(int index) {
        if (entries.size() > 1) {
            entries.remove(index);
            this.rebuildWidgets();
        } else {
            entries.get(0).fromItem = ItemStack.EMPTY;
            entries.get(0).toItem = ItemStack.EMPTY;
        }
    }

    private void apply() {
        List<ChangeEntry> valid = new ArrayList<>();
        for (ChangeEntry entry : entries) {
            if (entry.isValid()) {
                valid.add(entry);
            }
        }
        if (!valid.isEmpty()) {
            YENetwork.sendToServer(new PacketChangeBlocks(valid));
        }
        this.onClose();
    }

    private static class ChangeRow {
        Button fromButton;
        Button toButton;
        Button removeButton;
    }
}
