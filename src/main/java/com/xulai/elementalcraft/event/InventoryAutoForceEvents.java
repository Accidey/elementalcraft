package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ForcedItemHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class InventoryAutoForceEvents {

    private static final String TAG_FORCED = "elementalcraft_forced";

    private static final String TAG_FORCED_DATA = "elementalcraft_forced_data";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null || player.level().isClientSide()) return;

        if (player.tickCount % 100 != 0) return;

        processList(player.getInventory().items, player);
        processList(player.getInventory().armor, player);
        processList(player.getInventory().offhand, player);
    }

    private static void processList(List<ItemStack> stacks, Player player) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            boolean isForced =
                    ForcedItemHelper.getForcedWeapon(stack.getItem()) != null
                            || ForcedItemHelper.getForcedArmor(stack.getItem()) != null;

            CompoundTag tag = stack.getTag();
            boolean wasForced = tag != null && tag.getBoolean(TAG_FORCED);

            if (isForced) {
                applyForcedAttributes(stack, wasForced);
            } else if (wasForced) {
                removeForcedAttributes(stack);
                stack.removeTagKey(TAG_FORCED);
                stack.removeTagKey(TAG_FORCED_DATA);
            }
        }
    }

    public static void applyForcedAttributes(ItemStack stack, boolean isTracked) {
        if (stack.isEmpty()) return;

        boolean changed = false;
        Map<Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(stack);
        CompoundTag forcedData = new CompoundTag();

        ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(stack.getItem());
        if (weaponData != null && weaponData.attackType() != null) {
            Enchantment targetEnchant = getAttackEnchantment(weaponData.attackType());

            if (targetEnchant != null) {
                forcedData.putString("attack", weaponData.attackType().getId());

                Enchantment currentAttackEnchant = null;
                for (ElementType type : ElementType.values()) {
                    if (type == ElementType.NONE) continue;
                    Enchantment e = getAttackEnchantment(type);
                    if (currentEnchants.containsKey(e)) {
                        currentAttackEnchant = e;
                        break;
                    }
                }

                if (isTracked) {
                    if (currentAttackEnchant != targetEnchant) {
                        if (currentAttackEnchant != null) currentEnchants.remove(currentAttackEnchant);
                        currentEnchants.put(targetEnchant, 1);
                        changed = true;
                    }
                } else {
                    if (currentAttackEnchant == null) {
                        currentEnchants.put(targetEnchant, 1);
                        changed = true;
                    }
                }
            }
        }

        ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(stack.getItem());
        if (armorData != null) {
            if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                Enchantment targetEnhance = getEnhancementEnchantment(armorData.enhanceType());
                int level = Math.max(1, Math.min(5, armorData.enhancePoints() / ElementalConfig.getStrengthPerLevel()));

                if (targetEnhance != null) {
                    forcedData.putString("enhance", armorData.enhanceType().getId());

                    if (isTracked) {
                        for (ElementType type : ElementType.values()) {
                            if (type == ElementType.NONE) continue;
                            Enchantment e = getEnhancementEnchantment(type);
                            if (e != null && e != targetEnhance && currentEnchants.containsKey(e)) {
                                currentEnchants.remove(e);
                                changed = true;
                            }
                        }
                        if (currentEnchants.getOrDefault(targetEnhance, 0) != level) {
                            currentEnchants.put(targetEnhance, level);
                            changed = true;
                        }
                    } else {
                        boolean hasEnhance = false;
                        for (ElementType type : ElementType.values()) {
                            if (type == ElementType.NONE) continue;
                            if (currentEnchants.containsKey(getEnhancementEnchantment(type))) {
                                hasEnhance = true;
                                break;
                            }
                        }
                        if (!hasEnhance) {
                            currentEnchants.put(targetEnhance, level);
                            changed = true;
                        }
                    }
                }
            }

            if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                Enchantment targetResist = getResistanceEnchantment(armorData.resistType());
                int level = Math.max(1, Math.min(5, armorData.resistPoints() / ElementalConfig.getResistPerLevel()));

                if (targetResist != null) {
                    forcedData.putString("resist", armorData.resistType().getId());

                    if (isTracked) {
                        for (ElementType type : ElementType.values()) {
                            if (type == ElementType.NONE) continue;
                            Enchantment e = getResistanceEnchantment(type);
                            if (e != null && e != targetResist && currentEnchants.containsKey(e)) {
                                currentEnchants.remove(e);
                                changed = true;
                            }
                        }
                        if (currentEnchants.getOrDefault(targetResist, 0) != level) {
                            currentEnchants.put(targetResist, level);
                            changed = true;
                        }
                    } else {
                        boolean hasResist = false;
                        for (ElementType type : ElementType.values()) {
                            if (type == ElementType.NONE) continue;
                            if (currentEnchants.containsKey(getResistanceEnchantment(type))) {
                                hasResist = true;
                                break;
                            }
                        }
                        if (!hasResist) {
                            currentEnchants.put(targetResist, level);
                            changed = true;
                        }
                    }
                }
            }
        }

        if (changed) {
            EnchantmentHelper.setEnchantments(currentEnchants, stack);
        }

        CompoundTag stackTag = stack.getOrCreateTag();
        stackTag.putBoolean(TAG_FORCED, true);
        if (!forcedData.isEmpty()) {
            stackTag.put(TAG_FORCED_DATA, forcedData);
        }
    }

    private static void removeForcedAttributes(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        Map<Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(stack);
        boolean changed = false;

        if (tag.contains(TAG_FORCED_DATA)) {
            CompoundTag data = tag.getCompound(TAG_FORCED_DATA);

            if (data.contains("attack")) {
                ElementType type = ElementType.fromId(data.getString("attack"));
                Enchantment ench = getAttackEnchantment(type);
                if (ench != null && currentEnchants.remove(ench) != null) changed = true;
            }
            if (data.contains("enhance")) {
                ElementType type = ElementType.fromId(data.getString("enhance"));
                Enchantment ench = getEnhancementEnchantment(type);
                if (ench != null && currentEnchants.remove(ench) != null) changed = true;
            }
            if (data.contains("resist")) {
                ElementType type = ElementType.fromId(data.getString("resist"));
                Enchantment ench = getResistanceEnchantment(type);
                if (ench != null && currentEnchants.remove(ench) != null) changed = true;
            }
        }

        if (changed) {
            EnchantmentHelper.setEnchantments(currentEnchants, stack);
        }
    }

    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_STRIKE.get();
            case FROST -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            case NATURE -> ModEnchantments.NATURE_STRIKE.get();
            default -> null;
        };
    }

    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
            case FROST -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
            default -> null;
        };
    }

    private static Enchantment getResistanceEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_RESIST.get();
            case FROST -> ModEnchantments.FROST_RESIST.get();
            case THUNDER -> ModEnchantments.THUNDER_RESIST.get();
            case NATURE -> ModEnchantments.NATURE_RESIST.get();
            default -> null;
        };
    }
}