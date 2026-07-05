package com.pinkara.ye.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BlockPickerScreen extends Screen {
    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 9;
    private static final int PADDING = 8;
    private static final int TOP_OFFSET = 50;
    private static final int BOTTOM_OFFSET = 40;

    private final Consumer<ItemStack> onSelect;
    private final Screen parent;

    private EditBox searchBox;
    private Button cancelButton;
    private List<ItemStack> allStacks = new ArrayList<>();
    private List<ItemStack> filteredStacks = new ArrayList<>();
    private int scrollOffset = 0;
    private int rowsVisible = 0;
    private int gridX = 0;
    private int gridY = 0;

    public BlockPickerScreen(Screen parent, Consumer<ItemStack> onSelect) {
        super(Component.translatable("gui.ye.block_picker"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();

        this.allStacks.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == null) continue;
            if (block.asItem() instanceof BlockItem blockItem && blockItem != null) {
                ItemStack stack = new ItemStack(blockItem);
                if (!stack.isEmpty()) {
                    this.allStacks.add(stack);
                }
            }
        }
        this.allStacks.sort((a, b) -> a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString()));
        this.filteredStacks = new ArrayList<>(this.allStacks);

        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 22, 200, 18, Component.literal("Search"));
        this.searchBox.setResponder(this::updateFilter);
        this.searchBox.setFocused(true);
        this.addRenderableWidget(this.searchBox);

        this.cancelButton = Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .pos(this.width / 2 - 50, this.height - 30).size(100, 20).build();
        this.addRenderableWidget(this.cancelButton);

        this.gridX = (this.width - COLUMNS * SLOT_SIZE - (COLUMNS - 1) * 2) / 2;
        this.gridY = TOP_OFFSET;
        this.rowsVisible = Math.max(1, (this.height - TOP_OFFSET - BOTTOM_OFFSET) / (SLOT_SIZE + 2));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        int rowsTotal = (this.filteredStacks.size() + COLUMNS - 1) / COLUMNS;
        int maxScroll = Math.max(0, rowsTotal - rowsVisible);
        if (this.scrollOffset > maxScroll) this.scrollOffset = maxScroll;
        if (this.scrollOffset < 0) this.scrollOffset = 0;

        for (int row = 0; row < rowsVisible; ++row) {
            for (int col = 0; col < COLUMNS; ++col) {
                int index = (row + this.scrollOffset) * COLUMNS + col;
                if (index >= this.filteredStacks.size()) break;

                int x = this.gridX + col * (SLOT_SIZE + 2);
                int y = this.gridY + row * (SLOT_SIZE + 2);
                renderSlot(guiGraphics, x, y, mouseX, mouseY, this.filteredStacks.get(index));
            }
        }

        // Tooltip
        ItemStack hovered = getHoveredStack(mouseX, mouseY);
        if (!hovered.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    private void renderSlot(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, ItemStack stack) {
        boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
        int bgColor = hovered ? 0xFFAAAAAA : 0xFF8B8B8B;
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF373737);
        guiGraphics.renderItem(stack, x + 1, y + 1);
    }

    private ItemStack getHoveredStack(int mouseX, int mouseY) {
        for (int row = 0; row < rowsVisible; ++row) {
            for (int col = 0; col < COLUMNS; ++col) {
                int index = (row + this.scrollOffset) * COLUMNS + col;
                if (index >= this.filteredStacks.size()) continue;
                int x = this.gridX + col * (SLOT_SIZE + 2);
                int y = this.gridY + row * (SLOT_SIZE + 2);
                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    return this.filteredStacks.get(index);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        ItemStack hovered = getHoveredStack((int) mouseX, (int) mouseY);
        if (!hovered.isEmpty()) {
            this.onSelect.accept(hovered.copy());
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        int rowsTotal = (this.filteredStacks.size() + COLUMNS - 1) / COLUMNS;
        int maxScroll = Math.max(0, rowsTotal - rowsVisible);
        this.scrollOffset = (int) Math.max(0, Math.min(maxScroll, this.scrollOffset - scrollY));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void updateFilter(String filter) {
        String lower = filter.toLowerCase();
        this.filteredStacks.clear();
        for (ItemStack stack : this.allStacks) {
            if (lower.isEmpty() || stack.getHoverName().getString().toLowerCase().contains(lower)) {
                this.filteredStacks.add(stack);
            }
        }
        this.scrollOffset = 0;
    }
}
