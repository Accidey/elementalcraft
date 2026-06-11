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
    public static final ForgeConfigSpec.DoubleValue STATIC_AURA_HEIGHT_CEILING;

    public static final ForgeConfigSpec.IntValue THUNDER_COUNTER_MIN_SPORE_STACKS;
    public static final ForgeConfigSpec.IntValue NATURE_ATTACK_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue STATIC_STACKS_WHEN_NO_WETNESS;
    public static final ForgeConfigSpec.DoubleValue COUNTER_LIGHTNING_DAMAGE;

    public static final ForgeConfigSpec.IntValue PARALYSIS_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue PARALYSIS_DURATION_PER_STACK_TICKS;
    public static final ForgeConfigSpec.DoubleValue PARALYSIS_DAMAGE_PERCENTAGE;
    public static final ForgeConfigSpec.IntValue PARALYSIS_COOLDOWN_TICKS;

    public static final ForgeConfigSpec.DoubleValue STATIC_SPORE_BLAST_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue STATIC_SPORE_BLAST_PER_STATIC_STACK;
    public static final ForgeConfigSpec.DoubleValue STATIC_SPORE_BLAST_PER_SPORE_STACK;
    public static final ForgeConfigSpec.DoubleValue STATIC_CREEPER_IGNITE_CHANCE;


    public static final ForgeConfigSpec.DoubleValue STATIC_MAX_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue STATIC_MAX_PROJECTILE_PROT_CAP;

    public static final ForgeConfigSpec.IntValue STATIC_STEAM_CLOUD_TRIGGER_STACKS;

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

    public static final ForgeConfigSpec.DoubleValue THUNDER_BREAK_FREEZE_CHANCE;
    public static final ForgeConfigSpec.IntValue THUNDER_BREAK_FREEZE_WETNESS_LAYERS;

    public static final ForgeConfigSpec.IntValue FROSTBITE_FIRE_STEAM_THRESHOLD;

    public static final ForgeConfigSpec.IntValue FIRE_FROST_MELT_BASE_THRESHOLD;
    public static final ForgeConfigSpec.IntValue FIRE_FROST_MELT_ADDITIONAL_COST;
    public static final ForgeConfigSpec.DoubleValue FIRE_FROST_MELT_DAMAGE_MULT;

    public static final ForgeConfigSpec.BooleanValue FROSTBITE_REDUCE_SPORES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_SPORE_DECAY_SPEED;

    public static final ForgeConfigSpec.BooleanValue FREEZE_CLEAR_SPORES_ENABLED;

    public static final ForgeConfigSpec.BooleanValue FROST_SCORCH_STEAM_REACTION_ENABLED;

    public static final ForgeConfigSpec.BooleanValue SCORCHED_FROSTBITE_TO_WETNESS_ENABLED;
    public static final ForgeConfigSpec.IntValue SCORCHED_FROSTBITE_TO_WETNESS_RATIO;

    public static final ForgeConfigSpec.BooleanValue FROSTBITE_CLEAR_BY_HEAT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue FROSTBITE_NETHER_CLEAR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_NETHER_DURATION_MULTIPLIER;
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

    public static final ForgeConfigSpec.IntValue FROSTED_STEAM_CLOUD_TRIGGER_STACKS;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Static Shock (Thunder) Reaction Configuration",
                        "静电（雷霆）效果配置")
                .push("static_shock");

        BUILDER.comment("Trigger & Stack Rules", "触发与叠加规则")
                .push("trigger_and_stack");

        THUNDER_STRENGTH_THRESHOLD = BUILDER
                .comment("攻击者触发静电效果所需的最低雷霆属性强化点数，设为0则关闭静电效果。",
                         "Minimum Thunder Strength points required to apply Static Shock. Set to 0 to disable Static Shock.",
                         "Default: 20")
                .defineInRange("thunder_strength_threshold", 20, 0, 10000);

        BUILDER.comment(" ");

        STATIC_BASE_CHANCE = BUILDER
                .comment("达到门槛后，攻击触发静电的基础概率。",
                         "Base chance to apply Static Shock on attack when the threshold is met.",
                         "Default: 0.3 (30%)")
                .defineInRange("static_base_chance", 0.3, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_SCALING_STEP = BUILDER
                .comment("触发概率成长所需的强化点数步长。",
                         "Strength step size for increasing the application chance.",
                         "Default: 20")
                .defineInRange("static_scaling_step", 20, 1, 10000);

        BUILDER.comment(" ");

        STATIC_SCALING_CHANCE = BUILDER
                .comment("每达到一个步长增加的额外概率。",
                         "Additional chance gained per each scaling step.",
                         "Default: 0.1 (10%)")
                .defineInRange("static_scaling_chance", 0.1, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_WETNESS_BONUS_CHANCE_PER_LEVEL = BUILDER
                .comment("目标身上每层潮湿效果增加的额外概率。",
                         "Additional chance per level of Wetness effect on the target.",
                         "Default: 0.05 (5% per level)")
                .defineInRange("static_wetness_bonus_chance_per_level", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_STACKING_BONUS_CHANCE = BUILDER
                .comment("目标已存在静电效果时的额外叠加概率。",
                         "Additional chance when target already has Static Shock effect.",
                         "Default: 0.05 (5%)")
                .defineInRange("static_stacking_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_MAX_STACKS_PER_ATTACK = BUILDER
                .comment("单次攻击最多可施加的静电层数。",
                         "Maximum number of Static Shock stacks that can be applied in a single attack.",
                         "Default: 1")
                .defineInRange("static_max_stacks_per_attack", 1, 1, 100);

        BUILDER.comment(" ");

        STATIC_MAX_TOTAL_STACKS = BUILDER
                .comment("目标身上静电的最大总层数。达到上限后无法继续叠加。",
                         "Maximum total stacks of Static Shock a target can have. Once reached, no more stacks can be applied.",
                         "Default: 5")
                .defineInRange("static_max_total_stacks", 5, 1, 1000);

        BUILDER.comment(" ");

        STATIC_DURATION_PER_STACK_TICKS = BUILDER
                .comment("每层静电的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Base duration (in ticks) per stack of Static Shock. 20 ticks = 1 second.",
                         "Default: 100 (5 seconds)")
                .defineInRange("static_duration_per_stack_ticks", 100, 1, 72000);

        BUILDER.pop();

        BUILDER.comment("Immunity Rule", "免疫规则")
                .push("immunity");

        STATIC_RESIST_IMMUNITY_THRESHOLD = BUILDER
                .comment("实体完全免疫静电（叠加和伤害）所需的雷霆抗性点数。",
                         "Thunder Resistance points required for an entity to become completely immune to Static Shock (both stacking and damage).",
                         "Default: 80")
                .defineInRange("static_resist_immunity_threshold", 80, 1, 10000);

        BUILDER.comment(" ");

        STATIC_IMMUNITY_BLACKLIST = BUILDER
                .comment("处于此黑名单中的实体完全免疫静电效果（无法被施加）。",
                         "Entities in this blacklist are completely immune to Static Shock effect (cannot be applied).",
                         "Example: [\"minecraft:creeper\", \"minecraft:skeleton\"]")
                .defineListAllowEmpty("static_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.comment("Periodic Damage (Damage over Time)", "周期性伤害（持续伤害）")
                .push("periodic_damage");

        STATIC_DAMAGE_MIN = BUILDER
                .comment("每次伤害的最小值（以半心为单位，1.0 = 1心）。",
                         "Minimum damage dealt per damage tick (in half-hearts, where 1.0 = 1 heart).",
                         "Default: 2.0")
                .defineInRange("static_damage_min", 2.0, 0.0, 10000.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_MAX = BUILDER
                .comment("每次伤害的最大值（以半心为单位，1.0 = 1心）。",
                         "Maximum damage dealt per damage tick (in half-hearts, where 1.0 = 1 heart).",
                         "Default: 10.0")
                .defineInRange("static_damage_max", 10.0, 0.0, 10000.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("每次静电伤害触发的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Interval (in ticks) between each Static Shock damage tick. 20 ticks = 1 second.",
                         "Default: 100 (5 seconds)")
                .defineInRange("static_damage_interval_ticks", 100, 1, 72000);

        BUILDER.pop();

        BUILDER.comment("Elemental Attribute Modifiers", "元素属性修正")
                .push("elemental_modifiers");

        STATIC_DAMAGE_FIRE_MULTIPLIER = BUILDER
                .comment("赤焰属性生物受到静电伤害时的伤害倍率。",
                         "Damage multiplier for Fire attribute mobs when taking Static Shock damage.",
                         "Default: 1.0")
                .defineInRange("static_damage_fire_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_NATURE_MULTIPLIER = BUILDER
                .comment("自然属性生物受到静电伤害时的伤害倍率。0.5 = 50%伤害。",
                         "Damage multiplier for Nature attribute mobs when taking Static Shock damage. 0.5 = 50% damage.",
                         "Default: 0.5")
                .defineInRange("static_damage_nature_multiplier", 0.5, 0.0, 10.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_THUNDER_MULTIPLIER = BUILDER
                .comment("雷霆属性生物受到静电伤害时的伤害倍率。",
                         "Damage multiplier for Thunder attribute mobs when taking Static Shock damage.",
                         "Default: 1.0")
                .defineInRange("static_damage_thunder_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_FROST_MULTIPLIER = BUILDER
                .comment("冰霜属性生物受到静电伤害时的伤害倍率。2.0 = 200%伤害。",
                         "Damage multiplier for Frost attribute mobs when taking Static Shock damage. 1.5 = 150% damage.",
                         "Default: 2.0")
                .defineInRange("static_damage_frost_multiplier", 2.0, 0.0, 10.0);

        BUILDER.pop();

        BUILDER.comment("Static Shock Aura (Visual Ring Threshold)",
                        "静电光环（视觉光圈阈值）")
                .push("static_aura");

        STATIC_AURA_THRESHOLD = BUILDER
                .comment("激活静电光环所需的最低静电层数，设为0则关闭静电光环。",
                         "Minimum Static Shock stacks required to activate the Static Shock aura. Set to 0 to disable.",
                         "Default: 3")
                .defineInRange("static_aura_threshold", 3, 0, 100);

        BUILDER.comment(" ");

        STATIC_AURA_BASE_RANGE = BUILDER
                .comment("每层静电对应的光环范围（以方块为单位）。总范围 = 层数 × 该值。",
                         "Range (in blocks) per stack of Static Shock for the aura. Total range = stacks × this value.",
                         "Default: 1.0")
                .defineInRange("static_aura_base_range", 1.0, 0.1, 20.0);

        BUILDER.comment(" ");

        STATIC_AURA_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("静电光环伤害触发的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Interval (in ticks) between each Static Aura damage tick. 20 ticks = 1 second.",
                         "Default: 40 (2 seconds)")
                .defineInRange("static_aura_damage_interval_ticks", 40, 1, 72000);

        BUILDER.comment(" ");

        STATIC_AURA_EXCLUDE_FRIENDLY = BUILDER
                .comment("如果为 true，玩家和已驯服的宠物免疫静电光环伤害。",
                         "If true, players and tamed pets are immune to Static Aura damage.",
                         "Default: true")
                .define("static_aura_exclude_friendly", true);

        BUILDER.comment(" ");

        STATIC_AURA_ONLY_HOSTILE = BUILDER
                .comment("如果为 true，只有敌对生物会受到静电光环影响，中立/被动生物将被忽略。",
                         "If true, only hostile mobs (MobCategory.MONSTER) are affected by Static Aura.",
                         "Default: false")
                .define("static_aura_only_hostile", false);

        BUILDER.comment(" ");

        STATIC_AURA_HEIGHT_CEILING = BUILDER
                .comment("静电光环可影响生物的最大高度差（格）。超过此 Y 偏移量的目标免疫光环伤害和效果。",
                         "Maximum height difference (blocks) for the Static Aura to affect entities above the source. Entities above this Y offset are immune to aura damage and effects.",
                         "Default: 3.0")
                .defineInRange("static_aura_height_ceiling", 3.0, 0.0, 64.0);

        BUILDER.pop();

        BUILDER.comment("Static Shock + Spores -> Toxic Blast Configuration",
                        "静电+孢子触发毒火爆燃配置")
                .push("static_spore_blast");

        STATIC_SPORE_BLAST_BASE_CHANCE = BUILDER
                .comment("静电+孢子触发毒火爆燃的基础概率，设为0则关闭。",
                         "Base chance for Static Shock + Spores to trigger Toxic Blast. Set to 0 to disable.",
                         "Default: 0.30 (30%)")
                .defineInRange("base_chance", 0.30, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_SPORE_BLAST_PER_STATIC_STACK = BUILDER
                .comment("目标每层静电增加的额外概率。",
                         "Additional chance per stack of Static Shock on the target.",
                         "Default: 0.10 (10%)")
                .defineInRange("per_static_stack", 0.10, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_SPORE_BLAST_PER_SPORE_STACK = BUILDER
                .comment("目标每层孢子增加的额外概率。",
                         "Additional chance per stack of Spores on the target.",
                         "Default: 0.05 (5%)")
                .defineInRange("per_spore_stack", 0.05, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Static Shock Creeper Ignite (Converts to Charged Creeper then Ignites)",
                        "静电苦力怕变高压+引爆")
                .push("static_creeper_ignite");

        STATIC_CREEPER_IGNITE_CHANCE = BUILDER
                .comment("静电击中的苦力怕变高压并引爆的概率。设为0则关闭此反应。",
                         "Chance for Static Shock to prime (->charged) and ignite a Creeper. Set to 0 to disable.",
                         "Default: 0.4 (40%)")
                .defineInRange("chance", 0.4, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Static Shock Enchantment Reduction Configuration",
                        "静电附魔减伤配置")
                .push("static_enchantment_reduction");

        STATIC_MAX_PROT_CAP = BUILDER
                .comment("保护附魔对静电伤害的最大减免比例。",
                         "4件保护IV = 16级 × (该值 / 16) = 该值。",
                         "Maximum damage reduction from Protection enchantment against Static Shock damage.",
                         "4 pieces of Protection IV = 16 levels × (this_value / 16) = this_value.",
                         "Default: 0.25 (25%)")
                .defineInRange("static_max_prot_cap", 0.25, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_MAX_PROJECTILE_PROT_CAP = BUILDER
                .comment("弹射物保护附魔对静电伤害的最大减免比例。",
                         "4件弹射物保护IV = 16级 × (该值 / 16) = 该值。",
                         "Maximum damage reduction from Projectile Protection enchantment against Static Shock damage.",
                         "4 pieces of Projectile Protection IV = 16 levels × (this_value / 16) = this_value.",
                         "Default: 0.50 (50%)")
                .defineInRange("static_max_projectile_prot_cap", 0.50, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Thunder Counter Configuration",

                        "雷霆反制配置")
                .push("thunder_counter");

        THUNDER_COUNTER_MIN_SPORE_STACKS = BUILDER
                .comment("雷霆目标需要携带的最小易燃孢子层数，自然属性攻击者才能够触发雷霆反制。设为0则关闭雷霆反制。",
                         "Minimum Spore stacks on Thunder target for Nature attacker to trigger Thunder Counter. Set to 0 to disable.",
                         "Default: 2")
                .defineInRange("min_spore_stacks", 2, 0, 100);

        BUILDER.comment(" ");

        NATURE_ATTACK_COOLDOWN_TICKS = BUILDER
                .comment("雷霆反制触发后，自然属性攻击者再次尝试触发雷霆反制的冷却时间（刻）。",
                         "Cooldown ticks after the Thunder Counter is triggered on a Nature attacker.",
                         "Default: 200 (10 seconds)")
                .defineInRange("cooldown_ticks", 200, 1, 72000);

        BUILDER.comment(" ");

        STATIC_STACKS_WHEN_NO_WETNESS = BUILDER
                .comment("自然属性攻击者没有潮湿效果时，雷霆反制施加的静电层数。",
                         "Static Shock stacks applied to the Nature attacker when they are not under Wetness effect.",
                         "Default: 2")
                .defineInRange("static_stacks_when_no_wetness", 2, 1, 100);

        BUILDER.comment(" ");

        COUNTER_LIGHTNING_DAMAGE = BUILDER
                .comment("雷霆反制召唤的闪电造成的伤害。",
                         "Damage dealt by the lightning bolt summoned by Thunder Counter.",
                         "Default: 10.0")
                .defineInRange("lightning_damage", 10.0, 0.0, 100.0);

        BUILDER.pop();

        BUILDER.comment("Paralysis Reaction Configuration",
                        "麻痹反应配置")
                .push("paralysis");

        PARALYSIS_MAX_STACKS = BUILDER
                .comment("目标身上麻痹的最大总层数，设为0则不会触发任何麻痹效果。",
                         "Maximum total stacks of Paralysis a target can have. Set to 0 to disable all Paralysis effects.",
                         "Default: 5")
                .defineInRange("paralysis_max_stacks", 5, 0, 1000);

        BUILDER.comment(" ");

        PARALYSIS_DURATION_PER_STACK_TICKS = BUILDER
                .comment("每层麻痹的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Base duration (in ticks) per stack of Paralysis. 20 ticks = 1 second.",
                         "Default: 40 (2 seconds)")
                .defineInRange("paralysis_duration_per_stack_ticks", 40, 1, 72000);

        BUILDER.comment(" ");

        PARALYSIS_DAMAGE_PERCENTAGE = BUILDER
                .comment("触发麻痹时，静电剩余伤害的百分比。0.5 = 50%。",
                         "Percentage of remaining Static Shock damage dealt when Paralysis is triggered. 0.5 = 50%.",
                         "Default: 0.5")
                .defineInRange("paralysis_damage_percentage", 0.5, 0.0, 1.0);

        BUILDER.comment(" ");

        PARALYSIS_COOLDOWN_TICKS = BUILDER
                .comment("目标在麻痹结束后，再次进入麻痹流程所需的冷却时间（刻）。\n" +
                         "冷却期间，潮湿和静电效果正常运作（潮湿减火伤、静电持续伤害）。\n" +
                         "仅在尝试触发麻痹流程时检测冷却，冷却中则清除潮湿和静电效果，\n" +
                         "不结算静电伤害也不施加麻痹状态。",
                         "Cooldown ticks before a target can enter the Paralysis flow again after Paralysis ends.\n" +
                         "During cooldown, Wetness and Static Shock effects still work normally. Only when the\n" +
                         "paralysis flow is about to be triggered (target has both Wetness and Static), the cooldown\n" +
                         "is checked. If in cooldown, Wetness and Static are cleared but NO static damage is dealt\n" +
                         "and NO Paralysis is applied.",
                         "Default: 200 (10 seconds)")
                .defineInRange("paralysis_cooldown_ticks", 200, 0, 72000);

        BUILDER.pop();

        PARALYSIS_IMMUNITY_BLACKLIST = BUILDER
                .comment("处于此黑名单中的实体完全免疫麻痹效果（无法被施加）。",
                         "Entities in this blacklist are completely immune to Paralysis effect (cannot be applied).",
                         "Example: [\"minecraft:iron_golem\", \"minecraft:wither\"]")
                .defineListAllowEmpty("paralysis_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.comment("Static Steam Cloud Reaction (Static Shock + Condensing Steam Cloud)",
                        "静电蒸汽云反应（静电+低温蒸汽云）")
                .push("static_steam_cloud");

        STATIC_STEAM_CLOUD_TRIGGER_STACKS = BUILDER
                .comment("带静电的实体进入低温蒸汽云时触发麻痹和感电化所需的最低静电层数，设为0则关闭静电蒸汽云反应。",
                         "Minimum Static stacks to trigger the Static Steam Cloud reaction. Set to 0 to disable.",
                         "Default: 3")
                .defineInRange("static_steam_cloud_trigger_stacks", 3, 0, 1000);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("Water Electrification (Static Shock + Water)",
                        "感电水域（静电+水体）")
                .push("water_electrification");

        WATER_ELECTRIFICATION_RANGE_BASE = BUILDER
                .comment("用于触发电解麻痹和静电伤害的水中感电范围，设为0则关闭感电水域。",
                         "Base range (in blocks) of water electrification. Set to 0 to disable Water Electrification.",
                         "Default: 3.0")
                .defineInRange("water_electrification_range_base", 3.0, 0.0, 50.0);

        BUILDER.comment(" ");

        WATER_ELECTRIFICATION_RANGE_PER_STACK = BUILDER
                .comment("源实体每层静电增加的范围。",
                         "Additional range per static stack of the source entity.",
                         "Default: 1.0")
                .defineInRange("water_electrification_range_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        WATER_ELECTRIFICATION_MAX_RANGE = BUILDER
                .comment("感电水域的最大范围限制。",
                         "Maximum range of water electrification regardless of stacks.",
                         "Default: 16.0")
                .defineInRange("water_electrification_max_range", 16.0, 1.0, 64.0);

        BUILDER.comment(" ");

        WATER_ELECTRIFICATION_PARALYSIS_DURATION = BUILDER
                .comment("感电水域对水中生物施加的麻痹持续时间（以刻为单位）。",
                         "设为0则只造成伤害，不附加麻痹。20刻 = 1秒。",
                         "Duration (in ticks) of paralysis applied to entities in electrified water.",
                         "Set to 0 to disable paralysis (damage only). 20 ticks = 1 second.",
                         "Default: 100 (5 seconds)")
                .defineInRange("water_electrification_paralysis_duration", 100, 0, 200);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("Frostbite (Frost) Reaction Configuration",
                        "霜冻（冰霜）效果配置")
                .push("frostbite");

        BUILDER.comment("Trigger & Stack Rules", "触发与叠加规则")
                .push("trigger_and_stack");

        FROST_STRENGTH_THRESHOLD = BUILDER
                .comment("冰霜属性攻击给予霜冻效果所需的冰霜强化值阈值，设为0则关闭。",
                         "Minimum Frost Strength required to apply Frostbite. Set to 0 to disable Frostbite application.",
                         "Default: 20")
                .defineInRange("frost_strength_threshold", 20.0, 0.0, 10000.0);

        BUILDER.comment(" ");

        FROSTBITE_BASE_CHANCE = BUILDER
                .comment("达到门槛后，攻击触发霜冻的基础概率。",
                         "Base chance to apply Frostbite on attack when the threshold is met.",
                         "Default: 0.2 (20%)")
                .defineInRange("frostbite_base_chance", 0.2, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_SCALING_STEP = BUILDER
                .comment("触发概率成长所需的强化点数步长。",
                         "Strength step size for increasing the application chance.",
                         "Default: 20")
                .defineInRange("frostbite_scaling_step", 20.0, 1.0, 10000.0);

        BUILDER.comment(" ");

        FROSTBITE_SCALING_CHANCE = BUILDER
                .comment("每达到一个步长增加的额外概率。",
                         "Additional chance gained per each scaling step.",
                         "Default: 0.1 (10%)")
                .defineInRange("frostbite_scaling_chance", 0.1, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_WETNESS_BONUS_CHANCE = BUILDER
                .comment("目标身上每层潮湿效果增加的额外概率。",
                         "Additional chance per level of Wetness effect on the target.",
                         "Default: 0.05 (5% per level)")
                .defineInRange("frostbite_wetness_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_STACKING_BONUS_CHANCE = BUILDER
                .comment("目标已存在霜冻效果时的额外叠加概率。",
                         "Additional chance when target already has Frostbite effect.",
                         "Default: 0.05 (5%)")
                .defineInRange("frostbite_stacking_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_MAX_STACKS_PER_ATTACK = BUILDER
                .comment("单次攻击最多可施加的霜冻层数。",
                         "Maximum number of Frostbite stacks that can be applied in a single attack.",
                         "Default: 1")
                .defineInRange("frostbite_max_stacks_per_attack", 1, 1, 100);

        BUILDER.comment(" ");

        FROSTBITE_MAX_TOTAL_STACKS = BUILDER
                .comment("目标身上霜冻的最大总层数。",
                         "Maximum total stacks of Frostbite a target can have.",
                         "Default: 5")
                .defineInRange("frostbite_max_total_stacks", 5, 1, 1000);

        BUILDER.comment(" ");

        FROSTBITE_BASE_DURATION_TICKS = BUILDER
                .comment("霜冻首层的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Base duration (in ticks) for the first Frostbite stack. 20 ticks = 1 second.",
                         "Default: 200 (10 seconds)")
                .defineInRange("frostbite_base_duration_ticks", 200, 1, 72000);

        BUILDER.comment(" ");

        FROSTBITE_DURATION_PER_EXTRA_STACK_TICKS = BUILDER
                .comment("超过首层后，每层霜冻额外增加的持续时间（以刻为单位）。20刻 = 1秒。",
                         "Additional duration (in ticks) per extra Frostbite stack beyond the first. 20 ticks = 1 second.",
                         "Default: 100 (5 seconds)")
                .defineInRange("frostbite_duration_per_extra_stack_ticks", 100, 1, 72000);

        BUILDER.comment(" ");

        FROSTBITE_SPEED_REDUCTION_PER_STACK = BUILDER
                .comment("每层霜冻降低的移动速度和攻击速度比例。0.1 = 10%减速。0 = 关闭。上限90%。",
                         "Movement speed and attack speed reduction per stack of Frostbite. 0.1 = 10% reduction. 0 = disable. Max 90%.",
                         "Default: 0.1 (10%)")
                .defineInRange("frostbite_speed_reduction_per_stack", 0.1, 0.0, 0.9);

        BUILDER.comment(" ");

        FROSTBITE_PERIODIC_DAMAGE = BUILDER
                .comment("霜冻周期性伤害的伤害值（每隔 frostbite_damage_interval_ticks 触发一次）。",
                         "Damage dealt by Frostbite periodic tick (every frostbite_damage_interval_ticks).",
                         "Default: 2.0")
                .defineInRange("frostbite_periodic_damage", 2.0, 0.0, 10000.0);

        BUILDER.comment(" ");

        FROSTBITE_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("霜冻周期性伤害的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Interval (in ticks) between each Frostbite periodic damage. 20 ticks = 1 second.",
                         "Default: 100 (5 seconds)")
                .defineInRange("frostbite_damage_interval_ticks", 100, 1, 72000);

        BUILDER.comment(" ");

        FROSTBITE_RESIST_IMMUNITY_THRESHOLD = BUILDER
                .comment("实体完全免疫霜冻所需的冰霜抗性点数。",
                         "Frost Resistance points required for an entity to become completely immune to Frostbite.",
                         "Default: 80")
                .defineInRange("frostbite_resist_immunity_threshold", 80, 1, 10000);

        BUILDER.comment(" ");

        FROSTBITE_IMMUNITY_BLACKLIST = BUILDER
                .comment("处于此黑名单中的实体完全免疫霜冻效果。",
                         "Entities in this blacklist are completely immune to Frostbite effect.",
                         "Example: [\"minecraft:blaze\", \"minecraft:magma_cube\"]")
                .defineListAllowEmpty("frostbite_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.comment(" ");


        BUILDER.push("damage_multipliers");

        FROSTBITE_DAMAGE_FIRE_MULTIPLIER = BUILDER
                .comment("赤焰属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Damage multiplier for Fire attribute mobs when taking Frostbite periodic/aura damage.",
                         "Default: 1.5")
                .defineInRange("frostbite_damage_fire_multiplier", 1.5, 0.0, 10.0);

        BUILDER.comment(" ");

        FROSTBITE_DAMAGE_NATURE_MULTIPLIER = BUILDER
                .comment("自然属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Damage multiplier for Nature attribute mobs when taking Frostbite periodic/aura damage.",
                         "Default: 1.0")
                .defineInRange("frostbite_damage_nature_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        FROSTBITE_DAMAGE_THUNDER_MULTIPLIER = BUILDER
                .comment("雷霆属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Damage multiplier for Thunder attribute mobs when taking Frostbite periodic/aura damage.",
                         "Default: 1.0")
                .defineInRange("frostbite_damage_thunder_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        FROSTBITE_DAMAGE_FROST_MULTIPLIER = BUILDER
                .comment("冰霜属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Damage multiplier for Frost attribute mobs when taking Frostbite periodic/aura damage.",
                         "Default: 1.0")
                .defineInRange("frostbite_damage_frost_multiplier", 1.0, 0.0, 10.0);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Freeze Reaction Configuration", "冻结反应配置（霜冻+潮湿触发）")
                .push("freeze");

        FREEZE_MAX_STACKS = BUILDER
                .comment("目标身上冻结的最大总层数，设为0则关闭冻结反应。",
                         "Maximum total stacks of Freeze a target can have. Set to 0 to disable Freeze reactions.",
                         "Default: 5")
                .defineInRange("freeze_max_stacks", 5, 0, 1000);

        BUILDER.comment(" ");

        FREEZE_DURATION_PER_STACK_TICKS = BUILDER
                .comment("每层冻结的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Base duration (in ticks) per stack of Freeze. 20 ticks = 1 second.",
                         "Default: 20 (1 second)")
                .defineInRange("freeze_duration_per_stack_ticks", 20, 1, 72000);

        BUILDER.comment(" ");

        FREEZE_ELEMENTAL_VULNERABILITY = BUILDER
                .comment("霜冻(Frostbite)期间受到的元素伤害倍率。1.5 = 150%伤害。",
                         "Damage multiplier for elemental damage while Frostbitten. 1.5 = 150% damage.",
                         "Default: 1.5")
                .defineInRange("freeze_elemental_vulnerability", 1.5, 0.0, 10.0);

        BUILDER.comment(" ");

        FREEZE_SETTLEMENT_DAMAGE_PER_STACK = BUILDER
                .comment("触发冻结时每层霜冻结算的伤害倍率（基于frostbite_periodic_damage的百分比）。1.0 = 100%，0.5 = 50%，2.0 = 200%。",
                         "Damage multiplier per Frostbite stack when Freeze is triggered (percentage of frostbite_periodic_damage). 1.0 = 100%, 0.5 = 50%, 2.0 = 200%.",
                         "Default: 1.0")
                .defineInRange("freeze_settlement_damage_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        FREEZE_COOLDOWN_TICKS = BUILDER
                .comment("冻结结束后再次被冻结所需的冷却时间（刻）。",
                         "Cooldown ticks before a target can be frozen again after Freeze ends.",
                         "Default: 200 (10 seconds)")
                .defineInRange("freeze_cooldown_ticks", 200, 0, 72000);

        BUILDER.comment(" ");

        FREEZE_IMMUNITY_BLACKLIST = BUILDER
                .comment("处于此黑名单中的实体免疫冻结（但不免疫霜冻减速）。",
                         "Entities in this blacklist are immune to Freeze (but not Frostbite slow).",
                         "Example: [\"minecraft:ender_dragon\", \"minecraft:wither\"]")
                .defineListAllowEmpty("freeze_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.comment(" ");

        THUNDER_BREAK_FREEZE_CHANCE = BUILDER
                .comment("每层静电解除冰冻的概率。总概率 = 层数 × 此值。0 = 关闭。",
                         "Chance per static stack to break Freeze. Total chance = stacks × this value. 0 = disabled.",
                         "Default: 0.05 (5% per stack)")
                .defineInRange("thunder_break_freeze_chance", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        THUNDER_BREAK_FREEZE_WETNESS_LAYERS = BUILDER
                .comment("静电解冻后赋予的潮湿层数。0 = 关闭此功能。",
                         "Wetness layers to apply after breaking Freeze via Static Shock. 0 = disabled.",
                         "Default: 2")
                .defineInRange("thunder_break_freeze_wetness_layers", 2, 0, 100);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("Frozen + Fire (Fire melts Frozen to Wetness)",
                        "冻结+赤焰（赤焰融冰→潮湿）")
                .push("frostbite_fire_steam");

        FROSTBITE_FIRE_STEAM_THRESHOLD = BUILDER
                .comment("赤焰属性攻击冰冻目标解除冻结并转化为潮湿所需的最低赤焰强化（固定值，不随冻结层数变化），设为0则关闭此反应。",
                         "Min Fire enhancement to break Freeze and convert to Wetness (flat threshold, no per-stack scaling). Set to 0 to disable.",
                         "Default: 50")
                .defineInRange("frostbite_fire_steam_threshold", 50, 0, 10000);

        BUILDER.pop();

        BUILDER.comment("Fire + Frostbite -> Steam (Fire clears Frostbite, spawns High-Heat Steam)",
                        "赤焰+霜冻->蒸汽（赤焰清除霜冻，生成高温蒸汽云）")
                .push("fire_frost_melt");

        FIRE_FROST_MELT_BASE_THRESHOLD = BUILDER
                .comment("赤焰攻击霜冻目标触发高温蒸汽反应所需的基础赤焰强化点数（首层门槛），设为0则关闭此反应。",
                         "每多一层霜冻需额外增加 fire_frost_melt_additional_cost 点。",
                         "Base Fire Enhancement points to trigger High-Heat Steam when attacking a Frostbitten target (first layer threshold). Set to 0 to disable.",
                         "Each additional Frostbite layer requires fire_frost_melt_additional_cost more points.",
                         "Default: 20")
                .defineInRange("fire_frost_melt_base_threshold", 20, 0, 10000);

        BUILDER.comment(" ");

        FIRE_FROST_MELT_ADDITIONAL_COST = BUILDER
                .comment("超过首层后，每多一层霜冻额外需要的赤焰属性强化点数。",
                         "例如：20基础+10×(层数-1)，3层霜冻需要40点赤焰强化。",
                         "Additional Fire Enhancement points needed per extra Frostbite layer beyond the first.",
                         "Example: 20 base + 10×(layers-1), 3 Frostbite layers require 40 Fire points.",
                         "Default: 10")
                .defineInRange("fire_frost_melt_additional_cost", 10, 1, 10000);

        BUILDER.comment(" ");

        FIRE_FROST_MELT_DAMAGE_MULT = BUILDER
                .comment("赤焰攻击冰冻(Frozen)目标触发融冰时，属性伤害的倍率。",
                         "0.5 = 原本属性伤害的50%（降低50%）。",
                         "Damage multiplier when Fire attacks a Frozen target and triggers melt.",
                         "0.5 = 50% of original elemental damage (50% reduction).",
                         "Default: 0.5")
                .defineInRange("fire_frost_melt_damage_mult", 0.5, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Frostbite + Flammable Spores (Frost Reduces Spore Duration)",
                        "霜冻+易燃孢子（霜冻削减孢子持续时间）")
                .push("frostbite_spores");

        FROSTBITE_REDUCE_SPORES_ENABLED = BUILDER
                .comment("启用后，对带有易燃孢子的目标施加霜冻效果时，会削减孢子效果的剩余持续时间。",
                         "When enabled, applying Frostbite to a target with Flammable Spores will reduce the remaining duration of the Spores effect.",
                         "Default: true")
                .define("frostbite_reduce_spores_enabled", true);

        BUILDER.comment(" ");

        FROSTBITE_SPORE_DECAY_SPEED = BUILDER
                .comment("霜冻和易燃孢子同时存在时，孢子剩余时间的流逝速度倍率。2 = 2倍速流逝，1 = 正常速度。",
                         "When Frostbite and Flammable Spores coexist, the Spores duration ticks down at this speed multiplier. 2 = 2x speed (half duration), 1 = normal speed.",
                         "Default: 10.0")
                .defineInRange("frostbite_spore_decay_speed", 10.0, 1.0, 100.0);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("Freeze + Flammable Spores (Freeze Clears Spores)",
                        "冻结+易燃孢子（冻结清除孢子）")
                .push("freeze_spores");

        FREEZE_CLEAR_SPORES_ENABLED = BUILDER
                .comment("启用后，冻结状态的生物无法获得易燃孢子效果，已有孢子会在进入冻结时被清除。",
                         "When enabled, frozen entities cannot gain Flammable Spores. Existing spores are cleared on freeze.",
                         "Default: true")
                .define("freeze_clear_spores_enabled", true);

        BUILDER.pop();

        BUILDER.comment("Frozen-Scorch Steam Reaction (Scorched + Frozen triggers Low-Heat Steam)",
                        "灼烧-冻结蒸汽反应（灼烧与冻结同时存在时触发低温蒸汽云）")
                .push("frost_scorch_steam_reaction");

        FROST_SCORCH_STEAM_REACTION_ENABLED = BUILDER
                .comment("启用后，灼烧与冻结(Frozen)同时存在时触发低温蒸汽云，解除冻结并清除霜冻和灼烧。",
                         "When enabled, Scorched + Frozen on the same entity triggers a Low-Heat Steam Cloud, unfreezing and clearing both effects.",
                         "Steam level = sourceFirePower / step + frozenStacks",
                         "Default: true")
                .define("frost_scorch_steam_reaction_enabled", true);

        BUILDER.pop();

        BUILDER.comment("Scorched-Frostbite to Wetness Conversion (Scorched + Frostbite converts to Wetness)",
                        "灼烧-霜冻转潮湿（灼烧与霜冻共存时，霜冻转化为潮湿并清除灼烧）")
                .push("scorched_frostbite_to_wetness");

        SCORCHED_FROSTBITE_TO_WETNESS_ENABLED = BUILDER
                .comment("启用后，灼烧与霜冻(Frostbite，未冻结)同时存在时，将霜冻层数转化为潮湿层数并清除灼烧。",
                         "When enabled, Scorched + Frostbite (not Frozen) on the same entity converts Frostbite stacks to Wetness and clears Scorched.",
                         "Default: true")
                .define("scorched_frostbite_to_wetness_enabled", true);

        BUILDER.comment(" ");

        SCORCHED_FROSTBITE_TO_WETNESS_RATIO = BUILDER
                .comment("霜冻转化为潮湿的层数比例。1 = 1层霜冻转1层潮湿，2 = 1层霜冻转2层潮湿。",
                         "Ratio of Frostbite stacks converted to Wetness. 1 = 1 Frostbite → 1 Wetness, 2 = 1 Frostbite → 2 Wetness.",
                         "0 = disabled (same as enabled=false)",
                         "Default: 1")
                .defineInRange("scorched_frostbite_to_wetness_ratio", 1, 0, 10);

        BUILDER.pop();

        BUILDER.comment("Frostbite Heat Clearing (Heat Sources Remove Frostbite)",
                        "霜冻热源清除（热源清除霜冻效果）")
                .push("frostbite_heat_clearing");

        FROSTBITE_CLEAR_BY_HEAT_ENABLED = BUILDER
                .comment("总开关：启用后，热源（火焰、熔岩、岩浆块、下界）会清除霜冻效果。",
                         "Master toggle: when enabled, heat sources (fire, lava, magma blocks, Nether) will remove Frostbite.",
                         "Default: true")
                .define("frostbite_clear_by_heat_enabled", true);

        BUILDER.comment(" ");

        FROSTBITE_NETHER_CLEAR_ENABLED = BUILDER
                .comment("启用后，进入下界维度时霜冻效果会被立即清除。",
                         "When enabled, Frostbite is cleared immediately upon entering the Nether dimension.",
                         "Default: true")
                .define("frostbite_nether_clear_enabled", true);

        BUILDER.comment(" ");
        FROSTBITE_NETHER_DURATION_MULTIPLIER = BUILDER
                .comment("下界维度中霜冻的持续时间倍率。0.5 = 50%持续时间。",
                         "Duration multiplier for Frostbite in the Nether dimension. 0.5 = 50% duration.",
                         "Default: 0.5")
                .defineInRange("frostbite_nether_duration_multiplier", 0.5, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_HEAT_SEARCH_RADIUS = BUILDER
                .comment("检测周围热源（熔岩/岩浆块）清除霜冻的半径范围（格）。岩浆块的检测半径会减少1格。",
                         "Radius (blocks) to search for nearby heat sources (Lava/Magma) that clear Frostbite. Magma Block detection radius is reduced by 1.",
                         "Default: 2.0")
                .defineInRange("frostbite_heat_search_radius", 2.0, 1.0, 16.0);

        BUILDER.comment(" ");

        FROSTBITE_FIRE_STAND_CLEARING_TIME = BUILDER
                .comment("站在火中（普通火或灵魂火）清除所有霜冻效果所需的秒数。",
                         "Seconds required to stand on a fire block (Fire or Soul Fire) to clear all Frostbite.",
                         "Default: 2")
                .defineInRange("frostbite_fire_stand_clearing_time", 2, 1, 600);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("Frosted Steam Cloud Reaction (Frostbite + Condensing Steam Cloud)",
                        "霜寒蒸汽云反应（霜寒+低温蒸汽云）")
                .push("frosted_steam_cloud");

        FROSTED_STEAM_CLOUD_TRIGGER_STACKS = BUILDER
                .comment("触发霜寒蒸汽云反应所需的最低霜冻层数，设为0则关闭霜寒蒸汽云反应。",
                         "Minimum Frostbite stacks to trigger the Frosted Steam Cloud reaction. Set to 0 to disable.",
                         "Default: 3")
                .defineInRange("frosted_steam_cloud_trigger_stacks", 3, 0, 1000);

        BUILDER.pop();

        BUILDER.comment("Frostbite Aura (AoE Frost Damage)",
                        "霜冻光环（范围冰霜伤害）")
                .push("frostbite_aura");

        FROSTBITE_AURA_THRESHOLD = BUILDER
                .comment("激活霜冻光环所需的最低霜冻层数，设为0则关闭霜冻光环。",
                         "Minimum Frostbite stacks required to activate the Frostbite Aura. Set to 0 to disable.",
                         "Default: 3")
                .defineInRange("frostbite_aura_threshold", 3, 0, 100);

        BUILDER.comment(" ");

        FROSTBITE_AURA_BASE_RANGE = BUILDER
                .comment("霜冻层数等于阈值时的基础光环半径（以方块为单位）。",
                         "Base radius (in blocks) of the Frostbite Aura when stacks equal the threshold.",
                         "Default: 3.0")
                .defineInRange("frostbite_aura_base_range", 3.0, 1.0, 50.0);

        BUILDER.comment(" ");

        FROSTBITE_AURA_RANGE_PER_STACK = BUILDER
                .comment("超过阈值后，每层霜冻增加的光环范围（以方块为单位）。",
                         "Additional radius (in blocks) per extra Frostbite stack beyond the threshold.",
                         "Default: 1.0")
                .defineInRange("frostbite_aura_range_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        FROSTBITE_AURA_MAX_RANGE = BUILDER
                .comment("霜冻光环的最大范围限制（以方块为单位）。",
                         "Maximum radius (in blocks) of the Frostbite Aura regardless of stacks.",
                         "Default: 8.0")
                .defineInRange("frostbite_aura_max_range", 8.0, 1.0, 50.0);

        BUILDER.comment(" ");

        FROSTBITE_AURA_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("霜冻光环伤害触发的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Interval (in ticks) between each Frostbite Aura damage tick. 20 ticks = 1 second.",
                         "Default: 40 (2 seconds)")
                .defineInRange("frostbite_aura_damage_interval_ticks", 40, 1, 72000);

        BUILDER.comment(" ");

        FROSTBITE_AURA_EXCLUDE_FRIENDLY = BUILDER
                .comment("如果为 true，玩家和已驯服的宠物免疫霜冻光环伤害。",
                         "If true, players and tamed pets are immune to Frostbite Aura damage.",
                         "Default: true")
                .define("frostbite_aura_exclude_friendly", true);

        BUILDER.comment(" ");

        FROSTBITE_AURA_ONLY_HOSTILE = BUILDER
                .comment("如果为 true，只有敌对生物会受到霜冻光环影响，中立/被动生物将被忽略。",
                         "If true, only hostile mobs (MobCategory.MONSTER) are affected by Frostbite Aura.",
                         "Default: false")
                .define("frostbite_aura_only_hostile", false);

        BUILDER.comment(" ");

        FROSTBITE_AURA_SCORCHED_STEAM_ENABLED = BUILDER
                .comment("冻结(Frozen)状态的目标走进灼烧光环范围时，是否解除冻结并触发高温蒸汽云？",
                         "Whether to unfreeze and trigger High-Heat Steam Cloud when a frozen entity enters Scorched Aura range.",
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
    public static double staticAuraHeightCeiling;

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



    public static double staticSporeBlastBaseChance;
    public static double staticSporeBlastPerStaticStack;
    public static double staticSporeBlastPerSporeStack;
    public static double staticCreeperIgniteChance;


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

    public static double thunderBreakFreezeChance;
    public static int thunderBreakFreezeWetnessLayers;

    public static int frostbiteFireSteamThreshold;

    public static int fireFrostMeltBaseThreshold;
    public static int fireFrostMeltAdditionalCost;
    public static double fireFrostMeltDamageMult;

    public static boolean frostbiteReduceSporesEnabled;
    public static double frostbiteSporeDecaySpeed;
    public static boolean freezeClearSporesEnabled;
    public static boolean frostScorchSteamReactionEnabled;
    public static boolean scorchedFrostbiteToWetnessEnabled;
    public static int scorchedFrostbiteToWetnessRatio;
    public static boolean frostbiteClearByHeatEnabled;
    public static boolean frostbiteNetherClearEnabled;
    public static double frostbiteNetherDurationMultiplier;
    public static double frostbiteHeatSearchRadius;
    public static int frostbiteFireStandClearingTime;

    public static int staticSteamCloudTriggerStacks;

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
        staticAuraHeightCeiling = STATIC_AURA_HEIGHT_CEILING.get();

        thunderCounterMinSporeStacks = THUNDER_COUNTER_MIN_SPORE_STACKS.get();
        natureAttackCooldownTicks = NATURE_ATTACK_COOLDOWN_TICKS.get();
        staticStacksWhenNoWetness = STATIC_STACKS_WHEN_NO_WETNESS.get();
        counterLightningDamage = COUNTER_LIGHTNING_DAMAGE.get();

        paralysisMaxStacks = PARALYSIS_MAX_STACKS.get();
        paralysisDurationPerStackTicks = PARALYSIS_DURATION_PER_STACK_TICKS.get();
        paralysisDamagePercentage = PARALYSIS_DAMAGE_PERCENTAGE.get();
        paralysisCooldownTicks = PARALYSIS_COOLDOWN_TICKS.get();

        staticSporeBlastBaseChance = STATIC_SPORE_BLAST_BASE_CHANCE.get();
        staticSporeBlastPerStaticStack = STATIC_SPORE_BLAST_PER_STATIC_STACK.get();
        staticSporeBlastPerSporeStack = STATIC_SPORE_BLAST_PER_SPORE_STACK.get();
        staticCreeperIgniteChance = STATIC_CREEPER_IGNITE_CHANCE.get();

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

        thunderBreakFreezeChance = THUNDER_BREAK_FREEZE_CHANCE.get();
        thunderBreakFreezeWetnessLayers = THUNDER_BREAK_FREEZE_WETNESS_LAYERS.get();

        frostbiteFireSteamThreshold = FROSTBITE_FIRE_STEAM_THRESHOLD.get();

        fireFrostMeltBaseThreshold = FIRE_FROST_MELT_BASE_THRESHOLD.get();
        fireFrostMeltAdditionalCost = FIRE_FROST_MELT_ADDITIONAL_COST.get();
        fireFrostMeltDamageMult = FIRE_FROST_MELT_DAMAGE_MULT.get();

        frostbiteReduceSporesEnabled = FROSTBITE_REDUCE_SPORES_ENABLED.get();
        frostbiteSporeDecaySpeed = FROSTBITE_SPORE_DECAY_SPEED.get();
        freezeClearSporesEnabled = FREEZE_CLEAR_SPORES_ENABLED.get();
        frostScorchSteamReactionEnabled = FROST_SCORCH_STEAM_REACTION_ENABLED.get();
        scorchedFrostbiteToWetnessEnabled = SCORCHED_FROSTBITE_TO_WETNESS_ENABLED.get();
        scorchedFrostbiteToWetnessRatio = SCORCHED_FROSTBITE_TO_WETNESS_RATIO.get();
        frostbiteClearByHeatEnabled = FROSTBITE_CLEAR_BY_HEAT_ENABLED.get();
        frostbiteNetherClearEnabled = FROSTBITE_NETHER_CLEAR_ENABLED.get();
        frostbiteNetherDurationMultiplier = FROSTBITE_NETHER_DURATION_MULTIPLIER.get();
        frostbiteHeatSearchRadius = FROSTBITE_HEAT_SEARCH_RADIUS.get();
        frostbiteFireStandClearingTime = FROSTBITE_FIRE_STAND_CLEARING_TIME.get();

        staticSteamCloudTriggerStacks = STATIC_STEAM_CLOUD_TRIGGER_STACKS.get();

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

        frostedSteamCloudTriggerStacks = FROSTED_STEAM_CLOUD_TRIGGER_STACKS.get();

    }

    private ElementalThunderFrostReactionsConfig() {}
}
