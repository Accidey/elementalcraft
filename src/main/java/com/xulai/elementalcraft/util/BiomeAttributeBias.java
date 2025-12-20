// src/main/java/com/xulai/elementalcraft/util/BiomeAttributeBias.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.CustomBiomeBias;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Random;

/**
 * BiomeAttributeBias 类负责实现生物群系 + 天气 + 自定义配置的元素属性生成偏好系统。
 * 该系统在怪物生成元素属性时，根据所在群系、天气以及自定义配置决定更倾向于生成哪种元素。
 * 支持优先级：自定义配置 > 雷雨天气 > 默认群系偏好 > 兜底随机。
 *
 * BiomeAttributeBias class implements the biome + weather + custom configuration elemental attribute generation bias system.
 * When mobs generate elemental attributes, this system determines which element is more likely based on the current biome, weather, and custom configuration.
 * Priority order: custom configuration > thunderstorm weather > default biome bias > fallback random.
 */
public final class BiomeAttributeBias {

    /**
     * 私有构造函数，防止实例化（工具类）。
     *
     * Private constructor to prevent instantiation (utility class).
     */
    private BiomeAttributeBias() {}

    /**
     * 随机数生成器，用于所有概率计算。
     *
     * Random number generator used for all probability calculations.
     */
    private static final Random RANDOM = new Random();

    /**
     * 根据玩家/怪物所在位置的群系和天气，返回偏好后的随机元素属性。
     *
     * Returns a biased random elemental attribute based on the biome and weather at the given position.
     *
     * @param level 服务器世界 / Server level
     * @param pos 位置坐标 / Block position
     * @return 偏向后的元素类型 / Biased element type
     */
    public static ElementType getBiasedElement(ServerLevel level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        ResourceLocation biomeId = biomeHolder.unwrapKey().map(key -> key.location()).orElse(null);

        // 1. 自定义群系配置优先（支持叠加和 all 模式） / 1. Custom biome configuration has highest priority
        if (biomeId != null) {
            Map<ElementType, Double> customBias = CustomBiomeBias.getCustomBias(biomeId);
            double totalCustom = customBias.values().stream().mapToDouble(Double::doubleValue).sum();
            if (totalCustom > 0) {
                double roll = RANDOM.nextDouble() * totalCustom;
                double current = 0;
                for (Map.Entry<ElementType, Double> entry : customBias.entrySet()) {
                    if (entry.getValue() > 0) {
                        current += entry.getValue();
                        if (roll < current) {
                            return entry.getKey();
                        }
                    }
                }
            }
        }

        // 2. 雷雨天气强制雷霆属性 / 2. Thunderstorm weather forces thunder element
        if (level.isThundering()) {
            return ElementType.THUNDER;
        }

        // 3. 默认群系偏好 / 3. Default biome preferences
        float temperature = level.getBiome(pos).value().getBaseTemperature();

        // 炎热群系偏向赤焰 / Hot biomes bias toward fire
        if (temperature >= 0.95F) {
            if (RANDOM.nextDouble() < ElementalConfig.HOT_FIRE_BIAS.get() / 100.0) {
                return ElementType.FIRE;
            }
        }

        // 寒冷群系偏向冰霜 / Cold biomes bias toward frost
        if (level.getBiome(pos).value().coldEnoughToSnow(pos) || temperature <= 0.05F) {
            if (RANDOM.nextDouble() < ElementalConfig.COLD_FROST_BIAS.get() / 100.0) {
                return ElementType.FROST;
            }
        }

        // 森林类群系偏向自然 / Forest-type biomes bias toward nature
        if (isForest(biomeHolder)) {
            if (RANDOM.nextDouble() < ElementalConfig.FOREST_NATURE_BIAS.get() / 100.0) {
                return ElementType.NATURE;
            }
        }

        // 4. 兜底随机（排除 NONE） / 4. Fallback random (exclude NONE)
        return ElementType.values()[1 + RANDOM.nextInt(4)]; // values()[0] 是 NONE，从 1 开始随机
    }

    /**
     * 判断给定群系是否属于森林类群系。
     *
     * Determines if the given biome belongs to the forest-type category.
     *
     * @param biomeHolder 群系持有者 / Biome holder
     * @return 是否为森林类群系 / Whether it is a forest-type biome
     */
    private static boolean isForest(Holder<Biome> biomeHolder) {
        return biomeHolder.is(Biomes.FOREST) ||
               biomeHolder.is(Biomes.DARK_FOREST) ||
               biomeHolder.is(Biomes.BIRCH_FOREST) ||
               biomeHolder.is(Biomes.OLD_GROWTH_BIRCH_FOREST) ||
               biomeHolder.is(Biomes.JUNGLE) ||
               biomeHolder.is(Biomes.SPARSE_JUNGLE) ||
               biomeHolder.is(Biomes.BAMBOO_JUNGLE) ||
               biomeHolder.is(Biomes.FLOWER_FOREST) ||
               biomeHolder.is(Biomes.WINDSWEPT_FOREST);
    }
}