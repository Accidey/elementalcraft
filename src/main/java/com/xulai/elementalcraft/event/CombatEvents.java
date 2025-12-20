package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * CombatEvents
 *
 * 中文说明：
 * 负责处理实体受到伤害时的元素属性伤害计算逻辑。
 * 当攻击者拥有元素攻击属性时，会根据攻击者的强化、目标的抗性以及克制关系计算并添加额外元素伤害。
 * 支持近战以及所有远程/投掷伤害。
 * 集成了潮湿系统逻辑：攻击雷/冰属性时伤害增加，火属性时伤害减少。
 *
 * English description:
 * Handles elemental damage calculation when an entity takes damage.
 * Adds extra elemental damage based on the attacker's enhancement, target's resistance, and restraint relationships.
 * Supports melee and all projectile/thrown damage.
 * Integrated Wetness System logic: Increased damage for Thunder/Frost, decreased for Fire.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    /**
     * 监听 LivingDamageEvent，执行元素伤害计算并添加到总伤害中。
     *
     * Listens to LivingDamageEvent to calculate and add elemental damage to total damage.
     *
     * @param event 实体伤害事件 / Living damage event
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // 获取真正的攻击者
        // Get the true attacker
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // ==================== 1. 确定攻击使用的物品 / Determine Attack Item ====================

        ItemStack weaponStack = ItemStack.EMPTY;
        Entity directEntity = source.getDirectEntity();

        // 特判：投掷的三叉戟
        // Special case: Thrown Trident
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

        // ==================== 2. 获取攻击元素属性 / Get Attack Element ====================

        ElementType attackElement = ElementUtils.getAttackElement(weaponStack);

        if (attackElement == ElementType.NONE) {
            return;
        }

        // ==================== 3. 基础数值计算 / Base Calculation ====================

        float physicalDamage = event.getAmount();
        int enhancementPoints = ElementUtils.getTotalEnhancement(attacker, attackElement);
        int resistancePoints = ElementUtils.getTotalResistance(target, attackElement);

        float strengthPerHalf = (float) ElementalConfig.getStrengthPerHalfDamage();
        float resistPerHalf = (float) ElementalConfig.getResistPerHalfReduction();

        // 基础元素伤害 (强化 - 抗性)
        // Base elemental damage (Enhancement - Resistance)
        float preResistDmg = (enhancementPoints / strengthPerHalf) * 0.5f;
        float resistReduction = (resistancePoints / resistPerHalf) * 0.5f;
        float elementalDmg = Math.max(0, preResistDmg - resistReduction);

        // 应用全局倍率
        // Apply global multiplier
        elementalDmg *= ElementalConfig.elementalDamageMultiplier;

        // ==================== 4. 潮湿效果修正 / Wetness Modification ====================
        // 读取目标潮湿等级，并根据元素类型调整伤害
        // Read target wetness level and adjust damage based on element type
        
        int wetnessLevel = 0;
        float wetnessMultiplier = 1.0f; // 默认为 1.0 (无变化) / Default 1.0 (No change)
        
        // 使用 WetnessHandler 中的常量键名读取 NBT
        // Read NBT using constant keys from WetnessHandler
        if (target.getPersistentData().contains(WetnessHandler.NBT_WETNESS)) {
            wetnessLevel = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        }

        if (wetnessLevel > 0) {
            // 计算因子：每层 10%，最高 50%
            // Calculate factor: 10% per level, max 50%
            float factor = Math.min(0.5f, 0.1f * wetnessLevel);
            
            if (attackElement == ElementType.FIRE) {
                // 赤焰：伤害减少 (抗性增加)
                // Fire: Damage Reduced (Resistance Increased)
                wetnessMultiplier = 1.0f - factor;
            } else if (attackElement == ElementType.THUNDER || attackElement == ElementType.FROST) {
                // 雷霆/冰霜：伤害增加 (易伤)
                // Thunder/Frost: Damage Increased (Vulnerability)
                wetnessMultiplier = 1.0f + factor;
            }
        }
        
        // 应用潮湿倍率
        // Apply wetness multiplier
        elementalDmg *= wetnessMultiplier;

        // ==================== 5. 克制关系计算 / Restraint Calculation ====================

        ElementType targetDominant = ElementType.NONE;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            ElementType dominant = ElementUtils.getDominantElement(stack);
            if (dominant != ElementType.NONE) {
                targetDominant = dominant;
                break;
            }
        }

        float restraintMultiplier = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);
        float finalElementalDmg = elementalDmg * restraintMultiplier;

        // ==================== 6. 保底机制 / Floor Mechanism ====================

        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        if (restraintMultiplier > 1.0f) {
            // 计算保底 (基于未受潮湿修正前的基础强化伤害)
            // Calculate floor (Based on base enhancement damage before wetness modification)
            float minRetained = (float) (preResistDmg * minPercent);

            if (finalElementalDmg < minRetained) {
                finalElementalDmg = minRetained;
                isFloored = true;
            }
        }

        // ==================== 7. 应用与日志 / Apply & Log ====================

        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        // 发送详细调试日志 (添加了潮湿相关参数)
        // Send detailed debug log (Added wetness parameters)
        DebugCommand.sendCombatLog(
            attacker, target, directEntity,
            physicalDamage, preResistDmg, resistReduction,
            restraintMultiplier, finalElementalDmg, totalDamage,
            isFloored, minPercent,
            wetnessLevel, wetnessMultiplier // 传递潮湿数据 / Pass wetness data
        );
    }
}