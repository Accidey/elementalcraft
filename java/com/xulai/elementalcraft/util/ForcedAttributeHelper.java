// src/main/java/com/xulai/elementalcraft/util/ForcedAttributeHelper.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.world.entity.EntityType;

import java.util.Random;

/**
 * ForcedAttributeHelper 类负责解析并应用配置文件中的强制元素属性配置（FORCED_ENTITIES）。
 * 配置格式为6字段：实体ID,攻击属性,强化属性,强化点数,抗性属性,抗性点数。
 * 支持固定点数和随机范围（0-100 或 50-250），随机值会遵循全局分段概率并保持10的倍数。
 * 该类是怪物生成强制属性的核心工具类。
 *
 * ForcedAttributeHelper class is responsible for parsing and applying forced elemental attribute configurations (FORCED_ENTITIES) from the config file.
 * Configuration format has 6 fields: entity ID, attack element, enhancement element, enhancement points, resistance element, resistance points.
 * Supports fixed points and random ranges (0-100 or 50-250). Random values follow global tiered probability and are multiples of 10.
 * This class is the core utility for applying forced attributes during monster generation.
 */
public final class ForcedAttributeHelper {

    /**
     * 私有构造函数，防止实例化（工具类）。
     *
     * Private constructor to prevent instantiation (utility class).
     */
    private ForcedAttributeHelper() {}

    /**
     * 记录类，用于存储解析后的强制属性数据。
     *
     * Record class to store parsed forced attribute data.
     *
     * @param attackType 攻击属性类型 / Attack element type
     * @param enhanceType 强化属性类型 / Enhancement element type
     * @param enhancePoints 强化点数 / Enhancement points
     * @param resistType 抗性属性类型 / Resistance element type
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
     * 随机数生成器，用于点数随机范围计算。
     *
     * Random number generator used for point range calculations.
     */
    private static final Random RANDOM = new Random();

    /**
     * 根据实体类型获取其强制属性配置。
     * 若配置文件中存在匹配条目，则返回解析后的 ForcedData，否则返回 null。
     *
     * Retrieves forced attribute configuration for the given entity type.
     * Returns parsed ForcedData if a matching entry exists in the config file, otherwise null.
     *
     * @param type 实体类型 / Entity type
     * @return 强制属性数据或 null / Forced attribute data or null
     */
    public static ForcedData getForcedData(EntityType<?> type) {
        String entityId = EntityType.getKey(type).toString();

        for (String rawLine : ElementalConfig.FORCED_ENTITIES.get()) {
            String line = rawLine.replace("\"", "").replace("'", "").trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split(",", -1);
            if (parts.length != 6) continue; // 必须严格6个字段 / Must have exactly 6 fields

            if (!parts[0].trim().equals(entityId)) continue;

            try {
                ElementType attack = parts[1].trim().isBlank() ? null : ElementType.fromId(parts[1].trim().toLowerCase());
                ElementType enhance = parts[2].trim().isBlank() ? null : ElementType.fromId(parts[2].trim().toLowerCase());
                int enhancePts = parsePointsValue(parts[3].trim());

                // 抗性属性独立解析 / Independent parsing of resistance attribute
                ElementType resist = parts[4].trim().isBlank() ? null : ElementType.fromId(parts[4].trim().toLowerCase());
                int resistPts = parsePointsValue(parts[5].trim());

                // 只要有任意有效属性则返回 / Return if any valid attribute exists
                if (attack != null || enhance != null || resist != null || enhancePts > 0 || resistPts > 0) {
                    return new ForcedData(
                        attack,
                        enhance,
                        enhancePts,
                        resist,
                        resistPts
                    );
                }
            } catch (Exception e) {
                ElementalCraft.LOGGER.warn("Failed to parse forced entity line: {}", rawLine);
            }
        }
        return null;
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
    private static int parsePointsValue(String input) {
        if (input == null || input.isBlank() || "0".equals(input.trim())) {
            return 0;
        }

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
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            try {
                int val = (Integer.parseInt(s) / 10) * 10;
                return Math.max(0, Math.min(val, ElementalConfig.getMaxStatCap()));
            } catch (NumberFormatException e) {
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

        // 限制在用户指定范围并保持10的倍数 / Constrain to user range and maintain multiples of 10
        targetMin = Math.max(min, ((targetMin + 9) / 10) * 10);
        targetMax = Math.min(max, (targetMax / 10) * 10);
        if (targetMax < targetMin) targetMax = targetMin;

        int steps = (targetMax - targetMin) / 10 + 1;
        return targetMin + (RANDOM.nextInt(steps) * 10);
    }
}