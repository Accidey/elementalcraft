// src/main/java/com/xulai/elementalcraft/command/DebugCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
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
 * 同时提供静态工具方法，用于格式化并广播详细的战斗伤害计算公式、饱食度消耗信息、
 * 以及各类元素反应（如寄生吸取、毒火爆燃、野火喷射）的触发日志。
 * <p>
 * English Description:
 * Debug command and log utility class.
 * Responsible for registering the /elementalcraft debug command, allowing players to toggle debug mode.
 * Also provides static utility methods to format and broadcast detailed combat damage formulas, exhaustion consumption info,
 * and trigger logs for various elemental reactions (e.g., Parasitic Drain, Toxic Blast, Wildfire Ejection).
 */
public class DebugCommand {

    /**
     * 注册调试命令到调度器。
     * 允许拥有权限的玩家使用 /elementalcraft debug 切换调试模式。
     * <p>
     * Registers the debug command to the dispatcher.
     * Allows players with permission to toggle debug mode using /elementalcraft debug.
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
     * 如果开启了调试模式，此方法会向玩家聊天栏发送格式化的计算过程。
     * <p>
     * Sends detailed combat damage calculation log.
     * Includes detailed formulas for physical damage, elemental damage, resistance reduction, restraint coefficients, etc.
     * If debug mode is enabled, this method sends the formatted calculation process to the player's chat.
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
        // Check if any debug mode is enabled
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

        // 3.1 攻击部分 (基础伤害 x 倍率 x 克制 x 潮湿 x 自我干燥)
        // 3.1 Attack Part (Base x Multiplier x Restraint x Wetness x Self-Drying)
        formula.append(Component.translatable("debug.elementalcraft.formula.enhance", String.format("%.2f", rawElemDmg)).withStyle(ChatFormatting.GOLD));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.dmg_mult", String.format("%.2f", globalDmgMult)).withStyle(ChatFormatting.GRAY));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.restraint", String.format("%.2f", restraintMult)).withStyle(ChatFormatting.LIGHT_PURPLE));

        // --- 智能拆分潮湿与自我干燥逻辑 (Smart Split Logic) ---
        // 分析当前的 wetnessMult 是由什么组成的（纯潮湿减伤、自我干燥惩罚，还是两者叠加）
        // Analyze what constitutes the current wetnessMult (pure wetness reduction, self-drying penalty, or both)
        double reductionPerLevel = ElementalReactionConfig.wetnessFireReduction;
        double selfDryingPenalty = ElementalReactionConfig.wetnessSelfDryingDamagePenalty;
        double selfDryingFactor = 1.0 - selfDryingPenalty;

        double calculatedBaseWetnessMult = 1.0;
        boolean isSelfDrying = false;

        if (wetnessLevel > 0) {
            double fireBase = Math.max(0.0, 1.0 - (wetnessLevel * reductionPerLevel));
            
            // 判定：实际倍率匹配 (FireBase * SelfDrying) -> 潮湿+自我干燥
            // Check: Actual multiplier matches (FireBase * SelfDrying) -> Wetness + Self-Drying
            if (Math.abs(wetnessMult - (fireBase * selfDryingFactor)) < 0.01) {
                calculatedBaseWetnessMult = fireBase;
                isSelfDrying = true;
            } 
            // 判定：匹配 FireBase -> 仅潮湿减伤
            // Check: Matches FireBase -> Only Wetness Reduction
            else if (Math.abs(wetnessMult - fireBase) < 0.01) {
                calculatedBaseWetnessMult = fireBase;
            }
            // 兜底逻辑：如果反推失败但确实有数值变化，直接使用实际传入的 wetnessMult
            // Fallback: If inference fails but value changed, use actual wetnessMult
            else if (wetnessMult != 1.0f) {
                calculatedBaseWetnessMult = wetnessMult;
            }
        } else {
            // 0 层潮湿，但如果 wetnessMult 不为 1，说明是自我干燥残留
            // 0 Wetness Level, but if wetnessMult is not 1, it indicates residual Self-Drying
            if (Math.abs(wetnessMult - selfDryingFactor) < 0.01) {
                isSelfDrying = true;
            }
        }

        // 显示基础潮湿修正
        // Display base wetness modification
        if (wetnessLevel > 0) {
             if (calculatedBaseWetnessMult != 1.0f) {
                 formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.wetness", wetnessLevel, String.format("%.2f", calculatedBaseWetnessMult)).withStyle(ChatFormatting.AQUA));
             } else {
                 formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.wetness_none", wetnessLevel).withStyle(ChatFormatting.DARK_GRAY));
             }
        }

        // 显示自我干燥修正
        // Display self-drying modification
        if (isSelfDrying) {
             formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.self_drying", String.format("%.2f", selfDryingFactor)).withStyle(ChatFormatting.RED));
        }
        // ----------------------------------------------------

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

        // 复用 sendDebugMessage 逻辑
        // Reuse sendDebugMessage logic
        sendDebugMessage(attacker, fullMessage);
    }

    /**
     * 发送寄生吸取调试日志。
     * 显示吸取的层数和回复的生命值。
     * <p>
     * Sends Parasitic Drain debug log.
     * Shows drained layers and healed amount.
     */
    public static void sendNatureSiphonLog(LivingEntity attacker, LivingEntity target, int drainedLayers, float healedAmount) {
        if (!DebugMode.hasAnyDebugEnabled()) return;

        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.nature_siphon.header")
                .withStyle(ChatFormatting.GREEN);

        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.nature_siphon.message",
                attacker.getDisplayName(),
                target.getDisplayName(),
                Component.literal(String.valueOf(drainedLayers)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.format("%.1f", healedAmount)).withStyle(ChatFormatting.RED)
        ).withStyle(ChatFormatting.WHITE);

        sendDebugMessage(attacker, prefix.append(" ").append(content));
    }

    /**
     * 发送毒火爆燃调试日志。
     * 显示引爆的孢子层数、爆炸半径及影响实体数。
     * <p>
     * Sends Toxic Blast debug log.
     * Shows detonated spore stacks, explosion radius, and affected entity count.
     */
    public static void sendToxicBlastLog(LivingEntity attacker, LivingEntity target, int stacks, double radius, int affectedCount) {
        if (!DebugMode.hasAnyDebugEnabled()) return;

        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.toxic_blast.header")
                .withStyle(ChatFormatting.RED);

        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.toxic_blast.message",
                attacker.getDisplayName(),
                target.getDisplayName(),
                Component.literal(String.valueOf(stacks)).withStyle(ChatFormatting.DARK_GREEN),
                String.format("%.1f", radius),
                affectedCount
        ).withStyle(ChatFormatting.WHITE);

        sendDebugMessage(attacker, prefix.append(" ").append(content));
    }

    /**
     * 发送野火喷射调试日志。
     * 显示反击半径和击退的敌人数量。
     * <p>
     * Sends Wildfire Ejection debug log.
     * Shows rejection radius and number of enemies knocked back.
     */
    public static void sendWildfireLog(LivingEntity victim, double radius, int affectedCount) {
        if (!DebugMode.hasAnyDebugEnabled()) return;

        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.wildfire.header")
                .withStyle(ChatFormatting.GOLD);

        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.wildfire.message",
                victim.getDisplayName(),
                String.format("%.1f", radius),
                affectedCount
        ).withStyle(ChatFormatting.WHITE);

        sendDebugMessage(victim, prefix.append(" ").append(content));
    }

    /**
     * 发送蒸汽触发调试日志。
     * 显示触发的蒸汽类型（高温/低温）和层数。
     * <p>
     * Sends steam trigger debug log.
     * Shows triggered steam type (High/Low) and level.
     */
    public static void sendSteamTriggerLog(LivingEntity attacker, boolean isHighHeat, int level) {
        // 检查是否有 Debug 开启
        // Check if Debug is enabled
        if (!DebugMode.hasAnyDebugEnabled()) return;

        // 构建前缀
        // Build prefix
        MutableComponent prefix = Component.translatable("debug.elementalcraft.steam_trigger.header")
                .withStyle(ChatFormatting.YELLOW);

        // 类型文本键值 (高温/低温)
        // Type Key (High/Low)
        String typeKey = isHighHeat ? "debug.elementalcraft.steam_trigger.high" : "debug.elementalcraft.steam_trigger.low";
        ChatFormatting color = isHighHeat ? ChatFormatting.RED : ChatFormatting.AQUA;

        // 构建内容
        // Build content
        MutableComponent content = Component.translatable("debug.elementalcraft.steam_trigger.message",
                attacker.getDisplayName(), // Arg 0: Name
                Component.translatable(typeKey).withStyle(color), // Arg 1: Type (High/Low)
                level // Arg 2: Level
        ).withStyle(ChatFormatting.WHITE);

        // 组合并广播
        // Combine and broadcast
        sendDebugMessage(attacker, prefix.append(" ").append(content));
    }

    /**
     * 发送自我干燥调试日志。
     * 显示潮湿层数的变化、移除层数以及当前的赤焰强度。
     * <p>
     * Sends self-drying debug log.
     * Shows the change in wetness levels, layers removed, and current fire power.
     */
    public static void sendDryLog(LivingEntity entity, int oldLevel, int newLevel, int removedLayers, int firePower) {
        // 检查是否有 Debug 开启
        // Check if Debug is enabled
        if (!DebugMode.hasAnyDebugEnabled()) return;

        // 构建前缀
        // Build prefix
        MutableComponent prefix = Component.translatable("debug.elementalcraft.drying.header")
                .withStyle(ChatFormatting.YELLOW);

        // 构建内容
        // Build content
        MutableComponent content = Component.translatable("debug.elementalcraft.drying.message",
                entity.getDisplayName(), // Arg 0
                Component.literal(String.valueOf(oldLevel)).withStyle(ChatFormatting.GOLD), // Arg 1
                Component.literal(String.valueOf(newLevel)).withStyle(ChatFormatting.GREEN), // Arg 2
                removedLayers, // Arg 3
                firePower      // Arg 4
        ).withStyle(ChatFormatting.WHITE);

        // 组合并广播
        // Combine and broadcast
        sendDebugMessage(entity, prefix.append(" ").append(content));
    }

    /**
     * 发送饱食度消耗调试日志 (Action Bar)。
     * 显示基础消耗和因潮湿状态增加的额外消耗。
     * <p>
     * Sends exhaustion consumption debug log (Action Bar).
     * Shows base consumption and additional consumption due to wetness.
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
     * 接受 Component 类型，支持本地化。消息会发送给所有开启调试模式的玩家，并在服务端控制台打印。
     * <p>
     * Sends generic debug message.
     * Accepts Component type to support localization. Messages are sent to all players with debug mode enabled and printed to the server console.
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