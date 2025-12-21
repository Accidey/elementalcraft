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
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_RADIUS;
    public static final ForgeConfigSpec.IntValue STEAM_CLOUD_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_BLINDNESS_DURATION;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Elemental Reaction System Configuration", "元素反应系统配置")
                .push("wetness_system");

        WETNESS_MAX_LEVEL = BUILDER
                .comment("Maximum wetness level", "最大潮湿层数")
                .defineInRange("wetness_max_level", 5, 1, 100);

        WETNESS_RESIST_MODIFIER = BUILDER
                .comment("Percentage change in resistance per wetness level (0.1 = 10%)",
                         "每层潮湿改变抗性的百分比（0.1 = 10%）。")
                .defineInRange("wetness_resist_modifier", 0.1, 0.0, 1.0);

        WETNESS_FIRE_REDUCTION = BUILDER
                .comment("Extra fire damage reduction per wetness level (0.1 = 10%)",
                         "每层潮湿额外减免的火属性伤害百分比（0.1 = 10%）")
                .defineInRange("wetness_fire_reduction", 0.1, 0.0, 1.0);

        WETNESS_RAIN_GAIN_INTERVAL = BUILDER
                .comment("Time interval (in seconds) to gain 1 wetness level while in rain",
                         "在雨中每获得 1 层潮湿效果所需的时间间隔（秒）。")
                .defineInRange("wetness_rain_gain_interval", 10, 1, 3600);

        WETNESS_DECAY_BASE_TIME = BUILDER
                .comment("Base decay time (in seconds) per level.",
                         "Formula: Decay Time = Current Level * Base Time.",
                         "Example (Base=10): Level 5 lasts 50s, Level 2 lasts 20s.",
                         "每层潮湿的基础衰减时间（秒）。",
                         "公式：衰减时间 = 当前层数 * 基础时间。",
                         "示例（基础10秒）：5级持续50秒，2级持续20秒。")
                .defineInRange("wetness_decay_base_time", 10, 1, 3600);

        WETNESS_EXHAUSTION_INCREASE = BUILDER
                .comment("Extra exhaustion percentage added per wetness level when consuming hunger (0.05 = 5%)",
                         "模拟衣服变重。当消耗饱食度时，每层潮湿额外增加的耗竭度百分比。")
                .defineInRange("wetness_exhaustion_increase", 0.05, 0.0, 10.0);

        WETNESS_POTION_ADD_LEVEL = BUILDER
                .comment("Levels of wetness added when hit by a Splash Water Bottle",
                         "被喷溅水瓶击中时增加的潮湿层数")
                .defineInRange("wetness_potion_add_level", 2, 1, 100);

        // ======================== Immunity Settings / 免疫设置 ========================
        BUILDER.comment("Immunity Settings for Wetness", "潮湿效果的免疫设置")
               .push("immunity");

        WETNESS_WATER_ANIMAL_IMMUNE = BUILDER
                .comment("Whether water animals (e.g. Fish, Squid, Dolphins) are immune to wetness.",
                         "水生生物（如鱼、鱿鱼、海豚）是否对潮湿效果免疫。")
                .define("water_animal_immune", true);

        WETNESS_NETHER_DIMENSION_IMMUNE = BUILDER
                .comment("Whether all entities in the Nether dimension are immune to wetness.",
                         "Under high temperature, wetness evaporates instantly.",
                         "下界维度中的所有生物是否对潮湿效果免疫。",
                         "在高温下，潮湿效果会瞬间蒸发。")
                .define("nether_dimension_immune", true);

        WETNESS_ENTITY_BLACKLIST = BUILDER
                .comment("",
                         "Blacklist for Wetness Effect",
                         "【潮湿效果生物黑名单】",
                         "",
                         "Entities in this list will NEVER gain wetness effect.",
                         "在此列表中的生物永远不会获得潮湿效果。",
                         "Format: Entity ID",
                         "格式：实体ID",
                         "Example: \"minecraft:blaze\"")
                .defineListAllowEmpty("wetness_entity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop(); // Pop immunity
        BUILDER.pop(); // Pop wetness_system

        // ======================== Steam Reaction / 蒸汽反应 ========================
        BUILDER.comment("Steam Reaction Configuration (Fire Attack + Wet Target)", 
                        "蒸汽反应配置 (赤焰攻击 + 潮湿目标)")
               .push("steam_reaction");

        STEAM_REACTION_ENABLED = BUILDER
                .comment("Enable Steam Reaction?", "是否开启蒸汽反应？")
                .define("steam_reaction_enabled", true);

        STEAM_DAMAGE_REDUCTION = BUILDER
                .comment("Damage reduction percentage when steam reaction triggers (0.5 = 50% reduction)",
                         "蒸汽反应触发时，火属性伤害的衰减比例 (0.5 = 降低 50% 伤害)")
                .defineInRange("steam_damage_reduction", 0.5, 0.0, 1.0);

        STEAM_CLOUD_RADIUS = BUILDER
                .comment("Radius of the generated steam cloud", "生成的蒸汽云半径")
                .defineInRange("steam_cloud_radius", 2.0, 0.5, 10.0);

        STEAM_CLOUD_DURATION = BUILDER
                .comment("Duration of the steam cloud in ticks (20 ticks = 1 second)",
                         "蒸汽云的持续时间 (Tick，20 = 1秒)")
                .defineInRange("steam_cloud_duration", 100, 20, 1200);

        STEAM_BLINDNESS_DURATION = BUILDER
                .comment("Duration of Blindness effect applied by the cloud (in ticks)",
                         "云雾给予的失明效果持续时间 (Tick)")
                .defineInRange("steam_blindness_duration", 60, 20, 600);

        STEAM_CLEAR_AGGRO = BUILDER
                .comment("Whether to clear the attack target of mobs inside the steam cloud (confuse AI)",
                         "是否清除位于蒸汽云内怪物的仇恨目标 (迷惑AI)")
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
    public static int wetnessPotionAddLevel = 2;
    
    public static boolean wetnessWaterAnimalImmune = true;
    public static boolean wetnessNetherDimensionImmune = true;
    public static List<? extends String> cachedWetnessBlacklist = List.of();

    // Steam Reaction Cache
    public static boolean steamReactionEnabled = true;
    public static double steamDamageReduction = 0.5;
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
        steamDamageReduction = STEAM_DAMAGE_REDUCTION.get();
        steamCloudRadius = STEAM_CLOUD_RADIUS.get();
        steamCloudDuration = STEAM_CLOUD_DURATION.get();
        steamBlindnessDuration = STEAM_BLINDNESS_DURATION.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        
        ElementalCraft.LOGGER.debug("[ElementalCraft] Reaction Config cache refreshed.");
    }
}