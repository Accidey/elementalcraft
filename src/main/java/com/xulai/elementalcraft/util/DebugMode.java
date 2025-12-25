// src/main/java/com/xulai/elementalcraft/util/DebugMode.java
package com.xulai.elementalcraft.util;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DebugMode 类负责管理模组的属性调试模式。
 * 它使用线程安全的 ConcurrentHashMap 记录已开启调试的玩家 UUID。
 * 开启调试的玩家会在受到或造成元素伤害时收到详细的战斗日志信息。
 *
 * DebugMode class manages the elemental debug mode of the mod.
 * It uses a thread-safe ConcurrentHashMap to track UUIDs of players who have debug mode enabled.
 * Players with debug mode enabled will receive detailed combat log messages when dealing or receiving elemental damage.
 */
public class DebugMode {
    /**
     * 存储开启调试模式的玩家 UUID 映射（线程安全）。
     *
     * Thread-safe map storing UUIDs of players with debug mode enabled.
     */
    private static final Map<UUID, Boolean> DEBUG_PLAYERS = new ConcurrentHashMap<>();

    /**
     * 为指定玩家设置调试模式状态。
     *
     * Sets the debug mode status for the specified player.
     *
     * @param player 要设置的玩家 / Player to set
     * @param enabled 是否开启 / Whether to enable
     */
    public static void setEnabled(Player player, boolean enabled) {
        if (enabled) {
            DEBUG_PLAYERS.put(player.getUUID(), true);
        } else {
            DEBUG_PLAYERS.remove(player.getUUID());
        }
    }

    /**
     * 检查指定玩家是否已开启调试模式。
     *
     * Checks if the specified player has debug mode enabled.
     *
     * @param player 要检查的玩家 / Player to check
     * @return 是否开启 / Whether enabled
     */
    public static boolean isEnabled(Player player) {
        return player != null && DEBUG_PLAYERS.containsKey(player.getUUID());
    }

    /**
     * 切换指定玩家的调试模式状态（开启 ↔ 关闭）。
     *
     * Toggles the debug mode status for the specified player (enabled ↔ disabled).
     *
     * @param player 要切换的玩家 / Player to toggle
     */
    public static void toggle(Player player) {
        setEnabled(player, !isEnabled(player));
    }

    /**
     * 检查是否有任意玩家开启了调试模式。
     * 用于优化：如果无人开启调试，则跳过日志计算和发送。
     *
     * Checks if any player has debug mode enabled.
     * Used for optimization: skip log calculation and sending if no one has debug enabled.
     *
     * @return 是否有玩家开启 / Whether any player has debug enabled
     */
    public static boolean hasAnyDebugEnabled() {
        return !DEBUG_PLAYERS.isEmpty();
    }
}