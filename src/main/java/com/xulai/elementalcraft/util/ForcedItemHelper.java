package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class ForcedItemHelper {

    private static final Map<Item, WeaponData> WEAPON_CACHE = new ConcurrentHashMap<>();

    private static final Map<Item, ArmorTemplate> ARMOR_CACHE = new ConcurrentHashMap<>();

    private ForcedItemHelper() {}

    public static void clearCache() {
        WEAPON_CACHE.clear();
        ARMOR_CACHE.clear();
    }

    public record WeaponData(ElementType attackType) {}

    public record ArmorData(ElementType enhanceType, int enhancePoints, ElementType resistType, int resistPoints) {}

    private record ArmorTemplate(ElementType enhanceType, RangeValue enhanceRange, ElementType resistType, RangeValue resistRange) {}

    private record RangeValue(int min, int max, boolean isFixed) {
        public int roll() {
            if (isFixed) return min;
            return rollInRange(min, max);
        }
    }

    public static WeaponData getForcedWeapon(Item item) {
        if (WEAPON_CACHE.isEmpty() && !ForcedItemConfig.FORCED_WEAPONS.get().isEmpty()) {
            parseWeapons();
        }
        return WEAPON_CACHE.get(item);
    }

    public static ArmorData getForcedArmor(Item item) {
        if (ARMOR_CACHE.isEmpty() && !ForcedItemConfig.FORCED_ARMOR.get().isEmpty()) {
            parseArmor();
        }

        ArmorTemplate template = ARMOR_CACHE.get(item);
        if (template == null) return null;

        int enhancePts = template.enhanceRange().roll();
        int resistPts = template.resistRange().roll();

        return new ArmorData(template.enhanceType(), enhancePts, template.resistType(), resistPts);
    }

    @SuppressWarnings("deprecation")
    private static void parseWeapons() {
        for (String line : ForcedItemConfig.FORCED_WEAPONS.get()) {
            try {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                ResourceLocation itemId = new ResourceLocation(parts[0].trim());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null) continue;

                ElementType attack = ElementType.fromId(parts[1].trim());
                if (attack != ElementType.NONE) {
                    WEAPON_CACHE.put(item, new WeaponData(attack));
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void parseArmor() {
        for (String line : ForcedItemConfig.FORCED_ARMOR.get()) {
            try {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                ResourceLocation itemId = new ResourceLocation(parts[0].trim());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null) continue;

                ElementType enhance = parseElement(parts[1]);
                RangeValue enhanceRange = parsePointsRange(parts[2]);

                ElementType resist = parseElement(parts[3]);
                RangeValue resistRange = parsePointsRange(parts[4]);

                if (enhanceRange.max > 0 || resistRange.max > 0) {
                    ARMOR_CACHE.put(item, new ArmorTemplate(enhance, enhanceRange, resist, resistRange));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static ElementType parseElement(String s) {
        if (s == null || s.isBlank()) return ElementType.NONE;
        return ElementType.fromId(s.trim());
    }

    private static RangeValue parsePointsRange(String s) {
        if (s == null || s.isBlank()) return new RangeValue(0, 0, true);
        String val = s.trim();

        try {
            if (val.contains("-")) {
                String[] range = val.split("-");
                if (range.length == 2) {
                    int min = Integer.parseInt(range[0]);
                    int max = Integer.parseInt(range[1]);
                    return new RangeValue(Math.min(min, max), Math.max(min, max), false);
                }
            }
            int fixed = Integer.parseInt(val);
            return new RangeValue(fixed, fixed, true);
        } catch (NumberFormatException e) {
            return new RangeValue(0, 0, true);
        }
    }

    private static int rollInRange(int min, int max) {
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        if (min > max) return min;
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

        int result = segmentMin + ThreadLocalRandom.current().nextInt(segmentMax - segmentMin + 1);
        return (result / 10) * 10;
    }
}
