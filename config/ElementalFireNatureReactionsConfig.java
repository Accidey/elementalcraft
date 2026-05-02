package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import java.util.List;

public class ElementalFireNatureReactionsConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue WETNESS_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue WETNESS_SHALLOW_WATER_CAP_RATIO;
    public static final ForgeConfigSpec.DoubleValue WETNESS_FIRE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue WETNESS_MAX_REDUCTION;
    public static final ForgeConfigSpec.IntValue WETNESS_RAIN_GAIN_INTERVAL;
    public static final ForgeConfigSpec.IntValue WETNESS_DECAY_BASE_TIME;
    public static final ForgeConfigSpec.DoubleValue WETNESS_EXHAUSTION_INCREASE;
    public static final ForgeConfigSpec.IntValue WETNESS_POTION_ADD_LEVEL;
    public static final ForgeConfigSpec.IntValue WETNESS_DRYING_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue WETNESS_SELF_DRYING_DAMAGE_PENALTY;
    public static final ForgeConfigSpec.IntValue WETNESS_FIRE_DRYING_TIME;
    public static final ForgeConfigSpec.IntValue WETNESS_TICK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue WETNESS_HEAT_SEARCH_RADIUS;
    public static final ForgeConfigSpec.BooleanValue WETNESS_WATER_ANIMAL_IMMUNE;
    public static final ForgeConfigSpec.BooleanValue WETNESS_NETHER_DIMENSION_IMMUNE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WETNESS_ENTITY_BLACKLIST;
    public static final ForgeConfigSpec.IntValue SPORE_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue SPORE_REACTION_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SPORE_POISON_DAMAGE;
    public static final ForgeConfigSpec.IntValue SPORE_DAMAGE_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue SPORE_SPEED_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SPORE_PHYS_RESIST;
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_VULN_PER_STACK;
    public static final ForgeConfigSpec.IntValue SPORE_DURATION_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue SPORE_THUNDER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_DURATION_REDUCTION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SPORE_ENTITY_BLACKLIST;
    public static final ForgeConfigSpec.IntValue CONTAGION_CHECK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_BASE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_RADIUS_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_INTENSITY_RATIO;
    public static final ForgeConfigSpec.BooleanValue CONTAGION_ONLY_HOSTILE;
    public static final ForgeConfigSpec.BooleanValue CONTAGION_ALLOW_INFECTED_SPREAD;
    public static final ForgeConfigSpec.BooleanValue CONTAGION_ALLOW_REINFECTED;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_BASE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_SCALING_STEP;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_SCALING_CHANCE;
    public static final ForgeConfigSpec.IntValue NATURE_PARASITE_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_WETNESS_BONUS;
    public static final ForgeConfigSpec.IntValue NATURE_IMMUNITY_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.IntValue WILDFIRE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_VERTICAL_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue WILDFIRE_SPORE_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue WILDFIRE_CLEAR_BURNING;
    public static final ForgeConfigSpec.DoubleValue BLAST_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue BLAST_WEAK_IGNITE_MULT;
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_DMG_STEP;
    public static final ForgeConfigSpec.DoubleValue BLAST_DMG_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_RANGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_RANGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_SCORCH_BASE;
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_SCORCH_TIME;
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_SCORCH_TIME;
    public static final ForgeConfigSpec.BooleanValue BLAST_CHAIN_REACTION;
    public static final ForgeConfigSpec.DoubleValue BLAST_MAX_BLAST_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue BLAST_MAX_GENERAL_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue ENCHANTMENT_CALCULATION_DENOMINATOR;
    public static final ForgeConfigSpec.BooleanValue STEAM_REACTION_ENABLED;
    public static final ForgeConfigSpec.IntValue STEAM_HIGH_HEAT_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_LOW_HEAT_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_RADIUS;
    public static final ForgeConfigSpec.DoubleValue STEAM_RADIUS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_CLOUD_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_DURATION_PER_LEVEL;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;
    public static final ForgeConfigSpec.IntValue STEAM_CHECK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_HEIGHT_CEILING;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_STEP_FIRE;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_STEP_FROST;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DELAY;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DURATION_BASE;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DURATION_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_SPORE_GROWTH_RATE;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_SCALE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_WEAKNESS;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_SPORE;
    public static final ForgeConfigSpec.IntValue STEAM_IMMUNITY_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STEAM_IMMUNITY_BLACKLIST;
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_THRESHOLD_FIRE;
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_THRESHOLD_FROST;
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_FLOOR_RATIO;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_FIRE_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_GENERAL_PROT_CAP;
    public static final ForgeConfigSpec.IntValue SCORCHED_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_CHANCE_PER_POINT;
    public static final ForgeConfigSpec.IntValue SCORCHED_DURATION;
    public static final ForgeConfigSpec.IntValue SCORCHED_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_DAMAGE_BASE;
    public static final ForgeConfigSpec.IntValue SCORCHED_DAMAGE_SCALING_STEP;
    public static final ForgeConfigSpec.IntValue SCORCHED_RESIST_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_IMMUNE_MODIFIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_FIRE_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_SHOCK_DAMAGE_RATIO;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_GEN_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_NATURE_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_FROST_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCORCHED_ENTITY_BLACKLIST;

    static {
    ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    BUILDER.comment("Elemental Reaction System Configuration",
            "元素反应系统配置 - 调整赤焰和自然元素交互的平衡性。",
            "Elemental Reaction System Configuration - Adjust balances for fire and nature interactions.")
            .push("wetness_system");
    BUILDER.comment("潮湿系统 - Wetness System",
            "生物在接触水、雨或蒸汽时会获得“潮湿”效果，潮湿层数越高，受到的赤焰伤害越低，但同时会加速饱食度消耗。",
            "当拥有足够赤焰强化点数的生物攻击潮湿目标时，可能触发高温蒸汽反应。",
            "Entities gain Wetness stacks when in contact with water, rain, or steam. Higher stacks reduce Fire damage taken but increase hunger exhaustion.",
            "Attacking a wet target with sufficient Fire points may trigger High-Heat Steam reaction.");
    BUILDER.comment("");

    WETNESS_MAX_LEVEL = BUILDER
            .comment("潮湿状态的最高叠加层数。",
                    "The maximum stack amount for Wetness status.",
                    "Default: 5")
            .defineInRange("wetness_max_level", 5, 1, 1000);
    BUILDER.comment("");

    WETNESS_SHALLOW_WATER_CAP_RATIO = BUILDER
            .comment("在浅水中（仅脚部接触水）时，获得的潮湿层数上限比例。(0.6 = 最大值的60%)",
                    "The cap ratio for Wetness stacks when in shallow water (feet only). (0.6 = 60% of max)",
                    "Default: 0.6")
            .defineInRange("wetness_shallow_water_cap_ratio", 0.6, 0.0, 1.0);
    BUILDER.comment("");

    WETNESS_FIRE_REDUCTION = BUILDER
            .comment("每一层潮湿抵挡赤焰属性伤害的百分比。(0.1 = 10% 减伤)",
                    "Percentage of fire damage reduction provided by each stack of Wetness. (0.1 = 10% reduction)",
                    "Default: 0.1")
            .defineInRange("wetness_fire_reduction", 0.1, 0.0, 1.0);
    BUILDER.comment("");

    WETNESS_MAX_REDUCTION = BUILDER
            .comment("潮湿状态提供的最大赤焰属性伤害减免比例。(0.9 = 90%)",
                    "Maximum fire damage reduction provided by Wetness status. (0.9 = 90%)",
                    "Default: 0.9")
            .defineInRange("wetness_max_reduction", 0.9, 0.0, 1.0);
    BUILDER.comment("");

    WETNESS_RAIN_GAIN_INTERVAL = BUILDER
            .comment("在雨中站立时，增加一层潮湿所需的时间（秒）。",
                    "Time (in seconds) required to gain a Wetness stack while standing in rain.",
                    "Default: 10")
            .defineInRange("wetness_rain_gain_interval", 10, 1, 3600);
    BUILDER.comment("");

    WETNESS_DECAY_BASE_TIME = BUILDER
            .comment("离开水源后，每一层潮湿自然消退所需的基础时间（秒）。",
                    "Base time (in seconds) for each Wetness stack to decay naturally after leaving water.",
                    "Default: 10")
            .defineInRange("wetness_decay_base_time", 10, 1, 3600);
    BUILDER.comment("");

    WETNESS_EXHAUSTION_INCREASE = BUILDER
            .comment("潮湿状态下，饥饿感（饱食度消耗）的增加倍率。",
                    "Multiplier for hunger exhaustion when under Wetness status.",
                    "Default: 0.05")
            .defineInRange("wetness_exhaustion_increase", 0.05, 0.0, 10.0);
    BUILDER.comment("");

    WETNESS_POTION_ADD_LEVEL = BUILDER
            .comment("被喷溅水瓶击中时，瞬间增加的潮湿层数。",
                    "Number of Wetness stacks instantly added when hit by a Splash Water Bottle.",
                    "Default: 1")
            .defineInRange("wetness_potion_add_level", 1, 1, 100);
    BUILDER.comment("");

    WETNESS_DRYING_THRESHOLD = BUILDER
            .comment("瞬间蒸发1层潮湿所需的赤焰属性强化点数阈值。",
                    "Threshold of Fire points required to instantly evaporate 1 layer of Wetness.",
                    "Default: 20")
            .defineInRange("wetness_drying_threshold", 20, 1, 1000);
    BUILDER.comment("");

    WETNESS_SELF_DRYING_DAMAGE_PENALTY = BUILDER
            .comment("赤焰生物自我蒸干潮湿时，造成的伤害降低比例。(0.3 = 降低30%)",
                    "Damage reduction penalty when a Fire entity tries to dry itself. (0.3 = 30% reduction)",
                    "Default: 0.3")
            .defineInRange("wetness_self_drying_damage_penalty", 0.3, 0.0, 1.0);
    BUILDER.comment("");

    WETNESS_FIRE_DRYING_TIME = BUILDER
            .comment("站在火中烧多少秒可瞬间清除所有潮湿效果。",
                    "Seconds required to stand in fire to instantly clear all Wetness effects.",
                    "Default: 2")
            .defineInRange("wetness_fire_drying_time", 2, 1, 600);
    BUILDER.comment("");

    WETNESS_TICK_INTERVAL = BUILDER
            .comment("潮湿状态逻辑更新的间隔（Tick）。",
                    "Interval (Ticks) for updating Wetness logic (decay/accumulation).",
                    "Default: 20")
            .defineInRange("wetness_tick_interval", 20, 1, 1200);
    BUILDER.comment("");

    WETNESS_HEAT_SEARCH_RADIUS = BUILDER
            .comment("检测周围热源（熔岩/岩浆块）的半径范围（格）。注意：岩浆块的检测半径会-1格。",
                    "Radius (blocks) to search for nearby heat sources (Lava/Magma). Note: Magma Block detection radius is automatically reduced by 1.",
                    "Default: 2.0")
            .defineInRange("wetness_heat_search_radius", 2.0, 1.0, 16.0);

    BUILDER.push("immunity");
    WETNESS_WATER_ANIMAL_IMMUNE = BUILDER
            .comment("水生生物（如鱼、鱿鱼）是否完全免疫潮湿效果？",
                    "Are water animals (e.g., fish, squids) completely immune to Wetness?",
                    "Default: true")
            .define("water_animal_immune", true);
    BUILDER.comment("");

    WETNESS_NETHER_DIMENSION_IMMUNE = BUILDER
            .comment("下界维度的生物是否天生免疫潮湿效果？",
                    "Are entities in the Nether dimension naturally immune to Wetness?",
                    "Default: true")
            .define("nether_dimension_immune", true);
    BUILDER.comment("");

    WETNESS_ENTITY_BLACKLIST = BUILDER
            .comment("潮湿效果免疫黑名单（填入实体ID）。",
                    "Wetness immunity blacklist (Entity IDs).",
                    "Default: []")
            .defineListAllowEmpty("wetness_entity_blacklist", List.of(), o -> o instanceof String);
    BUILDER.pop();
    BUILDER.pop();

    BUILDER.push("steam_reaction");
    BUILDER.comment("蒸汽反应系统 - Steam Reaction System",
            "当赤焰属性攻击潮湿目标（或冰霜属性攻击赤焰目标）时，会在目标位置生成一团蒸汽云。",
            "蒸汽云分为高温（烫伤）和低温（冷凝/繁殖孢子）两种，其等级、范围和伤害受双方属性点数及潮湿/孢子层数影响。",
            "Steam clouds are created when Fire attacks Wetness (or Frost attacks Fire).",
            "They come in High-Heat (scalding) and Low-Heat (condensation/spore growth) varieties.");
    BUILDER.comment("");

    STEAM_REACTION_ENABLED = BUILDER
            .comment("是否开启蒸汽反应机制（赤焰属性与冰霜属性或潮湿目标相遇产生蒸汽）？",
                    "Whether to enable the Steam Reaction mechanism (Fire meets Frost or Wetness).",
                    "Default: true")
            .define("steam_reaction_enabled", true);
    BUILDER.comment("");

    STEAM_HIGH_HEAT_MAX_LEVEL = BUILDER
            .comment("高温蒸汽云（赤焰属性攻击潮湿目标）的最高等级。",
                    "Maximum level for High-Heat Steam clouds (Fire attacks Wetness).",
                    "Default: 5")
            .defineInRange("steam_high_heat_max_level", 5, 1, 10000);
    BUILDER.comment("");

    STEAM_LOW_HEAT_MAX_LEVEL = BUILDER
            .comment("低温蒸汽云（冰霜属性攻击赤焰属性目标）的最高等级。",
                    "Maximum level for Low-Heat Steam clouds (Frost attacks Fire).",
                    "Default: 5")
            .defineInRange("steam_low_heat_max_level", 5, 1, 10000);

    BUILDER.push("cloud_properties");
    BUILDER.comment("蒸汽云属性 - Cloud Properties",
            "控制蒸汽云的基础半径、持续时间、检测间隔以及上升高度等物理特性。",
            "Controls base radius, duration, scan interval, and vertical ceiling of steam clouds.");
    BUILDER.comment("");

    STEAM_CLOUD_RADIUS = BUILDER
            .comment("蒸汽云的基础半径（格）。",
                    "Base radius (blocks) for steam clouds.",
                    "Default: 2.0")
            .defineInRange("steam_cloud_radius", 2.0, 0.5, 1000.0);
    BUILDER.comment("");

    STEAM_RADIUS_PER_LEVEL = BUILDER
            .comment("每增加一级（潮湿层数），蒸汽云增加的半径（格）。",
                    "Additional radius (blocks) per steam level (wetness stack).",
                    "Default: 0.5")
            .defineInRange("steam_radius_per_level", 0.5, 0.0, 5.0);
    BUILDER.comment("");

    STEAM_CLOUD_DURATION = BUILDER
            .comment("蒸汽云的基础存在时间（Tick）。",
                    "Base duration (Ticks) for steam clouds.",
                    "Default: 100")
            .defineInRange("steam_cloud_duration", 100, 20, 12000);
    BUILDER.comment("");

    STEAM_DURATION_PER_LEVEL = BUILDER
            .comment("每增加一级，蒸汽云增加的存在时间（Tick）。",
                    "Additional duration (Ticks) per steam level.",
                    "Default: 20")
            .defineInRange("steam_duration_per_level", 20, 0, 2000);
    BUILDER.comment("");

    STEAM_CLEAR_AGGRO = BUILDER
            .comment("蒸汽云是否会遮蔽视线，强制清除里面怪物的仇恨目标？",
                    "Whether steam clouds obscure vision and force mobs inside to lose aggro.",
                    "Default: true")
            .define("steam_clear_aggro", true);
    BUILDER.comment("");

    STEAM_CHECK_INTERVAL = BUILDER
            .comment("实体检测周围蒸汽云的间隔（Tick）。",
                    "Interval (Ticks) for entities to scan for nearby steam clouds.",
                    "Default: 10")
            .defineInRange("steam_check_interval", 10, 1, 1200);
    BUILDER.comment("");

    STEAM_CLOUD_HEIGHT_CEILING = BUILDER
            .comment("蒸汽云向上飘散的有效高度（格）。超过这个高度不造成烫伤。",
                    "Effective height ceiling (blocks) for rising steam clouds. No scalding above this height.",
                    "Default: 3.0")
            .defineInRange("steam_cloud_height_ceiling", 3.0, 0.0, 16.0);
    BUILDER.pop();

    BUILDER.push("condensation_logic");
    BUILDER.comment("冷凝逻辑 - Condensation Logic",
            "低温蒸汽云的特殊行为：包括基于属性点数提升蒸汽等级、使内部生物获得潮湿效果、以及促进易燃孢子繁殖。",
            "Special behaviors for Low-Heat Steam: level scaling based on elemental points, applying Wetness to entities inside, and accelerating Flammable Spore growth.");
    BUILDER.comment("");

    STEAM_CONDENSATION_STEP_FROST = BUILDER
            .comment("冰霜属性强化点数对蒸汽等级的影响步长。",
                    "在低温蒸汽中，攻击者每拥有这么多冰霜属性强化点数，蒸汽等级增加1。",
                    "Frost points required to increase steam level by one.",
                    "In Low-Heat Steam : each multiple of this value adds 1 level.",
                    "Default: 20")
            .defineInRange("steam_condensation_step_frost", 20, 1, 10000);
    BUILDER.comment("");

    STEAM_CONDENSATION_STEP_FIRE = BUILDER
            .comment("赤焰属性强化点数对蒸汽等级的影响步长。",
                    "在高温蒸汽云中，攻击者每拥有这么多赤焰属性强化点数，蒸汽等级增加1。",
                    "在低温蒸汽云中，目标每拥有这么多赤焰属性强化点数，蒸汽等级增加1（目标为赤焰属性时）。",
                    "Fire points required to increase steam level by one.",
                    "In High-Heat Steam : each multiple of this value adds 1 level.",
                    "In Low-Heat Steam: each multiple of this value adds 1 level (when target is Fire).",
                    "Default: 20")
            .defineInRange("steam_condensation_step_fire", 20, 1, 10000);
    BUILDER.comment("");

    STEAM_CONDENSATION_DELAY = BUILDER
            .comment("在低温蒸汽里待多久（Tick）才会获得潮湿效果？",
                    "Time (Ticks) required to stay in Low-Heat Steam to gain Wetness.",
                    "Default: 100")
            .defineInRange("steam_condensation_delay", 100, 1, 24000);
    BUILDER.comment("");

    STEAM_CONDENSATION_DURATION_BASE = BUILDER
            .comment("低温蒸汽云的基础持续时间（Tick）。",
                    "Base duration (Ticks) for Low-Heat Steam clouds.",
                    "Default: 100")
            .defineInRange("steam_condensation_duration_base", 100, 20, 12000);
    BUILDER.comment("");

    STEAM_CONDENSATION_DURATION_PER_LEVEL = BUILDER
            .comment("低温蒸汽每升一级增加的持续时间（Tick）。",
                    "Additional duration (Ticks) per level for Low-Heat Steam.",
                    "Default: 20")
            .defineInRange("steam_condensation_duration_per_level", 20, 0, 2000);
    BUILDER.comment("");

    STEAM_SPORE_GROWTH_RATE = BUILDER
            .comment("在低温蒸汽中，易燃孢子繁殖的间隔时间（Tick）。",
                    "Interval (Ticks) for Flammable Spore reproduction inside Low-Heat Steam.",
                    "Default: 20")
            .defineInRange("steam_spore_growth_rate", 20, 1, 6000);
    BUILDER.pop();

    BUILDER.push("scalding_damage");
    BUILDER.comment("烫伤伤害 - Scalding Damage",
            "高温蒸汽云对内部生物造成的持续伤害。伤害受蒸汽等级、目标弱点（冰霜/自然/携带孢子）以及抗性/附魔影响。",
            "Damage over time dealt by High-Heat Steam. Scales with steam level and multipliers for vulnerable targets (Frost/Nature/Spore carriers).");
    BUILDER.comment("");

    STEAM_SCALDING_DAMAGE = BUILDER
            .comment("高温蒸汽每秒造成的基础烫伤伤害。",
                    "Base scalding damage per second from High-Heat Steam.",
                    "Default: 1.0")
            .defineInRange("steam_scalding_damage", 1.0, 0.0, 10000.0);
    BUILDER.comment("");

    STEAM_DAMAGE_SCALE_PER_LEVEL = BUILDER
            .comment("蒸汽等级每升一级，烫伤伤害增加的比例（1级无加成）。例如：0.2 = 每升一级+20%，2级为120%，3级为140%",
                    "Percentage increase per steam level (level 1 has no bonus). e.g., 0.2 = +20% per level, level 2 = 120%, level 3 = 140%",
                    "Default: 0.2")
            .defineInRange("steam_damage_scale_per_level", 0.2, 0.0, 10.0);
    BUILDER.comment("");

    STEAM_SCALDING_MULTIPLIER_WEAKNESS = BUILDER
            .comment("冰霜/自然属性生物受到高温蒸汽伤害的**乘法倍率**。",
                    "该倍率作用于计算完蒸汽等级加成后的烫伤伤害（最终伤害再乘以此值）。",
                    "例：1.5 = 造成 150% 的蒸汽烫伤伤害。与孢子倍率乘算叠加。",
                    "Multiplicative damage multiplier for Frost/Nature entities. ",
                    "Applied after steam level scaling (multiplies the final scalding damage). ",
                    "e.g., 1.5 = 150% scalding damage. Stacks multiplicatively with spore multiplier.",
                    "Default: 1.5")
            .defineInRange("steam_scalding_multiplier_weakness", 1.5, 1.0, 1000.0);
    BUILDER.comment("");

    STEAM_SCALDING_MULTIPLIER_SPORE = BUILDER
            .comment("携带易燃孢子的生物受到蒸汽烫伤伤害的**乘法倍率**。",
                    "该倍率作用于计算完蒸汽等级加成后的烫伤伤害（最终伤害再乘以此值）。",
                    "例：1.5 = 造成 150% 的蒸汽烫伤伤害。与弱点倍率乘算叠加。",
                    "Multiplicative damage multiplier for entities with Flammable Spores. ",
                    "Applied after steam level scaling. ",
                    "e.g., 1.5 = 150% scalding damage. Stacks multiplicatively with weakness multiplier.",
                    "Default: 1.5")
            .defineInRange("steam_scalding_multiplier_spore", 1.5, 1.0, 1000.0);
    BUILDER.comment("");

    STEAM_IMMUNITY_THRESHOLD = BUILDER
            .comment("完全免疫蒸汽烫伤所需的赤焰抗性点数。",
                    "Fire Resistance points required to be completely immune to steam scalding.",
                    "Default: 80")
            .defineInRange("steam_immunity_threshold", 80, 0, 1000);
    BUILDER.comment("");

    STEAM_IMMUNITY_BLACKLIST = BUILDER
            .comment("蒸汽烫伤免疫黑名单（填入实体ID）。",
                    "Steam scalding immunity blacklist (Entity IDs).",
                    "Default: []")
            .defineListAllowEmpty("steam_immunity_blacklist", List.of(), o -> o instanceof String);
    BUILDER.pop();

    BUILDER.push("trigger_logic");
    BUILDER.comment("触发逻辑 - Trigger Logic",
            "定义高温/低温蒸汽反应触发所需的最小属性点数、触发后的冷却时间，以及附魔减伤相关的通用计算参数。",
            "Defines minimum elemental points to trigger steam reactions, cooldowns, and shared enchantment protection parameters.");
    BUILDER.comment("");

    STEAM_TRIGGER_THRESHOLD_FIRE = BUILDER
            .comment("攻击时，触发高温蒸汽所需的最小赤焰属性强化点数。",
                    "Minimum Fire points required to trigger High-Heat Steam (evaporate water/ice).",
                    "Default: 20")
            .defineInRange("fire_trigger_threshold", 20, 0, 1000);
    BUILDER.comment("");

    STEAM_TRIGGER_THRESHOLD_FROST = BUILDER
            .comment("攻击时，触发低温蒸汽所需的最小冰霜属性强化点数。",
                    "Minimum Frost points required to trigger Low-Heat Steam (cool down fire).",
                    "Default: 20")
            .defineInRange("frost_trigger_threshold", 20, 0, 1000);
    BUILDER.comment("");

    STEAM_TRIGGER_COOLDOWN = BUILDER
            .comment("触发蒸汽反应后的冷却时间（Tick）。",
                    "Cooldown (Ticks) applied to an entity after triggering a steam reaction.",
                    "Default: 200")
            .defineInRange("steam_trigger_cooldown", 200, 0, 6000);
    BUILDER.comment("");

    ENCHANTMENT_CALCULATION_DENOMINATOR = BUILDER
            .comment("用于计算附魔保护比例的分母，所有可以用附魔减伤的元素反应都使用此值。",
                    "默认值 16.0 意味着需要4件装备都附魔保护IV（4 * 4 = 16）才能达到设定的最大减伤比例。",
                    "The denominator used to calculate enchantment protection ratios. ",
                    "This value is shared by all elemental reactions that support enchantment-based damage reduction.",
                    "Default 16.0 means full protection (100% weight) requires 4 armor pieces with Protection IV (4 * 4 = 16).",
                    "Default: 16.0")
            .defineInRange("enchantment_calculation_denominator", 16.0, 1.0, 1000.0);
    BUILDER.comment("");

    STEAM_MAX_FIRE_PROT_CAP = BUILDER
            .comment("火焰保护附魔最多能抵消的蒸汽伤害比例。(0.5 = 50%)",
                    "Maximum steam damage mitigation provided by 'Fire Protection' enchantment. (0.5 = 50%)",
                    "Default: 0.5")
            .defineInRange("max_fire_prot_cap", 0.5, 0.0, 1.0);
    BUILDER.comment("");

    STEAM_MAX_GENERAL_PROT_CAP = BUILDER
            .comment("普通保护附魔最多能抵消的蒸汽伤害比例。(0.25 = 25%)",
                    "Maximum steam damage mitigation provided by general 'Protection' enchantment. (0.25 = 25%)",
                    "Default: 0.25")
            .defineInRange("max_general_prot_cap", 0.25, 0.0, 1.0);
    BUILDER.comment("");

    STEAM_DAMAGE_FLOOR_RATIO = BUILDER
            .comment("冰霜/自然属性生物的蒸汽伤害保底比例。",
                    "无论附魔提供多少减伤，它们至少要承受原始伤害的这一比例。（0.1 = 10%）",
                    "Minimum damage floor ratio for vulnerable entities (Frost/Nature), regardless of resistance. (0.1 = 10%)",
                    "Default: 0.1")
            .defineInRange("damage_floor_ratio", 0.1, 0.0, 1.0);
    BUILDER.comment("");

    STEAM_MAX_REDUCTION = BUILDER
            .comment("蒸汽烫伤减伤上限比例。(0.9 = 90%)",
                    "Maximum damage reduction for steam scalding. (0.9 = 90%)",
                    "Default: 0.9")
            .defineInRange("steam_max_reduction", 0.9, 0.0, 1.0);
    BUILDER.pop();

    BUILDER.pop();

    BUILDER.push("scorched_mechanic");
    BUILDER.comment("灼烧机制 - Scorched Mechanic",
            "赤焰属性攻击有一定几率使目标陷入“灼烧”状态，造成持续火焰伤害。",
            "灼烧持续时间受目标属性影响（自然生物更长，冰霜生物更短），且遇水（潮湿/蒸汽）会触发“热休克”立即结算部分伤害。",
            "Fire attacks have a chance to inflict 'Scorched', a fire-based DoT.",
            "Duration varies by target element (longer on Nature, shorter on Frost). Contact with water/steam triggers 'Thermal Shock', dealing instant damage.");
    BUILDER.comment("");

    SCORCHED_TRIGGER_THRESHOLD = BUILDER
            .comment("攻击触发“灼烧”效果所需的最小赤焰属性强化点数。",
                    "Minimum Fire points required to trigger 'Scorched' effect on attack.",
                    "Default: 20")
            .defineInRange("scorched_trigger_threshold", 20, 1, 10000);
    BUILDER.comment("");

    SCORCHED_BASE_CHANCE = BUILDER
            .comment("触发灼烧的基础概率。(0.2 = 20%)",
                    "Base chance to trigger Scorched effect. (0.2 = 20%)",
                    "Default: 0.2")
            .defineInRange("scorched_base_chance", 0.2, 0.0, 1.0);
    BUILDER.comment("");

    SCORCHED_CHANCE_PER_POINT = BUILDER
            .comment("每点赤焰属性额外增加的灼烧触发概率（加法叠加）。",
                    "总概率 = scorched_base_chance + (赤焰属性点数 × scorched_chance_per_point)。",
                    "最终概率不会超过 1.0（100%）。",
                    "例如：基础概率 0.2，每点加成 0.008，则拥有 100 点赤焰属性时总概率为 0.2 + 0.8 = 1.0（100%）。",
                    "Chance increase per Fire point (additive).",
                    "Total chance = scorched_base_chance + (Fire points × scorched_chance_per_point).",
                    "Capped at 1.0 (100%).",
                    "Default: 0.008")
            .defineInRange("scorched_chance_per_point", 0.008, 0.0, 0.1);
    BUILDER.comment("");

    SCORCHED_DURATION = BUILDER
            .comment("灼烧状态的持续时间（Tick）。(100 Tick = 5秒)",
                    "Duration (Ticks) of the Scorched effect. (100 Ticks = 5 seconds)",
                    "Default: 100")
            .defineInRange("scorched_duration", 100, 20, 12000);
    BUILDER.comment("");

    SCORCHED_COOLDOWN = BUILDER
            .comment("灼烧效果触发后的冷却时间（Tick）。",
                    "Cooldown (Ticks) after triggering the Scorched effect.",
                    "Default: 200")
            .defineInRange("scorched_cooldown", 200, 0, 6000);
    BUILDER.comment("");

    SCORCHED_DAMAGE_BASE = BUILDER
            .comment("灼烧每秒造成的基础伤害。",
                    "Base damage per second from Scorched effect.",
                    "Default: 1.0")
            .defineInRange("scorched_damage_base", 1.0, 0.1, 10000.0);
    BUILDER.comment("");

    SCORCHED_DAMAGE_SCALING_STEP = BUILDER
            .comment("灼烧伤害增加0.5点所需的赤焰属性增量。",
                    "Fire increment required to increase Scorched damage by 0.5.",
                    "Default: 20")
            .defineInRange("scorched_damage_scaling_step", 20, 1, 10000);
    BUILDER.comment("");

    SCORCHED_RESIST_THRESHOLD = BUILDER
            .comment("完全免疫灼烧伤害所需的赤焰抗性点数。",
                    "Fire Resistance points required to be completely immune to Scorched damage.",
                    "Default: 80")
            .defineInRange("scorched_resist_threshold", 80, 1, 10000);
    BUILDER.comment("");

    SCORCHED_IMMUNE_MODIFIER = BUILDER
            .comment("天生免疫火的生物受到灼烧伤害的倍率。(0.5 = 50%)",
                    "Damage multiplier for naturally fire-immune entities when Scorched. (0.5 = 50%)",
                    "Default: 0.5")
            .defineInRange("scorched_immune_modifier", 0.5, 0.0, 1.0);
    BUILDER.comment("");

    SCORCHED_FIRE_PROT_REDUCTION = BUILDER
            .comment("火焰保护附魔最多能抵消的灼烧伤害比例。",
                    "Maximum Scorched damage mitigation provided by 'Fire Protection' enchantment.",
                    "Default: 0.5")
            .defineInRange("scorched_fire_prot_reduction", 0.5, 0.0, 1.0);
    BUILDER.comment("");

    SCORCHED_GEN_PROT_REDUCTION = BUILDER
            .comment("普通保护附魔最多能抵消的灼烧伤害比例。",
                    "Maximum Scorched damage mitigation provided by general 'Protection' enchantment.",
                    "Default: 0.25")
            .defineInRange("scorched_gen_prot_reduction", 0.25, 0.0, 1.0);
    BUILDER.comment("");

    SCORCHED_SHOCK_DAMAGE_RATIO = BUILDER
            .comment("热休克（灼烧遇水）时，剩余持续伤害瞬间结算的比例。(0.5 = 50%)",
                    "Ratio of remaining DOT damage dealt instantly during Thermal Shock. (0.5 = 50%)",
                    "Default: 0.5")
            .defineInRange("scorched_shock_damage_ratio", 0.5, 0.0, 10.0);
    BUILDER.comment("");

    SCORCHED_NATURE_DURATION_MULTIPLIER = BUILDER
            .comment("自然属性生物获得灼烧时，持续时间的倍率（1.5 表示增加 50% 时长）。",
                    "Duration multiplier for Scorched effect on Nature entities (1.5 = +50% duration).",
                    "Default: 1.5")
            .defineInRange("scorched_nature_duration_multiplier", 1.5, 0.1, 100.0);
    BUILDER.comment("");

    SCORCHED_FROST_DURATION_MULTIPLIER = BUILDER
            .comment("冰霜属性生物获得灼烧时，持续时间的倍率（0.5 表示减少 50% 时长）。",
                    "Duration multiplier for Scorched effect on Frost entities (0.5 = -50% duration).",
                    "Default: 0.5")
            .defineInRange("scorched_frost_duration_multiplier", 0.5, 0.1, 100.0);
    BUILDER.comment("");

    SCORCHED_ENTITY_BLACKLIST = BUILDER
            .comment("灼烧效果免疫黑名单。",
                    "Scorched effect immunity blacklist.",
                    "Default: []")
            .defineListAllowEmpty("scorched_entity_blacklist", List.of(), o -> o instanceof String);
    BUILDER.pop();

    BUILDER.push("nature_reaction");
    BUILDER.comment("自然元素反应 - Nature Reaction System",
            "包含易燃孢子的施加、传染、以及被赤焰属性引爆（毒火爆燃）等核心机制。",
            "Includes Flammable Spore application, contagion spread, and detonation by Fire (Toxic Blast).");
    BUILDER.push("dynamic_parasitism");
    BUILDER.comment("动态寄生 - Dynamic Parasitism",
            "控制自然属性攻击施加“易燃孢子”的概率。概率受自然强化点数、攻击者自身的潮湿层数影响。",
            "Controls the chance for Nature attacks to apply Flammable Spores. Chance scales with Nature points and attacker's Wetness stacks.");
    BUILDER.comment("");

    NATURE_PARASITE_BASE_THRESHOLD = BUILDER
            .comment("攻击触发易燃孢子效果所需的最小自然属性强化点数。",
                    "Minimum Nature points required to trigger Flammable Spores effect on attack.",
                    "Default: 5.0")
            .defineInRange("base_threshold", 5.0, 0.0, 10000.0);
    BUILDER.comment("");

    NATURE_PARASITE_BASE_CHANCE = BUILDER
            .comment("触发易燃孢子效果的基础概率。(0.1 = 10%)",
                    "Base chance to trigger Flammable Spores effect. (0.1 = 10%)",
                    "Default: 0.1")
            .defineInRange("base_chance", 0.1, 0.0, 1.0);
    BUILDER.comment("");

    NATURE_PARASITE_SCALING_STEP = BUILDER
            .comment("易燃孢子概率成长的属性阶梯值。例如设为20时，自然属性强化点数达到20/40/60点都会触发概率提升。",
                    "The step size for Flammable Spores chance scaling. E.g., if set to 20, chance increases at 20, 40, 60 points.",
                    "Default: 20.0")
            .defineInRange("scaling_step", 20.0, 1.0, 10000.0);
    BUILDER.comment("");

    NATURE_PARASITE_SCALING_CHANCE = BUILDER
            .comment("每个阶梯（等级）额外增加的易燃孢子触发概率。",
                    "Flammable Spores chance increase per tier.",
                    "Default: 0.1")
            .defineInRange("scaling_chance", 0.1, 0.0, 1.0);
    BUILDER.comment("");

    NATURE_PARASITE_AMOUNT = BUILDER
            .comment("每次触发效果时施加的易燃孢子层数。",
                    "Number of Flammable Spore stacks applied when effect is triggered.",
                    "Default: 1")
            .defineInRange("parasite_amount", 1, 1, 100);
    BUILDER.comment("");

    NATURE_PARASITE_WETNESS_BONUS = BUILDER
            .comment("自身每层潮湿提供的额外易燃孢子触发概率。",
                    "Extra Flammable Spores chance provided per stack of self-wetness.",
                    "Default: 0.1")
            .defineInRange("wetness_bonus", 0.1, 0.0, 1.0);
    BUILDER.comment("");

    NATURE_IMMUNITY_THRESHOLD = BUILDER
            .comment("完全免疫易燃孢子所需的自然抗性点数。",
                    "Nature Resistance points required to be completely immune to Flammable Spores.",
                    "Default: 80")
            .defineInRange("nature_immunity_threshold", 80, 0, 10000);
    BUILDER.pop();

    BUILDER.push("spore_system");
    BUILDER.comment("易燃孢子效果 - Spore Effect",
            "定义孢子层数带来的具体效果：持续伤害、减速、物理减伤、赤焰易伤，以及不同属性宿主对持续时间的影响。",
            "Defines per-stack effects: damage over time, slowness, physical resistance, Fire vulnerability, and duration modifiers for different elemental hosts.");
    BUILDER.comment("");

    SPORE_MAX_STACKS = BUILDER
            .comment("易燃孢子的最大叠加层数。",
                    "Maximum stack amount for Flammable Spores.",
                    "Default: 5")
            .defineInRange("max_spore_stacks", 5, 1, 1000);
    BUILDER.comment("");

    SPORE_POISON_DAMAGE = BUILDER
            .comment("感染易燃孢子后，每次造成的无视护甲伤害。",
                    "Armor-bypassing damage per second when infected with Flammable Spores.",
                    "Default: 2.0")
            .defineInRange("spore_poison_damage", 2.0, 0.0, 200.0);
    BUILDER.comment("");

    SPORE_DAMAGE_INTERVAL = BUILDER
            .comment("易燃孢子伤害触发的间隔（Tick）。默认40 Tick = 2秒。",
                    "Interval (Ticks) for Flammable Spores damage ticks. Default 40 Ticks = 2 seconds.",
                    "Default: 40")
            .defineInRange("spore_damage_interval", 40, 1, 12000);
    BUILDER.comment("");

    SPORE_SPEED_REDUCTION = BUILDER
            .comment("每一层易燃孢子效果造成的减速比例。(0.1 = 10%)",
                    "Percentage of slowness applied per Flammable Spore stack. (0.1 = 10%)",
                    "Default: 0.1")
            .defineInRange("spore_speed_reduction", 0.1, 0.0, 0.5);
    BUILDER.comment("");

    SPORE_PHYS_RESIST = BUILDER
            .comment("每一层易燃孢子提供的物理伤害减免比例。(0.05 = 5%)",
                    "Percentage of physical resistance provided per Flammable Spore stack. (0.05 = 5%)",
                    "Default: 0.05")
            .defineInRange("spore_phys_resist", 0.05, 0.0, 0.5);
    BUILDER.comment("");

    SPORE_FIRE_VULN_PER_STACK = BUILDER
            .comment("每一层孢子增加受到的赤焰属性伤害比例。(0.1 = 10%)",
                    "#Percentage of Fire elemental vulnerability increased per Flammable Spore stack. (0.1 = 10%)",
                    "Default: 0.1")
            .defineInRange("spore_fire_vuln_per_stack", 0.1, 0.0, 1.0);
    BUILDER.comment("");

    SPORE_DURATION_PER_STACK = BUILDER
            .comment("每一层易燃孢子增加的基础持续时间（秒）。",
                    "Base duration (seconds) added per Flammable Spore stack.",
                    "Default: 5")
            .defineInRange("spore_duration_per_stack", 5, 1, 6000);
    BUILDER.comment("");

    SPORE_THUNDER_MULTIPLIER = BUILDER
            .comment("雷霆属性宿主的持续时间倍率。(2.0 = 时间翻倍)",
                    "Duration multiplier for Thunder hosts. (2.0 = Doubled duration)",
                    "Default: 2.0")
            .defineInRange("spore_thunder_multiplier", 2.0, 1.0, 5.0);
    BUILDER.comment("");

    SPORE_FIRE_DURATION_REDUCTION = BUILDER
            .comment("赤焰属性宿主的持续时间缩减比例。(0.5 = 时间减半)",
                    "Duration reduction multiplier for Fire hosts. (0.5 = Halved duration)",
                    "Default: 0.5")
            .defineInRange("spore_fire_duration_reduction", 0.5, 0.0, 1.0);
    BUILDER.comment("");

    SPORE_ENTITY_BLACKLIST = BUILDER
            .comment("易燃孢子效果免疫黑名单（填入实体ID，例如：minecraft:creeper）。",
                    "Flammable Spore immunity blacklist (Entity IDs, e.g., minecraft:creeper).",
                    "Default: []")
            .defineListAllowEmpty("spore_entity_blacklist", List.of(), o -> o instanceof String);
    BUILDER.pop();

    BUILDER.push("contagion_system");
    BUILDER.comment("传染系统 - Contagion System",
            "当生物身上的孢子层数达到阈值后，会周期性向周围生物传播孢子。",
            "When an entity's spore stacks reach the threshold, it periodically spreads spores to nearby entities.");
    BUILDER.comment("");

    SPORE_REACTION_THRESHOLD = BUILDER
            .comment("易燃孢子触发剧烈反应（传染扩散、毒火爆燃）所需的最小层数。",
                    "Minimum Flammable Spore stacks required to trigger severe reactions (Contagion spread, Toxic Blast explosion).",
                    "Default: 3")
            .defineInRange("spore_reaction_threshold", 3, 1, 100);
    BUILDER.comment("");

    CONTAGION_CHECK_INTERVAL = BUILDER
            .comment("感染易燃孢子后触发传染所需时间（Tick）。",
                    "Interval (Ticks) required to trigger contagion after being infected with Flammable Spores.",
                    "Default: 20")
            .defineInRange("contagion_check_interval", 20, 1, 12000);
    BUILDER.comment("");

    CONTAGION_BASE_RADIUS = BUILDER
            .comment("易燃孢子传染的基础半径（格）。",
                    "Base radius (blocks) for Flammable Spore contagion.",
                    "Default: 2.0")
            .defineInRange("contagion_base_radius", 2.0, 1.0, 16.0);
    BUILDER.comment("");

    CONTAGION_RADIUS_PER_STACK = BUILDER
            .comment("高层数时，每多一层增加的传染半径（格）。",
                    "Additional contagion radius (blocks) per extra stack at high levels.",
                    "Default: 1.0")
            .defineInRange("contagion_radius_per_stack", 1.0, 0.0, 5.0);
    BUILDER.comment("");

    CONTAGION_INTENSITY_RATIO = BUILDER
            .comment("传染时，传递给受害者的易燃孢子层数比例。(0.2 = 20%)",
                    "Ratio of Flammable Spore stacks transferred to the victim during contagion. (0.2 = 20%)",
                    "Default: 0.2")
            .defineInRange("contagion_intensity_ratio", 0.2, 0.0, 1.0);
    BUILDER.comment("");

    CONTAGION_ONLY_HOSTILE = BUILDER
            .comment("是否只允许易燃孢子传染给敌对生物？如果为 true，玩家和被动生物将不会被环境传染。",
                    "Whether Flammable Spores should only spread to hostile entities. If true, players and passive mobs will not be infected by contagion.",
                    "Default: false")
            .define("contagion_only_hostile", false);
    BUILDER.comment("");

    CONTAGION_ALLOW_INFECTED_SPREAD = BUILDER
            .comment("被传染的目标在孢子层数达到阈值后是否也能触发传染机制？",
                    "If true, an entity that was infected by contagion can later become a source of contagion itself when its spore stacks reach the threshold.",
                    "If false, only the original source can spread spores; infected entities cannot spread further.",
                    "Default: false")
            .define("contagion_allow_infected_spread", false);
    BUILDER.comment("");

    CONTAGION_ALLOW_REINFECTED = BUILDER
            .comment("当实体的孢子效果完全消失后，若再次获得孢子，是否允许重新触发传染（清除之前的感染标记）？",
                    "If true, an entity can trigger contagion again after its spore effect expires and is reapplied.",
                    "If false, an entity will never trigger contagion again once it has been infected (marked as infected permanently).",
                    "Default: true")
            .define("contagion_allow_reinfected", true);
    BUILDER.pop();

    BUILDER.push("wildfire_ejection");
    BUILDER.comment("野火喷射 - Wildfire Ejection",
            "自然属性生物被点燃时的一种反击机制：瞬间清除自身火焰并爆发出孢子云雾，击退周围敌人并施加孢子。",
            "A counter-attack for Nature entities when ignited: clears own fire, creates a spore cloud that knocks back enemies and applies spores.");
    BUILDER.comment("");

    WILDFIRE_TRIGGER_THRESHOLD = BUILDER
            .comment("触发野火喷射（反击）所需的最小自然属性强化点数。",
                    "Minimum Nature points required to trigger Wildfire Ejection (counter-attack).",
                    "Default: 20.0")
            .defineInRange("wildfire_trigger_threshold", 20.0, 0.0, 10000.0);
    BUILDER.comment("");

    WILDFIRE_COOLDOWN = BUILDER
            .comment("野火喷射的冷却时间（Tick）。",
                    "Cooldown (Ticks) for Wildfire Ejection.",
                    "Default: 200")
            .defineInRange("wildfire_cooldown", 200, 0, 60000);
    BUILDER.comment("");

    WILDFIRE_RADIUS = BUILDER
            .comment("反击爆炸的半径（格）。",
                    "Radius (blocks) of the counter-attack explosion.",
                    "Default: 3.0")
            .defineInRange("wildfire_radius", 3.0, 1.0, 16.0);
    BUILDER.comment("");

    WILDFIRE_KNOCKBACK = BUILDER
            .comment("反击造成的水平击退力度。",
                    "Horizontal knockback strength of the counter-attack.",
                    "Default: 1.5")
            .defineInRange("wildfire_knockback", 1.5, 0.0, 10.0);
    BUILDER.comment("");

    WILDFIRE_VERTICAL_KNOCKBACK = BUILDER
            .comment("反击造成的垂直击退力度。",
                    "Vertical knockback strength of the counter-attack.",
                    "Default: 0.5")
            .defineInRange("wildfire_vertical_knockback", 0.5, 0.0, 10.0);
    BUILDER.comment("");

    WILDFIRE_SPORE_AMOUNT = BUILDER
            .comment("反击时施加给敌人的孢子层数。",
                    "Number of Spore stacks applied to enemies during counter-attack.",
                    "Default: 2")
            .defineInRange("wildfire_spore_amount", 2, 0, 10);
    BUILDER.comment("");

    WILDFIRE_CLEAR_BURNING = BUILDER
            .comment("野火喷射是否清除自身的燃烧和灼烧效果？如果为 false，则保留这些效果。",
                    "Whether Wildfire Ejection clears burning and scorched effects from itself. If false, these effects are preserved.",
                    "Default: true")
            .define("wildfire_clear_burning", true);
    BUILDER.pop();
    BUILDER.pop();

    BUILDER.push("fire_reaction");
    BUILDER.comment("赤焰元素反应 - Fire Reaction System",
            "主要包括赤焰属性攻击引爆易燃孢子（毒火爆燃）的机制。",
            "Primarily handles the detonation of Flammable Spores by Fire attacks (Toxic Blast).");

    BUILDER.push("toxic_blast");
    BUILDER.comment("毒火爆燃 - Toxic Blast",
            "当赤焰属性攻击命中带有足够孢子层数的目标时，会引发爆炸，伤害、半径和附加灼烧时间随孢子层数和赤焰强化点数成长。",
            "Detonates spore-infected targets, causing an explosion. Damage, radius, and scorch duration scale with spore stacks and Fire points.");
    BUILDER.comment("");

    BLAST_TRIGGER_THRESHOLD = BUILDER
            .comment("触发毒火爆燃（引爆易燃孢子）所需的最小赤焰属性强化点数。",
                    "Minimum Fire points required to trigger Toxic Blast (detonate Flammable Spores).",
                    "Default: 50.0")
            .defineInRange("blast_trigger_threshold", 50.0, 0.0, 10000.0);
    BUILDER.comment("");

    BLAST_WEAK_IGNITE_MULT = BUILDER
            .comment("当孢子层数不足时，弱效爆燃施加的灼烧伤害倍率。",
                    "该倍率直接乘以灼烧的每跳最终伤害。",
                    "例如：设为 1.5，则灼烧每跳伤害变为原来的 1.5 倍。",
                    "Scorch damage multiplier for weak blast (when spore stacks are below reaction threshold).",
                    "Directly multiplies the final Scorch damage per tick.",
                    "Example: 1.5 = 1.5x damage per tick.",
                    "Default: 1.5")
            .defineInRange("blast_weak_ignite_mult", 1.5, 1.0, 100.0);
    BUILDER.comment("");

    BLAST_BASE_DAMAGE = BUILDER
            .comment("达到反应阈值（默认3层）引爆时的基础爆炸伤害。",
                    "Base explosion damage when detonating at the reaction threshold (default 3).",
                    "Default: 5.0")
            .defineInRange("blast_base_damage", 5.0, 0.0, 1000.0);
    BUILDER.comment("");

    BLAST_DMG_STEP = BUILDER
            .comment("爆炸伤害提升一级所需的额外赤焰属性强化点数（基于触发阈值计算）。",
                    "即：赤焰强化点数超过 blast_trigger_threshold 后，每超出该步长值，爆炸伤害增加一级。",
                    "例如：阈值为50，步长为20，则70点强化提升1级，90点提升2级，以此类推。",
                    "Additional Fire points required to increase explosion damage tier (calculated above the trigger threshold).",
                    "Meaning: For every multiple of this step value beyond the blast_trigger_threshold, the explosion damage increases by one tier.",
                    "Example: threshold=50, step=20, then 70 points gives +1 tier, 90 points gives +2 tiers, etc.",
                    "Default: 25.0")
            .defineInRange("blast_dmg_step", 25.0, 1.0, 10000.0);
    BUILDER.comment("");

    BLAST_DMG_AMOUNT = BUILDER
            .comment("每一级提升增加的爆炸伤害点数。",
                    "Explosion damage added per tier.",
                    "Default: 2.0")
            .defineInRange("blast_dmg_amount", 2.0, 0.0, 1000.0);
    BUILDER.comment("");

    BLAST_GROWTH_DAMAGE = BUILDER
            .comment("超过反应阈值后，每多一层易燃孢子增加的爆炸伤害。",
                    "Bonus explosion damage per extra Flammable Spore stack above the threshold.",
                    "Default: 2.0")
            .defineInRange("blast_growth_damage", 2.0, 0.0, 1000.0);
    BUILDER.comment("");

    BLAST_BASE_RANGE = BUILDER
            .comment("基础爆炸半径（格）。",
                    "Base explosion radius (blocks).",
                    "Default: 1.5")
            .defineInRange("blast_base_range", 1.5, 0.5, 1000.0);
    BUILDER.comment("");

    BLAST_GROWTH_RANGE = BUILDER
            .comment("超过反应阈值后，每多一层易燃孢子增加的爆炸半径（格）。",
                    "Bonus explosion radius per extra Flammable Spore stack above the threshold.",
                    "Default: 1.0")
            .defineInRange("blast_growth_range", 1.0, 0.0, 5.0);
    BUILDER.comment("");

    BLAST_SCORCH_BASE = BUILDER
            .comment("弱效引燃（低于反应阈值）造成的灼烧持续时间（秒）。",
                    "Duration (seconds) of weak scorching applied by low stacks (below reaction threshold).",
                    "Default: 3.0")
            .defineInRange("blast_scorch_base", 3.0, 0.0, 6000.0);
    BUILDER.comment("");

    BLAST_BASE_SCORCH_TIME = BUILDER
            .comment("成功爆炸（达到反应阈值）后造成的灼烧持续时间（秒）。",
                    "Duration (seconds) of scorching applied by a successful explosion (at reaction threshold).",
                    "Default: 3.0")
            .defineInRange("blast_base_scorch_time", 3.0, 0.0, 6000.0);
    BUILDER.comment("");

    BLAST_GROWTH_SCORCH_TIME = BUILDER
            .comment("超过反应阈值后，每多一层易燃孢子增加的灼烧时间（秒）。",
                    "Bonus scorch time (seconds) per extra Flammable Spore stack above the threshold.",
                    "Default: 1.0")
            .defineInRange("blast_growth_scorch_time", 1.0, 0.0, 1000.0);
    BUILDER.comment("");

    BLAST_CHAIN_REACTION = BUILDER
            .comment("是否开启毒火爆燃的连锁反应机制？如果开启，当爆炸波及到身上有孢子的生物时，会立即诱发它们也发生爆炸（连环爆炸）。",
                    "Whether to enable the Chain Reaction mechanic for Toxic Blast. If enabled, detonating a spore-infected entity will recursively detonate other nearby infected entities immediately.",
                    "Default: true")
            .define("blast_chain_reaction", true);
    BUILDER.comment("");

    BLAST_MAX_BLAST_PROT_CAP = BUILDER
            .comment("“爆炸保护”附魔最多能抵消的爆燃伤害比例。(0.5 = 50%)",
                    "Maximum blast damage mitigation provided by 'Blast Protection' enchantment. (0.5 = 50%)",
                    "Default: 0.5")
            .defineInRange("blast_max_blast_prot_cap", 0.5, 0.0, 1.0);
    BUILDER.comment("");

    BLAST_MAX_GENERAL_PROT_CAP = BUILDER
            .comment("普通“保护”附魔最多能抵消的爆燃伤害比例。(0.25 = 25%)",
                    "Maximum blast damage mitigation provided by general 'Protection' enchantment. (0.25 = 25%)",
                    "Default: 0.25")
            .defineInRange("blast_max_general_prot_cap", 0.25, 0.0, 1.0);
    BUILDER.pop();
    BUILDER.pop();

    SPEC = BUILDER.build();
}

    public static int wetnessMaxLevel;
    public static double wetnessShallowWaterCapRatio;
    public static double wetnessFireReduction;
    public static double wetnessMaxReduction;
    public static int wetnessRainGainInterval;
    public static int wetnessDecayBaseTime;
    public static double wetnessExhaustionIncrease;
    public static int wetnessPotionAddLevel;
    public static int wetnessDryingThreshold;
    public static double wetnessSelfDryingDamagePenalty;
    public static int wetnessFireDryingTime;
    public static int wetnessTickInterval;
    public static double wetnessHeatSearchRadius;
    public static boolean wetnessWaterAnimalImmune;
    public static boolean wetnessNetherDimensionImmune;
    public static List<? extends String> cachedWetnessBlacklist;
    public static int sporeMaxStacks;
    public static int sporeReactionThreshold;
    public static double sporePoisonDamage;
    public static int sporeDamageInterval;
    public static double sporeSpeedReduction;
    public static double sporePhysResist;
    public static double sporeFireVulnPerStack;
    public static int sporeDurationPerStack;
    public static double sporeThunderMultiplier;
    public static double sporeFireDurationReduction;
    public static List<? extends String> cachedSporeBlacklist;
    public static int contagionCheckInterval;
    public static double contagionBaseRadius;
    public static double contagionRadiusPerStack;
    public static double contagionIntensityRatio;
    public static boolean contagionOnlyHostile;
    public static boolean contagionAllowInfectedSpread;
    public static boolean contagionAllowReinfected;
    public static double natureParasiteBaseThreshold;
    public static double natureParasiteBaseChance;
    public static double natureParasiteScalingStep;
    public static double natureParasiteScalingChance;
    public static int natureParasiteAmount;
    public static double natureParasiteWetnessBonus;
    public static int natureImmunityThreshold;
    public static double wildfireTriggerThreshold;
    public static int wildfireCooldown;
    public static double wildfireRadius;
    public static double wildfireKnockback;
    public static double wildfireVerticalKnockback;
    public static int wildfireSporeAmount;
    public static boolean wildfireClearBurning;
    public static double blastTriggerThreshold;
    public static double blastWeakIgniteMult;
    public static double blastBaseDamage;
    public static double blastDmgStep;
    public static double blastDmgAmount;
    public static double blastGrowthDamage;
    public static double blastBaseRange;
    public static double blastGrowthRange;
    public static double blastScorchBase;
    public static double blastBaseScorchTime;
    public static double blastGrowthScorchTime;
    public static boolean blastChainReaction;
    public static double blastMaxBlastProtCap;
    public static double blastMaxGeneralProtCap;
    public static double enchantmentCalculationDenominator;
    public static boolean steamReactionEnabled;
    public static int steamHighHeatMaxLevel;
    public static int steamLowHeatMaxLevel;
    public static double steamMaxReduction;
    public static double steamCloudRadius;
    public static double steamRadiusPerLevel;
    public static int steamCloudDuration;
    public static int steamDurationPerLevel;
    public static boolean steamClearAggro;
    public static int steamCheckInterval;
    public static double steamCloudHeightCeiling;
    public static int steamCondensationStepFire;
    public static int steamCondensationStepFrost;
    public static int steamCondensationDelay;
    public static int steamCondensationDurationBase;
    public static int steamCondensationDurationPerLevel;
    public static int steamSporeGrowthRate;
    public static double steamScaldingDamage;
    public static double steamDamageScalePerLevel;
    public static double steamScaldingMultiplierWeakness;
    public static double steamScaldingMultiplierSpore;
    public static int steamImmunityThreshold;
    public static List<? extends String> cachedSteamBlacklist;
    public static int steamTriggerThresholdFire;
    public static int steamTriggerThresholdFrost;
    public static int steamTriggerCooldown;
    public static double steamDamageFloorRatio;
    public static double steamMaxFireProtCap;
    public static double steamMaxGeneralProtCap;
    public static int scorchedTriggerThreshold;
    public static double scorchedBaseChance;
    public static double scorchedChancePerPoint;
    public static int scorchedDuration;
    public static int scorchedCooldown;
    public static double scorchedDamageBase;
    public static int scorchedDamageScalingStep;
    public static int scorchedResistThreshold;
    public static double scorchedImmuneModifier;
    public static double scorchedFireProtReduction;
    public static double scorchedShockDamageRatio;
    public static double scorchedGenProtReduction;
    public static double scorchedNatureDurationMultiplier;
    public static double scorchedFrostDurationMultiplier;
    public static List<? extends String> cachedScorchedBlacklist;

    public static void refreshCache() {
        wetnessMaxLevel = WETNESS_MAX_LEVEL.get();
        wetnessShallowWaterCapRatio = WETNESS_SHALLOW_WATER_CAP_RATIO.get();
        wetnessFireReduction = WETNESS_FIRE_REDUCTION.get();
        wetnessMaxReduction = WETNESS_MAX_REDUCTION.get();
        wetnessRainGainInterval = WETNESS_RAIN_GAIN_INTERVAL.get();
        wetnessDecayBaseTime = WETNESS_DECAY_BASE_TIME.get();
        wetnessExhaustionIncrease = WETNESS_EXHAUSTION_INCREASE.get();
        wetnessPotionAddLevel = WETNESS_POTION_ADD_LEVEL.get();
        wetnessDryingThreshold = WETNESS_DRYING_THRESHOLD.get();
        wetnessSelfDryingDamagePenalty = WETNESS_SELF_DRYING_DAMAGE_PENALTY.get();
        wetnessFireDryingTime = WETNESS_FIRE_DRYING_TIME.get();
        wetnessTickInterval = WETNESS_TICK_INTERVAL.get();
        wetnessHeatSearchRadius = WETNESS_HEAT_SEARCH_RADIUS.get();
        wetnessWaterAnimalImmune = WETNESS_WATER_ANIMAL_IMMUNE.get();
        wetnessNetherDimensionImmune = WETNESS_NETHER_DIMENSION_IMMUNE.get();
        cachedWetnessBlacklist = WETNESS_ENTITY_BLACKLIST.get();
        sporeMaxStacks = SPORE_MAX_STACKS.get();
        sporeReactionThreshold = SPORE_REACTION_THRESHOLD.get();
        sporePoisonDamage = SPORE_POISON_DAMAGE.get();
        sporeDamageInterval = SPORE_DAMAGE_INTERVAL.get();
        sporeSpeedReduction = SPORE_SPEED_REDUCTION.get();
        sporePhysResist = SPORE_PHYS_RESIST.get();
        sporeFireVulnPerStack = SPORE_FIRE_VULN_PER_STACK.get();
        sporeDurationPerStack = SPORE_DURATION_PER_STACK.get();
        sporeThunderMultiplier = SPORE_THUNDER_MULTIPLIER.get();
        sporeFireDurationReduction = SPORE_FIRE_DURATION_REDUCTION.get();
        cachedSporeBlacklist = SPORE_ENTITY_BLACKLIST.get();
        contagionCheckInterval = CONTAGION_CHECK_INTERVAL.get();
        contagionBaseRadius = CONTAGION_BASE_RADIUS.get();
        contagionRadiusPerStack = CONTAGION_RADIUS_PER_STACK.get();
        contagionIntensityRatio = CONTAGION_INTENSITY_RATIO.get();
        contagionOnlyHostile = CONTAGION_ONLY_HOSTILE.get();
        contagionAllowInfectedSpread = CONTAGION_ALLOW_INFECTED_SPREAD.get();
        contagionAllowReinfected = CONTAGION_ALLOW_REINFECTED.get();
        natureParasiteBaseThreshold = NATURE_PARASITE_BASE_THRESHOLD.get();
        natureParasiteBaseChance = NATURE_PARASITE_BASE_CHANCE.get();
        natureParasiteScalingStep = NATURE_PARASITE_SCALING_STEP.get();
        natureParasiteScalingChance = NATURE_PARASITE_SCALING_CHANCE.get();
        natureParasiteAmount = NATURE_PARASITE_AMOUNT.get();
        natureParasiteWetnessBonus = NATURE_PARASITE_WETNESS_BONUS.get();
        natureImmunityThreshold = NATURE_IMMUNITY_THRESHOLD.get();
        wildfireTriggerThreshold = WILDFIRE_TRIGGER_THRESHOLD.get();
        wildfireCooldown = WILDFIRE_COOLDOWN.get();
        wildfireRadius = WILDFIRE_RADIUS.get();
        wildfireKnockback = WILDFIRE_KNOCKBACK.get();
        wildfireVerticalKnockback = WILDFIRE_VERTICAL_KNOCKBACK.get();
        wildfireSporeAmount = WILDFIRE_SPORE_AMOUNT.get();
        wildfireClearBurning = WILDFIRE_CLEAR_BURNING.get();
        blastTriggerThreshold = BLAST_TRIGGER_THRESHOLD.get();
        blastWeakIgniteMult = BLAST_WEAK_IGNITE_MULT.get();
        blastBaseDamage = BLAST_BASE_DAMAGE.get();
        blastDmgStep = BLAST_DMG_STEP.get();
        blastDmgAmount = BLAST_DMG_AMOUNT.get();
        blastGrowthDamage = BLAST_GROWTH_DAMAGE.get();
        blastBaseRange = BLAST_BASE_RANGE.get();
        blastGrowthRange = BLAST_GROWTH_RANGE.get();
        blastScorchBase = BLAST_SCORCH_BASE.get();
        blastBaseScorchTime = BLAST_BASE_SCORCH_TIME.get();
        blastGrowthScorchTime = BLAST_GROWTH_SCORCH_TIME.get();
        blastChainReaction = BLAST_CHAIN_REACTION.get();
        blastMaxBlastProtCap = BLAST_MAX_BLAST_PROT_CAP.get();
        blastMaxGeneralProtCap = BLAST_MAX_GENERAL_PROT_CAP.get();
        enchantmentCalculationDenominator = ENCHANTMENT_CALCULATION_DENOMINATOR.get();
        steamReactionEnabled = STEAM_REACTION_ENABLED.get();
        steamHighHeatMaxLevel = STEAM_HIGH_HEAT_MAX_LEVEL.get();
        steamLowHeatMaxLevel = STEAM_LOW_HEAT_MAX_LEVEL.get();
        steamMaxReduction = STEAM_MAX_REDUCTION.get();
        steamCloudRadius = STEAM_CLOUD_RADIUS.get();
        steamRadiusPerLevel = STEAM_RADIUS_PER_LEVEL.get();
        steamCloudDuration = STEAM_CLOUD_DURATION.get();
        steamDurationPerLevel = STEAM_DURATION_PER_LEVEL.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        steamCheckInterval = STEAM_CHECK_INTERVAL.get();
        steamCloudHeightCeiling = STEAM_CLOUD_HEIGHT_CEILING.get();
        steamCondensationStepFire = STEAM_CONDENSATION_STEP_FIRE.get();
        steamCondensationStepFrost = STEAM_CONDENSATION_STEP_FROST.get();
        steamCondensationDelay = STEAM_CONDENSATION_DELAY.get();
        steamCondensationDurationBase = STEAM_CONDENSATION_DURATION_BASE.get();
        steamCondensationDurationPerLevel = STEAM_CONDENSATION_DURATION_PER_LEVEL.get();
        steamSporeGrowthRate = STEAM_SPORE_GROWTH_RATE.get();
        steamScaldingDamage = STEAM_SCALDING_DAMAGE.get();
        steamDamageScalePerLevel = STEAM_DAMAGE_SCALE_PER_LEVEL.get();
        steamScaldingMultiplierWeakness = STEAM_SCALDING_MULTIPLIER_WEAKNESS.get();
        steamScaldingMultiplierSpore = STEAM_SCALDING_MULTIPLIER_SPORE.get();
        steamImmunityThreshold = STEAM_IMMUNITY_THRESHOLD.get();
        cachedSteamBlacklist = STEAM_IMMUNITY_BLACKLIST.get();
        steamTriggerThresholdFire = STEAM_TRIGGER_THRESHOLD_FIRE.get();
        steamTriggerThresholdFrost = STEAM_TRIGGER_THRESHOLD_FROST.get();
        steamTriggerCooldown = STEAM_TRIGGER_COOLDOWN.get();
        steamDamageFloorRatio = STEAM_DAMAGE_FLOOR_RATIO.get();
        steamMaxFireProtCap = STEAM_MAX_FIRE_PROT_CAP.get();
        steamMaxGeneralProtCap = STEAM_MAX_GENERAL_PROT_CAP.get();
        scorchedTriggerThreshold = SCORCHED_TRIGGER_THRESHOLD.get();
        scorchedBaseChance = SCORCHED_BASE_CHANCE.get();
        scorchedChancePerPoint = SCORCHED_CHANCE_PER_POINT.get();
        scorchedDuration = SCORCHED_DURATION.get();
        scorchedCooldown = SCORCHED_COOLDOWN.get();
        scorchedDamageBase = SCORCHED_DAMAGE_BASE.get();
        scorchedDamageScalingStep = SCORCHED_DAMAGE_SCALING_STEP.get();
        scorchedResistThreshold = SCORCHED_RESIST_THRESHOLD.get();
        scorchedImmuneModifier = SCORCHED_IMMUNE_MODIFIER.get();
        scorchedFireProtReduction = SCORCHED_FIRE_PROT_REDUCTION.get();
        scorchedShockDamageRatio = SCORCHED_SHOCK_DAMAGE_RATIO.get();
        scorchedGenProtReduction = SCORCHED_GEN_PROT_REDUCTION.get();
        scorchedNatureDurationMultiplier = SCORCHED_NATURE_DURATION_MULTIPLIER.get();
        scorchedFrostDurationMultiplier = SCORCHED_FROST_DURATION_MULTIPLIER.get();
        cachedScorchedBlacklist = SCORCHED_ENTITY_BLACKLIST.get();
    }

    @SuppressWarnings("deprecation")
    public static void register(String fileName) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, fileName);
    }
}