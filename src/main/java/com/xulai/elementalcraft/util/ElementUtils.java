package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.EquipmentSlot;

public class ElementUtils {

    public static ElementType getAttackElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ElementType.NONE;

        if (EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FIRE_STRIKE.get(), stack) > 0)    return ElementType.FIRE;
        if (EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.NATURE_STRIKE.get(), stack) > 0) return ElementType.NATURE;
        if (EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FROST_STRIKE.get(), stack) > 0)  return ElementType.FROST;
        if (EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.THUNDER_STRIKE.get(), stack) > 0) return ElementType.THUNDER;
        return ElementType.NONE;
    }

    public static int getEnhancementLevel(ItemStack stack, ElementType type) {
        if (stack == null || stack.isEmpty() || type == ElementType.NONE) return 0;
        return switch (type) {
            case FIRE    -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FIRE_ENHANCE.get(), stack);
            case NATURE  -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.NATURE_ENHANCE.get(), stack);
            case FROST   -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FROST_ENHANCE.get(), stack);
            case THUNDER -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.THUNDER_ENHANCE.get(), stack);
            default      -> 0;
        };
    }

    public static int getResistanceLevel(ItemStack stack, ElementType type) {
        if (stack == null || stack.isEmpty() || type == ElementType.NONE) return 0;
        return switch (type) {
            case FIRE    -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FIRE_RESIST.get(), stack);
            case NATURE  -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.NATURE_RESIST.get(), stack);
            case FROST   -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.FROST_RESIST.get(), stack);
            case THUNDER -> EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.THUNDER_RESIST.get(), stack);
            default      -> 0;
        };
    }

    public static ElementType getAttackElement(LivingEntity attacker) {
        if (attacker == null) return ElementType.NONE;
        ElementType type = getAttackElement(attacker.getMainHandItem());
        if (type != ElementType.NONE) return type;
        return getAttackElement(attacker.getOffhandItem());
    }

    public static int getDisplayEnhancement(LivingEntity entity, ElementType type) {
        if (entity == null || type == ElementType.NONE) return 0;

        int totalLevels = getEnhancementLevel(entity.getMainHandItem(), type);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                totalLevels += getEnhancementLevel(entity.getItemBySlot(slot), type);
            }
        }

        return totalLevels * ElementalConfig.getStrengthPerLevel();
    }

    public static int getDisplayResistance(LivingEntity entity, ElementType type) {
        if (entity == null || type == ElementType.NONE) return 0;

        int totalLevels = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                totalLevels += getResistanceLevel(entity.getItemBySlot(slot), type);
            }
        }

        return totalLevels * ElementalConfig.getResistPerLevel();
    }

    public static ElementType getDominantElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ElementType.NONE;

        ElementType attack = getAttackElement(stack);
        if (attack != ElementType.NONE) return attack;

        for (ElementType t : ElementType.values()) {
            if (t == ElementType.NONE) continue;
            if (getEnhancementLevel(stack, t) > 0) return t;
        }

        for (ElementType t : ElementType.values()) {
            if (t == ElementType.NONE) continue;
            if (getResistanceLevel(stack, t) > 0) return t;
        }

        return ElementType.NONE;
    }

    public static ElementType getElementType(LivingEntity entity) {
        if (entity == null) return ElementType.NONE;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            ElementType type = getDominantElement(stack);
            if (type != ElementType.NONE) {
                return type;
            }
        }
        return ElementType.NONE;
    }

    public static ElementType getConsistentAttackElement(LivingEntity attacker) {
        if (attacker == null) return ElementType.NONE;

        ElementType weaponElement = getAttackElement(attacker);
        if (weaponElement == ElementType.NONE) return ElementType.NONE;

        int enhancementPoints = getDisplayEnhancement(attacker, weaponElement);

        if (enhancementPoints > 0) {
            return weaponElement;
        }

        return ElementType.NONE;
    }
}