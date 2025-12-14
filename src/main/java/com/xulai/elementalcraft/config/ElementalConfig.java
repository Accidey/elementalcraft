// src/main/java/com/xulai/elementalcraft/config/ElementalConfig.java
package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;
import java.util.Random;

/**
 * Configuration class for ElementalCraft.
 * Includes Mob attributes, damage calculation, restraints, and biome preferences.
 * <p>
 * 元素工艺模组配置文件类。
 * 包含怪物属性、伤害计算、克制关系以及生物群系偏好设置。
 */
public final class ElementalConfig {
    public static final ForgeConfigSpec SPEC;

    // ======================== Mob Attribute Generation System / 怪物属性生成系统 ========================
    public static final ForgeConfigSpec.DoubleValue MOB_ATTRIBUTE_CHANCE_HOSTILE;
    public static final ForgeConfigSpec.DoubleValue MOB_ATTRIBUTE_CHANCE_NEUTRAL;
    public static final ForgeConfigSpec.DoubleValue ATTACK_ATTRIBUTE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue COUNTER_RESIST_CHANCE;

    public static final ForgeConfigSpec.DoubleValue CHANCE_0_20;
    public static final ForgeConfigSpec.DoubleValue CHANCE_20_40;
    public static final ForgeConfigSpec.DoubleValue CHANCE_40_60;
    public static final ForgeConfigSpec.DoubleValue CHANCE_60_80;
    public static final ForgeConfigSpec.DoubleValue CHANCE_80_100;

    // ======================== Values Per Level / 每级数值配置 ========================
    public static final ForgeConfigSpec.IntValue STRENGTH_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue RESIST_PER_LEVEL;

    // ======================= Elemental Damage System / 属性伤害系统 ========================
    public static final ForgeConfigSpec.IntValue    STRENGTH_PER_HALF_DAMAGE;
    public static final ForgeConfigSpec.IntValue    RESIST_PER_HALF_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue ELEMENTAL_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ELEMENTAL_RESISTANCE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue    MAX_STAT_CAP;

    // ======================== Elemental Restraint System / 属性克制系统 ========================
    public static final ForgeConfigSpec.DoubleValue RESTRAINT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WEAK_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELEMENT_RESTRAINTS;

    // ======================== Resistance to Healing / 高抗性转治疗 ========================
    public static final ForgeConfigSpec.BooleanValue RESISTANCE_TO_HEAL_ENABLED;
    public static final ForgeConfigSpec.IntValue     RESISTANCE_TO_HEAL_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue RESISTANCE_TO_HEAL_MULTIPLIER;

    // ======================== Forced Attribute System / 强制属性系统 ========================
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FORCED_ENTITIES;

    // ======================== Nether Forced Fire / 下界维度全生物强制赤焰属性 ========================
    public static final ForgeConfigSpec.BooleanValue NETHER_DIMENSION_FORCED_FIRE;
    public static final ForgeConfigSpec.IntValue     NETHER_FIRE_POINTS;

    // ======================== End Forced Thunder / 末地维度全生物强制雷属性 ========================
    public static final ForgeConfigSpec.BooleanValue END_DIMENSION_FORCED_THUNDER;
    public static final ForgeConfigSpec.IntValue     END_THUNDER_POINTS;

    // ======================== Blacklist System / 黑名单系统 ========================
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ENTITIES;

    // ======================== Biome & Weather Bias / 生物群系与天气属性偏好 ========================
    public static final ForgeConfigSpec.DoubleValue HOT_FIRE_BIAS;
    public static final ForgeConfigSpec.DoubleValue COLD_FROST_BIAS;
    public static final ForgeConfigSpec.DoubleValue FOREST_NATURE_BIAS;
    public static final ForgeConfigSpec.DoubleValue THUNDERSTORM_THUNDER_BIAS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_BIOME_ATTRIBUTE_BIAS;

    private static final Random RANDOM = new Random();

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        // ======================== Mob Attribute Generation System / 怪物属性生成系统 ========================
        BUILDER.comment("Mob Attribute Generation System (Fully supports hot reload)", "怪物属性生成系统（全部支持热重载）")
                .push("mob_attribute_generation");

        MOB_ATTRIBUTE_CHANCE_HOSTILE = BUILDER
                .comment("Chance for hostile mobs (e.g., Zombies, Skeletons) to become elemental",
                         "敌对生物（如僵尸、骷髅）成为属性生物的概率")
                .defineInRange("mob_attribute_chance_hostile", 0.40, 0.0, 1.0);

        MOB_ATTRIBUTE_CHANCE_NEUTRAL = BUILDER
                .comment("Chance for neutral mobs (e.g., Piglins, Endermen) to become elemental",
                         "中立生物（如猪灵、末影人）成为属性生物的概率")
                .defineInRange("mob_attribute_chance_neutral", 0.30, 0.0, 1.0);

        ATTACK_ATTRIBUTE_CHANCE = BUILDER
                .comment("Chance to gain an attack attribute (weapon enchantment) when generating attributes",
                         "怪物生成属性时，获得攻击属性（武器附魔）的概率")
                .defineInRange("attack_attribute_chance", 0.60, 0.0, 1.0);

        COUNTER_RESIST_CHANCE = BUILDER
                .comment("Chance to gain resistance against the element that counters them",
                         "怪物生成属性时，获得对应抗性（防止被自己克制）的概率")
                .defineInRange("counter_resist_chance", 0.70, 0.0, 1.0);

        BUILDER.comment("Probability Distribution for Mob Strength & Resistance (Sum should be <= 1.0)",
                        "【怪物强化值与抗性统一分段概率】（总和建议 ≤1.0）");
        CHANCE_0_20   = BUILDER.defineInRange("chance_0_20",   0.40, 0.0, 1.0);
        CHANCE_20_40  = BUILDER.defineInRange("chance_20_40",  0.30, 0.0, 1.0);
        CHANCE_40_60  = BUILDER.defineInRange("chance_40_60",  0.15, 0.0, 1.0);
        CHANCE_60_80  = BUILDER.defineInRange("chance_60_80",  0.10, 0.0, 1.0);
        CHANCE_80_100 = BUILDER.defineInRange("chance_80_100", 0.05, 0.0, 1.0);

        BUILDER.pop();

        // ======================== Attribute Value System / 属性数值系统 ========================
        BUILDER.comment("Attribute Value System (Supports hot reload)", "属性数值系统（支持热重载）")
                .push("attribute_values");

        STRENGTH_PER_LEVEL = BUILDER
                .comment("Strength value provided per level of [Elemental Strength] enchantment",
                         "每级【属性强化】附魔提供多少强化值（玩家装备显示的就是这个数 × 等级）")
                .defineInRange("strength_per_level", 5, 1, 1000);

        RESIST_PER_LEVEL = BUILDER
                .comment("Resistance value provided per level of [Hidden Resistance] enchantment",
                         "每级【隐藏抗性】附魔提供多少抗性值")
                .defineInRange("resist_per_level", 5, 1, 1000);

        BUILDER.pop();

        // ======================== Elemental Damage System / 属性伤害系统 ========================
        BUILDER.comment("Elemental Damage System (Fully supports hot reload)", "属性伤害系统（全部支持热重载）")
                .push("elemental_damage");

        STRENGTH_PER_HALF_DAMAGE = BUILDER
                .comment("How many strength points increase elemental damage by 0.5",
                         "多少点强化值使属性伤害增加0.5")
                .defineInRange("strength_per_half_damage", 10, 1, 1000);

        RESIST_PER_HALF_REDUCTION = BUILDER
                .comment("How many resistance points reduce elemental damage by 0.5",
                         "多少点抗性值使属性伤害减少0.5")
                .defineInRange("resist_per_half_reduction", 10, 1, 1000);

        ELEMENTAL_DAMAGE_MULTIPLIER = BUILDER
                .comment("Global Elemental Damage Multiplier",
                         "全局属性伤害倍率")
                .defineInRange("elemental_damage_multiplier", 1.0, 0.0, 100.0);

        ELEMENTAL_RESISTANCE_MULTIPLIER = BUILDER
                .comment("Global Elemental Resistance Multiplier",
                         "全局属性抗性减免倍率")
                .defineInRange("elemental_resistance_multiplier", 1.0, 0.0, 100.0);

        MAX_STAT_CAP = BUILDER
                .comment("Maximum point cap for a single attribute",
                         "单种属性最大点数上限")
                .defineInRange("max_stat_cap", 100, 10, 100000);

        BUILDER.pop();

        // ======================== Restraint System / 属性克制系统 ========================
        BUILDER.comment("Elemental Restraint System (Supports hot reload)", "属性克制系统（支持热重载）")
                .push("elemental_restraints");

        RESTRAINT_MULTIPLIER = BUILDER
                .comment("Damage multiplier when target is restrained (Victim takes more damage)",
                         "被克制时伤害倍率")
                .defineInRange("restraint_multiplier", 1.5, 0.1, 10.0);

        WEAK_MULTIPLIER = BUILDER
                .comment("Damage multiplier when target resists the element (Victim takes less damage)",
                         "克制时（效果微弱）伤害倍率")
                .defineInRange("weak_multiplier", 0.5, 0.1, 10.0);

        ELEMENT_RESTRAINTS = BUILDER
                .comment("Restraint Relations: Attacker -> Victim (Supports hot reload)",
                         "克制关系：克制方 -> 被克制方（支持热重载）",
                         "Supported IDs: fire, nature, frost, thunder",
                         "支持属性ID：fire, nature, frost, thunder",
                         "Example: fire->frost means Fire restrains Frost",
                         "示例：fire->frost 表示赤焰克制冰霜")
                .defineList("element_restraints",
                        List.of("fire->frost", "frost->nature", "nature->thunder", "thunder->fire"),
                        obj -> obj instanceof String s && s.matches("^[a-z]+->[a-z]+$"));

        BUILDER.pop();

        // ======================== Resistance to Healing / 高抗性转治疗 ========================
        BUILDER.comment("Resistance to Healing System (Reflect/Absorb mechanism)", "高抗性转治疗系统（反伤机制）")
                .push("resistance_to_heal");

        RESISTANCE_TO_HEAL_ENABLED = BUILDER
                .comment("Enable converting excess resistance to healing",
                         "是否开启抗性过高时转为治疗")
                .define("resistance_to_heal_enabled", true);

        RESISTANCE_TO_HEAL_THRESHOLD = BUILDER
                .comment("Resistance threshold to trigger healing",
                         "抗性阈值，超过此值转为治疗")
                .defineInRange("resistance_to_heal_threshold", 100, 1, 10000);

        RESISTANCE_TO_HEAL_MULTIPLIER = BUILDER
                .comment("Healing conversion multiplier",
                         "转治疗倍率")
                .defineInRange("resistance_to_heal_multiplier", 1.0, 0.0, 10.0);

        BUILDER.pop();

        // ======================== Forced Attribute System / 强制属性系统 ========================
        BUILDER.comment("Forced Attribute System (Supports hot reload)", "强制属性系统（支持热重载）")
                .push("forced_attributes");

        FORCED_ENTITIES = BUILDER
                .comment("",
                         "Custom Forced Entity Attributes List (Supports hot reload)",
                         "【自定义强制属性生物列表】（支持热重载）",
                         "",
                         "One entity per line, enclosed in double quotes \"\"",
                         "每行写一个生物，整行必须用 英文双引号 \"\" 包起来",
                         "Format (Strictly 6 fields):",
                         "格式（严格6个字段）：",
                         "    \"EntityID,AttackElement,StrengthElement,StrengthPoints,ResistElement,ResistPoints\"",
                         "    \"实体ID,攻击属性,强化属性,强化点数,抗性属性,抗性点数\"",
                         "",
                         "Element IDs: fire / frost / thunder / nature / none / empty = none",
                         "属性ID：fire / frost / thunder / nature / none / 留空=无",
                         "Points:",
                         "点数：",
                         "    • Fixed value: 100",
                         "    • 固定值：100",
                         "    • Random range: 0-100 / 50-250 (Randomized within range, step 10, follows global chance settings)",
                         "    • 随机范围：0-100 / 50-250（会在范围内随机，步进10，完全遵循 chance_0_20~chance_80_100 概率）",
                         "    • Empty or 0 = No attribute",
                         "    • 留空或写0 = 不给该属性",
                         "",
                         "Examples / 示例：")
                .defineListAllowEmpty("forced_entities",
                        () -> List.of(
                            "#\"minecraft:zombie,fire,fire,100,fire,100\",              # Fire Zombie, 100 Str, 100 Res / 赤焰僵尸，赤炎强化100，赤炎抗性100",
                            "#\"minecraft:skeleton,,nature,50-100,nature,100\",         # Skeleton: Nature Str 50-100, Nature Res 100 / 骷髅只有自然强化50-200随机，自然抗性固定100",
                            "#\"minecraft:piglin,thunder,thunder,200,frost,150-400\",   # Piglin: Thunder Atk/Str 200, Frost Res 150-400 / 猪灵雷属性攻击和雷属性强化+冰霜抗性150-400随机",
                            "#\"minecraft:creeper,none,fire,0-150,fire,50-200\",        # Creeper: No Atk, Fire Str 0-150, Fire Res 50-200 / 爬行者无攻击，赤焰强化0-150随机+赤焰抗性50-200随机",
                            "#\"minecraft:blaze,,,fire,300\",                           # Blaze: Fire Res 300 only / 烈焰人无攻击无强化，只有赤焰抗性300",
                            "#\"minecraft:wither,thunder,,100-500,,200-800\"            # Wither: Thunder Atk, Random Str/Res / 凋灵雷攻击，强化和抗性都随机"
                        ),
                        o -> o instanceof String);

        BUILDER.pop();

        // ======================== Nether Dimension Defaults / 下界维度默认配置 ========================
        BUILDER.comment("Force Fire attributes for all mobs in Nether", "下界维度全生物强制赤焰属性")
                .push("nether_forced_fire");

        NETHER_DIMENSION_FORCED_FIRE = BUILDER
                .comment("Enable: All mobs spawned in Nether (including modded) force gain Fire Attack + Max Fire Strength + Max Fire Resist",
                         "是否开启：下界维度中所有生成的生物（包括模组怪）强制获得赤焰攻击 + 满级赤焰强化 + 满级赤焰抗性",
                         "Disable to use normal random generation or forced_entities config",
                         "关闭后恢复正常概率生成或按 forced_entities 配置")
                .define("nether_dimension_forced_fire", true);

        NETHER_FIRE_POINTS = BUILDER
                .comment("Forced Fire attribute points in Nether (Used for both Strength and Resistance)",
                         "下界强制赤焰属性点数（强化和抗性都用这个值）",
                         "Per level requires STRENGTH_PER_LEVEL * Level points",
                         "每级强化需要 STRENGTH_PER_LEVEL * 等级 点数")
                .defineInRange("nether_fire_points", 100, 100, 100000);

        BUILDER.pop();

        // ======================== End Dimension Defaults / 末地维度默认配置 ========================
        BUILDER.comment("Force Thunder attributes for all mobs in The End", "末地维度全局强制雷属性（与下界赤焰属性同级功能）")
                .push("end_forced_thunder");

        END_DIMENSION_FORCED_THUNDER = BUILDER
                .comment("Enable: All mobs spawned in The End force gain Thunder Attack + Max Thunder Strength + Max Thunder Resist",
                         "是否开启：末地维度中所有生成的生物强制获得雷攻击 + 满级雷强化 + 满级雷抗性",
                         "Disable to use normal random generation or forced_entities config",
                         "关闭后恢复正常随机或按 forced_entities 配置")
                .define("end_dimension_forced_thunder", true);

        END_THUNDER_POINTS = BUILDER
                .comment("Forced Thunder attribute points in The End (Used for Attack, Strength, and Resistance)",
                         "末地强制雷属性点数（攻击、强化、抗性三者都用这个值）",
                         "Recommended to set >= max_stat_cap",
                         "建议设为 max_stat_cap 的值或更高")
                .defineInRange("end_thunder_points", 100, 100, 100000);

        BUILDER.pop();

        // ======================== Blacklist System / 黑名单系统 ========================
        BUILDER.comment("Blacklist System (Supports hot reload)", "黑名单系统（支持热重载）")
                .push("blacklist");

        BLACKLISTED_ENTITIES = BUILDER
                .comment("",
                         "Entity Attribute Blacklist",
                         "【属性生物黑名单】",
                         "",
                         "Listed entities will NEVER become elemental mobs.",
                         "填入的生物永远不会成为属性生物。",
                         "Prevents attributes even in Nether/End or via random generation.",
                         "即使在下界/末地或随机生成也不会获得属性。",
                         "",
                         "Format: Entity ID",
                         "格式：直接写实体ID即可",
                         "Examples / 示例：",
                         "    \"minecraft:creeper\"      # Creeper never gains attributes / 苦力怕永不获得属性",
                         "    \"minecraft:ghast\"        # Ghast never gains attributes / 恶魂永不获得属性",
                         "    \"minecraft:ender_dragon\" # Ender Dragon never gains attributes / 末影龙永不获得属性")
                .defineListAllowEmpty("blacklisted_entities", List.of(), obj -> obj instanceof String);

        BUILDER.pop();

        // ======================== Biome & Weather Bias / 生物群系与天气属性偏好 ========================
        BUILDER.comment("Biome & Weather Attribute Bias System (Supports hot reload)", "生物群系 & 天气 属性偏好系统（支持热重载）")
                .comment("Values represent percentage probability (%) for that element in that context.",
                         "每个值直接代表对应环境下该属性的生成概率（%）")
                .comment("Probabilities are automatically normalized.",
                         "填多少就是多少概率，会自动归一化处理")
                .comment("Thunderstorm weather has highest priority.",
                         "雷雨天气优先级最高，会强制使用雷霆配置的概率")
                .push("biome_weather_bias");

        HOT_FIRE_BIAS = BUILDER
                .comment("Probability (%) for Fire element in Hot biomes (Desert, Jungle, Badlands)",
                         "炎热生物群系（如沙漠、丛林、恶地）生成赤焰属性的概率（%）")
                .defineInRange("hot_fire_bias", 60.0, 0.0, 100.0);

        COLD_FROST_BIAS = BUILDER
                .comment("Probability (%) for Frost element in Cold biomes (Snowy, Ice Spikes, Frozen Ocean)",
                         "寒冷生物群系（如雪原、冰刺、冻洋）生成冰霜属性的概率（%）")
                .defineInRange("cold_frost_bias", 60.0, 0.0, 100.0);

        FOREST_NATURE_BIAS = BUILDER
                .comment("Probability (%) for Nature element in Forest biomes (Forests, Jungles, Flower Forests)",
                         "森林类生物群系（如所有森林、丛林、花林）生成自然属性的概率（%）")
                .defineInRange("forest_nature_bias", 60.0, 0.0, 100.0);

        THUNDERSTORM_THUNDER_BIAS = BUILDER
                .comment("Probability (%) for Thunder element during Thunderstorms (Global)",
                         "雷雨天气时生成雷霆属性的概率（%），全局生效")
                .defineInRange("thunderstorm_thunder_bias", 80.0, 0.0, 100.0);

        BUILDER.pop();

        // ======================== Custom Biome Bias / 自定义生物群系属性概率 ========================
        BUILDER.comment("Custom Biome Attribute Probabilities (Supports hot reload)", "自定义生物群系属性概率（支持热重载）")
                .comment("Format: \"BiomeID,ElementID,Probability\"",
                         "格式：\"群系ID,属性ID,概率值\"")
                .comment("BiomeID: e.g., minecraft:desert, minecraft:snowy_plains",
                         "群系ID：原版如 minecraft:desert, minecraft:snowy_plains")
                .comment("ElementID: fire / frost / thunder / nature",
                         "属性ID：fire / frost / thunder / nature")
                .comment("Probability: 0-100 (%), multiple entries for same biome accumulate weight",
                         "概率值：0~100（%），同一群系多个条目会叠加权重")
                .comment("Examples / 例子：")
                .comment("  \"minecraft:desert,fire,70\"       # Desert: 70% Fire / 沙漠70%概率生成赤焰属性")
                .comment("  \"minecraft:snowy_plains,frost,80\" # Snowy: 80% Frost / 雪原80%概率生成冰霜属性")
                .comment("  \"minecraft:jungle,nature,60\"     # Jungle: 60% Nature / 丛林60%概率生成自然属性")
                .comment("  \"minecraft:desert,frost,30\"      # Desert: 30% Frost (Extreme gameplay) / 沙漠也可以有30%冰霜（极端玩法）")
                .push("custom_biome_attribute_bias");

        CUSTOM_BIOME_ATTRIBUTE_BIAS = BUILDER
                .defineListAllowEmpty("custom_biome_attribute_bias", List.of(), obj -> obj instanceof String s && s.matches("^[a-z_]+:[a-z_]+,[a-z]+,[0-9]{1,3}(\\.[0-9])?$"));

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // ==================== Utility Methods / 工具方法 ====================
    public static double getDamageMultiplier() { return Math.floor(ELEMENTAL_DAMAGE_MULTIPLIER.get() * 2.0) / 2.0; }
    public static double getResistanceMultiplier() { return Math.floor(ELEMENTAL_RESISTANCE_MULTIPLIER.get() * 2.0) / 2.0; }
    public static int getStrengthPerHalfDamage() { return Math.max(10, (STRENGTH_PER_HALF_DAMAGE.get() / 10) * 10); }
    public static int getResistPerHalfReduction() { return Math.max(10, (RESIST_PER_HALF_REDUCTION.get() / 10) * 10); }
    public static int getMaxStatCap() { return (MAX_STAT_CAP.get() / 10) * 10; }
    public static int getStrengthPerLevel() { return STRENGTH_PER_LEVEL.get(); }
    public static int getResistPerLevel()   { return RESIST_PER_LEVEL.get(); }

    /**
     * Calculates a dynamic value based on configured probability segments.
     * <p>
     * 根据配置的概率分段计算动态数值。
     */
    private static int rollDynamicValue(double c1, double c2, double c3, double c4, double c5, int maxValue) {
        int cap = (maxValue / 10) * 10;
        if (cap < 10) cap = 10;

        double roll = RANDOM.nextDouble();
        double s1 = c1, s2 = s1 + c2, s3 = s2 + c3, s4 = s3 + c4;

        int min, max;
        if (roll < s1) { min = 10; max = (int)(cap * 0.20); }
        else if (roll < s2) { min = (int)(cap * 0.20) + 1; max = (int)(cap * 0.40); }
        else if (roll < s3) { min = (int)(cap * 0.40) + 1; max = (int)(cap * 0.60); }
        else if (roll < s4) { min = (int)(cap * 0.60) + 1; max = (int)(cap * 0.80); }
        else { min = (int)(cap * 0.80) + 1; max = cap; }

        min = ((min + 9) / 10) * 10;
        max = (max / 10) * 10;
        if (max < min) max = min;

        int steps = (max - min) / 10 + 1;
        return min + (RANDOM.nextInt(steps) * 10);
    }

    public static int rollMonsterStrength() {
        return rollDynamicValue(CHANCE_0_20.get(), CHANCE_20_40.get(), CHANCE_40_60.get(), CHANCE_60_80.get(), CHANCE_80_100.get(), getMaxStatCap());
    }

    public static int rollMonsterResist() {
        return rollDynamicValue(CHANCE_0_20.get(), CHANCE_20_40.get(), CHANCE_40_60.get(), CHANCE_60_80.get(), CHANCE_80_100.get(), getMaxStatCap());
    }
}