// src/main/java/com/xulai/elementalcraft/event/CombatEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;
import java.util.Random;

/**
 * CombatEvents
 * <p>
 * 中文说明：
 * 负责处理实体受到伤害时的元素属性伤害计算逻辑。
 * 实现了自定义的伤害公式：(属性伤害 * 全局倍率 * 克制倍率 * 潮湿/蒸汽修正) - (抗性抵消 * 抗性倍率)。
 * <p>
 * V1.5.0 变更：
 * 1. 新增易燃孢子(Flammable Spores)的伤害修正逻辑（物理硬化/火焰易伤）。
 * 2. [Removed] 移除了自然属性对潮湿目标的伤害加成机制。
 * 3. 优化了类型安全性。
 * <p>
 * English description:
 * Handles elemental damage calculation when an entity takes damage.
 * Implements custom damage formula.
 * <p>
 * V1.5.0 Changes:
 * 1. Added Flammable Spores damage modification logic (Physical Resist / Fire Vuln).
 * 2. [Removed] Removed damage bonus mechanism for Nature element against wet targets.
 * 3. Improved type safety.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    private static final String NBT_WETNESS = "EC_WetnessLevel";
    private static final float MAX_RESISTANCE_BENCHMARK = 100.0f;
    
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float currentDamage = event.getAmount();

        // ==================== [V1.5] 易燃孢子伤害修正 (Spore Modifications) ====================
        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            MobEffectInstance effect = target.getEffect(ModMobEffects.SPORES.get());
            int stacks = (effect != null) ? (effect.getAmplifier() + 1) : 0;

            if (stacks > 0) {
                // A. 火焰易伤 (Debuff) - 随层数叠加
                if (source.is(DamageTypeTags.IS_FIRE)) {
                    float vulnPerStack = (float) ElementalReactionConfig.sporeFireVulnPerStack;
                    float multiplier = 1.0f + (stacks * vulnPerStack);
                    currentDamage *= multiplier;
                }
                // B. 物理硬化 (Buff)
                // 排除火焰、魔法、间接魔法和爆炸，剩下的主要为物理伤害
                else if (!source.is(DamageTypeTags.IS_FIRE) 
                        && !source.is(DamageTypes.MAGIC) 
                        && !source.is(DamageTypes.INDIRECT_MAGIC) 
                        && !source.is(DamageTypeTags.IS_EXPLOSION)) {
                    
                    float resistPerStack = (float) ElementalReactionConfig.sporePhysResist;
                    float totalResist = Math.min(0.9f, stacks * resistPerStack); // Cap at 90%
                    currentDamage *= (1.0f - totalResist);
                }
                
                // 应用修正后的基础伤害
                event.setAmount(currentDamage);
            }
        }

        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // ==================== 1. 确定攻击使用的物品 ====================
        ItemStack weaponStack = ItemStack.EMPTY;
        Entity directEntity = source.getDirectEntity();

        if (directEntity instanceof ThrownTrident trident) {
            try {
                CompoundTag tag = new CompoundTag();
                trident.addAdditionalSaveData(tag);
                if (tag.contains("Trident", 10)) {
                    weaponStack = ItemStack.of(tag.getCompound("Trident"));
                }
            } catch (Exception ignored) {}
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

        // ==================== 2. 获取攻击元素属性 ====================
        ElementType attackElement = ElementUtils.getAttackElement(weaponStack);
        if (attackElement == ElementType.NONE) {
            return;
        }

        // ==================== 3. 获取基础数值 ====================
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

        // ==================== 4. 潮湿、蒸汽与自然反应修正 ====================
        int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);
        
        // 尝试恢复快照 (用于处理瞬间移除潮湿导致伤害计算时状态丢失的问题)
        if (wetnessLevel <= 0) {
            for (String tag : target.getTags()) {
                if (tag.startsWith("EC_WetnessSnapshot_")) {
                    try {
                        wetnessLevel = Integer.parseInt(tag.substring(19)); 
                        target.removeTag(tag); 
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        }

        float wetnessMultiplier = 1.0f;
        float maxCap = (float) ElementalReactionConfig.steamMaxReduction;

        if (wetnessLevel > 0) {
            if (attackElement == ElementType.FIRE) {
                // 赤焰：伤害降低（被水扑灭）
                float reductionPerLevel = (float) ElementalReactionConfig.wetnessFireReduction;
                float finalReduction = Math.min(wetnessLevel * reductionPerLevel, maxCap);
                wetnessMultiplier = 1.0f - finalReduction;
                
            } else if (attackElement == ElementType.THUNDER || attackElement == ElementType.FROST) {
                // 雷霆/冰霜：伤害增加（导电/冻结）
                float increasePerLevel = (float) ElementalReactionConfig.wetnessResistModifier;
                float totalIncrease = wetnessLevel * increasePerLevel;
                wetnessMultiplier = 1.0f + totalIncrease;

            } 
            // [Fix] Removed Nature Element damage bonus logic
            // 自然属性：不再享受潮湿增伤，专注于寄生机制
        }

        // ==================== 5. 自我干燥惩罚检查 ====================
        // 如果攻击者刚触发了自我干燥 (Self-Drying)，本击伤害降低
        if (attacker.getTags().contains(com.xulai.elementalcraft.event.SteamReactionHandler.TAG_SELF_DRYING_PENALTY) 
                && attackElement == ElementType.FIRE) {
            float penalty = 1.0f - (float) ElementalReactionConfig.wetnessSelfDryingDamagePenalty;
            wetnessMultiplier *= penalty;
            attacker.removeTag(com.xulai.elementalcraft.event.SteamReactionHandler.TAG_SELF_DRYING_PENALTY);
        }

        // ==================== 6. 克制关系计算 ====================
        ElementType targetDominant = ElementType.NONE;
        ItemStack targetWeapon = target.getMainHandItem();
        ElementType targetWeaponElement = ElementUtils.getAttackElement(targetWeapon);
        
        if (targetWeaponElement == ElementType.NONE) {
             targetWeapon = target.getOffhandItem();
             targetWeaponElement = ElementUtils.getAttackElement(targetWeapon);
        }

        if (targetWeaponElement != ElementType.NONE) {
            int targetEnhancement = ElementUtils.getDisplayEnhancement(target, targetWeaponElement);
            if (targetEnhancement > 0) {
                targetDominant = targetWeaponElement;
            }
        }

        float restraintMultiplier = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);

        // ==================== 7. 应用最终伤害公式 ====================
        float attackPart = rawElementalDamage
                * (float) ElementalConfig.elementalDamageMultiplier
                * wetnessMultiplier
                * restraintMultiplier;

        float finalElementalDmg;
        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        // 抗性计算 (含保底机制)
        if (restraintMultiplier > 1.0f && resistancePoints >= MAX_RESISTANCE_BENCHMARK) {
            float resistRatio = Math.min(resistancePoints / MAX_RESISTANCE_BENCHMARK, 1.0f);
            double maxReductionAllowed = 1.0 - minPercent;
            double actualReduction = resistRatio * maxReductionAllowed;
            
            finalElementalDmg = attackPart * (float) (1.0 - actualReduction);
            isFloored = true; 
        } else {
            float defensePart = rawResistReduction * (float) ElementalConfig.elementalResistanceMultiplier;
            finalElementalDmg = Math.max(0.0f, attackPart - defensePart);
        }

        // ==================== 8. 应用总伤害并输出调试日志 ====================
        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        DebugCommand.sendCombatLog(
                attacker, target, directEntity,
                physicalDamage,
                rawElementalDamage,
                rawResistReduction,
                ElementalConfig.elementalDamageMultiplier,
                ElementalConfig.elementalResistanceMultiplier,
                restraintMultiplier,
                wetnessMultiplier,
                finalElementalDmg,
                totalDamage,
                isFloored,
                minPercent,
                wetnessLevel
        );

        // ==================== 9. 灼烧触发判定 (Legacy Logic) ====================
        // 注：新的毒火爆燃 (Toxic Blast) 逻辑已接管部分灼烧触发，此处保留作为基础攻击的灼烧判定
        if (attackElement == ElementType.FIRE) {
            tryTriggerScorched(attacker, target, enhancementPoints);
        }
    }

    /**
     * 尝试触发灼烧 (Scorched) 状态。
     */
    private static void tryTriggerScorched(LivingEntity attacker, LivingEntity target, int firePower) {
        if (firePower < ElementalReactionConfig.scorchedTriggerThreshold) return;

        // 双重干燥检查：双方均不可潮湿
        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get())) || 
            attacker.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()))) {
            return; 
        }

        // 非冰霜判定
        if (ElementUtils.getDisplayResistance(target, ElementType.FROST) > 0 || 
            ElementUtils.getDisplayEnhancement(target, ElementType.FROST) > 0) {
            return;
        }

        // 状态互斥检查
        if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            return;
        }

        // 概率计算
        double baseChance = ElementalReactionConfig.scorchedBaseChance;
        double growth = firePower * ElementalReactionConfig.scorchedChancePerPoint;
        double totalChance = Math.min(1.0, baseChance + growth);

        if (RANDOM.nextDouble() < totalChance) {
            int duration = ElementalReactionConfig.scorchedDuration;
            ScorchedHandler.applyScorched(target, firePower, duration);

            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), 
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }
}