package com.pinkara.ye.network;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.ChangeEntry;
import com.pinkara.ye.editor.Editor;
import com.pinkara.ye.editor.EditorManager;
import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.block.BlockUtil;
import com.pinkara.youma.math.AABBInt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record PacketChangeBlocks(List<ChangeEntry> entries) implements CustomPacketPayload {
    public static final Type<PacketChangeBlocks> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YE.MODID, "change_blocks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketChangeBlocks> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                List<ChangeEntry> list = pkt.entries();
                buf.writeVarInt(list.size());
                for (ChangeEntry entry : list) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.fromItem);
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.toItem);
                }
            },
            buf -> {
                int count = buf.readVarInt();
                List<ChangeEntry> list = new ArrayList<>(count);
                for (int i = 0; i < count; ++i) {
                    ChangeEntry entry = new ChangeEntry();
                    entry.fromItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    entry.toItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    list.add(entry);
                }
                return new PacketChangeBlocks(list);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketChangeBlocks message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Editor editor = EditorManager.INSTANCE.getEditor(player);
            if (editor == null) return;

            AABBInt box = editor.getSelectBox();
            if (box == null) return;

            editor.record(box);
            box.repeat((x, y, z, count) -> {
                BlockState current = BlockUtil.getBlockState(editor.getWorld(), x, y, z);
                for (ChangeEntry entry : message.entries()) {
                    if (entry.isValid() && current == entry.getFromState()) {
                        BlockSet set = new BlockSet(entry.getToState(), null);
                        editor.setBlock(x, y, z, set, true);
                        break;
                    }
                }
            });
            editor.updateBlocks(box);
        });
    }
}
