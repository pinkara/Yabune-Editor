package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.Editor;
import com.pinkara.ye.editor.EditorManager;
import com.pinkara.ye.editor.StructureManager;
import com.pinkara.youma.io.NGTLog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSaveStructure(String name) implements CustomPacketPayload {
    public static final Type<PacketSaveStructure> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "save_structure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSaveStructure> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketSaveStructure::name,
            PacketSaveStructure::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSaveStructure message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor == null || !editor.hasClipboard()) {
                NGTLog.sendChatMessage(player.createCommandSourceStack(), "Nothing to save. Copy a selection first.");
                return;
            }
            boolean ok = StructureManager.INSTANCE.save(message.name(), editor.getClipboard().convertYPO());
            NGTLog.sendChatMessage(player.createCommandSourceStack(), ok ? "Saved structure: " + message.name() : "Failed to save structure.");
        });
    }
}
