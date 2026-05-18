package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;
import java.util.Random;

public final class BiomeAttributeBias {

    private BiomeAttributeBias() {}

    private static final Random RANDOM = new Random();

    public static ElementType getBiasedElement(ServerLevel level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        ResourceLocation biomeId = biomeHolder.unwrapKey().map(key -> key.location()).orElse(null);

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

        if (level.isThundering()) {
            if (RANDOM.nextDouble() < ElementalConfig.thunderstormBias / 100.0) {
                return ElementType.THUNDER;
            }
        }

        float temperature = level.getBiome(pos).value().getBaseTemperature();

        if (temperature >= 0.95F) {
            if (RANDOM.nextDouble() < ElementalConfig.hotFireBias / 100.0) {
                return ElementType.FIRE;
            }
        }

        if (level.getBiome(pos).value().coldEnoughToSnow(pos) || temperature <= 0.05F) {
            if (RANDOM.nextDouble() < ElementalConfig.coldFrostBias / 100.0) {
                return ElementType.FROST;
            }
        }

        if (isForest(biomeHolder)) {
            if (RANDOM.nextDouble() < ElementalConfig.forestNatureBias / 100.0) {
                return ElementType.NATURE;
            }
        }

        return ElementType.values()[1 + RANDOM.nextInt(4)];
    }

    private static boolean isForest(Holder<Biome> biomeHolder) {
        return biomeHolder.is(BiomeTags.IS_FOREST) || biomeHolder.is(BiomeTags.IS_JUNGLE);
    }
}