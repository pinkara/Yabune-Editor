package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.gui.EditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketOpenEditorGui(int editorId) implements CustomPacketPayload {
    public static final Type<PacketOpenEditorGui> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "open_editor_gui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketOpenEditorGui> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacketOpenEditorGui::editorId,
            PacketOpenEditorGui::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketOpenEditorGui message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(message.editorId());
            if (entity instanceof com.pinkara.ye.editor.EntityEditor editor) {
                mc.setScreen(new EditorScreen(editor));
            }
        });
    }
}
