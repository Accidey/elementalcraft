// src/main/java/com/xulai/elementalcraft/command/DebugCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.util.DebugMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;

/**
 * DebugCommand
 *
 * 该命令用于为玩家切换 ElementalCraft 的属性调试模式。
 * 同时作为工具类，负责处理和发送详细的战斗调试日志。
 * 调试模式开启后，玩家可以看到额外的属性伤害计算信息。
 *
 * This command toggles ElementalCraft's debug mode for the executing player.
 * Also acts as a utility class to handle and send detailed combat debug logs.
 * When enabled, additional elemental damage calculation details will be shown.
 */
public class DebugCommand {

    /**
     * 注册 /elementalcraft debug 命令
     * - 命令路径：/elementalcraft debug
     * - 权限等级：0（所有玩家可用）
     * - 仅允许玩家实体执行
     *
     * Register /elementalcraft debug command
     * - Command path: /elementalcraft debug
     * - Permission level: 0 (all players)
     * - Player-only execution
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("elementalcraft")
                // 检查权限：允许所有玩家使用（权限等级 0）
                // Check permission: Allow all players to use (permission level 0)
                .requires(source -> source.hasPermission(0))

                .then(
                    Commands.literal("debug")
                        .executes(context -> {

                            CommandSourceStack source = context.getSource();

                            // 确保命令只能由玩家执行
                            // Ensure the command is executed by a player
                            if (!(source.getEntity() instanceof Player player)) {
                                source.sendFailure(
                                    Component.translatable("command.elementalcraft.only_players")
                                );
                                return 0;
                            }

                            // 记录切换前的调试状态
                            // Store previous debug state
                            boolean wasEnabled = DebugMode.isEnabled(player);

                            // 切换当前玩家的调试模式状态
                            // Toggle the debug mode status for this player
                            DebugMode.toggle(player);

                            // 获取切换后的调试状态
                            // Get the current debug state after toggling
                            boolean nowEnabled = DebugMode.isEnabled(player);

                            // 仅在“关闭 → 开启”时发送提示
                            // Send enable message only when toggled on
                            if (!wasEnabled && nowEnabled) {

                                // 使用语言文件的“开启提示”
                                // Use localized "enabled" message
                                player.displayClientMessage(
                                    Component.translatable("command.elementalcraft.debug.enabled")
                                        .withStyle(style -> style.withColor(0x55FF55)),
                                    false
                                );

                                // 使用语言文件的调试模式说明提示
                                // Use localized debug mode description
                                player.displayClientMessage(
                                    Component.translatable("command.elementalcraft.debug.global_notice")
                                        .withStyle(style -> style.withBold(true)),
                                    false
                                );

                            }
                            // 仅在“开启 → 关闭”时发送提示
                            // Send disable message only when toggled off
                            else if (wasEnabled && !nowEnabled) {

                                player.displayClientMessage(
                                    Component.translatable("command.elementalcraft.debug.disabled")
                                        .withStyle(style -> style.withColor(0xFF5555)),
                                    false
                                );
                            }

                            // 命令成功执行
                            // Command executed successfully
                            return 1;
                        })
                )
        );
    }

    /**
     * 发送战斗调试日志。
     * 被 CombatEvents 调用，用于将详细的伤害计算过程广播给开启调试模式的管理员。
     * 包含了物理伤害和克制低保机制的详细数据。
     *
     * Sends combat debug log.
     * Called by CombatEvents to broadcast detailed damage calculation processes to admins with debug mode enabled.
     * Includes detailed data on physical damage and the restraint floor mechanism.
     *
     * @param attacker          攻击者 / Attacker
     * @param target            受击者 / Target
     * @param directEntity      直接伤害来源（如三叉戟） / Direct damage source (e.g. Trident)
     * @param physicalDamage    原始物理伤害 / Original physical damage
     * @param preResistDmg      抗性前元素伤害 / Elemental damage before resistance
     * @param resistReduction   抗性减免量 / Resistance reduction amount
     * @param restraintMultiplier 克制倍率 / Restraint multiplier
     * @param finalElementalDmg 最终元素伤害 / Final elemental damage
     * @param totalDamage       总伤害（物理+元素） / Total damage (Physical + Elemental)
     * @param isFloored         是否触发了低保机制 / Whether the floor mechanism was triggered
     * @param minPercent        低保百分比 / Floor percentage
     */
    public static void sendCombatLog(LivingEntity attacker, LivingEntity target, Entity directEntity,
                                     float physicalDamage, float preResistDmg, float resistReduction,
                                     float restraintMultiplier, float finalElementalDmg, float totalDamage,
                                     boolean isFloored, double minPercent) {

        // 检查是否有玩家开启了调试模式，如果没有则直接返回
        // Check if any player has debug mode enabled, return immediately if not
        if (!DebugMode.hasAnyDebugEnabled()) return;

        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        // 获取克制状态文本
        // Get restraint status text
        Component status = Component.translatable(
            restraintMultiplier > 1.0f ? "debug.elementalcraft.restraint" :
            restraintMultiplier < 1.0f ? "debug.elementalcraft.weakness" :
                                        "debug.elementalcraft.neutral");

        // 确定显示的攻击来源名称（如果是三叉戟则显示 Trident，否则显示攻击者名字）
        // Determine displayed source name (show Trident if it's a trident, otherwise attacker's name)
        String sourceName = (directEntity instanceof ThrownTrident) ? "Trident" : attacker.getDisplayName().getString();

        // 构建基础日志信息（使用 MutableComponent 以便后续追加）
        // Build base log message (use MutableComponent for appending later)
        // 这里的参数对应 zh_cn.json 中的 %1$s 到 %8$s
        MutableComponent logBuilder = Component.translatable("debug.elementalcraft.damage_log",
            sourceName,                                 // 1. 来源 / Source
            status,                                     // 2. 克制状态 / Restraint status
            String.format("%.2f", preResistDmg),        // 3. 基础元素伤 / Base elemental damage
            String.format("%.2f", resistReduction),     // 4. 抗性减免 / Resistance reduction
            String.format("%.2f", restraintMultiplier), // 5. 克制倍率 / Restraint multiplier
            String.format("%.2f", finalElementalDmg),   // 6. 最终元素伤 / Final elemental damage
            String.format("%.2f", physicalDamage),      // 7. 物理伤 / Physical damage
            String.format("%.2f", totalDamage)          // 8. 总伤害 / Total damage
        );

        // 如果触发了克制低保机制，追加 MinLimit 提示
        // If the restraint floor mechanism is triggered, append MinLimit notice
        if (isFloored) {
            logBuilder.append(Component.translatable("debug.elementalcraft.floor_trigger", String.format("%.0f", minPercent * 100)));
        }

        // 将 MutableComponent 赋值给一个 final 变量，供 Lambda 表达式使用
        // Assign MutableComponent to a final variable for use in Lambda expressions
        // 
        final Component finalLogMessage = logBuilder;

        // 输出到控制台日志
        // Output to console log
        ElementalCraft.LOGGER.info("[ElementalCraft Debug] " + stripColor(finalLogMessage.getString()));

        // 发送给开启调试模式的玩家
        // Send to players with debug mode enabled
        serverLevel.getServer().getPlayerList().getPlayers().stream()
            .filter(DebugMode::isEnabled)
            .forEach(p -> p.displayClientMessage(Component.literal("§e[属性调试] ").append(finalLogMessage), false));
    }

    /**
     * 去除颜色代码，用于控制台日志输出。
     *
     * Strips color codes for console logging.
     *
     * @param s 输入字符串 / Input string
     * @return 无颜色代码的字符串 / String without color codes
     */
    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }
}