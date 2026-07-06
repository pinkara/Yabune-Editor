package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.StructureManager;
import com.pinkara.ye.gui.LibraryScreen;
import com.pinkara.ye.gui.StructureBrowserScreen;
import com.pinkara.ye.gui.StructureExportScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record PacketStructureList(List<String> names) implements CustomPacketPayload {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static final Type<PacketStructureList> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "structure_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketStructureList> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), PacketStructureList::names,
            PacketStructureList::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketStructureList message, IPayloadContext ctx) {
        LOGGER.info("PacketStructureList received client-side: {} names", message.names().size());
        ctx.enqueueWork(() -> {
            StructureBrowserScreen.setServerList(message.names());
            LibraryScreen.setServerList(message.names());
            StructureExportScreen.setServerList(message.names());
        });
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player) {
        List<String> names = StructureManager.INSTANCE.listStructures();
        LOGGER.info("PacketStructureList sending to {}: {} names", player.getName().getString(), names.size());
        PacketDistributor.sendToPlayer(player, new PacketStructureList(names));
    }
}
