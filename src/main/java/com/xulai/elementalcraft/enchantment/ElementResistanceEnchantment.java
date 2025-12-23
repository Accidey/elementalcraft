// src/main/java/com/xulai/elementalcraft/enchantment/ElementResistanceEnchantment.java
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
 * ElementResistanceEnchantment 类是元素抗性属性的附魔实现。
 * 该附魔用于提供对特定元素攻击的抗性减免。
 * 玩家可以通过附魔台、村民交易、附魔书等方式正常获得。
 *
 * ElementResistanceEnchantment class implements the elemental resistance enchantment.
 * This enchantment provides damage reduction against specific elemental attacks.
 * Players can obtain it normally via enchanting table, villager trading, or enchanted books.
 */
public class ElementResistanceEnchantment extends Enchantment {
    private final ElementType element;

    /**
     * 自定义附魔类别：严格限制只能附魔于护甲。
     *
     * Custom enchantment category: strictly limited to armor items.
     */
    public static final EnchantmentCategory STRICT_ARMOR = EnchantmentCategory.create("strict_armor",
            item -> item instanceof ArmorItem);

    /**
     * 构造函数，创建对应元素的抗性附魔。
     * 使用 RARE 稀有度，并适用于护甲槽位。
     *
     * Constructor to create the resistance enchantment for the corresponding element.
     * Uses RARE rarity and applies to armor slots.
     *
     * @param element 元素类型 / Elemental type
     */
    public ElementResistanceEnchantment(ElementType element) {
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
     * 最大附魔等级动态计算，基于配置的 maxStatCap 和 resist_per_level。
     * Maximum enchantment level is dynamically calculated based on config maxStatCap and resist_per_level.
     */
    @Override
    public int getMaxLevel() {
        int perLevel = ElementalConfig.getResistPerLevel();
        int cap = ElementalConfig.getMaxStatCap();
        return Math.max(1, cap / perLevel);
    }

    /**
     * 最小附魔费用（随等级递增）。
     * Minimum enchanting cost (increases with level).
     *
     * @param level 附魔等级 / Enchantment level
     * @return 最小费用 / Minimum cost
     */
    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 12;
    }

    /**
     * 最大附魔费用。
     * Maximum enchanting cost.
     *
     * @param level 附魔等级 / Enchantment level
     * @return 最大费用 / Maximum cost
     */
    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }

    /**
     * 该附魔可在附魔台被发现。
     *
     * This enchantment is discoverable at the enchanting table.
     */
    @Override
    public boolean isDiscoverable() {
        return true;
    }

    /**
     * 该附魔可通过村民交易获得。
     *
     * This enchantment can be obtained through villager trading.
     */
    @Override
    public boolean isTradeable() {
        return true;
    }

    /**
     * 该附魔可附魔到书上。
     *
     * This enchantment can be applied to books.
     */
    @Override
    public boolean isAllowedOnBooks() {
        return true;
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
     * 铁砧合并时严格检查物品是否属于护甲类别，防止武器获得抗性附魔。
     *
     * Strictly check if the item is armor during anvil merging to prevent weapons from getting resistance enchantments.
     *
     * @param stack 物品栈 / Item stack
     * @return 是否可附魔 / Whether enchantable
     */
    @Override
    public boolean canEnchant(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    /**
     * 附魔兼容性检查。
     * 同元素抗性附魔之间兼容（允许铁砧升级或合并更高等级），不同元素抗性附魔互斥。
     * 与其他附魔（如保护）正常兼容。
     *
     * Enchantment compatibility check.
     * Same-element resistance enchantments are compatible (allow anvil upgrading or merging higher levels),
     * different-element resistance enchantments are mutually exclusive.
     *
     * @param other 另一个附魔 / Other enchantment
     * @return 是否兼容 / Whether compatible
     */
    @Override
    public boolean checkCompatibility(@Nonnull Enchantment other) {
        if (other instanceof ElementResistanceEnchantment that) {
            return this.element == that.element;
        }
        return super.checkCompatibility(other);
    }
}