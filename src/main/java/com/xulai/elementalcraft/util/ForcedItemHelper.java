// src/main/java/com/xulai/elementalcraft/util/ForcedItemHelper.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ForcedItemHelper
 * <p>
 * 中文说明：
 * 强制物品属性辅助工具类。
 * 负责解析并缓存配置文件中的强制武器和护甲属性配置（FORCED_WEAPONS 和 FORCED_ARMOR）。
 * 模组通过此类来查询特定物品是否被强制赋予了元素属性。
 * 支持热重载：在配置更新时通过 {@link #clearCache()} 清理缓存，下次访问时会自动重新解析。
 * <p>
 * English Description:
 * Forced Item Attribute Helper Class.
 * Responsible for parsing and caching forced weapon and armor attribute configurations from the config file (FORCED_WEAPONS and FORCED_ARMOR).
 * The mod uses this class to query whether specific items are forced to have elemental attributes.
 * Supports hot-reloading: Caches are cleared via {@link #clearCache()} upon config updates, triggering automatic re-parsing on next access.
 */
public final class ForcedItemHelper {

    /**
     * 强制武器属性配置的缓存。
     * Key: 物品对象
     * Value: 预解析的武器数据记录
     * 使用 ConcurrentHashMap 确保线程安全。
     * <p>
     * Cache for forced weapon attribute configurations.
     * Key: Item object
     * Value: Pre-parsed weapon data record
     * Uses ConcurrentHashMap for thread safety.
     */
    private static final Map<Item, WeaponData> WEAPON_CACHE = new ConcurrentHashMap<>();

    /**
     * 强制护甲属性配置的缓存。
     * Key: 物品对象
     * Value: 预解析的护甲配置模板（包含数值范围对象）
     * 使用 ConcurrentHashMap 确保线程安全。
     * <p>
     * Cache for forced armor attribute configurations.
     * Key: Item object
     * Value: Pre-parsed armor config template (containing range value objects)
     * Uses ConcurrentHashMap for thread safety.
     */
    private static final Map<Item, ArmorTemplate> ARMOR_CACHE = new ConcurrentHashMap<>();

    /**
     * 随机数生成器。
     * <p>
     * Random number generator.
     */
    private static final Random RANDOM = new Random();

    private ForcedItemHelper() {}

    /**
     * 清理所有缓存。
     * 在配置重载（/reload 或文件变更）时调用。
     * 清理后，下一次获取数据时会触发重新解析逻辑。
     * <p>
     * Clears all caches.
     * Called during config reload (via /reload or file changes).
     * After clearing, the next data retrieval will trigger re-parsing logic.
     */
    public static void clearCache() {
        WEAPON_CACHE.clear();
        ARMOR_CACHE.clear();
    }

    // ======================== Data Records / 数据记录类 ========================

    /**
     * 武器强制属性数据记录。
     * <p>
     * Record for forced weapon attribute data.
     *
     * @param attackType 攻击元素类型 / Attack element type
     */
    public record WeaponData(ElementType attackType) {}

    /**
     * 护甲强制属性数据记录（运行时结果）。
     * 用于返回具体的数值。
     * <p>
     * Record for forced armor attribute data (Runtime Result).
     * Used to return specific values.
     *
     * @param enhanceType  强化元素类型 / Enhancement element type
     * @param enhancePoints 强化点数 / Enhancement points
     * @param resistType    抗性元素类型 / Resistance element type
     * @param resistPoints  抗性点数 / Resistance points
     */
    public record ArmorData(ElementType enhanceType, int enhancePoints, ElementType resistType, int resistPoints) {}

    /**
     * 护甲配置模板（预解析结构）。
     * 存储解析后的范围对象，而非原始字符串，避免运行时解析。
     * <p>
     * Armor configuration template (Pre-parsed Structure).
     * Stores parsed range objects instead of raw strings to avoid runtime parsing.
     *
     * @param enhanceType 强化类型 / Enhancement type
     * @param enhanceRange 强化数值范围 / Enhancement value range
     * @param resistType 抗性类型 / Resistance type
     * @param resistRange 抗性数值范围 / Resistance value range
     */
    private record ArmorTemplate(ElementType enhanceType, RangeValue enhanceRange, ElementType resistType, RangeValue resistRange) {}

    /**
     * 数值范围辅助类。
     * 用于存储解析后的 min/max 值，并提供快速随机生成方法。
     * <p>
     * Value Range Helper Class.
     * Stores parsed min/max values and provides fast random generation methods.
     */
    private record RangeValue(int min, int max, boolean isFixed) {
        public int roll() {
            if (isFixed) return min;
            return rollInRange(min, max);
        }
    }

    // ======================== Public API / 公共接口 ========================

    /**
     * 获取指定物品的强制武器属性配置。
     * 采用懒加载机制：如果缓存为空且配置不为空，则触发解析。
     * <p>
     * Retrieves the forced weapon attribute configuration for the specified item.
     * Uses lazy loading: triggers parsing if the cache is empty but the config is not.
     *
     * @param item 物品对象 / Item object
     * @return 武器属性数据，若无配置则返回 null / Weapon data, or null if not configured
     */
    public static WeaponData getForcedWeapon(Item item) {
        if (WEAPON_CACHE.isEmpty() && !ForcedItemConfig.FORCED_WEAPONS.get().isEmpty()) {
            parseWeapons();
        }
        return WEAPON_CACHE.get(item);
    }

    /**
     * 获取指定物品的强制护甲属性配置。
     * 采用懒加载机制：如果缓存为空且配置不为空，则触发解析。
     * 每次调用都会根据预解析的范围模板生成新的随机数值。
     * <p>
     * Retrieves the forced armor attribute configuration for the specified item.
     * Uses lazy loading: triggers parsing if the cache is empty but the config is not.
     * Generates new random values based on the pre-parsed range template on each call.
     *
     * @param item 物品对象 / Item object
     * @return 护甲属性数据（包含随机生成的数值），若无配置则返回 null / Armor data (with randomized values), or null if not configured
     */
    public static ArmorData getForcedArmor(Item item) {
        if (ARMOR_CACHE.isEmpty() && !ForcedItemConfig.FORCED_ARMOR.get().isEmpty()) {
            parseArmor();
        }
        
        ArmorTemplate template = ARMOR_CACHE.get(item);
        if (template == null) return null;

        // 运行时快速生成随机数，无需字符串解析
        // Runtime fast random generation, no string parsing required
        int enhancePts = template.enhanceRange().roll();
        int resistPts = template.resistRange().roll();

        return new ArmorData(template.enhanceType(), enhancePts, template.resistType(), resistPts);
    }

    // ======================== Internal Parsing Logic / 内部解析逻辑 ========================

    /**
     * 解析武器配置列表并将结果存入缓存。
     * <p>
     * Parses the weapon configuration list and populates the cache.
     */
    private static void parseWeapons() {
        for (String line : ForcedItemConfig.FORCED_WEAPONS.get()) {
            try {
                // 格式：minecraft:diamond_sword,fire
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
                // 忽略解析错误的行
                // Ignore malformed lines
            }
        }
    }

    /**
     * 解析护甲配置列表并将结果存入缓存。
     * 这里将字符串预处理为 ArmorTemplate 对象，包含 RangeValue。
     * <p>
     * Parses the armor configuration list and populates the cache.
     * Pre-processes strings into ArmorTemplate objects containing RangeValues here.
     */
    private static void parseArmor() {
        for (String line : ForcedItemConfig.FORCED_ARMOR.get()) {
            try {
                // 格式：minecraft:diamond_chestplate,fire,100,frost,50-100
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                ResourceLocation itemId = new ResourceLocation(parts[0].trim());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null) continue;

                ElementType enhance = parseElement(parts[1]);
                RangeValue enhanceRange = parsePointsRange(parts[2]);
                
                ElementType resist = parseElement(parts[3]);
                RangeValue resistRange = parsePointsRange(parts[4]);

                // 只有当至少有一项数值可能大于0时才缓存
                // Cache only if at least one value can be greater than 0
                if (enhanceRange.max > 0 || resistRange.max > 0) {
                    ARMOR_CACHE.put(item, new ArmorTemplate(enhance, enhanceRange, resist, resistRange));
                }
            } catch (Exception ignored) {
                // 忽略解析错误的行
                // Ignore malformed lines
            }
        }
    }

    /**
     * 解析元素类型字符串。
     * <p>
     * Parses element type string.
     */
    private static ElementType parseElement(String s) {
        if (s == null || s.isBlank()) return ElementType.NONE;
        return ElementType.fromId(s.trim());
    }

    /**
     * 解析点数字符串为范围对象。
     * 预处理 "100" 或 "50-150" 格式，避免运行时重复分割字符串。
     * <p>
     * Parses points string into a RangeValue object.
     * Pre-processes "100" or "50-150" formats to avoid repetitive string splitting at runtime.
     *
     * @param s 输入字符串 / Input string
     * @return 范围值对象 / RangeValue object
     */
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

    /**
     * 在指定范围内随机生成一个数值。
     * 逻辑与之前相同，但现在仅在 RangeValue.roll() 中调用，不再包含字符串解析。
     * <p>
     * Generates a random value within the specified range.
     * Logic remains the same, but now called only within RangeValue.roll(), without string parsing.
     */
    private static int rollInRange(int min, int max) {
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        if (min > max) return min; // 已经在 parsePointsRange 处理过交换，这里双重保险
        if (min == max) return min;

        int rangeDiff = max - min;

        double c1 = ElementalConfig.chance0_20;
        double c2 = ElementalConfig.chance30_50;
        double c3 = ElementalConfig.chance60_80;

        double roll = RANDOM.nextDouble();
        double s1 = c1;
        double s2 = s1 + c2;
        double s3 = s2 + c3;

        double minPct, maxPct;

        if (roll < s1) {
            minPct = 0.0;
            maxPct = 0.20;
        } else if (roll < s2) {
            minPct = 0.30;
            maxPct = 0.50;
        } else if (roll < s3) {
            minPct = 0.60;
            maxPct = 0.80;
        } else {
            minPct = 0.90;
            maxPct = 1.0;
        }

        int segmentMin = min + (int) (rangeDiff * minPct);
        int segmentMax = min + (int) (rangeDiff * maxPct);

        if (segmentMax < segmentMin) segmentMax = segmentMin;

        int result = segmentMin + RANDOM.nextInt(segmentMax - segmentMin + 1);
        return (result / 10) * 10;
    }
}