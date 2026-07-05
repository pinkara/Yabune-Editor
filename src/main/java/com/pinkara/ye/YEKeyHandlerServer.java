package com.pinkara.ye;

import com.pinkara.ye.editor.Editor;
import com.pinkara.ye.editor.EditorManager;
import com.pinkara.ye.editor.EditorTransform;
import com.pinkara.ye.editor.filter.Config;
import com.pinkara.ye.editor.filter.EditFilterCopy;
import com.pinkara.ye.editor.filter.EditFilterCut;
import com.pinkara.ye.editor.filter.EditFilterDelete;
import com.pinkara.ye.editor.filter.EditFilterFill;
import com.pinkara.ye.editor.filter.EditFilterPaste;
import com.pinkara.ye.network.PacketOpenEditorGui;
import com.pinkara.youma.io.NGTLog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class YEKeyHandlerServer {
    public static final YEKeyHandlerServer INSTANCE = new YEKeyHandlerServer();

    private YEKeyHandlerServer() {
    }

    public void onKeyDown(Player player, byte keyCode) {
        if (keyCode == YEKeyHandlerClient.KEY_EditMenu) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null && editor.getEntity().isSelectEnd()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new PacketOpenEditorGui(editor.getEntity().getId()));
                }
            }
        } else if (keyCode == YEKeyHandlerClient.KEY_EditMode) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                byte b = (byte) (editor.getEntity().getEditMode() + 1);
                b = editor.getEntity().isSelectEnd()
                        ? (b > 3 ? (byte) 2 : (b < 2 ? (byte) 2 : b))
                        : (b > 1 ? (byte) 0 : b);
                editor.getEntity().setEditMode(b);
                String key = switch (b) {
                    case 0 -> "message.ye.mode.select_start";
                    case 1 -> "message.ye.mode.select_end";
                    case 2 -> "message.ye.mode.paste";
                    case 3 -> "message.ye.mode.clone";
                    default -> "";
                };
                if (!key.isEmpty()) {
                    NGTLog.sendChatMessage(player.createCommandSourceStack(), key);
                }
            }
        } else if (keyCode == YEKeyHandlerClient.KEY_Undo) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                editor.undo();
                NGTLog.sendChatMessage(player.createCommandSourceStack(), "message.ye.undo");
            }
        } else if (keyCode == YEKeyHandlerClient.KEY_Clear) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                editor.getEntity().remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        } else if (keyCode == YEKeyHandlerClient.KEY_Delete) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                EditFilterDelete filter = new EditFilterDelete();
                filter.init(new Config());
                if (filter.edit(editor)) {
                    NGTLog.sendChatMessage(player.createCommandSourceStack(), "message.ye.deleted");
                }
            }
        } else if (keyCode == YEKeyHandlerClient.KEY_Cut) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                EditFilterCut filter = new EditFilterCut();
                filter.init(new Config());
                if (filter.edit(editor)) {
                    NGTLog.sendChatMessage(player.createCommandSourceStack(), "message.ye.cut");
                }
            }
        } else if (keyCode == YEKeyHandlerClient.KEY_Copy) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                EditFilterCopy filter = new EditFilterCopy();
                filter.init(new Config());
                filter.edit(editor);
                editor.getEntity().setEditMode((byte) 2);
                NGTLog.sendChatMessage(player.createCommandSourceStack(), "message.ye.copied");
            }
        } else if (keyCode == YEKeyHandlerClient.KEY_Paste) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                byte mode = editor.getEntity().getEditMode();
                if (mode == 3) {
                    editor.editBlocks(Editor.EditType_Clone, 0.0f);
                    NGTLog.sendChatMessage(player.createCommandSourceStack(), "message.ye.cloned");
                } else {
                    if (mode != 2) {
                        editor.getEntity().setEditMode((byte) 2);
                    }
                    EditFilterPaste filter = new EditFilterPaste();
                    filter.init(new Config());
                    if (filter.edit(editor)) {
                        NGTLog.sendChatMessage(player.createCommandSourceStack(), "message.ye.pasted");
                    }
                }
            }
        } else if (keyCode == YEKeyHandlerClient.KEY_Fill) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                EditFilterFill filter = new EditFilterFill();
                filter.init(new Config());
                if (filter.edit(editor)) {
                    NGTLog.sendChatMessage(player.createCommandSourceStack(), "message.ye.filled");
                }
            }
        } else if (isCloneAdjustKey(keyCode)) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null) {
                applyCloneAdjustment(editor, keyCode);
            }
        } else if (isNumpadTransformKey(keyCode)) {
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor != null && editor.hasClipboard()) {
                EditorTransform transform = numpadKeyToTransform(keyCode);
                if (transform != null) {
                    editor.transformBlocks(transform);
                }
            }
        }
    }

    private static boolean isNumpadTransformKey(byte keyCode) {
        return keyCode >= YEKeyHandlerClient.KEY_NP_RotX && keyCode <= YEKeyHandlerClient.KEY_NP_MirrorZ;
    }

    private static boolean isCloneAdjustKey(byte keyCode) {
        return keyCode >= YEKeyHandlerClient.KEY_CloneXMinus && keyCode <= YEKeyHandlerClient.KEY_CloneCountPlus;
    }

    private static void applyCloneAdjustment(Editor editor, byte keyCode) {
        int[] box = editor.getEntity().getCloneBox();
        int delta = 1;
        switch (keyCode) {
            case YEKeyHandlerClient.KEY_CloneXMinus -> box[0] -= delta;
            case YEKeyHandlerClient.KEY_CloneXPlus -> box[0] += delta;
            case YEKeyHandlerClient.KEY_CloneYMinus -> box[1] -= delta;
            case YEKeyHandlerClient.KEY_CloneYPlus -> box[1] += delta;
            case YEKeyHandlerClient.KEY_CloneZMinus -> box[2] -= delta;
            case YEKeyHandlerClient.KEY_CloneZPlus -> box[2] += delta;
            case YEKeyHandlerClient.KEY_CloneCountMinus -> box[3] = Math.max(0, box[3] - 1);
            case YEKeyHandlerClient.KEY_CloneCountPlus -> box[3] = Math.min(255, box[3] + 1);
        }
        editor.getEntity().setCloneBox(box[0], box[1], box[2], box[3]);
    }

    private static EditorTransform numpadKeyToTransform(byte keyCode) {
        return switch (keyCode) {
            case YEKeyHandlerClient.KEY_NP_RotX -> EditorTransform.Transform_RotateX;
            case YEKeyHandlerClient.KEY_NP_RotX_Minus -> EditorTransform.Transform_RotateX_Minus;
            case YEKeyHandlerClient.KEY_NP_RotY -> EditorTransform.Transform_RotateY;
            case YEKeyHandlerClient.KEY_NP_RotY_Minus -> EditorTransform.Transform_RotateY_Minus;
            case YEKeyHandlerClient.KEY_NP_RotZ -> EditorTransform.Transform_RotateZ;
            case YEKeyHandlerClient.KEY_NP_RotZ_Minus -> EditorTransform.Transform_RotateZ_Minus;
            case YEKeyHandlerClient.KEY_NP_MirrorX -> EditorTransform.Transform_MirrorX;
            case YEKeyHandlerClient.KEY_NP_MirrorY -> EditorTransform.Transform_MirrorY;
            case YEKeyHandlerClient.KEY_NP_MirrorZ -> EditorTransform.Transform_MirrorZ;
            default -> null;
        };
    }
}
