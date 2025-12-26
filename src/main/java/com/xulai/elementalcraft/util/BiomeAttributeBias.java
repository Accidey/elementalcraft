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
 * BiomeAttributeBias
 * <p>
 * 中文说明：
 * 生物群系属性偏好工具类。
 * 负责实现“生物群系 + 天气 + 自定义配置”的元素属性生成偏好系统。
 * 在怪物生成元素属性时，该系统会根据其实际所在位置的环境因素决定哪种元素更容易生成。
 * 判定优先级：自定义配置 > 雷雨天气 > 默认群系偏好 > 兜底随机。
 * 已优化为使用 {@link ElementalConfig} 中的静态缓存字段，避免频繁访问配置系统，提高性能。
 * <p>
 * English Description:
 * Biome Attribute Bias utility class.
 * Implements the "Biome + Weather + Custom Config" elemental attribute generation bias system.
 * When mobs generate elemental attributes, this system determines which element is more likely based on the environmental factors at their location.
 * Priority order: Custom Config > Thunderstorm > Default Biome Bias > Fallback Random.
 * Optimized to use static cached fields from {@link ElementalConfig} to avoid frequent config access and improve performance.
 */
public final class BiomeAttributeBias {

    /**
     * 私有构造函数，防止实例化（工具类）。
     * <p>
     * Private constructor to prevent instantiation (utility class).
     */
    private BiomeAttributeBias() {}

    /**
     * 随机数生成器，用于所有概率计算。
     * <p>
     * Random number generator used for all probability calculations.
     */
    private static final Random RANDOM = new Random();

    /**
     * 根据玩家/怪物所在位置的群系和天气，返回偏好后的随机元素属性。
     * <p>
     * Returns a biased random elemental attribute based on the biome and weather at the given position.
     *
     * @param level 服务器世界 / Server level
     * @param pos   位置坐标 / Block position
     * @return 偏向后的元素类型 / Biased element type
     */
    public static ElementType getBiasedElement(ServerLevel level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        ResourceLocation biomeId = biomeHolder.unwrapKey().map(key -> key.location()).orElse(null);

        // 1. 自定义群系配置优先（支持叠加和 "all" 模式）
        // 1. Custom biome configuration has highest priority (supports stacking and "all" mode)
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

        // 2. 雷雨天气强制雷霆属性
        // 2. Thunderstorm weather forces Thunder element
        if (level.isThundering()) {
            // 使用缓存的概率检查
            // Use cached probability check
            if (RANDOM.nextDouble() < ElementalConfig.thunderstormBias / 100.0) {
                return ElementType.THUNDER;
            }
        }

        // 3. 默认群系偏好
        // 3. Default biome preferences
        float temperature = level.getBiome(pos).value().getBaseTemperature();

        // 炎热群系偏向赤焰
        // Hot biomes bias toward Fire
        if (temperature >= 0.95F) {
            // 使用静态缓存字段
            // Use static cached field
            if (RANDOM.nextDouble() < ElementalConfig.hotFireBias / 100.0) {
                return ElementType.FIRE;
            }
        }

        // 寒冷群系偏向冰霜
        // Cold biomes bias toward Frost
        if (level.getBiome(pos).value().coldEnoughToSnow(pos) || temperature <= 0.05F) {
            // 使用静态缓存字段
            // Use static cached field
            if (RANDOM.nextDouble() < ElementalConfig.coldFrostBias / 100.0) {
                return ElementType.FROST;
            }
        }

        // 森林类群系偏向自然
        // Forest-type biomes bias toward Nature
        if (isForest(biomeHolder)) {
            // 使用静态缓存字段
            // Use static cached field
            if (RANDOM.nextDouble() < ElementalConfig.forestNatureBias / 100.0) {
                return ElementType.NATURE;
            }
        }

        // 4. 兜底随机（排除 NONE）
        // 4. Fallback random (exclude NONE)
        return ElementType.values()[1 + RANDOM.nextInt(4)]; // values()[0] is NONE, random from 1 onwards
    }

    /**
     * 判断给定群系是否属于森林类群系。
     * 包含：森林、黑森林、白桦林、原始白桦林、丛林、稀疏丛林、竹林、繁花森林、风袭森林。
     * <p>
     * Determines if the given biome belongs to the forest-type category.
     * Includes: Forest, Dark Forest, Birch Forest, Old Growth Birch Forest, Jungle, Sparse Jungle, Bamboo Jungle, Flower Forest, Windswept Forest.
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