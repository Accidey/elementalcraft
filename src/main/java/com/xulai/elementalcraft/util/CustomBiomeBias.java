package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomBiomeBias {

    private static final Map<ResourceLocation, Map<ElementType, Double>> BIOME_BIAS_CACHE = new ConcurrentHashMap<>();

    private CustomBiomeBias() {}

    public static void clearCache() {
        BIOME_BIAS_CACHE.clear();
    }

    public static Map<ElementType, Double> getCustomBias(ResourceLocation id) {
        return BIOME_BIAS_CACHE.computeIfAbsent(id, k -> {
            Map<ElementType, Double> bias = new HashMap<>();
            bias.put(ElementType.FIRE, 0.0);
            bias.put(ElementType.FROST, 0.0);
            bias.put(ElementType.NATURE, 0.0);
            bias.put(ElementType.THUNDER, 0.0);

            for (String line : ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get()) {
                String[] parts = line.split(",");
                if (parts.length != 3) continue;

                String biomeId = parts[0].trim();
                String elementId = parts[1].trim().toLowerCase();
                String probStr = parts[2].trim();

                if (!biomeId.equals(id.toString())) continue;

                try {
                    double prob = Double.parseDouble(probStr);
                    if (prob < 0) prob = 0;
                    if (prob > 100) prob = 100;

                    if ("all".equals(elementId)) {
                        bias.put(ElementType.FIRE, bias.get(ElementType.FIRE) + prob);
                        bias.put(ElementType.FROST, bias.get(ElementType.FROST) + prob);
                        bias.put(ElementType.NATURE, bias.get(ElementType.NATURE) + prob);
                        bias.put(ElementType.THUNDER, bias.get(ElementType.THUNDER) + prob);
                    } else {
                        ElementType type = ElementType.fromId(elementId);
                        if (type != null && type != ElementType.NONE) {
                            bias.put(type, bias.get(type) + prob);
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            return bias;
        });
    }
}