// src/main/java/com/xulai/elementalcraft/enchantment/ElementAttackEnchantment.java
package com.xulai.elementalcraft.enchantment;

import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.TridentItem;

/**
 * ElementAttackEnchantment 类是元素攻击属性的附魔实现。
 * 每个元素（赤焰、冰霜、自然、雷霆）对应一个独立的附魔实例，用于赋予武器特定元素攻击能力。
 * 该附魔只能附着于主手武器（剑、斧、三叉戟），最大等级为1。
 *
 * ElementAttackEnchantment class implements the elemental attack enchantment.
 * Each element (Flame, Frost, Nature, Thunder) has its own enchantment instance to grant weapons a specific elemental attack type.
 * This enchantment can only be applied to main-hand weapons (sword, axe, trident) and has a maximum level of 1.
 */
public class ElementAttackEnchantment extends Enchantment {
    private final ElementType element;

    /**
     * 自定义附魔类别：严格限制只能附魔于剑、斧、三叉戟。
     *
     * Custom enchantment category: strictly limited to swords, axes, and tridents.
     */
    public static final EnchantmentCategory STRICT_WEAPON = EnchantmentCategory.create("strict_weapon",
            item -> item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem);

    /**
     * 构造函数，创建对应元素的攻击附魔。
     *
     * Constructor to create the attack enchantment for the corresponding element.
     *
     * @param element 元素类型 / Elemental type
     */
    public ElementAttackEnchantment(ElementType element) {
        super(Rarity.UNCOMMON, STRICT_WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
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
     * 最大附魔等级为1。
     *
     * Maximum enchantment level is 1.
     */
    @Override
    public int getMaxLevel() {
        return 1;
    }

    /**
     * 允许在附魔台上发现。
     *
     * Allow discovery at the enchanting table.
     */
    @Override
    public boolean isDiscoverable() {
        return true;
    }

    /**
     * 允许村民交易获取。
     *
     * Allow acquisition through villager trading.
     */
    @Override
    public boolean isTradeable() {
        return true;
    }

    /**
     * 允许附魔到书上。
     *
     * Allow enchanting onto books.
     */
    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }

    /**
     * 附魔兼容性检查。
     * 同元素攻击附魔之间兼容（允许铁砧合并/升级），不同元素攻击附魔互斥。
     * 与其他非元素攻击附魔（如锋利、击退）兼容。
     *
     * Enchantment compatibility check.
     * Same-element attack enchantments are compatible (allow anvil merging/upgrading),
     * different-element attack enchantments are mutually exclusive.
     * Compatible with other non-elemental attack enchantments (e.g., Sharpness, Knockback).
     *
     * @param other 另一个附魔 / Other enchantment
     * @return 是否兼容 / Whether compatible
     */
    @Override
    public boolean checkCompatibility(Enchantment other) {
        if (other instanceof ElementAttackEnchantment that) {
            // 相同元素允许兼容（铁砧可合并） / Same element allows compatibility (anvil mergeable)
            // 不同元素互斥 / Different elements are mutually exclusive
            return this.element == that.element;
        }
        // 与其他所有附魔兼容 / Compatible with all other enchantments
        return super.checkCompatibility(other);
    }

    /**
     * 仅允许在附魔台上对严格武器类别物品附魔。
     *
     * Only allow enchanting items in the STRICT_WEAPON category at the enchanting table.
     *
     * @param stack 物品栈 / Item stack
     * @return 是否可附魔 / Whether enchantable
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return STRICT_WEAPON.canEnchant(stack.getItem());
    }
}