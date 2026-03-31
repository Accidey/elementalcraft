package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

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
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STATIC_IMMUNITY_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PARALYSIS_IMMUNITY_BLACKLIST;

    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_MIN;
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_MAX;
    public static final ForgeConfigSpec.IntValue STATIC_DAMAGE_INTERVAL_TICKS;

    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_NATURE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_FROST_MULTIPLIER;

    public static final ForgeConfigSpec.BooleanValue STATIC_SPLASH_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STATIC_SPLASH_DAMAGE_PERCENTAGE;
    public static final ForgeConfigSpec.IntValue STATIC_SPLASH_BASE_RANGE;
    public static final ForgeConfigSpec.IntValue STATIC_SPLASH_RANGE_PER_STACK;
    public static final ForgeConfigSpec.IntValue STATIC_SPLASH_MAX_RANGE;
    public static final ForgeConfigSpec.BooleanValue STATIC_SPLASH_SKIP_IF_TARGET_HAS_STATIC;
    public static final ForgeConfigSpec.BooleanValue STATIC_SPLASH_ALLOW_FROM_SPREAD;
    public static final ForgeConfigSpec.BooleanValue STATIC_SPLASH_EXCLUDE_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue STATIC_SPLASH_EXCLUDE_PETS;
    public static final ForgeConfigSpec.BooleanValue STATIC_SPLASH_TRIGGER_PARALYSIS_ON_WET;

    public static final ForgeConfigSpec.DoubleValue NATURE_ATTACK_TRIGGER_BASE_CHANCE;
    public static final ForgeConfigSpec.IntValue THUNDER_ENHANCE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue THUNDER_ENHANCE_CHANCE_PER_STEP;
    public static final ForgeConfigSpec.IntValue NATURE_ATTACK_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue STATIC_STACKS_WHEN_NO_WETNESS;

    public static final ForgeConfigSpec.DoubleValue COUNTER_LIGHTNING_DAMAGE;

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
                        "静电（雷霆）效果配置")
                .push("static_shock");

        BUILDER.comment("Trigger & Stack Rules", "触发与叠加规则")
                .push("trigger_and_stack");

        THUNDER_STRENGTH_THRESHOLD = BUILDER
                .comment("Minimum Thunder Strength points required for the attacker to have a chance to apply Static Shock.",
                         "攻击者触发静电效果所需的最低雷霆属性强化点数。",
                         "Default: 20")
                .defineInRange("thunder_strength_threshold", 20, 1, 10000);

        STATIC_BASE_CHANCE = BUILDER
                .comment("Base chanceto apply Static Shock on attack when the threshold is met.",
                         "达到门槛后，攻击触发静电的基础概率。",
                         "Default: 0.05 (5%)")
                .defineInRange("static_base_chance", 0.05, 0.0, 1.0);

        STATIC_SCALING_STEP = BUILDER
                .comment("Strength step size for increasing the application chance.",
                         "触发概率成长所需的强化点数步长。",
                         "Default: 20")
                .defineInRange("static_scaling_step", 20, 1, 10000);

        STATIC_SCALING_CHANCE = BUILDER
                .comment("Additional chancegained per each scaling step.",
                         "每达到一个步长增加的额外概率。",
                         "Default: 0.05 (5%)")
                .defineInRange("static_scaling_chance", 0.05, 0.0, 1.0);

        STATIC_WETNESS_BONUS_CHANCE_PER_LEVEL = BUILDER
                .comment("Additional chanceper level of Wetness effect on the target.",
                         "目标身上每层潮湿效果增加的额外概率。",
                         "Default: 0.05 (5% per level)")
                .defineInRange("static_wetness_bonus_chance_per_level", 0.05, 0.0, 1.0);

        STATIC_STACKING_BONUS_CHANCE = BUILDER
                .comment("Additional chancewhen target already has Static Shock effect.",
                         "目标已存在静电效果时的额外叠加概率。",
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

        STATIC_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this blacklist are completely immune to Static Shock effect (cannot be applied).",
                         "处于此黑名单中的实体完全免疫静电效果（无法被施加）。",
                         "Example: [\"minecraft:creeper\", \"minecraft:skeleton\"]")
                .defineListAllowEmpty("static_immunity_blacklist", List.of(), o -> o instanceof String);

        PARALYSIS_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this blacklist are completely immune to Paralysis effect (cannot be applied).",
                         "处于此黑名单中的实体完全免疫麻痹效果（无法被施加）。",
                         "Example: [\"minecraft:iron_golem\", \"minecraft:wither\"]")
                .defineListAllowEmpty("paralysis_immunity_blacklist", List.of(), o -> o instanceof String);

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
                .comment("Damage multiplier for Nature attribute mobs when taking Static Shock damage . 0.5 = 50% damage.",
                         "自然属性生物受到静电伤害时的伤害倍率。0.5 = 50%伤害。",
                         "Default: 0.5")
                .defineInRange("static_damage_nature_multiplier", 0.5, 0.0, 10.0);

        STATIC_DAMAGE_FROST_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Frost attribute mobs when taking Static Shock damage . 1.5 = 150% damage.",
                         "冰霜属性生物受到静电伤害时的伤害倍率。1.5 = 150%伤害。",
                         "Default: 1.5")
                .defineInRange("static_damage_frost_multiplier", 1.5, 0.0, 10.0);

        BUILDER.pop();

        BUILDER.comment("Static Splash (Chain Lightning) Configuration",
                        "静电传导配置")
                .push("static_splash");

        STATIC_SPLASH_ENABLED = BUILDER
                .comment("Enable static splash effect. When enabled, entities with Static Shock will damage nearby entities when taking periodic damage.",
                         "启用静电传导效果。启用后，带有静电效果的实体受到周期性伤害时会对周围实体造成溅射伤害。",
                         "Default: true")
                .define("static_splash_enabled", true);

        STATIC_SPLASH_DAMAGE_PERCENTAGE = BUILDER
                .comment("Percentage of the original damage dealt to splash targets. 0.5 = 50% damage.",
                         "溅射伤害占原始伤害的百分比。0.5 = 50%伤害。",
                         "Default: 0.5")
                .defineInRange("static_splash_damage_percentage", 0.5, 0.0, 1.0);

        STATIC_SPLASH_BASE_RANGE = BUILDER
                .comment("Base splash range (in blocks) when target has 1 stack of Static Shock.",
                         "1层静电时的基础溅射范围（以方块为单位）。",
                         "Default: 1")
                .defineInRange("static_splash_base_range", 1, 1, 20);

        STATIC_SPLASH_RANGE_PER_STACK = BUILDER
                .comment("Additional range (in blocks) per extra stack of Static Shock beyond the first.",
                         "超过1层后，每层静电增加的范围（以方块为单位）。",
                         "Default: 1")
                .defineInRange("static_splash_range_per_stack", 1, 0, 10);

        STATIC_SPLASH_MAX_RANGE = BUILDER
                .comment("Maximum splash range (in blocks) regardless of stacks.",
                         "溅射最大范围限制（以方块为单位）。",
                         "Default: 10")
                .defineInRange("static_splash_max_range", 10, 1, 50);

        STATIC_SPLASH_SKIP_IF_TARGET_HAS_STATIC = BUILDER
                .comment("If true, splash damage will not affect entities that already have Static Shock effect.",
                         "如果为 true，溅射伤害不会影响已经带有静电效果的实体。",
                         "Default: true")
                .define("static_splash_skip_if_target_has_static", true);

        STATIC_SPLASH_ALLOW_FROM_SPREAD = BUILDER
                .comment("If true, Static Shock that originated from paralysis spread can also trigger splash damage.",
                         "如果为 true，由麻痹传播而来的静电也能触发溅射伤害。",
                         "Default: false")
                .define("static_splash_allow_from_spread", false);

        STATIC_SPLASH_EXCLUDE_PLAYERS = BUILDER
                .comment("If true, players are immune to static splash damage.",
                         "如果为 true，玩家免疫静电传导伤害。",
                         "Default: true")
                .define("static_splash_exclude_players", true);

        STATIC_SPLASH_EXCLUDE_PETS = BUILDER
                .comment("If true, tamed pets are immune to static splash damage.",
                         "如果为 true，已驯服的宠物免疫静电传导伤害。",
                         "Default: true")
                .define("static_splash_exclude_pets", true);

        STATIC_SPLASH_TRIGGER_PARALYSIS_ON_WET = BUILDER
                .comment("If true, splash on a wet target will trigger Paralysis reaction (instead of dealing splash damage).",
                         "如果为 true，溅射到潮湿目标时触发麻痹反应（而不是造成溅射伤害）。",
                         "Default: true")
                .define("static_splash_trigger_paralysis_on_wet", true);

        BUILDER.pop();

        BUILDER.pop(); 

        BUILDER.comment("Nature Attack Lightning Trigger Configuration",
                        "自然属性攻击触发闪电配置")
                .push("nature_lightning_trigger");

        NATURE_ATTACK_TRIGGER_BASE_CHANCE = BUILDER
                .comment("Base chance to trigger Lightning when Nature attacks Thunder without Wetness",
                        "自然属性没潮湿效果时攻击雷霆属性时触发闪电的基础概率",
                        "Default: 0.05 (5%)")
                .defineInRange("nature_attack_trigger_base_chance", 0.05, 0.0, 1.0);

        THUNDER_ENHANCE_THRESHOLD = BUILDER
                .comment("Thunder enhancement points threshold for chance increase",
                        "触发概率增加的雷霆强化点数阈值",
                        "Default: 20")
                .defineInRange("thunder_enhance_threshold", 20, 1, 10000); 

        THUNDER_ENHANCE_CHANCE_PER_STEP = BUILDER
                .comment("Additional chance  per thunder enhancement step",
                        "每达到一个雷霆强化步长增加的额外概率",
                        "Default: 0.05 (5%)")
                .defineInRange("thunder_enhance_chance_per_step", 0.05, 0.0, 1.0);

        NATURE_ATTACK_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown ticks after triggering lightning from nature attack",
                        "自然属性触发闪电后的冷却时间(刻)",
                        "Default: 200 (10 seconds)")
                .defineInRange("nature_attack_cooldown_ticks", 200, 1, 72000);

        STATIC_STACKS_WHEN_NO_WETNESS = BUILDER
                .comment("Static shock stacks applied when target has no wetness effect",
                        "目标没有潮湿效果时施加的静电层数",
                        "Default: 2")
                .defineInRange("static_stacks_when_no_wetness", 2, 1, 100);

        COUNTER_LIGHTNING_DAMAGE = BUILDER
                .comment("Damage dealt by counter lightning when nature attacks thunder",
                        "雷霆属性反击自然属性时闪电造成的伤害",
                        "Default: 5.0")
                .defineInRange("counter_lightning_damage", 5.0, 0.0, 100.0);

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
                .comment("Percentage of remaining Static Shock damage dealt when Paralysis is triggered . 0.5 = 50%.",
                         "触发麻痹时，静电剩余伤害的百分比。0.5 = 50%。",
                         "Default: 0.5")
                .defineInRange("paralysis_damage_percentage", 0.5, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Paralysis Spread Configuration",
                "麻痹传染配置")
        .push("paralysis_spread");

PARALYSIS_SPREAD_THRESHOLD_PERCENTAGE = BUILDER
        .comment("Percentage of max paralysis stacks required to trigger paralysis spread. 0.5 = 50%.",
                 "触发麻痹传染所需的麻痹层数百分比。0.5 = 50%。",
                 "Default: 0.5")
        .defineInRange("paralysis_spread_threshold_percentage", 0.5, 0.0, 1.0);

PARALYSIS_SPREAD_BASE_RANGE = BUILDER
        .comment("Base spread range (in blocks) for paralysis from paralyzed entities. 3 = 3x3 area.",
                 "麻痹生物传染麻痹的基础范围（以方块为单位）。3 = 3x3区域。",
                 "Default: 3")
        .defineInRange("paralysis_spread_base_range", 3, 1, 20);

PARALYSIS_SPREAD_RANGE_PER_EXTRA_STACK = BUILDER
        .comment("Additional spread range per extra paralysis stack beyond threshold.",
                 "超过阈值后，每层额外麻痹增加的传染范围。",
                 "Default: 1")
        .defineInRange("paralysis_spread_range_per_extra_stack", 1, 0, 10);

PARALYSIS_SPREAD_STATIC_PERCENTAGE = BUILDER
        .comment("Percentage of source paralysis stacks to apply as paralysis on the target. 0.5 = 50%.",
                 "源实体麻痹层数转换为目标麻痹层数的百分比。0.5 = 50%。",
                 "Default: 0.5")
        .defineInRange("paralysis_spread_static_percentage", 0.5, 0.0, 1.0);

PARALYSIS_SPREAD_ALLOW_CHAIN = BUILDER
        .comment("Whether infected entities can further spread paralysis to others.",
                 "被传染的实体是否能够继续传染麻痹给其他生物。",
                 "Default: false")
        .define("paralysis_spread_allow_chain", false);

PARALYSIS_SPREAD_EXCLUDE_PLAYERS = BUILDER
        .comment("Whether to exclude players from paralysis spread.",
                 "是否排除玩家，使其不会被传染麻痹。",
                 "Default: true")
        .define("paralysis_spread_exclude_players", true);

PARALYSIS_SPREAD_EXCLUDE_PETS = BUILDER
        .comment("Whether to exclude player pets from paralysis spread.",
                 "是否排除玩家的宠物，使其不会被传染麻痹。",
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

    public static boolean staticSplashEnabled;
    public static double staticSplashDamagePercentage;
    public static int staticSplashBaseRange;
    public static int staticSplashRangePerStack;
    public static int staticSplashMaxRange;
    public static boolean staticSplashSkipIfTargetHasStatic;
    public static boolean staticSplashAllowFromSpread;
    public static boolean staticSplashExcludePlayers;
    public static boolean staticSplashExcludePets;
    public static boolean staticSplashTriggerParalysisOnWet;

    public static List<? extends String> cachedStaticImmunityBlacklist;
    public static List<? extends String> cachedParalysisImmunityBlacklist;

    public static double counterLightningDamage;

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

    public static double natureAttackTriggerBaseChance;
    public static int thunderEnhanceThreshold;
    public static double thunderEnhanceChancePerStep;
    public static int natureAttackCooldownTicks;
    public static int staticStacksWhenNoWetness;
    
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
        cachedStaticImmunityBlacklist = STATIC_IMMUNITY_BLACKLIST.get();
        cachedParalysisImmunityBlacklist = PARALYSIS_IMMUNITY_BLACKLIST.get();
        staticDamageMin = STATIC_DAMAGE_MIN.get();
        staticDamageMax = STATIC_DAMAGE_MAX.get();
        staticDamageIntervalTicks = STATIC_DAMAGE_INTERVAL_TICKS.get();

        staticDamageNatureMultiplier = STATIC_DAMAGE_NATURE_MULTIPLIER.get();
        staticDamageFrostMultiplier = STATIC_DAMAGE_FROST_MULTIPLIER.get();

        staticSplashEnabled = STATIC_SPLASH_ENABLED.get();
        staticSplashDamagePercentage = STATIC_SPLASH_DAMAGE_PERCENTAGE.get();
        staticSplashBaseRange = STATIC_SPLASH_BASE_RANGE.get();
        staticSplashRangePerStack = STATIC_SPLASH_RANGE_PER_STACK.get();
        staticSplashMaxRange = STATIC_SPLASH_MAX_RANGE.get();
        staticSplashSkipIfTargetHasStatic = STATIC_SPLASH_SKIP_IF_TARGET_HAS_STATIC.get();
        staticSplashAllowFromSpread = STATIC_SPLASH_ALLOW_FROM_SPREAD.get();
        staticSplashExcludePlayers = STATIC_SPLASH_EXCLUDE_PLAYERS.get();
        staticSplashExcludePets = STATIC_SPLASH_EXCLUDE_PETS.get();
        staticSplashTriggerParalysisOnWet = STATIC_SPLASH_TRIGGER_PARALYSIS_ON_WET.get();

        counterLightningDamage = COUNTER_LIGHTNING_DAMAGE.get();

        natureAttackTriggerBaseChance = NATURE_ATTACK_TRIGGER_BASE_CHANCE.get();
        thunderEnhanceThreshold = THUNDER_ENHANCE_THRESHOLD.get();
        thunderEnhanceChancePerStep = THUNDER_ENHANCE_CHANCE_PER_STEP.get();
        natureAttackCooldownTicks = NATURE_ATTACK_COOLDOWN_TICKS.get();
        staticStacksWhenNoWetness = STATIC_STACKS_WHEN_NO_WETNESS.get();

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