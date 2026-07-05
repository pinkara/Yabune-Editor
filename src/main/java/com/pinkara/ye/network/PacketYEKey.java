package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.YEKeyHandlerServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketYEKey(byte keyId) implements CustomPacketPayload {
    public static final Type<PacketYEKey> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketYEKey> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, PacketYEKey::keyId,
            PacketYEKey::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketYEKey message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> YEKeyHandlerServer.INSTANCE.onKeyDown(ctx.player(), message.keyId));
    }
}
