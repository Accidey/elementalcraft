package com.xulai.elementalcraft.enchantment;

import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;



public class ElementAttackEnchantment extends Enchantment {
    private final ElementType element;




    public ElementAttackEnchantment(ElementType element) {
        super(Rarity.UNCOMMON, ModEnchantments.STRICT_WEAPON, new EquipmentSlot[0]);
        this.element = element;
    }




    public ElementType getElement() {
        return element;
    }




    @Override
    public int getMaxLevel() {
        return 1;
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
    public boolean checkCompatibility(@NotNull Enchantment other) {


        if (other == Enchantments.FIRE_ASPECT) {
            return false;
        }

        if (other == Enchantments.FLAMING_ARROWS || other == Enchantments.CHANNELING) {
            return false;
        }

        if (other instanceof ElementAttackEnchantment that) {
            return this.element == that.element;
        }

        return super.checkCompatibility(other);
    }




    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack) {
        return ModEnchantments.STRICT_WEAPON.canEnchant(stack.getItem());
    }




    @Override
    public boolean canEnchant(@NotNull ItemStack stack) {
        return ModEnchantments.STRICT_WEAPON.canEnchant(stack.getItem());
    }
}
