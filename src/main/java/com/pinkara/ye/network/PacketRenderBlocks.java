package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.youma.block.NGTObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketRenderBlocks(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PacketRenderBlocks> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "render_blocks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRenderBlocks> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG, PacketRenderBlocks::data,
            PacketRenderBlocks::new);

    public PacketRenderBlocks(NGTObject ngto) {
        this(ngto.writeToNBT(false));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketRenderBlocks message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().isClientSide) {
                com.pinkara.ye.editor.EntityEditor editor = findEditor(ctx.player());
                if (editor != null) {
                    editor.blocksForRenderer = NGTObject.readFromNBT(message.data());
                    editor.setUpdate(true);
                }
            }
        });
    }

    private static com.pinkara.ye.editor.EntityEditor findEditor(net.minecraft.world.entity.player.Player player) {
        if (!(player.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
            return null;
        }
        String name = player.getGameProfile().getName();
        for (net.minecraft.world.entity.Entity entity : clientLevel.entitiesForRendering()) {
            if (entity instanceof com.pinkara.ye.editor.EntityEditor editor) {
                if (name.equals(editor.getEntityData().get(com.pinkara.ye.editor.EntityEditor.PLAMCTER))) {
                    return editor;
                }
            }
        }
        return null;
    }
}
