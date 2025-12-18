// src/main/java/com/xulai/elementalcraft/event/CombatEvents.java
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
 * 支持近战以及所有远程/投掷伤害（弓箭、弩箭、投掷药水、点燃TNT、火焰弹、三叉戟等）。
 *
 * English description:
 * Handles elemental damage calculation when an entity takes damage.
 * Adds extra elemental damage based on the attacker's enhancement, target's resistance, and restraint relationships.
 * Supports melee and all projectile/thrown damage (arrows, bolts, splash potions, primed TNT, fireballs, tridents, etc.).
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    /**
     * 监听 LivingDamageEvent，执行元素伤害计算并添加到总伤害中。
     * 包含对投掷武器（如三叉戟）的特殊处理，确保飞行过程中切换物品不影响伤害计算。
     *
     * Listens to LivingDamageEvent to calculate and add elemental damage to total damage.
     * Includes special handling for thrown weapons (e.g., Tridents) to ensure damage calculation is unaffected by item switching during flight.
     *
     * @param event 实体伤害事件 / Living damage event
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // 获取真正的攻击者（支持所有由活体实体发起的伤害，包括远程）
        // Get the true attacker (supports all damage initiated by living entities, including ranged)
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // ==================== 1. 确定攻击使用的物品 / Determine Attack Item ====================

        ItemStack weaponStack = ItemStack.EMPTY;
        Entity directEntity = source.getDirectEntity(); // 直接造成伤害的实体 / Direct entity causing damage

        // 特判：如果是投掷的三叉戟，从实体 NBT 中读取原本的物品栈
        // Special case: If it is a thrown trident, read the original item stack from the entity NBT
        if (directEntity instanceof ThrownTrident trident) {
            try {
                CompoundTag tag = new CompoundTag();
                trident.addAdditionalSaveData(tag);
                if (tag.contains("Trident", 10)) {
                    weaponStack = ItemStack.of(tag.getCompound("Trident"));
                }
            } catch (Exception ignored) {}
        }

        // 如果不是三叉戟（或是读取失败），则回退到检查攻击者当前手持的物品
        // If not a trident (or reading failed), fallback to checking the item currently held by the attacker
        if (weaponStack.isEmpty()) {
            ItemStack mainHand = attacker.getMainHandItem();
            ItemStack offHand = attacker.getOffhandItem();

            // 优先检查主手是否有元素属性
            // Prioritize checking main hand for elemental attributes
            if (ElementUtils.getAttackElement(mainHand) != ElementType.NONE) {
                weaponStack = mainHand;
            }
            // 如果主手没有，检查副手
            // If main hand has none, check off-hand
            else if (ElementUtils.getAttackElement(offHand) != ElementType.NONE) {
                weaponStack = offHand;
            }
        }

        // ==================== 2. 获取攻击元素属性 / Get Attack Element ====================

        ElementType attackElement = ElementUtils.getAttackElement(weaponStack);

        // 如果没有元素攻击属性，直接返回，不进行后续计算
        // If no elemental attack attribute, return immediately, skipping further calculation
        if (attackElement == ElementType.NONE) {
            return;
        }

        // ==================== 3. 伤害数值计算 / Damage Calculation ====================

        // 记录原始物理伤害
        // Record original physical damage
        float physicalDamage = event.getAmount();

        // 获取攻击者的元素强化点数和目标的元素抗性点数
        // Get attacker's enhancement points and target's resistance points
        int enhancementPoints = ElementUtils.getTotalEnhancement(attacker, attackElement);
        int resistancePoints = ElementUtils.getTotalResistance(target, attackElement);

        // 使用静态方法获取配置值以支持热重载，并强制转换为 float 避免整数除法精度丢失
        // Use static methods for config values to support hot-reload, cast to float to prevent integer division precision loss
        float strengthPerHalf = (float) ElementalConfig.getStrengthPerHalfDamage();
        float resistPerHalf = (float) ElementalConfig.getResistPerHalfReduction();

        // 基础元素伤害计算
        // Base elemental damage calculation
        float preResistDmg = (enhancementPoints / strengthPerHalf) * 0.5f;

        // 抗性减免计算
        // Resistance reduction calculation
        float resistReduction = (resistancePoints / resistPerHalf) * 0.5f;

        // 计算抗性减免后的伤害（最小值为 0）
        // Calculate damage after resistance reduction (minimum 0)
        float elementalDmg = Math.max(0, preResistDmg - resistReduction);

        // 应用全局元素伤害倍率配置（使用静态缓存）
        // Apply global elemental damage multiplier configuration (using static cache)
        elementalDmg *= ElementalConfig.elementalDamageMultiplier;

        // ==================== 4. 克制关系计算 / Restraint Calculation ====================

        // 获取目标的主导元素，用于判断是否触发克制
        // Get target's dominant element to determine if restraint is triggered
        ElementType targetDominant = ElementType.NONE;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            ElementType dominant = ElementUtils.getDominantElement(stack);
            if (dominant != ElementType.NONE) {
                targetDominant = dominant;
                break;
            }
        }

        // 计算克制倍率并应用
        // Calculate and apply restraint multiplier
        float restraintMultiplier = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);
        float finalElementalDmg = elementalDmg * restraintMultiplier;

        // ==================== 5. 最低伤害保底机制 / Floor Mechanism ====================

        boolean isFloored = false;
        // 使用静态缓存获取保底百分比配置
        // Use static cache for floor percentage config
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        // 仅在触发克制（倍率 > 1.0）时检查保底逻辑
        // Only check floor logic when restraint is triggered (multiplier > 1.0)
        if (restraintMultiplier > 1.0f) {
            // 计算保底伤害
            // Calculate floor damage
            float minRetained = (float) (preResistDmg * minPercent);

            // 如果最终伤害低于保底值，则强制提升至保底值
            // If final damage is below floor value, force it up to floor value
            if (finalElementalDmg < minRetained) {
                finalElementalDmg = minRetained;
                isFloored = true;
            }
        }

        // ==================== 6. 应用伤害与日志 / Apply & Log ====================

        // 将最终计算出的元素伤害叠加到事件的总伤害中
        // Add final calculated elemental damage to the event's total damage
        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        // 发送详细调试日志
        // Send detailed debug log
        DebugCommand.sendCombatLog(
            attacker, target, directEntity,
            physicalDamage, preResistDmg, resistReduction,
            restraintMultiplier, finalElementalDmg, totalDamage,
            isFloored, minPercent
        );
    }
}