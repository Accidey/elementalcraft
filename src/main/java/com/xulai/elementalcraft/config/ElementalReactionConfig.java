// src/main/java/com/xulai/elementalcraft/config/ElementalReactionConfig.java
package com.xulai.elementalcraft.config;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

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
    
    // [Modified] Renamed to reflect the new logic: Level * BaseTime
    // [修改] 重命名以反映新逻辑：层数 * 基础时间
    public static final ForgeConfigSpec.IntValue WETNESS_DECAY_BASE_TIME;

    // Exhaustion & Potion / 饱食度与药水
    public static final ForgeConfigSpec.DoubleValue WETNESS_EXHAUSTION_INCREASE;
    public static final ForgeConfigSpec.IntValue WETNESS_POTION_ADD_LEVEL;

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

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    // Static Cache / 静态缓存
    public static int wetnessMaxLevel = 5;
    public static double wetnessResistModifier = 0.1;
    public static double wetnessFireReduction = 0.1;
    public static int wetnessRainGainInterval = 10;
    public static int wetnessDecayBaseTime = 10; // Updated field
    public static double wetnessExhaustionIncrease = 0.05;
    public static int wetnessPotionAddLevel = 2;

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "elementalcraft-reactions.toml");
    }

    public static void refreshCache() {
        wetnessMaxLevel = WETNESS_MAX_LEVEL.get();
        wetnessResistModifier = WETNESS_RESIST_MODIFIER.get();
        wetnessFireReduction = WETNESS_FIRE_REDUCTION.get();
        wetnessRainGainInterval = WETNESS_RAIN_GAIN_INTERVAL.get();
        wetnessDecayBaseTime = WETNESS_DECAY_BASE_TIME.get();
        wetnessExhaustionIncrease = WETNESS_EXHAUSTION_INCREASE.get();
        wetnessPotionAddLevel = WETNESS_POTION_ADD_LEVEL.get();
        ElementalCraft.LOGGER.debug("[ElementalCraft] Reaction Config cache refreshed.");
    }
}