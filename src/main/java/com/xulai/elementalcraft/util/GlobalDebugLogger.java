package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.util.List;

public class GlobalDebugLogger {

    public static void log(Level level, String module, String message) {
        if (!DebugMode.hasAnyDebugEnabled()) return;

        MutableComponent component = Component.literal("§8[§bDebug§8] §7[" + module + "]§r " + message);
        if (level instanceof ServerLevel serverLevel) {
            List<ServerPlayer> players = serverLevel.getServer().getPlayerList().getPlayers();
            for (ServerPlayer player : players) {
                if (DebugMode.isEnabled(player)) {
                    player.displayClientMessage(component, false);
                }
            }
        }
        ElementalCraft.LOGGER.info("[{}] {}", module, message);
    }

    public static void log(Level level, String message) {
        log(level, "Global", message);
    }

    public static void log(String module, String message) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        ElementalCraft.LOGGER.info("[{}] {}", module, message);
    }

    public static void logf(Level level, String module, String format, Object... args) {
        log(level, module, String.format(format, args));
    }

    public static void send(Level level, Component message) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        if (level instanceof ServerLevel serverLevel) {
            List<ServerPlayer> players = serverLevel.getServer().getPlayerList().getPlayers();
            for (ServerPlayer player : players) {
                if (DebugMode.isEnabled(player)) {
                    player.displayClientMessage(message, false);
                }
            }
        }
        ElementalCraft.LOGGER.info(message.getString());
    }
}