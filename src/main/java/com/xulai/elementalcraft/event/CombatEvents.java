// src/main/java/com/xulai/elementalcraft/event/CombatEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
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
 * 实现了自定义的伤害公式：(属性伤害 * 全局倍率 * 克制倍率) - (抗性抵消 * 抗性倍率)。
 * 包含保底机制：当发生克制时，即使抗性很高，也能造成一定比例的保底伤害。
 * 集成了蒸汽反应 (Steam Reaction) 的伤害衰减逻辑。
 *
 * English description:
 * Handles elemental damage calculation when an entity takes damage.
 * Implements custom damage formula: (Elemental Dmg * Global Multiplier * Restraint Multiplier) - (Resist Reduction * Resist Multiplier).
 * Includes a floor mechanism.
 * Integrated Steam Reaction damage reduction logic.
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

        // 特判：投掷的三叉戟，尝试从 NBT 恢复物品数据以获取附魔
        // Special case: Thrown Trident, try to recover item data from NBT to get enchantments
        if (directEntity instanceof ThrownTrident trident) {
            try {
                CompoundTag tag = new CompoundTag();
                trident.addAdditionalSaveData(tag);
                if (tag.contains("Trident", 10)) {
                    weaponStack = ItemStack.of(tag.getCompound("Trident"));
                }
            } catch (Exception ignored) {}
        }

        // 如果不是三叉戟或获取失败，检查主手和副手
        // If not trident or failed, check main hand and offhand
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

        // ==================== 3. 基础数值获取 / Get Base Values ====================

        float physicalDamage = event.getAmount();
        
        // 获取强化点数和抗性点数
        // Get enhancement points and resistance points
        int enhancementPoints = ElementUtils.getTotalEnhancement(attacker, attackElement);
        int resistancePoints = ElementUtils.getTotalResistance(target, attackElement);

        // 获取配置参数：每多少点对应 0.5 伤害/减免
        // Get config parameters: points per 0.5 damage/reduction
        float strengthPerHalf = (float) ElementalConfig.getStrengthPerHalfDamage();
        float resistPerHalf = (float) ElementalConfig.getResistPerHalfReduction();

        // 计算原始伤害和原始抵消量（未乘倍率）
        // Calculate raw damage and raw reduction (without multipliers)
        float rawElementalDamage = (enhancementPoints / strengthPerHalf) * 0.5f;
        float rawResistReduction = (resistancePoints / resistPerHalf) * 0.5f;

        // 如果没有强化伤害，直接跳过后续计算
        // If no enhancement damage, skip further calculations
        if (rawElementalDamage <= 0) return;

        // ==================== 4. 潮湿与反应修正 / Wetness & Reaction Mods ====================
        
        int wetnessLevel = 0;
        float wetnessMultiplier = 1.0f;
        
        // 读取目标潮湿数据
        // Read target wetness data
        if (target.getPersistentData().contains(WetnessHandler.NBT_WETNESS)) {
            wetnessLevel = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        }

        // 4.1 基础潮湿修正 / Basic Wetness Mod
        if (wetnessLevel > 0) {
            float factor = Math.min(0.5f, 0.1f * wetnessLevel); // Max 50%
            
            if (attackElement == ElementType.FIRE) {
                // 赤焰：目标潮湿时，受到的火属性攻击倍率降低
                // Fire: Damage multiplier reduced when target is wet
                wetnessMultiplier = 1.0f - factor;
            } else if (attackElement == ElementType.THUNDER || attackElement == ElementType.FROST) {
                // 雷/冰：目标潮湿时，受到的雷/冰属性攻击倍率增加
                // Thunder/Frost: Damage multiplier increased when target is wet
                wetnessMultiplier = 1.0f + factor;
            }
        }

        // 4.2 蒸汽反应修正 / Steam Reaction Mod
        // 如果开启了蒸汽反应，且是火打湿，额外乘一个衰减系数
        // If steam reaction enabled, Fire vs Wet, apply extra reduction multiplier
        float steamMultiplier = 1.0f;
        if (ElementalReactionConfig.steamReactionEnabled && 
            attackElement == ElementType.FIRE && 
            wetnessLevel > 0) {
            
            // 获取配置中的减伤比例 (默认 0.5)
            // Get reduction percentage from config (default 0.5)
            float reduction = (float) ElementalReactionConfig.steamDamageReduction;
            steamMultiplier = 1.0f - reduction;
        }

        // ==================== 5. 克制关系计算 / Restraint Calculation ====================

        // 确定目标的主导元素（用于判断是否被克制）
        // Determine target's dominant element (to check restraint)
        ElementType targetDominant = ElementType.NONE;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            ElementType dominant = ElementUtils.getDominantElement(stack);
            if (dominant != ElementType.NONE) {
                targetDominant = dominant;
                break;
            }
        }

        // 获取克制倍率（例如 1.5）或微弱倍率（例如 0.5）
        // Get restraint multiplier (e.g. 1.5) or weak multiplier (e.g. 0.5)
        float restraintMultiplier = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);

        // ==================== 6. 最终公式应用 / Apply Final Formula ====================

        // [攻击部分] = 原始伤害 * 全局伤害倍率 * 潮湿修正 * 蒸汽修正 * 克制倍率
        // [Attack Part] = Raw Dmg * Global Dmg Multiplier * Wetness Mod * Steam Mod * Restraint Multiplier
        float attackPart = (float) (rawElementalDamage 
                * ElementalConfig.elementalDamageMultiplier 
                * wetnessMultiplier 
                * steamMultiplier // [蒸汽修正生效点 / Steam Mod Applied Here]
                * restraintMultiplier);

        // [防御部分] = 原始抵消 * 全局抗性倍率
        // [Defense Part] = Raw Reduction * Global Resistance Multiplier
        float defensePart = (float) (rawResistReduction * ElementalConfig.elementalResistanceMultiplier);

        // 初始结果：攻击部分 - 防御部分（不低于0）
        // Initial result: Attack Part - Defense Part (Not less than 0)
        float finalElementalDmg = Math.max(0, attackPart - defensePart);

        // ==================== 7. 保底机制 / Floor Mechanism ====================

        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        // 仅当存在克制优势（倍率 > 1.0）时触发保底判定
        // Trigger floor check only when restraint advantage exists (Multiplier > 1.0)
        if (restraintMultiplier > 1.0f) {
            // 保底阈值 = 攻击部分 * 保底百分比
            // Floor Threshold = Attack Part * Min Percent
            // 逻辑：如果由于抗性过高导致实际伤害低于“攻击部分的50%”，则强制造成50%伤害
            // Logic: If resistance is too high causing damage < 50% of attack part, force 50% damage
            float floorValue = (float) (attackPart * minPercent);

            if (finalElementalDmg < floorValue) {
                finalElementalDmg = floorValue;
                isFloored = true;
            }
        }

        // ==================== 8. 应用与日志 / Apply & Log ====================

        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        // 合并显示潮湿和蒸汽倍率，以便调试日志简洁明了 (例如: 潮湿x0.8 * 蒸汽x0.5 = 最终x0.4)
        // Combine wetness and steam multipliers for concise debug log
        float combinedWetnessMult = wetnessMultiplier * steamMultiplier;

        // 发送调试日志，参数列表已修正以匹配 DebugCommand.sendCombatLog 的新签名
        // Send debug log, parameters corrected to match new signature of DebugCommand.sendCombatLog
        DebugCommand.sendCombatLog(
            attacker, target, directEntity,
            physicalDamage, 
            rawElementalDamage, // 1. 原始强化伤害 (float)
            rawResistReduction, // 2. 原始抗性抵消 (float)
            ElementalConfig.elementalDamageMultiplier,     // 3. 全局伤害倍率 (double)
            ElementalConfig.elementalResistanceMultiplier, // 4. 全局抗性倍率 (double)
            restraintMultiplier, // 5. 克制倍率 (float)
            combinedWetnessMult, // 6. 综合潮湿倍率 (float) [Updated]
            finalElementalDmg,   // 7. 最终属性伤害 (float)
            totalDamage,         // 8. 总伤害 (float)
            isFloored,           // 9. 是否保底 (boolean)
            minPercent,          // 10. 保底百分比 (double)
            wetnessLevel         // 11. 潮湿等级 (int)
        );
    }
}