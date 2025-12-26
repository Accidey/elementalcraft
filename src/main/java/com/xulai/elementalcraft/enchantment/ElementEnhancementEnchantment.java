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
 * ElementEnhancementEnchantment
 * <p>
 * 中文说明：
 * 元素强化属性附魔实现类。
 * 用于提升玩家对特定元素攻击造成的伤害加成。
 * 每个元素类型（赤焰、冰霜、自然、雷霆）对应一个独立的强化附魔实例。
 * 该附魔只能附着于护甲部位（头、胸、腿、脚），其最大等级由配置文件动态决定。
 * <p>
 * English Description:
 * Implementation class for elemental enhancement attribute enchantment.
 * Used to increase the damage bonus for attacks of a specific element dealt by the player.
 * Each element type (Flame, Frost, Nature, Thunder) corresponds to an independent enhancement enchantment instance.
 * This enchantment can only be applied to armor slots (head, chest, legs, feet), and its maximum level is dynamically determined by configuration.
 */
public class ElementEnhancementEnchantment extends Enchantment {
    private final ElementType element;

    /**
     * 自定义附魔类别：严格限制只能附魔于护甲物品。
     * <p>
     * Custom enchantment category: Strictly limited to armor items.
     */
    public static final EnchantmentCategory STRICT_ARMOR = EnchantmentCategory.create("strict_armor",
            item -> item instanceof ArmorItem);

    /**
     * 构造函数，创建对应元素的强化附魔。
     * <p>
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
     * <p>
     * Get the elemental type associated with this enchantment.
     *
     * @return 元素类型 / Elemental type
     */
    public ElementType getElement() {
        return element;
    }

    /**
     * 获取最大附魔等级。
     * 动态基于配置中的 maxStatCap（属性上限）和 strength_per_level（每级强化值）计算得出。
     * <p>
     * Get the maximum enchantment level.
     * Dynamically calculated based on maxStatCap (stat cap) and strength_per_level (value per level) in the config.
     */
    @Override
    public int getMaxLevel() {
        int perLevel = ElementalConfig.getStrengthPerLevel();
        int cap = ElementalConfig.getMaxStatCap();
        return Math.max(1, cap / perLevel);
    }

    /**
     * 计算指定等级的最低附魔成本（附魔台需求）。
     * <p>
     * Calculate the minimum enchanting cost for the specified level (Enchanting Table requirement).
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
     * <p>
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
     * 是否允许在附魔台中被发现（附魔出来）。
     * <p>
     * Whether it allows discovery at the enchanting table.
     */
    @Override
    public boolean isDiscoverable() {
        return true;
    }

    /**
     * 是否允许通过村民交易获取。
     * <p>
     * Whether it allows acquisition through villager trading.
     */
    @Override
    public boolean isTradeable() {
        return true;
    }

    /**
     * 是否允许附魔到书上。
     * <p>
     * Whether it allows enchanting onto books.
     */
    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }

    /**
     * 附魔兼容性检查。
     * 同一元素类型的强化附魔之间兼容（允许在铁砧上合并升级），不同元素类型的强化附魔互斥。
     * 与其他非元素强化类的附魔兼容。
     * <p>
     * Enchantment compatibility check.
     * Enhancement enchantments of the same element type are compatible (allowing merge upgrades on anvils),
     * while enhancement enchantments of different element types are mutually exclusive.
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
     * 仅允许在附魔台上对护甲物品进行附魔。
     * <p>
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
     * 检查物品是否可以接受此附魔（用于铁砧等场景）。
     * 严格检查物品是否属于护甲类别，防止武器错误获得强化属性附魔。
     * <p>
     * Check if the item can accept this enchantment (for anvils, etc.).
     * Strictly check if the item belongs to the armor category to prevent weapons from incorrectly getting enhancement enchantments.
     *
     * @param stack 物品栈 / Item stack
     * @return 是否可附魔 / Whether enchantable
     */
    @Override
    public boolean canEnchant(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }
}