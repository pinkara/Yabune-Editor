package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketRequestStructureList() implements CustomPacketPayload {
    public static final Type<PacketRequestStructureList> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "request_structure_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequestStructureList> STREAM_CODEC = StreamCodec.unit(new PacketRequestStructureList());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketRequestStructureList message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PacketStructureList.sendToPlayer(serverPlayer);
            }
        });
    }
}
