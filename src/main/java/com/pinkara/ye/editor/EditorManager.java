package com.pinkara.ye.editor;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class EditorManager {
    public static final EditorManager INSTANCE = new EditorManager();
    private final Map<String, Editor> editorMap = new HashMap<>();

    private EditorManager() {
    }

    public void add(String playerName, Editor editor) {
        this.editorMap.put(playerName, editor);
    }

    public void remove(EntityEditor entity) {
        String key = "";
        for (Map.Entry<String, Editor> entry : this.editorMap.entrySet()) {
            if (!entry.getValue().getEntity().equals(entity)) continue;
            key = entry.getKey();
            break;
        }
        if (!key.isEmpty()) {
            this.editorMap.remove(key);
        }
    }

    public void remove(Player player) {
        this.editorMap.remove(player.getGameProfile().name());
    }

    public void removeAll() {
        for (Editor editor : this.editorMap.values()) {
            EntityEditor entity = editor.getEntity();
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
        this.editorMap.clear();
    }

    public boolean canPlayerUseEditor(Player par1) {
        return par1.hasPermissions(2); // OP level 2 or creative
    }

    public Editor getEditor(Player par1) {
        return this.getEditor(par1.getGameProfile().name());
    }

    public Editor getEditor(String par1) {
        for (Map.Entry<String, Editor> entry : this.editorMap.entrySet()) {
            if (!entry.getKey().equals(par1)) continue;
            return entry.getValue();
        }
        return null;
    }
}
