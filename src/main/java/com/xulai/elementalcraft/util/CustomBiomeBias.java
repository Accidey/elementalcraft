// src/main/java/com/xulai/elementalcraft/util/CustomBiomeBias.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CustomBiomeBias
 * <p>
 * 中文说明：
 * 自定义生物群系偏向工具类。
 * 负责解析并缓存配置文件中的自定义群系属性概率设置。
 * 支持格式：`biome_id:element_id,probability` 或 `biome_id:all,probability`。
 * 使用并发安全的缓存机制，避免在怪物生成的高频调用中重复解析配置文件，从而提高性能。
 * <p>
 * English Description:
 * Custom Biome Bias utility class.
 * Responsible for parsing and caching custom biome attribute probability settings from the configuration file.
 * Supports formats: `biome_id:element_id,probability` or `biome_id:all,probability`.
 * Uses a concurrent-safe caching mechanism to avoid repetitive config parsing during high-frequency mob spawning, improving performance.
 */
public final class CustomBiomeBias {

    /**
     * 缓存解析后的群系偏向数据。
     * Key: 群系资源 ID (ResourceLocation)
     * Value: 元素类型到概率加成值的映射 (Map<ElementType, Double>)
     * 使用 ConcurrentHashMap 确保多线程并发访问时的安全性。
     * <p>
     * Cache for parsed biome bias data.
     * Key: Biome ResourceLocation
     * Value: Map of ElementType to probability bonus values
     * Uses ConcurrentHashMap to ensure thread safety during concurrent access.
     */
    private static final Map<ResourceLocation, Map<ElementType, Double>> BIOME_BIAS_CACHE = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，防止实例化（工具类）。
     * <p>
     * Private constructor to prevent instantiation (utility class).
     */
    private CustomBiomeBias() {}

    /**
     * 清理缓存。
     * 在配置重载（如热重载或 /reload）时调用，确保缓存数据与配置文件保持一致。
     * <p>
     * Clears the cache.
     * Called during config reload (e.g., hot-reload or /reload) to ensure cached data stays consistent with the config file.
     */
    public static void clearCache() {
        BIOME_BIAS_CACHE.clear();
    }

    /**
     * 获取指定群系的自定义属性偏向配置。
     * 如果缓存中不存在，则从配置文件中解析并存入缓存。
     * <p>
     * Retrieves custom attribute bias configuration for the specified biome.
     * If not present in the cache, parses it from the configuration file and stores it in the cache.
     *
     * @param id 群系资源位置 / Biome resource location
     * @return 该群系的属性概率 Map，若无配置则返回初始全 0 的 Map / Attribute probability Map for the biome, or a map with 0s if no config exists
     */
    public static Map<ElementType, Double> getCustomBias(ResourceLocation id) {
        // 使用 computeIfAbsent 确保只会计算一次并存入缓存
        // Use computeIfAbsent to ensure calculation happens only once and is cached
        return BIOME_BIAS_CACHE.computeIfAbsent(id, k -> {
            Map<ElementType, Double> bias = new HashMap<>();
            // 初始化所有元素概率为 0.0
            // Initialize probability for all elements to 0.0
            bias.put(ElementType.FIRE, 0.0);
            bias.put(ElementType.FROST, 0.0);
            bias.put(ElementType.NATURE, 0.0);
            bias.put(ElementType.THUNDER, 0.0);

            // 遍历配置列表
            // Iterate through configuration list
            for (String line : ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get()) {
                // 格式解析：biome:element,probability
                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                String keyPart = parts[0].trim();
                String probStr = parts[1].trim();

                int colonIndex = keyPart.indexOf(':');
                if (colonIndex <= 0) continue;

                String configBiomeId = keyPart.substring(0, colonIndex);
                String elementId = keyPart.substring(colonIndex + 1);

                // 检查是否匹配当前查询的群系 ID
                // Check if it matches the currently requested biome ID
                if (!configBiomeId.equals(id.toString())) continue;

                try {
                    double prob = Double.parseDouble(probStr);
                    // 限制概率在 0 到 100 之间
                    // Clamp probability between 0 and 100
                    if (prob < 0) prob = 0;
                    if (prob > 100) prob = 100;

                    if ("all".equalsIgnoreCase(elementId)) {
                        // 如果是 "all"，将概率加成应用到所有四种元素
                        // If "all", apply probability bonus to all four elements
                        bias.put(ElementType.FIRE, bias.get(ElementType.FIRE) + prob);
                        bias.put(ElementType.FROST, bias.get(ElementType.FROST) + prob);
                        bias.put(ElementType.NATURE, bias.get(ElementType.NATURE) + prob);
                        bias.put(ElementType.THUNDER, bias.get(ElementType.THUNDER) + prob);
                    } else {
                        // 如果是特定元素，仅应用到该元素
                        // If specific element, apply only to that element
                        ElementType type = ElementType.fromId(elementId.toLowerCase());
                        if (type != null && type != ElementType.NONE) {
                            bias.put(type, bias.get(type) + prob);
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // 忽略格式错误的行
                    // Ignore malformed lines
                }
            }

            return bias;
        });
    }
}