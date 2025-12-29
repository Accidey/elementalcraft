// src/main/java/com/xulai/elementalcraft/util/ElementUtils.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * ElementUtils
 * <p>
 * 中文说明：
 * 元素属性工具类。
 * 提供了一系列静态方法，用于查询和计算实体或物品上的元素属性信息。
 * 主要功能包括：
 * - 获取武器的攻击元素属性（支持主手和副手检测）。
 * - 获取装备的元素强化和抗性等级。
 * - 计算实体上某种元素的总强化/抗性点数（累加所有装备，不受单件上限限制，用于数值计算和显示）。
 * - 获取实体的“主导元素”（用于 Tooltip 显示）。
 * - 获取经过“严格一致性”判定后的攻击属性（用于元素反应触发判定）。
 * <p>
 * English Description:
 * Utility class for elemental attributes.
 * Provides static methods for querying and calculating elemental attribute information on entities or items.
 * Key features include:
 * - Retrieving weapon attack element attributes (supports main-hand and off-hand checks).
 * - Retrieving armor enhancement and resistance levels.
 * - Calculating total enhancement/resistance points for an element on an entity (sums all equipment, ignores per-piece caps, used for calculation and display).
 * - Retrieving the "dominant element" of an entity (used for Tooltip display).
 * - Retrieving the attack attribute after "Strict Consistency" validation (used for elemental reaction triggers).
 */
public class ElementUtils {

    /**
     * 从物品栈中获取其拥有的攻击元素属性。
     * 主要用于武器（剑、斧、弓、弩、三叉戟等）。
     * <p>
     * Retrieves the attack element attribute from an item stack.
     * Primarily used for weapons (swords, axes, bows, crossbows, tridents, etc.).
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
     * <p>
     * Gets the enhancement enchantment level for the specified element on the item stack.
     *
     * @param stack 物品栈 / Item stack
     * @param type  元素类型 / Element type
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
     * 获取物品栈上指定元素的抗性附魔等级（通常为怪物隐藏附魔）。
     * <p>
     * Gets the resistance enchantment level for the specified element on the item stack (usually hidden mob enchantments).
     *
     * @param stack 物品栈 / Item stack
     * @param type  元素类型 / Element type
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
     * 从实体获取其当前使用的攻击元素属性。
     * 优先检查主手物品，若无则检查副手物品。
     * <p>
     * Retrieves the currently used attack element attribute from the entity.
     * Prioritizes checking the main-hand item, then checks the off-hand item if none found.
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
     * 计算实体在指定元素上的总强化点数。
     * 累加所有护甲槽位和主手物品的强化等级对应的点数。
     * 此方法不受单件装备的点数上限限制，主要用于最终数值计算和 UI 显示（如 Jade）。
     * <p>
     * Calculates total enhancement points for the specified element on the entity.
     * Sums up points corresponding to enhancement levels from all armor slots and the main-hand item.
     * This method is not restricted by per-piece caps and is primarily used for final value calculation and UI display (e.g., Jade).
     *
     * @param entity 活体实体 / Living entity
     * @param type   元素类型 / Element type
     * @return 显示用总强化点数 / Total display enhancement points
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
     * 计算实体在指定元素上的总抗性点数。
     * 累加所有护甲槽位的抗性等级对应的点数。
     * 此方法不受单件装备的点数上限限制，主要用于最终数值计算和 UI 显示（如 Jade）。
     * <p>
     * Calculates total resistance points for the specified element on the entity.
     * Sums up points corresponding to resistance levels from all armor slots.
     * This method is not restricted by per-piece caps and is primarily used for final value calculation and UI display (e.g., Jade).
     *
     * @param entity 活体实体 / Living entity
     * @param type   元素类型 / Element type
     * @return 显示用总抗性点数 / Total display resistance points
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
     * 获取物品栈上的“主导”元素类型。
     * 用于决定是否显示【元素】前缀，以及前缀的颜色。
     * 优先级：攻击附魔（武器） > 强化附魔（装备） > 抗性附魔（怪物隐藏）。
     * <p>
     * Retrieves the "dominant" element type on the item stack.
     * Used to determine whether to display the [Element] prefix and its color.
     * Priority: Attack Enchantment (Weapon) > Enhancement Enchantment (Armor) > Resistance Enchantment (Monster Hidden).
     *
     * @param stack 物品栈 / Item stack
     * @return 主导元素类型，若无则返回 NONE / Dominant element type, or NONE if none
     */
    public static ElementType getDominantElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ElementType.NONE;

        // 1. 攻击附魔（武器）
        // 1. Attack enchantment (weapon)
        ElementType attack = getAttackElement(stack);
        if (attack != ElementType.NONE) return attack;

        // 2. 强化附魔（装备）
        // 2. Enhancement enchantment (armor)
        for (ElementType t : ElementType.values()) {
            if (t == ElementType.NONE) continue;
            if (getEnhancementLevel(stack, t) > 0) return t;
        }

        // 3. 抗性附魔（怪物装备）
        // 3. Resistance enchantment (monster equipment)
        for (ElementType t : ElementType.values()) {
            if (t == ElementType.NONE) continue;
            if (getResistanceLevel(stack, t) > 0) return t;
        }

        return ElementType.NONE;
    }

    /**
     * 获取实体的主导元素类型。
     * 遍历实体所有装备槽，返回找到的第一个主导元素类型。
     * 用于判定实体在元素反应中的属性倾向（例如：是否被视为冰霜属性从而导致易伤）。
     * <p>
     * Retrieves the dominant element type of the entity.
     * Iterates through all equipment slots and returns the first dominant element type found.
     * Used to determine the entity's attribute tendency in elemental reactions (e.g., whether it is considered Frost type leading to vulnerability).
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

    /**
     * 获取经过“严格一致性”判定后的攻击元素属性。
     * 规则：只有当【手持武器的攻击属性】与【身上装备的属性强化】一致时，才判定为拥有该属性。
     * 这意味着单纯手持元素武器但没有对应强化，或者只有强化但没有对应武器，都不会被视为有效的元素攻击源。
     * <p>
     * Retrieves the attack element attribute after "Strict Consistency" validation.
     * Rule: Only considered to have the attribute if [Held Weapon Attack Element] matches [Equipped Armor Attribute Enhancement].
     * This means holding an elemental weapon without corresponding enhancement, or having enhancement without a corresponding weapon, will not be considered a valid elemental attack source.
     *
     * @param attacker 攻击者 / Attacker
     * @return 经过验证的元素属性，若不一致则返回 NONE / Validated element attribute, or NONE if inconsistent
     */
    public static ElementType getConsistentAttackElement(LivingEntity attacker) {
        if (attacker == null) return ElementType.NONE;

        // 1. 获取武器攻击属性
        // 1. Get weapon attack element
        ElementType weaponElement = getAttackElement(attacker);
        if (weaponElement == ElementType.NONE) return ElementType.NONE;

        // 2. 检查身上是否有对应的属性强化
        // 只要总强化点数 > 0，即视为拥有该属性的驱动力（满足一致性条件）
        // 2. Check for corresponding attribute enhancement
        // As long as total enhancement points > 0, it meets the consistency condition (considered to have driving force for the attribute)
        int enhancementPoints = getDisplayEnhancement(attacker, weaponElement);

        if (enhancementPoints > 0) {
            return weaponElement;
        }

        return ElementType.NONE;
    }
}