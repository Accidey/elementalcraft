// src/main/java/com/xulai/elementalcraft/enchantment/ModEnchantments.java
package com.xulai.elementalcraft.enchantment;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModEnchantments 类负责注册模组中所有自定义附魔。
 * 包括：
 * - 武器元素攻击附魔（ElementAttackEnchantment）
 * - 装备元素强化附魔（ElementEnhancementEnchantment）
 * - 怪物专属隐藏抗性附魔（ElementResistanceEnchantment）
 *
 * ModEnchantments class is responsible for registering all custom enchantments in the mod.
 * Including:
 * - Weapon elemental attack enchantments (ElementAttackEnchantment)
 * - Armor elemental enhancement enchantments (ElementEnhancementEnchantment)
 * - Monster-exclusive hidden resistance enchantments (ElementResistanceEnchantment)
 */
public class ModEnchantments {
    /**
     * 延迟注册器，用于注册所有自定义附魔。
     *
     * Deferred register for registering all custom enchantments.
     */
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ElementalCraft.MODID);

    // ======================== Weapon Elemental Attack Enchantments ========================
    // ======================== 武器元素攻击附魔 ========================

    /**
     * 赤焰属性攻击附魔 / Flame Strike enchantment
     */
    public static final RegistryObject<Enchantment> FIRE_STRIKE = ENCHANTMENTS.register("fire_strike",
            () -> new ElementAttackEnchantment(ElementType.FIRE));

    /**
     * 自然属性攻击附魔 / Nature Strike enchantment
     */
    public static final RegistryObject<Enchantment> NATURE_STRIKE = ENCHANTMENTS.register("nature_strike",
            () -> new ElementAttackEnchantment(ElementType.NATURE));

    /**
     * 冰霜属性攻击附魔 / Frost Strike enchantment
     */
    public static final RegistryObject<Enchantment> FROST_STRIKE = ENCHANTMENTS.register("frost_strike",
            () -> new ElementAttackEnchantment(ElementType.FROST));

    /**
     * 雷霆属性攻击附魔 / Thunder Strike enchantment
     */
    public static final RegistryObject<Enchantment> THUNDER_STRIKE = ENCHANTMENTS.register("thunder_strike",
            () -> new ElementAttackEnchantment(ElementType.THUNDER));

    // ======================== Armor Elemental Enhancement Enchantments ========================
    // ======================== 装备元素强化附魔 ========================

    /**
     * 赤焰强化附魔 / Flame Enhancement enchantment
     */
    public static final RegistryObject<Enchantment> FIRE_ENHANCE = ENCHANTMENTS.register("fire_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.FIRE));

    /**
     * 自然强化附魔 / Nature Enhancement enchantment
     */
    public static final RegistryObject<Enchantment> NATURE_ENHANCE = ENCHANTMENTS.register("nature_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.NATURE));

    /**
     * 冰霜强化附魔 / Frost Enhancement enchantment
     */
    public static final RegistryObject<Enchantment> FROST_ENHANCE = ENCHANTMENTS.register("frost_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.FROST));

    /**
     * 雷霆强化附魔 / Thunder Enhancement enchantment
     */
    public static final RegistryObject<Enchantment> THUNDER_ENHANCE = ENCHANTMENTS.register("thunder_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.THUNDER));

    // ======================== Monster Hidden Resistance Enchantments ========================
    // ======================== 怪物专属隐藏抗性附魔 ========================

    /**
     * 赤焰抗性（隐藏） / Flame Resistance (hidden)
     */
    public static final RegistryObject<Enchantment> FIRE_RESIST = ENCHANTMENTS.register("fire_resistance_hidden",
            () -> new ElementResistanceEnchantment(ElementType.FIRE));

    /**
     * 自然抗性（隐藏） / Nature Resistance (hidden)
     */
    public static final RegistryObject<Enchantment> NATURE_RESIST = ENCHANTMENTS.register("nature_resistance_hidden",
            () -> new ElementResistanceEnchantment(ElementType.NATURE));

    /**
     * 冰霜抗性（隐藏） / Frost Resistance (hidden)
     */
    public static final RegistryObject<Enchantment> FROST_RESIST = ENCHANTMENTS.register("frost_resistance_hidden",
            () -> new ElementResistanceEnchantment(ElementType.FROST));

    /**
     * 雷霆抗性（隐藏） / Thunder Resistance (hidden)
     */
    public static final RegistryObject<Enchantment> THUNDER_RESIST = ENCHANTMENTS.register("thunder_resistance_hidden",
            () -> new ElementResistanceEnchantment(ElementType.THUNDER));

    /**
     * 将所有附魔注册到模组事件总线。
     *
     * Registers all enchantments to the mod event bus.
     *
     * @param eventBus 模组事件总线 / Mod event bus
     */
    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}