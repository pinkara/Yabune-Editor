package com.pinkara.ye.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SimpleStringList extends AbstractWidget {
    private final List<String> items = new ArrayList<>();
    private int selected = -1;
    private int scrollOffset = 0;
    private final int itemHeight;
    private final Consumer<String> onSelect;

    public SimpleStringList(int x, int y, int width, int height, int itemHeight, Consumer<String> onSelect) {
        super(x, y, width, height, Component.empty());
        this.itemHeight = itemHeight;
        this.onSelect = onSelect;
    }

    public void update(List<String> items) {
        this.items.clear();
        this.items.addAll(items);
        this.selected = -1;
        this.scrollOffset = 0;
    }

    public String getSelectedItem() {
        return selected >= 0 && selected < items.size() ? items.get(selected) : null;
    }

    public int itemCount() {
        return items.size();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF111111);
        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        for (int i = 0; i < items.size(); i++) {
            int y = getY() + i * itemHeight - scrollOffset;
            if (y + itemHeight < getY() || y > getY() + height) {
                continue;
            }
            boolean hovered = mouseX >= getX() && mouseX <= getX() + width && mouseY >= y && mouseY < y + itemHeight;
            int bg = (i == selected) ? 0xFF5555AA : hovered ? 0xFF666666 : 0xFF111111;
            graphics.fill(getX(), y, getX() + width, y + itemHeight, bg);
            graphics.drawString(Minecraft.getInstance().font, items.get(i), getX() + 4, y + 4, 0xFFFFFFFF);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (this.isMouseOver(event.x(), event.y())) {
            int idx = ((int) event.y() - getY() + scrollOffset) / itemHeight;
            if (idx >= 0 && idx < items.size()) {
                this.selected = idx;
                if (onSelect != null) {
                    onSelect.accept(items.get(idx));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMouseOver(mouseX, mouseY)) {
            int max = Math.max(0, items.size() * itemHeight - height);
            this.scrollOffset = Mth.clamp(this.scrollOffset - (int) (scrollY * itemHeight), 0, max);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.literal("Structure list, " + items.size() + " items"));
    }
}
