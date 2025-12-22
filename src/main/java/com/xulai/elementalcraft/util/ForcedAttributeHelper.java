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
 * 配置条目格式为 6 个字段：实体ID, 攻击属性, 强化属性, 强化点数, 抗性属性, 抗性点数。
 * 点数字段支持：
 * • 固定值，例如 "100" → 返回 100
 * • 范围值，例如 "50-100" → 在区间内随机选择一个值，随机分布遵循全局概率分段（chance0_20、chance30_50、chance60_80、chance90_100）。
 * • 空或 "0" → 返回 0（表示不赋予该属性）。
 * 该类实现了解析结果缓存，避免每次生物生成时重复解析配置文件以提升性能。
 * 范围点数在每次生物生成时重新随机，确保每只生物独立。
 *
 * English description:
 * Forced Attribute Helper class.
 * Responsible for parsing and applying forced elemental attribute entries from the configuration (FORCED_ENTITIES).
 * Each config entry contains 6 comma-separated fields: EntityID, AttackElement, EnhancementElement, EnhancementPoints, ResistElement, ResistPoints.
 * The points field supports fixed integers and ranges (e.g., "50-100").
 * Range values are randomized following the global tiered probability settings (chance0_20, chance30_50, chance60_80, chance90_100).
 * Parsed entries are cached to avoid repeated parsing at runtime.
 * Range points are re-rolled on each mob spawn to ensure per-entity randomness.
 */
public final class ForcedAttributeHelper {

    /**
     * 缓存已解析的强制属性配置原始字符串。
     * Key: EntityType, Value: 配置行原始字符串
     * 每次获取强制属性时重新解析并在范围时重新随机点数，确保每只生物独立随机。
     *
     * Cache for raw forced attribute configuration strings.
     * Key: EntityType, Value: Raw configuration line string
     * Re-parses and re-rolls points on each access when needed to ensure per-entity randomness.
     */
    private static final Map<EntityType<?>, String> CONFIG_CACHE = new ConcurrentHashMap<>();

    /** 随机数生成器。 / Random number generator. */
    private static final Random RANDOM = new Random();

    private ForcedAttributeHelper() {}

    /**
     * 清理缓存（用于配置热重载时调用）。
     *
     * Clears the cache (called during config hot-reload).
     */
    public static void clearCache() {
        CONFIG_CACHE.clear();
        ElementalCraft.LOGGER.debug("[ElementalCraft] ForcedAttributeHelper cache cleared.");
    }

    /**
     * 强制属性数据记录。
     *
     * Record for forced attribute data.
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
     * 获取指定实体类型的强制属性配置。
     * 若缓存中无对应配置，则从当前配置列表中查找并缓存原始字符串。
     * 每次调用均重新解析配置行，并在点数为范围时重新随机，确保每只生物属性独立。
     *
     * Retrieves forced attribute configuration for the specified entity type.
     * If not in cache, searches current config list and caches the raw string.
     * Re-parses the line and re-rolls range points on each call to ensure per-entity randomness.
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
     *
     * Parses element string to ElementType enum.
     */
    private static ElementType parseElement(String s) {
        if (s == null || s.isBlank()) return ElementType.NONE;
        return ElementType.fromId(s.toLowerCase());
    }

    /**
     * 解析点数字符串。
     * 支持固定整数值或范围格式（例如 "100" 或 "50-100"）。
     *
     * Parses points string.
     * Supports fixed integers or range format (e.g., "100" or "50-100").
     */
    private static int parsePoints(String s) {
        if (s == null || s.isBlank()) return 0;
        String val = s.trim();

        // 明确的 0 表示不赋予该属性 / Explicit 0 means no attribute
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

            // 固定值：直接返回原值 / Fixed value: return as is
            int fixed = Integer.parseInt(val);
            return Math.max(0, fixed);

        } catch (NumberFormatException e) {
            ElementalCraft.LOGGER.error("[ElementalCraft] Invalid number format in forced_entities config: {}", val);
            return 0;
        }
    }

    /**
     * 在指定范围内生成随机点数。
     * 将全局概率分段映射到用户指定的 [min, max] 区间内。
     *
     * 算法逻辑：
     * 1. 计算区间范围 range = max - min。
     * 2. 根据 config 中的 chance0_20 等概率值，决定生成在 range 的哪个百分比段内。
     * 3. 计算出最终值 = min + (range * 百分比)。
     *
     * 这确保了即使 min 很大（超过全局上限），随机值依然在 min-max 之间且符合概率分布。
     *
     * Generates random points within specified range.
     * Maps global probability tiers to the user-specified [min, max] interval.
     *
     * Algorithm logic:
     * 1. Calculate range interval = max - min.
     * 2. Determine which percentage segment of the range to generate in, based on chance0_20 etc. from config.
     * 3. Calculate final value = min + (range * percentage).
     *
     * This ensures that even if min is large (exceeding global cap), the random value remains between min-max and follows the probability distribution.
     */
    private static int rollInRange(int min, int max) {
        // 边界与合法性调整 / Boundary and validity adjustment
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        if (min > max) {
            int t = min; min = max; max = t;
        }
        if (min == max) return min;

        // 计算区间差值 / Calculate range delta
        int rangeDiff = max - min;
        
        // 获取概率配置 / Get probability config
        double c1 = ElementalConfig.chance0_20;
        double c2 = ElementalConfig.chance30_50;
        double c3 = ElementalConfig.chance60_80;

        double roll = RANDOM.nextDouble();
        double s1 = c1;
        double s2 = s1 + c2;
        double s3 = s2 + c3;

        // 定义在区间内的百分比范围 / Define percentage range within the interval
        double minPct, maxPct;

        if (roll < s1) {
            minPct = 0.0; maxPct = 0.20;       // 0-20% of range
        } else if (roll < s2) {
            minPct = 0.30; maxPct = 0.50;      // 30-50% of range
        } else if (roll < s3) {
            minPct = 0.60; maxPct = 0.80;      // 60-80% of range
        } else {
            minPct = 0.90; maxPct = 1.0;       // 90-100% of range
        }

        // 计算目标区间的具体数值 / Calculate specific values for target segment
        int segmentMin = min + (int) (rangeDiff * minPct);
        int segmentMax = min + (int) (rangeDiff * maxPct);

        // 确保段内最大值不小于最小值 / Ensure segment max is not less than min
        if (segmentMax < segmentMin) segmentMax = segmentMin;

        // 在段内随机并取整 / Randomize within segment and round
        int result = segmentMin + RANDOM.nextInt(segmentMax - segmentMin + 1);

        // 步进取整到10（可选，保持与全局风格一致，此处为了精确控制暂不强制步进，直接返回）
        // Return result directly
        return result;
    }
}