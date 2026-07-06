package com.pinkara.ye.client;

import com.mojang.logging.LogUtils;
import com.pinkara.ye.editor.EntityEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.slf4j.Logger;

public class EditorHud {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int logCooldown = 0;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            logRare("HUD: mc.player is null");
            return;
        }
        if (mc.level == null) {
            logRare("HUD: mc.level is null");
            return;
        }
        if (mc.screen != null && mc.screen.isPauseScreen()) {
            logRare("HUD: pause screen active");
            return;
        }

        EntityEditor editor = findEditorEntity(mc);
        if (editor == null) {
            logRare("HUD: no editor entity found");
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int w = 170;
        int h = 78;
        int x = 4;
        int y = 4;

        graphics.fill(x, y, x + w, y + h, 0xEE000000);
        graphics.hLine(x, x + w - 1, y, 0xFF555555);
        graphics.hLine(x, x + w - 1, y + h - 1, 0xFF555555);
        graphics.vLine(x, y, y + h - 1, 0xFF555555);
        graphics.vLine(x + w - 1, y, y + h - 1, 0xFF555555);

        String mode = getModeName(editor.getEditMode());
        int[] s = editor.getPos(EntityEditor.START_POS);
        int[] e = editor.getPos(EntityEditor.END_POS);
        int sx = Math.abs(s[0] - e[0]) + 1;
        int sy = Math.abs(s[1] - e[1]) + 1;
        int sz = Math.abs(s[2] - e[2]) + 1;

        graphics.drawString(mc.font, "Yabune Editor", x + 4, y + 4, 0xFFFFFF55);
        graphics.drawString(mc.font, "Mode: " + mode, x + 4, y + 16, 0xFFFFFFFF);
        graphics.drawString(mc.font, String.format("Size: %dx%dx%d", sx, sy, sz), x + 4, y + 28, 0xFF00FF00);
        graphics.drawString(mc.font, "M: mode  K: menu", x + 4, y + 42, 0xFFAAAAAA);
        graphics.drawString(mc.font, "X/C/V: cut/copy/paste", x + 4, y + 53, 0xFFAAAAAA);
        graphics.drawString(mc.font, "Z: undo  B: fill", x + 4, y + 64, 0xFFAAAAAA);

        logRare("HUD: rendered mode=" + mode);
    }

    private static void logRare(String msg) {
        if (++logCooldown >= 60) {
            logCooldown = 0;
            LOGGER.info(msg);
        }
    }

    private static EntityEditor findEditorEntity(Minecraft mc) {
        String myName = mc.player.getName().getString();
        EntityEditor found = null;
        double best = 128.0D * 128.0D;
        int count = 0;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EntityEditor ee) {
                count++;
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
        if (count == 0) {
            logRare("HUD: no EntityEditor in entitiesForRendering");
        } else {
            logRare("HUD: found " + count + " EntityEditor(s), selected=" + (found != null));
        }
        return found;
    }

    private static String getModeName(byte mode) {
        return switch (mode) {
            case 0 -> "Set Start";
            case 1 -> "Set End";
            case 2 -> "Paste";
            case 3 -> "Clone";
            default -> "?";
        };
    }
}
