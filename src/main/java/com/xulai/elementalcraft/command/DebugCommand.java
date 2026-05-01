package com.xulai.elementalcraft.command;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.mojang.brigadier.CommandDispatcher;
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

    public static class CombatLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public Entity directEntity;
        public float originalPhysicalDamage;
        public float physicalDamage;
        public int sporeStacksForPhysReduce;
        public float sporePhysResistPerStack;
        public float sporePhysReduceRatio;
        public ElementType attackElement;
        public int attackerEnhancement;
        public int targetResistance;
        public float baseEnhancementDamage;
        public float globalDamageMult;
        public float restraintMult;
        public float sporeVulnMult;
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
            if (sporeStacksForPhysReduce > 0) {
                details.append(Component.literal(", "));
                details.append(Component.translatable("debug.elementalcraft.combat.details.spores", sporeStacksForPhysReduce).withStyle(ChatFormatting.DARK_GREEN));
            }
            return details;
        }
    }

    public static class NatureSiphonLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public int drainedLayers;
        public float healedAmount;
    }

    public static class ToxicBlastLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public int stacks;
        public double radius;
        public int affectedCount;
    }

    public static class WildfireLogContext {
        public LivingEntity victim;
        public double radius;
        public int affectedCount;
    }

    public static class SteamTriggerLogContext {
        public LivingEntity attacker;
        public boolean isHighHeat;
        public int level;
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
    }

    public static class ParalysisLogContext {
        public LivingEntity attacker;
        public LivingEntity target;
        public int paralysisStacks;
        public int remainingHits;
        public float totalDamage;
    }

    public static class StaticConductionLogContext {
        public LivingEntity source;
        public int stacks;
        public int range;
        public int affectedCount;
        public int paralysisCount;
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

        MutableComponent physicalDetail = Component.empty();
        if (ctx.sporeStacksForPhysReduce > 0) {
            physicalDetail = Component.translatable("debug.elementalcraft.physical.spore_reduce",
                    String.format("%.2f", ctx.originalPhysicalDamage),
                    ctx.sporeStacksForPhysReduce,
                    String.format("%.0f", ctx.sporePhysResistPerStack * 100),
                    String.format("%.0f", ctx.sporePhysReduceRatio * 100),
                    String.format("%.2f", ctx.physicalDamage)
            ).withStyle(ChatFormatting.GRAY);
        }

        MutableComponent fullMessage = Component.literal("")
                .append(prefix)
                .append(Component.literal(" "))
                .append(overview)
                .append(ctx.buildFormulaComponent());
        if (!physicalDetail.getString().isEmpty()) {
            fullMessage.append(Component.literal("\n")).append(physicalDetail);
        }
        fullMessage.append(Component.literal("\n")).append(ctx.buildDetailsComponent());

        sendDebugMessage(ctx.attacker, fullMessage);
    }

    public static void sendNatureSiphonLog(NatureSiphonLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.nature_siphon.header").withStyle(ChatFormatting.GREEN);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.nature_siphon.message",
                ctx.attacker.getDisplayName(),
                ctx.target.getDisplayName(),
                Component.literal(String.valueOf(ctx.drainedLayers)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.format("%.1f", ctx.healedAmount)).withStyle(ChatFormatting.RED)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.attacker, prefix.append(Component.literal(" ")).append(content));
    }

    public static void sendToxicBlastLog(ToxicBlastLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.toxic_blast.header").withStyle(ChatFormatting.RED);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.toxic_blast.message",
                ctx.attacker.getDisplayName(),
                ctx.target.getDisplayName(),
                Component.literal(String.valueOf(ctx.stacks)).withStyle(ChatFormatting.DARK_GREEN),
                String.format("%.1f", ctx.radius),
                ctx.affectedCount
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

    public static void sendSteamTriggerLog(SteamTriggerLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.steam_trigger.header").withStyle(ChatFormatting.YELLOW);
        String typeKey = ctx.isHighHeat ? "debug.elementalcraft.steam_trigger.high" : "debug.elementalcraft.steam_trigger.low";
        ChatFormatting color = ctx.isHighHeat ? ChatFormatting.RED : ChatFormatting.AQUA;
        MutableComponent content = Component.translatable("debug.elementalcraft.steam_trigger.message",
                ctx.attacker.getDisplayName(),
                Component.translatable(typeKey).withStyle(color),
                ctx.level
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.attacker, prefix.append(Component.literal(" ")).append(content));
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

    public static void sendStaticConductionLog(StaticConductionLogContext ctx) {
        if (!DebugMode.hasAnyDebugEnabled()) return;
        MutableComponent prefix = Component.translatable("debug.elementalcraft.reaction.static_conduction.header").withStyle(ChatFormatting.GOLD);
        MutableComponent content = Component.translatable("debug.elementalcraft.reaction.static_conduction.message",
                ctx.source.getDisplayName(),
                Component.literal(String.valueOf(ctx.stacks)).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(String.valueOf(ctx.range)).withStyle(ChatFormatting.GREEN),
                Component.literal(String.valueOf(ctx.affectedCount)).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf(ctx.paralysisCount)).withStyle(ChatFormatting.RED)
        ).withStyle(ChatFormatting.WHITE);
        sendDebugMessage(ctx.source, prefix.append(Component.literal(" ")).append(content));
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
                                    : ChatFormatting.GOLD)
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
}
