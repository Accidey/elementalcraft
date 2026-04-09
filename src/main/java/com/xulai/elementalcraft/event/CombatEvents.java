package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.util.GlobalDebugLogger;
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

    // 使用反射缓存 ThrownTrident 的内部物品字段，避免高开销的实体序列化
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

        net.minecraft.world.effect.MobEffect sporeEffect = SPORES_EFFECT.get();
        if (sporeEffect != null && target.hasEffect(sporeEffect)) {
            net.minecraft.world.effect.MobEffectInstance effect = target.getEffect(sporeEffect);
            int stacks = (effect != null) ? (effect.getAmplifier() + 1) : 0;
            if (stacks > 0) {
                boolean isMelee = false;
                boolean isProjectile = false;
                Entity directEntity = source.getDirectEntity();

                if (directEntity instanceof LivingEntity && !(directEntity instanceof net.minecraft.world.entity.projectile.Projectile)) {
                    isMelee = true;
                }
                isProjectile = source.is(DamageTypeTags.IS_PROJECTILE);

                if (isMelee || isProjectile) {
                    float resistPerStack = (float) ElementalFireNatureReactionsConfig.sporePhysResist;
                    float totalResist = Math.min(0.9f, stacks * resistPerStack);
                    float originalDamage = currentDamage;
                    currentDamage *= (1.0f - totalResist);
                    Debug.logSporePhysResist(target, stacks, totalResist, originalDamage, currentDamage);
                }
            }
        }
        event.setAmount(currentDamage);

        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack weaponStack = ItemStack.EMPTY;
        Entity directEntity = source.getDirectEntity();

        // 高性能获取三叉戟物品（无 NBT 序列化开销）
        if (directEntity instanceof ThrownTrident trident && TRIDENT_ITEM_FIELD != null) {
            try {
                ItemStack tridentStack = (ItemStack) TRIDENT_ITEM_FIELD.get(trident);
                if (tridentStack != null && !tridentStack.isEmpty()) {
                    weaponStack = tridentStack;
                }
            } catch (IllegalAccessException e) {
                // 忽略极少数情况下的反射错误
            }
        }
        if (weaponStack.isEmpty()) {
            ItemStack mainHand = attacker.getMainHandItem();
            ItemStack offHand = attacker.getOffhandItem();
            if (ElementUtils.getAttackElement(mainHand) != ElementType.NONE) {
                weaponStack = mainHand;
            } else if (ElementUtils.getAttackElement(offHand) != ElementType.NONE) {
                weaponStack = offHand;
            }
        }

        // 修复点1：获取攻击元素并严格进行属性一致性校验（兼顾三叉戟等投射物）
        ElementType attackElement = ElementUtils.getAttackElement(weaponStack);
        if (attackElement != ElementType.NONE) {
            // 必须具备对应的元素强化点数，才视为真正的属性攻击
            if (ElementUtils.getDisplayEnhancement(attacker, attackElement) <= 0) {
                attackElement = ElementType.NONE;
            }
        }
        
        if (attackElement == ElementType.NONE) {
            return;
        }

        float fireVulnMultiplier = 1.0f;
        if (attackElement == ElementType.FIRE && sporeEffect != null && target.hasEffect(sporeEffect)) {
            net.minecraft.world.effect.MobEffectInstance spore = target.getEffect(sporeEffect);
            int stacks = (spore != null) ? (spore.getAmplifier() + 1) : 0;
            if (stacks > 0) {
                double vulnPerStack = ElementalFireNatureReactionsConfig.sporeFireVulnPerStack;
                fireVulnMultiplier = 1.0f + (float)(stacks * vulnPerStack);
                Debug.logFireVuln(target, stacks, fireVulnMultiplier);
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
                        DebugCommand.sendDryLog(attacker, attackerWetness, newLevel, actuallyRemoved, firePower);
                        Debug.logSelfDry(attacker, attackerWetness, newLevel, actuallyRemoved, firePower);
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

        float rawElementalDamage = enhancementPoints / (float) strengthPerHalfDamage * 0.5f;
        float rawResistReduction = resistancePoints / (float) resistPerHalfReduction * 0.5f;

        if (rawElementalDamage <= 0.0f) {
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
                        Debug.logWetnessSnapshot(target, wetnessLevel);
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        }

        float wetnessMultiplier = 1.0f;
        float maxCap = (float) ElementalFireNatureReactionsConfig.wetnessMaxReduction;
        if (wetnessLevel > 0 && attackElement == ElementType.FIRE) {
            float reductionPerLevel = (float) ElementalFireNatureReactionsConfig.wetnessFireReduction;
            float finalReduction = Math.min(wetnessLevel * reductionPerLevel, maxCap);
            wetnessMultiplier = 1.0f - finalReduction;
            Debug.logWetnessEffect(target, wetnessLevel, finalReduction, wetnessMultiplier);
        }

        CompoundTag attackerData = attacker.getPersistentData();
        if (attackerData.getInt(NBT_SELF_DRYING_PENALTY) != 0 && attackElement == ElementType.FIRE) {
            float penalty = 1.0f - (float) ElementalFireNatureReactionsConfig.wetnessSelfDryingDamagePenalty;
            wetnessMultiplier *= penalty;
            Debug.logSelfDryPenalty(attacker, penalty, wetnessMultiplier);
            attackerData.putInt(NBT_SELF_DRYING_PENALTY, 0);
        }

        // 修复点2：直接使用一致性方法获取目标主导元素，消除冗余逻辑
        ElementType targetDominant = ElementUtils.getConsistentAttackElement(target);
        float restraintMultiplier = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);
        Debug.logRestraint(attackElement, targetDominant, restraintMultiplier);

        float attackPart = rawElementalDamage * (float) ElementalConfig.elementalDamageMultiplier * wetnessMultiplier * restraintMultiplier * fireVulnMultiplier;
        float finalElementalDmg;
        boolean isFloored = false;

        double minPercent = ElementalConfig.restraintMinDamagePercent;
        float benchmark = (float) ElementalConfig.getMaxStatCap();

        if (restraintMultiplier > 1.0f && resistancePoints >= benchmark) {
            float resistRatio = Math.min(resistancePoints / benchmark, 1.0f);
            double maxReductionAllowed = 1.0 - minPercent;
            double actualReduction = resistRatio * maxReductionAllowed;
            finalElementalDmg = attackPart * (float) (1.0 - actualReduction);
            isFloored = true;
            Debug.logFloorProtection(target, resistancePoints, benchmark, actualReduction, finalElementalDmg);
        } else {
            float defensePart = rawResistReduction * (float) ElementalConfig.elementalResistanceMultiplier;
            finalElementalDmg = Math.max(0.0f, attackPart - defensePart);
        }

        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        DebugCommand.sendCombatLog(
                attacker, target, directEntity,
                physicalDamage, rawElementalDamage, rawResistReduction,
                ElementalConfig.elementalDamageMultiplier, ElementalConfig.elementalResistanceMultiplier,
                restraintMultiplier, wetnessMultiplier,
                finalElementalDmg, totalDamage,
                isFloored, minPercent, wetnessLevel
        );

        if (attackElement == ElementType.FIRE) {
            tryTriggerScorched(attacker, target, enhancementPoints);
        } else if (attackElement == ElementType.NATURE) {
            if (ElementUtils.getConsistentAttackElement(target) == ElementType.THUNDER) {
                net.minecraft.world.effect.MobEffect spore = SPORES_EFFECT.get();
                if (spore == null || !target.hasEffect(spore)) {
                    Debug.logNatureCounterNoSpores(attacker, target);
                    return;
                }

                long currentGameTime = attacker.level().getGameTime();
                long cooldownEndTime = attacker.getPersistentData().getLong(NBT_NATURE_ATTACK_COOLDOWN);
                if (currentGameTime < cooldownEndTime) {
                    return;
                }

                double baseChance = ElementalThunderFrostReactionsConfig.natureAttackTriggerBaseChance;
                int thunderEnhance = ElementUtils.getDisplayEnhancement(target, ElementType.THUNDER);
                int threshold = ElementalThunderFrostReactionsConfig.thunderEnhanceThreshold;
                int steps = Math.max(0, (thunderEnhance - threshold) / threshold);
                double bonusChance = steps * ElementalThunderFrostReactionsConfig.thunderEnhanceChancePerStep;
                double totalChance = Math.min(1.0, baseChance + bonusChance);

                boolean success = RANDOM.nextDouble() < totalChance;
                Debug.logNatureCounter(attacker, target, totalChance, success);

                if (success) {
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
                                    paralysisEffect, ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks * paralysisStacks, paralysisStacks - 1));
                        }
                        reactionTarget.removeEffect(wetnessEffect);
                        Debug.logNatureCounterEffect(reactionTarget, "麻痹", paralysisStacks);
                    } else {
                        int staticStacks = ElementalThunderFrostReactionsConfig.staticStacksWhenNoWetness;
                        net.minecraft.world.effect.MobEffect staticEffect = STATIC_SHOCK_EFFECT.get();
                        if (staticEffect != null) {
                            reactionTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    staticEffect, ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks * staticStacks, staticStacks - 1));
                        }
                        Debug.logNatureCounterEffect(reactionTarget, "静电", staticStacks);
                    }

                    reactionTarget.level().playSound(null, reactionTarget.getX(), reactionTarget.getY(), reactionTarget.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 1.0f);
                    reactionTarget.getPersistentData().putLong(NBT_NATURE_ATTACK_COOLDOWN, reactionTarget.level().getGameTime() + ElementalThunderFrostReactionsConfig.natureAttackCooldownTicks);
                }
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

        Debug.logScorchedTrigger(attacker, target, firePower, totalChance, triggered);

        if (triggered) {
            int duration = ElementalFireNatureReactionsConfig.scorchedDuration;
            ScorchedHandler.applyScorched(target, attacker, firePower, duration, firePower);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    private static final class Debug {
        private static void logSporePhysResist(LivingEntity target, int stacks, float reduction, float original, float newDamage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "孢子物理减免", String.format("%s 孢子层数 %d，减免 %.1f%%，伤害 %.2f -> %.2f", target.getName().getString(), stacks, reduction * 100, original, newDamage));
        }

        private static void logFireVuln(LivingEntity target, int stacks, float multiplier) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "火焰易伤", String.format("%s 孢子层数 %d，火焰伤害倍率 %.2f", target.getName().getString(), stacks, multiplier));
        }

        private static void logSelfDry(LivingEntity attacker, int oldLevel, int newLevel, int removed, int firePower) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "自我干燥", String.format("%s 潮湿 %d -> %d (移除 %d 层)，赤焰点数 %d", attacker.getName().getString(), oldLevel, newLevel, removed, firePower));
        }

        private static void logWetnessSnapshot(LivingEntity target, int wetness) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "潮湿快照", String.format("%s 从快照中读取到潮湿层数 %d", target.getName().getString(), wetness));
        }

        private static void logWetnessEffect(LivingEntity target, int wetness, float reduction, float multiplier) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "潮湿减伤", String.format("%s 潮湿层数 %d，减免 %.1f%%，最终伤害倍率 %.2f", target.getName().getString(), wetness, reduction * 100, multiplier));
        }

        private static void logSelfDryPenalty(LivingEntity attacker, float penalty, float newMultiplier) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "自我干燥惩罚", String.format("%s 触发自我干燥惩罚 (x%.2f)，最终潮湿倍率 %.2f", attacker.getName().getString(), penalty, newMultiplier));
        }

        private static void logRestraint(ElementType attack, ElementType targetDominant, float multiplier) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(null, "克制计算", String.format("攻击元素 %s，目标主导元素 %s，克制倍率 %.2f", attack, targetDominant, multiplier));
        }

        private static void logFloorProtection(LivingEntity target, int resistPoints, float benchmark, double reduction, float finalDamage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "保底保护", String.format("%s 抗性 %d >= 基准 %.0f，额外减免 %.1f%%，最终属性伤害 %.2f", target.getName().getString(), resistPoints, benchmark, reduction * 100, finalDamage));
        }

        private static void logNatureCounter(LivingEntity attacker, LivingEntity target, double chance, boolean success) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "自然反击", String.format("%s 反击 %s：触发概率 %.1f%%，结果 %s", attacker.getName().getString(), target.getName().getString(), chance * 100, success ? "§a成功" : "§c失败"));
        }

        private static void logNatureCounterNoSpores(LivingEntity attacker, LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "自然反击", String.format("%s 攻击 %s：目标没有易燃孢子，不触发反击", attacker.getName().getString(), target.getName().getString()));
        }

        private static void logNatureCounterEffect(LivingEntity target, String effectName, int stacks) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "自然反击效果", String.format("%s 获得 %s 层数 %d", target.getName().getString(), effectName, stacks));
        }

        private static void logScorchedTrigger(LivingEntity attacker, LivingEntity target, int firePower, double chance, boolean triggered) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "灼烧触发", String.format("%s 尝试灼烧 %s：赤焰点数 %d，概率 %.1f%%，结果 %s", attacker.getName().getString(), target.getName().getString(), firePower, chance * 100, triggered ? "§c成功" : "§a未触发"));
        }
    }
}
