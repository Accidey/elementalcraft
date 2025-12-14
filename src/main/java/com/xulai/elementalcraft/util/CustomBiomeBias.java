// src/main/java/com/xulai/elementalcraft/util/CustomBiomeBias.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * CustomBiomeBias 类负责解析并缓存配置文件中的自定义生物群系属性概率配置。
 * 通过 /elementalcraft biomebias 命令添加的条目会被读取，用于在特定群系中覆盖或叠加默认的元素生成概率。
 * 使用缓存机制避免每次怪物生成时重复解析配置，提高性能。
 *
 * CustomBiomeBias class is responsible for parsing and caching custom biome attribute probability configurations from the config file.
 * Entries added via the /elementalcraft biomebias command are read and used to override or stack default elemental spawn probabilities in specific biomes.
 * Utilizes a caching mechanism to avoid re-parsing the configuration on every mob spawn, improving performance.
 */
public final class CustomBiomeBias {

    /**
     * 私有构造函数，防止实例化（工具类）。
     *
     * Private constructor to prevent instantiation (utility class).
     */
    private CustomBiomeBias() {}

    /**
     * 内部记录类，用于临时存储解析出的属性和概率。
     *
     * Internal record class used to temporarily store parsed attribute and probability.
     */
    private static record BiasEntry(ElementType type, double probability) {}

    /**
     * 缓存已解析的群系偏好配置，键为群系 ResourceLocation，值为各元素概率 Map。
     *
     * Cache for parsed biome bias configurations. Key is biome ResourceLocation, value is a Map of element probabilities.
     */
    private static final Map<ResourceLocation, Map<ElementType, Double>> BIOME_BIAS_CACHE = new HashMap<>();

    /**
     * 清空缓存，通常在配置保存后调用以确保下次读取最新配置。
     *
     * Clears the cache, typically called after saving configuration to ensure the latest config is read next time.
     */
    public static void clearCache() {
        BIOME_BIAS_CACHE.clear();
    }

    /**
     * 获取指定群系的自定义属性概率偏向 Map。
     * 若缓存中不存在，则实时解析配置文件并缓存结果。
     *
     * Retrieves the custom attribute probability bias Map for the specified biome.
     * If not present in cache, parses the configuration file in real-time and caches the result.
     *
     * @param biomeHolder 群系持有者 / Biome holder
     * @return 各元素概率的 Map（概率值 >= 0） / Map of element probabilities (values >= 0)
     */
    public static Map<ElementType, Double> getCustomBias(Holder<Biome> biomeHolder) {
        ResourceLocation biomeId = biomeHolder.unwrapKey().orElseThrow().location();

        return BIOME_BIAS_CACHE.computeIfAbsent(biomeId, id -> {
            // 初始化默认概率为 0 / Initialize default probabilities to 0
            Map<ElementType, Double> bias = new HashMap<>();
            bias.put(ElementType.FIRE, 0.0);
            bias.put(ElementType.FROST, 0.0);
            bias.put(ElementType.NATURE, 0.0);
            bias.put(ElementType.THUNDER, 0.0);

            // 遍历配置文件所有自定义条目 / Iterate through all custom entries in the config
            for (String line : ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get()) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = trimmed.split(",");
                if (parts.length != 3) continue;

                String configBiomeId = parts[0].trim();
                if (!configBiomeId.equals(id.toString())) continue;

                // 解析元素类型 / Parse element type
                ElementType type = ElementType.fromId(parts[1].trim().toLowerCase());
                if (type == null || type == ElementType.NONE) continue;

                // 解析并限制概率值范围 / Parse and clamp probability value
                try {
                    double prob = Double.parseDouble(parts[2].trim());
                    if (prob < 0) prob = 0;
                    if (prob > 100) prob = 100;
                    bias.put(type, bias.get(type) + prob); // 支持同一群系多条目叠加 / Support stacking multiple entries for the same biome
                } catch (NumberFormatException ignored) {}
            }

            return bias;
        });
    }
}