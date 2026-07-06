package com.pinkara.ye.network;

import com.mojang.logging.LogUtils;
import com.pinkara.ye.YE;
import com.pinkara.ye.editor.MeshExporter;
import com.pinkara.youma.block.NGTObject;
import com.pinkara.youma.io.NGTLog;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.nio.file.Path;

public record PacketStructureData(CompoundTag tag, byte format, String path) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Type<PacketStructureData> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "structure_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketStructureData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, PacketStructureData::tag,
            ByteBufCodecs.BYTE, PacketStructureData::format,
            ByteBufCodecs.STRING_UTF8, PacketStructureData::path,
            PacketStructureData::new);

    public PacketStructureData(NGTObject data, MeshExporter.Format format, Path path) {
        this(data.writeToNBT(), (byte) switch (format) {
            case STL -> 1;
            case FBX -> 2;
            default -> 0;
        }, path.toString());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketStructureData message, IPayloadContext ctx) {
        LOGGER.info("PacketStructureData received format={} path='{}'", message.format(), message.path());
        ctx.enqueueWork(() -> {
            NGTObject data = NGTObject.readFromNBT(message.tag());
            if (data == null) {
                LOGGER.error("Failed to read structure data");
                return;
            }
            MeshExporter.Format fmt = switch (message.format()) {
                case 1 -> MeshExporter.Format.STL;
                case 2 -> MeshExporter.Format.FBX;
                default -> MeshExporter.Format.OBJ;
            };
            Path target = Path.of(message.path());
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Exporting structure..."), false);
            }
            boolean ok;
            String error = null;
            try {
                ok = MeshExporter.export(data, mc.getBlockRenderer(), target, fmt);
            } catch (Exception e) {
                ok = false;
                error = e.getClass().getSimpleName() + ": " + e.getMessage();
                LOGGER.error("Exception during structure export", e);
            }
            if (mc.player != null) {
                if (ok) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Exported structure to: " + target), false);
                } else {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Failed to export structure." + (error != null ? " " + error : "")), false);
                }
            }
        });
    }
}
