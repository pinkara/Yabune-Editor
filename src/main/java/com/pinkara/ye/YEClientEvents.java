package com.pinkara.ye;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.pinkara.ye.network.YENetwork;

@EventBusSubscriber(modid = YE.MODID, value = Dist.CLIENT)
public class YEClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (YEKeyHandlerClient.KEY_EDIT_MODE.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_EditMode));
        }
        if (YEKeyHandlerClient.KEY_EDIT_MENU.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_EditMenu));
        }
        if (YEKeyHandlerClient.KEY_UNDO.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Undo));
        }
        if (YEKeyHandlerClient.KEY_CLEAR.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Clear));
        }
        if (YEKeyHandlerClient.KEY_DELETE.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Delete));
        }
        if (YEKeyHandlerClient.KEY_CUT.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Cut));
        }
        if (YEKeyHandlerClient.KEY_COPY.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Copy));
        }
        if (YEKeyHandlerClient.KEY_PASTE.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Paste));
        }
        if (YEKeyHandlerClient.KEY_FILL.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_Fill));
        }

        // Numpad direct transform keys
        if (YEKeyHandlerClient.KEY_NP_ROT_X.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotX));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_X_MINUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotX_Minus));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_Y.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotY));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_Y_MINUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotY_Minus));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_Z.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotZ));
        }
        if (YEKeyHandlerClient.KEY_NP_ROT_Z_MINUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_RotZ_Minus));
        }
        if (YEKeyHandlerClient.KEY_NP_MIRROR_X.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_MirrorX));
        }
        if (YEKeyHandlerClient.KEY_NP_MIRROR_Y.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_MirrorY));
        }
        if (YEKeyHandlerClient.KEY_NP_MIRROR_Z.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_NP_MirrorZ));
        }

        // Clone box adjustment keys
        if (YEKeyHandlerClient.KEY_CLONE_X_MINUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneXMinus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_X_PLUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneXPlus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_Y_MINUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneYMinus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_Y_PLUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneYPlus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_Z_MINUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneZMinus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_Z_PLUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneZPlus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_COUNT_MINUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneCountMinus));
        }
        if (YEKeyHandlerClient.KEY_CLONE_COUNT_PLUS.consumeClick()) {
            YENetwork.sendToServer(new com.pinkara.ye.network.PacketYEKey(YEKeyHandlerClient.KEY_CloneCountPlus));
        }


    }
}
