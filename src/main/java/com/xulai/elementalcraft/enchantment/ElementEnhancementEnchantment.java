// src/main/java/com/xulai/elementalcraft/enchantment/ElementEnhancementEnchantment.java
package com.xulai.elementalcraft.enchantment;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;

import javax.annotation.Nonnull;  

/**
 * ElementEnhancementEnchantment 类是元素强化属性的附魔实现。
 * 每个元素（赤焰、冰霜、自然、雷霆）对应一个独立的强化附魔实例，用于提升装备对该元素攻击的伤害加成。
 * 该附魔只能附着于护甲（头、胸、腿、脚），最大等级由配置动态决定。
 *
 * ElementEnhancementEnchantment class implements the elemental enhancement enchantment.
 * Each element (Flame, Frost, Nature, Thunder) has its own enhancement enchantment instance to increase damage bonus against that element's attacks.
 * This enchantment can only be applied to armor (head, chest, legs, feet) and has a maximum level dynamically determined by config.
 */
public class ElementEnhancementEnchantment extends Enchantment {
    private final ElementType element;

    /**
     * 自定义附魔类别：严格限制只能附魔于护甲。
     *
     * Custom enchantment category: strictly limited to armor items.
     */
    public static final EnchantmentCategory STRICT_ARMOR = EnchantmentCategory.create("strict_armor",
            item -> item instanceof ArmorItem);

    /**
     * 构造函数，创建对应元素的强化附魔。
     *
     * Constructor to create the enhancement enchantment for the corresponding element.
     *
     * @param element 元素类型 / Elemental type
     */
    public ElementEnhancementEnchantment(ElementType element) {
        super(Rarity.RARE, STRICT_ARMOR,
                new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
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
     * 最大附魔等级动态计算，基于配置的 maxStatCap 和 strength_per_level。
     * Maximum enchantment level is dynamically calculated based on config maxStatCap and strength_per_level.
     */
    @Override
    public int getMaxLevel() {
        int perLevel = ElementalConfig.getStrengthPerLevel();
        int cap = ElementalConfig.getMaxStatCap();
        return Math.max(1, cap / perLevel);
    }

    /**
     * 计算指定等级的最低附魔成本。
     *
     * Calculate the minimum enchanting cost for the specified level.
     *
     * @param level 附魔等级 / Enchantment level
     * @return 最低成本 / Minimum cost
     */
    @Override
    public int getMinCost(int level) {
        return 10 + level * 10;
    }

    /**
     * 计算指定等级的最高附魔成本。
     *
     * Calculate the maximum enchanting cost for the specified level.
     *
     * @param level 附魔等级 / Enchantment level
     * @return 最高成本 / Maximum cost
     */
    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
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
     * 同元素强化附魔之间兼容（允许铁砧升级或合并更高等级），不同元素强化附魔互斥。
     * 与其他非元素强化附魔兼容。
     *
     * Enchantment compatibility check.
     * Same-element enhancement enchantments are compatible (allow anvil upgrading or merging higher levels),
     * different-element enhancement enchantments are mutually exclusive.
     * Compatible with other non-elemental enhancement enchantments.
     *
     * @param other 另一个附魔 / Other enchantment
     * @return 是否兼容 / Whether compatible
     */
    @Override
    public boolean checkCompatibility(@Nonnull Enchantment other) {
        if (other instanceof ElementEnhancementEnchantment that) {
            return this.element == that.element;
        }
        return super.checkCompatibility(other);
    }

    /**
     * 仅允许在附魔台上对护甲物品附魔。
     *
     * Only allow enchanting armor items at the enchanting table.
     *
     * @param stack 物品栈 / Item stack
     * @return 是否可附魔 / Whether enchantable
     */
    @Override
    public boolean canApplyAtEnchantingTable(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    /**
     * 铁砧合并时严格检查物品是否属于护甲类别，防止武器附魔强化属性。
     *
     * Strictly check if the item is armor during anvil merging to prevent weapons from getting enhancement enchantments.
     *
     * @param stack 物品栈 / Item stack
     * @return 是否可附魔 / Whether enchantable
     */
    @Override
    public boolean canEnchant(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }
}