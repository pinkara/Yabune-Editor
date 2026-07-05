package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class YENetwork {
    public static void register(IEventBus bus) {
        bus.addListener(YENetwork::registerMessages);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void registerMessages(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(YE.MODID).versioned("1");
        registrar.playToServer(PacketYEKey.TYPE, PacketYEKey.STREAM_CODEC, PacketYEKey::handle);
        registrar.playToServer(PacketEditorAction.TYPE, PacketEditorAction.STREAM_CODEC, PacketEditorAction::handle);
        registrar.playToServer(PacketEditor.TYPE, PacketEditor.STREAM_CODEC, PacketEditor::handle);
        registrar.playToServer(PacketChangeBlocks.TYPE, PacketChangeBlocks.STREAM_CODEC, PacketChangeBlocks::handle);
        registrar.playToClient(PacketRenderBlocks.TYPE, PacketRenderBlocks.STREAM_CODEC, PacketRenderBlocks::handle);
        registrar.playToClient(PacketOpenEditorGui.TYPE, PacketOpenEditorGui.STREAM_CODEC, PacketOpenEditorGui::handle);
        registrar.playToServer(PacketSaveStructure.TYPE, PacketSaveStructure.STREAM_CODEC, PacketSaveStructure::handle);
        registrar.playToServer(PacketLoadStructure.TYPE, PacketLoadStructure.STREAM_CODEC, PacketLoadStructure::handle);
        registrar.playToServer(PacketRequestStructureList.TYPE, PacketRequestStructureList.STREAM_CODEC, PacketRequestStructureList::handle);
        registrar.playToClient(PacketStructureList.TYPE, PacketStructureList.STREAM_CODEC, PacketStructureList::handle);
        registrar.playToServer(PacketExportMesh.TYPE, PacketExportMesh.STREAM_CODEC, PacketExportMesh::handle);
    }
}
