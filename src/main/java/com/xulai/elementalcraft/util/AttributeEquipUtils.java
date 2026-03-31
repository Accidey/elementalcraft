package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.*;

public class AttributeEquipUtils {

    private static final Random RANDOM = new Random();

    public static int[] distributePointsToLevels(int totalPoints, int pointsPerLevel, int pieceCount) {
        if (pointsPerLevel <= 0) pointsPerLevel = 1;

        int totalLevelsNeeded = totalPoints / pointsPerLevel;
        int[] levels = new int[pieceCount];

        Arrays.fill(levels, 0);

        if (totalLevelsNeeded <= 0) {
            if (totalPoints > 0) {
                levels[RANDOM.nextInt(pieceCount)] = 1;
            }
            return levels;
        }

        int baseLevel = totalLevelsNeeded / pieceCount;
        int remainingLevels = totalLevelsNeeded % pieceCount;

        Arrays.fill(levels, baseLevel);

        for (int i = 0; i < remainingLevels; i++) {
            int chosen = RANDOM.nextInt(pieceCount);
            levels[chosen]++;
        }

        int maxConfigLevel = ElementalConfig.getMaxStatCap() / pointsPerLevel;
        if (maxConfigLevel <= 0) maxConfigLevel = 1;

        for (int i = 0; i < pieceCount; i++) {
            if (levels[i] > maxConfigLevel) levels[i] = maxConfigLevel;
        }

        return levels;
    }

    public static void applyAttackEnchant(ItemStack stack, ElementType type) {
        if (stack.isEmpty() || type == null || type == ElementType.NONE) return;
        Enchantment ench = getAttackEnchantment(type);
        if (ench != null) stack.enchant(ench, 1);
    }

    public static void applyArmorEnchants(ItemStack stack, ElementType enhType, int enhPoints, ElementType resType, int resPoints, int pointsPerLevelDivider) {
        if (stack.isEmpty()) return;

        if (pointsPerLevelDivider <= 0) pointsPerLevelDivider = 1;

        int maxConfigLevel = ElementalConfig.getMaxStatCap() / pointsPerLevelDivider;
        if (maxConfigLevel <= 0) maxConfigLevel = 1;

        int enhLv = (enhPoints > 0) ? Math.max(1, Math.min(maxConfigLevel, enhPoints / pointsPerLevelDivider)) : 0;
        int resLv = (resPoints > 0) ? Math.max(1, Math.min(maxConfigLevel, resPoints / pointsPerLevelDivider)) : 0;

        applyArmorEnchantsLevel(stack, enhType, enhLv, resType, resLv);
    }

    public static void applyArmorEnchantsLevel(ItemStack stack, ElementType enhType, int enhLv, ElementType resType, int resLv) {
        if (stack.isEmpty()) return;

        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(stack);
        Map<Enchantment, Integer> newMap = new HashMap<>(existing);

        if (enhType != null && enhType != ElementType.NONE && enhLv > 0) {
            Enchantment ench = getEnhancementEnchantment(enhType);
            if (ench != null) {
                newMap.put(ench, Math.max(enhLv, newMap.getOrDefault(ench, 0)));
            }
        }

        if (resType != null && resType != ElementType.NONE && resLv > 0) {
            Enchantment ench = getResistanceEnchantment(resType);
            if (ench != null) {
                newMap.put(ench, Math.max(resLv, newMap.getOrDefault(ench, 0)));
            }
        }

        if (!newMap.equals(existing)) {
            EnchantmentHelper.setEnchantments(newMap, stack);
        }
    }

    public static ItemStack createIronArmor(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> new ItemStack(Items.IRON_HELMET);
            case 1 -> new ItemStack(Items.IRON_CHESTPLATE);
            case 2 -> new ItemStack(Items.IRON_LEGGINGS);
            case 3 -> new ItemStack(Items.IRON_BOOTS);
            default -> ItemStack.EMPTY;
        };
    }

    public static ElementType getCounterElement(ElementType type) {
        if (type == null || type == ElementType.NONE) return ElementType.NONE;

        List<? extends String> restraints = ElementalConfig.cachedRestraints;
        if (restraints == null || restraints.isEmpty()) return ElementType.NONE;

        for (String relation : restraints) {
            String[] split = relation.split("->");
            if (split.length == 2) {
                String attackerId = split[0].trim();
                String victimId = split[1].trim();

                if (victimId.equalsIgnoreCase(type.getId())) {
                    return ElementType.fromId(attackerId);
                }
            }
        }

        return ElementType.NONE;
    }

    public static ElementType randomNonNoneElement() {
        ElementType[] valid = {ElementType.FIRE, ElementType.NATURE, ElementType.FROST, ElementType.THUNDER};
        return valid[RANDOM.nextInt(valid.length)];
    }

    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_STRIKE.get();
            case NATURE -> ModEnchantments.NATURE_STRIKE.get();
            case FROST -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            default -> null;
        };
    }

    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
            case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
            case FROST -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            default -> null;
        };
    }

    private static Enchantment getResistanceEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_RESIST.get();
            case NATURE -> ModEnchantments.NATURE_RESIST.get();
            case FROST -> ModEnchantments.FROST_RESIST.get();
            case THUNDER -> ModEnchantments.THUNDER_RESIST.get();
            default -> null;
        };
    }
}