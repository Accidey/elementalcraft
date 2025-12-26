// src/main/java/com/xulai/elementalcraft/util/ForcedAttributeHelper.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ForcedAttributeHelper
 * <p>
 * 中文说明：
 * 强制属性辅助工具类。
 * 负责解析并应用配置文件（FORCED_ENTITIES）中定义的强制元素属性。
 * 配置条目包含 6 个字段：实体ID, 攻击属性, 强化属性, 强化点数, 抗性属性, 抗性点数。
 * 点数字段支持以下格式：
 * 1. 固定整数（如 "100"）：直接返回该数值。
 * 2. 范围区间（如 "50-100"）：在区间内随机生成一个值。随机分布逻辑遵循全局配置的概率分段（chance0_20 等），确保生成的数值符合模组整体的平衡性分布。
 * 3. 空值或 "0"：返回 0，表示不赋予该属性。
 * 该类实现了缓存机制：缓存原始配置字符串，每次生成生物时重新解析并随机化范围数值，确保每一只生物的属性数值是独立的。
 * <p>
 * English Description:
 * Forced Attribute Helper Class.
 * Responsible for parsing and applying forced elemental attribute entries from the configuration (FORCED_ENTITIES).
 * Config entries contain 6 fields: EntityID, AttackElement, EnhancementElement, EnhancementPoints, ResistElement, ResistPoints.
 * The points field supports the following formats:
 * 1. Fixed Integer (e.g., "100"): Returns the value directly.
 * 2. Range Interval (e.g., "50-100"): Generates a random value within the interval. The distribution follows the global probability tiers (chance0_20, etc.) to maintain balance consistency.
 * 3. Empty or "0": Returns 0, indicating no attribute assignment.
 * This class implements caching: it caches the raw config string and re-parses/re-rolls range values on each mob spawn to ensure unique stats per entity.
 */
public final class ForcedAttributeHelper {

    /**
     * 缓存已解析的强制属性配置原始字符串。
     * Key: 实体类型 (EntityType)
     * Value: 配置行原始字符串
     * 使用 ConcurrentHashMap 确保并发安全。
     * 注意：我们缓存的是字符串而非解析后的结果对象，以便在每次调用时对范围值进行重新随机。
     * <p>
     * Cache for raw forced attribute configuration strings.
     * Key: EntityType
     * Value: Raw configuration line string
     * Uses ConcurrentHashMap for thread safety.
     * Note: We cache the string instead of the parsed object to allow re-rolling range values on each call.
     */
    private static final Map<EntityType<?>, String> CONFIG_CACHE = new ConcurrentHashMap<>();

    /**
     * 随机数生成器。
     * <p>
     * Random number generator.
     */
    private static final Random RANDOM = new Random();

    private ForcedAttributeHelper() {}

    /**
     * 清理缓存。
     * 通常在配置热重载（/reload 或文件变更）时调用。
     * <p>
     * Clears the cache.
     * Usually called during config hot-reload (via /reload or file changes).
     */
    public static void clearCache() {
        CONFIG_CACHE.clear();
    }

    /**
     * 强制属性数据记录类 (Record)。
     * 用于传输解析后的属性数据。
     * <p>
     * Forced Attribute Data Record.
     * Used to transport parsed attribute data.
     */
    public record ForcedData(
            /** 攻击属性类型 / Attack element type */
            ElementType attackType,
            /** 强化属性类型 / Enhancement element type */
            ElementType enhanceType,
            /** 强化点数 / Enhancement points */
            int enhancePoints,
            /** 抗性属性类型 / Resistance element type */
            ElementType resistType,
            /** 抗性点数 / Resistance points */
            int resistPoints
    ) {}

    /**
     * 获取指定实体类型的强制属性配置数据。
     * 1. 检查缓存中是否有该实体的配置字符串。
     * 2. 如果没有，遍历配置文件查找匹配项并存入缓存。
     * 3. 解析配置字符串，计算具体的属性值（如果是范围则进行随机）。
     * <p>
     * Retrieves forced attribute configuration data for the specified entity type.
     * 1. Checks cache for the entity's config string.
     * 2. If missing, iterates config file to find a match and caches it.
     * 3. Parses the string and calculates specific attribute values (rolling RNG for ranges).
     *
     * @param type 实体类型 / Entity type
     * @return 解析后的强制属性数据，若无配置则返回 null / Parsed forced attribute data, or null if no config exists
     */
    public static ForcedData getForcedData(EntityType<?> type) {
        String line = CONFIG_CACHE.computeIfAbsent(type, t -> {
            String id = ForgeRegistries.ENTITY_TYPES.getKey(t).toString();
            return ElementalConfig.FORCED_ENTITIES.get().stream()
                    .map(s -> s.replace("\"", "").trim())
                    .filter(s -> s.startsWith(id + ","))
                    .findFirst()
                    .orElse(null);
        });

        if (line == null || line.isBlank()) return null;

        String[] parts = line.split(",");
        if (parts.length < 6) {
            ElementalCraft.LOGGER.error("[ElementalCraft] Invalid forced attribute config line (not enough args): {}", line);
            return null;
        }

        ElementType attack = parseElement(parts[1].trim());
        ElementType enhance = parseElement(parts[2].trim());
        int enhancePts = parsePoints(parts[3].trim());
        ElementType resist = parseElement(parts[4].trim());
        int resistPts = parsePoints(parts[5].trim());

        return new ForcedData(attack, enhance, enhancePts, resist, resistPts);
    }

    /**
     * 解析元素类型字符串为 ElementType 枚举。
     * <p>
     * Parses element string into ElementType enum.
     *
     * @param s 元素 ID 字符串 / Element ID string
     * @return 对应的元素类型 / Corresponding ElementType
     */
    private static ElementType parseElement(String s) {
        if (s == null || s.isBlank()) return ElementType.NONE;
        return ElementType.fromId(s.toLowerCase());
    }

    /**
     * 解析点数字符串。
     * 支持固定整数值（如 "100"）或范围格式（如 "50-100"）。
     * 如果是范围，则调用 {@link #rollInRange(int, int)} 进行加权随机。
     * <p>
     * Parses points string.
     * Supports fixed integers (e.g., "100") or range format (e.g., "50-100").
     * If it is a range, calls {@link #rollInRange(int, int)} for weighted randomization.
     *
     * @param s 点数字符串 / Points string
     * @return 解析或随机后的点数值 / Parsed or randomized point value
     */
    private static int parsePoints(String s) {
        if (s == null || s.isBlank()) return 0;
        String val = s.trim();

        // 明确的 0 表示不赋予该属性
        // Explicit 0 means no attribute
        if ("0".equals(val)) return 0;

        try {
            if (val.contains("-")) {
                String[] range = val.split("-");
                if (range.length == 2) {
                    int rawMin = Integer.parseInt(range[0].trim());
                    int rawMax = Integer.parseInt(range[1].trim());
                    return rollInRange(rawMin, rawMax);
                }
            }

            // 固定值：直接返回原值
            // Fixed value: return as is
            int fixed = Integer.parseInt(val);
            return Math.max(0, fixed);

        } catch (NumberFormatException e) {
            ElementalCraft.LOGGER.error("[ElementalCraft] Invalid number format in forced_entities config: {}", val);
            return 0;
        }
    }

    /**
     * 在指定范围内生成加权随机点数。
     * 将全局配置中的概率分段（0-20%, 30-50% 等）映射到用户指定的 [min, max] 区间内。
     * <p>
     * 算法逻辑：
     * 1. 计算区间长度 range = max - min。
     * 2. 根据 config 中的 chance0_20 等概率值，决定生成在 range 的哪个百分比段内（例如前 20% 或后 10%）。
     * 3. 计算出最终值 = min + (range * 百分比位置)。
     * 这样既保证了数值在用户指定的范围内，又保留了全局配置定义的“强弱分布曲线”。
     * <p>
     * Generates weighted random points within the specified range.
     * Maps the global probability tiers (0-20%, 30-50%, etc.) to the user-specified [min, max] interval.
     * <p>
     * Algorithm Logic:
     * 1. Calculate range length = max - min.
     * 2. Determine which percentage segment of the range to generate in based on config chances.
     * 3. Calculate final value = min + (range * percentage position).
     * This ensures the value stays within the user's range while preserving the "strength distribution curve" defined in the global config.
     *
     * @param min 最小值 / Minimum value
     * @param max 最大值 / Maximum value
     * @return 加权随机后的数值 / Weighted random value
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

        // 获取概率配置
        // Get probability config
        double c1 = ElementalConfig.chance0_20;
        double c2 = ElementalConfig.chance30_50;
        double c3 = ElementalConfig.chance60_80;

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

        return result;
    }
}