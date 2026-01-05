// src/main/java/com/xulai/elementalcraft/enchantment/ModEnchantments.java
package com.xulai.elementalcraft.enchantment;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModEnchantments
 * <p>
 * 中文说明：
 * 模组附魔注册类。
 * 负责使用 DeferredRegister 将模组中的所有自定义附魔注册到游戏中。
 * 包含三类附魔：武器元素攻击、装备元素强化、装备元素抗性。
 * 同时集中管理自定义附魔类别，防止重复注册导致的冲突。
 * <p>
 * English Description:
 * Mod Enchantments Registration Class.
 * Responsible for registering all custom enchantments in the mod into the game using DeferredRegister.
 * Includes three categories of enchantments: Weapon Elemental Attack, Armor Elemental Enhancement, and Armor Elemental Resistance.
 * Also centralizes the management of custom enchantment categories to prevent conflicts caused by duplicate registration.
 */
public class ModEnchantments {

    /**
     * 延迟注册器，用于注册附魔。
     * <p>
     * Deferred register for registering enchantments.
     */
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ElementalCraft.MODID);

    // ======================== Custom Categories / 自定义类别 ========================

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
     * 自定义附魔类别：严格限制只能附魔于护甲物品。
     * <p>
     * Custom enchantment category: Strictly limited to armor items.
     */
    public static final EnchantmentCategory STRICT_ARMOR = EnchantmentCategory.create("strict_armor",
            item -> item instanceof ArmorItem);

    // ======================== Weapon Elemental Attack Enchantments ========================
    // ======================== 武器元素攻击附魔 ========================

    /**
     * 注册赤焰属性攻击附魔。
     * <p>
     * Registers the Flame Strike enchantment.
     */
    public static final RegistryObject<Enchantment> FIRE_STRIKE = ENCHANTMENTS.register("fire_strike",
            () -> new ElementAttackEnchantment(ElementType.FIRE));

    /**
     * 注册自然属性攻击附魔。
     * <p>
     * Registers the Nature Strike enchantment.
     */
    public static final RegistryObject<Enchantment> NATURE_STRIKE = ENCHANTMENTS.register("nature_strike",
            () -> new ElementAttackEnchantment(ElementType.NATURE));

    /**
     * 注册冰霜属性攻击附魔。
     * <p>
     * Registers the Frost Strike enchantment.
     */
    public static final RegistryObject<Enchantment> FROST_STRIKE = ENCHANTMENTS.register("frost_strike",
            () -> new ElementAttackEnchantment(ElementType.FROST));

    /**
     * 注册雷霆属性攻击附魔。
     * <p>
     * Registers the Thunder Strike enchantment.
     */
    public static final RegistryObject<Enchantment> THUNDER_STRIKE = ENCHANTMENTS.register("thunder_strike",
            () -> new ElementAttackEnchantment(ElementType.THUNDER));

    // ======================== Armor Elemental Enhancement Enchantments ========================
    // ======================== 装备元素强化附魔 ========================

    /**
     * 注册赤焰强化附魔。
     * <p>
     * Registers the Flame Enhancement enchantment.
     */
    public static final RegistryObject<Enchantment> FIRE_ENHANCE = ENCHANTMENTS.register("fire_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.FIRE));

    /**
     * 注册自然强化附魔。
     * <p>
     * Registers the Nature Enhancement enchantment.
     */
    public static final RegistryObject<Enchantment> NATURE_ENHANCE = ENCHANTMENTS.register("nature_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.NATURE));

    /**
     * 注册冰霜强化附魔。
     * <p>
     * Registers the Frost Enhancement enchantment.
     */
    public static final RegistryObject<Enchantment> FROST_ENHANCE = ENCHANTMENTS.register("frost_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.FROST));

    /**
     * 注册雷霆强化附魔。
     * <p>
     * Registers the Thunder Enhancement enchantment.
     */
    public static final RegistryObject<Enchantment> THUNDER_ENHANCE = ENCHANTMENTS.register("thunder_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.THUNDER));

    // ======================== Elemental Resistance Enchantments ========================
    // ======================== 元素抗性附魔 ========================

    /**
     * 注册赤焰抗性附魔。
     * <p>
     * Registers the Flame Resistance enchantment.
     */
    public static final RegistryObject<Enchantment> FIRE_RESIST = ENCHANTMENTS.register("fire_resistance_hidden",
            () -> new ElementResistanceEnchantment(ElementType.FIRE));

    /**
     * 注册自然抗性附魔。
     * <p>
     * Registers the Nature Resistance enchantment.
     */
    public static final RegistryObject<Enchantment> NATURE_RESIST = ENCHANTMENTS.register("nature_resistance_hidden",
            () -> new ElementResistanceEnchantment(ElementType.NATURE));

    /**
     * 注册冰霜抗性附魔。
     * <p>
     * Registers the Frost Resistance enchantment.
     */
    public static final RegistryObject<Enchantment> FROST_RESIST = ENCHANTMENTS.register("frost_resistance_hidden",
            () -> new ElementResistanceEnchantment(ElementType.FROST));

    /**
     * 注册雷霆抗性附魔。
     * <p>
     * Registers the Thunder Resistance enchantment.
     */
    public static final RegistryObject<Enchantment> THUNDER_RESIST = ENCHANTMENTS.register("thunder_resistance_hidden",
            () -> new ElementResistanceEnchantment(ElementType.THUNDER));

    /**
     * 将附魔注册器关联到模组事件总线。
     * <p>
     * Registers the enchantment registry to the mod event bus.
     *
     * @param eventBus 模组事件总线 / Mod event bus
     */
    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}