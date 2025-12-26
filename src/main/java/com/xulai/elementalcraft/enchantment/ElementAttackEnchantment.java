// src/main/java/com/xulai/elementalcraft/enchantment/ElementAttackEnchantment.java
package com.xulai.elementalcraft.enchantment;

import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;

/**
 * ElementAttackEnchantment
 * <p>
 * 中文说明：
 * 元素攻击属性附魔实现类。
 * 为武器赋予特定的元素属性（如赤焰、冰霜、自然、雷霆）。
 * 每个元素类型对应一个附魔实例，且该附魔只能应用在主手武器（剑、斧、三叉戟、弓、弩）上，最大等级为 1。
 * <p>
 * English Description:
 * Implementation class for elemental attack attribute enchantment.
 * Grants weapons specific elemental attributes (e.g., Flame, Frost, Nature, Thunder).
 * Each element type corresponds to an enchantment instance, and this enchantment can only be applied to main-hand weapons (swords, axes, tridents, bows, crossbows) with a maximum level of 1.
 */
public class ElementAttackEnchantment extends Enchantment {
    private final ElementType element;

    /**
     * 自定义附魔类别：严格限制只能附魔于可用于攻击的武器。
     * 包含：剑、斧、三叉戟、弓、弩。
     * <p>
     * Custom enchantment category: Strictly limited to weapons that can be used for attacking.
     * Includes: Sword, Axe, Trident, Bow, Crossbow.
     */
    public static final EnchantmentCategory STRICT_WEAPON = EnchantmentCategory.create("strict_weapon",
            item -> item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem
                    || item instanceof BowItem || item instanceof CrossbowItem);

    /**
     * 构造函数，创建对应元素的攻击附魔。
     * <p>
     * Constructor to create the attack enchantment for the corresponding element.
     *
     * @param element 元素类型 / Elemental type
     */
    public ElementAttackEnchantment(ElementType element) {
        super(Rarity.UNCOMMON, STRICT_WEAPON, new EquipmentSlot[0]);
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
     * 获取最大附魔等级，固定为 1。
     * <p>
     * Get the maximum enchantment level, fixed at 1.
     */
    @Override
    public int getMaxLevel() {
        return 1;
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
     * 1. 检查配置：若配置开启互斥，则元素攻击附魔与原版火焰附加 (Fire Aspect) 互斥。
     * 2. 检查同类：不同元素类型的攻击附魔互斥（如赤焰与冰霜不能共存），同类型兼容。
     * <p>
     * Enchantment compatibility check.
     * 1. Config Check: If exclusivity is enabled in config, Element Attack is mutually exclusive with Vanilla Fire Aspect.
     * 2. Class Check: Different elemental attack types are mutually exclusive (e.g., Flame vs Frost), same types are compatible.
     *
     * @param other 另一个附魔 / Other enchantment
     * @return 是否兼容 / Whether compatible
     */
    @Override
    public boolean checkCompatibility(Enchantment other) {
        // 规则1：与原版火焰附加互斥（可配置）
        // Rule 1: Exclusivity with Vanilla Fire Aspect (Configurable)
        if (ElementalReactionConfig.enchantFireAspectExclusive && other == Enchantments.FIRE_ASPECT) {
            return false;
        }

        // 规则2：不同元素类型之间互斥
        // Rule 2: Exclusivity between different Element Types
        if (other instanceof ElementAttackEnchantment that) {
            return this.element == that.element;
        }

        return super.checkCompatibility(other);
    }

    /**
     * 仅允许在附魔台上对严格定义的武器类别物品进行附魔。
     * <p>
     * Only allow enchanting items in the strictly defined weapon category at the enchanting table.
     *
     * @param stack 物品栈 / Item stack
     * @return 是否可附魔 / Whether enchantable
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return STRICT_WEAPON.canEnchant(stack.getItem());
    }

    /**
     * 检查物品是否可以接受此附魔（用于铁砧等场景）。
     * 严格检查物品是否属于武器类别，防止护甲错误获得攻击属性附魔。
     * <p>
     * Check if the item can accept this enchantment (for anvils, etc.).
     * Strictly check if the item belongs to the weapon category to prevent armor from incorrectly getting attack enchantments.
     *
     * @param stack 物品栈 / Item stack
     * @return 是否可附魔 / Whether enchantable
     */
    @Override
    public boolean canEnchant(ItemStack stack) {
        return STRICT_WEAPON.canEnchant(stack.getItem());
    }
}