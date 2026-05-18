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



public class ModEnchantments {




    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ElementalCraft.MODID);






    public static final EnchantmentCategory STRICT_WEAPON = EnchantmentCategory.create("strict_weapon",
            item -> item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem
                    || item instanceof BowItem || item instanceof CrossbowItem);




    public static final EnchantmentCategory STRICT_ARMOR = EnchantmentCategory.create("strict_armor",
            item -> item instanceof ArmorItem);







    public static final RegistryObject<Enchantment> FIRE_STRIKE = ENCHANTMENTS.register("fire_strike",
            () -> new ElementAttackEnchantment(ElementType.FIRE));




    public static final RegistryObject<Enchantment> NATURE_STRIKE = ENCHANTMENTS.register("nature_strike",
            () -> new ElementAttackEnchantment(ElementType.NATURE));




    public static final RegistryObject<Enchantment> FROST_STRIKE = ENCHANTMENTS.register("frost_strike",
            () -> new ElementAttackEnchantment(ElementType.FROST));




    public static final RegistryObject<Enchantment> THUNDER_STRIKE = ENCHANTMENTS.register("thunder_strike",
            () -> new ElementAttackEnchantment(ElementType.THUNDER));







    public static final RegistryObject<Enchantment> FIRE_ENHANCE = ENCHANTMENTS.register("fire_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.FIRE));




    public static final RegistryObject<Enchantment> NATURE_ENHANCE = ENCHANTMENTS.register("nature_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.NATURE));




    public static final RegistryObject<Enchantment> FROST_ENHANCE = ENCHANTMENTS.register("frost_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.FROST));




    public static final RegistryObject<Enchantment> THUNDER_ENHANCE = ENCHANTMENTS.register("thunder_enhancement",
            () -> new ElementEnhancementEnchantment(ElementType.THUNDER));







    public static final RegistryObject<Enchantment> FIRE_RESIST = ENCHANTMENTS.register("fire_resistance",
            () -> new ElementResistanceEnchantment(ElementType.FIRE));




    public static final RegistryObject<Enchantment> NATURE_RESIST = ENCHANTMENTS.register("nature_resistance",
            () -> new ElementResistanceEnchantment(ElementType.NATURE));




    public static final RegistryObject<Enchantment> FROST_RESIST = ENCHANTMENTS.register("frost_resistance",
            () -> new ElementResistanceEnchantment(ElementType.FROST));




    public static final RegistryObject<Enchantment> THUNDER_RESIST = ENCHANTMENTS.register("thunder_resistance",
            () -> new ElementResistanceEnchantment(ElementType.THUNDER));




    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
