// src/main/java/com/xulai/elementalcraft/util/AttributeEquipUtils.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.enchantment.ModEnchantments;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.*;

/**
 * AttributeEquipUtils
 *
 * 中文说明：
 * 提供装备、附魔和数值计算的静态工具方法。
 * 包含附魔获取、点数分配算法、隐形头盔创建等底层逻辑。
 *
 * English description:
 * Provides static utility methods for equipment, enchantments, and value calculations.
 * Includes enchantment retrieval, point distribution algorithms, invisible helmet creation, etc.
 */
public class AttributeEquipUtils {

    private static final Random RANDOM = new Random();

    /**
     * 将总点数均匀分配到多个护甲部位并转换为附魔等级。
     *
     * Distributes total points evenly across armor pieces and converts to enchantment levels.
     *
     * @param totalPoints 总点数 / Total points
     * @param pointsPerLevel 每级点数配置 / Configured points per level
     * @param pieceCount 护甲件数 / Number of armor pieces
     * @return 每个部位的等级数组 / Array of levels for each piece
     */
    public static int[] distributePointsToLevels(int totalPoints, int pointsPerLevel, int pieceCount) {
        // 防止除以零错误 / Prevent divide by zero error
        if (pointsPerLevel <= 0) pointsPerLevel = 1;

        int totalLevelsNeeded = totalPoints / pointsPerLevel;
        int[] levels = new int[pieceCount];
        Arrays.fill(levels, 1);
        
        int remaining = totalLevelsNeeded - pieceCount;
        if (remaining <= 0) return levels;

        // 随机分配剩余等级 / Randomly distribute remaining levels
        for (int i = 0; i < remaining; i++) {
            List<Integer> candidates = new ArrayList<>();
            for (int j = 0; j < pieceCount; j++) {
                if (levels[j] < 5) candidates.add(j);
            }
            if (candidates.isEmpty()) break;
            int chosen = candidates.get(RANDOM.nextInt(candidates.size()));
            levels[chosen]++;
        }
        return levels;
    }

    /**
     * 为物品应用攻击附魔。
     *
     * Apply attack enchantment to an item.
     */
    public static void applyAttackEnchant(ItemStack stack, ElementType type) {
        if (stack.isEmpty() || type == null || type == ElementType.NONE) return;
        Enchantment ench = getAttackEnchantment(type);
        if (ench != null) stack.enchant(ench, 1);
    }

    /**
     * 为护甲应用强化和抗性附魔（基于点数）。
     *
     * Apply enhancement and resistance enchantments to armor (based on points).
     */
    public static void applyArmorEnchants(ItemStack stack, ElementType enhType, int enhPoints, ElementType resType, int resPoints, int pointsPerLevelDivider) {
        if (stack.isEmpty()) return;
        
        // 防止除以零 / Prevent divide by zero
        if (pointsPerLevelDivider <= 0) pointsPerLevelDivider = 1;

        int enhLv = (enhPoints > 0) ? Math.max(1, Math.min(5, enhPoints / pointsPerLevelDivider)) : 0;
        int resLv = (resPoints > 0) ? Math.max(1, Math.min(5, resPoints / pointsPerLevelDivider)) : 0;
        
        applyArmorEnchantsLevel(stack, enhType, enhLv, resType, resLv);
    }

    /**
     * 为护甲应用强化和抗性附魔（基于等级）。
     *
     * Apply enhancement and resistance enchantments to armor (based on levels).
     */
    public static void applyArmorEnchantsLevel(ItemStack stack, ElementType enhType, int enhLv, ElementType resType, int resLv) {
        if (stack.isEmpty()) return;
        
        Map<Enchantment, Integer> map = new HashMap<>();
        
        if (enhType != null && enhType != ElementType.NONE && enhLv > 0) {
            Enchantment ench = getEnhancementEnchantment(enhType);
            if (ench != null) map.put(ench, enhLv);
        }
        
        if (resType != null && resType != ElementType.NONE && resLv > 0) {
            Enchantment ench = getResistanceEnchantment(resType);
            if (ench != null) map.put(ench, resLv);
        }

        if (!map.isEmpty()) {
            EnchantmentHelper.setEnchantments(map, stack);
            stack.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
        }
    }

    /**
     * 创建用于存储属性的隐形皮革头盔。
     *
     * Creates an invisible leather helmet used to store attributes.
     */
    public static ItemStack createInvisibleHelmet() {
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        CompoundTag tag = helmet.getOrCreateTag();
        tag.putBoolean("Unbreakable", true);
        tag.putInt("HideFlags", 127);
        tag.putString("elementalcraft_marker", "invisible_resist");
        CompoundTag display = new CompoundTag();
        display.putInt("color", 0);
        tag.put("display", display);
        return helmet;
    }

    /**
     * 根据索引创建对应的铁甲部件。
     *
     * Creates iron armor piece based on slot index.
     * @param slotIndex 0:Boots, 1:Leggings, 2:Chestplate, 3:Helmet
     */
    public static ItemStack createIronArmor(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> new ItemStack(Items.IRON_BOOTS);
            case 1 -> new ItemStack(Items.IRON_LEGGINGS);
            case 2 -> new ItemStack(Items.IRON_CHESTPLATE);
            case 3 -> new ItemStack(Items.IRON_HELMET);
            default -> ItemStack.EMPTY;
        };
    }

    public static ElementType getCounterElement(ElementType type) {
        return switch (type) {
            case FIRE -> ElementType.FROST;
            case FROST -> ElementType.NATURE;
            case NATURE -> ElementType.THUNDER;
            case THUNDER -> ElementType.FIRE;
            default -> ElementType.NONE;
        };
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