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
 * ElementResistanceEnchantment
 * <p>
 * 中文说明：
 * 元素抗性属性附魔实现类。
 * 用于提供对特定元素（赤焰、冰霜、自然、雷霆）攻击的伤害减免。
 * 玩家可以通过附魔台、村民交易、附魔书等正常途径获得此附魔。
 * 该附魔仅适用于护甲装备。
 * <p>
 * English Description:
 * Implementation class for elemental resistance attribute enchantment.
 * Provides damage reduction against attacks of specific elements (Flame, Frost, Nature, Thunder).
 * Players can obtain this enchantment normally via enchanting tables, villager trading, or enchanted books.
 * This enchantment is applicable only to armor items.
 */
public class ElementResistanceEnchantment extends Enchantment {
    private final ElementType element;

    /**
     * 自定义附魔类别：严格限制只能附魔于护甲物品。
     * <p>
     * Custom enchantment category: Strictly limited to armor items.
     */
    public static final EnchantmentCategory STRICT_ARMOR = EnchantmentCategory.create("strict_armor",
            item -> item instanceof ArmorItem);

    /**
     * 构造函数，创建对应元素的抗性附魔。
     * 使用 RARE (稀有) 稀有度，并适用于所有护甲槽位（头、胸、腿、脚）。
     * <p>
     * Constructor to create the resistance enchantment for the corresponding element.
     * Uses RARE rarity and applies to all armor slots (Head, Chest, Legs, Feet).
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
     * 动态基于配置中的 maxStatCap（属性上限）和 resist_per_level（每级抗性值）计算得出。
     * <p>
     * Get the maximum enchantment level.
     * Dynamically calculated based on config maxStatCap (stat cap) and resist_per_level (value per level).
     */
    @Override
    public int getMaxLevel() {
        int perLevel = ElementalConfig.getResistPerLevel();
        int cap = ElementalConfig.getMaxStatCap();
        return Math.max(1, cap / perLevel);
    }

    /**
     * 计算指定等级的最低附魔成本（附魔台需求）。
     * 随等级递增。
     * <p>
     * Calculate the minimum enchanting cost for the specified level (Enchanting Table requirement).
     * Increases with level.
     *
     * @param level 附魔等级 / Enchantment level
     * @return 最低成本 / Minimum cost
     */
    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 12;
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
     * 严格检查物品是否属于护甲类别，防止武器错误获得抗性附魔。
     * <p>
     * Check if the item can accept this enchantment (for anvils, etc.).
     * Strictly check if the item belongs to the armor category to prevent weapons from incorrectly getting resistance enchantments.
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
     * 同一元素类型的抗性附魔之间兼容（允许在铁砧上合并升级），不同元素类型的抗性附魔互斥。
     * 与其他非元素抗性类的附魔（如保护、爆炸保护）正常兼容。
     * <p>
     * Enchantment compatibility check.
     * Resistance enchantments of the same element type are compatible (allowing merge upgrades on anvils),
     * while resistance enchantments of different element types are mutually exclusive.
     * Compatible with other non-elemental resistance enchantments (e.g., Protection, Blast Protection).
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