// src/main/java/com/xulai/elementalcraft/util/CustomBiomeBias.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CustomBiomeBias 类负责解析并缓存配置文件中的自定义生物群系属性概率配置。
 * 支持新格式：biome_id:element_id,probability 和 biome_id:all,probability
 * 通过 /elementalcraft biomebias 命令添加的条目会被读取，用于在特定群系中覆盖或叠加默认的元素生成概率。
 * 使用缓存机制避免每次怪物生成时重复解析配置，提高性能。
 *
 * CustomBiomeBias class is responsible for parsing and caching custom biome attribute probability configurations from the config file.
 * Supports new format: biome_id:element_id,probability and biome_id:all,probability
 * Entries added via the /elementalcraft biomebias command are read and used to override or stack default elemental spawn probabilities in specific biomes.
 * Utilizes a caching mechanism to avoid re-parsing the configuration on every mob spawn, improving performance.
 */
public final class CustomBiomeBias {

    /**
     * 缓存解析后的群系偏向数据。
     * Key: 群系资源ID, Value: Map<元素类型, 概率加成值>
     * 使用 ConcurrentHashMap 确保多线程并发安全。
     *
     * Cache for parsed biome bias data.
     * Key: Biome ResourceLocation, Value: Map<ElementType, Probability Bonus>
     * Uses ConcurrentHashMap to ensure thread safety during concurrency.
     */
    private static final Map<ResourceLocation, Map<ElementType, Double>> BIOME_BIAS_CACHE = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，防止实例化（工具类）。
     *
     * Private constructor to prevent instantiation (utility class).
     */
    private CustomBiomeBias() {}

    /**
     * 清理缓存。
     * 在配置重载时调用，确保数据与配置文件保持一致。
     *
     * Clears the cache.
     * Called during config reload to ensure data stays consistent with the config file.
     */
    public static void clearCache() {
        BIOME_BIAS_CACHE.clear();
    }

    /**
     * 获取指定群系的自定义属性偏向配置。
     * 如果缓存中不存在，则从配置文件中解析并存入缓存。
     *
     * Retrieves custom attribute bias configuration for the specified biome.
     * If not present in cache, parses from the configuration file and stores it in the cache.
     *
     * @param id 群系资源位置 / Biome resource location
     * @return 该群系的属性概率 Map，若无配置则返回空 Map / Attribute probability Map for the biome, or empty Map if no config
     */
    public static Map<ElementType, Double> getCustomBias(ResourceLocation id) {
        return BIOME_BIAS_CACHE.computeIfAbsent(id, k -> {
            Map<ElementType, Double> bias = new HashMap<>();
            bias.put(ElementType.FIRE, 0.0);
            bias.put(ElementType.FROST, 0.0);
            bias.put(ElementType.NATURE, 0.0);
            bias.put(ElementType.THUNDER, 0.0);

            for (String line : ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get()) {
                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                String keyPart = parts[0].trim();
                String probStr = parts[1].trim();

                int colonIndex = keyPart.indexOf(':');
                if (colonIndex <= 0) continue;

                String configBiomeId = keyPart.substring(0, colonIndex);
                String elementId = keyPart.substring(colonIndex + 1);

                if (!configBiomeId.equals(id.toString())) continue;

                try {
                    double prob = Double.parseDouble(probStr);
                    if (prob < 0) prob = 0;
                    if (prob > 100) prob = 100;

                    if ("all".equalsIgnoreCase(elementId)) {
                        // all 时将概率同时加到四个元素 / When "all", add probability to all four elements
                        bias.put(ElementType.FIRE, bias.get(ElementType.FIRE) + prob);
                        bias.put(ElementType.FROST, bias.get(ElementType.FROST) + prob);
                        bias.put(ElementType.NATURE, bias.get(ElementType.NATURE) + prob);
                        bias.put(ElementType.THUNDER, bias.get(ElementType.THUNDER) + prob);
                    } else {
                        // 单个元素 / Single element
                        ElementType type = ElementType.fromId(elementId.toLowerCase());
                        if (type != null && type != ElementType.NONE) {
                            bias.put(type, bias.get(type) + prob);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }

            return bias;
        });
    }
}