package com.pinkara.ye.network;

import com.mojang.logging.LogUtils;
import com.pinkara.ye.YE;
import com.pinkara.ye.editor.StructureManager;
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

public record PacketDeleteStructure(String name) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Type<PacketDeleteStructure> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "delete_structure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketDeleteStructure> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketDeleteStructure::name,
            PacketDeleteStructure::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketDeleteStructure message, IPayloadContext ctx) {
        Player player = ctx.player();
        LOGGER.info("PacketDeleteStructure received name='{}' player={}", message.name(), player);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.error("PacketDeleteStructure: player is not ServerPlayer");
            return;
        }
        ctx.enqueueWork(() -> {
            boolean ok = StructureManager.INSTANCE.delete(message.name());
            LOGGER.info("PacketDeleteStructure delete result={}", ok);
            NGTLog.sendChatMessage(serverPlayer.createCommandSourceStack(), ok ? "Deleted structure: " + message.name() : "Failed to delete structure: " + message.name());
            PacketStructureList.sendToPlayer(serverPlayer);
        });
    }
}
