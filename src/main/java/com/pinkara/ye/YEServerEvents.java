package com.pinkara.ye;

import com.pinkara.ye.editor.EditorManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = YE.MODID, bus = EventBusSubscriber.Bus.GAME)
public class YEServerEvents {
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        EditorManager.INSTANCE.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        EditorManager.INSTANCE.removeAll();
    }
}
