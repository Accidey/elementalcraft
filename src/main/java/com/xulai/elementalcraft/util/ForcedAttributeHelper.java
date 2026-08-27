package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class ForcedAttributeHelper {

    private static final Map<EntityType<?>, List<String>> CONFIG_CACHE = new ConcurrentHashMap<>();

    private ForcedAttributeHelper() {}

    public static void clearCache() {
        CONFIG_CACHE.clear();
    }

    public record ForcedData(
            ElementType attackType,
            ElementType enhanceType,
            int enhancePoints,
            ElementType resistType,
            int resistPoints
    ) {}

    public static List<ForcedData> getForcedDataList(EntityType<?> type) {
        List<String> lines = CONFIG_CACHE.computeIfAbsent(type, t -> {
            String id = ForgeRegistries.ENTITY_TYPES.getKey(t).toString();
            return ElementalConfig.FORCED_ENTITIES.get().stream()
                    .map(s -> s.replace("\"", "").trim())
                    .filter(s -> s.startsWith(id + ","))
                    .collect(Collectors.toList());
        });

        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        List<ForcedData> result = new ArrayList<>();
        for (String line : lines) {
            ForcedData data = parseLine(line);
            if (data != null) {
                result.add(data);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Deprecated
    public static ForcedData getForcedData(EntityType<?> type) {
        List<ForcedData> list = getForcedDataList(type);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private static ForcedData parseLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 6) {
            ElementalCraft.LOGGER.error("[ElementalCraft] Invalid forced attribute config line (not enough args): {}", line);
            return null;
        }

        String entityKey = parts[0].trim();
        if (entityKey.isEmpty()) {
            ElementalCraft.LOGGER.error("[ElementalCraft] Invalid forced attribute config line (empty entity key): {}", line);
            return null;
        }

        ElementType attack = parseElement(parts[1].trim());
        ElementType enhance = parseElement(parts[2].trim());
        int enhancePts = parsePoints(parts[3].trim());
        ElementType resist = parseElement(parts[4].trim());
        int resistPts = parsePoints(parts[5].trim());

        return new ForcedData(attack, enhance, enhancePts, resist, resistPts);
    }

    private static ElementType parseElement(String s) {
        if (s == null || s.isBlank()) return ElementType.NONE;
        return ElementType.fromId(s.toLowerCase());
    }

    private static int parsePoints(String s) {
        if (s == null || s.isBlank()) return 0;
        String val = s.trim();

        try {
            if (val.contains("-")) {
                String[] range = val.split("-");
                if (range.length == 2) {
                    int rawMin = Integer.parseInt(range[0].trim());
                    int rawMax = Integer.parseInt(range[1].trim());
                    return rollInRange(rawMin, rawMax);
                }
            }

            int fixed = Integer.parseInt(val);
            return Math.max(0, fixed);

        } catch (NumberFormatException e) {
            ElementalCraft.LOGGER.error("[ElementalCraft] Invalid number format in forced_entities config: {}", val);
            return 0;
        }
    }

    private static int rollInRange(int min, int max) {
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        if (min == max) return min;

        int rangeDiff = max - min;

        double c1 = ElementalConfig.chance0_20;
        double c2 = ElementalConfig.chance20_50;
        double c3 = ElementalConfig.chance50_80;

        double roll = ThreadLocalRandom.current().nextDouble();
        double s1 = c1;
        double s2 = s1 + c2;
        double s3 = s2 + c3;

        double minPct, maxPct;

        if (roll < s1) {
            minPct = 0.0;
            maxPct = 0.20;
        } else if (roll < s2) {
            minPct = 0.20;
            maxPct = 0.50;
        } else if (roll < s3) {
            minPct = 0.50;
            maxPct = 0.80;
        } else {
            minPct = 0.80;
            maxPct = 1.0;
        }

        int segmentMin = min + (int) (rangeDiff * minPct);
        int segmentMax = min + (int) (rangeDiff * maxPct);

        if (segmentMax < segmentMin) segmentMax = segmentMin;

        return segmentMin + ThreadLocalRandom.current().nextInt(segmentMax - segmentMin + 1);
    }
}
