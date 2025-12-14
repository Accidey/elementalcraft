// src/main/java/com/xulai/elementalcraft/util/ForcedItemHelper.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * ForcedItemHelper 类负责解析并缓存配置文件中的强制物品属性配置（FORCED_WEAPONS 和 FORCED_ARMOR）。
 * 通过 /elementalcraft forceditem 命令添加的条目会被读取，用于在物品合成、熔炼或捡起时自动应用对应元素附魔。
 * 使用缓存机制避免重复解析，提高性能，支持热重载（修改配置后需 /reload 或重启）。
 *
 * ForcedItemHelper class is responsible for parsing and caching forced item attribute configurations (FORCED_WEAPONS and FORCED_ARMOR) from the config file.
 * Entries added via the /elementalcraft forceditem command are read and used to automatically apply corresponding elemental enchantments when items are crafted, smelted, or picked up.
 * Utilizes a caching mechanism to avoid repeated parsing for performance, supports hot-reloading (requires /reload or restart after config changes).
 */
public final class ForcedItemHelper {

    /**
     * 私有构造函数，防止实例化（工具类）。
     *
     * Private constructor to prevent instantiation (utility class).
     */
    private ForcedItemHelper() {}

    /**
     * 随机数生成器，用于点数随机范围计算。
     *
     * Random number generator used for point range calculations.
     */
    private static final Random RANDOM = new Random();

    /**
     * 记录类，用于存储武器强制攻击属性数据。
     *
     * Record class to store forced weapon attack attribute data.
     *
     * @param attackType 攻击元素类型 / Attack element type
     */
    public record WeaponData(ElementType attackType) {}

    /**
     * 记录类，用于存储装备强制强化和抗性属性数据。
     *
     * Record class to store forced armor enhancement and resistance attribute data.
     *
     * @param enhanceType 强化元素类型 / Enhancement element type
     * @param enhancePoints 强化点数 / Enhancement points
     * @param resistType 抗性元素类型 / Resistance element type
     * @param resistPoints 抗性点数 / Resistance points
     */
    public record ArmorData(ElementType enhanceType, int enhancePoints, ElementType resistType, int resistPoints) {}

    /**
     * 武器强制属性缓存（物品 ResourceLocation -> WeaponData）。
     *
     * Cache for forced weapon attributes (item ResourceLocation -> WeaponData).
     */
    private static final Map<ResourceLocation, WeaponData> WEAPON_CACHE = new HashMap<>();

    /**
     * 装备强制属性缓存（物品 ResourceLocation -> ArmorData）。
     *
     * Cache for forced armor attributes (item ResourceLocation -> ArmorData).
     */
    private static final Map<ResourceLocation, ArmorData> ARMOR_CACHE = new HashMap<>();

    /**
     * 清空所有缓存，通常在配置保存后调用以确保下次读取最新配置。
     *
     * Clears all caches, typically called after saving configuration to ensure the latest config is read next time.
     */
    public static void clearCache() {
        WEAPON_CACHE.clear();
        ARMOR_CACHE.clear();
    }

    /**
     * 查询指定物品的强制武器攻击属性配置。
     * 若配置文件中存在匹配条目，则返回缓存的 WeaponData，否则返回 null。
     *
     * Queries forced weapon attack attribute configuration for the specified item.
     * Returns cached WeaponData if a matching entry exists in the config file, otherwise null.
     *
     * @param item 要查询的物品 / Item to query
     * @return 强制武器数据或 null / Forced weapon data or null
     */
    public static WeaponData getForcedWeapon(Item item) {
        ResourceLocation id = item.builtInRegistryHolder().key().location();
        return WEAPON_CACHE.computeIfAbsent(id, key -> {
            for (String line : ForcedItemConfig.FORCED_WEAPONS.get()) {
                String trimmed = line.replace("\"", "").trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = trimmed.split(",", -1);
                if (parts.length != 2) continue;

                if (parts[0].trim().equals(key.toString())) {
                    ElementType type = ElementType.fromId(parts[1].trim().toLowerCase());
                    if (type != null && type != ElementType.NONE) {
                        return new WeaponData(type);
                    }
                }
            }
            return null;
        });
    }

    /**
     * 查询指定物品的强制装备强化/抗性属性配置。
     * 若配置文件中存在匹配条目，则返回缓存的 ArmorData，否则返回 null。
     *
     * Queries forced armor enhancement/resistance attribute configuration for the specified item.
     * Returns cached ArmorData if a matching entry exists in the config file, otherwise null.
     *
     * @param item 要查询的物品 / Item to query
     * @return 强制装备数据或 null / Forced armor data or null
     */
    public static ArmorData getForcedArmor(Item item) {
        ResourceLocation id = item.builtInRegistryHolder().key().location();
        return ARMOR_CACHE.computeIfAbsent(id, key -> {
            for (String line : ForcedItemConfig.FORCED_ARMOR.get()) {
                String trimmed = line.replace("\"", "").trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = trimmed.split(",", -1);
                if (parts.length != 5) continue;

                if (!parts[0].trim().equals(key.toString())) continue;

                ElementType enhance = parts[1].trim().isBlank() ? null : ElementType.fromId(parts[1].trim().toLowerCase());
                int enhancePts = parsePoints(parts[2].trim());

                ElementType resist = parts[3].trim().isBlank() ? null : ElementType.fromId(parts[3].trim().toLowerCase());
                int resistPts = parsePoints(parts[4].trim());

                if (enhance != null || resist != null || enhancePts > 0 || resistPts > 0) {
                    return new ArmorData(enhance, enhancePts, resist, resistPts);
                }
            }
            return null;
        });
    }

    /**
     * 解析点数字段，支持固定值（如 "100"）和随机范围（如 "0-100" 或 "50-250"）。
     * 随机值会遵循全局分段概率，并确保结果为10的倍数且不超过点数上限。
     *
     * Parses point value field, supporting fixed values (e.g., "100") and random ranges (e.g., "0-100" or "50-250").
     * Random values follow global tiered probability and are rounded to multiples of 10, capped by max stat limit.
     *
     * @param input 输入字符串 / Input string
     * @return 解析后的点数 / Parsed point value
     */
    private static int parsePoints(String input) {
        if (input == null || input.isBlank() || "0".equals(input.trim())) return 0;

        String s = input.trim();
        if (s.contains("-")) {
            String[] range = s.split("-");
            if (range.length != 2) return 0;
            try {
                int min = Math.max(0, (Integer.parseInt(range[0].trim()) / 10) * 10);
                int max = Math.max(min, (Integer.parseInt(range[1].trim()) / 10) * 10);
                max = Math.min(max, ElementalConfig.getMaxStatCap());
                if (min >= max) return min;
                return rollInRange(min, max);
            } catch (Exception e) {
                return 0;
            }
        } else {
            try {
                int val = (Integer.parseInt(s) / 10) * 10;
                return Math.max(0, Math.min(val, ElementalConfig.getMaxStatCap()));
            } catch (Exception e) {
                return 0;
            }
        }
    }

    /**
     * 在指定范围内滚动随机点数，遵循 ElementalConfig 中的分段概率配置。
     * 结果始终为10的倍数，并限制在用户指定的 min-max 范围内。
     *
     * Rolls a random point value within the specified range, following tiered probability configuration from ElementalConfig.
     * Result is always a multiple of 10 and constrained within the user-specified min-max range.
     *
     * @param min 最小值 / Minimum value
     * @param max 最大值 / Maximum value
     * @return 随机点数 / Random point value
     */
    private static int rollInRange(int min, int max) {
        if (min >= max) return min;

        int cap = ElementalConfig.getMaxStatCap();

        double c1 = ElementalConfig.CHANCE_0_20.get();
        double c2 = ElementalConfig.CHANCE_20_40.get();
        double c3 = ElementalConfig.CHANCE_40_60.get();
        double c4 = ElementalConfig.CHANCE_60_80.get();

        double roll = RANDOM.nextDouble();
        double s1 = c1;
        double s2 = s1 + c2;
        double s3 = s2 + c3;
        double s4 = s3 + c4;

        int targetMin, targetMax;
        if (roll < s1) {
            targetMin = 0;
            targetMax = (int) (cap * 0.20);
        } else if (roll < s2) {
            targetMin = (int) (cap * 0.20) + 1;
            targetMax = (int) (cap * 0.40);
        } else if (roll < s3) {
            targetMin = (int) (cap * 0.40) + 1;
            targetMax = (int) (cap * 0.60);
        } else if (roll < s4) {
            targetMin = (int) (cap * 0.60) + 1;
            targetMax = (int) (cap * 0.80);
        } else {
            targetMin = (int) (cap * 0.80) + 1;
            targetMax = cap;
        }

        targetMin = Math.max(min, ((targetMin + 9) / 10) * 10);
        targetMax = Math.min(max, (targetMax / 10) * 10);
        if (targetMax < targetMin) targetMax = targetMin;

        int steps = (targetMax - targetMin) / 10 + 1;
        return targetMin + (RANDOM.nextInt(steps) * 10);
    }
}