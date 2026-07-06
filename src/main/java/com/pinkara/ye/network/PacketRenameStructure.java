package com.pinkara.ye.network;

import com.mojang.logging.LogUtils;
import com.pinkara.ye.YE;
import com.pinkara.ye.editor.StructureManager;
import com.pinkara.youma.block.NGTObject;
import com.pinkara.youma.io.NGTLog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public record PacketRenameStructure(String oldName, String newName) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Type<PacketRenameStructure> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "rename_structure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRenameStructure> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketRenameStructure::oldName,
            ByteBufCodecs.STRING_UTF8, PacketRenameStructure::newName,
            PacketRenameStructure::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketRenameStructure message, IPayloadContext ctx) {
        Player player = ctx.player();
        LOGGER.info("PacketRenameStructure received old='{}' new='{}' player={}", message.oldName(), message.newName(), player);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.error("PacketRenameStructure: player is not ServerPlayer");
            return;
        }
        ctx.enqueueWork(() -> {
            String oldName = message.oldName();
            String newName = StructureManager.INSTANCE.sanitizeName(message.newName());
            if (newName.isEmpty() || newName.equals(oldName)) {
                NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), "Invalid new name.");
                return;
            }
            NGTObject data = StructureManager.INSTANCE.load(oldName);
            if (data == null) {
                NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), "Structure not found: " + oldName);
                return;
            }
            boolean saved = StructureManager.INSTANCE.save(newName, data);
            if (!saved) {
                NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), "Failed to save renamed structure.");
                return;
            }
            StructureManager.INSTANCE.delete(oldName);
            NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), "Renamed structure to: " + newName);
            PacketStructureList.sendToPlayer(serverPlayer);
        });
    }
}
