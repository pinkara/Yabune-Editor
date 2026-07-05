package com.pinkara.youma.io;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class NGTLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("YP");
    private static final List<Long> START_TIMES = new ArrayList<>();

    public static void debug(String par1) {
        debug(par1, new Object[0]);
    }

    public static void debug(String par1, Object... par2) {
        try {
            String message = par1;
            if (par2 != null && par2.length > 0) {
                message = String.format(par1, par2);
            }
            LOGGER.info(message);
        } catch (Exception ignored) {
        }
    }

    public static void sendChatMessage(CommandSourceStack source, String message, Object... objects) {
        source.sendSuccess(() -> Component.translatable(message, objects), false);
    }

    public static void startTimer() {
        START_TIMES.add(System.currentTimeMillis());
    }

    public static void resetTimer(String msg) {
        stopTimer(msg);
        startTimer();
    }

    public static void stopTimer(String msg) {
        if (START_TIMES.isEmpty()) {
            debug("Timer is not started");
        } else {
            long start = START_TIMES.remove(START_TIMES.size() - 1);
            long time = System.currentTimeMillis() - start;
            debug(msg + ":" + time + "ms");
        }
    }
}
