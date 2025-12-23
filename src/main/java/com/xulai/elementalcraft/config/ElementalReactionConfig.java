// src/main/java/com/xulai/elementalcraft/config/ElementalReactionConfig.java
package com.xulai.elementalcraft.config;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

/**
 * ElementalReactionConfig
 *
 * 中文说明：
 * 元素反应专用配置文件类。
 * 用于管理所有元素反应（如潮湿、蒸汽、自然吸取等）的相关参数。
 *
 * English description:
 * Configuration class dedicated to Elemental Reactions.
 * Manages parameters for all elemental reactions (e.g., Wetness, Steam, Nature Siphon).
 */
public class ElementalReactionConfig {
    public static final ForgeConfigSpec SPEC;

    // ======================== Wetness System / 潮湿系统 ========================
    public static final ForgeConfigSpec.IntValue WETNESS_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue WETNESS_RESIST_MODIFIER;
    public static final ForgeConfigSpec.DoubleValue WETNESS_FIRE_REDUCTION;

    // Time Configs / 时间配置
    public static final ForgeConfigSpec.IntValue WETNESS_RAIN_GAIN_INTERVAL;
    public static final ForgeConfigSpec.IntValue WETNESS_DECAY_BASE_TIME;

    // Exhaustion & Potion / 饱食度与药水
    public static final ForgeConfigSpec.DoubleValue WETNESS_EXHAUSTION_INCREASE;
    public static final ForgeConfigSpec.IntValue WETNESS_POTION_ADD_LEVEL;
    
    // Self-Drying Config / 自我干燥配置
    public static final ForgeConfigSpec.IntValue WETNESS_DRYING_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue WETNESS_SELF_DRYING_DAMAGE_PENALTY; // 新增配置

    // Immunity Configs / 免疫配置
    public static final ForgeConfigSpec.BooleanValue WETNESS_WATER_ANIMAL_IMMUNE;
    public static final ForgeConfigSpec.BooleanValue WETNESS_NETHER_DIMENSION_IMMUNE;
    
    // Wetness Blacklist / 潮湿黑名单
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WETNESS_ENTITY_BLACKLIST;

    // ======================== Nature Reaction / 自然反应 ========================
    public static final ForgeConfigSpec.DoubleValue NATURE_WETNESS_BONUS;
    public static final ForgeConfigSpec.IntValue NATURE_SIPHON_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NATURE_SIPHON_HEAL;

    // ======================== Steam Reaction / 蒸汽反应 ========================
    public static final ForgeConfigSpec.BooleanValue STEAM_REACTION_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_REDUCTION; 
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_RADIUS;
    public static final ForgeConfigSpec.IntValue STEAM_CLOUD_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_BLINDNESS_DURATION;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;
    
    // Scalding Config / 烫伤配置
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_WEAKNESS;
    public static final ForgeConfigSpec.IntValue STEAM_IMMUNITY_THRESHOLD;
    
    // Steam Immunity Blacklist / 蒸汽免疫黑名单
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STEAM_IMMUNITY_BLACKLIST;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Elemental Reaction System Configuration", "元素反应系统配置")
                .push("wetness_system");

        WETNESS_MAX_LEVEL = BUILDER
                .comment("Maximum stack level of the Wetness effect.", 
                         "潮湿效果的最大堆叠层数。")
                .defineInRange("wetness_max_level", 5, 1, 100);

        WETNESS_RESIST_MODIFIER = BUILDER
                .comment("General resistance modifier provided per Wetness level.",
                         "Controls damage increase for Thunder/Frost/Nature against wet targets.",
                         "Value 0.1 means: Damage taken +10% per level.",
                         "每层潮湿提供的通用易伤修正。",
                         "控制雷霆、冰霜、自然属性对潮湿目标的伤害增幅。",
                         "数值 0.1 代表：每层受到的伤害增加 10%。")
                .defineInRange("wetness_resist_modifier", 0.1, 0.0, 1.0);

        WETNESS_FIRE_REDUCTION = BUILDER
                .comment("Fire damage reduction percentage per Wetness level.",
                         "Defines how much Fire damage is blocked by water.",
                         "Value 0.1 means: Fire Damage -10% per level.",
                         "每层潮湿提供的赤焰属性伤害减免比例。",
                         "定义了潮湿效果能抵消多少火属性伤害。",
                         "数值 0.1 代表：每层减少 10% 的火属性伤害。")
                .defineInRange("wetness_fire_reduction", 0.1, 0.0, 1.0);

        WETNESS_RAIN_GAIN_INTERVAL = BUILDER
                .comment("Interval (in seconds) to gain 1 Wetness level while standing in rain.",
                         "玩家或生物在雨中时，获得 1 层潮湿效果的时间间隔（秒）。")
                .defineInRange("wetness_rain_gain_interval", 10, 1, 3600);

        WETNESS_DECAY_BASE_TIME = BUILDER
                .comment("Base natural decay time (in seconds) per Wetness level.",
                         "Total Duration = Current Level * Base Time.",
                         "每层潮湿的基础自然消退时间（秒）。",
                         "总持续时间 = 当前层数 * 基础时间。")
                .defineInRange("wetness_decay_base_time", 10, 1, 3600);

        WETNESS_EXHAUSTION_INCREASE = BUILDER
                .comment("Extra exhaustion added per Wetness level when consuming hunger.",
                         "Simulates heavier movement due to wet clothes.",
                         "每层潮湿额外增加的耗竭度。",
                         "用于模拟衣服湿透变重后的体力消耗增加。")
                .defineInRange("wetness_exhaustion_increase", 0.05, 0.0, 10.0);

        WETNESS_POTION_ADD_LEVEL = BUILDER
                .comment("Number of Wetness levels added instantly by a Splash Water Bottle.",
                         "被喷溅水瓶击中时，一次性增加的潮湿层数。")
                .defineInRange("wetness_potion_add_level", 1, 1, 100);
                
        WETNESS_DRYING_THRESHOLD = BUILDER
                .comment("Fire enhancement points required to remove ONE extra wetness level (Self-Drying).",
                         "Formula: Removed Levels = 1 + (FireEnhancement / Threshold).",
                         "Lower value means faster drying.",
                         "攻击时移除 1 层额外自身潮湿效果所需的赤焰强化点数（自我干燥）。",
                         "公式：移除层数 = 1 + (赤焰强化 / 阈值)。",
                         "数值越低，烘干速度越快。")
                .defineInRange("wetness_drying_threshold", 20, 1, 1000);

        WETNESS_SELF_DRYING_DAMAGE_PENALTY = BUILDER
                .comment("Damage penalty ratio when a wet Fire attacker dries themselves (Self-Drying).",
                         "Mechanism: Attack heat is consumed to evaporate own wetness instead of damaging the enemy.",
                         "Value: 0.3 = -30% Damage. Set to 0.0 to disable penalty.",
                         "当赤焰属性攻击者处于潮湿状态时，触发自我干燥导致的伤害惩罚比例。",
                         "机制说明：攻击产生的热量优先用于蒸发自身的水分，只有剩余热量能造成伤害。",
                         "数值说明：0.3 代表伤害降低 30%（即只造成 70% 伤害）。设为 0.0 则无惩罚。")
                .defineInRange("wetness_self_drying_damage_penalty", 0.3, 0.0, 1.0);

        // ======================== Immunity Settings ========================
        BUILDER.comment("Immunity Settings for Wetness", "潮湿效果的免疫设置")
               .push("immunity");

        WETNESS_WATER_ANIMAL_IMMUNE = BUILDER
                .comment("Whether water animals (Fish, Squid, etc.) are immune to Wetness.",
                         "水生生物（如鱼、鱿鱼）是否完全免疫潮湿效果。")
                .define("water_animal_immune", true);

        WETNESS_NETHER_DIMENSION_IMMUNE = BUILDER
                .comment("Whether entities in the Nether dimension are immune to Wetness.",
                         "下界维度中的所有生物是否免疫潮湿效果（瞬间蒸发）。")
                .define("nether_dimension_immune", true);

        WETNESS_ENTITY_BLACKLIST = BUILDER
                .comment("Entities in this list will NEVER gain the Wetness effect.",
                         "Format: Entity ID (e.g., \"minecraft:blaze\")",
                         "【潮湿效果黑名单】",
                         "在此列表中的生物永远不会获得潮湿效果。",
                         "格式：实体ID（例如：\"minecraft:blaze\"）")
                .defineListAllowEmpty("wetness_entity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop(); 
        BUILDER.pop(); // Pop wetness_system

        // ======================== Nature Reaction ========================
        BUILDER.comment("Nature Reaction Configuration (Siphon & Growth)", "自然反应配置 (汲取与生长)")
               .push("nature_reaction");

        NATURE_WETNESS_BONUS = BUILDER
                .comment("Damage bonus per Wetness level when attacking with Nature element.",
                         "Value 0.1 means: +10% Damage per level.",
                         "自然属性攻击潮湿目标时，每层潮湿提供的额外伤害倍率。",
                         "数值 0.1 代表：每层增加 10% 伤害。")
                .defineInRange("nature_wetness_bonus", 0.1, 0.0, 10.0);

        NATURE_SIPHON_THRESHOLD = BUILDER
                .comment("Nature enhancement points required to siphon ONE extra wetness level.",
                         "Formula: Siphoned Levels = 1 + (NatureEnhancement / Threshold).",
                         "攻击时吸取 1 层额外潮湿效果所需的自然强化点数。",
                         "公式：吸取层数 = 1 + (自然强化 / 阈值)。")
                .defineInRange("nature_siphon_threshold", 20, 1, 1000);

        NATURE_SIPHON_HEAL = BUILDER
                .comment("Health points restored when successfully siphoning wetness.",
                         "成功吸取潮湿效果时恢复的生命值点数（0 表示不回血）。")
                .defineInRange("nature_siphon_heal", 1.0, 0.0, 100.0);

        BUILDER.pop(); // Pop nature_reaction

        // ======================== Steam Reaction ========================
        BUILDER.comment("Steam Reaction Configuration", "蒸汽反应配置")
               .push("steam_reaction");

        STEAM_REACTION_ENABLED = BUILDER
                .comment("Whether to enable the Steam Reaction mechanism.",
                         "是否开启蒸汽反应机制？")
                .define("steam_reaction_enabled", true);
        
        STEAM_MAX_REDUCTION = BUILDER
                .comment("Maximum Cap for all Wetness-based Damage Modifications.",
                         "Limits Fire reduction, Thunder/Frost bonus, and Nature bonus.",
                         "Example: 0.9 = Effects are capped at 90%.",
                         "所有基于潮湿的伤害修正（增伤或减伤）的最高上限。",
                         "同时限制赤焰减伤、雷/冰/自然增伤。",
                         "示例：0.9 = 修正幅度最高不超过 90%。")
                .defineInRange("steam_max_reduction", 0.9, 0.0, 1.0);

        STEAM_CLOUD_RADIUS = BUILDER
                .comment("Radius of the steam cloud generated upon reaction.",
                         "触发反应时生成的蒸汽云雾半径。")
                .defineInRange("steam_cloud_radius", 2.0, 0.5, 10.0);

        STEAM_CLOUD_DURATION = BUILDER
                .comment("Duration of the steam cloud (in Ticks).",
                         "蒸汽云雾的持续时间（单位：Tick，20 Tick = 1秒）。")
                .defineInRange("steam_cloud_duration", 100, 20, 1200);

        STEAM_BLINDNESS_DURATION = BUILDER
                .comment("Duration of Blindness effect applied inside the cloud (in Ticks).",
                         "进入蒸汽云雾时获得的失明效果持续时间（单位：Tick）。")
                .defineInRange("steam_blindness_duration", 60, 20, 600);

        STEAM_CLEAR_AGGRO = BUILDER
                .comment("Whether steam clouds clear mob aggro (mobs lose their target).",
                         "是否清除位于蒸汽云内怪物的仇恨目标（模拟丢失视野）。")
                .define("steam_clear_aggro", true);
        
        STEAM_SCALDING_DAMAGE = BUILDER
                .comment("Damage dealt by High Heat steam per second.", 
                         "Type: Environmental Fire Damage (bypasses armor).",
                         "高温蒸汽每秒造成的烫伤伤害。",
                         "类型：环境火焰伤害（无视护甲）。")
                .defineInRange("steam_scalding_damage", 1.0, 0.0, 100.0);
                
        STEAM_SCALDING_MULTIPLIER_WEAKNESS = BUILDER
                .comment("Damage multiplier for Frost/Nature entities in hot steam.", 
                         "冰霜/自然属性生物在高温蒸汽中受到的易伤倍率。")
                .defineInRange("steam_scalding_multiplier_weakness", 1.5, 1.0, 10.0);
                
        STEAM_IMMUNITY_THRESHOLD = BUILDER
                .comment("Fire Resistance points required to be immune to steam scalding.", 
                         "Default: 80 (High resistance required).",
                         "免疫蒸汽烫伤所需的赤焰抗性点数阈值。",
                         "默认：80（需要极高的抗性才能免疫）。")
                .defineInRange("steam_immunity_threshold", 80, 0, 1000);
                
        STEAM_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this list are immune to steam scalding.", 
                         "在此列表中的生物免疫蒸汽烫伤（ID 格式）。")
                .defineListAllowEmpty("steam_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop(); 

        SPEC = BUILDER.build();
    }

    // Static Cache / 静态缓存
    public static int wetnessMaxLevel = 5;
    public static double wetnessResistModifier = 0.1;
    public static double wetnessFireReduction = 0.1;
    public static int wetnessRainGainInterval = 10;
    public static int wetnessDecayBaseTime = 10;
    public static double wetnessExhaustionIncrease = 0.05;
    public static int wetnessPotionAddLevel = 1;
    public static int wetnessDryingThreshold = 20; 
    public static double wetnessSelfDryingDamagePenalty = 0.3; 
    
    public static boolean wetnessWaterAnimalImmune = true;
    public static boolean wetnessNetherDimensionImmune = true;
    public static List<? extends String> cachedWetnessBlacklist = List.of();

    // Nature Reaction Cache
    public static double natureWetnessBonus = 0.1;
    public static int natureSiphonThreshold = 20;
    public static double natureSiphonHeal = 1.0;

    // Steam Reaction Cache
    public static boolean steamReactionEnabled = true;
    public static double steamMaxReduction = 0.9;
    public static double steamCloudRadius = 2.0;
    public static int steamCloudDuration = 100;
    public static int steamBlindnessDuration = 60;
    public static boolean steamClearAggro = true;
    public static double steamScaldingDamage = 1.0;
    public static double steamScaldingMultiplierWeakness = 1.5;
    public static int steamImmunityThreshold = 20;
    public static List<? extends String> cachedSteamBlacklist = List.of();

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "elementalcraft-reactions.toml");
    }

    public static void refreshCache() {
        // Wetness
        wetnessMaxLevel = WETNESS_MAX_LEVEL.get();
        wetnessResistModifier = WETNESS_RESIST_MODIFIER.get();
        wetnessFireReduction = WETNESS_FIRE_REDUCTION.get();
        wetnessRainGainInterval = WETNESS_RAIN_GAIN_INTERVAL.get();
        wetnessDecayBaseTime = WETNESS_DECAY_BASE_TIME.get();
        wetnessExhaustionIncrease = WETNESS_EXHAUSTION_INCREASE.get();
        wetnessPotionAddLevel = WETNESS_POTION_ADD_LEVEL.get();
        wetnessDryingThreshold = WETNESS_DRYING_THRESHOLD.get();
        wetnessSelfDryingDamagePenalty = WETNESS_SELF_DRYING_DAMAGE_PENALTY.get(); // 刷新缓存
        
        wetnessWaterAnimalImmune = WETNESS_WATER_ANIMAL_IMMUNE.get();
        wetnessNetherDimensionImmune = WETNESS_NETHER_DIMENSION_IMMUNE.get();
        cachedWetnessBlacklist = WETNESS_ENTITY_BLACKLIST.get();

        // Nature
        natureWetnessBonus = NATURE_WETNESS_BONUS.get();
        natureSiphonThreshold = NATURE_SIPHON_THRESHOLD.get();
        natureSiphonHeal = NATURE_SIPHON_HEAL.get();
        
        // Steam
        steamReactionEnabled = STEAM_REACTION_ENABLED.get();
        steamMaxReduction = STEAM_MAX_REDUCTION.get();
        steamCloudRadius = STEAM_CLOUD_RADIUS.get();
        steamCloudDuration = STEAM_CLOUD_DURATION.get();
        steamBlindnessDuration = STEAM_BLINDNESS_DURATION.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        steamScaldingDamage = STEAM_SCALDING_DAMAGE.get();
        steamScaldingMultiplierWeakness = STEAM_SCALDING_MULTIPLIER_WEAKNESS.get();
        steamImmunityThreshold = STEAM_IMMUNITY_THRESHOLD.get();
        cachedSteamBlacklist = STEAM_IMMUNITY_BLACKLIST.get();
        
        ElementalCraft.LOGGER.debug("[ElementalCraft] Reaction Config cache refreshed.");
    }
}