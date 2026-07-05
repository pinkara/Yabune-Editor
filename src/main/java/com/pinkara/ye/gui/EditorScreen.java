package com.pinkara.ye.gui;

import com.pinkara.ye.editor.EntityEditor;
import com.pinkara.ye.network.YENetwork;
import com.pinkara.ye.network.PacketSaveStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class EditorScreen extends NonBlockingScreen {
    private final EntityEditor editor;

    private final EditBox[] startFields = new EditBox[3];
    private final EditBox[] endFields = new EditBox[3];

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 170;
    private static final int BUTTON_W = 86;
    private static final int BUTTON_H = 18;

    private int panelX, panelY;

    public EditorScreen(EntityEditor editor) {
        super(Component.translatable("gui.ye.editor"));
        this.editor = editor;
    }

    @Override
    protected void init() {
        super.init();

        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = 24;

        int cx = this.width / 2;
        int top = this.panelY + 8;

        // --- Start / End coordinates ---
        int startX = cx - 190;
        int endX = cx + 60;
        addCoordRow("Start", startX, top, startFields, editor.getPos(EntityEditor.START_POS), 0);
        addCoordRow("End", endX, top, endFields, editor.getPos(EntityEditor.END_POS), 1);

        // --- Main action buttons ---
        int ay = top + 70;
        int ax = cx - 190;
        addActionButton(ax, ay, "Change", b -> openChangeScreen());
        addActionButton(ax + 96, ay, "Load", b -> openStructureBrowser(StructureBrowserScreen.Mode.LOAD));
        addActionButton(ax + 192, ay, "Save", b -> openStructureBrowser(StructureBrowserScreen.Mode.SAVE));
        ay += 24;
        addActionButton(ax, ay, "Export", b -> openExportScreen());
        addActionButton(ax + 96, ay, "Favorite", b -> saveFavorite());

        // --- Close ---
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .pos(cx - 30, this.panelY + PANEL_H - 24).size(60, 20).build());
    }

    private void addCoordRow(String label, int x, int y, EditBox[] fields, int[] pos, int group) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {}).pos(x, y).size(36, 16).build());
        for (int i = 0; i < 3; ++i) {
            fields[i] = new EditBox(this.font, x + 38, y + 14 + i * 20, 44, 16, Component.literal(label + i));
            fields[i].setValue(String.valueOf(pos[i]));
            fields[i].setFilter(s -> s.matches("-?\\d*"));
            addRenderableWidget(fields[i]);
            addNudgeButtons(x + 84, y + 14 + i * 20, group, i);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xCC000000);
        guiGraphics.hLine(panelX, panelX + PANEL_W - 1, panelY, 0xFF555555);
        guiGraphics.hLine(panelX, panelX + PANEL_W - 1, panelY + PANEL_H - 1, 0xFF555555);
        guiGraphics.vLine(panelX, panelY, panelY + PANEL_H - 1, 0xFF555555);
        guiGraphics.vLine(panelX + PANEL_W - 1, panelY, panelY + PANEL_H - 1, 0xFF555555);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 4, 0xFFFFFF);

        int[] s = editor.getPos(EntityEditor.START_POS);
        int[] e = editor.getPos(EntityEditor.END_POS);
        int sizeX = Math.abs(s[0] - e[0]) + 1;
        int sizeY = Math.abs(s[1] - e[1]) + 1;
        int sizeZ = Math.abs(s[2] - e[2]) + 1;
        String info = String.format("Size: %d x %d x %d", sizeX, sizeY, sizeZ);
        guiGraphics.drawCenteredString(this.font, info, this.width / 2, panelY + PANEL_H - 38, 0x00FF00);
    }

    private void addNudgeButtons(int x, int y, int group, int index) {
        addRenderableWidget(Button.builder(Component.literal("<"), b -> nudge(group, index, -1))
                .pos(x, y).size(14, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> nudge(group, index, 1))
                .pos(x + 58, y).size(14, 16).build());
    }

    private void nudge(int group, int index, int delta) {
        EditBox box = switch (group) {
            case 0 -> startFields[index];
            case 1 -> endFields[index];
            default -> null;
        };
        if (box == null) return;
        try {
            int val = Integer.parseInt(box.getValue().isEmpty() ? "0" : box.getValue());
            box.setValue(String.valueOf(val + delta));
        } catch (NumberFormatException ignored) {
        }
    }

    private void addActionButton(int x, int y, String text, Button.OnPress onPress) {
        addRenderableWidget(Button.builder(Component.literal(text), onPress)
                .pos(x, y).size(BUTTON_W, BUTTON_H).build());
    }

    private void openChangeScreen() {
        Minecraft.getInstance().setScreen(new ChangeScreen());
    }

    private void openStructureBrowser(StructureBrowserScreen.Mode mode) {
        Minecraft.getInstance().setScreen(new StructureBrowserScreen(this, mode));
    }

    private void openExportScreen() {
        Minecraft.getInstance().setScreen(new ExportScreen());
    }

    private void saveFavorite() {
        String name = "favorite_" + java.time.LocalDateTime.now().toString().replaceAll("[^a-zA-Z0-9]", "_");
        YENetwork.sendToServer(new PacketSaveStructure(name));
    }
}
