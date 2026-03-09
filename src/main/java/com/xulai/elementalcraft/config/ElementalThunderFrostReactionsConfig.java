package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ElementalThunderFrostReactionsConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue THUNDER_STRENGTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue STATIC_BASE_CHANCE;
    public static final ForgeConfigSpec.IntValue STATIC_SCALING_STEP;
    public static final ForgeConfigSpec.DoubleValue STATIC_SCALING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue STATIC_WETNESS_BONUS_CHANCE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STATIC_STACKING_BONUS_CHANCE;
    public static final ForgeConfigSpec.IntValue STATIC_MAX_STACKS_PER_ATTACK;
    public static final ForgeConfigSpec.IntValue STATIC_MAX_TOTAL_STACKS;
    public static final ForgeConfigSpec.IntValue STATIC_DURATION_PER_STACK_TICKS;

    public static final ForgeConfigSpec.IntValue STATIC_RESIST_IMMUNITY_THRESHOLD;

    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_MIN;
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_MAX;
    // 新增：静电伤害触发间隔（刻）
    public static final ForgeConfigSpec.IntValue STATIC_DAMAGE_INTERVAL_TICKS;

    // 元素属性伤害修正（百分比）
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_NATURE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_FROST_MULTIPLIER;

    public static final ForgeConfigSpec.IntValue PARALYSIS_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue PARALYSIS_DURATION_PER_STACK_TICKS;
    public static final ForgeConfigSpec.DoubleValue PARALYSIS_DAMAGE_PERCENTAGE;

    public static final ForgeConfigSpec.DoubleValue PARALYSIS_SPREAD_THRESHOLD_PERCENTAGE;
    public static final ForgeConfigSpec.IntValue PARALYSIS_SPREAD_BASE_RANGE;
    public static final ForgeConfigSpec.IntValue PARALYSIS_SPREAD_RANGE_PER_EXTRA_STACK;
    public static final ForgeConfigSpec.DoubleValue PARALYSIS_SPREAD_STATIC_PERCENTAGE;
    public static final ForgeConfigSpec.BooleanValue PARALYSIS_SPREAD_ALLOW_CHAIN;
    public static final ForgeConfigSpec.BooleanValue PARALYSIS_SPREAD_EXCLUDE_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue PARALYSIS_SPREAD_EXCLUDE_PETS;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Static Shock (Thunder) Reaction Configuration",
                        "静电（雷霆）反应配置")
                .push("static_shock");

        BUILDER.comment("Trigger & Stack Rules", "触发与叠加规则")
                .push("trigger_and_stack");

        THUNDER_STRENGTH_THRESHOLD = BUILDER
                .comment("Minimum Thunder Strength points required for the attacker to have a chance to apply Static Shock.",
                         "攻击者触发静电效果所需的最低雷霆属性强化点数。",
                         "Default: 20")
                .defineInRange("thunder_strength_threshold", 20, 1, 10000);

        STATIC_BASE_CHANCE = BUILDER
                .comment("Base chance (as a decimal) to apply Static Shock on attack when the threshold is met.",
                         "达到门槛后，攻击触发静电的基础概率（小数形式）。",
                         "Default: 0.05 (5%)")
                .defineInRange("static_base_chance", 0.05, 0.0, 1.0);

        STATIC_SCALING_STEP = BUILDER
                .comment("Strength step size for increasing the application chance.",
                         "触发概率成长所需的强化点数步长。",
                         "Default: 20")
                .defineInRange("static_scaling_step", 20, 1, 10000);

        STATIC_SCALING_CHANCE = BUILDER
                .comment("Additional chance (as a decimal) gained per each scaling step.",
                         "每达到一个步长增加的额外概率（小数形式）。",
                         "Default: 0.05 (5%)")
                .defineInRange("static_scaling_chance", 0.05, 0.0, 1.0);

        STATIC_WETNESS_BONUS_CHANCE_PER_LEVEL = BUILDER
                .comment("Additional chance (as a decimal) per level of Wetness effect on the target.",
                         "目标身上每层潮湿效果增加的额外概率（小数形式）。",
                         "Default: 0.05 (5% per level)")
                .defineInRange("static_wetness_bonus_chance_per_level", 0.05, 0.0, 1.0);

        STATIC_STACKING_BONUS_CHANCE = BUILDER
                .comment("Additional chance (as a decimal) when target already has Static Shock effect.",
                         "目标已存在静电效果时的额外叠加概率（小数形式）。",
                         "Default: 0.05 (5%)")
                .defineInRange("static_stacking_bonus_chance", 0.05, 0.0, 1.0);

        STATIC_MAX_STACKS_PER_ATTACK = BUILDER
                .comment("Maximum number of Static Shock stacks that can be applied in a single attack.",
                         "单次攻击最多可施加的静电层数。",
                         "Default: 1")
                .defineInRange("static_max_stacks_per_attack", 1, 1, 100);

        STATIC_MAX_TOTAL_STACKS = BUILDER
                .comment("Maximum total stacks of Static Shock a target can have. Once reached, no more stacks can be applied.",
                         "目标身上静电的最大总层数。达到上限后无法继续叠加。",
                         "Default: 5")
                .defineInRange("static_max_total_stacks", 5, 1, 1000);

        STATIC_DURATION_PER_STACK_TICKS = BUILDER
                .comment("Base duration (in ticks) per stack of Static Shock. 20 ticks = 1 second.",
                         "每层静电的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 100 (5 seconds)")
                .defineInRange("static_duration_per_stack_ticks", 100, 1, 72000);

        BUILDER.pop();

        BUILDER.comment("Immunity Rule", "免疫规则")
                .push("immunity");

        STATIC_RESIST_IMMUNITY_THRESHOLD = BUILDER
                .comment("Thunder Resistance points required for an entity to become completely immune to Static Shock (both stacking and damage).",
                         "实体完全免疫静电（叠加和伤害）所需的雷霆抗性点数。",
                         "Default: 80")
                .defineInRange("static_resist_immunity_threshold", 80, 1, 10000);

        BUILDER.pop();

        BUILDER.comment("Periodic Damage (Damage over Time)", "周期性伤害（持续伤害）")
                .push("periodic_damage");

        STATIC_DAMAGE_MIN = BUILDER
                .comment("Minimum damage dealt per damage tick (in half-hearts, where 1.0 = 1 heart).",
                         "每次伤害的最小值（以半心为单位，1.0 = 1心）。",
                         "Default: 1.0")
                .defineInRange("static_damage_min", 1.0, 0.0, 10000.0);

        STATIC_DAMAGE_MAX = BUILDER
                .comment("Maximum damage dealt per damage tick (in half-hearts, where 1.0 = 1 heart).",
                         "每次伤害的最大值（以半心为单位，1.0 = 1心）。",
                         "Default: 5.0")
                .defineInRange("static_damage_max", 5.0, 0.0, 10000.0);

        STATIC_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Interval (in ticks) between each Static Shock damage tick. 20 ticks = 1 second.",
                         "每次静电伤害触发的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 100 (5 seconds)")
                .defineInRange("static_damage_interval_ticks", 100, 1, 72000);

        BUILDER.pop();

        BUILDER.comment("Elemental Attribute Modifiers", "元素属性修正")
                .push("elemental_modifiers");

        STATIC_DAMAGE_NATURE_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Nature attribute mobs when taking Static Shock damage (as decimal). 0.5 = 50% damage.",
                         "自然属性生物受到静电伤害时的伤害倍率（小数形式）。0.5 = 50%伤害。",
                         "Default: 0.5")
                .defineInRange("static_damage_nature_multiplier", 0.5, 0.0, 10.0);

        STATIC_DAMAGE_FROST_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Frost attribute mobs when taking Static Shock damage (as decimal). 1.5 = 150% damage.",
                         "冰霜属性生物受到静电伤害时的伤害倍率（小数形式）。1.5 = 150%伤害。",
                         "Default: 1.5")
                .defineInRange("static_damage_frost_multiplier", 1.5, 0.0, 10.0);

        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("Paralysis Reaction Configuration",
                        "麻痹反应配置")
                .push("paralysis");

        PARALYSIS_MAX_STACKS = BUILDER
                .comment("Maximum total stacks of Paralysis a target can have.",
                         "目标身上麻痹的最大总层数。",
                         "Default: 5")
                .defineInRange("paralysis_max_stacks", 5, 1, 1000);

        PARALYSIS_DURATION_PER_STACK_TICKS = BUILDER
                .comment("Base duration (in ticks) per stack of Paralysis. 20 ticks = 1 second.",
                         "每层麻痹的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 10 (0.5 seconds)")
                .defineInRange("paralysis_duration_per_stack_ticks", 10, 1, 72000);

        PARALYSIS_DAMAGE_PERCENTAGE = BUILDER
                .comment("Percentage of remaining Static Shock damage dealt when Paralysis is triggered (as decimal). 0.5 = 50%.",
                         "触发麻痹时，静电剩余伤害的百分比（小数形式）。0.5 = 50%。",
                         "Default: 0.5")
                .defineInRange("paralysis_damage_percentage", 0.5, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Paralysis Static Shock Spread Configuration",
                        "麻痹静电传染配置")
                .push("paralysis_spread");

        PARALYSIS_SPREAD_THRESHOLD_PERCENTAGE = BUILDER
                .comment("Percentage of max paralysis stacks required to trigger static shock spread (as decimal). 0.5 = 50%.",
                         "触发静电传染所需的麻痹层数百分比（小数形式）。0.5 = 50%。",
                         "Default: 0.5")
                .defineInRange("paralysis_spread_threshold_percentage", 0.5, 0.0, 1.0);

        PARALYSIS_SPREAD_BASE_RANGE = BUILDER
                .comment("Base spread range (in blocks) for static shock from paralyzed entities. 3 = 3x3 area.",
                         "麻痹生物传染静电的基础范围（以方块为单位）。3 = 3x3区域。",
                         "Default: 3")
                .defineInRange("paralysis_spread_base_range", 3, 1, 20);

        PARALYSIS_SPREAD_RANGE_PER_EXTRA_STACK = BUILDER
                .comment("Additional spread range per extra paralysis stack beyond threshold.",
                         "超过阈值后，每层额外麻痹增加的传染范围。",
                         "Default: 1")
                .defineInRange("paralysis_spread_range_per_extra_stack", 1, 0, 10);

        PARALYSIS_SPREAD_STATIC_PERCENTAGE = BUILDER
                .comment("Percentage of paralysis stacks to convert to static shock stacks (as decimal). 0.5 = 50%.",
                         "麻痹层数转换为静电层数的百分比（小数形式）。0.5 = 50%。",
                         "Default: 0.5")
                .defineInRange("paralysis_spread_static_percentage", 0.5, 0.0, 1.0);

        PARALYSIS_SPREAD_ALLOW_CHAIN = BUILDER
                .comment("Whether infected entities can further spread static shock to others.",
                         "被传染的实体是否能够继续传染静电给其他生物。",
                         "Default: false")
                .define("paralysis_spread_allow_chain", false);

        PARALYSIS_SPREAD_EXCLUDE_PLAYERS = BUILDER
                .comment("Whether to exclude players from static shock spread.",
                         "是否排除玩家，使其不会被传染静电。",
                         "Default: true")
                .define("paralysis_spread_exclude_players", true);

        PARALYSIS_SPREAD_EXCLUDE_PETS = BUILDER
                .comment("Whether to exclude player pets from static shock spread.",
                         "是否排除玩家的宠物，使其不会被传染静电。",
                         "Default: true")
                .define("paralysis_spread_exclude_pets", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static int thunderStrengthThreshold;
    public static double staticBaseChance;
    public static int staticScalingStep;
    public static double staticScalingChance;
    public static double staticWetnessBonusChancePerLevel;
    public static double staticStackingBonusChance;
    public static int staticMaxStacksPerAttack;
    public static int staticMaxTotalStacks;
    public static int staticDurationPerStackTicks;
    public static int staticResistImmunityThreshold;
    public static double staticDamageMin;
    public static double staticDamageMax;
    public static int staticDamageIntervalTicks; 
    public static double staticDamageNatureMultiplier;
    public static double staticDamageFrostMultiplier;

    public static int paralysisMaxStacks;
    public static int paralysisDurationPerStackTicks;
    public static double paralysisDamagePercentage;

    public static double paralysisSpreadThresholdPercentage;
    public static int paralysisSpreadBaseRange;
    public static int paralysisSpreadRangePerExtraStack;
    public static double paralysisSpreadStaticPercentage;
    public static boolean paralysisSpreadAllowChain;
    public static boolean paralysisSpreadExcludePlayers;
    public static boolean paralysisSpreadExcludePets;

    public static void register() {
        register("elementalcraft-thunderfrost-reactions.toml");
    }
    
    public static void register(String configPath) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, configPath);
    }

    public static void refreshCache() {
        thunderStrengthThreshold = THUNDER_STRENGTH_THRESHOLD.get();
        staticBaseChance = STATIC_BASE_CHANCE.get();
        staticScalingStep = STATIC_SCALING_STEP.get();
        staticScalingChance = STATIC_SCALING_CHANCE.get();
        staticWetnessBonusChancePerLevel = STATIC_WETNESS_BONUS_CHANCE_PER_LEVEL.get();
        staticStackingBonusChance = STATIC_STACKING_BONUS_CHANCE.get();
        staticMaxStacksPerAttack = STATIC_MAX_STACKS_PER_ATTACK.get();
        staticMaxTotalStacks = STATIC_MAX_TOTAL_STACKS.get();
        staticDurationPerStackTicks = STATIC_DURATION_PER_STACK_TICKS.get();
        staticResistImmunityThreshold = STATIC_RESIST_IMMUNITY_THRESHOLD.get();
        staticDamageMin = STATIC_DAMAGE_MIN.get();
        staticDamageMax = STATIC_DAMAGE_MAX.get();
        staticDamageIntervalTicks = STATIC_DAMAGE_INTERVAL_TICKS.get(); // 新增

        staticDamageNatureMultiplier = STATIC_DAMAGE_NATURE_MULTIPLIER.get();
        staticDamageFrostMultiplier = STATIC_DAMAGE_FROST_MULTIPLIER.get();

        paralysisMaxStacks = PARALYSIS_MAX_STACKS.get();
        paralysisDurationPerStackTicks = PARALYSIS_DURATION_PER_STACK_TICKS.get();
        paralysisDamagePercentage = PARALYSIS_DAMAGE_PERCENTAGE.get();

        paralysisSpreadThresholdPercentage = PARALYSIS_SPREAD_THRESHOLD_PERCENTAGE.get();
        paralysisSpreadBaseRange = PARALYSIS_SPREAD_BASE_RANGE.get();
        paralysisSpreadRangePerExtraStack = PARALYSIS_SPREAD_RANGE_PER_EXTRA_STACK.get();
        paralysisSpreadStaticPercentage = PARALYSIS_SPREAD_STATIC_PERCENTAGE.get();
        paralysisSpreadAllowChain = PARALYSIS_SPREAD_ALLOW_CHAIN.get();
        paralysisSpreadExcludePlayers = PARALYSIS_SPREAD_EXCLUDE_PLAYERS.get();
        paralysisSpreadExcludePets = PARALYSIS_SPREAD_EXCLUDE_PETS.get();
    }

    private ElementalThunderFrostReactionsConfig() {}
}