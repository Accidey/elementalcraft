// src/main/java/com/xulai/elementalcraft/util/ElementUtils.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * ElementUtils 类提供了一系列静态工具方法，用于查询和计算实体或物品上的元素属性信息。
 * 包括：
 * - 获取武器攻击属性
 * - 获取装备强化/抗性等级及总点数
 * - 获取实体主导元素（用于 Tooltip 前缀显示）
 * - 计算显示用的强化值（带点数上限）
 *
 * ElementUtils class provides a series of static utility methods for querying and calculating elemental attribute information on entities or items.
 * Including:
 * - Get weapon attack element
 * - Get armor enhancement/resistance levels and total points
 * - Get dominant element of an entity (used for tooltip prefix display)
 * - Calculate display enhancement value (with point cap)
 */
public class ElementUtils {

    /**
     * 从物品栈中获取其拥有的攻击元素属性。
     * 仅检查主手武器附魔。
     *
     * Retrieves the attack element attribute from an item stack.
     * Only checks main-hand weapon enchantments.
     *
     * @param stack 物品栈 / Item stack
     * @return 攻击元素类型，若无则返回 NONE / Attack element type, or NONE if none
     */
    public static ElementType getAttackElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ElementType.NONE;

        if (EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FIRE_STRIKE.get(), stack) > 0)    return ElementType.FIRE;
        if (EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.NATURE_STRIKE.get(), stack) > 0) return ElementType.NATURE;
        if (EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FROST_STRIKE.get(), stack) > 0)  return ElementType.FROST;
        if (EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.THUNDER_STRIKE.get(), stack) > 0) return ElementType.THUNDER;
        return ElementType.NONE;
    }

    /**
     * 获取物品栈上指定元素的强化附魔等级。
     *
     * Gets the enhancement enchantment level for the specified element on the item stack.
     *
     * @param stack 物品栈 / Item stack
     * @param type 元素类型 / Element type
     * @return 强化等级 / Enhancement level
     */
    public static int getEnhancementLevel(ItemStack stack, ElementType type) {
        if (stack == null || stack.isEmpty() || type == ElementType.NONE) return 0;
        return switch (type) {
            case FIRE    -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FIRE_ENHANCE.get(), stack);
            case NATURE  -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.NATURE_ENHANCE.get(), stack);
            case FROST   -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FROST_ENHANCE.get(), stack);
            case THUNDER -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.THUNDER_ENHANCE.get(), stack);
            default      -> 0;
        };
    }

    /**
     * 计算实体在指定元素上的总强化点数（所有护甲槽位相加，并应用点数上限）。
     *
     * Calculates total enhancement points for the specified element on the entity (sum of all armor slots, with point cap applied).
     *
     * @param entity 活体实体 / Living entity
     * @param type 元素类型 / Element type
     * @return 总强化点数 / Total enhancement points
     */
    public static int getTotalEnhancement(LivingEntity entity, ElementType type) {
        if (entity == null || type == ElementType.NONE) return 0;

        int totalLevels = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                totalLevels += getEnhancementLevel(entity.getItemBySlot(slot), type);
            }
        }
        int valuePerLevel = ElementalConfig.STRENGTH_PER_LEVEL.get();
        return Math.min(totalLevels * valuePerLevel, ElementalConfig.getMaxStatCap());
    }

    /**
     * 获取物品栈上指定元素的抗性附魔等级（隐藏附魔）。
     *
     * Gets the resistance enchantment level for the specified element on the item stack (hidden enchantment).
     *
     * @param stack 物品栈 / Item stack
     * @param type 元素类型 / Element type
     * @return 抗性等级 / Resistance level
     */
    public static int getResistanceLevel(ItemStack stack, ElementType type) {
        if (stack == null || stack.isEmpty() || type == ElementType.NONE) return 0;
        return switch (type) {
            case FIRE    -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FIRE_RESIST.get(), stack);
            case NATURE  -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.NATURE_RESIST.get(), stack);
            case FROST   -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FROST_RESIST.get(), stack);
            case THUNDER -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.THUNDER_RESIST.get(), stack);
            default      -> 0;
        };
    }

    /**
     * 计算实体在指定元素上的总抗性点数（所有护甲槽位相加，并应用点数上限）。
     *
     * Calculates total resistance points for the specified element on the entity (sum of all armor slots, with point cap applied).
     *
     * @param entity 活体实体 / Living entity
     * @param type 元素类型 / Element type
     * @return 总抗性点数 / Total resistance points
     */
    public static int getTotalResistance(LivingEntity entity, ElementType type) {
        if (entity == null || type == ElementType.NONE) return 0;

        int totalLevels = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                totalLevels += getResistanceLevel(entity.getItemBySlot(slot), type);
            }
        }
        int valuePerLevel = ElementalConfig.RESIST_PER_LEVEL.get();
        return Math.min(totalLevels * valuePerLevel, ElementalConfig.getMaxStatCap());
    }

    /**
     * 从实体获取其当前使用的攻击元素属性（优先主手武器，其次检查护甲槽位）。
     *
     * Retrieves the currently used attack element attribute from the entity (prioritizes main-hand weapon, then checks armor slots).
     *
     * @param attacker 攻击者实体 / Attacker entity
     * @return 攻击元素类型，若无则返回 NONE / Attack element type, or NONE if none
     */
    public static ElementType getAttackElement(LivingEntity attacker) {
        if (attacker == null) return ElementType.NONE;
        ElementType type = getAttackElement(attacker.getMainHandItem());
        if (type != ElementType.NONE) return type;
        for (ItemStack stack : attacker.getArmorSlots()) {
            type = getAttackElement(stack);
            if (type != ElementType.NONE) return type;
        }
        return ElementType.NONE;
    }

    /**
     * 计算实体在指定元素上的显示用强化点数（包括主手武器，用于 Tooltip/Jade 显示）。
     *
     * Calculates display enhancement points for the specified element on the entity (includes main-hand weapon, used for Tooltip/Jade display).
     *
     * @param entity 活体实体 / Living entity
     * @param type 元素类型 / Element type
     * @return 显示用强化点数 / Display enhancement points
     */
    public static int getDisplayEnhancement(LivingEntity entity, ElementType type) {
        if (entity == null || type == ElementType.NONE) return 0;
        int levels = getEnhancementLevel(entity.getMainHandItem(), type);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                levels += getEnhancementLevel(entity.getItemBySlot(slot), type);
            }
        }
        return levels * ElementalConfig.STRENGTH_PER_LEVEL.get();
    }

    /**
     * 获取物品栈上的“主导”元素类型，用于决定是否显示【元素】前缀。
     * 优先级：攻击附魔（武器） > 强化附魔（装备） > 抗性附魔（怪物隐藏）。
     *
     * Retrieves the "dominant" element type on the item stack, used to determine whether to display the [Element] prefix.
     * Priority: attack enchantment (weapon) > enhancement enchantment (armor) > resistance enchantment (monster hidden).
     *
     * @param stack 物品栈 / Item stack
     * @return 主导元素类型，若无则返回 NONE / Dominant element type, or NONE if none
     */
    public static ElementType getDominantElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ElementType.NONE;

        // 1. 攻击附魔（武器） / 1. Attack enchantment (weapon)
        ElementType attack = getAttackElement(stack);
        if (attack != ElementType.NONE) return attack;

        // 2. 强化附魔（装备） / 2. Enhancement enchantment (armor)
        for (ElementType t : ElementType.values()) {
            if (t == ElementType.NONE) continue;
            if (getEnhancementLevel(stack, t) > 0) return t;
        }

        // 3. 抗性附魔（怪物装备，玩家永远拿不到） / 3. Resistance enchantment (monster equipment, unreachable by players)
        for (ElementType t : ElementType.values()) {
            if (t == ElementType.NONE) continue;
            if (getResistanceLevel(stack, t) > 0) return t;
        }

        return ElementType.NONE;
    }
}