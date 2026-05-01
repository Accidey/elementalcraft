package com.xulai.elementalcraft.enchantment;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;



public class ElementResistanceEnchantment extends Enchantment {
    private final ElementType element;




    public ElementResistanceEnchantment(ElementType element) {
        super(Rarity.RARE, ModEnchantments.STRICT_ARMOR,
                new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
        this.element = element;
    }




    public ElementType getElement() {
        return element;
    }




    @Override
    public int getMaxLevel() {
        int perLevel = ElementalConfig.getResistPerLevel();
        if (perLevel <= 0) return 1;
        int cap = ElementalConfig.getMaxStatCap();
        return Math.max(1, cap / perLevel);
    }




    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 12;
    }




    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }




    @Override
    public boolean isDiscoverable() {
        return true;
    }




    @Override
    public boolean isTradeable() {
        return true;
    }




    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }




    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }




    @Override
    public boolean canEnchant(@NotNull ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }




    @Override
    public boolean checkCompatibility(@NotNull Enchantment other) {
        if (other instanceof ElementResistanceEnchantment that) {
            return this.element == that.element;
        }
        return super.checkCompatibility(other);
    }
}
