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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
    private static final String NBT_WETNESS = "EC_WetnessLevel";
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
        int sporeStacksForPhysReduce = 0;
        float sporePhysResistPerStack = (float) ElementalFireNatureReactionsConfig.sporePhysResist;
        float sporePhysReduceRatio = 0f;

        if (sporeEffect != null && target.hasEffect(sporeEffect)) {
            net.minecraft.world.effect.MobEffectInstance effect = target.getEffect(sporeEffect);
            int stacks = (effect != null) ? (effect.getAmplifier() + 1) : 0;
            if (stacks > 0) {
                long applyTick = target.getPersistentData().getLong("EC_SporeApplyTick");
                if (target.level().getGameTime() != applyTick) {
                    boolean isMelee = false;
                    boolean isProjectile = false;
                    Entity directEntity = source.getDirectEntity();
                    if (directEntity instanceof LivingEntity && !(directEntity instanceof net.minecraft.world.entity.projectile.Projectile)) {
                        isMelee = true;
                    }
                    isProjectile = source.is(DamageTypeTags.IS_PROJECTILE);

                    if (isMelee || isProjectile) {
                        sporeStacksForPhysReduce = stacks;
                        float totalResist = stacks * sporePhysResistPerStack;
                        if (totalResist > 1.0f) totalResist = 1.0f;
                        sporePhysReduceRatio = totalResist;
                        currentDamage = originalPhysicalDamage * (1.0f - totalResist);
                    }
                }
            }
        }
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
            int attackerWetness = attackerData.getInt(NBT_WETNESS);
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
                        attackerData.putInt(NBT_WETNESS, newLevel);
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

        int strengthPerHalfDamage = ElementalConfig.getStrengthPerHalfDamage();
        int resistPerHalfReduction = ElementalConfig.getResistPerHalfReduction();

        float baseEnhancementDamage = enhancementPoints / (float) strengthPerHalfDamage * 0.5f;
        float baseResistReduction = resistancePoints / (float) resistPerHalfReduction * 0.5f;

        if (baseEnhancementDamage <= 0.0f) {
            return;
        }

        int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);
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
        float maxCap = (float) ElementalFireNatureReactionsConfig.wetnessMaxReduction;
        if (wetnessLevel > 0 && attackElement == ElementType.FIRE) {
            float reductionPerLevel = (float) ElementalFireNatureReactionsConfig.wetnessFireReduction;
            float finalReduction = Math.min(wetnessLevel * reductionPerLevel, maxCap);
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

        float attackPart = baseEnhancementDamage * globalDamageMult * combinedWetnessMult * restraintMult * sporeVulnMult;

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
        combatCtx.sporeStacksForPhysReduce = sporeStacksForPhysReduce;
        combatCtx.sporePhysResistPerStack = sporePhysResistPerStack;
        combatCtx.sporePhysReduceRatio = sporePhysReduceRatio;
        combatCtx.attackElement = attackElement;
        combatCtx.attackerEnhancement = enhancementPoints;
        combatCtx.targetResistance = resistancePoints;
        combatCtx.baseEnhancementDamage = baseEnhancementDamage;
        combatCtx.globalDamageMult = globalDamageMult;
        combatCtx.restraintMult = restraintMult;
        combatCtx.sporeVulnMult = sporeVulnMult;
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

        DebugCommand.sendCombatLog(combatCtx);

        if (attackElement == ElementType.FIRE) {
            tryTriggerScorched(attacker, target, enhancementPoints);
        } else if (attackElement == ElementType.NATURE) {
            if (ElementUtils.getConsistentAttackElement(target) == ElementType.THUNDER) {
                net.minecraft.world.effect.MobEffect spore = SPORES_EFFECT.get();
                if (spore == null || !target.hasEffect(spore)) {
                return;
                }

                net.minecraft.world.effect.MobEffectInstance sporeInstance = target.getEffect(spore);
                int sporeStacks = sporeInstance != null ? sporeInstance.getAmplifier() + 1 : 0;
                int minSporeStacks = ElementalThunderFrostReactionsConfig.thunderCounterMinSporeStacks;
                if (sporeStacks < minSporeStacks) {
                    return;
                }

                long currentGameTime = attacker.level().getGameTime();
                long cooldownEndTime = target.getPersistentData().getLong(NBT_NATURE_ATTACK_COOLDOWN);
                if (currentGameTime < cooldownEndTime) {
                    return;
                }

                DebugCommand.ThunderCounterLogContext thunderCtx = new DebugCommand.ThunderCounterLogContext();
                thunderCtx.attacker = attacker;
                thunderCtx.target = target;
                thunderCtx.chance = 1.0;
                thunderCtx.success = true;

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

                if (attackerHasWetness && wetnessEffect != null) {
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
        }
    }

    private static void tryTriggerScorched(LivingEntity attacker, LivingEntity target, int firePower) {
        net.minecraft.world.effect.MobEffect sporeEffect = SPORES_EFFECT.get();
        if (sporeEffect != null && target.hasEffect(sporeEffect)) {
            if (firePower >= ElementalFireNatureReactionsConfig.blastTriggerThreshold) {
                return;
            }
        }

        if (firePower < ElementalFireNatureReactionsConfig.scorchedTriggerThreshold) return;
        net.minecraft.world.effect.MobEffect wetnessEffect = WETNESS_EFFECT.get();
        if ((wetnessEffect != null && target.hasEffect(wetnessEffect)) || (wetnessEffect != null && attacker.hasEffect(wetnessEffect))) {
            return;
        }
        if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            return;
        }

        double baseChance = ElementalFireNatureReactionsConfig.scorchedBaseChance;
        double growth = firePower * ElementalFireNatureReactionsConfig.scorchedChancePerPoint;
        double totalChance = Math.min(1.0, baseChance + growth);
        boolean triggered = RANDOM.nextDouble() < totalChance;

        if (triggered) {
            int duration = ElementalFireNatureReactionsConfig.scorchedDuration;
            ScorchedHandler.applyScorched(target, attacker, firePower, duration, firePower);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }
}