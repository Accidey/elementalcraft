// src/main/java/com/xulai/elementalcraft/util/AttributeEquipUtils.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
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
     * 将总点数分配到多个护甲部位并转换为附魔等级。
     * 采用“基础分配 + 随机余数”的策略，确保点数分配尽可能均匀且符合总值。
     * 最大等级限制基于配置文件的点数上限动态计算。
     *
     * Distributes total points across armor pieces and converts to enchantment levels.
     * Uses a "base distribution + random remainder" strategy to ensure points are distributed as evenly as possible while adhering to the total value.
     * The maximum level limit is dynamically calculated based on the config's point cap.
     *
     * @param totalPoints 总点数 / Total points
     * @param pointsPerLevel 每级点数配置 / Configured points per level
     * @param pieceCount 护甲件数 / Number of armor pieces
     * @return 每个部位的等级数组 / Array of levels for each piece
     */
    public static int[] distributePointsToLevels(int totalPoints, int pointsPerLevel, int pieceCount) {
        // 防止除以零错误 / Prevent divide by zero error
        if (pointsPerLevel <= 0) pointsPerLevel = 1;

        // 计算所需的总等级数
        // Calculate total levels needed
        int totalLevelsNeeded = totalPoints / pointsPerLevel;
        int[] levels = new int[pieceCount];
        
        // 初始化等级数组，默认值为 0
        // Initialize level array with default value 0
        Arrays.fill(levels, 0);

        // 如果总点数不足以产生任何等级，直接返回全 0 数组
        // If total points are insufficient for any level, return array of zeros
        if (totalLevelsNeeded <= 0) return levels;

        // 计算每件装备的基础等级和剩余等级
        // Calculate base level per piece and remaining levels
        int baseLevel = totalLevelsNeeded / pieceCount;
        int remainingLevels = totalLevelsNeeded % pieceCount;

        // 为所有部位填充基础等级
        // Fill base level for all parts
        Arrays.fill(levels, baseLevel);

        // 将剩余的等级随机分配给不同的护甲部位
        // Randomly distribute the remaining levels to different armor pieces
        for (int i = 0; i < remainingLevels; i++) {
            int chosen = RANDOM.nextInt(pieceCount);
            levels[chosen]++;
        }

        // 根据配置计算允许的最大等级（总上限 / 单级点数）
        // Calculate allowed max level based on config (Total Cap / Points Per Level)
        int maxConfigLevel = ElementalConfig.getMaxStatCap() / pointsPerLevel;
        if (maxConfigLevel <= 0) maxConfigLevel = 1; // 至少允许1级 / Allow at least level 1

        // 确保每个部位的等级不超过配置允许的最大值
        // Ensure level for each piece does not exceed the max value allowed by config
        for (int i = 0; i < pieceCount; i++) {
            if (levels[i] > maxConfigLevel) levels[i] = maxConfigLevel;
        }

        return levels;
    }

    /**
     * 为物品应用攻击附魔。
     *
     * Apply attack enchantment to an item.
     *
     * @param stack 物品栈 / Item stack
     * @param type 元素类型 / Element type
     */
    public static void applyAttackEnchant(ItemStack stack, ElementType type) {
        if (stack.isEmpty() || type == null || type == ElementType.NONE) return;
        Enchantment ench = getAttackEnchantment(type);
        if (ench != null) stack.enchant(ench, 1);
    }

    /**
     * 为护甲应用强化和抗性附魔（基于点数）。
     * 自动将点数转换为等级，最大等级受配置限制。
     *
     * Apply enhancement and resistance enchantments to armor (based on points).
     * Automatically converts points to levels, max level is limited by config.
     *
     * @param stack 物品栈 / Item stack
     * @param enhType 强化元素类型 / Enhancement element type
     * @param enhPoints 强化点数 / Enhancement points
     * @param resType 抗性元素类型 / Resistance element type
     * @param resPoints 抗性点数 / Resistance points
     * @param pointsPerLevelDivider 每级点数除数 / Divider for points per level
     */
    public static void applyArmorEnchants(ItemStack stack, ElementType enhType, int enhPoints, ElementType resType, int resPoints, int pointsPerLevelDivider) {
        if (stack.isEmpty()) return;
        
        // 防止除以零 / Prevent divide by zero
        if (pointsPerLevelDivider <= 0) pointsPerLevelDivider = 1;

        // 根据配置动态计算最大等级
        // Dynamically calculate max level based on config
        int maxConfigLevel = ElementalConfig.getMaxStatCap() / pointsPerLevelDivider;
        if (maxConfigLevel <= 0) maxConfigLevel = 1;

        int enhLv = (enhPoints > 0) ? Math.max(1, Math.min(maxConfigLevel, enhPoints / pointsPerLevelDivider)) : 0;
        int resLv = (resPoints > 0) ? Math.max(1, Math.min(maxConfigLevel, resPoints / pointsPerLevelDivider)) : 0;
        
        applyArmorEnchantsLevel(stack, enhType, enhLv, resType, resLv);
    }

    /**
     * 为护甲应用强化和抗性附魔（基于等级）。
     * 并设置 HideFlags 隐藏附魔信息，保持工具提示整洁。
     *
     * Apply enhancement and resistance enchantments to armor (based on levels).
     * Also sets HideFlags to hide enchantment info, keeping tooltip clean.
     *
     * @param stack 物品栈 / Item stack
     * @param enhType 强化元素类型 / Enhancement element type
     * @param enhLv 强化等级 / Enhancement level
     * @param resType 抗性元素类型 / Resistance element type
     * @param resLv 抗性等级 / Resistance level
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
     * 该头盔不可破坏、不可见，仅作为怪物属性数据的载体。
     *
     * Creates an invisible leather helmet used to store attributes.
     * The helmet is unbreakable and invisible, serving solely as a carrier for mob attribute data.
     *
     * @return 隐形头盔物品栈 / Invisible helmet item stack
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
     *
     * @param slotIndex 0:Boots, 1:Leggings, 2:Chestplate, 3:Helmet
     * @return 铁甲物品栈 / Iron armor item stack
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

    /**
     * 获取指定元素的克制（反制）元素。
     * 关系：火->冰, 冰->自然, 自然->雷, 雷->火。
     *
     * Gets the counter element for the specified element.
     * Relation: Fire->Frost, Frost->Nature, Nature->Thunder, Thunder->Fire.
     *
     * @param type 源元素 / Source element
     * @return 克制它的元素 / Countering element
     */
    public static ElementType getCounterElement(ElementType type) {
        return switch (type) {
            case FIRE -> ElementType.FROST;
            case FROST -> ElementType.NATURE;
            case NATURE -> ElementType.THUNDER;
            case THUNDER -> ElementType.FIRE;
            default -> ElementType.NONE;
        };
    }

    /**
     * 随机获取一个非 NONE 的元素类型。
     *
     * Randomly gets a non-NONE element type.
     *
     * @return 随机元素类型 / Random element type
     */
    public static ElementType randomNonNoneElement() {
        ElementType[] valid = {ElementType.FIRE, ElementType.NATURE, ElementType.FROST, ElementType.THUNDER};
        return valid[RANDOM.nextInt(valid.length)];
    }

    /**
     * 获取对应元素的攻击附魔实例。
     *
     * Gets the attack enchantment instance for the corresponding element.
     */
    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_STRIKE.get();
            case NATURE -> ModEnchantments.NATURE_STRIKE.get();
            case FROST -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            default -> null;
        };
    }

    /**
     * 获取对应元素的强化附魔实例。
     *
     * Gets the enhancement enchantment instance for the corresponding element.
     */
    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
            case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
            case FROST -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            default -> null;
        };
    }

    /**
     * 获取对应元素的抗性附魔实例。
     *
     * Gets the resistance enchantment instance for the corresponding element.
     */
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