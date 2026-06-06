package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.util.DebugMode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {
    private static final String NBT_LAST_DRY_TICK = "EC_LastSelfDryTick";
    private static final String NBT_NATURE_ATTACK_COOLDOWN = "EC_NatureAttackCooldown";
    private static final String NBT_SELF_DRYING_PENALTY = "EC_SelfDryingPenalty";

    private static final Supplier<net.minecraft.world.effect.MobEffect> SPORES_EFFECT = ModMobEffects.SPORES;
    private static final Supplier<net.minecraft.world.effect.MobEffect> WETNESS_EFFECT = ModMobEffects.WETNESS;
    private static final Supplier<net.minecraft.world.effect.MobEffect> PARALYSIS_EFFECT = ModMobEffects.PARALYSIS;
    private static final Supplier<net.minecraft.world.effect.MobEffect> STATIC_SHOCK_EFFECT = ModMobEffects.STATIC_SHOCK;

    private static final Random RANDOM = new Random();

    private static final Field TRIDENT_ITEM_FIELD;
    static {
        Field field = null;
        try {
            field = ThrownTrident.class.getDeclaredField("tridentItem");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            ElementalCraft.LOGGER.error("Failed to find ThrownTrident.tridentItem field", e);
        }
        TRIDENT_ITEM_FIELD = field;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float currentDamage = event.getAmount();

        if (source.is(ModDamageTypes.LAVA_MAGIC)) {
            return;
        }

        net.minecraft.world.effect.MobEffect sporeEffect = SPORES_EFFECT.get();
        float originalPhysicalDamage = currentDamage;
        event.setAmount(currentDamage);

        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack weaponStack = ItemStack.EMPTY;
        Entity directEntity = source.getDirectEntity();

        if (directEntity instanceof ThrownTrident trident && TRIDENT_ITEM_FIELD != null) {
            try {
                ItemStack tridentStack = (ItemStack) TRIDENT_ITEM_FIELD.get(trident);
                if (tridentStack != null && !tridentStack.isEmpty()) {
                    weaponStack = tridentStack;
                }
            } catch (IllegalAccessException e) {
            }
        }
        if (weaponStack.isEmpty()) {
            ElementType consistentElement = ElementUtils.getConsistentAttackElement(attacker);
            if (consistentElement != ElementType.NONE) {
                ItemStack mainHand = attacker.getMainHandItem();
                ItemStack offHand = attacker.getOffhandItem();
                if (ElementUtils.getAttackElement(mainHand) == consistentElement) {
                    weaponStack = mainHand;
                } else if (ElementUtils.getAttackElement(offHand) == consistentElement) {
                    weaponStack = offHand;
                }
            }
        }

        ElementType attackElement = ElementUtils.getAttackElement(weaponStack);
        if (attackElement != ElementType.NONE) {
            if (ElementUtils.getDisplayEnhancement(attacker, attackElement) <= 0) {
                attackElement = ElementType.NONE;
            }
        }
        if (attackElement == ElementType.NONE) {
            return;
        }

        float sporeVulnMult = 1.0f;
        if (attackElement == ElementType.FIRE && sporeEffect != null && target.hasEffect(sporeEffect)) {
            net.minecraft.world.effect.MobEffectInstance spore = target.getEffect(sporeEffect);
            int stacks = (spore != null) ? (spore.getAmplifier() + 1) : 0;
            if (stacks > 0) {
                float vulnPerStack = (float) ElementalFireNatureReactionsConfig.sporeFireVulnPerStack;
                sporeVulnMult = 1.0f + (stacks * vulnPerStack);
            }
        }

        if (attackElement == ElementType.FIRE) {
            CompoundTag attackerData = attacker.getPersistentData();
            int attackerWetness = WetnessHandler.getWetnessLevel(attacker);
            if (attackerWetness > 0) {
                long currentTick = attacker.level().getGameTime();
                long lastDryTick = attackerData.getLong(NBT_LAST_DRY_TICK);
                if (currentTick != lastDryTick) {
                    int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
                    int threshold = Math.max(1, ElementalFireNatureReactionsConfig.wetnessDryingThreshold);
                    int layersToRemove = firePower / threshold;
                    if (layersToRemove > 0) {
                        int newLevel = Math.max(0, attackerWetness - layersToRemove);
                        int actuallyRemoved = attackerWetness - newLevel;
                        WetnessHandler.updateWetnessLevel(attacker, newLevel);
                        attackerData.putLong(NBT_LAST_DRY_TICK, currentTick);
                        net.minecraft.world.effect.MobEffect wetnessEffect = WETNESS_EFFECT.get();
                        if (newLevel == 0 && wetnessEffect != null && attacker.hasEffect(wetnessEffect)) {
                            attacker.removeEffect(wetnessEffect);
                        }
                        int maxBurstLevel = ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel;
                        EffectHelper.playSteamBurst((ServerLevel) attacker.level(), attacker, 0.5f, Math.min(layersToRemove, maxBurstLevel), true);
                        attackerData.putInt(NBT_SELF_DRYING_PENALTY, 1);

                        DebugCommand.DryLogContext dryCtx = new DebugCommand.DryLogContext();
                        dryCtx.entity = attacker;
                        dryCtx.oldLevel = attackerWetness;
                        dryCtx.newLevel = newLevel;
                        dryCtx.removedLayers = actuallyRemoved;
                        dryCtx.firePower = firePower;
                        DebugCommand.sendDryLog(dryCtx);
                    }
                } else {
                    attackerData.putInt(NBT_SELF_DRYING_PENALTY, 1);
                }
            }
        }

        float physicalDamage = currentDamage;
        int enhancementPoints = ElementUtils.getDisplayEnhancement(attacker, attackElement);
        int resistancePoints = ElementUtils.getDisplayResistance(target, attackElement);

        float frozenMeltMult = 1.0f;
        boolean frozenMelted = false;
        if (attackElement == ElementType.FIRE && ElementalThunderFrostReactionsConfig.frostbiteFireSteamThreshold > 0 && FrostbiteHandler.isFrozen(target)) {
            int required = ElementalThunderFrostReactionsConfig.frostbiteFireSteamThreshold;
            frozenMeltMult = (float) ElementalThunderFrostReactionsConfig.fireFrostMeltDamageMult;
            if (enhancementPoints >= required) {
                frozenMelted = true;
            }
        }

        int strengthPerHalfDamage = ElementalConfig.getStrengthPerHalfDamage();
        int resistPerHalfReduction = ElementalConfig.getResistPerHalfReduction();

        float baseEnhancementDamage = enhancementPoints / (float) strengthPerHalfDamage * 0.5f;
        float baseResistReduction = resistancePoints / (float) resistPerHalfReduction * 0.5f;

        if (baseEnhancementDamage <= 0.0f) {
            return;
        }

        int wetnessLevel = WetnessHandler.getWetnessLevel(target);
        if (wetnessLevel <= 0) {
            String prefix = "EC_WetnessSnapshot_";
            for (String tag : target.getTags()) {
                if (tag.startsWith(prefix)) {
                    try {
                        wetnessLevel = Integer.parseInt(tag.substring(prefix.length()));
                        target.removeTag(tag);
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        }

        float wetnessBaseMult = 1.0f;
        if (wetnessLevel > 0 && attackElement == ElementType.FIRE) {
            float reductionPerLevel = (float) ElementalFireNatureReactionsConfig.wetnessFireReduction;
            float finalReduction = wetnessLevel * reductionPerLevel;
            wetnessBaseMult = 1.0f - finalReduction;
        }

        CompoundTag attackerData = attacker.getPersistentData();
        float selfDryingPenaltyMult = 1.0f;
        if (attackerData.getInt(NBT_SELF_DRYING_PENALTY) != 0 && attackElement == ElementType.FIRE) {
            selfDryingPenaltyMult = 1.0f - (float) ElementalFireNatureReactionsConfig.wetnessSelfDryingDamagePenalty;
            attackerData.putInt(NBT_SELF_DRYING_PENALTY, 0);
        }

        float combinedWetnessMult = wetnessBaseMult * selfDryingPenaltyMult;

        ElementType targetDominant = ElementUtils.getConsistentAttackElement(target);
        float restraintMult = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);

        float globalDamageMult = (float) ElementalConfig.elementalDamageMultiplier;
        float globalResistMult = (float) ElementalConfig.elementalResistanceMultiplier;

        float freezeVulnMult = 1.0f;
        if (FrostbiteHandler.hasFrostbite(target) && attackElement != ElementType.NONE) {
            freezeVulnMult = (float) ElementalThunderFrostReactionsConfig.freezeElementalVulnerability;
        }

        float scorchVulnMult = 1.0f;
        if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)
                && attackElement != ElementType.NONE) {
            switch (attackElement) {
                case FROST -> scorchVulnMult = (float) ElementalFireNatureReactionsConfig.scorchedFrostDmgMultiplier;
                case NATURE -> scorchVulnMult = (float) ElementalFireNatureReactionsConfig.scorchedNatureDmgMultiplier;
                case FIRE -> scorchVulnMult = (float) ElementalFireNatureReactionsConfig.scorchedFireDmgMultiplier;
                case THUNDER -> scorchVulnMult = (float) ElementalFireNatureReactionsConfig.scorchedThunderDmgMultiplier;
                case NONE -> {}
            }
        }

        float attackPart = baseEnhancementDamage * globalDamageMult * combinedWetnessMult * restraintMult * sporeVulnMult * freezeVulnMult * scorchVulnMult * frozenMeltMult;

        float finalElementalDmg;
        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;
        float benchmark = (float) ElementalConfig.getMaxStatCap();

        if (restraintMult > 1.0f && resistancePoints >= benchmark) {
            float resistRatio = Math.min(resistancePoints / benchmark, 1.0f);
            double maxReductionAllowed = 1.0 - minPercent;
            double actualReduction = resistRatio * maxReductionAllowed;
            finalElementalDmg = attackPart * (float) (1.0 - actualReduction);
            isFloored = true;
        } else {
            float defensePart = baseResistReduction * globalResistMult;
            finalElementalDmg = Math.max(0.0f, attackPart - defensePart);
        }

        float remainingAbsorption = target.getAbsorptionAmount();
        if (remainingAbsorption > 0.0f && finalElementalDmg > 0.0f) {
            float absorbed = Math.min(remainingAbsorption, finalElementalDmg);
            target.setAbsorptionAmount(remainingAbsorption - absorbed);
            finalElementalDmg -= absorbed;
        }

        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        DebugCommand.CombatLogContext combatCtx = new DebugCommand.CombatLogContext();
        combatCtx.attacker = attacker;
        combatCtx.target = target;
        combatCtx.directEntity = directEntity;
        combatCtx.originalPhysicalDamage = originalPhysicalDamage;
        combatCtx.physicalDamage = physicalDamage;
        combatCtx.attackElement = attackElement;
        combatCtx.attackerEnhancement = enhancementPoints;
        combatCtx.targetResistance = resistancePoints;
        combatCtx.baseEnhancementDamage = baseEnhancementDamage;
        combatCtx.globalDamageMult = globalDamageMult;
        combatCtx.restraintMult = restraintMult;
        combatCtx.sporeVulnMult = sporeVulnMult;
        combatCtx.freezeVulnMult = freezeVulnMult;
        combatCtx.scorchVulnMult = scorchVulnMult;
        combatCtx.wetnessBaseMult = wetnessBaseMult;
        combatCtx.selfDryingPenaltyMult = selfDryingPenaltyMult;
        combatCtx.combinedWetnessMult = combinedWetnessMult;
        combatCtx.baseResistReduction = baseResistReduction;
        combatCtx.globalResistMult = globalResistMult;
        combatCtx.finalElemDmg = finalElementalDmg;
        combatCtx.totalDamage = totalDamage;
        combatCtx.isFloored = isFloored;
        combatCtx.minPercent = minPercent;
        combatCtx.wetnessLevel = wetnessLevel;
        combatCtx.frozenMeltMult = frozenMeltMult;
        combatCtx.frozenMelted = frozenMelted;

        DebugCommand.sendCombatLog(combatCtx);

        if (attackElement == ElementType.FIRE) {
            if (frozenMelted) {
                applyFireFreezeMelt(target, attacker, enhancementPoints);
            } else {
                tryTriggerScorched(attacker, target, enhancementPoints);
            }
            if (target instanceof Creeper creeper && creeper.isAlive() && !creeper.isDeadOrDying() && enhancementPoints >= 50) {
                target.level().explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(),
                        creeper.isPowered() ? 6.0f : 3.0f, Level.ExplosionInteraction.MOB);
            }
        } else if (attackElement == ElementType.NATURE) {
            if (ElementUtils.getConsistentAttackElement(target) == ElementType.THUNDER) {
                net.minecraft.world.effect.MobEffect spore = SPORES_EFFECT.get();
                if (spore == null || !target.hasEffect(spore)) {
                return;
                }

                net.minecraft.world.effect.MobEffectInstance sporeInstance = target.getEffect(spore);
                int sporeStacks = sporeInstance != null ? sporeInstance.getAmplifier() + 1 : 0;
                int minSporeStacks = ElementalThunderFrostReactionsConfig.thunderCounterMinSporeStacks;
                if (minSporeStacks <= 0 || sporeStacks < minSporeStacks) {
                    return;
                }

                long currentGameTime = attacker.level().getGameTime();
                long cooldownEndTime = target.getPersistentData().getLong(NBT_NATURE_ATTACK_COOLDOWN);
                if (currentGameTime < cooldownEndTime) {
                    DebugCommand.sendReactionCooldownBlock(target, "thunder_counter", cooldownEndTime - currentGameTime);
                    return;
                }

                DebugCommand.ThunderCounterLogContext thunderCtx = new DebugCommand.ThunderCounterLogContext();
                thunderCtx.attacker = attacker;
                thunderCtx.target = target;
                thunderCtx.chance = 1.0;
                thunderCtx.success = true;
                thunderCtx.lightningDamage = (float) ElementalThunderFrostReactionsConfig.counterLightningDamage;

                LivingEntity reactionTarget = attacker;
                net.minecraft.world.effect.MobEffect wetnessEffect = WETNESS_EFFECT.get();
                boolean attackerHasWetness = wetnessEffect != null && reactionTarget.hasEffect(wetnessEffect);

                if (reactionTarget.level() instanceof ServerLevel serverLevel) {
                    LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(serverLevel);
                    if (lightning != null) {
                        lightning.moveTo(reactionTarget.getX(), reactionTarget.getY(), reactionTarget.getZ());
                        lightning.setDamage((float) ElementalThunderFrostReactionsConfig.counterLightningDamage);
                        serverLevel.addFreshEntity(lightning);
                    }
                }

                if (attackerHasWetness && wetnessEffect != null
                        && ElementalThunderFrostReactionsConfig.paralysisMaxStacks > 0) {
                    net.minecraft.world.effect.MobEffectInstance wetnessInstance = reactionTarget.getEffect(wetnessEffect);
                    int wetnessStacks = wetnessInstance != null ? (wetnessInstance.getAmplifier() + 1) : 1;
                    int maxParalysisStacks = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
                    int paralysisStacks = Math.min(wetnessStacks, maxParalysisStacks);
                    net.minecraft.world.effect.MobEffect paralysisEffect = PARALYSIS_EFFECT.get();
                    if (paralysisEffect != null) {
                        reactionTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                paralysisEffect,
                                ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks * paralysisStacks,
                                paralysisStacks - 1));
                    }
                    reactionTarget.removeEffect(wetnessEffect);
                    thunderCtx.appliedEffectKey = "effect.elementalcraft.paralysis";
                    thunderCtx.appliedStacks = paralysisStacks;
                } else {
                    int staticStacks = ElementalThunderFrostReactionsConfig.staticStacksWhenNoWetness;
                    net.minecraft.world.effect.MobEffect staticEffect = STATIC_SHOCK_EFFECT.get();
                    if (staticEffect != null) {
                        reactionTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                staticEffect,
                                ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks * staticStacks,
                                staticStacks - 1));
                    }
                    thunderCtx.appliedEffectKey = "effect.elementalcraft.static_shock";
                    thunderCtx.appliedStacks = staticStacks;
                }

                reactionTarget.level().playSound(null, reactionTarget.getX(), reactionTarget.getY(), reactionTarget.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 1.0f);
                target.getPersistentData().putLong(NBT_NATURE_ATTACK_COOLDOWN, target.level().getGameTime() + ElementalThunderFrostReactionsConfig.natureAttackCooldownTicks);

                DebugCommand.sendThunderCounterLog(thunderCtx);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        DamageSource source = event.getSource();
        Entity attackerEntity = source.getEntity();
        if (attackerEntity instanceof LivingEntity attacker) {
            net.minecraft.world.effect.MobEffect paralysisEffect = PARALYSIS_EFFECT.get();
            if (paralysisEffect != null && attacker.hasEffect(paralysisEffect)) {
                event.setCanceled(true);
            }
            if (FrostbiteHandler.isFrozen(attacker) && !event.getSource().is(ModDamageTypes.FROSTBITE_THERMAL_SHOCK)) {
                event.setCanceled(true);
            }
        }
    }

    private static void tryTriggerScorched(LivingEntity attacker, LivingEntity target, int firePower) {
        net.minecraft.world.effect.MobEffect sporeEffect = SPORES_EFFECT.get();
        if (sporeEffect != null && target.hasEffect(sporeEffect)) {
            if (ElementalFireNatureReactionsConfig.blastTriggerThreshold > 0
                    && firePower >= ElementalFireNatureReactionsConfig.blastTriggerThreshold
                    && !(ElementalThunderFrostReactionsConfig.frostbiteReduceSporesEnabled && FrostbiteHandler.hasFrostbite(target))) {
                return;
            }
        }

        if (FrostbiteHandler.hasFrostbite(target)) {
            int fbStacks = FrostbiteHandler.getFrostbiteStacks(target);
            int required = ElementalThunderFrostReactionsConfig.fireFrostMeltBaseThreshold
                    + (fbStacks - 1) * ElementalThunderFrostReactionsConfig.fireFrostMeltAdditionalCost;
            if (ElementalThunderFrostReactionsConfig.fireFrostMeltBaseThreshold > 0 && firePower >= required) {
                FrostbiteHandler.clearFrostbite(target);
                ScorchedHandler.clearScorched(target);
                int fireStep = 20;
                int level = Math.max(1, Math.min(firePower / fireStep, ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel));
                if (ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel > 0
                        && !SteamReactionHandler.isOnSteamCooldown(attacker)) {
                    SteamReactionHandler.spawnSteamCloud(target, true, level);
                    SteamReactionHandler.applySteamCooldown(attacker, SteamReactionHandler.computeCloudDuration(true, level));
                }
                if (target.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 30, 1.0, 1.0, 1.0, 0.1);
                }
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.5f);
                DebugCommand.sendReactionSuccess(target, "frostbite_steam",
                        target.getDisplayName(),
                        attacker.getDisplayName(),
                        Component.literal(String.valueOf(level)).withStyle(ChatFormatting.AQUA),
                        Component.literal(String.valueOf(firePower)).withStyle(ChatFormatting.RED));
                return;
            }
        }

        if (ElementalFireNatureReactionsConfig.scorchedTriggerThreshold <= 0) return;
        if (firePower < ElementalFireNatureReactionsConfig.scorchedTriggerThreshold) {
            DebugCommand.sendReactionFailed(target, "scorched", "power_low",
                    attacker.getDisplayName(),
                    target.getDisplayName(),
                    Component.literal(String.valueOf(firePower)).withStyle(ChatFormatting.RED),
                    Component.literal(String.valueOf(ElementalFireNatureReactionsConfig.scorchedTriggerThreshold)).withStyle(ChatFormatting.GOLD));
            return;
        }
        net.minecraft.world.effect.MobEffect wetnessEffect = WETNESS_EFFECT.get();
        if ((wetnessEffect != null && target.hasEffect(wetnessEffect)) || (wetnessEffect != null && attacker.hasEffect(wetnessEffect))) {
            DebugCommand.sendReactionFailed(target, "scorched", "wet",
                    attacker.getDisplayName(),
                    target.getDisplayName());
            return;
        }
        if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            DebugCommand.sendReactionFailed(target, "scorched", "already",
                    attacker.getDisplayName(),
                    target.getDisplayName());
            return;
        }
        {
            CompoundTag attackerData = attacker.getPersistentData();
            long gameTime = target.level().getGameTime();
            if (attackerData.contains(ScorchedHandler.NBT_ATTACKER_SCORCHED_COOLDOWN)) {
                long cd = attackerData.getLong(ScorchedHandler.NBT_ATTACKER_SCORCHED_COOLDOWN);
                if (gameTime < cd) {
                    DebugCommand.sendReactionCooldownBlock(attacker, "scorched_attack", cd - gameTime);
                    return;
                }
            }
        }

        double baseChance = ElementalFireNatureReactionsConfig.scorchedBaseChance;
        int pointsPerStep = ElementalFireNatureReactionsConfig.scorchedChancePerPoint;
        int threshold = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
        double growth = Math.floor((firePower - threshold) / (double) pointsPerStep) * 0.05;
        double totalChance = Math.min(1.0, Math.max(0.0, baseChance + growth));
        boolean hasPoison = target.hasEffect(net.minecraft.world.effect.MobEffects.POISON);
        boolean triggered;
        if (hasPoison) {
            totalChance = 1.0;
            triggered = true;
        } else {
            triggered = RANDOM.nextDouble() < totalChance;
        }

        if (triggered) {
            int duration = ElementalFireNatureReactionsConfig.scorchedDuration;
            ScorchedHandler.ScorchedApplyResult result = ScorchedHandler.applyScorched(target, attacker, firePower, duration, firePower, 1.0f, false);

            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
            String durationInfo;
            double durationSec = duration / 20.0;
            double adjustedSec = result.adjustedDuration / 20.0;
            if (result.targetElement != ElementType.NONE && result.multiplier != 1.0f) {
                durationInfo = Component.translatable("debug.elementalcraft.reaction.scorched.duration_element_enhanced",
                        String.format("%.1f", durationSec), String.format("%.1f", adjustedSec),
                        result.targetElement.getDisplayName(), String.format("%.1f", result.multiplier)).getString();
            } else {
                durationInfo = Component.translatable("debug.elementalcraft.reaction.scorched.duration_seconds",
                        String.format("%.1f", adjustedSec)).getString();
            }
            String chanceInfo;
            if (hasPoison) {
                chanceInfo = String.format("%.0f%%", totalChance * 100)
                        + "(" + Component.translatable("debug.elementalcraft.reaction.scorched.poison_label").getString() + ")";
            } else {
                chanceInfo = String.format("%.0f%%", totalChance * 100);
            }
            if (hasPoison) {
                double enhancedSec = (int)(duration * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier) / 20.0;
                durationInfo = Component.translatable("debug.elementalcraft.reaction.scorched.duration_poison_enhanced",
                        String.format("%.1f", durationSec), String.format("%.1f", enhancedSec)).getString()
                        + "(" + Component.translatable("debug.elementalcraft.reaction.scorched.poison_label").getString() + ")";
            }
            float baseDamage = ScorchedHandler.calculateScorchedDamage(firePower, target);
            DebugCommand.sendReactionSuccess(target, "scorched",
                    attacker.getDisplayName(),
                    target.getDisplayName(),
                    Component.literal(String.valueOf(firePower)).withStyle(ChatFormatting.RED),
                    chanceInfo,
                    durationInfo,
                    String.format("%.1f", baseDamage));
        } else if (totalChance > 0.01) {
            DebugCommand.sendReactionFailed(target, "scorched", "chance",
                    attacker.getDisplayName(),
                    target.getDisplayName(),
                    String.format("%.0f", totalChance * 100));
        }
    }

    private static void applyFireFreezeMelt(LivingEntity target, LivingEntity attacker, int firePower) {
        if (target.level().isClientSide) return;
        CompoundTag data = target.getPersistentData();
        int frozenStacks = data.getInt(FrostbiteHandler.NBT_FREEZE_STACKS);
        if (frozenStacks <= 0) frozenStacks = 1;

        target.removeEffect(ModMobEffects.FREEZE.get());
        data.remove(FrostbiteHandler.NBT_FREEZE_STACKS);
        data.remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
        data.putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN,
                target.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);

        FrostbiteHandler.clearFrostbite(target);
        ScorchedHandler.clearScorched(target);

        int maxWetness = ElementalFireNatureReactionsConfig.wetnessMaxLevel;
        int newWetness = Math.min(frozenStacks, maxWetness);
        WetnessHandler.updateWetnessLevel(target, newWetness);
        data.putInt(WetnessHandler.NBT_DECAY_TIMER, 0);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.5f);
        DebugCommand.sendFireFreezeMeltLog(target, attacker, frozenStacks, firePower, ElementalThunderFrostReactionsConfig.frostbiteFireSteamThreshold);
        data.putBoolean("EC_FireFrostMeltResolved", true);
    }
}