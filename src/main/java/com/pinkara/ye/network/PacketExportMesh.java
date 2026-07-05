package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.Editor;
import com.pinkara.ye.editor.EditorManager;
import com.pinkara.ye.editor.MeshExporter;
import com.pinkara.youma.io.NGTLog;
import com.pinkara.youma.math.AABBInt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.file.Path;

public record PacketExportMesh(String fileName, byte format) implements CustomPacketPayload {
    public static final Type<PacketExportMesh> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "export_mesh"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketExportMesh> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketExportMesh::fileName,
            ByteBufCodecs.BYTE, PacketExportMesh::format,
            PacketExportMesh::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketExportMesh payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor == null) {
                NGTLog.sendChatMessage(player.createCommandSourceStack(), "No editor found.");
                return;
            }
            AABBInt box = editor.getSelectBox();
            if (box == null) {
                NGTLog.sendChatMessage(player.createCommandSourceStack(), "No selection set.");
                return;
            }

            MeshExporter.Format fmt = payload.format() == 1 ? MeshExporter.Format.STL : MeshExporter.Format.OBJ;
            String name = payload.fileName();
            if (name == null || name.isBlank()) name = "export";
            name = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String ext = fmt == MeshExporter.Format.OBJ ? ".obj" : ".stl";
            if (!name.toLowerCase().endsWith(ext)) name += ext;

            Path dir = FMLPaths.GAMEDIR.get().resolve("yabune_editor").resolve("exports");
            Path path = dir.resolve(name);
            boolean ok = MeshExporter.export(box, editor.getWorld(), path, fmt);
            NGTLog.sendChatMessage(player.createCommandSourceStack(), ok ? "Exported mesh to: " + path : "Failed to export mesh.");
        });
    }
}
