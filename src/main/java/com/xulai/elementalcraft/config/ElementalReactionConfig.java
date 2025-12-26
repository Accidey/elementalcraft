// src/main/java/com/xulai/elementalcraft/config/ElementalReactionConfig.java
package com.xulai.elementalcraft.config;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

/**
 * ElementalReactionConfig
 * <p>
 * 中文说明：
 * 元素反应系统的专用配置文件类。
 * 负责定义和管理所有与元素反应（如潮湿、蒸汽、自然吸取等）相关的可配置参数。
 * 包含 ForgeConfigSpec 的构建以及运行时缓存的刷新逻辑。
 * <p>
 * English Description:
 * Dedicated configuration class for the Elemental Reaction system.
 * Responsible for defining and managing all configurable parameters related to elemental reactions (e.g., Wetness, Steam, Nature Siphon).
 * Includes the construction of ForgeConfigSpec and the logic for refreshing runtime caches.
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
    public static final ForgeConfigSpec.DoubleValue WETNESS_SELF_DRYING_DAMAGE_PENALTY;

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
    public static final ForgeConfigSpec.DoubleValue STEAM_RADIUS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_CLOUD_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_DURATION_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_BLINDNESS_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;

    // Scalding Config / 烫伤配置
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_SCALE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_WEAKNESS;
    public static final ForgeConfigSpec.IntValue STEAM_IMMUNITY_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STEAM_IMMUNITY_BLACKLIST;

    // Trigger Logic (Dual-Track) / 触发逻辑（双轨制）
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_THRESHOLD_FIRE;
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_THRESHOLD_FROST;

    // Defense System (Layered Defense) / 防御体系（分层防御）
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_FLOOR_RATIO;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_FIRE_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_GENERAL_PROT_CAP;

    // ======================== Enchantment Settings / 附魔设置 ========================
    public static final ForgeConfigSpec.BooleanValue ENCHANT_FIRE_ASPECT_EXCLUSIVE;

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
                        "每层潮湿提供的通用易伤修正（0.1 = 10%）。")
                .defineInRange("wetness_resist_modifier", 0.1, 0.0, 1.0);

        WETNESS_FIRE_REDUCTION = BUILDER
                .comment("Fire damage reduction percentage per Wetness level.",
                        "每层潮湿提供的赤焰属性伤害减免比例（0.1 = 10%）。")
                .defineInRange("wetness_fire_reduction", 0.1, 0.0, 1.0);

        WETNESS_RAIN_GAIN_INTERVAL = BUILDER
                .comment("Interval (in seconds) to gain 1 Wetness level while standing in rain.",
                        "玩家或生物在雨中时，获得 1 层潮湿效果的时间间隔（秒）。")
                .defineInRange("wetness_rain_gain_interval", 10, 1, 3600);

        WETNESS_DECAY_BASE_TIME = BUILDER
                .comment("Base natural decay time (in seconds) per Wetness level.",
                        "每层潮湿的基础自然消退时间（秒）。")
                .defineInRange("wetness_decay_base_time", 10, 1, 3600);

        WETNESS_EXHAUSTION_INCREASE = BUILDER
                .comment("Extra exhaustion added per Wetness level when consuming hunger.",
                        "每层潮湿额外增加的耗竭度。")
                .defineInRange("wetness_exhaustion_increase", 0.05, 0.0, 10.0);

        WETNESS_POTION_ADD_LEVEL = BUILDER
                .comment("Number of Wetness levels added instantly by a Splash Water Bottle.",
                        "被喷溅水瓶击中时，一次性增加的潮湿层数。")
                .defineInRange("wetness_potion_add_level", 1, 1, 100);

        WETNESS_DRYING_THRESHOLD = BUILDER
                .comment("Fire enhancement points required to remove ONE extra wetness level (Self-Drying).",
                        "攻击时移除 1 层额外自身潮湿效果所需的赤焰强化点数（自我干燥）。")
                .defineInRange("wetness_drying_threshold", 20, 1, 1000);

        WETNESS_SELF_DRYING_DAMAGE_PENALTY = BUILDER
                .comment("Damage penalty ratio when a wet Fire attacker dries themselves.",
                        "当赤焰属性攻击者处于潮湿状态时，触发自我干燥导致的伤害惩罚比例（0.3 = -30% 伤害）。")
                .defineInRange("wetness_self_drying_damage_penalty", 0.3, 0.0, 1.0);

        BUILDER.push("immunity");
        WETNESS_WATER_ANIMAL_IMMUNE = BUILDER
                .comment("Whether water animals (Fish, Squid, etc.) are immune to Wetness.",
                        "水生生物（如鱼、鱿鱼）是否完全免疫潮湿效果。")
                .define("water_animal_immune", true);

        WETNESS_NETHER_DIMENSION_IMMUNE = BUILDER
                .comment("Whether entities in the Nether dimension are immune to Wetness.",
                        "下界维度中的所有生物是否免疫潮湿效果。")
                .define("nether_dimension_immune", true);

        WETNESS_ENTITY_BLACKLIST = BUILDER
                .comment("Entities in this list will NEVER gain the Wetness effect.",
                        "潮湿效果黑名单（ID格式）。")
                .defineListAllowEmpty("wetness_entity_blacklist", List.of(), o -> o instanceof String);
        BUILDER.pop();
        BUILDER.pop(); // End wetness_system

        // ======================== Nature Reaction ========================
        BUILDER.push("nature_reaction");
        NATURE_WETNESS_BONUS = BUILDER
                .comment("Damage bonus per Wetness level when attacking with Nature element.",
                        "自然属性攻击潮湿目标时，每层潮湿提供的额外伤害倍率（0.1 = +10%）。")
                .defineInRange("nature_wetness_bonus", 0.1, 0.0, 10.0);

        NATURE_SIPHON_THRESHOLD = BUILDER
                .comment("Nature enhancement points required to siphon ONE extra wetness level.",
                        "攻击时吸取 1 层额外潮湿效果所需的自然强化点数。")
                .defineInRange("nature_siphon_threshold", 20, 1, 1000);

        NATURE_SIPHON_HEAL = BUILDER
                .comment("Health points restored when successfully siphoning wetness.",
                        "成功吸取潮湿效果时恢复的生命值点数。")
                .defineInRange("nature_siphon_heal", 1.0, 0.0, 100.0);
        BUILDER.pop(); // End nature_reaction

        // ======================== Steam Reaction ========================
        BUILDER.push("steam_reaction");

        STEAM_REACTION_ENABLED = BUILDER
                .comment("Whether to enable the Steam Reaction mechanism.",
                        "是否开启蒸汽反应机制。")
                .define("steam_reaction_enabled", true);

        STEAM_MAX_REDUCTION = BUILDER
                .comment("Global Cap for damage modifications (Wetness effects).",
                        "潮湿相关伤害修正（增伤/减伤）的全局上限。")
                .defineInRange("steam_max_reduction", 0.9, 0.0, 1.0);

        STEAM_CLOUD_RADIUS = BUILDER
                .comment("Base radius of the steam cloud (Level 1).",
                        "蒸汽云的基础半径（1层潮湿）。")
                .defineInRange("steam_cloud_radius", 2.0, 0.5, 10.0);

        STEAM_RADIUS_PER_LEVEL = BUILDER
                .comment("Extra radius added per Wetness level.",
                        "每层潮湿额外增加的蒸汽云半径。")
                .defineInRange("steam_radius_per_level", 0.5, 0.0, 5.0);

        STEAM_CLOUD_DURATION = BUILDER
                .comment("Base duration of the steam cloud (in Ticks).",
                        "蒸汽云雾的基础持续时间（20 Tick = 1秒）。")
                .defineInRange("steam_cloud_duration", 100, 20, 1200);

        STEAM_DURATION_PER_LEVEL = BUILDER
                .comment("Extra duration added per Wetness level (in Ticks).",
                        "每层潮湿额外增加的持续时间（Tick）。")
                .defineInRange("steam_duration_per_level", 20, 0, 200);

        STEAM_BLINDNESS_DURATION = BUILDER
                .comment("Duration of Blindness effect applied inside the cloud (in Ticks).",
                        "进入蒸汽云雾时获得的失明效果持续时间。")
                .defineInRange("steam_blindness_duration", 60, 20, 600);

        STEAM_CONDENSATION_INTERVAL = BUILDER
                .comment("Interval (in Ticks) to add 1 Wetness level inside Low Heat Steam (Condensation).",
                        "在低温蒸汽（冷凝）中每隔多少 Tick 增加 1 层潮湿效果（20 Tick = 1秒）。")
                .defineInRange("steam_condensation_interval", 20, 1, 1200);

        STEAM_CLEAR_AGGRO = BUILDER
                .comment("Whether steam clouds clear mob aggro.",
                        "是否清除位于蒸汽云内怪物的仇恨目标。")
                .define("steam_clear_aggro", true);

        STEAM_SCALDING_DAMAGE = BUILDER
                .comment("Base damage dealt by High Heat steam per second.",
                        "高温蒸汽每秒造成的基础烫伤伤害。")
                .defineInRange("steam_scalding_damage", 1.0, 0.0, 100.0);

        STEAM_DAMAGE_SCALE_PER_LEVEL = BUILDER
                .comment("Damage scaling multiplier per steam cloud level.",
                        "蒸汽云每增加 1 个等级（由潮湿层数决定），伤害增加的倍率（0.2 = +20%）。")
                .defineInRange("steam_damage_scale_per_level", 0.2, 0.0, 10.0);

        STEAM_SCALDING_MULTIPLIER_WEAKNESS = BUILDER
                .comment("Damage multiplier for Frost/Nature entities in hot steam.",
                        "冰霜/自然属性生物在高温蒸汽中受到的易伤倍率。")
                .defineInRange("steam_scalding_multiplier_weakness", 1.5, 1.0, 10.0);

        STEAM_IMMUNITY_THRESHOLD = BUILDER
                .comment("Fire Resistance points required to be immune to steam scalding.",
                        "免疫蒸汽烫伤所需的赤焰抗性点数阈值。")
                .defineInRange("steam_immunity_threshold", 80, 0, 1000);

        STEAM_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this list are immune to steam scalding.",
                        "蒸汽烫伤免疫黑名单（ID格式）。")
                .defineListAllowEmpty("steam_immunity_blacklist", List.of(), o -> o instanceof String);

        // --- Trigger Logic (Dual-Track) ---
        BUILDER.push("trigger_logic");
        STEAM_TRIGGER_THRESHOLD_FIRE = BUILDER
                .comment("Fire Power threshold required to trigger Steam Burst vs Frost/Wet.",
                        "赤焰属性攻击触发蒸汽爆发（VS 冰霜/潮湿）所需的强化阈值。")
                .defineInRange("fire_trigger_threshold", 50, 0, 1000);

        STEAM_TRIGGER_THRESHOLD_FROST = BUILDER
                .comment("Frost Power threshold required to trigger Steam Condensation vs Fire.",
                        "冰霜属性攻击触发蒸汽冷凝（VS 赤焰）所需的强化阈值。")
                .defineInRange("frost_trigger_threshold", 50, 0, 1000);
        BUILDER.pop(); // End trigger_logic

        // --- Defense System ---
        BUILDER.push("defense_system");
        STEAM_DAMAGE_FLOOR_RATIO = BUILDER
                .comment("Minimum damage floor ratio for vulnerable targets (Frost/Nature).",
                        "Final Damage >= Raw Damage * Floor Ratio.",
                        "针对易伤目标（冰霜/自然）的最小保底伤害比例。",
                        "即使防御减免很高，最终伤害也不会低于：原始伤害 * 此比例（0.5 = 50%）。")
                .defineInRange("damage_floor_ratio", 0.5, 0.0, 1.0);

        STEAM_MAX_FIRE_PROT_CAP = BUILDER
                .comment("Maximum damage reduction cap allowed from Fire Protection enchantment.",
                        "火焰保护附魔允许提供的最大伤害减免上限（0.5 = 最多减免 50%）。")
                .defineInRange("max_fire_prot_cap", 0.5, 0.0, 1.0);

        STEAM_MAX_GENERAL_PROT_CAP = BUILDER
                .comment("Maximum damage reduction cap allowed from general Protection enchantment.",
                        "普通保护附魔允许提供的最大伤害减免上限（0.25 = 最多减免 25%）。")
                .defineInRange("max_general_prot_cap", 0.25, 0.0, 1.0);
        BUILDER.pop(); // End defense_system

        BUILDER.pop(); // End steam_reaction

        // ======================== Enchantment Settings ========================
        BUILDER.push("enchantment_settings");
        ENCHANT_FIRE_ASPECT_EXCLUSIVE = BUILDER
                .comment("Whether Elemental Aspect enchantments are exclusive with Vanilla Fire Aspect.",
                        "元素属性附魔（赤焰/冰霜等）是否与原版火焰附加互斥（铁砧/附魔台）。")
                .define("exclusive_with_fire_aspect", true);
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

    public static double natureWetnessBonus = 0.1;
    public static int natureSiphonThreshold = 20;
    public static double natureSiphonHeal = 1.0;

    public static boolean steamReactionEnabled = true;
    public static double steamMaxReduction = 0.9;
    public static double steamCloudRadius = 2.0;
    public static double steamRadiusPerLevel = 0.5; // Cache
    public static int steamCloudDuration = 100;
    public static int steamDurationPerLevel = 20; // Cache
    public static int steamBlindnessDuration = 60;
    public static int steamCondensationInterval = 20;
    public static boolean steamClearAggro = true;
    public static double steamScaldingDamage = 1.0;
    public static double steamDamageScalePerLevel = 0.2; // Cache
    public static double steamScaldingMultiplierWeakness = 1.5;
    public static int steamImmunityThreshold = 80;
    public static List<? extends String> cachedSteamBlacklist = List.of();

    public static int steamTriggerThresholdFire = 50;
    public static int steamTriggerThresholdFrost = 50;
    public static double steamDamageFloorRatio = 0.5;
    public static double steamMaxFireProtCap = 0.5;
    public static double steamMaxGeneralProtCap = 0.25;

    public static boolean enchantFireAspectExclusive = true;

    /**
     * 注册配置文件。
     * <p>
     * Registers the configuration file.
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "elementalcraft-reactions.toml");
    }

    /**
     * 刷新配置缓存。
     * 将配置文件中的值读取到静态变量中，以提高访问效率。
     * <p>
     * Refreshes the configuration cache.
     * Reads values from the config file into static variables to improve access efficiency.
     */
    public static void refreshCache() {
        wetnessMaxLevel = WETNESS_MAX_LEVEL.get();
        wetnessResistModifier = WETNESS_RESIST_MODIFIER.get();
        wetnessFireReduction = WETNESS_FIRE_REDUCTION.get();
        wetnessRainGainInterval = WETNESS_RAIN_GAIN_INTERVAL.get();
        wetnessDecayBaseTime = WETNESS_DECAY_BASE_TIME.get();
        wetnessExhaustionIncrease = WETNESS_EXHAUSTION_INCREASE.get();
        wetnessPotionAddLevel = WETNESS_POTION_ADD_LEVEL.get();
        wetnessDryingThreshold = WETNESS_DRYING_THRESHOLD.get();
        wetnessSelfDryingDamagePenalty = WETNESS_SELF_DRYING_DAMAGE_PENALTY.get();

        wetnessWaterAnimalImmune = WETNESS_WATER_ANIMAL_IMMUNE.get();
        wetnessNetherDimensionImmune = WETNESS_NETHER_DIMENSION_IMMUNE.get();
        cachedWetnessBlacklist = WETNESS_ENTITY_BLACKLIST.get();

        natureWetnessBonus = NATURE_WETNESS_BONUS.get();
        natureSiphonThreshold = NATURE_SIPHON_THRESHOLD.get();
        natureSiphonHeal = NATURE_SIPHON_HEAL.get();

        steamReactionEnabled = STEAM_REACTION_ENABLED.get();
        steamMaxReduction = STEAM_MAX_REDUCTION.get();
        steamCloudRadius = STEAM_CLOUD_RADIUS.get();
        steamRadiusPerLevel = STEAM_RADIUS_PER_LEVEL.get(); // Refresh
        steamCloudDuration = STEAM_CLOUD_DURATION.get();
        steamDurationPerLevel = STEAM_DURATION_PER_LEVEL.get(); // Refresh
        steamBlindnessDuration = STEAM_BLINDNESS_DURATION.get();
        steamCondensationInterval = STEAM_CONDENSATION_INTERVAL.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        steamScaldingDamage = STEAM_SCALDING_DAMAGE.get();
        steamDamageScalePerLevel = STEAM_DAMAGE_SCALE_PER_LEVEL.get(); // Refresh
        steamScaldingMultiplierWeakness = STEAM_SCALDING_MULTIPLIER_WEAKNESS.get();
        steamImmunityThreshold = STEAM_IMMUNITY_THRESHOLD.get();
        cachedSteamBlacklist = STEAM_IMMUNITY_BLACKLIST.get();

        steamTriggerThresholdFire = STEAM_TRIGGER_THRESHOLD_FIRE.get();
        steamTriggerThresholdFrost = STEAM_TRIGGER_THRESHOLD_FROST.get();
        steamDamageFloorRatio = STEAM_DAMAGE_FLOOR_RATIO.get();
        steamMaxFireProtCap = STEAM_MAX_FIRE_PROT_CAP.get();
        steamMaxGeneralProtCap = STEAM_MAX_GENERAL_PROT_CAP.get();

        enchantFireAspectExclusive = ENCHANT_FIRE_ASPECT_EXCLUSIVE.get();
    }
}