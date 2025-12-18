// src/main/java/com/xulai/elementalcraft/util/ForcedAttributeHelper.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ForcedAttributeHelper
 *
 * 中文说明：
 * 强制属性辅助类。
 * 负责解析并应用配置文件中的强制元素属性配置（FORCED_ENTITIES）。
 * 配置格式为6字段：实体ID,攻击属性,强化属性,强化点数,抗性属性,抗性点数。
 * 支持固定点数和随机范围（0-100 或 50-250），随机值会遵循全局分段概率并保持10的倍数。
 * 该类是怪物生成强制属性的核心工具类。
 *
 * English description:
 * Forced Attribute Helper class.
 * Responsible for parsing and applying forced elemental attribute configurations (FORCED_ENTITIES) from the config file.
 * Configuration format has 6 fields: entity ID, attack element, enhancement element, enhancement points, resistance element, resistance points.
 * Supports fixed points and random ranges (0-100 or 50-250). Random values follow global tiered probability and are multiples of 10.
 * This class is the core utility for applying forced attributes during monster generation.
 */
public final class ForcedAttributeHelper {

    /**
     * 缓存已解析的强制属性数据。
     * 避免每次生成怪物时重复解析配置文件，提高性能。
     * Key: 实体类型, Value: 强制属性数据记录
     *
     * Cache for parsed forced attribute data.
     * Avoids reparsing the config file every time a mob spawns to improve performance.
     * Key: EntityType, Value: Forced attribute data record
     */
    private static final Map<EntityType<?>, ForcedData> CACHE = new ConcurrentHashMap<>();

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
    private ForcedAttributeHelper() {}

    /**
     * 清理缓存。
     * 应当在配置重载事件（ModConfigEvent.Reloading）或自动同步检测到文件变更时调用。
     * 清理后，下一次获取数据时会强制重新解析配置文件。
     *
     * Clears the cache.
     * Should be called during the config reload event (ModConfigEvent.Reloading) or when auto-sync detects file changes.
     * After clearing, the next data retrieval will force a re-parsing of the config file.
     */
    public static void clearCache() {
        CACHE.clear();
        ElementalCraft.LOGGER.debug("[ElementalCraft] ForcedAttributeHelper cache cleared.");
    }

    /**
     * 强制属性数据记录类。
     * 用于存储解析后的单条配置数据。
     *
     * Forced attribute data record class.
     * Used to store parsed single configuration entry data.
     *
     * @param attackType 攻击元素类型 / Attack element type
     * @param enhanceType 强化元素类型 / Enhancement element type
     * @param enhancePoints 强化点数 / Enhancement points
     * @param resistType 抗性元素类型 / Resistance element type
     * @param resistPoints 抗性点数 / Resistance points
     */
    public record ForcedData(
            ElementType attackType,
            ElementType enhanceType,
            int enhancePoints,
            ElementType resistType,
            int resistPoints
    ) {}

    /**
     * 获取指定实体类型的强制属性配置。
     * 如果缓存中没有，则尝试解析配置并存入缓存。
     * 实现了懒加载机制。
     *
     * Retrieves the forced attribute configuration for the specified entity type.
     * If not in cache, attempts to parse the configuration and store it in the cache.
     * Implements lazy loading mechanism.
     *
     * @param type 实体类型 / Entity type
     * @return 强制属性数据，若无配置则返回 null / Forced attribute data, or null if not configured
     */
    public static ForcedData getForcedData(EntityType<?> type) {
        // 如果缓存为空，但配置列表不为空，说明需要解析（或者是缓存刚刚被清理过）
        // If cache is empty but config list is not, parsing is needed (or cache was just cleared)
        if (CACHE.isEmpty() && !ElementalConfig.FORCED_ENTITIES.get().isEmpty()) {
            parseConfig();
        }
        return CACHE.get(type);
    }

    /**
     * 解析配置文件中的所有强制实体属性条目并填充缓存。
     * 遍历配置列表，验证格式和实体有效性，然后存入 Map。
     *
     * Parses all forced entity attribute entries from the config file and populates the cache.
     * Iterates through the config list, validates format and entity validity, then stores into the Map.
     */
    private static void parseConfig() {
        for (String line : ElementalConfig.FORCED_ENTITIES.get()) {
            try {
                // 去除可能存在的引号（TOML特性）并去除首尾空格
                // Remove potential quotes (TOML feature) and trim whitespace
                String cleanLine = line.replace("\"", "").trim();
                if (cleanLine.isEmpty()) continue;

                // 格式：minecraft:zombie,fire,frost,100,nature,50-100
                // Format: minecraft:zombie,fire,frost,100,nature,50-100
                String[] parts = cleanLine.split(",");
                
                // 严格检查字段数量，防止越界错误
                // Strictly check field count to prevent out of bounds errors
                if (parts.length < 6) {
                    ElementalCraft.LOGGER.error("[ElementalCraft] Invalid forced attribute config line (not enough args): {}", cleanLine);
                    continue;
                }

                // 解析实体 ID
                // Parse Entity ID
                ResourceLocation id = new ResourceLocation(parts[0].trim());
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                    // 可能是拼写错误或该实体属于未安装的模组
                    // Could be a typo or the entity belongs to an uninstalled mod
                    ElementalCraft.LOGGER.warn("[ElementalCraft] Entity type not found in config: {}", id);
                    continue;
                }
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);

                // 解析元素类型和点数
                // Parse element types and points
                ElementType attack = parseElement(parts[1]);
                ElementType enhance = parseElement(parts[2]);
                int enhancePts = parsePoints(parts[3]);
                ElementType resist = parseElement(parts[4]);
                int resistPts = parsePoints(parts[5]);

                // 存入缓存
                // Store in cache
                CACHE.put(entityType, new ForcedData(attack, enhance, enhancePts, resist, resistPts));

            } catch (Exception e) {
                ElementalCraft.LOGGER.error("[ElementalCraft] Failed to parse forced attribute config line: " + line, e);
            }
        }
    }

    /**
     * 解析元素类型字符串。
     * 将字符串转换为 ElementType 枚举。
     *
     * Parses element type string.
     * Converts string to ElementType enum.
     *
     * @param s 输入字符串 / Input string
     * @return 元素类型 / Element Type
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
        try {
            // 处理范围格式
            // Handle range format
            if (val.contains("-")) {
                String[] range = val.split("-");
                if (range.length == 2) {
                    int min = Integer.parseInt(range[0]);
                    int max = Integer.parseInt(range[1]);
                    return rollInRange(min, max);
                }
            }
            // 处理固定值
            // Handle fixed value
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            ElementalCraft.LOGGER.error("[ElementalCraft] Invalid number format in config: {}", val);
            return 0;
        }
    }

    /**
     * 在指定范围内随机生成一个数值。
     * 使用 ElementalConfig 中的概率分布配置来决定生成的数值区间。
     * 最终结果会自动取整为 10 的倍数。
     *
     * Generates a random value within the specified range.
     * Uses probability distribution configuration from ElementalConfig to determine the value interval.
     * The final result is automatically rounded to a multiple of 10.
     *
     * @param min 最小值 / Minimum value
     * @param max 最大值 / Maximum value
     * @return 随机生成的点数 / Randomly generated points
     */
    private static int rollInRange(int min, int max) {
        if (min >= max) return min;

        int cap = ElementalConfig.getMaxStatCap();
        // 获取概率分布配置
        // Get probability distribution config
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
        // 根据概率确定生成区间
        // Determine generation interval based on probability
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

        // 修正区间
        // Correct interval
        if (targetMin > targetMax) targetMin = targetMax;

        // 在区间内随机
        // Randomize within interval
        int result = targetMin + RANDOM.nextInt(targetMax - targetMin + 1);
        
        // 确保结果在 min 和 max 限制范围内
        // Ensure result is within min and max limits
        if (result < min) result = min;
        if (result > max) result = max;

        // 向下取整为 10 的倍数
        // Round down to multiple of 10
        return (result / 10) * 10;
    }
}