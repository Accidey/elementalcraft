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

    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_FIRE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_NATURE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_THUNDER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_FROST_MULTIPLIER;

    public static final ForgeConfigSpec.IntValue STATIC_AURA_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue STATIC_AURA_BASE_RANGE;
    public static final ForgeConfigSpec.IntValue STATIC_AURA_DAMAGE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.BooleanValue STATIC_AURA_EXCLUDE_FRIENDLY;
    public static final ForgeConfigSpec.BooleanValue STATIC_AURA_ONLY_HOSTILE;

    public static final ForgeConfigSpec.IntValue THUNDER_COUNTER_MIN_SPORE_STACKS;
    public static final ForgeConfigSpec.IntValue NATURE_ATTACK_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue STATIC_STACKS_WHEN_NO_WETNESS;
    public static final ForgeConfigSpec.DoubleValue COUNTER_LIGHTNING_DAMAGE;

    public static final ForgeConfigSpec.IntValue PARALYSIS_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue PARALYSIS_DURATION_PER_STACK_TICKS;
    public static final ForgeConfigSpec.DoubleValue PARALYSIS_DAMAGE_PERCENTAGE;
    public static final ForgeConfigSpec.IntValue PARALYSIS_COOLDOWN_TICKS;

    public static final ForgeConfigSpec.IntValue PARALYSIS_SPREAD_THRESHOLD_STACKS;
    public static final ForgeConfigSpec.IntValue PARALYSIS_SPREAD_BASE_RANGE;
    public static final ForgeConfigSpec.IntValue PARALYSIS_SPREAD_RANGE_PER_EXTRA_STACK;
    public static final ForgeConfigSpec.BooleanValue PARALYSIS_SPREAD_ALLOW_CHAIN;
    public static final ForgeConfigSpec.BooleanValue PARALYSIS_SPREAD_ALLOW_TO_SOURCE;
    public static final ForgeConfigSpec.BooleanValue PARALYSIS_SPREAD_EXCLUDE_FRIENDLY_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue PARALYSIS_SPREAD_ONLY_HOSTILE;

    public static final ForgeConfigSpec.DoubleValue STATIC_SPORE_BLAST_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue STATIC_SPORE_BLAST_PER_STATIC_STACK;
    public static final ForgeConfigSpec.DoubleValue STATIC_SPORE_BLAST_PER_SPORE_STACK;


    public static final ForgeConfigSpec.DoubleValue STATIC_MAX_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue STATIC_MAX_PROJECTILE_PROT_CAP;

    public static final ForgeConfigSpec.BooleanValue STATIC_STEAM_CLOUD_REACTION_ENABLED;
    public static final ForgeConfigSpec.IntValue STATIC_STEAM_CLOUD_TRIGGER_STACKS;

    public static final ForgeConfigSpec.BooleanValue WATER_ELECTRIFICATION_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WATER_ELECTRIFICATION_RANGE_BASE;
    public static final ForgeConfigSpec.DoubleValue WATER_ELECTRIFICATION_RANGE_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue WATER_ELECTRIFICATION_MAX_RANGE;
    public static final ForgeConfigSpec.IntValue WATER_ELECTRIFICATION_PARALYSIS_DURATION;

    public static final ForgeConfigSpec.DoubleValue FROST_STRENGTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_SCALING_STEP;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_SCALING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_WETNESS_BONUS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_STACKING_BONUS_CHANCE;
    public static final ForgeConfigSpec.IntValue FROSTBITE_MAX_STACKS_PER_ATTACK;
    public static final ForgeConfigSpec.IntValue FROSTBITE_MAX_TOTAL_STACKS;
    public static final ForgeConfigSpec.IntValue FROSTBITE_BASE_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue FROSTBITE_DURATION_PER_EXTRA_STACK_TICKS;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_SPEED_REDUCTION_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_PERIODIC_DAMAGE;
    public static final ForgeConfigSpec.IntValue FROSTBITE_DAMAGE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue FROSTBITE_RESIST_IMMUNITY_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FROSTBITE_IMMUNITY_BLACKLIST;

    public static final ForgeConfigSpec.DoubleValue FROSTBITE_NETHER_DURATION_MULTIPLIER;

    public static final ForgeConfigSpec.DoubleValue FROSTBITE_DAMAGE_FIRE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_DAMAGE_NATURE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_DAMAGE_THUNDER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_DAMAGE_FROST_MULTIPLIER;

    public static final ForgeConfigSpec.IntValue FREEZE_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue FREEZE_DURATION_PER_STACK_TICKS;

    public static final ForgeConfigSpec.DoubleValue FREEZE_ELEMENTAL_VULNERABILITY;
    public static final ForgeConfigSpec.DoubleValue FREEZE_SETTLEMENT_DAMAGE_PER_STACK;
    public static final ForgeConfigSpec.IntValue FREEZE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FREEZE_IMMUNITY_BLACKLIST;

    public static final ForgeConfigSpec.IntValue FREEZE_SPREAD_THRESHOLD_STACKS;
    public static final ForgeConfigSpec.IntValue FREEZE_SPREAD_BASE_RANGE;
    public static final ForgeConfigSpec.IntValue FREEZE_SPREAD_RANGE_PER_EXTRA_STACK;
    public static final ForgeConfigSpec.BooleanValue FREEZE_SPREAD_ALLOW_CHAIN;
    public static final ForgeConfigSpec.BooleanValue FREEZE_SPREAD_ALLOW_TO_SOURCE;
    public static final ForgeConfigSpec.BooleanValue FREEZE_SPREAD_EXCLUDE_FRIENDLY_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue FREEZE_SPREAD_ONLY_HOSTILE;

    public static final ForgeConfigSpec.DoubleValue FROSTBITE_THERMAL_SHOCK_BASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_THERMAL_SHOCK_PER_STACK;

    public static final ForgeConfigSpec.DoubleValue FROSTBITE_STEAM_CLOUD_BONUS_CHANCE;

    public static final ForgeConfigSpec.BooleanValue FROSTBITE_CLEAR_SPORES_ENABLED;

    public static final ForgeConfigSpec.BooleanValue FROSTBITE_CLEAR_BY_HEAT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue FROSTBITE_NETHER_CLEAR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_HEAT_SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue FROSTBITE_FIRE_STAND_CLEARING_TIME;

    public static final ForgeConfigSpec.IntValue FROSTBITE_AURA_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_AURA_BASE_RANGE;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_AURA_RANGE_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_AURA_MAX_RANGE;
    public static final ForgeConfigSpec.IntValue FROSTBITE_AURA_DAMAGE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.BooleanValue FROSTBITE_AURA_EXCLUDE_FRIENDLY;
    public static final ForgeConfigSpec.BooleanValue FROSTBITE_AURA_ONLY_HOSTILE;
    public static final ForgeConfigSpec.BooleanValue FROSTBITE_AURA_SCORCHED_STEAM_ENABLED;

    public static final ForgeConfigSpec.BooleanValue FROSTED_STEAM_CLOUD_REACTION_ENABLED;
    public static final ForgeConfigSpec.IntValue FROSTED_STEAM_CLOUD_TRIGGER_STACKS;

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

        BUILDER.comment("");

        STATIC_BASE_CHANCE = BUILDER
                .comment("Base chance to apply Static Shock on attack when the threshold is met.",
                         "达到门槛后，攻击触发静电的基础概率。",
                         "Default: 0.2 (20%)")
                .defineInRange("static_base_chance", 0.2, 0.0, 1.0);

        BUILDER.comment("");

        STATIC_SCALING_STEP = BUILDER
                .comment("Strength step size for increasing the application chance.",
                         "触发概率成长所需的强化点数步长。",
                         "Default: 20")
                .defineInRange("static_scaling_step", 20, 1, 10000);

        BUILDER.comment("");

        STATIC_SCALING_CHANCE = BUILDER
                .comment("Additional chance gained per each scaling step.",
                         "每达到一个步长增加的额外概率。",
                         "Default: 0.1 (10%)")
                .defineInRange("static_scaling_chance", 0.1, 0.0, 1.0);

        BUILDER.comment("");

        STATIC_WETNESS_BONUS_CHANCE_PER_LEVEL = BUILDER
                .comment("Additional chance per level of Wetness effect on the target.",
                         "目标身上每层潮湿效果增加的额外概率。",
                         "Default: 0.05 (5% per level)")
                .defineInRange("static_wetness_bonus_chance_per_level", 0.05, 0.0, 1.0);

        BUILDER.comment("");

        STATIC_STACKING_BONUS_CHANCE = BUILDER
                .comment("Additional chance when target already has Static Shock effect.",
                         "目标已存在静电效果时的额外叠加概率。",
                         "Default: 0.05 (5%)")
                .defineInRange("static_stacking_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment("");

        STATIC_MAX_STACKS_PER_ATTACK = BUILDER
                .comment("Maximum number of Static Shock stacks that can be applied in a single attack.",
                         "单次攻击最多可施加的静电层数。",
                         "Default: 1")
                .defineInRange("static_max_stacks_per_attack", 1, 1, 100);

        BUILDER.comment("");

        STATIC_MAX_TOTAL_STACKS = BUILDER
                .comment("Maximum total stacks of Static Shock a target can have. Once reached, no more stacks can be applied.",
                         "目标身上静电的最大总层数。达到上限后无法继续叠加。",
                         "Default: 5")
                .defineInRange("static_max_total_stacks", 5, 1, 1000);

        BUILDER.comment("");

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

        BUILDER.comment("");

        STATIC_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this blacklist are completely immune to Static Shock effect (cannot be applied).",
                         "处于此黑名单中的实体完全免疫静电效果（无法被施加）。",
                         "Example: [\"minecraft:creeper\", \"minecraft:skeleton\"]")
                .defineListAllowEmpty("static_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.comment("Periodic Damage (Damage over Time)", "周期性伤害（持续伤害）")
                .push("periodic_damage");

        STATIC_DAMAGE_MIN = BUILDER
                .comment("Minimum damage dealt per damage tick (in half-hearts, where 1.0 = 1 heart).",
                         "每次伤害的最小值（以半心为单位，1.0 = 1心）。",
                         "Default: 2.0")
                .defineInRange("static_damage_min", 2.0, 0.0, 10000.0);

        BUILDER.comment("");

        STATIC_DAMAGE_MAX = BUILDER
                .comment("Maximum damage dealt per damage tick (in half-hearts, where 1.0 = 1 heart).",
                         "每次伤害的最大值（以半心为单位，1.0 = 1心）。",
                         "Default: 10.0")
                .defineInRange("static_damage_max", 10.0, 0.0, 10000.0);

        BUILDER.comment("");

        STATIC_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Interval (in ticks) between each Static Shock damage tick. 20 ticks = 1 second.",
                         "每次静电伤害触发的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 100 (5 seconds)")
                .defineInRange("static_damage_interval_ticks", 100, 1, 72000);

        BUILDER.pop();

        BUILDER.comment("Elemental Attribute Modifiers", "元素属性修正")
                .push("elemental_modifiers");

        STATIC_DAMAGE_FIRE_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Fire attribute mobs when taking Static Shock damage.",
                         "赤焰属性生物受到静电伤害时的伤害倍率。",
                         "Default: 1.0")
                .defineInRange("static_damage_fire_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment("");

        STATIC_DAMAGE_NATURE_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Nature attribute mobs when taking Static Shock damage . 0.5 = 50% damage.",
                         "自然属性生物受到静电伤害时的伤害倍率。0.5 = 50%伤害。",
                         "Default: 0.5")
                .defineInRange("static_damage_nature_multiplier", 0.5, 0.0, 10.0);

        BUILDER.comment("");

        STATIC_DAMAGE_THUNDER_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Thunder attribute mobs when taking Static Shock damage.",
                         "雷霆属性生物受到静电伤害时的伤害倍率。",
                         "Default: 1.0")
                .defineInRange("static_damage_thunder_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment("");

        STATIC_DAMAGE_FROST_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Frost attribute mobs when taking Static Shock damage . 1.5 = 150% damage.",
                         "冰霜属性生物受到静电伤害时的伤害倍率。2.0 = 200%伤害。",
                         "Default: 2.0")
                .defineInRange("static_damage_frost_multiplier", 2.0, 0.0, 10.0);

        BUILDER.pop();

        BUILDER.comment("Static Shock Aura (Visual Ring Threshold)",
                        "静电光环（视觉光圈阈值）")
                .push("static_aura");

        STATIC_AURA_THRESHOLD = BUILDER
                .comment("Minimum Static Shock stacks required to activate the Static Shock aura.",
                         "激活静电光环所需的最低静电层数。",
                         "Default: 3")
                .defineInRange("static_aura_threshold", 3, 1, 100);

        BUILDER.comment("");

        STATIC_AURA_BASE_RANGE = BUILDER
                .comment("Range (in blocks) per stack of Static Shock for the aura. Total range = stacks × this value.",
                         "每层静电对应的光环范围（以方块为单位）。总范围 = 层数 × 该值。",
                         "Default: 1.0")
                .defineInRange("static_aura_base_range", 1.0, 0.1, 20.0);

        BUILDER.comment("");

        STATIC_AURA_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Interval (in ticks) between each Static Aura damage tick. 20 ticks = 1 second.",
                         "静电光环伤害触发的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 40 (2 seconds)")
                .defineInRange("static_aura_damage_interval_ticks", 40, 1, 72000);

        BUILDER.comment("");

        STATIC_AURA_EXCLUDE_FRIENDLY = BUILDER
                .comment("If true, players and tamed pets are immune to Static Aura damage.",
                         "如果为 true，玩家和已驯服的宠物免疫静电光环伤害。",
                         "Default: true")
                .define("static_aura_exclude_friendly", true);

        BUILDER.comment("");

        STATIC_AURA_ONLY_HOSTILE = BUILDER
                .comment("If true, only hostile mobs (MobCategory.MONSTER) are affected by Static Aura.",
                         "如果为 true，只有敌对生物会受到静电光环影响，中立/被动生物将被忽略。",
                         "Default: false")
                .define("static_aura_only_hostile", false);

        BUILDER.pop();

        BUILDER.comment("Static Shock + Spores -> Toxic Blast Configuration",
                        "静电+孢子触发毒火爆燃配置")
                .push("static_spore_blast");

        STATIC_SPORE_BLAST_BASE_CHANCE = BUILDER
                .comment("Base chance for Static Shock damage to trigger Toxic Blast on a target with Spores.",
                         "静电伤害触发毒火爆燃的基础概率。",
                         "Default: 0.2 (20%)")
                .defineInRange("base_chance", 0.2, 0.0, 1.0);

        BUILDER.comment("");

        STATIC_SPORE_BLAST_PER_STATIC_STACK = BUILDER
                .comment("Additional chance per stack of Static Shock on the target.",
                         "目标每层静电增加的额外概率。",
                         "Default: 0.05 (5%)")
                .defineInRange("per_static_stack", 0.05, 0.0, 1.0);

        BUILDER.comment("");

        STATIC_SPORE_BLAST_PER_SPORE_STACK = BUILDER
                .comment("Additional chance per stack of Spores on the target.",
                         "目标每层孢子增加的额外概率。",
                         "Default: 0.05 (5%)")
                .defineInRange("per_spore_stack", 0.05, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Static Shock Enchantment Reduction Configuration",
                        "静电附魔减伤配置")
                .push("static_enchantment_reduction");

        STATIC_MAX_PROT_CAP = BUILDER
                .comment("Maximum damage reduction from Protection enchantment against Static Shock damage.",
                         "保护附魔对静电伤害的最大减免比例。",
                         "4 pieces of Protection IV = 16 levels × (this_value / 16) = this_value.",
                         "4件保护IV = 16级 × (该值 / 16) = 该值。",
                         "Default: 0.25 (25%)")
                .defineInRange("static_max_prot_cap", 0.25, 0.0, 1.0);

        BUILDER.comment("");

        STATIC_MAX_PROJECTILE_PROT_CAP = BUILDER
                .comment("Maximum damage reduction from Projectile Protection enchantment against Static Shock damage.",
                         "弹射物保护附魔对静电伤害的最大减免比例。",
                         "4 pieces of Projectile Protection IV = 16 levels × (this_value / 16) = this_value.",
                         "4件弹射物保护IV = 16级 × (该值 / 16) = 该值。",
                         "Default: 0.50 (50%)")
                .defineInRange("static_max_projectile_prot_cap", 0.50, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Thunder Counter Configuration",

                        "雷霆反制配置")
                .push("thunder_counter");

        THUNDER_COUNTER_MIN_SPORE_STACKS = BUILDER
                .comment("Minimum stacks of Flammable Spores required on a Thunder target for a Nature attacker to trigger Thunder Counter.",
                         "雷霆目标需要携带的最小易燃孢子层数，自然属性攻击者才能够触发雷霆反制。",
                         "Default: 2")
                .defineInRange("min_spore_stacks", 2, 1, 100);

        BUILDER.comment("");

        NATURE_ATTACK_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown ticks after the Thunder Counter is triggered on a Nature attacker.",
                         "雷霆反制触发后，自然属性攻击者再次尝试触发雷霆反制的冷却时间（刻）。",
                         "Default: 200 (10 seconds)")
                .defineInRange("cooldown_ticks", 200, 1, 72000);

        BUILDER.comment("");

        STATIC_STACKS_WHEN_NO_WETNESS = BUILDER
                .comment("Static Shock stacks applied to the Nature attacker when they are not under Wetness effect.",
                         "自然属性攻击者没有潮湿效果时，雷霆反制施加的静电层数。",
                         "Default: 2")
                .defineInRange("static_stacks_when_no_wetness", 2, 1, 100);

        BUILDER.comment("");

        COUNTER_LIGHTNING_DAMAGE = BUILDER
                .comment("Damage dealt by the lightning bolt summoned by Thunder Counter.",
                         "雷霆反制召唤的闪电造成的伤害。",
                         "Default: 10.0")
                .defineInRange("lightning_damage", 10.0, 0.0, 100.0);

        BUILDER.pop();

        BUILDER.comment("Paralysis Reaction Configuration",
                        "麻痹反应配置")
                .push("paralysis");

        PARALYSIS_MAX_STACKS = BUILDER
                .comment("Maximum total stacks of Paralysis a target can have.",
                         "目标身上麻痹的最大总层数。",
                         "Default: 5")
                .defineInRange("paralysis_max_stacks", 5, 1, 1000);

        BUILDER.comment("");

        PARALYSIS_DURATION_PER_STACK_TICKS = BUILDER
                .comment("Base duration (in ticks) per stack of Paralysis. 20 ticks = 1 second.",
                         "每层麻痹的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 10 (0.5 seconds)")
                .defineInRange("paralysis_duration_per_stack_ticks", 10, 1, 72000);

        BUILDER.comment("");

        PARALYSIS_DAMAGE_PERCENTAGE = BUILDER
                .comment("Percentage of remaining Static Shock damage dealt when Paralysis is triggered . 0.5 = 50%.",
                         "触发麻痹时，静电剩余伤害的百分比。0.5 = 50%。",
                         "Default: 0.5")
                .defineInRange("paralysis_damage_percentage", 0.5, 0.0, 1.0);

        BUILDER.comment("");

        PARALYSIS_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown ticks before a target can enter the Paralysis flow again after Paralysis ends.\n" +
                         "During cooldown, Wetness and Static Shock effects still work normally. Only when the\n" +
                         "paralysis flow is about to be triggered (target has both Wetness and Static), the cooldown\n" +
                         "is checked. If in cooldown, Wetness and Static are cleared but NO static damage is dealt\n" +
                         "and NO Paralysis is applied.\n" +
                         "目标在麻痹结束后，再次进入麻痹流程所需的冷却时间（刻）。\n" +
                         "冷却期间，潮湿和静电效果正常运作（潮湿减火伤、静电持续伤害）。\n" +
                         "仅在尝试触发麻痹流程时检测冷却，冷却中则清除潮湿和静电效果，\n" +
                         "不结算静电伤害也不施加麻痹状态。",
                         "Default: 200 (10 seconds)")
                .defineInRange("paralysis_cooldown_ticks", 200, 0, 72000);

        BUILDER.pop();

        BUILDER.comment("Paralysis Spread Configuration",
                        "麻痹传染配置")
                .push("paralysis_spread");

        PARALYSIS_SPREAD_THRESHOLD_STACKS = BUILDER
                .comment("Minimum paralysis stacks required for an entity to trigger paralysis spread.",
                         "触发麻痹传染所需的最小麻痹层数。",
                         "Default: 3")
                .defineInRange("paralysis_spread_threshold_stacks", 3, 1, 1000);

        BUILDER.comment("");

        PARALYSIS_SPREAD_BASE_RANGE = BUILDER
                .comment("Base spread range (in blocks) for paralysis from paralyzed entities. 3 = 3x3 area.",
                         "麻痹生物传染麻痹的基础范围（以方块为单位）。3 = 3x3区域。",
                         "Default: 3")
                .defineInRange("paralysis_spread_base_range", 3, 1, 20);

        BUILDER.comment("");

        PARALYSIS_SPREAD_RANGE_PER_EXTRA_STACK = BUILDER
                .comment("Additional spread range per extra paralysis stack beyond threshold.",
                         "超过阈值后，每层额外麻痹增加的传染范围。",
                         "Default: 1")
                .defineInRange("paralysis_spread_range_per_extra_stack", 1, 0, 10);

        BUILDER.comment("");

        PARALYSIS_SPREAD_ALLOW_CHAIN = BUILDER
                .comment("Whether infected entities can further spread paralysis to others.",
                         "被传染的实体是否能够继续传染麻痹给其他生物。",
                         "Default: false")
                .define("paralysis_spread_allow_chain", false);

        BUILDER.comment("");

        PARALYSIS_SPREAD_ALLOW_TO_SOURCE = BUILDER
                .comment("If true, paralysis can spread back to the original source entity.",
                         "如果为 true，麻痹可以传染回原始的父源实体。",
                         "If false, the original source entity will be skipped during contagion.",
                         "如果为 false，传染过程中会跳过父源实体。",
                         "Default: true")
                .define("paralysis_spread_allow_to_source", true);

        BUILDER.comment("");

        PARALYSIS_SPREAD_EXCLUDE_FRIENDLY_ENTITIES = BUILDER
                .comment("If true, players and tamed pets are immune to paralysis spread.",
                         "如果为 true，玩家和已驯服的宠物免疫麻痹传染。",
                         "Default: true")
                .define("paralysis_spread_exclude_friendly_entities", true);

        BUILDER.comment("");

        PARALYSIS_SPREAD_ONLY_HOSTILE = BUILDER
                .comment("If true, paralysis spread only affects hostile mobs (MobCategory.MONSTER).",
                         "如果为 true，麻痹传染仅影响敌对生物，忽略中立/被动生物。",
                         "Default: false")
                .define("paralysis_spread_only_hostile", true);

        BUILDER.comment("");

        PARALYSIS_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this blacklist are completely immune to Paralysis effect (cannot be applied).",
                         "处于此黑名单中的实体完全免疫麻痹效果（无法被施加）。",
                         "Example: [\"minecraft:iron_golem\", \"minecraft:wither\"]")
                .defineListAllowEmpty("paralysis_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.comment("Static Steam Cloud Reaction (Static Shock + Condensing Steam Cloud)",
                        "静电蒸汽云反应（静电+低温蒸汽云）")
                .push("static_steam_cloud");

        STATIC_STEAM_CLOUD_REACTION_ENABLED = BUILDER
                .comment("Global toggle for the Static Steam Cloud reaction.",
                         "When enabled, a static-charged entity entering a condensing steam cloud will trigger Paralysis and electrify the cloud.",
                         "开启/关闭静电蒸汽云反应的全局开关。",
                         "启用后，带静电的实体进入低温蒸汽云时会触发麻痹并将蒸汽云变为感电云。",
                         "Default: true")
                .define("static_steam_cloud_reaction_enabled", true);

        BUILDER.comment("");

        STATIC_STEAM_CLOUD_TRIGGER_STACKS = BUILDER
                .comment("Minimum Static Shock stacks required on an entity to trigger the Static Steam Cloud reaction.",
                         "触发电静蒸汽云反应所需的最低静电层数。",
                         "Default: 3")
                .defineInRange("static_steam_cloud_trigger_stacks", 3, 1, 1000);

        BUILDER.comment("");

        BUILDER.pop();

        BUILDER.comment("Water Electrification (Static Shock + Water)",
                        "感电水域（静电+水体）")
                .push("water_electrification");

        WATER_ELECTRIFICATION_ENABLED = BUILDER
                .comment("Global toggle for water electrification.",
                         "When enabled, a static-shocked entity entering water will electrify the surrounding water,",
                         "paralyzing all entities in the water and dealing static damage to those with static stacks.",
                         "开启/关闭感电水域的全局开关。",
                         "启用后，带静电的实体进入水中会使周围感电水域，",
                         "麻痹水中所有生物，并对带有静电的生物造成静电伤害。",
                         "Default: true")
                .define("water_electrification_enabled", true);

        BUILDER.comment("");

        WATER_ELECTRIFICATION_RANGE_BASE = BUILDER
                .comment("Base range (in blocks) of water electrification.",
                         "感电水域的基础范围（以方块为单位）。",
                         "Default: 3.0")
                .defineInRange("water_electrification_range_base", 3.0, 1.0, 50.0);

        BUILDER.comment("");

        WATER_ELECTRIFICATION_RANGE_PER_STACK = BUILDER
                .comment("Additional range per static stack of the source entity.",
                         "源实体每层静电增加的范围。",
                         "Default: 1.0")
                .defineInRange("water_electrification_range_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment("");

        WATER_ELECTRIFICATION_MAX_RANGE = BUILDER
                .comment("Maximum range of water electrification regardless of stacks.",
                         "感电水域的最大范围限制。",
                         "Default: 16.0")
                .defineInRange("water_electrification_max_range", 16.0, 1.0, 64.0);

        BUILDER.comment("");

        WATER_ELECTRIFICATION_PARALYSIS_DURATION = BUILDER
                .comment("Duration (in ticks) of paralysis applied to entities in electrified water.",
                         "Set to 0 to disable paralysis (damage only). 20 ticks = 1 second.",
                         "感电水域对水中生物施加的麻痹持续时间（以刻为单位）。",
                         "设为0则只造成伤害，不附加麻痹。20刻 = 1秒。",
                         "Default: 100 (5 seconds)")
                .defineInRange("water_electrification_paralysis_duration", 100, 0, 200);

        BUILDER.comment("");

        BUILDER.pop();

        BUILDER.comment("Frostbite (Frost) Reaction Configuration",
                        "霜冻（冰霜）效果配置")
                .push("frostbite");

        BUILDER.comment("Trigger & Stack Rules", "触发与叠加规则")
                .push("trigger_and_stack");

        FROST_STRENGTH_THRESHOLD = BUILDER
                .comment("Minimum Frost Strength points required for the attacker to have a chance to apply Frostbite.",
                         "攻击者触发霜冻效果所需的最低冰霜属性强化点数。",
                         "Default: 20")
                .defineInRange("frost_strength_threshold", 20.0, 1.0, 10000.0);

        BUILDER.comment("");

        FROSTBITE_BASE_CHANCE = BUILDER
                .comment("Base chance to apply Frostbite on attack when the threshold is met.",
                         "达到门槛后，攻击触发霜冻的基础概率。",
                         "Default: 0.2 (20%)")
                .defineInRange("frostbite_base_chance", 0.2, 0.0, 1.0);

        BUILDER.comment("");

        FROSTBITE_SCALING_STEP = BUILDER
                .comment("Strength step size for increasing the application chance.",
                         "触发概率成长所需的强化点数步长。",
                         "Default: 20")
                .defineInRange("frostbite_scaling_step", 20.0, 1.0, 10000.0);

        BUILDER.comment("");

        FROSTBITE_SCALING_CHANCE = BUILDER
                .comment("Additional chance gained per each scaling step.",
                         "每达到一个步长增加的额外概率。",
                         "Default: 0.1 (10%)")
                .defineInRange("frostbite_scaling_chance", 0.1, 0.0, 1.0);

        BUILDER.comment("");

        FROSTBITE_WETNESS_BONUS_CHANCE = BUILDER
                .comment("Additional chance per level of Wetness effect on the target.",
                         "目标身上每层潮湿效果增加的额外概率。",
                         "Default: 0.05 (5% per level)")
                .defineInRange("frostbite_wetness_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment("");

        FROSTBITE_STACKING_BONUS_CHANCE = BUILDER
                .comment("Additional chance when target already has Frostbite effect.",
                         "目标已存在霜冻效果时的额外叠加概率。",
                         "Default: 0.05 (5%)")
                .defineInRange("frostbite_stacking_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment("");

        FROSTBITE_MAX_STACKS_PER_ATTACK = BUILDER
                .comment("Maximum number of Frostbite stacks that can be applied in a single attack.",
                         "单次攻击最多可施加的霜冻层数。",
                         "Default: 1")
                .defineInRange("frostbite_max_stacks_per_attack", 1, 1, 100);

        BUILDER.comment("");

        FROSTBITE_MAX_TOTAL_STACKS = BUILDER
                .comment("Maximum total stacks of Frostbite a target can have.",
                         "目标身上霜冻的最大总层数。",
                         "Default: 5")
                .defineInRange("frostbite_max_total_stacks", 5, 1, 1000);

        BUILDER.comment("");

        FROSTBITE_BASE_DURATION_TICKS = BUILDER
                .comment("Base duration (in ticks) for the first Frostbite stack. 20 ticks = 1 second.",
                         "霜冻首层的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 200 (10 seconds)")
                .defineInRange("frostbite_base_duration_ticks", 200, 1, 72000);

        BUILDER.comment("");

        FROSTBITE_DURATION_PER_EXTRA_STACK_TICKS = BUILDER
                .comment("Additional duration (in ticks) per extra Frostbite stack beyond the first. 20 ticks = 1 second.",
                         "超过首层后，每层霜冻额外增加的持续时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 100 (5 seconds)")
                .defineInRange("frostbite_duration_per_extra_stack_ticks", 100, 1, 72000);

        BUILDER.comment("");

        FROSTBITE_SPEED_REDUCTION_PER_STACK = BUILDER
                .comment("Movement speed reduction per stack of Frostbite. 0.1 = 10% reduction.",
                         "每层霜冻降低的移动速度比例。0.1 = 10%减速。",
                         "Default: 0.1 (10%)")
                .defineInRange("frostbite_speed_reduction_per_stack", 0.1, 0.0, 1.0);

        BUILDER.comment("");

        FROSTBITE_PERIODIC_DAMAGE = BUILDER
                .comment("Damage dealt by Frostbite periodic tick (every frostbite_damage_interval_ticks).",
                         "霜冻周期性伤害的伤害值（每隔 frostbite_damage_interval_ticks 触发一次）。",
                         "Default: 2.0")
                .defineInRange("frostbite_periodic_damage", 2.0, 0.0, 10000.0);

        BUILDER.comment("");

        FROSTBITE_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Interval (in ticks) between each Frostbite periodic damage. 20 ticks = 1 second.",
                         "霜冻周期性伤害的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 100 (5 seconds)")
                .defineInRange("frostbite_damage_interval_ticks", 100, 1, 72000);

        BUILDER.comment("");

        FROSTBITE_RESIST_IMMUNITY_THRESHOLD = BUILDER
                .comment("Frost Resistance points required for an entity to become completely immune to Frostbite.",
                         "实体完全免疫霜冻所需的冰霜抗性点数。",
                         "Default: 80")
                .defineInRange("frostbite_resist_immunity_threshold", 80, 1, 10000);

        BUILDER.comment("");

        FROSTBITE_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this blacklist are completely immune to Frostbite effect.",
                         "处于此黑名单中的实体完全免疫霜冻效果。",
                         "Example: [\"minecraft:blaze\", \"minecraft:magma_cube\"]")
                .defineListAllowEmpty("frostbite_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.comment("");

        FROSTBITE_NETHER_DURATION_MULTIPLIER = BUILDER
                .comment("Duration multiplier for Frostbite in the Nether dimension. 0.5 = 50% duration.",
                         "下界维度中霜冻的持续时间倍率。0.5 = 50%持续时间。",
                         "Default: 0.5")
                .defineInRange("frostbite_nether_duration_multiplier", 0.5, 0.0, 1.0);

        BUILDER.comment("");

        BUILDER.push("damage_multipliers");

        FROSTBITE_DAMAGE_FIRE_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Fire attribute mobs when taking Frostbite periodic/aura damage.",
                         "赤焰属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Default: 1.5")
                .defineInRange("frostbite_damage_fire_multiplier", 1.5, 0.0, 10.0);

        BUILDER.comment("");

        FROSTBITE_DAMAGE_NATURE_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Nature attribute mobs when taking Frostbite periodic/aura damage.",
                         "自然属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Default: 1.0")
                .defineInRange("frostbite_damage_nature_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment("");

        FROSTBITE_DAMAGE_THUNDER_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Thunder attribute mobs when taking Frostbite periodic/aura damage.",
                         "雷霆属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Default: 1.0")
                .defineInRange("frostbite_damage_thunder_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment("");

        FROSTBITE_DAMAGE_FROST_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Frost attribute mobs when taking Frostbite periodic/aura damage.",
                         "冰霜属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Default: 1.0")
                .defineInRange("frostbite_damage_frost_multiplier", 1.0, 0.0, 10.0);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Freeze Reaction Configuration", "冻结反应配置（霜冻+潮湿触发）")
                .push("freeze");

        FREEZE_MAX_STACKS = BUILDER
                .comment("Maximum total stacks of Freeze a target can have.",
                         "目标身上冻结的最大总层数。",
                         "Default: 5")
                .defineInRange("freeze_max_stacks", 5, 1, 1000);

        BUILDER.comment("");

        FREEZE_DURATION_PER_STACK_TICKS = BUILDER
                .comment("Base duration (in ticks) per stack of Freeze. 20 ticks = 1 second.",
                         "每层冻结的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 20 (1 second)")
                .defineInRange("freeze_duration_per_stack_ticks", 20, 1, 72000);

        BUILDER.comment("");

        FREEZE_ELEMENTAL_VULNERABILITY = BUILDER
                .comment("Damage multiplier for elemental damage during Freeze. 1.5 = 150% damage.",
                         "冻结期间受到的元素伤害倍率。1.5 = 150%伤害。",
                         "Default: 1.5")
                .defineInRange("freeze_elemental_vulnerability", 1.5, 0.0, 10.0);

        BUILDER.comment("");

        FREEZE_SETTLEMENT_DAMAGE_PER_STACK = BUILDER
                .comment("Damage multiplier per Frostbite stack when Freeze is triggered (percentage of frostbite_periodic_damage). 1.0 = 100%, 0.5 = 50%, 2.0 = 200%.",
                         "触发冻结时每层霜冻结算的伤害倍率（基于frostbite_periodic_damage的百分比）。1.0 = 100%，0.5 = 50%，2.0 = 200%。",
                         "Default: 1.0")
                .defineInRange("freeze_settlement_damage_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment("");

        FREEZE_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown ticks before a target can be frozen again after Freeze ends.",
                         "冻结结束后再次被冻结所需的冷却时间（刻）。",
                         "Default: 200 (10 seconds)")
                .defineInRange("freeze_cooldown_ticks", 200, 0, 72000);

        BUILDER.comment("");

        FREEZE_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this blacklist are immune to Freeze (but not Frostbite slow).",
                         "处于此黑名单中的实体免疫冻结（但不免疫霜冻减速）。",
                         "Example: [\"minecraft:ender_dragon\", \"minecraft:wither\"]")
                .defineListAllowEmpty("freeze_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.comment("Freeze Spread (Contagion to nearby wet entities)",
                        "冻结传播（传染给附近的潮湿生物）")
                .push("freeze_spread");

        FREEZE_SPREAD_THRESHOLD_STACKS = BUILDER
                .comment("Minimum Freeze stacks required before an entity can spread Freeze to nearby wet entities.",
                         "生物可传播冻结前所需的最低冻结层数。",
                         "Default: 3")
                .defineInRange("freeze_spread_threshold_stacks", 3, 1, 1000);

        BUILDER.comment("");

        FREEZE_SPREAD_BASE_RANGE = BUILDER
                .comment("Base spread range in blocks (radius from source entity).",
                         "基础传播范围（以源实体为中心半径的方块数）。",
                         "Default: 3")
                .defineInRange("freeze_spread_base_range", 3, 1, 32);

        BUILDER.comment("");

        FREEZE_SPREAD_RANGE_PER_EXTRA_STACK = BUILDER
                .comment("Extra range in blocks per Freeze stack beyond the threshold.",
                         "超出阈值后每层冻结额外增加的范围。",
                         "Default: 1")
                .defineInRange("freeze_spread_range_per_extra_stack", 1, 0, 16);

        BUILDER.comment("");

        FREEZE_SPREAD_ALLOW_CHAIN = BUILDER
                .comment("If true, entities infected by spread can further spread Freeze to others.",
                         "启用后，通过传播感染的生物可继续传播冻结。",
                         "Default: false")
                .define("freeze_spread_allow_chain", false);

        BUILDER.comment("");

        FREEZE_SPREAD_ALLOW_TO_SOURCE = BUILDER
                .comment("If true, Freeze can spread back to the original contagion source entity.",
                         "启用后，冻结可回传给原始传染源生物。",
                         "Default: true")
                .define("freeze_spread_allow_to_source", true);

        BUILDER.comment("");

        FREEZE_SPREAD_EXCLUDE_FRIENDLY_ENTITIES = BUILDER
                .comment("If true, players and tamed pets are immune to Freeze spread.",
                         "启用后，玩家和已驯服的宠物免疫冻结传播。",
                         "Default: true")
                .define("freeze_spread_exclude_friendly_entities", true);

        BUILDER.comment("");

        FREEZE_SPREAD_ONLY_HOSTILE = BUILDER
                .comment("If true, only hostile mobs (MobCategory.MONSTER) are affected by Freeze spread.",
                         "启用后，仅敌对生物受冻结传播影响。",
                         "Default: true")
                .define("freeze_spread_only_hostile", true);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Frostbite + Fire (Thermal Shock)",
                        "霜冻+赤焰（热震蒸汽）")
                .push("frostbite_fire");

        FROSTBITE_THERMAL_SHOCK_BASE_DAMAGE = BUILDER
                .comment("Base damage dealt by Thermal Shock when a Fire attack hits a Frozen target.",
                         "火攻击命中冻结目标时热震的基础伤害。",
                         "Default: 5.0")
                .defineInRange("frostbite_thermal_shock_base_damage", 5.0, 0.0, 10000.0);

        BUILDER.comment("");

        FROSTBITE_THERMAL_SHOCK_PER_STACK = BUILDER
                .comment("Additional Thermal Shock damage per remaining Frostbite stack.",
                         "每层剩余霜冻层数增加的热震伤害。",
                         "Default: 2.0")
                .defineInRange("frostbite_thermal_shock_per_stack", 2.0, 0.0, 10000.0);

        BUILDER.pop();

        BUILDER.comment("Frostbite + Steam Cloud",
                        "霜冻+蒸汽云")
                .push("frostbite_steam");

        FROSTBITE_STEAM_CLOUD_BONUS_CHANCE = BUILDER
                .comment("Additional Frostbite application chance when target is in a condensing steam cloud.",
                         "目标处于冷凝蒸汽云中时霜冻施加的额外概率。",
                         "Default: 0.15 (15%)")
                .defineInRange("frostbite_steam_cloud_bonus_chance", 0.15, 0.0, 1.0);

        BUILDER.comment("");

        BUILDER.pop();

        BUILDER.comment("Frostbite + Flammable Spores (Frost Kills Spores)",
                        "霜冻+易燃孢子（冻死孢子）")
                .push("frostbite_spores");

        FROSTBITE_CLEAR_SPORES_ENABLED = BUILDER
                .comment("When enabled, applying Frostbite to a target with Flammable Spores will remove the Spores effect. The cold kills the spores.",
                         "启用后，对带有易燃孢子的目标施加霜冻效果时，会清除孢子效果——低温冻死了易燃孢子。",
                         "Default: true")
                .define("frostbite_clear_spores_enabled", true);

        BUILDER.comment("");

        BUILDER.pop();

        BUILDER.comment("Frostbite Heat Clearing (Heat Sources Remove Frostbite)",
                        "霜冻热源清除（热源清除霜冻效果）")
                .push("frostbite_heat_clearing");

        FROSTBITE_CLEAR_BY_HEAT_ENABLED = BUILDER
                .comment("Master toggle: when enabled, heat sources (fire, lava, magma blocks, Nether) will remove Frostbite.",
                         "总开关：启用后，热源（火焰、熔岩、岩浆块、下界）会清除霜冻效果。",
                         "Default: true")
                .define("frostbite_clear_by_heat_enabled", true);

        BUILDER.comment("");

        FROSTBITE_NETHER_CLEAR_ENABLED = BUILDER
                .comment("When enabled, Frostbite is cleared immediately upon entering the Nether dimension.",
                         "启用后，进入下界维度时霜冻效果会被立即清除。",
                         "Default: true")
                .define("frostbite_nether_clear_enabled", true);

        BUILDER.comment("");

        FROSTBITE_HEAT_SEARCH_RADIUS = BUILDER
                .comment("Radius (blocks) to search for nearby heat sources (Lava/Magma) that clear Frostbite. Magma Block detection radius is reduced by 1.",
                         "检测周围热源（熔岩/岩浆块）清除霜冻的半径范围（格）。岩浆块的检测半径会减少1格。",
                         "Default: 2.0")
                .defineInRange("frostbite_heat_search_radius", 2.0, 1.0, 16.0);

        BUILDER.comment("");

        FROSTBITE_FIRE_STAND_CLEARING_TIME = BUILDER
                .comment("Seconds required to stand on a fire block (Fire or Soul Fire) to clear all Frostbite.",
                         "站在火中（普通火或灵魂火）清除所有霜冻效果所需的秒数。",
                         "Default: 2")
                .defineInRange("frostbite_fire_stand_clearing_time", 2, 1, 600);

        BUILDER.comment("");

        BUILDER.pop();

        BUILDER.comment("Frosted Steam Cloud Reaction (Frostbite + Condensing Steam Cloud)",
                        "霜寒蒸汽云反应（霜寒+低温蒸汽云）")
                .push("frosted_steam_cloud");

        FROSTED_STEAM_CLOUD_REACTION_ENABLED = BUILDER
                .comment("Global toggle for the Frosted Steam Cloud reaction.",
                         "When enabled, a frostbitten entity (3+ stacks) entering a condensing steam cloud will frost the cloud and freeze entities inside.",
                         "开启/关闭霜寒蒸汽云反应的全局开关。",
                         "启用后，霜寒层数达标的实体进入低温蒸汽云时会使蒸汽云结霜，持续冻结云中生物。",
                         "Default: true")
                .define("frosted_steam_cloud_reaction_enabled", true);

        FROSTED_STEAM_CLOUD_TRIGGER_STACKS = BUILDER
                .comment("Minimum Frostbite stacks required on an entity to trigger the Frosted Steam Cloud reaction.",
                         "触发霜寒蒸汽云反应所需的最低霜寒层数。",
                         "Default: 3")
                .defineInRange("frosted_steam_cloud_trigger_stacks", 3, 1, 1000);

        BUILDER.pop();

        BUILDER.comment("Frostbite Aura (AoE Frost Damage)",
                        "霜冻光环（范围冰霜伤害）")
                .push("frostbite_aura");

        FROSTBITE_AURA_THRESHOLD = BUILDER
                .comment("Minimum Frostbite stacks required to activate the Frostbite Aura.",
                         "激活霜冻光环所需的最低霜冻层数。",
                         "Default: 3")
                .defineInRange("frostbite_aura_threshold", 3, 1, 100);

        BUILDER.comment("");

        FROSTBITE_AURA_BASE_RANGE = BUILDER
                .comment("Base radius (in blocks) of the Frostbite Aura when stacks equal the threshold.",
                         "霜冻层数等于阈值时的基础光环半径（以方块为单位）。",
                         "Default: 3.0")
                .defineInRange("frostbite_aura_base_range", 3.0, 1.0, 50.0);

        BUILDER.comment("");

        FROSTBITE_AURA_RANGE_PER_STACK = BUILDER
                .comment("Additional radius (in blocks) per extra Frostbite stack beyond the threshold.",
                         "超过阈值后，每层霜冻增加的光环范围（以方块为单位）。",
                         "Default: 1.0")
                .defineInRange("frostbite_aura_range_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment("");

        FROSTBITE_AURA_MAX_RANGE = BUILDER
                .comment("Maximum radius (in blocks) of the Frostbite Aura regardless of stacks.",
                         "霜冻光环的最大范围限制（以方块为单位）。",
                         "Default: 8.0")
                .defineInRange("frostbite_aura_max_range", 8.0, 1.0, 50.0);

        BUILDER.comment("");

        FROSTBITE_AURA_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Interval (in ticks) between each Frostbite Aura damage tick. 20 ticks = 1 second.",
                         "霜冻光环伤害触发的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Default: 40 (2 seconds)")
                .defineInRange("frostbite_aura_damage_interval_ticks", 40, 1, 72000);

        BUILDER.comment("");

        FROSTBITE_AURA_EXCLUDE_FRIENDLY = BUILDER
                .comment("If true, players and tamed pets are immune to Frostbite Aura damage.",
                         "如果为 true，玩家和已驯服的宠物免疫霜冻光环伤害。",
                         "Default: true")
                .define("frostbite_aura_exclude_friendly", true);

        BUILDER.comment("");

        FROSTBITE_AURA_ONLY_HOSTILE = BUILDER
                .comment("If true, only hostile mobs (MobCategory.MONSTER) are affected by Frostbite Aura.",
                         "如果为 true，只有敌对生物会受到霜冻光环影响，中立/被动生物将被忽略。",
                         "Default: false")
                .define("frostbite_aura_only_hostile", false);

        BUILDER.comment("");

        FROSTBITE_AURA_SCORCHED_STEAM_ENABLED = BUILDER
                .comment("霜冻效果的目标走进灼烧光环范围时，是否清除霜冻并触发高温蒸汽云？",
                         "Whether to clear Frostbite and trigger High-Heat Steam Cloud when a frostbitten entity enters Scorched Aura range.",
                         "Default: true")
                .define("frostbite_aura_scorched_steam_enabled", true);

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
    public static double staticDamageFireMultiplier;
    public static double staticDamageNatureMultiplier;
    public static double staticDamageThunderMultiplier;
    public static double staticDamageFrostMultiplier;

    public static int staticAuraThreshold;
    public static double staticAuraBaseRange;
    public static int staticAuraDamageIntervalTicks;
    public static boolean staticAuraExcludeFriendly;
    public static boolean staticAuraOnlyHostile;

    public static int thunderCounterMinSporeStacks;
    public static int natureAttackCooldownTicks;
    public static int staticStacksWhenNoWetness;
    public static double counterLightningDamage;

    public static List<? extends String> cachedStaticImmunityBlacklist;
    public static List<? extends String> cachedParalysisImmunityBlacklist;

    public static int paralysisMaxStacks;
    public static int paralysisDurationPerStackTicks;
    public static double paralysisDamagePercentage;
    public static int paralysisCooldownTicks;

    public static int paralysisSpreadThresholdStacks;
    public static int paralysisSpreadBaseRange;
    public static int paralysisSpreadRangePerExtraStack;
    public static boolean paralysisSpreadAllowChain;
    public static boolean paralysisSpreadAllowToSource;
    public static boolean paralysisSpreadExcludeFriendlyEntities;
    public static boolean paralysisSpreadOnlyHostile;

    public static double staticSporeBlastBaseChance;
    public static double staticSporeBlastPerStaticStack;
    public static double staticSporeBlastPerSporeStack;


    public static double staticMaxProtCap;
    public static double staticMaxProjectileProtCap;

    public static double frostStrengthThreshold;
    public static double frostbiteBaseChance;
    public static double frostbiteScalingStep;
    public static double frostbiteScalingChance;
    public static double frostbiteWetnessBonusChance;
    public static double frostbiteStackingBonusChance;
    public static int frostbiteMaxStacksPerAttack;
    public static int frostbiteMaxTotalStacks;
    public static int frostbiteBaseDurationTicks;
    public static int frostbiteDurationPerExtraStackTicks;
    public static double frostbiteSpeedReductionPerStack;
    public static double frostbitePeriodicDamage;
    public static int frostbiteDamageIntervalTicks;
    public static int frostbiteResistImmunityThreshold;
    public static List<? extends String> cachedFrostbiteImmunityBlacklist;

    public static double frostbiteNetherDurationMultiplier;

    public static double frostbiteDamageFireMultiplier;
    public static double frostbiteDamageNatureMultiplier;
    public static double frostbiteDamageThunderMultiplier;
    public static double frostbiteDamageFrostMultiplier;

    public static int freezeMaxStacks;
    public static int freezeDurationPerStackTicks;

    public static double freezeElementalVulnerability;
    public static double freezeSettlementDamagePerStack;
    public static int freezeCooldownTicks;
    public static List<? extends String> cachedFreezeImmunityBlacklist;

    public static int freezeSpreadThresholdStacks;
    public static int freezeSpreadBaseRange;
    public static int freezeSpreadRangePerExtraStack;
    public static boolean freezeSpreadAllowChain;
    public static boolean freezeSpreadAllowToSource;
    public static boolean freezeSpreadExcludeFriendlyEntities;
    public static boolean freezeSpreadOnlyHostile;

    public static double frostbiteThermalShockBaseDamage;
    public static double frostbiteThermalShockPerStack;

    public static double frostbiteSteamCloudBonusChance;

    public static boolean frostbiteClearSporesEnabled;
    public static boolean frostbiteClearByHeatEnabled;
    public static boolean frostbiteNetherClearEnabled;
    public static double frostbiteHeatSearchRadius;
    public static int frostbiteFireStandClearingTime;

    public static boolean staticSteamCloudReactionEnabled;
    public static int staticSteamCloudTriggerStacks;

    public static boolean waterElectrificationEnabled;
    public static double waterElectrificationRangeBase;
    public static double waterElectrificationRangePerStack;
    public static double waterElectrificationMaxRange;
    public static int waterElectrificationParalysisDuration;

    public static int frostbiteAuraThreshold;
    public static double frostbiteAuraBaseRange;
    public static double frostbiteAuraRangePerStack;
    public static double frostbiteAuraMaxRange;
    public static int frostbiteAuraDamageIntervalTicks;
    public static boolean frostbiteAuraExcludeFriendly;
    public static boolean frostbiteAuraOnlyHostile;
    public static boolean frostbiteAuraScorchedSteamEnabled;

    public static boolean frostedSteamCloudReactionEnabled;
    public static int frostedSteamCloudTriggerStacks;

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

        staticDamageFireMultiplier = STATIC_DAMAGE_FIRE_MULTIPLIER.get();
        staticDamageNatureMultiplier = STATIC_DAMAGE_NATURE_MULTIPLIER.get();
        staticDamageThunderMultiplier = STATIC_DAMAGE_THUNDER_MULTIPLIER.get();
        staticDamageFrostMultiplier = STATIC_DAMAGE_FROST_MULTIPLIER.get();

        staticAuraThreshold = STATIC_AURA_THRESHOLD.get();
        staticAuraBaseRange = STATIC_AURA_BASE_RANGE.get();
        staticAuraDamageIntervalTicks = STATIC_AURA_DAMAGE_INTERVAL_TICKS.get();
        staticAuraExcludeFriendly = STATIC_AURA_EXCLUDE_FRIENDLY.get();
        staticAuraOnlyHostile = STATIC_AURA_ONLY_HOSTILE.get();

        thunderCounterMinSporeStacks = THUNDER_COUNTER_MIN_SPORE_STACKS.get();
        natureAttackCooldownTicks = NATURE_ATTACK_COOLDOWN_TICKS.get();
        staticStacksWhenNoWetness = STATIC_STACKS_WHEN_NO_WETNESS.get();
        counterLightningDamage = COUNTER_LIGHTNING_DAMAGE.get();

        paralysisMaxStacks = PARALYSIS_MAX_STACKS.get();
        paralysisDurationPerStackTicks = PARALYSIS_DURATION_PER_STACK_TICKS.get();
        paralysisDamagePercentage = PARALYSIS_DAMAGE_PERCENTAGE.get();
        paralysisCooldownTicks = PARALYSIS_COOLDOWN_TICKS.get();

        paralysisSpreadThresholdStacks = PARALYSIS_SPREAD_THRESHOLD_STACKS.get();
        paralysisSpreadBaseRange = PARALYSIS_SPREAD_BASE_RANGE.get();
        paralysisSpreadRangePerExtraStack = PARALYSIS_SPREAD_RANGE_PER_EXTRA_STACK.get();
        paralysisSpreadAllowChain = PARALYSIS_SPREAD_ALLOW_CHAIN.get();
        paralysisSpreadAllowToSource = PARALYSIS_SPREAD_ALLOW_TO_SOURCE.get();
        paralysisSpreadExcludeFriendlyEntities = PARALYSIS_SPREAD_EXCLUDE_FRIENDLY_ENTITIES.get();
        paralysisSpreadOnlyHostile = PARALYSIS_SPREAD_ONLY_HOSTILE.get();

        staticSporeBlastBaseChance = STATIC_SPORE_BLAST_BASE_CHANCE.get();
        staticSporeBlastPerStaticStack = STATIC_SPORE_BLAST_PER_STATIC_STACK.get();
        staticSporeBlastPerSporeStack = STATIC_SPORE_BLAST_PER_SPORE_STACK.get();

        staticMaxProtCap = STATIC_MAX_PROT_CAP.get();
        staticMaxProjectileProtCap = STATIC_MAX_PROJECTILE_PROT_CAP.get();

        frostStrengthThreshold = FROST_STRENGTH_THRESHOLD.get();
        frostbiteBaseChance = FROSTBITE_BASE_CHANCE.get();
        frostbiteScalingStep = FROSTBITE_SCALING_STEP.get();
        frostbiteScalingChance = FROSTBITE_SCALING_CHANCE.get();
        frostbiteWetnessBonusChance = FROSTBITE_WETNESS_BONUS_CHANCE.get();
        frostbiteStackingBonusChance = FROSTBITE_STACKING_BONUS_CHANCE.get();
        frostbiteMaxStacksPerAttack = FROSTBITE_MAX_STACKS_PER_ATTACK.get();
        frostbiteMaxTotalStacks = FROSTBITE_MAX_TOTAL_STACKS.get();
        frostbiteBaseDurationTicks = FROSTBITE_BASE_DURATION_TICKS.get();
        frostbiteDurationPerExtraStackTicks = FROSTBITE_DURATION_PER_EXTRA_STACK_TICKS.get();
        frostbiteSpeedReductionPerStack = FROSTBITE_SPEED_REDUCTION_PER_STACK.get();
        frostbitePeriodicDamage = FROSTBITE_PERIODIC_DAMAGE.get();
        frostbiteDamageIntervalTicks = FROSTBITE_DAMAGE_INTERVAL_TICKS.get();
        frostbiteResistImmunityThreshold = FROSTBITE_RESIST_IMMUNITY_THRESHOLD.get();
        cachedFrostbiteImmunityBlacklist = FROSTBITE_IMMUNITY_BLACKLIST.get();

        frostbiteNetherDurationMultiplier = FROSTBITE_NETHER_DURATION_MULTIPLIER.get();

        frostbiteDamageFireMultiplier = FROSTBITE_DAMAGE_FIRE_MULTIPLIER.get();
        frostbiteDamageNatureMultiplier = FROSTBITE_DAMAGE_NATURE_MULTIPLIER.get();
        frostbiteDamageThunderMultiplier = FROSTBITE_DAMAGE_THUNDER_MULTIPLIER.get();
        frostbiteDamageFrostMultiplier = FROSTBITE_DAMAGE_FROST_MULTIPLIER.get();

        freezeMaxStacks = FREEZE_MAX_STACKS.get();
        freezeDurationPerStackTicks = FREEZE_DURATION_PER_STACK_TICKS.get();

        freezeElementalVulnerability = FREEZE_ELEMENTAL_VULNERABILITY.get();
        freezeSettlementDamagePerStack = FREEZE_SETTLEMENT_DAMAGE_PER_STACK.get();
        freezeCooldownTicks = FREEZE_COOLDOWN_TICKS.get();
        cachedFreezeImmunityBlacklist = FREEZE_IMMUNITY_BLACKLIST.get();

        freezeSpreadThresholdStacks = FREEZE_SPREAD_THRESHOLD_STACKS.get();
        freezeSpreadBaseRange = FREEZE_SPREAD_BASE_RANGE.get();
        freezeSpreadRangePerExtraStack = FREEZE_SPREAD_RANGE_PER_EXTRA_STACK.get();
        freezeSpreadAllowChain = FREEZE_SPREAD_ALLOW_CHAIN.get();
        freezeSpreadAllowToSource = FREEZE_SPREAD_ALLOW_TO_SOURCE.get();
        freezeSpreadExcludeFriendlyEntities = FREEZE_SPREAD_EXCLUDE_FRIENDLY_ENTITIES.get();
        freezeSpreadOnlyHostile = FREEZE_SPREAD_ONLY_HOSTILE.get();

        frostbiteThermalShockBaseDamage = FROSTBITE_THERMAL_SHOCK_BASE_DAMAGE.get();
        frostbiteThermalShockPerStack = FROSTBITE_THERMAL_SHOCK_PER_STACK.get();

        frostbiteSteamCloudBonusChance = FROSTBITE_STEAM_CLOUD_BONUS_CHANCE.get();

        frostbiteClearSporesEnabled = FROSTBITE_CLEAR_SPORES_ENABLED.get();
        frostbiteClearByHeatEnabled = FROSTBITE_CLEAR_BY_HEAT_ENABLED.get();
        frostbiteNetherClearEnabled = FROSTBITE_NETHER_CLEAR_ENABLED.get();
        frostbiteHeatSearchRadius = FROSTBITE_HEAT_SEARCH_RADIUS.get();
        frostbiteFireStandClearingTime = FROSTBITE_FIRE_STAND_CLEARING_TIME.get();

        staticSteamCloudReactionEnabled = STATIC_STEAM_CLOUD_REACTION_ENABLED.get();
        staticSteamCloudTriggerStacks = STATIC_STEAM_CLOUD_TRIGGER_STACKS.get();

        waterElectrificationEnabled = WATER_ELECTRIFICATION_ENABLED.get();
        waterElectrificationRangeBase = WATER_ELECTRIFICATION_RANGE_BASE.get();
        waterElectrificationRangePerStack = WATER_ELECTRIFICATION_RANGE_PER_STACK.get();
        waterElectrificationMaxRange = WATER_ELECTRIFICATION_MAX_RANGE.get();
        waterElectrificationParalysisDuration = WATER_ELECTRIFICATION_PARALYSIS_DURATION.get();

        frostbiteAuraThreshold = FROSTBITE_AURA_THRESHOLD.get();
        frostbiteAuraBaseRange = FROSTBITE_AURA_BASE_RANGE.get();
        frostbiteAuraRangePerStack = FROSTBITE_AURA_RANGE_PER_STACK.get();
        frostbiteAuraMaxRange = FROSTBITE_AURA_MAX_RANGE.get();
        frostbiteAuraDamageIntervalTicks = FROSTBITE_AURA_DAMAGE_INTERVAL_TICKS.get();
        frostbiteAuraExcludeFriendly = FROSTBITE_AURA_EXCLUDE_FRIENDLY.get();
        frostbiteAuraOnlyHostile = FROSTBITE_AURA_ONLY_HOSTILE.get();
        frostbiteAuraScorchedSteamEnabled = FROSTBITE_AURA_SCORCHED_STEAM_ENABLED.get();

        frostedSteamCloudReactionEnabled = FROSTED_STEAM_CLOUD_REACTION_ENABLED.get();
        frostedSteamCloudTriggerStacks = FROSTED_STEAM_CLOUD_TRIGGER_STACKS.get();

    }

    private ElementalThunderFrostReactionsConfig() {}
}
