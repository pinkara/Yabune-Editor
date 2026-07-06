package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.Editor;
import com.pinkara.ye.editor.EditorManager;
import com.pinkara.ye.editor.EditorTransform;
import com.pinkara.ye.editor.EntityEditor;
import com.pinkara.ye.editor.filter.Config;
import com.pinkara.ye.editor.filter.EditFilterCopy;
import com.pinkara.ye.editor.filter.EditFilterPaste;
import com.pinkara.youma.io.NGTLog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketEditorAction(byte action, int v0, int v1, int v2, int v3) implements CustomPacketPayload {
    public static final Type<PacketEditorAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "editor_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketEditorAction> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, PacketEditorAction::action,
            ByteBufCodecs.VAR_INT, PacketEditorAction::v0,
            ByteBufCodecs.VAR_INT, PacketEditorAction::v1,
            ByteBufCodecs.VAR_INT, PacketEditorAction::v2,
            ByteBufCodecs.VAR_INT, PacketEditorAction::v3,
            PacketEditorAction::new);

    public static final byte ACTION_COPY = 0;
    public static final byte ACTION_PASTE = 1;
    public static final byte ACTION_UNDO = 2;
    public static final byte ACTION_CLEAR = 3;
    public static final byte ACTION_CLONE = 5;
    public static final byte ACTION_FILL = 6;
    public static final byte ACTION_REPLACE = 7;
    public static final byte ACTION_CUT = 8;
    public static final byte ACTION_ROTATE_X = 10;
    public static final byte ACTION_ROTATE_Y = 11;
    public static final byte ACTION_ROTATE_Z = 12;
    public static final byte ACTION_MIRROR_X = 13;
    public static final byte ACTION_MIRROR_Y = 14;
    public static final byte ACTION_MIRROR_Z = 15;
    public static final byte ACTION_SET_CLONE_BOX = 20;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketEditorAction message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor == null) return;
            EntityEditor entity = editor.getEntity();

            switch (message.action()) {
                case ACTION_COPY -> {
                    EditFilterCopy filter = new EditFilterCopy();
                    filter.init(new Config());
                    filter.edit(editor);
                    NGTLog.sendChatMessage(((ServerPlayer) player).createCommandSourceStack(), "Copied selection");
                }
                case ACTION_PASTE -> {
                    EditFilterPaste filter = new EditFilterPaste();
                    filter.init(new Config());
                    filter.edit(editor);
                    NGTLog.sendChatMessage(((ServerPlayer) player).createCommandSourceStack(), "Pasted selection");
                }
                case ACTION_UNDO -> editor.undo();
                case ACTION_CLEAR -> entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                case ACTION_CLONE -> {
                    if (message.v3() > 0) {
                        entity.setCloneBox(message.v0(), message.v1(), message.v2(), message.v3());
                    }
                    editor.editBlocks(Editor.EditType_Clone, 0.0f);
                    NGTLog.sendChatMessage(((ServerPlayer) player).createCommandSourceStack(), "Cloned selection");
                }
                case ACTION_SET_CLONE_BOX -> entity.setCloneBox(message.v0(), message.v1(), message.v2(), message.v3());
                case ACTION_FILL -> editor.editBlocks(Editor.EditType_Replace, 0.0f);
                case ACTION_CUT -> {
                    com.pinkara.ye.editor.filter.EditFilterCut filter = new com.pinkara.ye.editor.filter.EditFilterCut();
                    filter.init(new Config());
                    filter.edit(editor);
                }
                case ACTION_ROTATE_X -> editor.transformBlocks(EditorTransform.Transform_RotateX);
                case ACTION_ROTATE_Y -> editor.transformBlocks(EditorTransform.Transform_RotateY);
                case ACTION_ROTATE_Z -> editor.transformBlocks(EditorTransform.Transform_RotateZ);
                case ACTION_MIRROR_X -> editor.transformBlocks(EditorTransform.Transform_MirrorX);
                case ACTION_MIRROR_Y -> editor.transformBlocks(EditorTransform.Transform_MirrorY);
                case ACTION_MIRROR_Z -> editor.transformBlocks(EditorTransform.Transform_MirrorZ);
            }
        });
    }
}
