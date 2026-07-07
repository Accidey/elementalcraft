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
    public static final ForgeConfigSpec.DoubleValue STATIC_AURA_HEIGHT_CEILING;

    public static final ForgeConfigSpec.DoubleValue THUNDER_COUNTER_BLOOD_THRESHOLD;
    public static final ForgeConfigSpec.IntValue THUNDER_COUNTER_STRENGTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue THUNDER_COUNTER_HEALTH_RECOVERY_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue THUNDER_COUNTER_RADIUS;
    public static final ForgeConfigSpec.DoubleValue THUNDER_COUNTER_EXPANSION_SPEED;
    public static final ForgeConfigSpec.IntValue THUNDER_COUNTER_STRIKE_INTERVAL;
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

    public static final ForgeConfigSpec.DoubleValue FREEZE_SETTLEMENT_DAMAGE_PER_STACK;
    public static final ForgeConfigSpec.IntValue FREEZE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FREEZE_IMMUNITY_BLACKLIST;

    public static final ForgeConfigSpec.DoubleValue THUNDER_BREAK_FREEZE_CHANCE;
    public static final ForgeConfigSpec.IntValue THUNDER_BREAK_FREEZE_TO_WETNESS_RATIO;

    public static final ForgeConfigSpec.IntValue FROSTBITE_FIRE_STEAM_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue FIRE_FROST_MELT_DAMAGE_MULT;
    public static final ForgeConfigSpec.IntValue FIRE_FROST_MELT_WETNESS_RATIO;

    public static final ForgeConfigSpec.DoubleValue FROSTBITE_SPORE_DECAY_SPEED;

    public static final ForgeConfigSpec.BooleanValue FREEZE_CLEAR_SPORES_ENABLED;

    public static final ForgeConfigSpec.IntValue SCORCHED_FROSTBITE_TO_WETNESS_RATIO;

    public static final ForgeConfigSpec.BooleanValue FROSTBITE_CLEAR_BY_HEAT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue FROSTBITE_NETHER_CLEAR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_NETHER_DECAY_SPEED;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_HEAT_SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue FROSTBITE_FIRE_STAND_CLEARING_TIME;

    public static final ForgeConfigSpec.IntValue FROSTBITE_AURA_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_AURA_BASE_RANGE;
    public static final ForgeConfigSpec.DoubleValue FROSTBITE_AURA_RANGE_PER_STACK;

    public static final ForgeConfigSpec.IntValue FROSTED_STEAM_CLOUD_TRIGGER_STACKS;

    public static final ForgeConfigSpec.BooleanValue FROST_COUNTER_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FROST_COUNTER_BLOOD_THRESHOLD;
    public static final ForgeConfigSpec.IntValue FROST_COUNTER_STRENGTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue FROST_COUNTER_HEALTH_RECOVERY_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue FROST_COUNTER_MAX_RADIUS;
    public static final ForgeConfigSpec.DoubleValue FROST_COUNTER_EXPANSION_SPEED;
    public static final ForgeConfigSpec.DoubleValue FROST_COUNTER_HEIGHT_CEILING;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Static Shock (Thunder) Reaction Configuration",
                        "静电（雷霆）效果配置",
                        "当雷霆属性强化达到门槛的生物攻击目标时，有概率施加「静电」效果。",
                        "静电会周期性造成雷击伤害，并可与其他元素（潮湿、孢子、冰霜）产生连锁反应。",
                        "静电层数越高，触发元素反应的概率和伤害越大。",
                        "When a mob with sufficient Thunder Strength attacks a target, there is a chance to apply Static Shock.",
                        "Static Shock deals periodic lightning damage and can trigger chain reactions with other elements (Wetness, Spores, Frost).",
                        "Higher stacks increase the probability and damage of elemental reactions.")
                .push("static_shock");

        BUILDER.comment("触发与叠加规则 - Trigger & Stack Rules",
                        "控制静电效果的施加条件、概率成长和层数上限。",
                        "Controls the conditions for applying Static Shock, chance scaling, and stack limits.")
                .push("trigger_and_stack");

        THUNDER_STRENGTH_THRESHOLD = BUILDER
                .comment("攻击者触发静电效果所需的最低雷霆属性强化点数，设为0则关闭静电效果。",
                         "Minimum Thunder Strength points required to apply Static Shock. Set to 0 to disable Static Shock.",
                         "Default: 20 / 默认：20")
                .defineInRange("thunder_strength_threshold", 20, 0, 10000);

        BUILDER.comment(" ");

        STATIC_BASE_CHANCE = BUILDER
                .comment("达到门槛后，攻击触发静电的基础概率。",
                         "Base chance to apply Static Shock on attack when the threshold is met.",
                         "Default: 0.3 (30%) / 默认：0.3（30%）")
                .defineInRange("static_base_chance", 0.3, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_SCALING_STEP = BUILDER
                .comment("静电概率成长的属性阶梯值（基于门槛之上计算）。例如门槛20、步长20时，雷霆强化达到40/60/80点都会触发概率提升。",
                         "The step size for Static Shock chance scaling (calculated above the threshold). E.g., threshold=20, step=20 → chance increases at 40, 60, 80 points.",
                         "Default: 20 / 默认：20")
                .defineInRange("static_scaling_step", 20, 1, 10000);

        BUILDER.comment(" ");

        STATIC_SCALING_CHANCE = BUILDER
                .comment("每达到一个步长增加的额外概率。",
                         "Additional chance gained per each scaling step.",
                         "Default: 0.1 (10%) / 默认：0.1（10%）")
                .defineInRange("static_scaling_chance", 0.1, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_WETNESS_BONUS_CHANCE_PER_LEVEL = BUILDER
                .comment("目标身上每层潮湿效果增加的额外概率。",
                         "Additional chance per level of Wetness effect on the target.",
                         "Default: 0.05 (5% per level) / 默认：0.05（每级5%）")
                .defineInRange("static_wetness_bonus_chance_per_level", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_STACKING_BONUS_CHANCE = BUILDER
                .comment("目标已存在静电效果时的额外叠加概率。",
                         "Additional chance when target already has Static Shock effect.",
                         "Default: 0.05 (5%) / 默认：0.05（5%）")
                .defineInRange("static_stacking_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_MAX_STACKS_PER_ATTACK = BUILDER
                .comment("单次攻击最多可施加的静电层数。",
                         "Maximum number of Static Shock stacks that can be applied in a single attack.",
                         "Default: 1 / 默认：1")
                .defineInRange("static_max_stacks_per_attack", 1, 1, 100);

        BUILDER.comment(" ");

        STATIC_MAX_TOTAL_STACKS = BUILDER
                .comment("目标身上静电的最大总层数。达到上限后无法继续叠加。",
                         "Maximum total stacks of Static Shock a target can have. Once reached, no more stacks can be applied.",
                         "Default: 5 / 默认：5")
                .defineInRange("static_max_total_stacks", 5, 1, 1000);

        BUILDER.comment(" ");

        STATIC_DURATION_PER_STACK_TICKS = BUILDER
                .comment("每层静电的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Base duration (in ticks) per stack of Static Shock. 20 ticks = 1 second.",
                         "Default: 100 (5 seconds) / 默认：100（5秒）")
                .defineInRange("static_duration_per_stack_ticks", 100, 1, 72000);

        BUILDER.pop();

        BUILDER.comment("免疫规则 - Immunity Rule",
                        "控制哪些实体可以完全免疫静电效果。",
                        "Controls which entities are completely immune to Static Shock.")
                .push("immunity");

        STATIC_RESIST_IMMUNITY_THRESHOLD = BUILDER
                .comment("实体完全免疫静电（叠加和伤害）所需的雷霆抗性点数。",
                         "Thunder Resistance points required for an entity to become completely immune to Static Shock (both stacking and damage).",
                         "Default: 80 / 默认：80")
                .defineInRange("static_resist_immunity_threshold", 80, 1, 10000);

        BUILDER.comment(" ");

        STATIC_IMMUNITY_BLACKLIST = BUILDER
                .comment("处于此黑名单中的实体完全免疫静电效果（无法被施加）。",
                         "Entities in this blacklist are completely immune to Static Shock effect (cannot be applied).",
                         "Example: [\"minecraft:creeper\", \"minecraft:skeleton\"]")
                .defineListAllowEmpty("static_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.comment("周期性伤害（持续伤害） - Periodic Damage",
                        "静电效果每间隔一段时间对目标造成一次雷击伤害。",
                        "伤害值在最小值和最大值之间随机浮动，受元素属性修正和附魔减免影响。",
                        "Static Shock deals periodic lightning damage to the target at regular intervals.",
                        "Damage is randomized between min and max values, modified by elemental attributes and enchantment reduction.")
                .push("periodic_damage");

        STATIC_DAMAGE_MIN = BUILDER
                .comment("每次伤害的最小值（以半心为单位，1.0 = 1心）。",
                         "Minimum damage dealt per damage tick (in half-hearts, where 1.0 = 1 heart).",
                         "Default: 1.0 / 默认：1.0")
                .defineInRange("static_damage_min", 1.0, 0.0, 10000.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_MAX = BUILDER
                .comment("每次伤害的最大值（以半心为单位，1.0 = 1心）。",
                         "Maximum damage dealt per damage tick (in half-hearts, where 1.0 = 1 heart).",
                         "Default: 5.0 / 默认：5.0")
                .defineInRange("static_damage_max", 5.0, 0.0, 10000.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("每次静电伤害触发的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Interval (in ticks) between each Static Shock damage tick. 20 ticks = 1 second.",
                         "Default: 100 (5 seconds) / 默认：100（5秒）")
                .defineInRange("static_damage_interval_ticks", 100, 1, 72000);

        BUILDER.pop();

        BUILDER.comment("元素属性修正 - Elemental Attribute Modifiers",
                        "不同元素属性的生物受到静电伤害时，会根据自身属性获得伤害倍率修正。",
                        "例：冰霜属性生物受到 200% 静电伤害，自然属性生物只受到 50%。",
                        "Entities of different elemental attributes receive modified Static Shock damage based on their type.",
                        "Example: Frost entities take 200% damage, Nature entities take only 50%.")
                .push("elemental_modifiers");

        STATIC_DAMAGE_FIRE_MULTIPLIER = BUILDER
                .comment("赤焰属性生物受到静电伤害时的伤害倍率。",
                         "Damage multiplier for Fire attribute mobs when taking Static Shock damage.",
                         "Default: 1.0 / 默认：1.0")
                .defineInRange("static_damage_fire_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_NATURE_MULTIPLIER = BUILDER
                .comment("自然属性生物受到静电伤害时的伤害倍率。0.5 = 50%伤害。",
                         "Damage multiplier for Nature attribute mobs when taking Static Shock damage. 0.5 = 50% damage.",
                         "Default: 0.5 / 默认：0.5")
                .defineInRange("static_damage_nature_multiplier", 0.5, 0.0, 10.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_THUNDER_MULTIPLIER = BUILDER
                .comment("雷霆属性生物受到静电伤害时的伤害倍率。",
                         "Damage multiplier for Thunder attribute mobs when taking Static Shock damage.",
                         "Default: 1.0 / 默认：1.0")
                .defineInRange("static_damage_thunder_multiplier", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        STATIC_DAMAGE_FROST_MULTIPLIER = BUILDER
                .comment("冰霜属性生物受到静电伤害时的伤害倍率。2.0 = 200%伤害。",
                         "Damage multiplier for Frost attribute mobs when taking Static Shock damage. 2.0 = 200% damage.",
                         "Default: 2.0 / 默认：2.0")
                .defineInRange("static_damage_frost_multiplier", 2.0, 0.0, 10.0);

        BUILDER.pop();

        BUILDER.comment("静电光环（视觉光圈阈值） - Static Shock Aura",
                        "当静电层数达到阈值时，目标周围会显示静电光环（视觉光圈）。",
                        "光环会对范围内的敌对生物造成静电伤害，层数越高范围越大。",
                        "可配置是否排除友方生物（玩家和已驯服宠物）。",
                        "When Static Shock stacks reach the threshold, a visual aura ring appears around the target.",
                        "The aura deals Static Shock damage to hostile entities within range; higher stacks increase the range.",
                        "You can configure whether to exclude friendly entities (players and tamed pets).")
                .push("static_aura");

        STATIC_AURA_THRESHOLD = BUILDER
                .comment("激活静电光环所需的最低静电层数，设为0则关闭静电光环。",
                         "Minimum Static Shock stacks required to activate the Static Shock aura. Set to 0 to disable.",
                         "Default: 3 / 默认：3")
                .defineInRange("static_aura_threshold", 3, 0, 100);

        BUILDER.comment(" ");

        STATIC_AURA_BASE_RANGE = BUILDER
                .comment("每层静电对应的光环范围（以方块为单位）。总范围 = 层数 × 该值。",
                         "Range (in blocks) per stack of Static Shock for the aura. Total range = stacks × this value.",
                         "Default: 1.0 / 默认：1.0")
                .defineInRange("static_aura_base_range", 1.0, 0.1, 20.0);

        BUILDER.comment(" ");

        STATIC_AURA_HEIGHT_CEILING = BUILDER
                .comment("静电光环可影响生物的最大高度差（格）。超过此 Y 偏移量的目标免疫光环伤害和效果。",
                         "Maximum height difference (blocks) for the Static Aura to affect entities above the source. Entities above this Y offset are immune to aura damage and effects.",
                         "Default: 3.0 / 默认：3.0")
                .defineInRange("static_aura_height_ceiling", 3.0, 0.0, 64.0);

        BUILDER.pop();

        BUILDER.comment("静电+孢子触发毒火爆燃 - Static Shock + Spores -> Toxic Blast",
                        "当目标同时拥有静电和易燃孢子效果时，静电会引爆孢子触发毒火爆燃。",
                        "总概率 = 基础概率 + 静电层数 × 每层静电加成 + 孢子层数 × 每层孢子加成。",
                        "当目标同时拥有麻痹效果时，必定触发（无概率判定）。",
                        "When a target has both Static Shock and Flammable Spores, Static Shock detonates the spores causing a Toxic Blast.",
                        "Total chance = base + static_stacks × per_static + spore_stacks × per_spore.",
                        "If the target also has Paralysis effect, it triggers unconditionally (no chance roll).")
                .push("static_spore_blast");

        STATIC_SPORE_BLAST_BASE_CHANCE = BUILDER
                .comment("静电+孢子触发毒火爆燃的基础概率，设为0则关闭。",
                         "Base chance for Static Shock + Spores to trigger Toxic Blast. Set to 0 to disable.",
                         "Default: 0.30 (30%) / 默认：0.30（30%）")
                .defineInRange("base_chance", 0.30, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_SPORE_BLAST_PER_STATIC_STACK = BUILDER
                .comment("目标每层静电增加的额外概率。",
                         "Additional chance per stack of Static Shock on the target.",
                         "Default: 0.10 (10%) / 默认：0.10（10%）")
                .defineInRange("per_static_stack", 0.10, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_SPORE_BLAST_PER_SPORE_STACK = BUILDER
                .comment("目标每层孢子增加的额外概率。",
                         "Additional chance per stack of Spores on the target.",
                         "Default: 0.05 (5%) / 默认：0.05（5%）")
                .defineInRange("per_spore_stack", 0.05, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("静电苦力怕变高压+引爆 - Static Shock Creeper Ignite",
                        "静电击中苦力怕时，有概率将其变为高压苦力怕并点燃（1.5秒引信）。",
                        "变高压后会触发原版高压爆炸机制，伤害和范围均按高压苦力怕计算。",
                        "When Static Shock hits a Creeper, there is a chance to convert it to a Charged Creeper and ignite it (1.5s fuse).",
                        "The Charged Creeper then triggers vanilla Charged explosion mechanics with increased damage and radius.")
                .push("static_creeper_ignite");

        STATIC_CREEPER_IGNITE_CHANCE = BUILDER
                .comment("静电击中的苦力怕变高压并引爆的概率。设为0则关闭此反应。",
                         "Chance for Static Shock to prime (->charged) and ignite a Creeper. Set to 0 to disable.",
                         "Default: 0.4 (40%) / 默认：0.4（40%）")
                .defineInRange("chance", 0.4, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("静电附魔减伤配置 - Enchantment Reduction",
                        "控制附魔对静电伤害的减免上限。",
                        "减免计算公式：减免率 = min(附魔等级 × (上限值 / 分母), 上限值)。",
                        "多种附魔的减免效果可叠加，但总减免不超过 100%。",
                        "Controls the maximum damage reduction from enchantments against Static Shock damage.",
                        "Reduction formula: rate = min(enchantment_level × (cap / denominator), cap).",
                        "Multiple enchantments stack, but total reduction cannot exceed 100%.")
                .push("static_enchantment_reduction");

        STATIC_MAX_PROT_CAP = BUILDER
                .comment("保护附魔对静电伤害的最大减免比例。",
                         "4件保护IV = 16级 × (该值 / 16) = 该值。",
                         "Maximum damage reduction from Protection enchantment against Static Shock damage.",
                         "4 pieces of Protection IV = 16 levels × (this_value / 16) = this_value.",
                         "Default: 0.25 (25%) / 默认：0.25（25%）")
                .defineInRange("static_max_prot_cap", 0.25, 0.0, 1.0);

        BUILDER.comment(" ");

        STATIC_MAX_PROJECTILE_PROT_CAP = BUILDER
                .comment("弹射物保护附魔对静电伤害的最大减免比例。",
                         "4件弹射物保护IV = 16级 × (该值 / 16) = 该值。",
                         "Maximum damage reduction from Projectile Protection enchantment against Static Shock damage.",
                         "4 pieces of Projectile Protection IV = 16 levels × (this_value / 16) = this_value.",
                         "Default: 0.50 (50%) / 默认：0.50（50%）")
                .defineInRange("static_max_projectile_prot_cap", 0.50, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("雷霆反制配置 - Thunder Counter",
                        "雷霆属性生物在低血量时受到伤害触发的反击机制：释放扩散雷暴，周期性劈击范围内所有实体并施加静电效果。",
                        "A counter-attack triggered when a Thunder entity takes damage at low health: releases an expanding thunder storm that periodically strikes all entities in range and applies Static Shock.")
                .push("thunder_counter");
        BUILDER.comment(" ");

        THUNDER_COUNTER_BLOOD_THRESHOLD = BUILDER
                .comment("触发雷霆反制所需的血量阈值（百分比）。雷霆属性生物血量低于此比例时受伤才会触发。设为0则关闭雷霆反制。",
                         "Health threshold to trigger Thunder Counter. Thunder entity must be below this health percentage when taking damage. Set to 0 to disable.",
                         "Default: 0.5 / 默认：0.5")
                .defineInRange("blood_threshold", 0.5, 0.0, 1.0);

        BUILDER.comment(" ");

        THUNDER_COUNTER_STRENGTH_THRESHOLD = BUILDER
                .comment("触发雷霆反制所需的最小雷霆属性强化点数。",
                         "Minimum Thunder points required to trigger Thunder Counter.",
                         "Default: 50 / 默认：50")
                .defineInRange("strength_threshold", 50, 0, 10000);
        BUILDER.comment(" ");

        THUNDER_COUNTER_HEALTH_RECOVERY_THRESHOLD = BUILDER
                .comment("触发雷霆反制后，生命值需要恢复到多少比例才能再次触发。例如0.8表示生命值恢复到80%后才可再次触发。",
                         "Health recovery threshold to re-trigger Thunder Counter. Entity must heal back to this percentage to be eligible again. E.g., 0.8 = 80%.",
                         "Default: 0.8 / 默认：0.8")
                .defineInRange("health_recovery_threshold", 0.8, 0.0, 1.0);

        BUILDER.comment(" ");

        COUNTER_LIGHTNING_DAMAGE = BUILDER
                .comment("雷霆反制召唤的闪电造成的伤害。",
                         "Damage dealt by the lightning bolt summoned by Thunder Counter.",
                         "Default: 10.0 / 默认：10.0")
                .defineInRange("lightning_damage", 10.0, 0.0, 100.0);

        BUILDER.comment(" ");

        THUNDER_COUNTER_RADIUS = BUILDER
                .comment("雷霆反制的范围半径（格）。",
                         "Thunder Counter effect radius (blocks).",
                         "Default: 8.0 / 默认：8.0")
                .defineInRange("radius", 8.0, 1.0, 32.0);

        BUILDER.comment(" ");

        THUNDER_COUNTER_EXPANSION_SPEED = BUILDER
                .comment("雷霆风暴圈的扩散速度（格/秒）。",
                         "Expansion speed of the thunder storm ring (blocks/second).",
                         "Default: 2.0 / 默认：2.0")
                .defineInRange("expansion_speed", 2.0, 0.1, 20.0);

        BUILDER.comment(" ");

        THUNDER_COUNTER_STRIKE_INTERVAL = BUILDER
                .comment("每次劈闪电的间隔刻数。20刻 = 1秒。",
                         "Interval ticks between lightning strikes. 20 ticks = 1 second.",
                         "Default: 20 / 默认：20")
                .defineInRange("strike_interval", 20, 5, 100);

        BUILDER.pop();

        BUILDER.comment("麻痹反应配置 - Paralysis Reaction",
                        "当静电层数积累到一定程度时，目标会被「麻痹」，无法移动和攻击。",
                        "麻痹期间若目标同时拥有易燃孢子，会必定触发毒火爆燃（无概率判定）。",
                        "麻痹会消耗剩余静电伤害的一定比例作为一次性伤害。",
                        "When Static Shock stacks accumulate enough, the target becomes Paralyzed and cannot move or attack.",
                        "During Paralysis, if the target also has Flammable Spores, Toxic Blast triggers unconditionally (no chance roll).",
                        "Paralysis consumes a percentage of remaining Static Shock damage as instant damage.")
                .push("paralysis");

        PARALYSIS_MAX_STACKS = BUILDER
                .comment("目标身上麻痹的最大总层数，设为0则不会触发任何麻痹效果。",
                         "Maximum total stacks of Paralysis a target can have. Set to 0 to disable all Paralysis effects.",
                         "Default: 5 / 默认：5")
                .defineInRange("paralysis_max_stacks", 5, 0, 1000);

        BUILDER.comment(" ");

        PARALYSIS_DURATION_PER_STACK_TICKS = BUILDER
                .comment("每层麻痹的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Base duration (in ticks) per stack of Paralysis. 20 ticks = 1 second.",
                         "Default: 40 (2 seconds) / 默认：40（2秒）")
                .defineInRange("paralysis_duration_per_stack_ticks", 40, 1, 72000);

        BUILDER.comment(" ");

        PARALYSIS_DAMAGE_PERCENTAGE = BUILDER
                .comment("触发麻痹时，静电剩余伤害的百分比。0.5 = 50%。",
                         "Percentage of remaining Static Shock damage dealt when Paralysis is triggered. 0.5 = 50%.",
                         "Default: 0.5 / 默认：0.5")
                .defineInRange("paralysis_damage_percentage", 0.5, 0.0, 1.0);

        BUILDER.comment(" ");

        PARALYSIS_COOLDOWN_TICKS = BUILDER
                .comment("目标在麻痹结束后，再次进入麻痹流程所需的冷却时间（刻）。\n" +
                         "冷却期间，潮湿和静电效果正常运作（潮湿减火伤、静电持续伤害）。\n" +
                         "仅在尝试触发麻痹流程时检测冷却，冷却中则清除潮湿和静电效果，\n" +
                         "不结算静电伤害也不施加麻痹状态。",
                          "Cooldown ticks before a target can enter the Paralysis flow again after Paralysis ends.\n" +
                          "During cooldown, Wetness still works normally. Only when the\n" +
                          "paralysis flow is about to be triggered (target has both Wetness and Static), the cooldown\n" +
                          "is checked. If in cooldown, Static Shock is cleared but NO static damage is dealt\n" +
                          "and NO Paralysis is applied. Wetness is not cleared during cooldown.",
                         "Default: 200 (10 seconds) / 默认：200（10秒）")
                .defineInRange("paralysis_cooldown_ticks", 200, 0, 72000);

        BUILDER.pop();

        PARALYSIS_IMMUNITY_BLACKLIST = BUILDER
                .comment("处于此黑名单中的实体完全免疫麻痹效果（无法被施加）。",
                         "Entities in this blacklist are completely immune to Paralysis effect (cannot be applied).",
                         "Example: [\"minecraft:iron_golem\", \"minecraft:wither\"]")
                .defineListAllowEmpty("paralysis_immunity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.comment("静电蒸汽云反应 - Static Steam Cloud Reaction",
                        "当带有静电的实体进入低温蒸汽云时，会触发麻痹和感电化反应。",
                        "感电化会使蒸汽云对范围内所有生物造成静电伤害，形成导电区域。",
                        "When an entity with Static Shock enters a Condensing Steam Cloud, it triggers Paralysis and Electrification.",
                        "Electrification makes the Steam Cloud deal Static Shock damage to all entities within range, creating a conductive zone.")
                .push("static_steam_cloud");

        STATIC_STEAM_CLOUD_TRIGGER_STACKS = BUILDER
                .comment("带静电的实体进入低温蒸汽云时触发麻痹和感电化所需的最低静电层数，设为0则关闭静电蒸汽云反应。",
                         "Minimum Static stacks to trigger the Static Steam Cloud reaction. Set to 0 to disable.",
                         "Default: 1 / 默认：1")
                .defineInRange("static_steam_cloud_trigger_stacks", 1, 0, 1000);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("感电水域 - Water Electrification",
                        "当带有静电的实体站在水中时，会感电周围的水域。",
                        "感电水域会对范围内所有水中的生物造成静电伤害和麻痹效果。",
                        "范围 = 基础范围 + (静电层数 - 1) × 每层范围。",
                        "When an entity with Static Shock stands in water, it electrifies the surrounding water.",
                        "Electrified water deals Static Shock damage and Paralysis to all entities in water within range.",
                        "Range = base + (static_stacks - 1) × per_stack.")
                .push("water_electrification");

        WATER_ELECTRIFICATION_RANGE_BASE = BUILDER
                .comment("用于触发电解麻痹和静电伤害的水中感电范围，设为0则关闭感电水域。",
                         "Base range (in blocks) of water electrification. Set to 0 to disable Water Electrification.",
                         "Default: 3.0 / 默认：3.0")
                .defineInRange("water_electrification_range_base", 3.0, 0.0, 50.0);

        BUILDER.comment(" ");

        WATER_ELECTRIFICATION_RANGE_PER_STACK = BUILDER
                .comment("源实体每层静电增加的范围，从第2层开始叠加。",
                         "Additional range per static stack, stacking from the 2nd stack onward.",
                         "Default: 1.0 / 默认：1.0")
                .defineInRange("water_electrification_range_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        WATER_ELECTRIFICATION_PARALYSIS_DURATION = BUILDER
                .comment("感电水域对水中生物施加的麻痹持续时间（以刻为单位）。",
                         "设为0则只造成伤害，不附加麻痹。20刻 = 1秒。",
                         "Duration (in ticks) of paralysis applied to entities in electrified water.",
                         "Set to 0 to disable paralysis (damage only). 20 ticks = 1 second.",
                         "Default: 100 (5 seconds) / 默认：100（5秒）")
                .defineInRange("water_electrification_paralysis_duration", 100, 0, 200);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("霜冻（冰霜）效果配置 - Frostbite (Frost) Reaction",
                        "当冰霜属性强化达到门槛的生物攻击目标时，有概率施加「霜冻」效果。",
                        "霜冻会降低目标移动速度和攻击速度，层数越高效果越强。",
                        "霜冻可与潮湿、静电产生元素反应，也可转化为冻结状态。",
                        "When a mob with sufficient Frost Strength attacks a target, there is a chance to apply Frostbite.",
                        "Frostbite reduces target's movement and attack speed; higher stacks intensify the effect.",
                        "Frostbite can react with Wetness and Static Shock, or convert to Freeze state.")
                .push("frostbite");

        BUILDER.comment("触发与叠加规则 - Trigger & Stack Rules",
                        "控制霜冻效果的施加条件、概率成长和层数上限。",
                        "Controls the conditions for applying Frostbite, chance scaling, and stack limits.")
                .push("trigger_and_stack");

        FROST_STRENGTH_THRESHOLD = BUILDER
                .comment("冰霜属性攻击给予霜冻效果所需的冰霜强化值阈值，设为0则关闭。",
                         "Minimum Frost Strength required to apply Frostbite. Set to 0 to disable Frostbite application.",
                         "Default: 20 / 默认：20")
                .defineInRange("frost_strength_threshold", 20.0, 0.0, 10000.0);

        BUILDER.comment(" ");

        FROSTBITE_BASE_CHANCE = BUILDER
                .comment("达到门槛后，攻击触发霜冻的基础概率。",
                         "Base chance to apply Frostbite on attack when the threshold is met.",
                         "Default: 0.2 (20%) / 默认：0.2（20%）")
                .defineInRange("frostbite_base_chance", 0.2, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_SCALING_STEP = BUILDER
                .comment("霜冻概率成长的属性阶梯值（基于门槛之上计算）。例如门槛20、步长20时，冰霜强化达到40/60/80点都会触发概率提升。",
                         "The step size for Frostbite chance scaling (calculated above the threshold). E.g., threshold=20, step=20 → chance increases at 40, 60, 80 points.",
                         "Default: 20 / 默认：20")
                .defineInRange("frostbite_scaling_step", 20.0, 1.0, 10000.0);

        BUILDER.comment(" ");

        FROSTBITE_SCALING_CHANCE = BUILDER
                .comment("每达到一个步长增加的额外概率。",
                         "Additional chance gained per each scaling step.",
                         "Default: 0.1 (10%) / 默认：0.1（10%）")
                .defineInRange("frostbite_scaling_chance", 0.1, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_WETNESS_BONUS_CHANCE = BUILDER
                .comment("目标身上每层潮湿效果增加的额外概率。",
                         "Additional chance per level of Wetness effect on the target.",
                         "Default: 0.05 (5% per level) / 默认：0.05（每级5%）")
                .defineInRange("frostbite_wetness_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_STACKING_BONUS_CHANCE = BUILDER
                .comment("目标已存在霜冻效果时的额外叠加概率。",
                         "Additional chance when target already has Frostbite effect.",
                         "Default: 0.05 (5%) / 默认：0.05（5%）")
                .defineInRange("frostbite_stacking_bonus_chance", 0.05, 0.0, 1.0);

        BUILDER.comment(" ");

        FROSTBITE_MAX_STACKS_PER_ATTACK = BUILDER
                .comment("单次攻击最多可施加的霜冻层数。",
                         "Maximum number of Frostbite stacks that can be applied in a single attack.",
                         "Default: 1 / 默认：1")
                .defineInRange("frostbite_max_stacks_per_attack", 1, 1, 100);

        BUILDER.comment(" ");

        FROSTBITE_MAX_TOTAL_STACKS = BUILDER
                .comment("目标身上霜冻的最大总层数。",
                         "Maximum total stacks of Frostbite a target can have.",
                         "Default: 5 / 默认：5")
                .defineInRange("frostbite_max_total_stacks", 5, 1, 1000);

        BUILDER.comment(" ");

        FROSTBITE_BASE_DURATION_TICKS = BUILDER
                .comment("霜冻首层的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Base duration (in ticks) for the first Frostbite stack. 20 ticks = 1 second.",
                         "Default: 200 (10 seconds) / 默认：200（10秒）")
                .defineInRange("frostbite_base_duration_ticks", 200, 1, 72000);

        BUILDER.comment(" ");

        FROSTBITE_DURATION_PER_EXTRA_STACK_TICKS = BUILDER
                .comment("超过首层后，每层霜冻额外增加的持续时间（以刻为单位）。20刻 = 1秒。",
                         "Additional duration (in ticks) per extra Frostbite stack beyond the first. 20 ticks = 1 second.",
                         "Default: 100 (5 seconds) / 默认：100（5秒）")
                .defineInRange("frostbite_duration_per_extra_stack_ticks", 100, 1, 72000);

        BUILDER.comment(" ");

        FROSTBITE_SPEED_REDUCTION_PER_STACK = BUILDER
                .comment("每层霜冻降低的移动速度和攻击速度比例。0.1 = 10%减速。0 = 关闭。上限90%。",
                         "Movement speed and attack speed reduction per stack of Frostbite. 0.1 = 10% reduction. 0 = disable. Max 90%.",
                         "Default: 0.1 (10%) / 默认：0.1（10%）")
                .defineInRange("frostbite_speed_reduction_per_stack", 0.1, 0.0, 0.9);

        BUILDER.comment(" ");

        FROSTBITE_PERIODIC_DAMAGE = BUILDER
                .comment("霜冻周期性伤害的伤害值（每隔 frostbite_damage_interval_ticks 触发一次）。",
                         "Damage dealt by Frostbite periodic tick (every frostbite_damage_interval_ticks).",
                         "Default: 2.0 / 默认：2.0")
                .defineInRange("frostbite_periodic_damage", 2.0, 0.0, 10000.0);

        BUILDER.comment(" ");

        FROSTBITE_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("霜冻周期性伤害的间隔时间（以刻为单位）。20刻 = 1秒。",
                         "Interval (in ticks) between each Frostbite periodic damage. 20 ticks = 1 second.",
                         "Default: 100 (5 seconds) / 默认：100（5秒）")
                .defineInRange("frostbite_damage_interval_ticks", 100, 1, 72000);

        BUILDER.comment(" ");

        FROSTBITE_RESIST_IMMUNITY_THRESHOLD = BUILDER
                .comment("实体完全免疫霜冻所需的冰霜抗性点数。",
                         "Frost Resistance points required for an entity to become completely immune to Frostbite.",
                         "Default: 80 / 默认：80")
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
                         "Default: 1.5 / 默认：1.5")
                .defineInRange("frostbite_damage_fire_multiplier", 1.5, 0.0, 10.0);

        BUILDER.comment(" ");

        FROSTBITE_DAMAGE_NATURE_MULTIPLIER = BUILDER
                .comment("自然属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Damage multiplier for Nature attribute mobs when taking Frostbite periodic/aura damage.",
                         "Default: 2.0 / 默认：2.0")
                .defineInRange("frostbite_damage_nature_multiplier", 2.0, 0.0, 10.0);

        BUILDER.comment(" ");

        FROSTBITE_DAMAGE_THUNDER_MULTIPLIER = BUILDER
                .comment("雷霆属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Damage multiplier for Thunder attribute mobs when taking Frostbite periodic/aura damage.",
                         "Default: 0.5 / 默认：0.5")
                .defineInRange("frostbite_damage_thunder_multiplier", 0.5, 0.0, 10.0);

        BUILDER.comment(" ");

        FROSTBITE_DAMAGE_FROST_MULTIPLIER = BUILDER
                .comment("冰霜属性生物受到霜冻周期性/光环伤害时的伤害倍率。",
                         "Damage multiplier for Frost attribute mobs when taking Frostbite periodic/aura damage.",
                         "Default: 1.0 / 默认：1.0")
                .defineInRange("frostbite_damage_frost_multiplier", 1.0, 0.0, 10.0);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("霜冻光环（范围冰霜伤害） - Frostbite Aura",
                        "当霜冻层数达到阈值时，目标周围会形成霜冻光环。",
                        "光环会对范围内的敌对生物造成冰霜伤害和减速效果，层数越高范围越大。",
                        "可配置是否排除友方生物（玩家和已驯服宠物）。",
                        "When Frostbite stacks reach the threshold, a Frostbite Aura forms around the target.",
                        "The aura deals Frost damage and slows hostile entities within range; higher stacks increase the range.",
                        "You can configure whether to exclude friendly entities (players and tamed pets).")
                .push("frostbite_aura");

        FROSTBITE_AURA_THRESHOLD = BUILDER
                .comment("激活霜冻光环所需的最低霜冻层数，设为0则关闭霜冻光环。",
                         "Minimum Frostbite stacks required to activate the Frostbite Aura. Set to 0 to disable.",
                         "Default: 3 / 默认：3")
                .defineInRange("frostbite_aura_threshold", 3, 0, 100);

        BUILDER.comment(" ");

        FROSTBITE_AURA_BASE_RANGE = BUILDER
                .comment("霜冻层数等于阈值时的基础光环半径（以方块为单位）。",
                         "Base radius (in blocks) of the Frostbite Aura when stacks equal the threshold.",
                         "Default: 3.0 / 默认：3.0")
                .defineInRange("frostbite_aura_base_range", 3.0, 1.0, 50.0);

        BUILDER.comment(" ");

        FROSTBITE_AURA_RANGE_PER_STACK = BUILDER
                .comment("超过阈值后，每层霜冻增加的光环范围（以方块为单位）。",
                         "Additional radius (in blocks) per extra Frostbite stack beyond the threshold.",
                         "Default: 1.0 / 默认：1.0")
                .defineInRange("frostbite_aura_range_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("灼烧-霜冻转潮湿 - Scorched-Frostbite to Wetness",
                        "当目标同时拥有灼烧和霜冻(Frostbite，未冻结)时，霜冻层数会转化为潮湿层数。",
                        "转化后灼烧效果会被清除，防止灼烧+霜冻的冲突状态持续存在。",
                        "转化比例可配置：1 = 1层霜冻转1层潮湿，2 = 1层霜冻转2层潮湿。",
                        "When a target has both Scorched and Frostbite (not Frozen), Frostbite stacks convert to Wetness.",
                        "Scorched is cleared after conversion, preventing the conflicting Scorched + Frostbite state.",
                        "Conversion ratio is configurable: 1 = 1 Frostbite → 1 Wetness, 2 = 1 Frostbite → 2 Wetness.")
                .push("scorched_frostbite_to_wetness");

        SCORCHED_FROSTBITE_TO_WETNESS_RATIO = BUILDER
                .comment("霜冻转化为潮湿的层数比例。1 = 1层霜冻转1层潮湿，2 = 1层霜冻转2层潮湿。转化后的潮湿层数受潮湿最大等级上限。",
                         "Ratio of Frostbite stacks converted to Wetness. 1 = 1 Frostbite → 1 Wetness, 2 = 1 Frostbite → 2 Wetness. Converted wetness is capped by the max wetness level.",
                         "0 = disabled / 0 = 关闭",
                         "Default: 1 / 默认：1")
                .defineInRange("scorched_frostbite_to_wetness_ratio", 1, 0, 10);

        BUILDER.pop();

        BUILDER.comment("霜冻+易燃孢子（霜冻削减孢子持续时间） - Frostbite + Spores",
                        "当目标同时拥有霜冻和易燃孢子时，霜冻会加速孢子效果的消退。",
                        "孢子剩余时间按配置的倍率加速流逝，霜冻也会被消耗。",
                        "此机制可防止霜冻+孢子的组合被无限维持。",
                        "When a target has both Frostbite and Flammable Spores, Frostbite accelerates Spore decay.",
                        "Spore duration ticks down at the configured speed multiplier, and Frostbite is also consumed.",
                        "This prevents the Frostbite + Spores combo from being maintained indefinitely.")
                .push("frostbite_spores");

        FROSTBITE_SPORE_DECAY_SPEED = BUILDER
                .comment("霜冻和易燃孢子同时存在时，孢子剩余时间的流逝速度倍率。2 = 2倍速流逝，1 = 正常速度。",
                         "When Frostbite and Flammable Spores coexist, the Spores duration ticks down at this speed multiplier. 2 = 2x speed (half duration), 1 = normal speed.",
                         "0 = disabled / 0 = 关闭",
                         "Default: 10.0 / 默认：10.0")
                .defineInRange("frostbite_spore_decay_speed", 10.0, 0.0, 100.0);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("霜冻热源清除 - Frostbite Heat Clearing",
                        "热源（火焰、熔岩、岩浆块、下界）会清除霜冻效果。",
                        "下界维度会立即清除霜冻，其他热源需要接触才会清除。",
                        "此机制模拟霜冻在高温环境下自然融化的物理现象。",
                        "Heat sources (fire, lava, magma blocks, Nether) will remove Frostbite.",
                        "The Nether clears Frostbite immediately; other heat sources require contact.",
                        "This simulates the physical phenomenon of frost melting in high-temperature environments.")
                .push("frostbite_heat_clearing");

        FROSTBITE_CLEAR_BY_HEAT_ENABLED = BUILDER
                .comment("总开关：启用后，热源（火焰、熔岩、岩浆块、下界）会清除霜冻效果。",
                         "Master toggle: when enabled, heat sources (fire, lava, magma blocks, Nether) will remove Frostbite.",
                         "Default: true / 默认：true")
                .define("frostbite_clear_by_heat_enabled", true);

        BUILDER.comment(" ");

        FROSTBITE_NETHER_CLEAR_ENABLED = BUILDER
                .comment("启用后，进入下界维度时霜冻效果会被立即清除。",
                         "When enabled, Frostbite is cleared immediately upon entering the Nether dimension.",
                         "Default: true / 默认：true")
                .define("frostbite_nether_clear_enabled", true);

        BUILDER.comment(" ");
        FROSTBITE_NETHER_DECAY_SPEED = BUILDER
                .comment("下界维度中霜冻倒计时的加速倍率。2.0 = 2倍速流逝，10.0 = 10倍速流逝。",
                         "Frostbite countdown speed multiplier in the Nether dimension. 2.0 = 2x faster, 10.0 = 10x faster.",
                         "Default: 2.0 / 默认：2.0")
                .defineInRange("frostbite_nether_decay_speed", 2.0, 1.0, 100.0);

        BUILDER.comment(" ");

        FROSTBITE_HEAT_SEARCH_RADIUS = BUILDER
                .comment("检测周围热源（熔岩/岩浆块）清除霜冻的半径范围（格）。岩浆块的检测半径会减少1格。",
                         "Radius (blocks) to search for nearby heat sources (Lava/Magma) that clear Frostbite. Magma Block detection radius is reduced by 1.",
                         "Default: 2.0 / 默认：2.0")
                .defineInRange("frostbite_heat_search_radius", 2.0, 1.0, 16.0);

        BUILDER.comment(" ");

        FROSTBITE_FIRE_STAND_CLEARING_TIME = BUILDER
                .comment("站在火中（普通火或灵魂火）清除所有霜冻效果所需的秒数。",
                         "Seconds required to stand on a fire block (Fire or Soul Fire) to clear all Frostbite.",
                         "Default: 2 / 默认：2")
                .defineInRange("frostbite_fire_stand_clearing_time", 2, 1, 600);

        BUILDER.comment(" ");

        BUILDER.pop();

        BUILDER.comment("霜冻蒸汽云反应 - Frosted Steam Cloud Reaction",
                        "当带有霜冻的实体进入低温蒸汽云时，会触发霜寒蒸汽云反应。",
                        "反应会将蒸汽云转化为霜寒蒸汽云，对范围内生物造成冰霜伤害和减速效果。",
                        "When an entity with Frostbite enters a Condensing Steam Cloud, a Frosted Steam Cloud reaction triggers.",
                        "The reaction converts the Steam Cloud into a Frosted Steam Cloud, dealing Frost damage and slowing entities in range.")
                .push("frosted_steam_cloud");

        FROSTED_STEAM_CLOUD_TRIGGER_STACKS = BUILDER
                .comment("触发霜寒蒸汽云反应所需的最低霜冻层数，设为0则关闭霜寒蒸汽云反应。",
                         "Minimum Frostbite stacks to trigger the Frosted Steam Cloud reaction. Set to 0 to disable.",
                         "Default: 3 / 默认：3")
                .defineInRange("frosted_steam_cloud_trigger_stacks", 3, 0, 1000);

        BUILDER.pop();

        BUILDER.comment("冰霜反制配置 - Frost Counter",
                        "冰霜属性生物在低血量时受到伤害触发的反击机制：释放冰暴霜冻环，冻结范围内的敌人。",
                        "A counter-attack triggered when a Frost entity takes damage at low health: releases an Ice Burst frost ring that freezes enemies in range.")
                .push("frost_counter");

        FROST_COUNTER_ENABLED = BUILDER
                .comment("是否启用冰霜反制。设为 false 则完全关闭此反应。",
                         "Whether to enable Frost Counter. Set to false to disable completely.",
                         "Default: true / 默认：true")
                .define("enabled", true);

        BUILDER.comment(" ");

        FROST_COUNTER_BLOOD_THRESHOLD = BUILDER
                .comment("触发冰霜反制所需的血量阈值（百分比）。冰霜属性生物血量低于此比例时受伤才会触发。设为0则关闭。",
                         "Health threshold to trigger Frost Counter. Frost entity must be below this health percentage when taking damage. Set to 0 to disable.",
                         "Default: 0.5 / 默认：0.5")
                .defineInRange("blood_threshold", 0.5, 0.0, 1.0);

        BUILDER.comment(" ");

        FROST_COUNTER_STRENGTH_THRESHOLD = BUILDER
                .comment("触发冰霜反制所需的最小冰霜属性强化点数。",
                         "Minimum Frost points required to trigger Frost Counter.",
                         "Default: 10 / 默认：10")
                .defineInRange("strength_threshold", 10, 0, 10000);

        BUILDER.comment(" ");

        FROST_COUNTER_HEALTH_RECOVERY_THRESHOLD = BUILDER
                .comment("触发冰霜反制后，生命值需要恢复到多少比例才能再次触发。例如0.8表示生命值恢复到80%后才可再次触发。",
                         "Health recovery threshold to re-trigger Frost Counter. Entity must heal back to this percentage to be eligible again. E.g., 0.8 = 80%.",
                         "Default: 0.8 / 默认：0.8")
                .defineInRange("health_recovery_threshold", 0.8, 0.0, 1.0);

        BUILDER.comment(" ");

        FROST_COUNTER_MAX_RADIUS = BUILDER
                .comment("冰暴霜冻环的最大半径（格）。达到此半径后霜冻环消失。",
                         "Maximum radius (blocks) of the Ice Burst frost ring. The ring disappears upon reaching this radius.",
                         "Default: 8.0 / 默认：8.0")
                .defineInRange("max_radius", 8.0, 1.0, 50.0);

        BUILDER.comment(" ");

        FROST_COUNTER_EXPANSION_SPEED = BUILDER
                .comment("冰暴霜冻环的扩大速度（格/秒）。",
                         "Expansion speed of the Ice Burst frost ring (blocks/second).",
                         "Default: 2.0 / 默认：2.0")
                .defineInRange("expansion_speed", 2.0, 0.1, 20.0);

        BUILDER.comment(" ");

        FROST_COUNTER_HEIGHT_CEILING = BUILDER
                .comment("冰暴可影响生物的最大高度差（格）。超过此偏移的目标不受霜冻效果影响。",
                         "Maximum height difference (blocks) for Ice Burst to affect entities.",
                         "Default: 3.0 / 默认：3.0")
                .defineInRange("height_ceiling", 3.0, 0.0, 64.0);

        BUILDER.pop();

        BUILDER.comment("冻结反应配置（霜冻+潮湿触发） - Freeze Reaction",
                        "当目标同时拥有霜冻和潮湿效果时，会触发「冻结」反应。",
                        "冻结会消耗霜冻和潮湿，将目标冰封在原地（无法移动、攻击、使用物品）。",
                        "冻结期间目标受到的物理伤害会增加，且可以被击碎（碎冰反应）。",
                        "When a target has both Frostbite and Wetness, a Freeze reaction triggers.",
                        "Freeze consumes Frostbite and Wetness, encasing the target in ice (cannot move, attack, or use items).",
                        "During Freeze, physical damage taken is increased, and the target can be shattered (Ice Shatter reaction).")
                .push("freeze");

        FREEZE_MAX_STACKS = BUILDER
                .comment("目标身上冻结的最大总层数，设为0则关闭冻结反应。",
                         "Maximum total stacks of Freeze a target can have. Set to 0 to disable Freeze reactions.",
                         "Default: 5 / 默认：5")
                .defineInRange("freeze_max_stacks", 5, 0, 1000);

        BUILDER.comment(" ");

        FREEZE_DURATION_PER_STACK_TICKS = BUILDER
                .comment("每层冻结的基础持续时间（以刻为单位）。20刻 = 1秒。",
                         "Base duration (in ticks) per stack of Freeze. 20 ticks = 1 second.",
                         "Default: 20 (1 second) / 默认：20（1秒）")
                .defineInRange("freeze_duration_per_stack_ticks", 20, 1, 72000);

        BUILDER.comment(" ");

        FREEZE_SETTLEMENT_DAMAGE_PER_STACK = BUILDER
                .comment("触发冻结时每层霜冻结算的伤害倍率（基于frostbite_periodic_damage的百分比）。1.0 = 100%，0.5 = 50%，2.0 = 200%。",
                         "Damage multiplier per Frostbite stack when Freeze is triggered (percentage of frostbite_periodic_damage). 1.0 = 100%, 0.5 = 50%, 2.0 = 200%.",
                         "Default: 1.0 / 默认：1.0")
                .defineInRange("freeze_settlement_damage_per_stack", 1.0, 0.0, 10.0);

        BUILDER.comment(" ");

        FREEZE_COOLDOWN_TICKS = BUILDER
                .comment("冻结结束后再次被冻结所需的冷却时间（刻）。",
                         "Cooldown ticks before a target can be frozen again after Freeze ends.",
                         "Default: 200 (10 seconds) / 默认：200（10秒）")
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
                         "Default: 0.2 (20% per stack) / 默认：0.2（每层20%）")
                .defineInRange("thunder_break_freeze_chance", 0.2, 0.0, 1.0);

        BUILDER.comment(" ");

        THUNDER_BREAK_FREEZE_TO_WETNESS_RATIO = BUILDER
                .comment("静电解冻后每层冻结转化为潮湿的倍率。0 = 关闭此功能。",
                         "Ratio per freeze stack to convert to wetness layers after breaking Freeze via Static Shock. 0 = disabled.",
                         "Default: 1 (1:1) / 默认：1（1：1）")
                .defineInRange("thunder_break_freeze_to_wetness_ratio", 1, 0, 10);

        BUILDER.comment(" ");

        FROSTBITE_FIRE_STEAM_THRESHOLD = BUILDER
                .comment("赤焰属性攻击冰冻目标解除冻结并转化为潮湿所需的最低赤焰强化（固定值，不随冻结层数变化），设为0则关闭此反应。",
                         "Min Fire enhancement to break Freeze and convert to Wetness (flat threshold, no per-stack scaling). Set to 0 to disable.",
                         "Default: 50 / 默认：50")
                .defineInRange("frostbite_fire_steam_threshold", 50, 0, 10000);

        BUILDER.comment(" ");

        FIRE_FROST_MELT_DAMAGE_MULT = BUILDER
                .comment("赤焰攻击冰冻(Frozen)目标触发融冰时，属性伤害的倍率。",
                         "0.5 = 原本属性伤害的50%（降低50%）。",
                         "Damage multiplier when Fire attacks a Frozen target and triggers melt.",
                         "0.5 = 50% of original elemental damage (50% reduction).",
                         "Default: 0.5 / 默认：0.5")
                .defineInRange("fire_frost_melt_damage_mult", 0.5, 0.0, 1.0);
                BUILDER.comment(" ");

        FIRE_FROST_MELT_WETNESS_RATIO = BUILDER
                .comment("赤焰融冰后每层冻结转化为潮湿的倍率。0 = 关闭此功能。",
                         "Ratio per freeze stack to convert to wetness layers after Fire Freeze Melt. 0 = disabled.",
                         "Default: 1 (1:1) / 默认：1（1：1）")
                .defineInRange("fire_frost_melt_wetness_ratio", 1, 0, 10);

        BUILDER.pop();

        BUILDER.pop();

        BUILDER.comment("冻结+易燃孢子（冻结清除孢子） - Freeze + Spores",
                        "冻结状态的生物无法获得易燃孢子效果，已有孢子会在进入冻结时被清除。",
                        "此机制防止冻结+孢子的组合被利用（冻结期间无法触发毒火爆燃）。",
                        "Frozen entities cannot gain Flammable Spores; existing spores are cleared on freeze.",
                        "This prevents the Freeze + Spores combo from being exploited (Toxic Blast cannot trigger during Freeze).")
                .push("freeze_spores");

        FREEZE_CLEAR_SPORES_ENABLED = BUILDER
                .comment("启用后，冻结状态的生物无法获得易燃孢子效果，已有孢子会在进入冻结时被清除。",
                         "When enabled, frozen entities cannot gain Flammable Spores. Existing spores are cleared on freeze.",
                         "Default: true / 默认：true")
                .define("freeze_clear_spores_enabled", true);

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
    public static double staticAuraHeightCeiling;

    public static double thunderCounterBloodThreshold;
    public static int thunderCounterStrengthThreshold;
    public static double thunderCounterHealthRecoveryThreshold;
    public static double thunderCounterRadius;
    public static double thunderCounterExpansionSpeed;
    public static int thunderCounterStrikeInterval;
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

    public static int frostbiteAuraThreshold;
    public static double frostbiteAuraBaseRange;
    public static double frostbiteAuraRangePerStack;

    public static int freezeMaxStacks;
    public static int freezeDurationPerStackTicks;

    public static double freezeSettlementDamagePerStack;
    public static int freezeCooldownTicks;
    public static List<? extends String> cachedFreezeImmunityBlacklist;

    public static double thunderBreakFreezeChance;
    public static int thunderBreakFreezeToWetnessRatio;

    public static int frostbiteFireSteamThreshold;
    public static double fireFrostMeltDamageMult;
    public static int fireFrostMeltWetnessRatio;

    public static double frostbiteSporeDecaySpeed;
    public static boolean freezeClearSporesEnabled;
    public static int scorchedFrostbiteToWetnessRatio;
    public static boolean frostbiteClearByHeatEnabled;
    public static boolean frostbiteNetherClearEnabled;
    public static double frostbiteNetherDecaySpeed;
    public static double frostbiteHeatSearchRadius;
    public static int frostbiteFireStandClearingTime;

    public static int staticSteamCloudTriggerStacks;

    public static double waterElectrificationRangeBase;
    public static double waterElectrificationRangePerStack;
    public static int waterElectrificationParalysisDuration;

    public static int frostedSteamCloudTriggerStacks;

    public static boolean frostCounterEnabled;
    public static double frostCounterBloodThreshold;
    public static int frostCounterStrengthThreshold;
    public static double frostCounterHealthRecoveryThreshold;
    public static double frostCounterMaxRadius;
    public static double frostCounterExpansionSpeed;
    public static double frostCounterHeightCeiling;

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
        staticAuraHeightCeiling = STATIC_AURA_HEIGHT_CEILING.get();

        thunderCounterBloodThreshold = THUNDER_COUNTER_BLOOD_THRESHOLD.get();
        thunderCounterStrengthThreshold = THUNDER_COUNTER_STRENGTH_THRESHOLD.get();
        thunderCounterHealthRecoveryThreshold = THUNDER_COUNTER_HEALTH_RECOVERY_THRESHOLD.get();
        thunderCounterRadius = THUNDER_COUNTER_RADIUS.get();
        thunderCounterExpansionSpeed = THUNDER_COUNTER_EXPANSION_SPEED.get();
        thunderCounterStrikeInterval = THUNDER_COUNTER_STRIKE_INTERVAL.get();
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

        frostbiteAuraThreshold = FROSTBITE_AURA_THRESHOLD.get();
        frostbiteAuraBaseRange = FROSTBITE_AURA_BASE_RANGE.get();
        frostbiteAuraRangePerStack = FROSTBITE_AURA_RANGE_PER_STACK.get();

        freezeMaxStacks = FREEZE_MAX_STACKS.get();
        freezeDurationPerStackTicks = FREEZE_DURATION_PER_STACK_TICKS.get();

        freezeSettlementDamagePerStack = FREEZE_SETTLEMENT_DAMAGE_PER_STACK.get();
        freezeCooldownTicks = FREEZE_COOLDOWN_TICKS.get();
        cachedFreezeImmunityBlacklist = FREEZE_IMMUNITY_BLACKLIST.get();

        thunderBreakFreezeChance = THUNDER_BREAK_FREEZE_CHANCE.get();
        thunderBreakFreezeToWetnessRatio = THUNDER_BREAK_FREEZE_TO_WETNESS_RATIO.get();

        frostbiteSporeDecaySpeed = FROSTBITE_SPORE_DECAY_SPEED.get();
        freezeClearSporesEnabled = FREEZE_CLEAR_SPORES_ENABLED.get();
        scorchedFrostbiteToWetnessRatio = SCORCHED_FROSTBITE_TO_WETNESS_RATIO.get();
        frostbiteClearByHeatEnabled = FROSTBITE_CLEAR_BY_HEAT_ENABLED.get();
        frostbiteNetherClearEnabled = FROSTBITE_NETHER_CLEAR_ENABLED.get();
        frostbiteNetherDecaySpeed = FROSTBITE_NETHER_DECAY_SPEED.get();
        frostbiteHeatSearchRadius = FROSTBITE_HEAT_SEARCH_RADIUS.get();
        frostbiteFireStandClearingTime = FROSTBITE_FIRE_STAND_CLEARING_TIME.get();

        staticSteamCloudTriggerStacks = STATIC_STEAM_CLOUD_TRIGGER_STACKS.get();

        waterElectrificationRangeBase = WATER_ELECTRIFICATION_RANGE_BASE.get();
        waterElectrificationRangePerStack = WATER_ELECTRIFICATION_RANGE_PER_STACK.get();
        waterElectrificationParalysisDuration = WATER_ELECTRIFICATION_PARALYSIS_DURATION.get();

        frostedSteamCloudTriggerStacks = FROSTED_STEAM_CLOUD_TRIGGER_STACKS.get();

        frostbiteFireSteamThreshold = FROSTBITE_FIRE_STEAM_THRESHOLD.get();
        fireFrostMeltDamageMult = FIRE_FROST_MELT_DAMAGE_MULT.get();
        fireFrostMeltWetnessRatio = FIRE_FROST_MELT_WETNESS_RATIO.get();

        frostCounterEnabled = FROST_COUNTER_ENABLED.get();
        frostCounterBloodThreshold = FROST_COUNTER_BLOOD_THRESHOLD.get();
        frostCounterStrengthThreshold = FROST_COUNTER_STRENGTH_THRESHOLD.get();
        frostCounterHealthRecoveryThreshold = FROST_COUNTER_HEALTH_RECOVERY_THRESHOLD.get();
        frostCounterMaxRadius = FROST_COUNTER_MAX_RADIUS.get();
        frostCounterExpansionSpeed = FROST_COUNTER_EXPANSION_SPEED.get();
        frostCounterHeightCeiling = FROST_COUNTER_HEIGHT_CEILING.get();
    }

    private ElementalThunderFrostReactionsConfig() {}
}
