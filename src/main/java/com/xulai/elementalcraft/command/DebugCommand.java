package com.xulai.elementalcraft.command;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.xulai.elementalcraft.util.ConfigAutoSync;
import com.xulai.elementalcraft.util.DebugMode;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
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
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ConfigAutoSync.reloadAll();
                                    context.getSource().sendSuccess(() ->
                                            Component.translatable("command.elementalcraft.reload.success"), true);
                                    return 1;
                                })
                        )
        );
    }

    public static class CombatLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public Entity directEntity;
        public float originalPhysicalDamage;
        public float physicalDamage;
        public ElementType attackElement;
        public int attackerEnhancement;
        public int targetResistance;
        public float baseEnhancementDamage;
        public float globalDamageMult;
        public float restraintMult;
        public float sporeVulnMult;
        public float freezeVulnMult;
        public float scorchVulnMult;
        public float wetnessBaseMult;
        public float selfDryingPenaltyMult;
        public float combinedWetnessMult;
        public float baseResistReduction;
        public float globalResistMult;
        public float finalElemDmg;
        public float totalDamage;
        public boolean isFloored;
        public double minPercent;
        public int wetnessLevel;
        public float frozenMeltMult;
        public boolean frozenMelted;

        public MutableComponent buildFormulaComponent() {
            MutableComponent formula = Component.literal(" (");
            formula.append(Component.translatable("debug.elementalcraft.formula.base_enhance", String.format("%.2f", baseEnhancementDamage)).withStyle(ChatFormatting.GOLD));
            if (Math.abs(globalDamageMult - 1.0f) > 0.001f) {
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.global_dmg_mult", String.format("%.2f", globalDamageMult)).withStyle(ChatFormatting.GRAY));
            }
            if (Math.abs(restraintMult - 1.0f) > 0.001f) {
                ChatFormatting color = restraintMult > 1.0f ? ChatFormatting.RED : ChatFormatting.BLUE;
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.restraint", String.format("%.2f", restraintMult)).withStyle(color));
            }
            if (Math.abs(sporeVulnMult - 1.0f) > 0.001f) {
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.spore_vuln", String.format("%.2f", sporeVulnMult)).withStyle(ChatFormatting.DARK_GREEN));
            }
            if (Math.abs(freezeVulnMult - 1.0f) > 0.001f) {
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.freeze_vuln", String.format("%.2f", freezeVulnMult)).withStyle(ChatFormatting.AQUA));
            }
            if (Math.abs(scorchVulnMult - 1.0f) > 0.001f) {
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.scorch_vuln", String.format("%.2f", scorchVulnMult)).withStyle(ChatFormatting.DARK_RED));
            }
            if (Math.abs(frozenMeltMult - 1.0f) > 0.001f) {
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.frozen_melt", String.format("%.1f", frozenMeltMult)).withStyle(ChatFormatting.AQUA));
            }
            if (Math.abs(wetnessBaseMult - 1.0f) > 0.001f) {
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.wetness_base", String.format("%.2f", wetnessBaseMult)).withStyle(ChatFormatting.AQUA));
            }
            if (Math.abs(selfDryingPenaltyMult - 1.0f) > 0.001f) {
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.self_drying", String.format("%.2f", selfDryingPenaltyMult)).withStyle(ChatFormatting.RED));
            }
            formula.append(Component.literal(" - "));
            formula.append(Component.translatable("debug.elementalcraft.formula.resist", String.format("%.2f", baseResistReduction)).withStyle(ChatFormatting.BLUE));
            if (Math.abs(globalResistMult - 1.0f) > 0.001f) {
                formula.append(Component.literal(" × ")).append(Component.translatable("debug.elementalcraft.formula.res_mult", String.format("%.2f", globalResistMult)).withStyle(ChatFormatting.GRAY));
            }
            if (isFloored) {
                formula.append(Component.literal(" + ")).append(Component.translatable("debug.elementalcraft.formula.floor", String.format("%.0f", minPercent * 100)).withStyle(ChatFormatting.RED));
            }
            formula.append(Component.literal(")"));
            return formula;
        }

        public MutableComponent buildDetailsComponent() {
            MutableComponent details = Component.translatable("debug.elementalcraft.combat.details.header").withStyle(ChatFormatting.GOLD);
            details.append(Component.literal(" "));
            details.append(Component.translatable("debug.elementalcraft.combat.details.enhancement",
                    Component.translatable("element." + attackElement.name().toLowerCase()).withStyle(ChatFormatting.YELLOW),
                    attackerEnhancement).withStyle(ChatFormatting.WHITE));
            details.append(Component.literal(", "));
            details.append(Component.translatable("debug.elementalcraft.combat.details.resistance", targetResistance).withStyle(ChatFormatting.WHITE));
            if (wetnessLevel > 0) {
                details.append(Component.literal(", "));
                details.append(Component.translatable("debug.elementalcraft.combat.details.wetness", wetnessLevel).withStyle(ChatFormatting.AQUA));
            }
            return details;
        }
    }

    public static class ToxicBlastLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public int stacks;
        public double radius;
        public int affectedCount;
        public float rawBaseDamage;
    }

    public static class WildfireLogContext {
        public LivingEntity victim;
        public double radius;
        public int affectedCount;
    }

    public static class DryLogContext {
        public LivingEntity entity;
        public int oldLevel;
        public int newLevel;
        public int removedLayers;
        public int firePower;
    }

    public static class ExhaustionLogContext {
        public Player player;
        public float baseDelta;
        public float wetnessDelta;
        public int wetnessLevel;
    }

    public static class ScorchedSporeReactionLogContext {
        public LivingEntity target;
        public LivingEntity applier;
        public int stacks;
    }

    public static class ThunderCounterLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public double chance;
        public boolean success;
        public String appliedEffectKey;
        public int appliedStacks;
        public float lightningDamage;
    }

    public static class ParalysisLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public int paralysisStacks;
        public int remainingHits;
        public float totalDamage;
    }

    public static class WaterElectrificationLogContext {
        public LivingEntity source;
        public int stacks;
        public double range;
        public int affectedCount;
        public float settlementDamage;
        public int paralysisDuration;
    }

    public static class FrostbiteLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public int stacksApplied;
        public int totalStacks;
        public double chance;
    }

    public static class FreezeLogContext {
        public LivingEntity target;
        public int freezeDuration;
        public int frostbiteStacks;
        public int freezeStacks;
        public boolean fromWetness;
        public float damage;
    }

    public static class StaticSteamCloudLogContext {
        public LivingEntity source;
        public int triggerStacks;
        public float settlementDamage;
        public int cloudDuration;
    }

    public static class ContagionLogContext {
        public LivingEntity source;
        public int sourceStacks;
        public int transferStacks;
        public double radius;
        public int affectedCount;
    }

    public static class FrostedSteamCloudLogContext {
        public LivingEntity source;
        public int triggerStacks;
        public int cloudDuration;
        public int affectedCount;
    }

    public static class AuraDamageLogContext {
        public LivingEntity source;
        public LivingEntity target;
        public float damage;
        public String reactionKey;
    }

    public static class ThermalShockLogContext {
        public LivingEntity target;
        public int remainingTicks;
        public float totalRemainingDamage;
        public float ratio;
        public float shockDamage;
        public int steamLevel;
    }

    public static void sendCombatLog(CombatLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        if (!(ctx.target.level() instanceof ServerLevel serverLevel)) return;

        String sourceName = (ctx.directEntity instanceof ThrownTrident) ? "Trident" : ctx.attacker.getDisplayName().getString();
        String relationKey = ctx.restraintMult > 1.0f ? "debug.elementalcraft.relation.restrain" : ctx.restraintMult < 1.0f ? "debug.elementalcraft.relation.weak" : "debug.elementalcraft.relation.neutral";

        MutableComponent prefix = Component.translatable(relationKey, sourceName, ctx.target.getDisplayName().getString());
        if (ctx.restraintMult > 1.0f) prefix.withStyle(ChatFormatting.RED);
        else if (ctx.restraintMult < 1.0f) prefix.withStyle(ChatFormatting.BLUE);
        else prefix.withStyle(ChatFormatting.GRAY);

        MutableComponent overview = Component.translatable("debug.elementalcraft.damage_overview",
                String.format("%.2f", ctx.totalDamage),
                String.format("%.2f", ctx.physicalDamage),
                String.format("%.2f", ctx.finalElemDmg)
        ).withStyle(ChatFormatting.WHITE);

        MutableComponent fullMessage = Component.literal("")
                .append(prefix)
                .append(Component.literal(" "))
                .append(overview)
                .append(ctx.buildFormulaComponent());
        fullMessage.append(Component.literal("\n")).append(ctx.buildDetailsComponent());

        sendDebugMessage(ctx.attacker, fullMessage);
    }

    public static void sendToxicBlastLog(ToxicBlastLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.toxic_blast.header").withStyle(ChatFormatting.RED);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.toxic_blast.message",
                ctx.attacker.getDisplayName(),
                ctx.target.getDisplayName(),
                Component.literal(String.valueOf(ctx.stacks)).withStyle(ChatFormatting.DARK_GREEN),
                String.format("%.1f", ctx.radius),
                ctx.affectedCount,
                Component.literal(String.format("%.1f", ctx.rawBaseDamage)).withStyle(ChatFormatting.RED)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.attacker, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendWildfireLog(WildfireLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.wildfire.header").withStyle(ChatFormatting.GOLD);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.wildfire.message",
                ctx.victim.getDisplayName(),
                String.format("%.1f", ctx.radius),
                ctx.affectedCount
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.victim, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendDryLog(DryLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.drying.header").withStyle(ChatFormatting.YELLOW);
        MutableComponent content = Component.translatable("debug.elementalcraft.drying.message",
                ctx.entity.getDisplayName(),
                Component.literal(String.valueOf(ctx.oldLevel)).withStyle(ChatFormatting.GOLD),
                Component.literal(String.valueOf(ctx.newLevel)).withStyle(ChatFormatting.GREEN),
                ctx.removedLayers,
                ctx.firePower
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.entity, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendExhaustionLog(ExhaustionLogContext ctx) {
        if (!DebugMode.isEnabled(ctx.player)) return;
        float total = ctx.baseDelta + ctx.wetnessDelta;
        MutableComponent msg = Component.translatable("debug.elementalcraft.exhaustion.header", String.format("%.2f", total)).withStyle(ChatFormatting.GOLD);
        msg.append(Component.literal(": "));
        msg.append(Component.translatable("debug.elementalcraft.exhaustion.base", String.format("%.2f", ctx.baseDelta)).withStyle(ChatFormatting.WHITE));
        if (ctx.wetnessLevel > 0 && ctx.wetnessDelta > 0) {
            msg.append(Component.literal(" + "));
            msg.append(Component.translatable("debug.elementalcraft.exhaustion.wetness", String.format("%.2f", ctx.wetnessDelta), ctx.wetnessLevel).withStyle(ChatFormatting.AQUA));
        }
        ctx.player.displayClientMessage(msg, true);
    }

    public static void sendScorchedSporeReactionLog(ScorchedSporeReactionLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.scorched_spore.header").withStyle(ChatFormatting.DARK_RED);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.scorched_spore.message",
                ctx.target.getDisplayName(),
                ctx.applier.getDisplayName(),
                Component.literal(String.valueOf(ctx.stacks)).withStyle(ChatFormatting.DARK_GREEN)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.target, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendParalysisLog(ParalysisLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.paralysis.header").withStyle(ChatFormatting.DARK_PURPLE);
        Component attackerName = ctx.attacker != null ? ctx.attacker.getDisplayName() : Component.translatable("debug.elementalcraft.reaction.paralysis.environment");
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.paralysis.message",
                attackerName,
                ctx.target.getDisplayName(),
                Component.literal(String.valueOf(ctx.paralysisStacks)).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(String.valueOf(ctx.remainingHits)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.format("%.2f", ctx.totalDamage)).withStyle(ChatFormatting.RED)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.target, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendWaterElectrificationLog(WaterElectrificationLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.water_electrification.header").withStyle(ChatFormatting.AQUA);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.water_electrification.message",
                ctx.source.getDisplayName(),
                Component.literal(String.valueOf(ctx.stacks)).withStyle(ChatFormatting.LIGHT_PURPLE),
                String.format("%.1f", ctx.range),
                ctx.affectedCount,
                Component.literal(String.format("%.1f", ctx.settlementDamage)).withStyle(ChatFormatting.RED),
                Component.literal(String.valueOf(ctx.paralysisDuration)).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.source, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendFrostbiteLog(FrostbiteLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.frostbite.header").withStyle(ChatFormatting.AQUA);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.frostbite.message",
                ctx.attacker.getDisplayName(),
                ctx.target.getDisplayName(),
                Component.literal(String.valueOf(ctx.stacksApplied)).withStyle(ChatFormatting.WHITE),
                Component.literal(String.valueOf(ctx.totalStacks)).withStyle(ChatFormatting.LIGHT_PURPLE),
                String.format("%.0f", ctx.chance * 100)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.attacker, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendFreezeLog(FreezeLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.freeze.header").withStyle(ChatFormatting.BLUE);
        String wetnessKey = ctx.fromWetness ? "debug.elementalcraft.reaction.freeze.with_wetness" : "debug.elementalcraft.reaction.freeze.normal";
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.freeze.message",
                ctx.target.getDisplayName(),
                Component.literal(String.valueOf(ctx.frostbiteStacks)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf(ctx.freezeStacks)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf(ctx.freezeDuration)).withStyle(ChatFormatting.WHITE),
                Component.translatable(wetnessKey).withStyle(ctx.fromWetness ? ChatFormatting.AQUA : ChatFormatting.GRAY),
                Component.literal(String.format("%.1f", ctx.damage)).withStyle(ChatFormatting.RED)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.target, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendFireFreezeMeltLog(LivingEntity target, LivingEntity attacker, int frozenStacks, int firePower, int requiredPoints) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.fire_freeze_melt.header").withStyle(ChatFormatting.RED);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.fire_freeze_melt.message",
                attacker.getDisplayName(),
                target.getDisplayName(),
                Component.literal(String.valueOf(frozenStacks)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf(requiredPoints)).withStyle(ChatFormatting.GOLD),
                Component.literal(String.valueOf(firePower)).withStyle(ChatFormatting.RED)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(attacker, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendStaticSteamCloudLog(StaticSteamCloudLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.static_steam_cloud.header").withStyle(ChatFormatting.LIGHT_PURPLE);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.static_steam_cloud.message",
                ctx.source.getDisplayName(),
                Component.literal(String.valueOf(ctx.triggerStacks)).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(String.format("%.1f", ctx.settlementDamage)).withStyle(ChatFormatting.RED),
                Component.literal(String.valueOf(ctx.cloudDuration)).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.source, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendContagionLog(ContagionLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.contagion.header").withStyle(ChatFormatting.DARK_GREEN);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.contagion.message",
                ctx.source.getDisplayName(),
                Component.literal(String.valueOf(ctx.sourceStacks)).withStyle(ChatFormatting.DARK_GREEN),
                Component.literal(String.valueOf(ctx.transferStacks)).withStyle(ChatFormatting.GREEN),
                String.format("%.1f", ctx.radius),
                ctx.affectedCount
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.source, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendFrostedSteamCloudLog(FrostedSteamCloudLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.frosted_steam_cloud.header").withStyle(ChatFormatting.AQUA);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.frosted_steam_cloud.message",
                ctx.source.getDisplayName(),
                Component.literal(String.valueOf(ctx.triggerStacks)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf(ctx.cloudDuration)).withStyle(ChatFormatting.WHITE),
                ctx.affectedCount
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.source, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendAuraDamageLog(AuraDamageLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent message = Component.translatable("debug.elementalcraft.reaction." + ctx.reactionKey + "_aura.message",
                ctx.target.getDisplayName(),
                Component.literal(String.format("%.1f", ctx.damage)).withStyle(ChatFormatting.RED),
                ctx.source.getDisplayName()
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.target, message);
    }

    public static void sendScorchedTickLog(LivingEntity target, float baseDamage, ElementType element, float elementMult, float finalDamage, float poisonMult) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent msg = Component.translatable("debug.elementalcraft.reaction.scorched_tick.header").withStyle(ChatFormatting.GOLD);
        msg.append(Component.literal(" "));
        msg.append(target.getDisplayName());
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("debug.elementalcraft.reaction.scorched_tick.base",
                Component.literal(String.format("%.1f", baseDamage)).withStyle(ChatFormatting.WHITE)));
        msg.append(Component.literal(" | "));
        msg.append(Component.translatable("debug.elementalcraft.reaction.scorched_tick.final",
                Component.literal(String.format("%.1f", finalDamage)).withStyle(ChatFormatting.RED)));
        String multipliers = "";
        if (element != ElementType.NONE && elementMult != 1.0f) {
            multipliers = element.getDisplayName().getString() + String.format(" × %.1f", elementMult);
        }
        if (poisonMult > 1.0f) {
            if (!multipliers.isEmpty()) multipliers += ", ";
            multipliers += Component.translatable("debug.elementalcraft.reaction.scorched.poison_label").getString()
                    + String.format(" × %.1f", poisonMult);
        }
        if (!multipliers.isEmpty()) {
            msg.append(Component.translatable("debug.elementalcraft.reaction.scorched.multiplier_format", multipliers).withStyle(ChatFormatting.GOLD));
        }
        sendDebugMessage(target, msg);
    }

    public static void sendScorchedAuraLog(LivingEntity source, LivingEntity target, float baseDamage, ElementType element, float elementMult, float finalDamage, float poisonMult) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent msg = Component.translatable("debug.elementalcraft.reaction.scorched_aura.header").withStyle(ChatFormatting.GOLD);
        msg.append(Component.literal(" "));
        msg.append(target.getDisplayName());
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("debug.elementalcraft.reaction.scorched_aura.base",
                Component.literal(String.format("%.1f", baseDamage)).withStyle(ChatFormatting.WHITE)));
        msg.append(Component.literal(" | "));
        msg.append(Component.translatable("debug.elementalcraft.reaction.scorched_aura.final",
                Component.literal(String.format("%.1f", finalDamage)).withStyle(ChatFormatting.RED)));
        String multipliers = "";
        if (element != ElementType.NONE && elementMult != 1.0f) {
            multipliers = element.getDisplayName().getString() + String.format(" × %.1f", elementMult);
        }
        if (poisonMult > 1.0f) {
            if (!multipliers.isEmpty()) multipliers += ", ";
            multipliers += Component.translatable("debug.elementalcraft.reaction.scorched.poison_label").getString()
                    + String.format(" × %.1f", poisonMult);
        }
        if (!multipliers.isEmpty()) {
            msg.append(Component.translatable("debug.elementalcraft.reaction.scorched.multiplier_format", multipliers).withStyle(ChatFormatting.GOLD));
        }
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("debug.elementalcraft.reaction.scorched_aura.source",
                source.getDisplayName()));
        sendDebugMessage(target, msg);
    }

    public static void sendThermalShockLog(ThermalShockLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.thermal_shock.header").withStyle(ChatFormatting.GOLD);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.thermal_shock.message",
                ctx.target.getDisplayName(),
                Component.literal(String.valueOf(ctx.remainingTicks)).withStyle(ChatFormatting.YELLOW),
                Component.literal(String.format("%.1f", ctx.totalRemainingDamage)).withStyle(ChatFormatting.GOLD),
                Component.literal(String.format("%.0f", ctx.ratio * 100)).withStyle(ChatFormatting.YELLOW),
                Component.literal(String.format("%.1f", ctx.shockDamage)).withStyle(ChatFormatting.RED),
                Component.literal(String.valueOf(ctx.steamLevel)).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.target, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendSteamScaldingTickLog(LivingEntity target, float baseDamage, float levelMultiplier, ElementType element, float elementMultiplier, float finalDamage) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent msg = Component.translatable("debug.elementalcraft.reaction.steam_scalding_tick.header").withStyle(ChatFormatting.GOLD);
        msg.append(Component.literal(" "));
        msg.append(target.getDisplayName());
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("debug.elementalcraft.reaction.steam_scalding_tick.base",
                Component.literal(String.format("%.1f", baseDamage)).withStyle(ChatFormatting.WHITE)));
        msg.append(Component.literal(" | "));
        msg.append(Component.translatable("debug.elementalcraft.reaction.steam_scalding_tick.level_mult",
                Component.literal(String.format("× %.1f", levelMultiplier)).withStyle(ChatFormatting.YELLOW)));
        msg.append(Component.literal(" | "));
        msg.append(Component.translatable("debug.elementalcraft.reaction.steam_scalding_tick.final",
                Component.literal(String.format("%.1f", finalDamage)).withStyle(ChatFormatting.RED)));
        if (element != ElementType.NONE && elementMultiplier != 1.0f) {
            msg.append(Component.translatable("debug.elementalcraft.reaction.scorched.multiplier_format",
                    element.getDisplayName().getString() + String.format(" × %.1f", elementMultiplier)).withStyle(ChatFormatting.GOLD));
        }
        sendDebugMessage(target, msg);
    }

    public static void sendSteamCloudCombinedLog(LivingEntity target, LivingEntity attacker, boolean isHighHeat, int level,
                                                    float baseDamage, float levelMultiplier, float radius, int durationTicks,
                                                    double heightCeiling, boolean clearAggro,
                                                    ElementType elementType, float elementMultiplier, boolean fireImmune, float finalDamage) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        String typeKey = isHighHeat ? "debug.elementalcraft.steam_trigger.high" : "debug.elementalcraft.steam_trigger.low";
        ChatFormatting typeColor = isHighHeat ? ChatFormatting.RED : ChatFormatting.AQUA;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.steam_trigger.header").withStyle(ChatFormatting.YELLOW);
        LivingEntity displayEntity = attacker != null ? attacker : target;
        MutableComponent msg = Component.translatable("debug.elementalcraft.steam_trigger.message",
                displayEntity.getDisplayName(),
                Component.translatable(typeKey).withStyle(typeColor),
                level
        ).withStyle(ChatFormatting.WHITE);
        msg.append(Component.translatable("debug.elementalcraft.steam_cloud_combined.base",
                Component.literal(String.format("%.1f", baseDamage)).withStyle(ChatFormatting.WHITE)));
        msg.append(Component.translatable("debug.elementalcraft.steam_cloud_combined.radius",
                Component.literal(String.format("%.1f", radius)).withStyle(ChatFormatting.WHITE)));
        msg.append(Component.translatable("debug.elementalcraft.steam_cloud_combined.duration",
                Component.literal(String.format("%.1f", durationTicks / 20.0)).withStyle(ChatFormatting.WHITE)));
        msg.append(Component.translatable("debug.elementalcraft.steam_cloud_combined.height",
                Component.literal(String.format("%.1f", heightCeiling)).withStyle(ChatFormatting.WHITE)));
        msg.append(Component.translatable("debug.elementalcraft.steam_cloud_combined.level_mult",
                Component.literal(String.format("×%.1f", levelMultiplier)).withStyle(ChatFormatting.YELLOW)));
        MutableComponent dmgPart = Component.literal(String.format("%.1f", finalDamage)).withStyle(ChatFormatting.RED);
        if (elementType != ElementType.NONE && elementMultiplier != 1.0f) {
            dmgPart.append(Component.literal("("));
            dmgPart.append(elementType.getDisplayName());
            dmgPart.append(Component.literal(String.format(" ×%.1f", elementMultiplier)).withStyle(ChatFormatting.GOLD));
            dmgPart.append(Component.literal(")"));
        }
        if (fireImmune) {
            dmgPart.append(Component.translatable("debug.elementalcraft.reaction.steam_scalding.immune",
                    Component.literal(String.format("×%.2f", ElementalFireNatureReactionsConfig.scorchedImmuneModifier)).withStyle(ChatFormatting.RED)));
        }
        msg.append(Component.translatable("debug.elementalcraft.steam_cloud_combined.damage", dmgPart));
        msg.append(Component.translatable("debug.elementalcraft.steam_cloud_combined.aggro",
                Component.translatable(clearAggro ? "debug.elementalcraft.yes" : "debug.elementalcraft.no")
                        .withStyle(clearAggro ? ChatFormatting.GREEN : ChatFormatting.RED)));
        sendDebugMessage(target, prefix.append(Component.literal(" ")).append(msg));
    }

    public static void sendThunderCounterLog(ThunderCounterLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.thunder_counter.header")
                .withStyle(ChatFormatting.YELLOW);
        MutableComponent body;
        if (ctx.success) {
            body = Component.translatable("debug.elementalcraft.reaction.thunder_counter.success",
                    ctx.target.getDisplayName(),
                    ctx.attacker.getDisplayName(),
                    String.format("%.1f", ctx.chance * 100),
                    String.valueOf(ctx.appliedStacks),
                    Component.translatable(ctx.appliedEffectKey)
                            .withStyle(ctx.appliedEffectKey.equals("effect.elementalcraft.paralysis")
                                    ? ChatFormatting.LIGHT_PURPLE
                                    : ChatFormatting.GOLD),
                    Component.literal(String.format("%.1f", ctx.lightningDamage)).withStyle(ChatFormatting.GOLD)
            ).withStyle(ChatFormatting.WHITE);
        } else {
            body = Component.translatable("debug.elementalcraft.reaction.thunder_counter.fail",
                    ctx.target.getDisplayName(),
                    ctx.attacker.getDisplayName(),
                    String.format("%.1f", ctx.chance * 100)
            ).withStyle(ChatFormatting.GRAY);
        }
        sendDebugMessage(ctx.attacker, prefix.append(Component.literal(" ")).append(body));
    }

    public static void sendDebugMessage(LivingEntity contextEntity, Component message) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        if (!(contextEntity.level() instanceof ServerLevel serverLevel)) return;
        ElementalCraft.LOGGER.info("[EC Debug] " + message.getString());
        serverLevel.getServer().getPlayerList().getPlayers().stream()
                .filter(DebugMode::isEnabled)
                .forEach(p -> p.displayClientMessage(message, false));
    }

    public static long getRemainingCooldown(LivingEntity entity, String nbtKey) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(nbtKey)) return 0;
        long endTick = data.getLong(nbtKey);
        long remaining = endTick - entity.level().getGameTime();
        return Math.max(0, remaining);
    }

    public static int getRemainingCooldownCountdown(LivingEntity entity, String nbtKey) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(nbtKey)) return 0;
        return Math.max(0, data.getInt(nbtKey));
    }

    public static void sendReactionSuccess(LivingEntity contextEntity, String reactionKey, Object... args) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent message = Component.translatable("debug.elementalcraft.reaction." + reactionKey + ".success", args)
                .withStyle(ChatFormatting.GREEN);
        sendDebugMessage(contextEntity, message);
    }

    public static void sendReactionCooldownBlock(LivingEntity contextEntity, String reactionKey, long remainingTicks) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent message = Component.translatable("debug.elementalcraft.reaction." + reactionKey + ".failed.cooldown")
                .withStyle(ChatFormatting.GRAY);
        message.append(Component.literal(" "));
        message.append(Component.translatable("debug.elementalcraft.cooldown.remaining",
                Component.literal(String.valueOf(remainingTicks)).withStyle(ChatFormatting.YELLOW),
                Component.literal(String.format("%.1f", remainingTicks / 20.0)).withStyle(ChatFormatting.GOLD)
        ).withStyle(ChatFormatting.GRAY));
        sendDebugMessage(contextEntity, message);
    }

    public static void sendReactionFailed(LivingEntity contextEntity, String reactionKey, String reason, Object... args) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent message = Component.translatable("debug.elementalcraft.reaction." + reactionKey + ".failed." + reason, args)
                .withStyle(ChatFormatting.GRAY);
        sendDebugMessage(contextEntity, message);
    }
}
