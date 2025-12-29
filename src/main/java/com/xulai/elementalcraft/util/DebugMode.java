// src/main/java/com/xulai/elementalcraft/util/DebugMode.java
package com.xulai.elementalcraft.util;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DebugMode
 * <p>
 * 中文说明：
 * 调试模式管理工具类。
 * 负责管理模组内的属性调试状态。
 * 使用线程安全的 ConcurrentHashMap 存储开启了调试模式的玩家 UUID。
 * 当玩家开启调试模式后，相关的战斗事件处理逻辑会向其发送详细的伤害计算公式和数值信息。
 * <p>
 * English Description:
 * Debug Mode Management Utility Class.
 * Responsible for managing the elemental debug status within the mod.
 * Uses a thread-safe ConcurrentHashMap to store the UUIDs of players who have enabled debug mode.
 * When a player enables debug mode, relevant combat event handling logic will send them detailed damage calculation formulas and value information.
 */
public class DebugMode {

    /**
     * 存储开启调试模式的玩家 UUID 映射表。
     * 使用 ConcurrentHashMap 以确保在多线程环境（如服务端 Tick 和事件处理）下的并发安全。
     * <p>
     * Map storing UUIDs of players with debug mode enabled.
     * Uses ConcurrentHashMap to ensure concurrency safety in multi-threaded environments (such as Server Tick and event handling).
     */
    private static final Map<UUID, Boolean> DEBUG_PLAYERS = new ConcurrentHashMap<>();

    /**
     * 为指定玩家设置调试模式状态。
     * 如果启用，将玩家 UUID 加入集合；如果禁用，则移除。
     * <p>
     * Sets the debug mode status for the specified player.
     * If enabled, adds the player UUID to the set; if disabled, removes it.
     *
     * @param player  要设置的玩家实体 / The player entity to set
     * @param enabled 是否开启调试模式 / Whether to enable debug mode
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
     * <p>
     * Checks if the specified player has debug mode enabled.
     *
     * @param player 要检查的玩家实体 / The player entity to check
     * @return 如果已开启返回 true，否则返回 false / true if enabled, false otherwise
     */
    public static boolean isEnabled(Player player) {
        return player != null && DEBUG_PLAYERS.containsKey(player.getUUID());
    }

    /**
     * 切换指定玩家的调试模式状态。
     * 如果当前开启则关闭，反之亦然。
     * <p>
     * Toggles the debug mode status for the specified player.
     * Disables if currently enabled, and vice versa.
     *
     * @param player 要切换状态的玩家实体 / The player entity to toggle
     */
    public static void toggle(Player player) {
        setEnabled(player, !isEnabled(player));
    }

    /**
     * 检查当前服务器是否有任意玩家开启了调试模式。
     * 用于性能优化：如果没有任何玩家开启调试，可以跳过复杂的调试信息构建和计算过程。
     * <p>
     * Checks if any player on the server has debug mode enabled.
     * Used for performance optimization: if no player has debug enabled, complex debug message construction and calculation can be skipped.
     *
     * @return 如果至少有一名玩家开启调试则返回 true / true if at least one player has debug enabled
     */
    public static boolean hasAnyDebugEnabled() {
        return !DEBUG_PLAYERS.isEmpty();
    }
}