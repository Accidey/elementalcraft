// src/main/java/com/xulai/elementalcraft/enchantment/ElementResistanceEnchantment.java
package com.xulai.elementalcraft.enchantment;

import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * ElementResistanceEnchantment 类是元素抗性属性的隐藏附魔实现。
 * 该附魔用于怪物装备，提供对特定元素攻击的抗性减免。
 * 它被设计为隐藏附魔：无法通过附魔台、村民交易或附魔书获得，仅由模组内部逻辑（如怪物生成）应用。
 *
 * ElementResistanceEnchantment class implements the hidden elemental resistance enchantment.
 * This enchantment is used on monster equipment to provide resistance reduction against specific elemental attacks.
 * It is designed as a hidden enchantment: cannot be obtained through enchanting table, villager trading, or enchanted books.
 * It is applied only by internal mod logic (e.g., monster generation).
 */
public class ElementResistanceEnchantment extends Enchantment {
    private final ElementType element;

    /**
     * 构造函数，创建对应元素的抗性附魔。
     * 使用 VERY_RARE 稀有度，并适用于所有装备槽位（包括头盔，用于隐形头盔机制）。
     *
     * Constructor to create the resistance enchantment for the corresponding element.
     * Uses VERY_RARE rarity and applies to all equipment slots (including helmet for invisible helmet mechanism).
     *
     * @param element 元素类型 / Elemental type
     */
    public ElementResistanceEnchantment(ElementType element) {
        super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR, EquipmentSlot.values());
        this.element = element;
    }

    /**
     * 获取该附魔对应的元素类型。
     *
     * Get the elemental type associated with this enchantment.
     *
     * @return 元素类型 / Elemental type
     */
    public ElementType getElement() {
        return element;
    }

    /**
     * 最大附魔等级为5。
     *
     * Maximum enchantment level is 5.
     */
    @Override
    public int getMaxLevel() {
        return 5;
    }

    /**
     * 该附魔不可在附魔台被发现（隐藏附魔）。
     *
     * This enchantment is not discoverable at the enchanting table (hidden enchantment).
     */
    @Override
    public boolean isDiscoverable() {
        return false;
    }

    /**
     * 该附魔不可通过村民交易获得（隐藏附魔）。
     *
     * This enchantment cannot be obtained through villager trading (hidden enchantment).
     */
    @Override
    public boolean isTradeable() {
        return false;
    }

    /**
     * 该附魔不可通过附魔台应用到物品上（隐藏附魔）。
     *
     * This enchantment cannot be applied to items at the enchanting table (hidden enchantment).
     *
     * @param stack 物品栈 / Item stack
     * @return 始终返回 false / Always returns false
     */
    @Override
    public boolean canApplyAtEnchantingTable(net.minecraft.world.item.ItemStack stack) {
        return false;
    }
}