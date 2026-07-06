package com.pinkara.ye.gui;

import com.pinkara.ye.editor.EntityEditor;
import com.pinkara.ye.network.YENetwork;
import com.pinkara.ye.network.PacketSaveStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EditorScreen extends NonBlockingScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final EntityEditor editor;

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
        int top = this.panelY + 20;

        // --- Main action buttons ---
        int ay = top;
        int ax = cx - 190;
        addActionButton(ax, ay, "Change", b -> openChangeScreen());
        addActionButton(ax + 96, ay, "Load", b -> openStructureBrowser(StructureBrowserScreen.Mode.LOAD));
        addActionButton(ax + 192, ay, "Save", b -> openStructureBrowser(StructureBrowserScreen.Mode.SAVE));
        ay += 24;
        addActionButton(ax, ay, "Export FBX", b -> openExportScreen());
        addActionButton(ax + 96, ay, "Favorite", b -> saveFavorite());
        addActionButton(ax + 192, ay, "Library", b -> openLibraryScreen());

        // --- Close ---
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .pos(cx - 30, this.panelY + PANEL_H - 24).size(60, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xCC000000);
        guiGraphics.hLine(panelX, panelX + PANEL_W - 1, panelY, 0xFF555555);
        guiGraphics.hLine(panelX, panelX + PANEL_W - 1, panelY + PANEL_H - 1, 0xFF555555);
        guiGraphics.vLine(panelX, panelY, panelY + PANEL_H - 1, 0xFF555555);
        guiGraphics.vLine(panelX + PANEL_W - 1, panelY, panelY + PANEL_H - 1, 0xFF555555);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 4, 0xFFFFFFFF);

        int[] s = editor.getPos(EntityEditor.START_POS);
        int[] e = editor.getPos(EntityEditor.END_POS);
        int sizeX = Math.abs(s[0] - e[0]) + 1;
        int sizeY = Math.abs(s[1] - e[1]) + 1;
        int sizeZ = Math.abs(s[2] - e[2]) + 1;
        String info = String.format("Size: %d x %d x %d", sizeX, sizeY, sizeZ);
        guiGraphics.drawCenteredString(this.font, info, this.width / 2, panelY + PANEL_H - 38, 0xFF00FF00);
    }

    private void addActionButton(int x, int y, String text, Button.OnPress onPress) {
        addRenderableWidget(Button.builder(Component.literal(text), onPress)
                .pos(x, y).size(BUTTON_W, BUTTON_H).build());
    }

    private void openChangeScreen() {
        Minecraft.getInstance().setScreen(new ChangeScreen());
    }

    private void openStructureBrowser(StructureBrowserScreen.Mode mode) {
        LOGGER.info("EditorScreen: opening StructureBrowserScreen mode=" + mode);
        Minecraft.getInstance().setScreen(new StructureBrowserScreen(this, mode));
    }

    private void openExportScreen() {
        LOGGER.info("EditorScreen: opening StructureExportScreen");
        Minecraft.getInstance().setScreen(new StructureExportScreen(this));
    }

    private void openLibraryScreen() {
        LOGGER.info("EditorScreen: opening LibraryScreen");
        Minecraft.getInstance().setScreen(new LibraryScreen(this));
    }

    private void saveFavorite() {
        LOGGER.info("EditorScreen: saveFavorite clicked");
        String name = "favorite_" + java.time.LocalDateTime.now().toString().replaceAll("[^a-zA-Z0-9]", "_");
        YENetwork.sendToServer(new PacketSaveStructure(name));
    }
}
