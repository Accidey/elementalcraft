// src/main/java/com/xulai/elementalcraft/util/ForcedItemHelper.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ForcedItemHelper
 * <p>
 * 中文说明：
 * 强制物品属性辅助工具类。
 * 负责解析并缓存配置文件中的强制武器和护甲属性配置（FORCED_WEAPONS 和 FORCED_ARMOR）。
 * 模组通过此类来查询特定物品是否被强制赋予了元素属性。
 * 支持热重载：在配置更新时通过 {@link #clearCache()} 清理缓存，下次访问时会自动重新解析。
 * <p>
 * English Description:
 * Forced Item Attribute Helper Class.
 * Responsible for parsing and caching forced weapon and armor attribute configurations from the config file (FORCED_WEAPONS and FORCED_ARMOR).
 * The mod uses this class to query whether specific items are forced to have elemental attributes.
 * Supports hot-reloading: Caches are cleared via {@link #clearCache()} upon config updates, triggering automatic re-parsing on next access.
 */
public final class ForcedItemHelper {

    /**
     * 强制武器属性配置的缓存。
     * Key: 物品对象
     * Value: 武器数据记录
     * 使用 ConcurrentHashMap 确保线程安全。
     * <p>
     * Cache for forced weapon attribute configurations.
     * Key: Item object
     * Value: Weapon data record
     * Uses ConcurrentHashMap for thread safety.
     */
    private static final Map<Item, WeaponData> WEAPON_CACHE = new ConcurrentHashMap<>();

    /**
     * 强制护甲属性配置的缓存。
     * Key: 物品对象
     * Value: 护甲数据记录
     * 使用 ConcurrentHashMap 确保线程安全。
     * <p>
     * Cache for forced armor attribute configurations.
     * Key: Item object
     * Value: Armor data record
     * Uses ConcurrentHashMap for thread safety.
     */
    private static final Map<Item, ArmorData> ARMOR_CACHE = new ConcurrentHashMap<>();

    /**
     * 随机数生成器。
     * <p>
     * Random number generator.
     */
    private static final Random RANDOM = new Random();

    private ForcedItemHelper() {}

    /**
     * 清理所有缓存。
     * 在配置重载（/reload 或文件变更）时调用。
     * 清理后，下一次获取数据时会触发重新解析逻辑。
     * <p>
     * Clears all caches.
     * Called during config reload (via /reload or file changes).
     * After clearing, the next data retrieval will trigger re-parsing logic.
     */
    public static void clearCache() {
        WEAPON_CACHE.clear();
        ARMOR_CACHE.clear();
    }

    // ======================== Data Records / 数据记录类 ========================

    /**
     * 武器强制属性数据记录。
     * <p>
     * Record for forced weapon attribute data.
     *
     * @param attackType 攻击元素类型 / Attack element type
     */
    public record WeaponData(ElementType attackType) {}

    /**
     * 护甲强制属性数据记录。
     * <p>
     * Record for forced armor attribute data.
     *
     * @param enhanceType   强化元素类型 / Enhancement element type
     * @param enhancePoints 强化点数 / Enhancement points
     * @param resistType    抗性元素类型 / Resistance element type
     * @param resistPoints  抗性点数 / Resistance points
     */
    public record ArmorData(ElementType enhanceType, int enhancePoints, ElementType resistType, int resistPoints) {}

    // ======================== Public API / 公共接口 ========================

    /**
     * 获取指定物品的强制武器属性配置。
     * 采用懒加载机制：如果缓存为空且配置不为空，则触发解析。
     * <p>
     * Retrieves the forced weapon attribute configuration for the specified item.
     * Uses lazy loading: triggers parsing if the cache is empty but the config is not.
     *
     * @param item 物品对象 / Item object
     * @return 武器属性数据，若无配置则返回 null / Weapon data, or null if not configured
     */
    public static WeaponData getForcedWeapon(Item item) {
        // 懒加载：缓存为空但配置不为空时解析
        // Lazy load: parse when cache is empty but config is not
        if (WEAPON_CACHE.isEmpty() && !ForcedItemConfig.FORCED_WEAPONS.get().isEmpty()) {
            parseWeapons();
        }
        return WEAPON_CACHE.get(item);
    }

    /**
     * 获取指定物品的强制护甲属性配置。
     * 采用懒加载机制：如果缓存为空且配置不为空，则触发解析。
     * <p>
     * Retrieves the forced armor attribute configuration for the specified item.
     * Uses lazy loading: triggers parsing if the cache is empty but the config is not.
     *
     * @param item 物品对象 / Item object
     * @return 护甲属性数据，若无配置则返回 null / Armor data, or null if not configured
     */
    public static ArmorData getForcedArmor(Item item) {
        // 懒加载：缓存为空但配置不为空时解析
        // Lazy load: parse when cache is empty but config is not
        if (ARMOR_CACHE.isEmpty() && !ForcedItemConfig.FORCED_ARMOR.get().isEmpty()) {
            parseArmor();
        }
        return ARMOR_CACHE.get(item);
    }

    // ======================== Internal Parsing Logic / 内部解析逻辑 ========================

    /**
     * 解析武器配置列表并将结果存入缓存。
     * <p>
     * Parses the weapon configuration list and populates the cache.
     */
    private static void parseWeapons() {
        for (String line : ForcedItemConfig.FORCED_WEAPONS.get()) {
            try {
                // 格式：minecraft:diamond_sword,fire
                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                ResourceLocation itemId = new ResourceLocation(parts[0].trim());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null) continue;

                ElementType attack = ElementType.fromId(parts[1].trim());
                if (attack != ElementType.NONE) {
                    WEAPON_CACHE.put(item, new WeaponData(attack));
                }
            } catch (Exception ignored) {
                // 忽略解析错误的行
                // Ignore malformed lines
            }
        }
    }

    /**
     * 解析护甲配置列表并将结果存入缓存。
     * <p>
     * Parses the armor configuration list and populates the cache.
     */
    private static void parseArmor() {
        for (String line : ForcedItemConfig.FORCED_ARMOR.get()) {
            try {
                // 格式：minecraft:diamond_chestplate,fire,100,frost,50
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                ResourceLocation itemId = new ResourceLocation(parts[0].trim());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null) continue;

                ElementType enhance = parseElement(parts[1]);
                int enhancePts = parsePoints(parts[2]);
                ElementType resist = parseElement(parts[3]);
                int resistPts = parsePoints(parts[4]);

                if (enhancePts > 0 || resistPts > 0) {
                    ARMOR_CACHE.put(item, new ArmorData(enhance, enhancePts, resist, resistPts));
                }
            } catch (Exception ignored) {
                // 忽略解析错误的行
                // Ignore malformed lines
            }
        }
    }

    /**
     * 解析元素类型字符串。
     * <p>
     * Parses element type string.
     *
     * @param s 输入字符串 / Input string
     * @return 元素类型 / ElementType
     */
    private static ElementType parseElement(String s) {
        if (s == null || s.isBlank()) return ElementType.NONE;
        return ElementType.fromId(s.trim());
    }

    /**
     * 解析点数字符串。
     * 支持固定整数值或范围格式（例如 "100" 或 "50-150"）。
     * <p>
     * Parses points string.
     * Supports fixed integer values or range format (e.g., "100" or "50-150").
     *
     * @param s 输入字符串 / Input string
     * @return 解析后的点数值（如果是范围则返回随机结果） / Parsed point value (returns random result if it is a range)
     */
    private static int parsePoints(String s) {
        if (s == null || s.isBlank()) return 0;
        String val = s.trim();
        // 处理范围格式
        // Handle range format
        if (val.contains("-")) {
            String[] range = val.split("-");
            if (range.length == 2) {
                try {
                    int min = Integer.parseInt(range[0]);
                    int max = Integer.parseInt(range[1]);
                    return rollInRange(min, max);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        // 处理固定值
        // Handle fixed value
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 在指定范围内随机生成一个数值。
     * 使用 {@link com.xulai.elementalcraft.config.ElementalConfig} 中的全局概率分布配置来决定生成的数值区间。
     * 最终结果会自动取整为 10 的倍数。
     * <p>
     * Generates a random value within the specified range.
     * Uses global probability distribution configuration from {@link com.xulai.elementalcraft.config.ElementalConfig} to determine the value interval.
     * The final result is automatically rounded to a multiple of 10.
     *
     * @param min 最小值 / Minimum value
     * @param max 最大值 / Maximum value
     * @return 随机生成的点数 / Randomly generated points
     */
    private static int rollInRange(int min, int max) {
        // 边界与合法性调整
        // Boundary and validity adjustment
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        if (min == max) return min;

        // 计算区间差值
        // Calculate range delta
        int rangeDiff = max - min;

        // 获取全局概率配置
        // Get global probability config
        double c1 = ElementalConfig.chance0_20;
        double c2 = ElementalConfig.chance30_50;
        double c3 = ElementalConfig.chance60_80;
        // double c4 = ElementalConfig.chance90_100; // Not explicitly needed for calculation logic

        double roll = RANDOM.nextDouble();
        double s1 = c1;
        double s2 = s1 + c2;
        double s3 = s2 + c3;

        // 定义在区间内的百分比范围
        // Define percentage range within the interval
        double minPct, maxPct;

        if (roll < s1) {
            minPct = 0.0;
            maxPct = 0.20;       // 0-20% of range
        } else if (roll < s2) {
            minPct = 0.30;
            maxPct = 0.50;      // 30-50% of range
        } else if (roll < s3) {
            minPct = 0.60;
            maxPct = 0.80;      // 60-80% of range
        } else {
            minPct = 0.90;
            maxPct = 1.0;       // 90-100% of range
        }

        // 计算目标区间的具体数值范围
        // Calculate specific value range for target segment
        int segmentMin = min + (int) (rangeDiff * minPct);
        int segmentMax = min + (int) (rangeDiff * maxPct);

        // 确保段内最大值不小于最小值
        // Ensure segment max is not less than min
        if (segmentMax < segmentMin) segmentMax = segmentMin;

        // 在段内随机并取整
        // Randomize within segment and round
        int result = segmentMin + RANDOM.nextInt(segmentMax - segmentMin + 1);

        // 取整到 10
        // Round to nearest 10
        return (result / 10) * 10;
    }
}