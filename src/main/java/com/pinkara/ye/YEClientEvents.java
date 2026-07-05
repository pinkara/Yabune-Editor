package com.pinkara.ye;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = YE.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class YEClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (YEKeyHandlerClient.KEY_EDIT_MODE.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_EditMode));
        }
        if (YEKeyHandlerClient.KEY_EDIT_MENU.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_EditMenu));
        }
        if (YEKeyHandlerClient.KEY_UNDO.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Undo));
        }
        if (YEKeyHandlerClient.KEY_CLEAR.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Clear));
        }
        if (YEKeyHandlerClient.KEY_DELETE.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Delete));
        }
        if (YEKeyHandlerClient.KEY_CUT.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Cut));
        }
        if (YEKeyHandlerClient.KEY_COPY.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Copy));
        }
        if (YEKeyHandlerClient.KEY_PASTE.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Paste));
        }
        if (YEKeyHandlerClient.KEY_FILL.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Fill));
        }

        // Numpad direct transform keys
        if (YEKeyHandlerClient.KEY_NP_ROT_X.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotX));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_X_MINUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotX_Minus));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_Y.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotY));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_Y_MINUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotY_Minus));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_Z.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotZ));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_Z_MINUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotZ_Minus));
        }
        if (YEKeyHandlerClient.KEY_NP_MIRROR_X.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_MirrorX));
        }
        if (YEKeyHandlerClient.KEY_NP_MIRROR_Y.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_MirrorY));
        }
        if (YEKeyHandlerClient.KEY_NP_MIRROR_Z.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_MirrorZ));
        }

        // Clone box adjustment keys
        if (YEKeyHandlerClient.KEY_CLONE_X_MINUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneXMinus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_X_PLUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneXPlus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_Y_MINUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneYMinus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_Y_PLUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneYPlus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_Z_MINUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneZMinus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_Z_PLUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneZPlus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_COUNT_MINUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneCountMinus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_COUNT_PLUS.consumeClick()) {
            PacketDistributor.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneCountPlus));
        }
    }
}
