package com.pinkara.ye.network;

import com.mojang.logging.LogUtils;
import com.pinkara.ye.YE;
import com.pinkara.ye.editor.MeshExporter;
import com.pinkara.ye.editor.StructureManager;
import com.pinkara.youma.block.NGTObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.nio.file.Path;

public record PacketRequestStructureData(String name, byte format, String path) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Type<PacketRequestStructureData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "request_structure_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequestStructureData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketRequestStructureData::name,
            ByteBufCodecs.BYTE, PacketRequestStructureData::format,
            ByteBufCodecs.STRING_UTF8, PacketRequestStructureData::path,
            PacketRequestStructureData::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketRequestStructureData message, IPayloadContext ctx) {
        Player player = ctx.player();
        LOGGER.info("PacketRequestStructureData received name='{}' format={} path='{}'", message.name(), message.format(), message.path());
        ctx.enqueueWork(() -> {
            NGTObject data = StructureManager.INSTANCE.load(message.name());
            if (data == null) {
                LOGGER.warn("Structure not found: {}", message.name());
                return;
            }
            MeshExporter.Format fmt = switch (message.format()) {
                case 1 -> MeshExporter.Format.STL;
                case 2 -> MeshExporter.Format.FBX;
                default -> MeshExporter.Format.OBJ;
            };
            Path target = Path.of(message.path());
            PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, new PacketStructureData(data, fmt, target));
        });
    }
}
