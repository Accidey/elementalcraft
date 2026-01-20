// src/main/java/com/xulai/elementalcraft/event/CombatEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.eventbus.api.EventPriority; // [Add] Import EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;
import java.util.Random;

/**
 * CombatEvents
 * <p>
 * 中文说明：
 * 负责处理实体受到伤害时的元素属性伤害计算逻辑。
 * 实现了自定义的伤害公式，并集成了易燃孢子修正、自我干燥机制、元素克制及环境反应逻辑。
 * <p>
 * English description:
 * Handles elemental damage calculation when an entity takes damage.
 * Implements custom damage formula, integrating flammable spores modification, self-drying mechanism, elemental restraint, and environmental reaction logic.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    private static final String NBT_WETNESS = "EC_WetnessLevel";
    private static final String NBT_LAST_DRY_TICK = "EC_LastSelfDryTick"; 
    private static final float MAX_RESISTANCE_BENCHMARK = 100.0f;
    
    private static final Random RANDOM = new Random();

    /**
     * 核心伤害处理方法。
     * 计算并应用元素伤害、抗性抵消、克制倍率以及特殊状态（孢子、潮湿、自我干燥）的修正。
     * 优先级设置为 HIGH，确保在 ReactionHandler 移除孢子之前执行物理硬化判定。
     * <p>
     * Core damage handling method.
     * Calculates and applies elemental damage, resistance reduction, restraint multipliers, 
     * and modifications from special states (Spores, Wetness, Self-Drying).
     * Priority set to HIGH to ensure physical hardening check runs before ReactionHandler removes spores.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        // 服务端检查：防止客户端重复运行逻辑
        // Server-side check: Prevent client-side logic duplication
        if (event.getEntity().level().isClientSide) return;

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float currentDamage = event.getAmount();

        // --------------------------------------------------------
        // 1. 易燃孢子物理硬化修正 (Spore Physical Hardening)
        // --------------------------------------------------------
        // 检查目标是否感染了易燃孢子，如果受到的不是元素/魔法/爆炸伤害，则根据层数提供物理减伤。
        // Check if the target is infected with Flammable Spores. If damage is not elemental/magic/explosion, provide physical resistance based on stacks.
        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            MobEffectInstance effect = target.getEffect(ModMobEffects.SPORES.get());
            int stacks = (effect != null) ? (effect.getAmplifier() + 1) : 0;

            if (stacks > 0) {
                // 排除各类非物理伤害源，确保只对物理攻击生效
                // Exclude various non-physical damage sources to ensure it applies only to physical attacks
                if (!source.is(DamageTypeTags.IS_FIRE) 
                        && !source.is(DamageTypes.MAGIC) 
                        && !source.is(DamageTypes.INDIRECT_MAGIC) 
                        && !source.is(DamageTypeTags.IS_EXPLOSION)
                        && !source.is(DamageTypes.WITHER)) { 
                    
                    float resistPerStack = (float) ElementalReactionConfig.sporePhysResist;
                    float totalResist = Math.min(0.9f, stacks * resistPerStack); // Cap at 90%
                    currentDamage *= (1.0f - totalResist);
                }
                
                event.setAmount(currentDamage);
            }
        }

        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // --------------------------------------------------------
        // 2. 武器与攻击判定 (Weapon & Attack Determination)
        // --------------------------------------------------------
        // 确定攻击者使用的物品（主手、副手或三叉戟），用于后续获取元素属性
        // Determine the item used by the attacker (Main Hand, Off Hand, or Trident) to retrieve elemental attributes
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

        ElementType attackElement = ElementUtils.getAttackElement(weaponStack);
        if (attackElement == ElementType.NONE) {
            return;
        }

        // --------------------------------------------------------
        // 3. 孢子火属性易伤 (Spore Fire Vulnerability)
        // --------------------------------------------------------
        // 如果攻击属性为赤焰且目标感染了孢子，根据孢子层数增加受到的伤害。
        // 这是一个双刃剑效果：孢子提供物理抗性的同时增加火属性易伤。
        // If the attack element is Fire and the target is infected with spores, increase damage based on spore stacks.
        // This is a double-edged sword effect: Spores provide physical resistance but increase fire vulnerability.
        if (attackElement == ElementType.FIRE && target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            MobEffectInstance spore = target.getEffect(ModMobEffects.SPORES.get());
            int stacks = (spore != null) ? (spore.getAmplifier() + 1) : 0;
            
            if (stacks > 0) {
                double vulnPerStack = ElementalReactionConfig.sporeFireVulnPerStack;
                float vulnMultiplier = 1.0f + (float)(stacks * vulnPerStack);
                currentDamage *= vulnMultiplier;
                event.setAmount(currentDamage);
            }
        }

        // --------------------------------------------------------
        // 4. 自我干燥机制 (Self-Drying Mechanism)
        // --------------------------------------------------------
        // 如果攻击者是赤焰属性且自身潮湿，利用高温蒸发自身水分。
        // 根据赤焰强度消耗潮湿层数，并为后续逻辑标记"自我干燥惩罚"。
        // If attacker is Fire element and Wet, evaporate own moisture using high heat.
        // Consumes wetness layers based on Fire Power and tags "Self-Drying Penalty" for later logic.
        if (attackElement == ElementType.FIRE) {
            CompoundTag attackerData = attacker.getPersistentData();
            int attackerWetness = attackerData.getInt(NBT_WETNESS);
            
            if (attackerWetness > 0) {
                // Tick级防抖：防止同一 Tick 内因多重伤害判定导致重复扣除层数
                // Tick-level Debounce: Prevent duplicate layer deduction due to multiple damage checks in the same tick
                long currentTick = attacker.level().getGameTime();
                long lastDryTick = attackerData.getLong(NBT_LAST_DRY_TICK);
                
                if (currentTick != lastDryTick) {
                    int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
                    int threshold = Math.max(1, ElementalReactionConfig.wetnessDryingThreshold);
                    
                    // 计算可移除的层数 (向下取整)
                    // Calculate layers to remove (floor)
                    int layersToRemove = firePower / threshold;
                    
                    if (layersToRemove > 0) {
                        int newLevel = Math.max(0, attackerWetness - layersToRemove);
                        int actuallyRemoved = attackerWetness - newLevel;
                        
                        // 更新 NBT
                        // Update NBT
                        attackerData.putInt(NBT_WETNESS, newLevel);
                        attackerData.putLong(NBT_LAST_DRY_TICK, currentTick); // 更新防抖时间戳
                        
                        // 如果潮湿归零，移除药水效果（视觉同步）
                        // If wetness reaches zero, remove potion effect (visual sync)
                        if (newLevel == 0 && attacker.hasEffect(ModMobEffects.WETNESS.get())) {
                            attacker.removeEffect(ModMobEffects.WETNESS.get());
                        }
                        
                        // 恢复视觉特效：播放蒸汽爆发粒子
                        // Restore visual effects: Play steam burst particles
                        int maxBurstLevel = ElementalReactionConfig.steamHighHeatMaxLevel;
                        EffectHelper.playSteamBurst((ServerLevel) attacker.level(), attacker, 0.5f, Math.min(layersToRemove, maxBurstLevel), true);

                        // 添加惩罚标记，供后续步骤使用
                        // Add penalty tag for use in subsequent steps
                        attacker.addTag(SteamReactionHandler.TAG_SELF_DRYING_PENALTY);
                        
                        // 发送调试日志
                        // Send Debug Log
                        DebugCommand.sendDryLog(attacker, attackerWetness, newLevel, actuallyRemoved, firePower);
                    }
                } else {
                    // 如果是同一 Tick 的重复判定，不扣除层数，但依然添加惩罚标记，以保证本次伤害计算正确应用减伤
                    // If repeated check in the same tick, do not deduct layers, but add penalty tag to ensure damage reduction applies
                    attacker.addTag(SteamReactionHandler.TAG_SELF_DRYING_PENALTY);
                }
            }
        }

        // --------------------------------------------------------
        // 5. 基础元素数据获取 (Base Elemental Data Retrieval)
        // --------------------------------------------------------
        // 获取攻击者的元素属性、强化点数以及目标的抗性点数
        // Retrieve attacker's elemental attribute, enhancement points, and target's resistance points
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

        // --------------------------------------------------------
        // 6. 目标潮湿修正 (Target Wetness Modification)
        // --------------------------------------------------------
        // 根据受击者的潮湿层数调整伤害（如水克火）。
        // Adjust damage based on victim's wetness level (e.g., Water dampens Fire).
        int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);
        
        // 尝试从快照恢复潮湿等级
        // Try to recover wetness level from snapshot
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
            // 赤焰属性：被水扑灭，伤害降低
            // Fire Element: Extinguished by water, damage reduced
            if (attackElement == ElementType.FIRE) {
                float reductionPerLevel = (float) ElementalReactionConfig.wetnessFireReduction;
                float finalReduction = Math.min(wetnessLevel * reductionPerLevel, maxCap);
                wetnessMultiplier = 1.0f - finalReduction;
            }
        }

        // --------------------------------------------------------
        // 7. 自我干燥惩罚应用 (Self-Drying Penalty Application)
        // --------------------------------------------------------
        // 如果在步骤 4 中触发了自我干燥，此处读取标记并降低本次伤害。
        // If self-drying was triggered in Step 4, read the tag here and reduce damage.
        if (attacker.getTags().contains(SteamReactionHandler.TAG_SELF_DRYING_PENALTY) 
                && attackElement == ElementType.FIRE) {
            float penalty = 1.0f - (float) ElementalReactionConfig.wetnessSelfDryingDamagePenalty;
            wetnessMultiplier *= penalty;
            
            // 移除标记，防止影响后续逻辑
            // Remove tag to prevent affecting subsequent logic
            attacker.removeTag(SteamReactionHandler.TAG_SELF_DRYING_PENALTY);
        }

        // --------------------------------------------------------
        // 8. 克制关系计算 (Restraint Calculation)
        // --------------------------------------------------------
        // 比较攻击元素与目标主手/副手武器的元素，计算克制倍率
        // Compare attack element with target's weapon element to calculate restraint multiplier
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

        // --------------------------------------------------------
        // 9. 最终伤害公式应用 (Final Damage Application)
        // --------------------------------------------------------
        // 综合所有系数计算最终的元素伤害，并处理防御抵消逻辑
        // Calculate final elemental damage combining all factors and handle defense reduction logic
        float attackPart = rawElementalDamage
                * (float) ElementalConfig.elementalDamageMultiplier
                * wetnessMultiplier
                * restraintMultiplier;

        float finalElementalDmg;
        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        // 抗性穿透与保底机制
        // Resistance penetration & floor mechanism
        if (restraintMultiplier > 1.0f && resistancePoints >= MAX_RESISTANCE_BENCHMARK) {
            float resistRatio = Math.min(resistancePoints / MAX_RESISTANCE_BENCHMARK, 1.0f);
            double maxReductionAllowed = 1.0 - minPercent;
            double actualReduction = resistRatio * maxReductionAllowed;
            
            finalElementalDmg = attackPart * (float) (1.0 - actualReduction);
            isFloored = true; 
        } else {
            // 常规减伤逻辑
            // Standard reduction logic
            float defensePart = rawResistReduction * (float) ElementalConfig.elementalResistanceMultiplier;
            finalElementalDmg = Math.max(0.0f, attackPart - defensePart);
        }

        // --------------------------------------------------------
        // 10. 应用伤害与日志 (Apply Damage & Logging)
        // --------------------------------------------------------
        // 将计算出的元素伤害附加到原始物理伤害上，并输出调试信息
        // Add calculated elemental damage to raw physical damage and output debug info
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

        // --------------------------------------------------------
        // 11. 灼烧判定 (Scorched Trigger)
        // --------------------------------------------------------
        // 判定赤焰属性攻击是否触发独立的灼烧效果
        // Determine if Fire attribute attack triggers the standalone Scorched effect
        if (attackElement == ElementType.FIRE) {
            tryTriggerScorched(attacker, target, enhancementPoints);
        }
    }

    /**
     * 灼烧效果触发判定辅助方法。
     * 检查前置条件（非冰霜、未潮湿、未免疫等）并根据概率应用灼烧。
     * <p>
     * Helper method for Scorched effect trigger.
     * Checks preconditions (Not Frost, Not Wet, Not Immune, etc.) and applies Scorched based on probability.
     */
    private static void tryTriggerScorched(LivingEntity attacker, LivingEntity target, int firePower) {
        if (firePower < ElementalReactionConfig.scorchedTriggerThreshold) return;

        // 双重干燥检查：双方均不可潮湿
        // Double Dry Check: Neither party can be wet
        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get())) || 
            attacker.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()))) {
            return; 
        }

        // 非冰霜判定：目标具有冰霜抗性或强化时不触发
        // Non-Frost Check: Does not trigger if target has Frost resistance or enhancement
        if (ElementUtils.getDisplayResistance(target, ElementType.FROST) > 0 || 
            ElementUtils.getDisplayEnhancement(target, ElementType.FROST) > 0) {
            return;
        }

        // 状态互斥检查：已灼烧则不叠加
        // Mutex Check: Do not stack if already scorched
        if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            return;
        }

        // 概率计算与应用
        // Probability calculation and application
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