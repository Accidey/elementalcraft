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
 *
 * 中文说明：
 * 调试命令与日志工具类。
 * 负责格式化并广播详细的战斗伤害公式和饱食度消耗信息。
 * sendDebugMessage 现在接受 Component 类型以支持本地化。
 *
 * English description:
 * Debug command and log utility class.
 * Responsible for formatting and broadcasting detailed combat damage formulas and exhaustion consumption info.
 * sendDebugMessage now accepts Component type to support localization.
 */
public class DebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("elementalcraft")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("debug")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        if (!(source.getEntity() instanceof Player player)) {
                            source.sendFailure(Component.translatable("command.elementalcraft.only_players"));
                            return 0;
                        }
                        boolean wasEnabled = DebugMode.isEnabled(player);
                        DebugMode.toggle(player);
                        boolean nowEnabled = DebugMode.isEnabled(player);

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
     *
     * Sends detailed combat damage calculation log.
     */
    public static void sendCombatLog(LivingEntity attacker, LivingEntity target, Entity directEntity,
                                     float physicalDamage, 
                                     float rawElemDmg, float rawResistReduct,
                                     double globalDmgMult, double globalResistMult,
                                     float restraintMult, float wetnessMult,
                                     float finalElemDmg, float totalDamage,
                                     boolean isFloored, double minPercent,
                                     int wetnessLevel) {

        if (!DebugMode.hasAnyDebugEnabled()) return;
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        // 1. 构建关系前缀 (Relation Prefix)
        String sourceName = (directEntity instanceof ThrownTrident) ? "Trident" : attacker.getDisplayName().getString();
        String relationKey = restraintMult > 1.0f ? "debug.elementalcraft.relation.restrain" :
                             restraintMult < 1.0f ? "debug.elementalcraft.relation.weak" :
                             "debug.elementalcraft.relation.neutral";
        
        MutableComponent prefix = Component.translatable(relationKey, sourceName, target.getDisplayName().getString());
        if (restraintMult > 1.0f) prefix.withStyle(ChatFormatting.RED);
        else if (restraintMult < 1.0f) prefix.withStyle(ChatFormatting.BLUE);
        else prefix.withStyle(ChatFormatting.GRAY);

        // 2. 构建伤害概览 (Damage Overview)
        MutableComponent overview = Component.translatable("debug.elementalcraft.damage_overview",
                String.format("%.2f", totalDamage),
                String.format("%.2f", physicalDamage),
                String.format("%.2f", finalElemDmg)
        ).withStyle(ChatFormatting.WHITE);

        // 3. 构建公式详情 (Formula Details)
        MutableComponent formula = Component.literal(" (");

        // 3.1 攻击部分 (Attack Part)
        formula.append(Component.translatable("debug.elementalcraft.formula.enhance", String.format("%.2f", rawElemDmg)).withStyle(ChatFormatting.GOLD));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.dmg_mult", String.format("%.2f", globalDmgMult)).withStyle(ChatFormatting.GRAY));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.restraint", String.format("%.2f", restraintMult)).withStyle(ChatFormatting.LIGHT_PURPLE));
        
        // 潮湿修正显示 (Wetness)
        if (wetnessLevel > 0) {
            if (wetnessMult != 1.0f) {
                formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.wetness", wetnessLevel, String.format("%.2f", wetnessMult)).withStyle(ChatFormatting.AQUA));
            } else {
                formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.wetness_none", wetnessLevel).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        formula.append(" - ");

        // 3.2 防御部分 (Defense Part)
        formula.append(Component.translatable("debug.elementalcraft.formula.resist", String.format("%.2f", rawResistReduct)).withStyle(ChatFormatting.BLUE));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.res_mult", String.format("%.2f", globalResistMult)).withStyle(ChatFormatting.GRAY));

        // 3.3 保底提示 (Floor)
        if (isFloored) {
            formula.append(" + ").append(Component.translatable("debug.elementalcraft.formula.floor", String.format("%.0f", minPercent * 100)).withStyle(ChatFormatting.RED));
        }

        formula.append(")");

        // 4. 组合并发送
        MutableComponent fullMessage = Component.literal("").append(prefix).append(" ").append(overview).append(formula);

        ElementalCraft.LOGGER.info("[EC Debug] " + fullMessage.getString());

        serverLevel.getServer().getPlayerList().getPlayers().stream()
            .filter(DebugMode::isEnabled)
            .forEach(p -> p.displayClientMessage(fullMessage, false));
    }

    /**
     * 发送饱食度消耗调试日志。
     *
     * Sends exhaustion consumption debug log.
     */
    public static void sendExhaustionLog(Player player, float baseDelta, float wetnessDelta, int wetnessLevel) {
        if (!DebugMode.isEnabled(player)) return;

        float total = baseDelta + wetnessDelta;
        
        MutableComponent msg = Component.translatable("debug.elementalcraft.exhaustion.header", String.format("%.2f", total))
                .withStyle(ChatFormatting.GOLD);
        
        msg.append(Component.literal(": "));
        msg.append(Component.translatable("debug.elementalcraft.exhaustion.base", String.format("%.2f", baseDelta)).withStyle(ChatFormatting.WHITE));

        if (wetnessLevel > 0 && wetnessDelta > 0) {
            msg.append(" + ");
            msg.append(Component.translatable("debug.elementalcraft.exhaustion.wetness", 
                    String.format("%.2f", wetnessDelta), 
                    wetnessLevel
            ).withStyle(ChatFormatting.AQUA));
        }

        player.displayClientMessage(msg, true);
    }

    /**
     * 发送通用调试消息。
     * 接受 Component 类型，支持本地化。
     *
     * Sends generic debug message.
     * Accepts Component type to support localization.
     *
     * @param contextEntity 上下文实体 (用于获取 ServerLevel) / Context entity (to get ServerLevel)
     * @param message 消息内容 / Message content
     */
    public static void sendDebugMessage(LivingEntity contextEntity, Component message) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        if (!(contextEntity.level() instanceof ServerLevel serverLevel)) return;

        // 记录到后台控制台
        ElementalCraft.LOGGER.info("[EC Debug] " + message.getString());

        // 发送给开启调试模式的玩家
        serverLevel.getServer().getPlayerList().getPlayers().stream()
                .filter(DebugMode::isEnabled)
                .forEach(p -> p.displayClientMessage(message, false));
    }
}