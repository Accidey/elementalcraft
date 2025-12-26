// src/main/java/com/xulai/elementalcraft/command/DebugCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.util.DebugMode;
import net.minecraft.ChatFormatting;
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
 * <p>
 * 中文说明：
 * 调试命令与日志工具类。
 * 负责注册 /elementalcraft debug 命令，允许玩家开启或关闭调试模式。
 * 同时提供静态工具方法，用于格式化并广播详细的战斗伤害计算公式、饱食度消耗信息以及通用调试信息。
 * <p>
 * English Description:
 * Debug command and log utility class.
 * Responsible for registering the /elementalcraft debug command, allowing players to toggle debug mode.
 * Also provides static utility methods to format and broadcast detailed combat damage formulas, exhaustion consumption info, and generic debug messages.
 */
public class DebugCommand {

    /**
     * 注册调试命令到调度器。
     * <p>
     * Registers the debug command to the dispatcher.
     *
     * @param dispatcher 命令调度器 / Command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("elementalcraft")
                        // 权限检查：允许所有玩家使用（权限等级 0）
                        // Permission check: Allow all players to use (Permission level 0)
                        .requires(source -> source.hasPermission(0))
                        .then(Commands.literal("debug")
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    // 确保执行者是玩家
                                    // Ensure the executor is a player
                                    if (!(source.getEntity() instanceof Player player)) {
                                        source.sendFailure(Component.translatable("command.elementalcraft.only_players"));
                                        return 0;
                                    }

                                    // 切换玩家的调试模式状态
                                    // Toggle the player's debug mode state
                                    boolean wasEnabled = DebugMode.isEnabled(player);
                                    DebugMode.toggle(player);
                                    boolean nowEnabled = DebugMode.isEnabled(player);

                                    // 发送状态变更反馈
                                    // Send status change feedback
                                    if (!wasEnabled && nowEnabled) {
                                        player.displayClientMessage(Component.translatable("command.elementalcraft.debug.enabled").withStyle(ChatFormatting.GREEN), false);
                                        player.displayClientMessage(Component.translatable("command.elementalcraft.debug.global_notice").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), false);
                                    } else if (wasEnabled && !nowEnabled) {
                                        player.displayClientMessage(Component.translatable("command.elementalcraft.debug.disabled").withStyle(ChatFormatting.RED), false);
                                    }
                                    return 1;
                                })
                        )
        );
    }

    /**
     * 发送详细的战斗伤害计算日志。
     * 包含物理伤害、元素伤害、抗性减免、克制系数等详细公式。
     * <p>
     * Sends detailed combat damage calculation log.
     * Includes detailed formulas for physical damage, elemental damage, resistance reduction, restraint coefficients, etc.
     *
     * @param attacker         攻击者 / Attacker
     * @param target           受击者 / Target
     * @param directEntity     直接造成伤害的实体（如三叉戟） / Direct entity causing damage (e.g., Trident)
     * @param physicalDamage   物理伤害部分 / Physical damage part
     * @param rawElemDmg       原始元素伤害 / Raw elemental damage
     * @param rawResistReduct  原始抗性减免 / Raw resistance reduction
     * @param globalDmgMult    全局伤害倍率 / Global damage multiplier
     * @param globalResistMult 全局抗性倍率 / Global resistance multiplier
     * @param restraintMult    克制系数 / Restraint multiplier
     * @param wetnessMult      潮湿易伤系数 / Wetness vulnerability multiplier
     * @param finalElemDmg     最终元素伤害 / Final elemental damage
     * @param totalDamage      总伤害 / Total damage
     * @param isFloored        是否触发保底伤害 / Whether minimum damage floor was triggered
     * @param minPercent       保底百分比 / Minimum damage percentage
     * @param wetnessLevel     潮湿等级 / Wetness level
     */
    public static void sendCombatLog(LivingEntity attacker, LivingEntity target, Entity directEntity,
                                     float physicalDamage,
                                     float rawElemDmg, float rawResistReduct,
                                     double globalDmgMult, double globalResistMult,
                                     float restraintMult, float wetnessMult,
                                     float finalElemDmg, float totalDamage,
                                     boolean isFloored, double minPercent,
                                     int wetnessLevel) {

        // 检查是否有任何调试模式开启（如果没有人开启，则跳过构建消息以节省性能）
        // Check if any debug mode is enabled (skip message building to save performance if no one is debugging)
        if (!DebugMode.hasAnyDebugEnabled()) return;
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        // 1. 构建关系前缀 (克制/抵抗/中立)
        // 1. Build relation prefix (Restrain/Weak/Neutral)
        String sourceName = (directEntity instanceof ThrownTrident) ? "Trident" : attacker.getDisplayName().getString();
        String relationKey = restraintMult > 1.0f ? "debug.elementalcraft.relation.restrain" :
                restraintMult < 1.0f ? "debug.elementalcraft.relation.weak" :
                        "debug.elementalcraft.relation.neutral";

        MutableComponent prefix = Component.translatable(relationKey, sourceName, target.getDisplayName().getString());
        if (restraintMult > 1.0f) prefix.withStyle(ChatFormatting.RED);
        else if (restraintMult < 1.0f) prefix.withStyle(ChatFormatting.BLUE);
        else prefix.withStyle(ChatFormatting.GRAY);

        // 2. 构建伤害概览 (总伤害/物理/元素)
        // 2. Build damage overview (Total/Physical/Elemental)
        MutableComponent overview = Component.translatable("debug.elementalcraft.damage_overview",
                String.format("%.2f", totalDamage),
                String.format("%.2f", physicalDamage),
                String.format("%.2f", finalElemDmg)
        ).withStyle(ChatFormatting.WHITE);

        // 3. 构建公式详情
        // 3. Build formula details
        MutableComponent formula = Component.literal(" (");

        // 3.1 攻击部分 (基础伤害 x 倍率 x 克制 x 潮湿)
        // 3.1 Attack Part (Base x Multiplier x Restraint x Wetness)
        formula.append(Component.translatable("debug.elementalcraft.formula.enhance", String.format("%.2f", rawElemDmg)).withStyle(ChatFormatting.GOLD));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.dmg_mult", String.format("%.2f", globalDmgMult)).withStyle(ChatFormatting.GRAY));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.restraint", String.format("%.2f", restraintMult)).withStyle(ChatFormatting.LIGHT_PURPLE));

        // 潮湿修正显示
        // Wetness modifier display
        if (wetnessLevel > 0) {
            if (wetnessMult != 1.0f) {
                formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.wetness", wetnessLevel, String.format("%.2f", wetnessMult)).withStyle(ChatFormatting.AQUA));
            } else {
                formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.wetness_none", wetnessLevel).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        formula.append(" - ");

        // 3.2 防御部分 (抗性 x 倍率)
        // 3.2 Defense Part (Resist x Multiplier)
        formula.append(Component.translatable("debug.elementalcraft.formula.resist", String.format("%.2f", rawResistReduct)).withStyle(ChatFormatting.BLUE));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.res_mult", String.format("%.2f", globalResistMult)).withStyle(ChatFormatting.GRAY));

        // 3.3 保底提示
        // 3.3 Floor hint
        if (isFloored) {
            formula.append(" + ").append(Component.translatable("debug.elementalcraft.formula.floor", String.format("%.0f", minPercent * 100)).withStyle(ChatFormatting.RED));
        }

        formula.append(")");

        // 4. 组合并发送消息
        // 4. Assemble and send message
        MutableComponent fullMessage = Component.literal("").append(prefix).append(" ").append(overview).append(formula);

        // 记录到服务器控制台
        // Log to server console
        ElementalCraft.LOGGER.info("[EC Debug] " + fullMessage.getString());

        // 发送给所有开启调试模式的玩家
        // Send to all players with debug mode enabled
        serverLevel.getServer().getPlayerList().getPlayers().stream()
                .filter(DebugMode::isEnabled)
                .forEach(p -> p.displayClientMessage(fullMessage, false));
    }

    /**
     * 发送饱食度消耗调试日志。
     * 显示基础消耗和因潮湿状态增加的额外消耗。
     * <p>
     * Sends exhaustion consumption debug log.
     * Shows base consumption and additional consumption due to wetness.
     *
     * @param player       玩家 / Player
     * @param baseDelta    基础消耗量 / Base consumption amount
     * @param wetnessDelta 潮湿额外消耗量 / Additional wetness consumption
     * @param wetnessLevel 潮湿等级 / Wetness level
     */
    public static void sendExhaustionLog(Player player, float baseDelta, float wetnessDelta, int wetnessLevel) {
        if (!DebugMode.isEnabled(player)) return;

        float total = baseDelta + wetnessDelta;

        // 构建消耗信息头
        // Build consumption header
        MutableComponent msg = Component.translatable("debug.elementalcraft.exhaustion.header", String.format("%.2f", total))
                .withStyle(ChatFormatting.GOLD);

        msg.append(Component.literal(": "));
        msg.append(Component.translatable("debug.elementalcraft.exhaustion.base", String.format("%.2f", baseDelta)).withStyle(ChatFormatting.WHITE));

        // 如果有潮湿消耗，追加显示
        // If there is wetness consumption, append display
        if (wetnessLevel > 0 && wetnessDelta > 0) {
            msg.append(" + ");
            msg.append(Component.translatable("debug.elementalcraft.exhaustion.wetness",
                    String.format("%.2f", wetnessDelta),
                    wetnessLevel
            ).withStyle(ChatFormatting.AQUA));
        }

        // 发送到玩家动作栏 (ActionBar)
        // Send to player Action Bar
        player.displayClientMessage(msg, true);
    }

    /**
     * 发送通用调试消息。
     * 接受 Component 类型，支持本地化。
     * <p>
     * Sends generic debug message.
     * Accepts Component type to support localization.
     *
     * @param contextEntity 上下文实体 (用于获取 ServerLevel) / Context entity (to get ServerLevel)
     * @param message       消息内容 / Message content
     */
    public static void sendDebugMessage(LivingEntity contextEntity, Component message) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        if (!(contextEntity.level() instanceof ServerLevel serverLevel)) return;

        // 记录到后台控制台
        // Log to server console
        ElementalCraft.LOGGER.info("[EC Debug] " + message.getString());

        // 发送给开启调试模式的玩家
        // Send to players with debug mode enabled
        serverLevel.getServer().getPlayerList().getPlayers().stream()
                .filter(DebugMode::isEnabled)
                .forEach(p -> p.displayClientMessage(message, false));
    }
}