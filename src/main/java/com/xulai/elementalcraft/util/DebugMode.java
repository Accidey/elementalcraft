package com.xulai.elementalcraft.util;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DebugMode {

    private static final Map<UUID, Boolean> DEBUG_PLAYERS = new ConcurrentHashMap<>();

    public static void setEnabled(Player player, boolean enabled) {
        if (enabled) {
            DEBUG_PLAYERS.put(player.getUUID(), true);
        } else {
            DEBUG_PLAYERS.remove(player.getUUID());
        }
    }

    public static void remove(Player player) {
        if (player != null) {
            DEBUG_PLAYERS.remove(player.getUUID());
        }
    }

    public static boolean isEnabled(Player player) {
        return player != null && DEBUG_PLAYERS.containsKey(player.getUUID());
    }

    public static void toggle(Player player) {
        setEnabled(player, !isEnabled(player));
    }

    public static boolean hasAnyDebugEnabled() {
        return !DEBUG_PLAYERS.isEmpty();
    }
}