package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.Editor;
import com.pinkara.ye.editor.EditorManager;
import com.pinkara.ye.editor.EntityEditor;
import com.pinkara.ye.editor.EditorTransform;
import com.pinkara.ye.editor.filter.EditFilterFill;
import com.pinkara.ye.editor.filter.Config;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketEditor(int sx, int sy, int sz, int ex, int ey, int ez, int cx, int cy, int cz, int cr, String action) implements CustomPacketPayload {
    public static final Type<PacketEditor> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "editor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketEditor> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeVarInt(pkt.sx());
                buf.writeVarInt(pkt.sy());
                buf.writeVarInt(pkt.sz());
                buf.writeVarInt(pkt.ex());
                buf.writeVarInt(pkt.ey());
                buf.writeVarInt(pkt.ez());
                buf.writeVarInt(pkt.cx());
                buf.writeVarInt(pkt.cy());
                buf.writeVarInt(pkt.cz());
                buf.writeVarInt(pkt.cr());
                ByteBufCodecs.STRING_UTF8.encode(buf, pkt.action());
            },
            buf -> new PacketEditor(
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), ByteBufCodecs.STRING_UTF8.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketEditor message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor == null) return;
            EntityEditor entity = editor.getEntity();

            entity.setPos(EntityEditor.START_POS, message.sx(), message.sy(), message.sz());
            entity.setPos(EntityEditor.END_POS, message.ex(), message.ey(), message.ez());
            entity.setCloneBox(message.cx(), message.cy(), message.cz(), message.cr());

            String action = message.action();
            if (action.isEmpty()) return;

            if (action.equals("fill") || action.equals("replace")) {
                EditFilterFill filter = new EditFilterFill();
                filter.init(new Config());
                filter.edit(editor);
            } else if (action.equals("clone")) {
                editor.editBlocks(Editor.EditType_Clone, 0.0f);
            } else if (action.startsWith("transform:")) {
                int type = Integer.parseInt(action.substring(10));
                editor.transformBlocks(EditorTransform.values()[type]);
            } else if (action.equals("delete_entity")) {
                entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        });
    }
}
