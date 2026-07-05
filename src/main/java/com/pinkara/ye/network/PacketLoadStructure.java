package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.Editor;
import com.pinkara.ye.editor.EditorManager;
import com.pinkara.ye.editor.StructureManager;
import com.pinkara.youma.block.NGTObject;
import com.pinkara.youma.io.NGTLog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketLoadStructure(String name) implements CustomPacketPayload {
    public static final Type<PacketLoadStructure> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "load_structure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketLoadStructure> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketLoadStructure::name,
            PacketLoadStructure::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketLoadStructure message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor == null) {
                NGTLog.sendChatMessage(player.createCommandSourceStack(), "No editor found.");
                return;
            }
            NGTObject ngto = StructureManager.INSTANCE.load(message.name());
            if (ngto == null) {
                NGTLog.sendChatMessage(player.createCommandSourceStack(), "Structure not found: " + message.name());
                return;
            }
            editor.loadData(ngto);
            editor.getEntity().setEditMode((byte) 2);
            editor.getEntity().updateBlockList(ngto);
            NGTLog.sendChatMessage(player.createCommandSourceStack(), "Loaded structure: " + message.name());
        });
    }
}
