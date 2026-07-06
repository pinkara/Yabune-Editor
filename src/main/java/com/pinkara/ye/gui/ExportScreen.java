package com.pinkara.ye.gui;

import com.mojang.logging.LogUtils;
import com.pinkara.ye.editor.EntityEditor;
import com.pinkara.ye.editor.MeshExporter;
import com.pinkara.youma.math.AABBInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class ExportScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
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

        this.fileName = new EditBox(this.font, cx - 100, cy - 30, 150, 20, Component.translatable("gui.ye.export.filename"));
        this.fileName.setMaxLength(512);
        this.fileName.setValue("structure");
        this.addRenderableWidget(this.fileName);

        this.addRenderableWidget(Button.builder(Component.literal("Browse"), b -> browse())
                .bounds(cx + 55, cy - 30, 45, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(".obj"), b -> format = 0)
                .bounds(cx - 100, cy, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(".stl"), b -> format = 1)
                .bounds(cx - 30, cy, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(".fbx"), b -> format = 2)
                .bounds(cx + 40, cy, 60, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.export.export"), b -> export())
                .bounds(cx - 100, cy + 35, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ye.export.cancel"), b -> this.onClose())
                .bounds(cx + 5, cy + 35, 95, 20).build());

        this.setInitialFocus(this.fileName);
    }

    private void export() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            this.onClose();
            return;
        }

        EntityEditor editor = findEditorEntity(mc);
        if (editor == null) {
            mc.player.displayClientMessage(Component.literal("No editor entity found."), false);
            this.onClose();
            return;
        }

        int[] s = editor.getPos(EntityEditor.START_POS);
        int[] e = editor.getPos(EntityEditor.END_POS);
        if ((s[0] == 0 && s[1] == 0 && s[2] == 0) || (e[0] == 0 && e[1] == 0 && e[2] == 0)) {
            mc.player.displayClientMessage(Component.literal("No selection set."), false);
            this.onClose();
            return;
        }
        int minX = Math.min(s[0], e[0]);
        int maxX = Math.max(s[0], e[0]) + 1;
        int minY = Math.min(s[1], e[1]);
        int maxY = Math.max(s[1], e[1]) + 1;
        int minZ = Math.min(s[2], e[2]);
        int maxZ = Math.max(s[2], e[2]) + 1;
        AABBInt box = new AABBInt(minX, minY, minZ, maxX, maxY, maxZ);

        String input = this.fileName.getValue();
        if (input == null || input.isBlank()) input = "structure";

        MeshExporter.Format fmt = switch (format) {
            case 1 -> MeshExporter.Format.STL;
            case 2 -> MeshExporter.Format.FBX;
            default -> MeshExporter.Format.OBJ;
        };
        String ext = switch (fmt) {
            case OBJ -> ".obj";
            case STL -> ".stl";
            case FBX -> ".fbx";
        };

        Path path;
        if (input.contains("\\") || input.contains("/")) {
            path = Path.of(input);
            String fileName = path.getFileName().toString();
            if (!fileName.toLowerCase().endsWith(ext)) {
                path = path.resolveSibling(fileName + ext);
            }
        } else {
            String name = input.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            if (!name.toLowerCase().endsWith(ext)) name += ext;
            Path dir = FMLPaths.GAMEDIR.get().resolve("yabune_editor").resolve("exports");
            path = dir.resolve(name);
        }

        LOGGER.info("Exporting mesh client-side to {}", path);
        mc.player.displayClientMessage(Component.literal("Exporting mesh..."), false);
        boolean ok = MeshExporter.export(box, mc.level, mc.getBlockRenderer(), path, fmt);
        mc.player.displayClientMessage(Component.literal(ok ? "Exported mesh to: " + path : "Failed to export mesh."), false);

        this.onClose();
    }

    private void browse() {
        String current = this.fileName.getValue();
        new Thread(() -> {
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                    chooser.setDialogTitle("Save export");
                    if (current != null && !current.isBlank()) {
                        File f = new File(current);
                        if (f.getParentFile() != null && f.getParentFile().exists()) {
                            chooser.setCurrentDirectory(f.getParentFile());
                        }
                        chooser.setSelectedFile(f);
                    }
                    int result = chooser.showSaveDialog(null);
                    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                        File selected = chooser.getSelectedFile();
                        Minecraft.getInstance().execute(() -> fileName.setValue(selected.getAbsolutePath()));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "YE-ExportBrowse").start();
    }

    private static EntityEditor findEditorEntity(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;
        String myName = mc.player.getName().getString();
        EntityEditor found = null;
        double best = 128.0D * 128.0D;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EntityEditor ee) {
                Player editorPlayer = ee.getPlayer();
                String editorName = editorPlayer != null ? editorPlayer.getName().getString() : "";
                if (editorPlayer == mc.player || myName.equals(editorName) || editorPlayer == null) {
                    double dist = entity.distanceToSqr(mc.player);
                    if (dist < best) {
                        best = dist;
                        found = ee;
                    }
                }
            }
        }
        return found;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, this.height / 2 - 60, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.ye.export.filename"), cx - 100, this.height / 2 - 45, 0xFFAAAAAA);
        graphics.drawString(this.font, Component.translatable("gui.ye.export.format"), cx - 100, this.height / 2 - 10, format == 0 ? 0xFF55FF55 : 0xFFAAAAAA);
    }
}
