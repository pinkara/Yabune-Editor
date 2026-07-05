package com.pinkara.ye.client;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.EntityEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = YE.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class EditorHud {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        EntityEditor editor = findEditorEntity(mc);
        if (editor == null) return;
        if (mc.screen != null && mc.screen.isPauseScreen()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 4;
        int y = 4;
        int w = 160;
        int h = 78;

        graphics.fill(x, y, x + w, y + h, 0xCC000000);
        graphics.hLine(x, x + w - 1, y, 0xFF555555);
        graphics.hLine(x, x + w - 1, y + h - 1, 0xFF555555);
        graphics.vLine(x, y, y + h - 1, 0xFF555555);
        graphics.vLine(x + w - 1, y, y + h - 1, 0xFF555555);

        String mode = getModeName(editor.getEditMode());
        graphics.drawString(mc.font, "Yabune Editor", x + 4, y + 4, 0xFFFF55);
        graphics.drawString(mc.font, "Mode: " + mode, x + 4, y + 16, 0xFFFFFF);

        int[] s = editor.getPos(EntityEditor.START_POS);
        int[] e = editor.getPos(EntityEditor.END_POS);
        int sx = Math.abs(s[0] - e[0]) + 1;
        int sy = Math.abs(s[1] - e[1]) + 1;
        int sz = Math.abs(s[2] - e[2]) + 1;
        graphics.drawString(mc.font, String.format("Size: %dx%dx%d", sx, sy, sz), x + 4, y + 28, 0x00FF00);

        graphics.drawString(mc.font, "M: mode  K: menu", x + 4, y + 42, 0xAAAAAA);
        graphics.drawString(mc.font, "X/C/V: cut/copy/paste", x + 4, y + 53, 0xAAAAAA);
        graphics.drawString(mc.font, "Z: undo  B: fill", x + 4, y + 64, 0xAAAAAA);
    }

    private static EntityEditor findEditorEntity(Minecraft mc) {
        if (mc.level == null) return null;
        EntityEditor found = null;
        double best = 64.0D * 64.0D;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EntityEditor ee && ee.getPlayer() == mc.player) {
                double dist = entity.distanceToSqr(mc.player);
                if (dist < best) {
                    best = dist;
                    found = ee;
                }
            }
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
