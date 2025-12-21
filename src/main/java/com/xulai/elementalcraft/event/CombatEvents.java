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
 * 实现了自定义的伤害公式：(属性伤害 * 全局倍率 * 克制倍率 * 潮湿/蒸汽修正) - (抗性抵消 * 抗性倍率)。
 * 包含保底机制：当发生克制时，即使抗性很高，也能造成一定比例的保底伤害。
 * 集成了蒸汽反应 (Steam Reaction) 的伤害衰减逻辑。
 * 强化/抗性点数使用显示用总点数（所有装备累加，不受单件上限影响）。
 *
 * English description:
 * Handles elemental damage calculation when an entity takes damage.
 * Implements custom damage formula: (Elemental Dmg * Global Multiplier * Restraint Multiplier * Wetness/Steam Mod) - (Resist Reduction * Resist Multiplier).
 * Includes a floor mechanism.
 * Integrated Steam Reaction damage reduction logic.
 * Enhancement/resistance points use display total points (sum of all equipment, no per-piece cap).
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    private static final String NBT_WETNESS = "EC_WetnessLevel";

    /**
     * 监听 LivingDamageEvent，在物理伤害基础上计算并添加元素属性伤害。
     *
     * @param event 实体伤害事件
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // 仅处理由生物造成的伤害
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // ==================== 1. 确定攻击使用的物品 ====================
        ItemStack weaponStack = ItemStack.EMPTY;
        Entity directEntity = source.getDirectEntity();

        // 处理投掷的三叉戟，从 NBT 恢复物品信息以获取附魔
        if (directEntity instanceof ThrownTrident trident) {
            try {
                CompoundTag tag = new CompoundTag();
                trident.addAdditionalSaveData(tag);
                if (tag.contains("Trident", 10)) {
                    weaponStack = ItemStack.of(tag.getCompound("Trident"));
                }
            } catch (Exception ignored) {}
        }

        // 若未获取到三叉戟信息，检查主手与副手
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
        float physicalDamage = event.getAmount();

        // 获取攻击者对应元素的强化总点数（所有护甲累加）
        int enhancementPoints = ElementUtils.getDisplayEnhancement(attacker, attackElement);
        // 获取目标对应元素的抗性总点数（所有护甲累加）
        int resistancePoints = ElementUtils.getDisplayResistance(target, attackElement);

        // 配置值：每多少点强化对应增加 0.5 点属性伤害 / 每多少点抗性对应减少 0.5 点伤害
        int strengthPerHalfDamage = ElementalConfig.getStrengthPerHalfDamage();
        int resistPerHalfReduction = ElementalConfig.getResistPerHalfReduction();

        // 计算原始属性伤害与原始抗性减免（每 strength_per_half_damage 点强化产生 0.5 伤害）
        float rawElementalDamage = enhancementPoints / (float) strengthPerHalfDamage * 0.5f;
        float rawResistReduction = resistancePoints / (float) resistPerHalfReduction * 0.5f;

        // 若无属性伤害则直接返回
        if (rawElementalDamage <= 0.0f) {
            return;
        }

        // ==================== 4. 潮湿与蒸汽反应修正 ====================
        int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);

        float wetnessMultiplier = 1.0f;
        if (wetnessLevel > 0) {
            float factor = Math.min(0.5f, 0.1f * wetnessLevel);

            if (attackElement == ElementType.FIRE) {
                wetnessMultiplier = 1.0f - factor;
            } else if (attackElement == ElementType.THUNDER || attackElement == ElementType.FROST) {
                wetnessMultiplier = 1.0f + factor;
            }
        }

        float steamMultiplier = 1.0f;
        if (ElementalReactionConfig.steamReactionEnabled &&
            attackElement == ElementType.FIRE && wetnessLevel > 0) {
            float reduction = (float) ElementalReactionConfig.steamDamageReduction;
            steamMultiplier = 1.0f - reduction;
        }

        // ==================== 5. 克制关系计算 ====================
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

        // ==================== 6. 应用最终伤害公式 ====================
        float attackPart = rawElementalDamage
                * (float) ElementalConfig.elementalDamageMultiplier
                * wetnessMultiplier
                * steamMultiplier
                * restraintMultiplier;

        float defensePart = rawResistReduction * (float) ElementalConfig.elementalResistanceMultiplier;

        float finalElementalDmg = Math.max(0.0f, attackPart - defensePart);

        // ==================== 7. 克制保底机制 ====================
        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        if (restraintMultiplier > 1.0f) {
            float floorValue = (float) (attackPart * minPercent);
            if (finalElementalDmg < floorValue) {
                finalElementalDmg = floorValue;
                isFloored = true;
            }
        }

        // ==================== 8. 应用总伤害并输出调试日志 ====================
        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        float combinedWetnessMult = wetnessMultiplier * steamMultiplier;

        ElementalCraft.LOGGER.info("[EC Combat Debug] enhancementPoints=" + enhancementPoints + 
                           " | strengthPerHalfDamage=" + strengthPerHalfDamage + 
                           " | rawElementalDamage=" + rawElementalDamage);

        DebugCommand.sendCombatLog(
                attacker, target, directEntity,
                physicalDamage,
                rawElementalDamage,
                rawResistReduction,
                ElementalConfig.elementalDamageMultiplier,
                ElementalConfig.elementalResistanceMultiplier,
                restraintMultiplier,
                combinedWetnessMult,
                finalElementalDmg,
                totalDamage,
                isFloored,
                minPercent,
                wetnessLevel
        );
    }
}