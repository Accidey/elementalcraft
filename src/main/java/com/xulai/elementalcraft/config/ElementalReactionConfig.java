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
 * 用于管理所有元素反应（如潮湿、以及未来可能添加的反应）的相关参数。
 *
 * English description:
 * Configuration class dedicated to Elemental Reactions.
 * Manages parameters for all elemental reactions (e.g., Wetness).
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

    // Immunity Configs / 免疫配置
    public static final ForgeConfigSpec.BooleanValue WETNESS_WATER_ANIMAL_IMMUNE;
    public static final ForgeConfigSpec.BooleanValue WETNESS_NETHER_DIMENSION_IMMUNE;
    
    // Blacklist Config / 黑名单配置
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WETNESS_ENTITY_BLACKLIST;

    // ======================== Steam Reaction / 蒸汽反应 ========================
    public static final ForgeConfigSpec.BooleanValue STEAM_REACTION_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_REDUCTION; 
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_RADIUS;
    public static final ForgeConfigSpec.IntValue STEAM_CLOUD_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_BLINDNESS_DURATION;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;

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
                         "0.1 means reducing Lightning resistance and Frost resistance by 10% per level.",
                         "每层潮湿提供的通用抗性修正。",
                         "0.1 代表每层降低雷霆抗性和冰霜抗性10%（即受到的伤害增加10%）。")
                .defineInRange("wetness_resist_modifier", 0.1, 0.0, 1.0);

        WETNESS_FIRE_REDUCTION = BUILDER
                .comment("Fire element damage reduction percentage per Wetness level.",
                         "Defines the proportion of Fire damage negated by each level of Wetness.",
                         "Value 0.1 represents 10% reduction per level (e.g., Level 5 = 50% damage reduction).",
                         "每层潮湿提供的赤焰属性伤害减免比例。",
                         "定义了每一级潮湿效果能抵消多少比例的赤焰属性伤害。",
                         "数值 0.1 代表每级减免 10%（例如：5级潮湿 = 减少 50% 伤害）。")
                .defineInRange("wetness_fire_reduction", 0.1, 0.0, 1.0);

        WETNESS_RAIN_GAIN_INTERVAL = BUILDER
                .comment("Interval in seconds for players or mobs to gain 1 Wetness level while standing in rain.",
                         "玩家或生物在雨中时，每隔多少秒获得 1 层潮湿效果。")
                .defineInRange("wetness_rain_gain_interval", 10, 1, 3600);

        WETNESS_DECAY_BASE_TIME = BUILDER
                .comment("Base natural decay time (in seconds) per Wetness level.",
                         "Total Duration = Current Level * Base Time.",
                         "Example (Base=10s): Level 5 lasts 50s; drops to Level 4 after 10s.",
                         "每层潮湿的基础自然消退时间（秒）。",
                         "总持续时间 = 当前层数 * 基础时间。",
                         "示例（基础10秒）：5级持续50秒；10秒后降为4级。")
                .defineInRange("wetness_decay_base_time", 10, 1, 3600);

        WETNESS_EXHAUSTION_INCREASE = BUILDER
                .comment("Percentage of extra exhaustion added per Wetness level when consuming hunger.",
                         "Simulates increased physical exertion due to heavy, wet clothes.",
                         "当消耗饱食度时，每层潮湿额外增加的耗竭度百分比。",
                         "用于模拟衣服湿透变重后的体力消耗增加。")
                .defineInRange("wetness_exhaustion_increase", 0.05, 0.0, 10.0);

        WETNESS_POTION_ADD_LEVEL = BUILDER
                .comment("The number of Wetness levels added instantly when hit by a Splash Water Bottle.",
                         "被喷溅水瓶击中时，一次性增加的潮湿层数。")
                .defineInRange("wetness_potion_add_level", 1, 1, 100);

        // ======================== Immunity Settings / 免疫设置 ========================
        BUILDER.comment("Immunity Settings for Wetness", "潮湿效果的免疫设置")
               .push("immunity");

        WETNESS_WATER_ANIMAL_IMMUNE = BUILDER
                .comment("Whether water animals (e.g., Fish, Squid, Dolphins) are completely immune to the Wetness effect.",
                         "水生生物（如鱼、鱿鱼、海豚）是否完全免疫潮湿效果。")
                .define("water_animal_immune", true);

        WETNESS_NETHER_DIMENSION_IMMUNE = BUILDER
                .comment("Whether all entities in the Nether dimension are immune to the Wetness effect.",
                         "If enabled, Wetness evaporates instantly in the Nether.",
                         "下界维度中的所有生物是否免疫潮湿效果。",
                         "如果开启，潮湿效果在下界维度会瞬间蒸发。")
                .define("nether_dimension_immune", true);

        WETNESS_ENTITY_BLACKLIST = BUILDER
                .comment("",
                         "[Wetness Effect Entity Blacklist]",
                         "Entities in this list will NEVER gain the Wetness effect.",
                         "【潮湿效果生物黑名单】",
                         "在此列表中的生物永远不会获得潮湿效果。",
                         "Format: Entity ID (e.g., \"minecraft:blaze\")",
                         "格式：实体ID（例如：\"minecraft:blaze\"）")
                .defineListAllowEmpty("wetness_entity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop(); // Pop immunity
        BUILDER.pop(); // Pop wetness_system

        // ======================== Steam Reaction / 蒸汽反应 ========================
        BUILDER.comment("Steam Reaction Configuration (Fire Attack + Wet Target)", 
                        "蒸汽反应配置 (当赤焰属性攻击潮湿目标时触发)")
               .push("steam_reaction");

        STEAM_REACTION_ENABLED = BUILDER
                .comment("Whether to enable the Steam Reaction mechanism.", 
                         "是否开启蒸汽反应机制？")
                .define("steam_reaction_enabled", true);

        STEAM_MAX_REDUCTION = BUILDER
                .comment("Maximum Cap for Wetness-based Damage Modification.",
                         "This value serves as a hard limit for:",
                         "1. Fire Damage Reduction (Steam Reaction).",
                         "2. Thunder/Frost Damage Increase (Resistance Drop).",
                         "Example: 0.9 = Effects are capped at 90%.",
                         "潮湿效果对伤害修正的最高上限（通用封顶值）。",
                         "此数值同时作为以下两种情况的硬上限：",
                         "1. 赤焰属性的伤害减免（蒸汽反应）。",
                         "2. 雷霆/冰霜属性的伤害增加（抗性降低）。",
                         "示例：0.9 = 所有潮湿带来的伤害修正幅度最高不超过 90%。")
                .defineInRange("steam_max_reduction", 0.9, 0.0, 1.0);

        STEAM_CLOUD_RADIUS = BUILDER
                .comment("Radius of the steam cloud generated when the reaction is triggered.", 
                         "触发反应时，生成的蒸汽云雾半径大小。")
                .defineInRange("steam_cloud_radius", 2.0, 0.5, 10.0);

        STEAM_CLOUD_DURATION = BUILDER
                .comment("Duration of the steam cloud (Unit: Tick, 20 Ticks = 1 second).", 
                         "蒸汽云雾的持续时间（单位：Tick，20 Tick = 1秒）。")
                .defineInRange("steam_cloud_duration", 100, 20, 1200);

        STEAM_BLINDNESS_DURATION = BUILDER
                .comment("Duration of the Blindness effect applied to entities entering the steam cloud (Unit: Tick).", 
                         "进入蒸汽云雾的生物获得的失明效果持续时间（单位：Tick）。")
                .defineInRange("steam_blindness_duration", 60, 20, 600);

        STEAM_CLEAR_AGGRO = BUILDER
                .comment("Whether to clear the attack target of mobs inside the steam cloud.",
                         "Simulates mobs losing sight and being unable to lock onto players in the steam.",
                         "是否清除位于蒸汽云内怪物的仇恨目标。",
                         "用于模拟怪物在蒸汽中丢失视野、无法锁定玩家的效果。")
                .define("steam_clear_aggro", true);

        BUILDER.pop(); // Pop steam_reaction

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
    
    public static boolean wetnessWaterAnimalImmune = true;
    public static boolean wetnessNetherDimensionImmune = true;
    public static List<? extends String> cachedWetnessBlacklist = List.of();

    // Steam Reaction Cache
    public static boolean steamReactionEnabled = true;
    public static double steamMaxReduction = 0.9;
    public static double steamCloudRadius = 2.0;
    public static int steamCloudDuration = 100;
    public static int steamBlindnessDuration = 60;
    public static boolean steamClearAggro = true;

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
        
        wetnessWaterAnimalImmune = WETNESS_WATER_ANIMAL_IMMUNE.get();
        wetnessNetherDimensionImmune = WETNESS_NETHER_DIMENSION_IMMUNE.get();
        cachedWetnessBlacklist = WETNESS_ENTITY_BLACKLIST.get();
        
        // Steam
        steamReactionEnabled = STEAM_REACTION_ENABLED.get();
        steamMaxReduction = STEAM_MAX_REDUCTION.get();
        steamCloudRadius = STEAM_CLOUD_RADIUS.get();
        steamCloudDuration = STEAM_CLOUD_DURATION.get();
        steamBlindnessDuration = STEAM_BLINDNESS_DURATION.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        
        ElementalCraft.LOGGER.debug("[ElementalCraft] Reaction Config cache refreshed.");
    }
}