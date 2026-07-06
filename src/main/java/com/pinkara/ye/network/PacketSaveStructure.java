package com.pinkara.ye.network;

import com.mojang.logging.LogUtils;
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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public record PacketSaveStructure(String name) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Type<PacketSaveStructure> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "save_structure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSaveStructure> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketSaveStructure::name,
            PacketSaveStructure::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSaveStructure message, IPayloadContext ctx) {
        Player player = ctx.player();
        LOGGER.info("PacketSaveStructure received name='{}' player={}", message.name(), player);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.error("PacketSaveStructure: player is not ServerPlayer");
            return;
        }
        ctx.enqueueWork(() -> {
            try {
                Editor editor = EditorManager.INSTANCE.getEditor(player);
                LOGGER.info("PacketSaveStructure editor={} hasClipboard={}", editor, editor != null && editor.hasClipboard());
                if (editor == null || !editor.hasClipboard()) {
                    NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), "Nothing to save. Copy a selection first.");
                    return;
                }
                NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), "Saving structure: " + message.name());
                NGTObject ypo = editor.getClipboard().convertYPO();
                if (ypo == null) {
                    NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), "Failed to convert clipboard.");
                    return;
                }
                boolean ok = StructureManager.INSTANCE.save(message.name(), ypo);
                LOGGER.info("PacketSaveStructure save result={}", ok);
                NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), ok ? "Saved structure: " + message.name() : "Failed to save structure.");
            } catch (Exception e) {
                LOGGER.error("PacketSaveStructure error", e);
                NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), "Save error: " + e.getMessage());
            }
        });
    }
}
