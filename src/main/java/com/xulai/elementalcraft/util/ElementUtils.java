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
 * - 获取武器攻击属性（支持主手+副手）
 * - 获取装备强化/抗性等级及总点数（所有护甲累加，不受单件上限限制）
 * - 获取实体主导元素（用于 Tooltip 前缀显示）
 * - 计算显示用的强化/抗性总点数（直接累加所有装备点数，用于 Jade/Tooltip）
 *
 * ElementUtils class provides a series of static utility methods for querying and calculating elemental attribute information on entities or items.
 * Including:
 * - Get weapon attack element (supports main hand + off hand)
 * - Get armor enhancement/resistance levels and total points (sum of all armor, no per-piece cap for display)
 * - Get dominant element of an entity (used for tooltip prefix display)
 * - Calculate display enhancement/resistance total points (direct sum across all equipment for Jade/Tooltip)
 */
public class ElementUtils {

    /**
     * 从物品栈中获取其拥有的攻击元素属性。
     * 用于武器（剑、斧等）。
     *
     * Retrieves the attack element attribute from an item stack.
     * Used for weapons (sword, axe, etc.).
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
     * 从实体获取其当前使用的攻击元素属性（优先主手，其次副手）。
     *
     * Retrieves the currently used attack element attribute from the entity (prioritizes main hand, then off hand).
     *
     * @param attacker 攻击者实体 / Attacker entity
     * @return 攻击元素类型，若无则返回 NONE / Attack element type, or NONE if none
     */
    public static ElementType getAttackElement(LivingEntity attacker) {
        if (attacker == null) return ElementType.NONE;
        ElementType type = getAttackElement(attacker.getMainHandItem());
        if (type != ElementType.NONE) return type;
        return getAttackElement(attacker.getOffhandItem());
    }

    /**
     * 计算实体在指定元素上的总强化点数（所有护甲 + 主手累加，不受单件上限限制，用于 Jade/Tooltip 显示）。
     *
     * Calculates total enhancement points for the specified element on the entity (all armor + main hand, no per-piece cap for display).
     *
     * @param entity 活体实体 / Living entity
     * @param type 元素类型 / Element type
     * @return 显示用总强化点数 / Display total enhancement points
     */
    public static int getDisplayEnhancement(LivingEntity entity, ElementType type) {
        if (entity == null || type == ElementType.NONE) return 0;

        int totalLevels = getEnhancementLevel(entity.getMainHandItem(), type);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                totalLevels += getEnhancementLevel(entity.getItemBySlot(slot), type);
            }
        }

        return totalLevels * ElementalConfig.getStrengthPerLevel();
    }

    /**
     * 计算实体在指定元素上的总抗性点数（所有护甲累加，不受单件上限限制，用于 Jade/Tooltip 显示）。
     *
     * Calculates total resistance points for the specified element on the entity (all armor, no per-piece cap for display).
     *
     * @param entity 活体实体 / Living entity
     * @param type 元素类型 / Element type
     * @return 显示用总抗性点数 / Display total resistance points
     */
    public static int getDisplayResistance(LivingEntity entity, ElementType type) {
        if (entity == null || type == ElementType.NONE) return 0;

        int totalLevels = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                totalLevels += getResistanceLevel(entity.getItemBySlot(slot), type);
            }
        }

        return totalLevels * ElementalConfig.getResistPerLevel();
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

        // 3. 抗性附魔（怪物装备） / 3. Resistance enchantment (monster equipment)
        for (ElementType t : ElementType.values()) {
            if (t == ElementType.NONE) continue;
            if (getResistanceLevel(stack, t) > 0) return t;
        }

        return ElementType.NONE;
    }

    /**
     * 获取实体的主导元素类型。
     * 遍历实体所有装备槽，返回找到的第一个主导元素类型。
     * 用于判定实体在元素反应中的属性（如：是否为冰霜属性导致易伤）。
     *
     * Retrieves the dominant element type of the entity.
     * Iterates through all equipment slots and returns the first dominant element type found.
     * Used to determine the entity's attribute in elemental reactions (e.g., whether it is Frost type causing vulnerability).
     *
     * @param entity 实体 / Entity
     * @return 元素类型 / ElementType
     */
    public static ElementType getElementType(LivingEntity entity) {
        if (entity == null) return ElementType.NONE;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            ElementType type = getDominantElement(stack);
            if (type != ElementType.NONE) {
                return type;
            }
        }
        return ElementType.NONE;
    }
}