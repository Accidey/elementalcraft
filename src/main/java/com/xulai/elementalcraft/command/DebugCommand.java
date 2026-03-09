package com.xulai.elementalcraft.command;

import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import java.util.Random;

import com.mojang.brigadier.CommandDispatcher;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
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
        String sourceName = (directEntity instanceof ThrownTrident) ? "Trident" : attacker.getDisplayName().getString();
        String relationKey = restraintMult > 1.0f ? "debug.elementalcraft.relation.restrain" :
                restraintMult < 1.0f ? "debug.elementalcraft.relation.weak" :
                        "debug.elementalcraft.relation.neutral";
        MutableComponent prefix = Component.translatable(relationKey, sourceName, target.getDisplayName().getString());
        if (restraintMult > 1.0f) prefix.withStyle(ChatFormatting.RED);
        else if (restraintMult < 1.0f) prefix.withStyle(ChatFormatting.BLUE);
        else prefix.withStyle(ChatFormatting.GRAY);
        MutableComponent overview = Component.translatable("debug.elementalcraft.damage_overview",
                String.format("%.2f", totalDamage),
                String.format("%.2f", physicalDamage),
                String.format("%.2f", finalElemDmg)
        ).withStyle(ChatFormatting.WHITE);
        MutableComponent formula = Component.literal(" (");
        formula.append(Component.translatable("debug.elementalcraft.formula.enhance", String.format("%.2f", rawElemDmg)).withStyle(ChatFormatting.GOLD));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.dmg_mult", String.format("%.2f", globalDmgMult)).withStyle(ChatFormatting.GRAY));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.restraint", String.format("%.2f", restraintMult)).withStyle(ChatFormatting.LIGHT_PURPLE));
        double reductionPerLevel = ElementalFireNatureReactionsConfig.wetnessFireReduction;
        double selfDryingPenalty = ElementalFireNatureReactionsConfig.wetnessSelfDryingDamagePenalty;
        double selfDryingFactor = 1.0 - selfDryingPenalty;
        double calculatedBaseWetnessMult = 1.0;
        boolean isSelfDrying = false;
        if (wetnessLevel > 0) {
            double fireBase = Math.max(0.0, 1.0 - (wetnessLevel * reductionPerLevel));
            if (Math.abs(wetnessMult - (fireBase * selfDryingFactor)) < 0.01) {
                calculatedBaseWetnessMult = fireBase;
                isSelfDrying = true;
            } else if (Math.abs(wetnessMult - fireBase) < 0.01) {
                calculatedBaseWetnessMult = fireBase;
            } else if (wetnessMult != 1.0f) {
                calculatedBaseWetnessMult = wetnessMult;
            }
        } else {
            if (Math.abs(wetnessMult - selfDryingFactor) < 0.01) {
                isSelfDrying = true;
            }
        }
        if (wetnessLevel > 0) {
             if (calculatedBaseWetnessMult != 1.0f) {
                 formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.wetness", wetnessLevel, String.format("%.2f", calculatedBaseWetnessMult)).withStyle(ChatFormatting.AQUA));
             } else {
                 formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.wetness_none", wetnessLevel).withStyle(ChatFormatting.DARK_GRAY));
             }
        }
        if (isSelfDrying) {
             formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.self_drying", String.format("%.2f", selfDryingFactor)).withStyle(ChatFormatting.RED));
        }
        formula.append(" - ");
        formula.append(Component.translatable("debug.elementalcraft.formula.resist", String.format("%.2f", rawResistReduct)).withStyle(ChatFormatting.BLUE));
        formula.append(" x ").append(Component.translatable("debug.elementalcraft.formula.res_mult", String.format("%.2f", globalResistMult)).withStyle(ChatFormatting.GRAY));
        if (isFloored) {
            formula.append(" + ").append(Component.translatable("debug.elementalcraft.formula.floor", String.format("%.0f", minPercent * 100)).withStyle(ChatFormatting.RED));
        }
        formula.append(")");
        MutableComponent fullMessage = Component.literal("").append(prefix).append(" ").append(overview).append(formula);
        sendDebugMessage(attacker, fullMessage);
    }

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

    public static void sendSteamTriggerLog(LivingEntity attacker, boolean isHighHeat, int level) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.steam_trigger.header")
                .withStyle(ChatFormatting.YELLOW);
        String typeKey = isHighHeat ? "debug.elementalcraft.steam_trigger.high" : "debug.elementalcraft.steam_trigger.low";
        ChatFormatting color = isHighHeat ? ChatFormatting.RED : ChatFormatting.AQUA;
        MutableComponent content = Component.translatable("debug.elementalcraft.steam_trigger.message",
                attacker.getDisplayName(),
                Component.translatable(typeKey).withStyle(color),
                level
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(attacker, prefix.append(" ").append(content));
    }

    public static void sendDryLog(LivingEntity entity, int oldLevel, int newLevel, int removedLayers, int firePower) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.drying.header")
                .withStyle(ChatFormatting.YELLOW);
        MutableComponent content = Component.translatable("debug.elementalcraft.drying.message",
                entity.getDisplayName(),
                Component.literal(String.valueOf(oldLevel)).withStyle(ChatFormatting.GOLD),
                Component.literal(String.valueOf(newLevel)).withStyle(ChatFormatting.GREEN),
                removedLayers,
                firePower
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(entity, prefix.append(" ").append(content));
    }

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

    public static void sendScorchedSporeReactionLog(LivingEntity target, LivingEntity applier, int stacks) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.scorched_spore.header")
                .withStyle(ChatFormatting.DARK_RED);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.scorched_spore.message",
                target.getDisplayName(),
                applier.getDisplayName(),
                Component.literal(String.valueOf(stacks)).withStyle(ChatFormatting.DARK_GREEN)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(target, prefix.append(" ").append(content));
    }

    public static void sendParalysisLog(LivingEntity attacker, LivingEntity target, int paralysisStacks, int remainingHits, float totalDamage) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.paralysis.header")
                .withStyle(ChatFormatting.DARK_PURPLE);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.paralysis.message",
                attacker != null ? attacker.getDisplayName() : Component.literal("环境触发"),
                target.getDisplayName(),
                Component.literal(String.valueOf(paralysisStacks)).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(String.valueOf(remainingHits)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.format("%.2f", totalDamage)).withStyle(ChatFormatting.RED)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(target, prefix.append(" ").append(content));
    }

    public static void sendDebugMessage(LivingEntity contextEntity, Component message) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        if (!(contextEntity.level() instanceof ServerLevel serverLevel)) return;
        ElementalCraft.LOGGER.info("[EC Debug] " + message.getString());
        serverLevel.getServer().getPlayerList().getPlayers().stream()
                .filter(DebugMode::isEnabled)
                .forEach(p -> p.displayClientMessage(message, false));
    }
}