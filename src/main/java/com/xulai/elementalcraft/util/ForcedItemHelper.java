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
 *
 * 中文说明：
 * 强制物品属性辅助类。
 * 负责解析并缓存配置文件中的强制物品属性配置（FORCED_WEAPONS 和 FORCED_ARMOR）。
 * 通过 /elementalcraft forceditem 命令添加的条目会被读取，用于在物品合成、熔炼或捡起时自动应用对应元素附魔。
 * 使用 ConcurrentHashMap 缓存机制避免重复解析配置，提高性能。
 * 支持热重载：通过 clearCache() 方法清理缓存后，下次访问会自动重新解析配置。
 *
 * English description:
 * Forced Item Helper class.
 * Responsible for parsing and caching forced item attribute configurations (FORCED_WEAPONS and FORCED_ARMOR) from the config file.
 * Entries added via the /elementalcraft forceditem command are read and used to automatically apply corresponding elemental enchantments when items are crafted, smelted, or picked up.
 * Utilizes ConcurrentHashMap caching mechanism to avoid repeated configuration parsing and improve performance.
 * Supports hot-reloading: After clearing cache via clearCache(), the next access will automatically re-parse the configuration.
 */
public final class ForcedItemHelper {

    /**
     * 缓存强制武器属性配置。
     * Key: 物品对象, Value: 武器属性数据记录
     * 使用 ConcurrentHashMap 确保线程安全。
     *
     * Cache for forced weapon attribute configuration.
     * Key: Item object, Value: Weapon data record
     * Uses ConcurrentHashMap to ensure thread safety.
     */
    private static final Map<Item, WeaponData> WEAPON_CACHE = new ConcurrentHashMap<>();

    /**
     * 缓存强制护甲属性配置。
     * Key: 物品对象, Value: 护甲属性数据记录
     * 使用 ConcurrentHashMap 确保线程安全。
     *
     * Cache for forced armor attribute configuration.
     * Key: Item object, Value: Armor data record
     * Uses ConcurrentHashMap to ensure thread safety.
     */
    private static final Map<Item, ArmorData> ARMOR_CACHE = new ConcurrentHashMap<>();

    /**
     * 随机数生成器。
     *
     * Random number generator.
     */
    private static final Random RANDOM = new Random();

    /**
     * 私有构造函数，防止实例化（工具类）。
     *
     * Private constructor to prevent instantiation (utility class).
     */
    private ForcedItemHelper() {}

    /**
     * 清理所有缓存。
     * 应当在配置重载事件（ModConfigEvent.Reloading）或自动同步检测到文件变更时调用。
     * 清理后，下一次获取数据时会强制重新解析配置文件。
     *
     * Clears all caches.
     * Should be called during the config reload event (ModConfigEvent.Reloading) or when auto-sync detects file changes.
     * After clearing, the next data retrieval will force a re-parsing of the config file.
     */
    public static void clearCache() {
        WEAPON_CACHE.clear();
        ARMOR_CACHE.clear();
        ElementalCraft.LOGGER.debug("[ElementalCraft] ForcedItemHelper cache cleared.");
    }

    // ======================== Data Records / 数据记录类 ========================

    /**
     * 武器强制属性数据记录。
     *
     * Weapon forced attribute data record.
     *
     * @param attackType 攻击元素类型 / Attack element type
     */
    public record WeaponData(ElementType attackType) {}

    /**
     * 护甲强制属性数据记录。
     *
     * Armor forced attribute data record.
     *
     * @param enhanceType 强化元素类型 / Enhancement element type
     * @param enhancePoints 强化点数 / Enhancement points
     * @param resistType 抗性元素类型 / Resistance element type
     * @param resistPoints 抗性点数 / Resistance points
     */
    public record ArmorData(ElementType enhanceType, int enhancePoints, ElementType resistType, int resistPoints) {}

    // ======================== Public API / 公共接口 ========================

    /**
     * 获取指定物品的强制武器属性配置。
     * 实现了懒加载机制：如果缓存为空且配置不为空，则触发解析。
     *
     * Retrieves forced weapon attribute configuration for the specified item.
     * Implements lazy loading mechanism: triggers parsing if cache is empty but config is not.
     *
     * @param item 物品 / Item
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
     * 实现了懒加载机制：如果缓存为空且配置不为空，则触发解析。
     *
     * Retrieves forced armor attribute configuration for the specified item.
     * Implements lazy loading mechanism: triggers parsing if cache is empty but config is not.
     *
     * @param item 物品 / Item
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
     * 解析武器配置列表并填充缓存。
     *
     * Parses weapon configuration list and populates the cache.
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
            } catch (Exception e) {
                // 忽略解析错误的行，打印调试信息
                // Ignore malformed lines, log debug info
                ElementalCraft.LOGGER.debug("[ElementalCraft] Failed to parse weapon config line: {}", line);
            }
        }
    }

    /**
     * 解析护甲配置列表并填充缓存。
     *
     * Parses armor configuration list and populates the cache.
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
            } catch (Exception e) {
                // 忽略解析错误的行，打印调试信息
                // Ignore malformed lines, log debug info
                ElementalCraft.LOGGER.debug("[ElementalCraft] Failed to parse armor config line: {}", line);
            }
        }
    }

    /**
     * 解析元素类型字符串。
     *
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
     *
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
     * 使用 ElementalConfig 中的全局概率分布配置来决定生成的数值区间。
     * 最终结果会自动取整为 10 的倍数。
     *
     * Generates a random value within the specified range.
     * Uses global probability distribution configuration from ElementalConfig to determine the value interval.
     * The final result is automatically rounded to a multiple of 10.
     *
     * @param min 最小值 / Minimum value
     * @param max 最大值 / Maximum value
     * @return 随机生成的点数 / Randomly generated points
     */
    private static int rollInRange(int min, int max) {
        if (min >= max) return min;

        int cap = ElementalConfig.getMaxStatCap();

        double c1 = ElementalConfig.chance0_20;
        double c2 = ElementalConfig.chance30_50;
        double c3 = ElementalConfig.chance60_80;
        double c4 = ElementalConfig.chance90_100;

        double roll = RANDOM.nextDouble();
        double s1 = c1;
        double s2 = s1 + c2;
        double s3 = s2 + c3;

        int targetMin, targetMax;
        if (roll < s1) {
            targetMin = 10;
            targetMax = (int) (cap * 0.20);
        } else if (roll < s2) {
            targetMin = (int) (cap * 0.30);
            targetMax = (int) (cap * 0.50);
        } else if (roll < s3) {
            targetMin = (int) (cap * 0.60);
            targetMax = (int) (cap * 0.80);
        } else {
            targetMin = (int) (cap * 0.90);
            targetMax = cap;
        }

        if (targetMin > targetMax) targetMin = targetMax;

        int result = targetMin + RANDOM.nextInt(targetMax - targetMin + 1);

        if (result < min) result = min;
        if (result > max) result = max;

        return (result / 10) * 10;
    }
}