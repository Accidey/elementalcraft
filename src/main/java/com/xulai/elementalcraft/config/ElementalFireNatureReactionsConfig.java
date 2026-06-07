package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import java.util.List;

public class ElementalFireNatureReactionsConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue WETNESS_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue WETNESS_SHALLOW_WATER_CAP;
    public static final ForgeConfigSpec.DoubleValue WETNESS_FIRE_REDUCTION;
    public static final ForgeConfigSpec.IntValue WETNESS_RAIN_GAIN_INTERVAL;
    public static final ForgeConfigSpec.IntValue WETNESS_DECAY_BASE_TIME;
    public static final ForgeConfigSpec.DoubleValue WETNESS_EXHAUSTION_INCREASE;
    public static final ForgeConfigSpec.IntValue WETNESS_POTION_ADD_LEVEL;
    public static final ForgeConfigSpec.IntValue WETNESS_DRYING_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue WETNESS_SELF_DRYING_DAMAGE_PENALTY;
    public static final ForgeConfigSpec.IntValue WETNESS_FIRE_DRYING_TIME;
    public static final ForgeConfigSpec.DoubleValue WETNESS_HEAT_SEARCH_RADIUS;
    public static final ForgeConfigSpec.BooleanValue WETNESS_WATER_ANIMAL_IMMUNE;
    public static final ForgeConfigSpec.BooleanValue WETNESS_NETHER_DIMENSION_IMMUNE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WETNESS_ENTITY_BLACKLIST;
    public static final ForgeConfigSpec.IntValue SPORE_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue SPORE_REACTION_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SPORE_POISON_DAMAGE;
    public static final ForgeConfigSpec.IntValue SPORE_DAMAGE_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_VULN_PER_STACK;
    public static final ForgeConfigSpec.IntValue SPORE_DURATION_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_DURATION_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SPORE_NATURE_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SPORE_THUNDER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SPORE_FROST_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SPORE_ENTITY_BLACKLIST;
    public static final ForgeConfigSpec.IntValue SPORE_DURABILITY_DAMAGE;
    public static final int CONTAGION_CHECK_INTERVAL = 20;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_BASE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_RADIUS_PER_STACK;
    public static final ForgeConfigSpec.IntValue CONTAGION_TRANSFER_BASE;
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
    public static final ForgeConfigSpec.BooleanValue SPORE_ENVIRONMENTAL_BLAST_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BLAST_MAX_BLAST_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue BLAST_MAX_GENERAL_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue ENCHANTMENT_CALCULATION_DENOMINATOR;
    public static final ForgeConfigSpec.IntValue STEAM_HIGH_HEAT_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_LOW_HEAT_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_HIGH_HEAT_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.IntValue STEAM_LOW_HEAT_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_RADIUS;
    public static final ForgeConfigSpec.DoubleValue STEAM_RADIUS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_CLOUD_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_DURATION_PER_LEVEL;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_HEIGHT_CEILING;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DELAY;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DURATION_BASE;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DURATION_PER_LEVEL;

    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_SCALE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_FIRE;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_NATURE;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_THUNDER;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_FROST;
    public static final ForgeConfigSpec.IntValue STEAM_IMMUNITY_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STEAM_IMMUNITY_BLACKLIST;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_FIRE_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_GENERAL_PROT_CAP;
    public static final ForgeConfigSpec.IntValue SCORCHED_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_BASE_CHANCE;
    public static final ForgeConfigSpec.IntValue SCORCHED_CHANCE_PER_POINT;
    public static final ForgeConfigSpec.IntValue SCORCHED_DURATION;
    public static final ForgeConfigSpec.IntValue SCORCHED_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_DAMAGE_BASE;
    public static final ForgeConfigSpec.IntValue SCORCHED_DAMAGE_SCALING_STEP;
    public static final ForgeConfigSpec.IntValue SCORCHED_RESIST_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_IMMUNE_MODIFIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_FIRE_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_SHOCK_DAMAGE_RATIO;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_GEN_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_FIRE_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_NATURE_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_THUNDER_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_FROST_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue POISON_SCORCH_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue POISON_SCORCH_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCORCHED_ENTITY_BLACKLIST;
    public static final ForgeConfigSpec.IntValue SCORCHED_AURA_FIRE_POWER_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_AURA_RADIUS;
    public static final ForgeConfigSpec.IntValue SCORCHED_AURA_DAMAGE_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue SCORCHED_AURA_STEAM_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SCORCHED_AURA_SPORE_DETONATION_ENABLED;

    public static final ForgeConfigSpec.DoubleValue SCORCHED_FROST_DMG_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_NATURE_DMG_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_FIRE_DMG_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_THUNDER_DMG_MULTIPLIER;

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
    BUILDER.comment(" ");

    WETNESS_MAX_LEVEL = BUILDER
            .comment("潮湿状态的最高叠加层数，设为0则不会获得任何潮湿效果。",
                    "The maximum stack amount for Wetness status. Set to 0 to disable all Wetness effects.",
                    "Default: 5")
            .defineInRange("wetness_max_level", 5, 0, 1000);
    BUILDER.comment(" ");

    WETNESS_SHALLOW_WATER_CAP = BUILDER
            .comment("在浅水中（仅脚部接触水）时，获得的潮湿层数上限。不会超过 wetness_max_level。设为0则浅水不获得潮湿。",
                    "Maximum Wetness stacks in shallow water (feet only). Capped at wetness_max_level. Set to 0 to disable wetness in shallow water.",
                    "Default: 3")
            .defineInRange("wetness_shallow_water_cap", 3, 0, 1000);
    BUILDER.comment(" ");

    WETNESS_FIRE_REDUCTION = BUILDER
            .comment("每一层潮湿抵挡赤焰属性伤害的百分比。(0.1 = 10% 减伤)",
                    "Percentage of fire damage reduction provided by each stack of Wetness. (0.1 = 10% reduction)",
                    "Default: 0.1")
            .defineInRange("wetness_fire_reduction", 0.1, 0.0, 1.0);
    BUILDER.comment(" ");

    WETNESS_RAIN_GAIN_INTERVAL = BUILDER
            .comment("在雨中站立时，增加一层潮湿所需的时间（秒）。",
                    "Time (in seconds) required to gain a Wetness stack while standing in rain.",
                    "Default: 10")
            .defineInRange("wetness_rain_gain_interval", 10, 1, 3600);
    BUILDER.comment(" ");

    WETNESS_DECAY_BASE_TIME = BUILDER
            .comment("离开水源后，每一层潮湿自然消退所需的基础时间（秒）。",
                    "Base time (in seconds) for each Wetness stack to decay naturally after leaving water.",
                    "Default: 10")
            .defineInRange("wetness_decay_base_time", 10, 1, 3600);
    BUILDER.comment(" ");

    WETNESS_EXHAUSTION_INCREASE = BUILDER
            .comment("每层潮湿额外增加的饱食度消耗值。",
                     "总消耗 = 基础消耗 + 潮湿层数 × 此值。",
                     "例：基础消耗0.05，此值0.5，LV3时总消耗 = 0.05 + 3×0.5 = 1.55。",
                     "Extra exhaustion per wetness level.",
                     "Total = base + wetness_level × this value.",
                     "Example: base 0.05, this value 0.5, LV3 → 0.05 + 3×0.5 = 1.55.",
                     "Default: 0.5")
            .defineInRange("wetness_exhaustion_increase", 0.5, 0.0, 10.0);
    BUILDER.comment(" ");

    WETNESS_POTION_ADD_LEVEL = BUILDER
            .comment("被喷溅水瓶击中时，瞬间增加的潮湿层数。",
                    "Number of Wetness stacks instantly added when hit by a Splash Water Bottle.",
                    "Default: 1")
            .defineInRange("wetness_potion_add_level", 1, 1, 100);
    BUILDER.comment(" ");

    WETNESS_DRYING_THRESHOLD = BUILDER
            .comment("瞬间蒸发1层潮湿所需的赤焰属性强化点数阈值。设为0关闭此功能。",
                    "Threshold of Fire points required to instantly evaporate 1 layer of Wetness. Set to 0 to disable.",
                    "Default: 20")
            .defineInRange("wetness_drying_threshold", 20, 0, 1000);
    BUILDER.comment(" ");

    WETNESS_SELF_DRYING_DAMAGE_PENALTY = BUILDER
            .comment("赤焰生物自我蒸干潮湿时，造成的伤害降低比例。(0.3 = 降低30%)",
                    "Damage reduction penalty when a Fire entity tries to dry itself. (0.3 = 30% reduction)",
                    "Default: 0.3")
            .defineInRange("wetness_self_drying_damage_penalty", 0.3, 0.0, 1.0);
    BUILDER.comment(" ");

    WETNESS_FIRE_DRYING_TIME = BUILDER
            .comment("站在火中烧多少秒可瞬间清除所有潮湿效果。",
                    "Seconds required to stand in fire to instantly clear all Wetness effects.",
                    "Default: 2")
            .defineInRange("wetness_fire_drying_time", 2, 1, 600);
    BUILDER.comment(" ");

    BUILDER.comment(" ");

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
    BUILDER.comment(" ");

    WETNESS_NETHER_DIMENSION_IMMUNE = BUILDER
            .comment("下界维度的生物是否天生免疫潮湿效果？",
                    "Are entities in the Nether dimension naturally immune to Wetness?",
                    "Default: true")
            .define("nether_dimension_immune", true);
    BUILDER.comment(" ");

    WETNESS_ENTITY_BLACKLIST = BUILDER
            .comment("潮湿效果免疫黑名单（填入实体ID）。",
                    "Wetness immunity blacklist (Entity IDs).",
                    "Default: []")
            .defineListAllowEmpty("wetness_entity_blacklist", List.of(), o -> o instanceof String);
    BUILDER.pop();
    BUILDER.pop();

    BUILDER.push("steam_reaction");
    BUILDER.comment("蒸汽反应系统 - Steam Reaction System",
            "当赤焰或冰霜属性攻击潮湿目标时，会在目标位置生成一团蒸汽云。",
            "蒸汽云分为高温（烫伤）和低温（冷凝/繁殖孢子）两种。蒸汽云等级由目标潮湿等级决定，赤焰/冰霜强化仅用于触发判定。",
            "Steam clouds are created when Fire or Frost attacks a wet target.",
            "They come in High-Heat (scalding) and Low-Heat (condensation/spore growth) varieties.",
            "Steam cloud level is determined by the target's wetness level; Fire or Frost power only affects the trigger threshold.");
    BUILDER.comment(" ");

    STEAM_HIGH_HEAT_MAX_LEVEL = BUILDER
            .comment("高温蒸汽云的最高等级，设为0则关闭高温蒸汽云反应。",
                    "Maximum level for High-Heat Steam clouds. Set to 0 to disable the High-Heat Steam Cloud reaction.",
                    "Default: 5")
            .defineInRange("steam_high_heat_max_level", 5, 0, 10000);
    BUILDER.comment(" ");

    STEAM_LOW_HEAT_MAX_LEVEL = BUILDER
            .comment("低温蒸汽云的最高等级，设为0则关闭低温蒸汽云反应。",
                    "Maximum level for Low-Heat Steam clouds. Set to 0 to disable the Low-Heat Steam Cloud reaction.",
                    "Default: 5")
            .defineInRange("steam_low_heat_max_level", 5, 0, 10000);
    BUILDER.comment(" ");

    STEAM_HIGH_HEAT_TRIGGER_THRESHOLD = BUILDER
            .comment("攻击时，触发高温蒸汽云所需的最小赤焰属性强化点数。",
                     "层数由目标潮湿等级决定，赤焰强化仅用于触发判定。",
                     "Minimum Fire points required to trigger a High-Heat Steam Cloud.",
                     "The cloud level is determined by the target's wetness level; Fire power only controls the trigger.",
                     "Default: 50")
            .defineInRange("steam_high_heat_trigger_threshold", 50, 0, 10000);
    BUILDER.comment(" ");
    STEAM_LOW_HEAT_TRIGGER_THRESHOLD = BUILDER
            .comment("攻击潮湿目标时，触发低温蒸汽云所需的最小冰霜属性强化点数。",
                     "层数由目标潮湿等级决定，冰霜强化仅用于触发判定。",
                     "Minimum Frost points required to trigger a Low-Heat Steam Cloud when attacking a wet target.",
                     "The cloud level is determined by the target's wetness level; Frost power only controls the trigger.",
                     "Default: 50")
            .defineInRange("steam_low_heat_trigger_threshold", 50, 0, 10000);
    BUILDER.comment(" ");

    STEAM_TRIGGER_COOLDOWN = BUILDER
            .comment("蒸汽云消散后的额外冷却时间（Tick）。总冷却 = 云持续时间 + 此值。",
                    "Additional cooldown (Ticks) after the steam cloud expires. Total cooldown = cloud duration + this value.",
                    "Default: 200")
            .defineInRange("steam_trigger_cooldown", 200, 0, 6000);
    BUILDER.comment(" ");

    BUILDER.push("cloud_properties");
    BUILDER.comment("蒸汽云属性 - Cloud Properties",
            "控制蒸汽云的基础半径、持续时间、检测间隔以及上升高度等物理特性。",
            "Controls base radius, duration, scan interval, and vertical ceiling of steam clouds.");
    BUILDER.comment(" ");

    STEAM_CLOUD_RADIUS = BUILDER
            .comment("蒸汽云的基础半径（格）。",
                    "Base radius (blocks) for steam clouds.",
                    "Default: 2.0")
            .defineInRange("steam_cloud_radius", 2.0, 0.5, 1000.0);
    BUILDER.comment(" ");

    STEAM_RADIUS_PER_LEVEL = BUILDER
            .comment("每增加一级（潮湿层数），蒸汽云增加的半径（格）。",
                    "Additional radius (blocks) per steam level (wetness stack).",
                    "Default: 1.0")
            .defineInRange("steam_radius_per_level", 1.0, 0.0, 5.0);
    BUILDER.comment(" ");

    STEAM_CLOUD_DURATION = BUILDER
            .comment("蒸汽云的基础存在时间（Tick）。",
                    "Base duration (Ticks) for steam clouds.",
                    "Default: 100")
            .defineInRange("steam_cloud_duration", 100, 20, 12000);
    BUILDER.comment(" ");

    STEAM_DURATION_PER_LEVEL = BUILDER
            .comment("每增加一级，蒸汽云增加的存在时间（Tick）。",
                    "Additional duration (Ticks) per steam level.",
                    "Default: 20")
            .defineInRange("steam_duration_per_level", 20, 0, 2000);
    BUILDER.comment(" ");

    STEAM_CLEAR_AGGRO = BUILDER
            .comment("蒸汽云是否会遮蔽视线，强制清除里面怪物的仇恨目标？",
                    "Whether steam clouds obscure vision and force mobs inside to lose aggro.",
                    "Default: true")
            .define("steam_clear_aggro", true);
    BUILDER.comment(" ");


    STEAM_CLOUD_HEIGHT_CEILING = BUILDER
            .comment("蒸汽云向上飘散的有效高度（格）。超过这个高度不造成烫伤。",
                    "Effective height ceiling (blocks) for rising steam clouds. No scalding above this height.",
                    "Default: 3.0")
            .defineInRange("steam_cloud_height_ceiling", 3.0, 0.0, 16.0);
    BUILDER.pop();

    BUILDER.push("condensation_logic");
    BUILDER.comment("冷凝逻辑 - Condensation Logic",
	            "低温蒸汽云的特殊行为：使内部生物获得潮湿效果、以及促进易燃孢子繁殖。",
	            "Special behaviors for Low-Heat Steam: applying Wetness to entities inside, and accelerating Flammable Spore growth.");
    BUILDER.comment(" ");


	STEAM_CONDENSATION_DELAY = BUILDER
            .comment("在低温蒸汽里待多久（Tick）才会获得潮湿效果？",
                    "Time (Ticks) required to stay in Low-Heat Steam to gain Wetness.",
                    "Default: 100")
            .defineInRange("steam_condensation_delay", 100, 1, 24000);
    BUILDER.comment(" ");

    STEAM_CONDENSATION_DURATION_BASE = BUILDER
            .comment("低温蒸汽云的基础持续时间（Tick）。",
                    "Base duration (Ticks) for Low-Heat Steam clouds.",
                    "Default: 200")
            .defineInRange("steam_condensation_duration_base", 200, 20, 12000);
    BUILDER.comment(" ");

    STEAM_CONDENSATION_DURATION_PER_LEVEL = BUILDER
            .comment("低温蒸汽每升一级增加的持续时间（Tick）。",
                    "Additional duration (Ticks) per level for Low-Heat Steam.",
                    "Default: 20")
            .defineInRange("steam_condensation_duration_per_level", 20, 0, 2000);
    BUILDER.comment(" ");

    BUILDER.pop();

    BUILDER.push("scalding_damage");
    BUILDER.comment("烫伤伤害 - Scalding Damage",
            "高温蒸汽云对内部生物造成的持续伤害。伤害受蒸汽等级、目标弱点（冰霜/自然）以及抗性/附魔影响。",
            "携带易燃孢子的生物受击时触发毒火爆燃。",
            "Damage over time dealt by High-Heat Steam. Scales with steam level and multipliers for vulnerable targets (Frost/Nature).",
            "Entities with Flammable Spores trigger Toxic Blast when hit.");
    BUILDER.comment(" ");

    STEAM_SCALDING_DAMAGE = BUILDER
            .comment("高温蒸汽每秒造成的基础烫伤伤害。",
                    "Base scalding damage per second from High-Heat Steam.",
                    "Default: 1.0")
            .defineInRange("steam_scalding_damage", 1.0, 0.0, 10000.0);
    BUILDER.comment(" ");

    STEAM_DAMAGE_SCALE_PER_LEVEL = BUILDER
            .comment("蒸汽等级每升一级，烫伤伤害增加的比例（1级无加成）。例如：0.2 = 每升一级+20%，2级为120%，3级为140%",
                    "Percentage increase per steam level (level 1 has no bonus). e.g., 0.2 = +20% per level, level 2 = 120%, level 3 = 140%",
                    "Default: 0.2")
            .defineInRange("steam_damage_scale_per_level", 0.2, 0.0, 10.0);
    BUILDER.comment(" ");

    STEAM_SCALDING_MULTIPLIER_FIRE = BUILDER
            .comment("赤焰属性生物受到高温蒸汽烫伤伤害的**乘法倍率**。",
                    "Multiplicative damage multiplier for Fire attribute entities.",
                    "Default: 1.0")
            .defineInRange("steam_scalding_multiplier_fire", 1.0, 0.0, 1000.0);
    BUILDER.comment(" ");

    STEAM_SCALDING_MULTIPLIER_NATURE = BUILDER
            .comment("自然属性生物受到高温蒸汽烫伤伤害的**乘法倍率**。",
                    "Multiplicative damage multiplier for Nature attribute entities.",
                    "Default: 1.5")
            .defineInRange("steam_scalding_multiplier_nature", 1.5, 0.0, 1000.0);
    BUILDER.comment(" ");

    STEAM_SCALDING_MULTIPLIER_THUNDER = BUILDER
            .comment("雷霆属性生物受到高温蒸汽烫伤伤害的**乘法倍率**。",
                    "Multiplicative damage multiplier for Thunder attribute entities.",
                    "Default: 1.0")
            .defineInRange("steam_scalding_multiplier_thunder", 1.0, 0.0, 1000.0);
    BUILDER.comment(" ");

    STEAM_SCALDING_MULTIPLIER_FROST = BUILDER
            .comment("冰霜属性生物受到高温蒸汽烫伤伤害的**乘法倍率**。",
                    "Multiplicative damage multiplier for Frost attribute entities.",
                    "Default: 0.5")
            .defineInRange("steam_scalding_multiplier_frost", 0.5, 0.0, 1000.0);
    BUILDER.comment(" ");

    STEAM_IMMUNITY_THRESHOLD = BUILDER
            .comment("完全免疫蒸汽烫伤所需的赤焰抗性点数。",
                    "Fire Resistance points required to be completely immune to steam scalding.",
                    "Default: 80")
            .defineInRange("steam_immunity_threshold", 80, 0, 1000);
    BUILDER.comment(" ");

    STEAM_IMMUNITY_BLACKLIST = BUILDER
            .comment("蒸汽烫伤免疫黑名单（填入实体ID）。",
                    "Steam scalding immunity blacklist (Entity IDs).",
                    "Default: []")
            .defineListAllowEmpty("steam_immunity_blacklist", List.of(), o -> o instanceof String);
    BUILDER.pop();

    BUILDER.push("trigger_logic");
    BUILDER.comment("触发逻辑 - Trigger Logic",
            "定义附魔减伤相关的通用计算参数。",
            "Defines shared enchantment protection parameters for steam reactions.");
    BUILDER.comment(" ");

    ENCHANTMENT_CALCULATION_DENOMINATOR = BUILDER
            .comment("用于计算附魔保护比例的分母，所有可以用附魔减伤的元素反应都使用此值。",
                    "默认值 16.0 意味着需要4件装备都附魔保护IV（4 * 4 = 16）才能达到设定的最大减伤比例。",
                    "The denominator used to calculate enchantment protection ratios. ",
                    "This value is shared by all elemental reactions that support enchantment-based damage reduction.",
                    "Default 16.0 means full protection (100% weight) requires 4 armor pieces with Protection IV (4 * 4 = 16).",
                    "Default: 16.0")
            .defineInRange("enchantment_calculation_denominator", 16.0, 1.0, 1000.0);
    BUILDER.comment(" ");

    STEAM_MAX_FIRE_PROT_CAP = BUILDER
            .comment("火焰保护附魔最多能抵消的蒸汽伤害比例。(0.5 = 50%)",
                    "Maximum steam damage mitigation provided by 'Fire Protection' enchantment. (0.5 = 50%)",
                    "Default: 0.5")
            .defineInRange("max_fire_prot_cap", 0.5, 0.0, 1.0);
    BUILDER.comment(" ");

    STEAM_MAX_GENERAL_PROT_CAP = BUILDER
            .comment("普通保护附魔最多能抵消的蒸汽伤害比例。(0.25 = 25%)",
                    "Maximum steam damage mitigation provided by general 'Protection' enchantment. (0.25 = 25%)",
                    "Default: 0.25")
            .defineInRange("max_general_prot_cap", 0.25, 0.0, 1.0);
    BUILDER.comment(" ");

    BUILDER.pop();

    BUILDER.pop();

    BUILDER.push("scorched_mechanic");
    BUILDER.comment("灼烧机制 - Scorched Mechanic",
            "赤焰属性攻击有一定几率使目标陷入“灼烧”状态，造成持续火焰伤害。",
            "灼烧持续时间受目标属性影响（自然生物更长，冰霜生物更短），且遇水（潮湿/蒸汽）会触发“热休克”立即结算部分伤害。",
            "Fire attacks have a chance to inflict 'Scorched', a fire-based DoT.",
            "Duration varies by target element (longer on Nature, shorter on Frost). Contact with water/steam triggers 'Thermal Shock', dealing instant damage.");
    BUILDER.comment(" ");

    SCORCHED_TRIGGER_THRESHOLD = BUILDER
            .comment("触发灼烧效果所需的最小赤焰属性强化点数，设为0则不会获得灼烧效果。",
                    "Minimum Fire points required to trigger Scorched effect. Set to 0 to disable all Scorched effects.",
                    "Default: 20")
            .defineInRange("scorched_trigger_threshold", 20, 0, 10000);
    BUILDER.comment(" ");

    SCORCHED_BASE_CHANCE = BUILDER
            .comment("触发灼烧的基础概率。(0.4 = 40%)",
                    "Base chance to trigger Scorched effect. (0.4 = 40%)",
                    "Default: 0.4")
            .defineInRange("scorched_base_chance", 0.4, 0.0, 1.0);
    BUILDER.comment(" ");

    SCORCHED_CHANCE_PER_POINT = BUILDER
            .comment("每多少点赤焰属性强化（超过阈值后）增加5%的灼烧触发概率。",
                    "总概率 = scorched_base_chance + floor((赤焰点数 - 阈值) / scorched_chance_per_point) × 0.05。",
                    "最终概率不会超过 1.0（100%）。",
                    "例如：阈值20，此值10，则30点时概率=base+5%，40点时=base+10%，以此类推。",
                    "How many Fire points (above threshold) per 5% Scorched chance increase.",
                    "Total chance = scorched_base_chance + floor((firePower - threshold) / scorched_chance_per_point) × 0.05.",
                    "Capped at 1.0 (100%).",
                    "Default: 10")
            .defineInRange("scorched_chance_per_point", 10, 1, 10000);
    BUILDER.comment(" ");

    SCORCHED_DURATION = BUILDER
            .comment("灼烧状态的持续时间（Tick）。(100 Tick = 5秒)",
                    "Duration (Ticks) of the Scorched effect. (100 Ticks = 5 seconds)",
                    "Default: 100")
            .defineInRange("scorched_duration", 100, 20, 12000);
    BUILDER.comment(" ");

    SCORCHED_COOLDOWN = BUILDER
            .comment("灼烧效果触发后的冷却时间（Tick）。",
                    "Cooldown (Ticks) after triggering the Scorched effect.",
                    "Default: 100")
            .defineInRange("scorched_cooldown", 100, 0, 6000);
    BUILDER.comment(" ");

    SCORCHED_DAMAGE_BASE = BUILDER
            .comment("灼烧每秒造成的基础伤害。",
                    "Base damage per second from Scorched effect.",
                    "Default: 1.0")
            .defineInRange("scorched_damage_base", 1.0, 0.1, 10000.0);
    BUILDER.comment(" ");

    SCORCHED_DAMAGE_SCALING_STEP = BUILDER
            .comment("灼烧伤害增加0.5点所需的赤焰属性增量（从触发阈值开始计算）。",
                    "Fire increment (above trigger threshold) required to increase Scorched damage by 0.5.",
                    "Default: 10")
            .defineInRange("scorched_damage_scaling_step", 10, 1, 10000);
    BUILDER.comment(" ");

    SCORCHED_RESIST_THRESHOLD = BUILDER
            .comment("完全免疫灼烧伤害所需的赤焰抗性点数。",
                    "Fire Resistance points required to be completely immune to Scorched damage.",
                    "Default: 80")
            .defineInRange("scorched_resist_threshold", 80, 1, 10000);
    BUILDER.comment(" ");

    SCORCHED_IMMUNE_MODIFIER = BUILDER
            .comment("天生免疫火的生物受到灼烧和蒸汽烫伤伤害的倍率。(0.5 = 50%)",
                    "Damage multiplier for naturally fire-immune entities when Scorched or Steam Scalding. (0.5 = 50%)",
                    "Default: 0.5")
            .defineInRange("scorched_immune_modifier", 0.5, 0.0, 1.0);
    BUILDER.comment(" ");

    SCORCHED_FIRE_PROT_REDUCTION = BUILDER
            .comment("火焰保护附魔最多能抵消的灼烧伤害比例。",
                    "Maximum Scorched damage mitigation provided by 'Fire Protection' enchantment.",
                    "Default: 0.5")
            .defineInRange("scorched_fire_prot_reduction", 0.5, 0.0, 1.0);
    BUILDER.comment(" ");

    SCORCHED_GEN_PROT_REDUCTION = BUILDER
            .comment("普通保护附魔最多能抵消的灼烧伤害比例。",
                    "Maximum Scorched damage mitigation provided by general 'Protection' enchantment.",
                    "Default: 0.25")
            .defineInRange("scorched_gen_prot_reduction", 0.25, 0.0, 1.0);
    BUILDER.comment(" ");

    SCORCHED_SHOCK_DAMAGE_RATIO = BUILDER
            .comment("热休克（灼烧遇水）时，剩余持续伤害瞬间结算的比例。(0.5 = 50%)",
                    "Ratio of remaining DOT damage dealt instantly during Thermal Shock. (0.5 = 50%)",
                    "Default: 0.5")
            .defineInRange("scorched_shock_damage_ratio", 0.5, 0.0, 10.0);
    BUILDER.comment(" ");

        SCORCHED_FIRE_DURATION_MULTIPLIER = BUILDER
                .comment("赤焰属性生物获得灼烧时，持续时间的倍率（1.0 表示不改变）。",
                    "Duration multiplier for Scorched effect on Fire entities (1.0 = no change).",
                    "Default: 1.0")
            .defineInRange("scorched_fire_duration_multiplier", 1.0, 0.1, 100.0);
    BUILDER.comment(" ");

    SCORCHED_NATURE_DURATION_MULTIPLIER = BUILDER
            .comment("自然属性生物获得灼烧时，持续时间的倍率（1.5 表示增加 50% 时长）。",
                    "Duration multiplier for Scorched effect on Nature entities (1.5 = +50% duration).",
                    "Default: 1.5")
            .defineInRange("scorched_nature_duration_multiplier", 1.5, 0.1, 100.0);
    BUILDER.comment(" ");

        SCORCHED_THUNDER_DURATION_MULTIPLIER = BUILDER
            .comment("雷霆属性生物获得灼烧时，持续时间的倍率（1.0 表示不改变）。",
                    "Duration multiplier for Scorched effect on Thunder entities (1.0 = no change).",
                    "Default: 1.0")
            .defineInRange("scorched_thunder_duration_multiplier", 1.0, 0.1, 100.0);
    BUILDER.comment(" ");

    SCORCHED_FROST_DURATION_MULTIPLIER = BUILDER
            .comment("冰霜属性生物获得灼烧时，持续时间的倍率（0.5 表示减少 50% 时长）。",
                    "Duration multiplier for Scorched effect on Frost entities (0.5 = -50% duration).",
                    "Default: 0.5")
            .defineInRange("scorched_frost_duration_multiplier", 0.5, 0.1, 100.0);
    BUILDER.comment(" ");

    POISON_SCORCH_DURATION_MULTIPLIER = BUILDER
            .comment("中毒目标受到赤焰伤害触发灼烧时，持续时间的倍率（1.5 表示增加 50% 时长）。",
                    "Duration multiplier for Scorched on poisoned entities. (1.5 = +50% duration)",
                    "Default: 1.5")
            .defineInRange("poison_scorch_duration_multiplier", 1.5, 0.1, 100.0);
    BUILDER.comment(" ");

    POISON_SCORCH_DAMAGE_MULTIPLIER = BUILDER
            .comment("中毒目标受到赤焰伤害触发灼烧时，灼烧伤害的倍率（1.5 表示增加 50% 伤害）。",
                    "Damage multiplier for Scorched on poisoned entities. (1.5 = +50% damage)",
                    "Default: 1.5")
            .defineInRange("poison_scorch_damage_multiplier", 1.5, 0.1, 100.0);
    BUILDER.comment(" ");

    SCORCHED_ENTITY_BLACKLIST = BUILDER
            .comment("灼烧效果免疫黑名单。",
                    "Scorched effect immunity blacklist.",
                    "Default: []")
            .defineListAllowEmpty("scorched_entity_blacklist", List.of(), o -> o instanceof String);
    BUILDER.comment(" ");

    BUILDER.push("scorched_aura");
    BUILDER.comment("灼烧光环 - Scorched Aura",
            "灼烧状态的生物对周围3x3范围（Y值一致）内的其他生物造成烫伤伤害，同时脚底出现火焰粒子光环。",
            "只有赤焰强化达到阈值触发的灼烧才能激活此效果。",
            "Scorched entities deal scalding damage to nearby 3x3 (same Y) entities and display a flame ring at their feet.",
            "Only scorched triggered by fire power reaching the threshold activates this effect.");
    BUILDER.comment(" ");

    SCORCHED_AURA_FIRE_POWER_THRESHOLD = BUILDER
            .comment("触发灼烧光环的赤焰属性强化值阈值，设为0则关闭灼烧光环。只有赤焰强化达到此值触发的灼烧才会产生光环效果。",
                    "Fire power threshold to activate Scorched Aura. Set to 0 to disable the Scorched Aura effect.",
                    "Default: 50")
            .defineInRange("scorched_aura_fire_power_threshold", 50, 0, 10000);
    BUILDER.comment(" ");


    SCORCHED_AURA_RADIUS = BUILDER
            .comment("灼烧光环的影响半径（格）。3.0 = 以灼烧目标为中心半径3格的圆形范围。",
                    "Scorched Aura effect radius (blocks). 3.0 = 3-block radius circle centered on scorched target.",
                    "Default: 3.0")
            .defineInRange("scorched_aura_radius", 3.0, 0.5, 10.0);
    BUILDER.comment(" ");

    SCORCHED_AURA_DAMAGE_INTERVAL = BUILDER
            .comment("灼烧光环内生物受到伤害的间隔（Tick），不影响灼烧主体。20 Tick = 1秒。",
                    "Interval (Ticks) for Scorched Aura damage ticks on nearby entities. Does not affect the main scorched target. 20 Ticks = 1 second.",
                    "Default: 20")
            .defineInRange("scorched_aura_damage_interval", 20, 1, 600);
    BUILDER.comment(" ");

    SCORCHED_AURA_STEAM_ENABLED = BUILDER
            .comment("灼烧光环内的生物有潮湿效果时，是否清除潮湿并触发高温蒸汽云？",
                    "Whether to clear Wetness and trigger High-Heat Steam Cloud for wet entities inside Scorched Aura.",
                    "Default: true")
            .define("scorched_aura_steam_enabled", true);
    BUILDER.comment(" ");

    SCORCHED_AURA_SPORE_DETONATION_ENABLED = BUILDER
            .comment("灼烧光环内的生物携带易燃孢子时，是否触发毒火爆燃（层数分流由爆燃机制内部处理）。",
                    "爆炸的赤焰属性强化由灼烧来源的sourceFirePower决定。",
                    "Whether to trigger Toxic Blast for entities inside Scorched Aura that carry Flammable Spores.",
                    "The stack-based weak/full blast behavior is handled internally by the Toxic Blast mechanism.",
                    "Default: true")
            .define("scorched_aura_spore_detonation_enabled", true);
    BUILDER.comment(" ");

    BUILDER.pop();
    BUILDER.pop();

    BUILDER.push("scorched_damage_multipliers");
    BUILDER.comment("Elemental Damage Multipliers against Scorched Targets",
                    "对灼烧目标的元素伤害倍率");

    SCORCHED_FROST_DMG_MULTIPLIER = BUILDER
            .comment("Damage multiplier when Frost attacks a scorched target.",
                    "冰霜属性攻击灼烧目标时的伤害倍率。",
                    "Default: 0.5")
            .defineInRange("scorched_frost_dmg_multiplier", 0.5, 0.0, 10.0);

    SCORCHED_NATURE_DMG_MULTIPLIER = BUILDER
            .comment("Damage multiplier when Nature attacks a scorched target.",
                    "自然属性攻击灼烧目标时的伤害倍率。",
                    "Default: 1.5")
            .defineInRange("scorched_nature_dmg_multiplier", 1.5, 0.0, 10.0);

    SCORCHED_FIRE_DMG_MULTIPLIER = BUILDER
            .comment("Damage multiplier when Fire attacks a scorched target.",
                    "赤焰属性攻击灼烧目标时的伤害倍率。",
                    "Default: 1.0")
            .defineInRange("scorched_fire_dmg_multiplier", 1.0, 0.0, 10.0);

    SCORCHED_THUNDER_DMG_MULTIPLIER = BUILDER
            .comment("Damage multiplier when Thunder attacks a scorched target.",
                    "雷霆属性攻击灼烧目标时的伤害倍率。",
                    "Default: 1.0")
            .defineInRange("scorched_thunder_dmg_multiplier", 1.0, 0.0, 10.0);

    BUILDER.pop();

    BUILDER.push("nature_reaction");
    BUILDER.comment("自然元素反应 - Nature Reaction System",
            "包含易燃孢子的施加、传染、以及被赤焰属性引爆（毒火爆燃）等核心机制。",
            "Includes Flammable Spore application, contagion spread, and detonation by Fire (Toxic Blast).");
    BUILDER.push("dynamic_parasitism");
    BUILDER.comment("动态寄生 - Dynamic Parasitism",
            "控制自然属性攻击施加“易燃孢子”的概率。概率受自然强化点数、攻击者自身的潮湿层数影响。",
            "Controls the chance for Nature attacks to apply Flammable Spores. Chance scales with Nature points and attacker's Wetness stacks.");
    BUILDER.comment(" ");

    NATURE_PARASITE_BASE_THRESHOLD = BUILDER
            .comment("攻击触发易燃孢子效果所需的最小自然属性强化点数，设为0则攻击不会获得易燃孢子。",
                    "Minimum Nature points required to trigger Flammable Spores effect on attack. Set to 0 to disable spore application.",
                    "Default: 5.0")
            .defineInRange("base_threshold", 5.0, 0.0, 10000.0);
    BUILDER.comment(" ");

    NATURE_PARASITE_BASE_CHANCE = BUILDER
            .comment("触发易燃孢子效果的基础概率。(0.1 = 10%)",
                    "Base chance to trigger Flammable Spores effect. (0.1 = 10%)",
                    "Default: 0.1")
            .defineInRange("base_chance", 0.1, 0.0, 1.0);
    BUILDER.comment(" ");

    NATURE_PARASITE_SCALING_STEP = BUILDER
            .comment("易燃孢子概率成长的属性阶梯值。例如设为20时，自然属性强化点数达到20/40/60点都会触发概率提升。",
                    "The step size for Flammable Spores chance scaling. E.g., if set to 20, chance increases at 20, 40, 60 points.",
                    "Default: 20.0")
            .defineInRange("scaling_step", 20.0, 1.0, 10000.0);
    BUILDER.comment(" ");

    NATURE_PARASITE_SCALING_CHANCE = BUILDER
            .comment("每个阶梯（等级）额外增加的易燃孢子触发概率。",
                    "Flammable Spores chance increase per tier.",
                    "Default: 0.1")
            .defineInRange("scaling_chance", 0.1, 0.0, 1.0);
    BUILDER.comment(" ");

    NATURE_PARASITE_AMOUNT = BUILDER
            .comment("每次触发效果时施加的易燃孢子层数。",
                    "Number of Flammable Spore stacks applied when effect is triggered.",
                    "Default: 1")
            .defineInRange("parasite_amount", 1, 1, 100);
    BUILDER.comment(" ");

    NATURE_PARASITE_WETNESS_BONUS = BUILDER
            .comment("自身每层潮湿提供的额外易燃孢子触发概率。",
                    "Extra Flammable Spores chance provided per stack of self-wetness.",
                    "Default: 0.1")
            .defineInRange("wetness_bonus", 0.1, 0.0, 1.0);
    BUILDER.comment(" ");

    NATURE_IMMUNITY_THRESHOLD = BUILDER
            .comment("完全免疫易燃孢子所需的自然抗性点数。",
                    "Nature Resistance points required to be completely immune to Flammable Spores.",
                    "Default: 80")
            .defineInRange("nature_immunity_threshold", 80, 0, 10000);
    BUILDER.pop();

    BUILDER.push("spore_system");
    BUILDER.comment("易燃孢子效果 - Spore Effect",
            "定义孢子层数带来的具体效果：周期性穿甲伤害、装备耐久侵蚀、赤焰易伤，以及不同属性宿主对持续时间的影响。",
            "Defines per-stack effects: periodic armor-piercing damage, equipment durability erosion, Fire vulnerability, and duration modifiers for different elemental hosts.");
    BUILDER.comment(" ");

    SPORE_MAX_STACKS = BUILDER
            .comment("易燃孢子的最大叠加层数。",
                    "Maximum stack amount for Flammable Spores.",
                    "Default: 5")
            .defineInRange("max_spore_stacks", 5, 1, 1000);
    BUILDER.comment(" ");

    SPORE_POISON_DAMAGE = BUILDER
            .comment("感染易燃孢子后，每次造成的无视护甲伤害。",
                    "Armor-bypassing damage per second when infected with Flammable Spores.",
                    "Default: 2.0")
            .defineInRange("spore_poison_damage", 2.0, 0.0, 200.0);
    BUILDER.comment(" ");

    SPORE_DAMAGE_INTERVAL = BUILDER
            .comment("易燃孢子伤害触发的间隔（Tick）。默认40 Tick = 2秒。",
                    "Interval (Ticks) for Flammable Spores damage ticks. Default 40 Ticks = 2 seconds.",
                    "Default: 40")
            .defineInRange("spore_damage_interval", 40, 1, 12000);
    BUILDER.comment(" ");


    SPORE_FIRE_VULN_PER_STACK = BUILDER
            .comment("每一层孢子增加受到的赤焰属性伤害比例。(0.1 = 10%)",
                    "#Percentage of Fire elemental vulnerability increased per Flammable Spore stack. (0.1 = 10%)",
                    "Default: 0.1")
            .defineInRange("spore_fire_vuln_per_stack", 0.1, 0.0, 1.0);
    BUILDER.comment(" ");

    SPORE_DURATION_PER_STACK = BUILDER
            .comment("每一层易燃孢子增加的基础持续时间（秒）。",
                    "Base duration (seconds) added per Flammable Spore stack.",
                    "Default: 5")
            .defineInRange("spore_duration_per_stack", 5, 1, 6000);
    BUILDER.comment(" ");

    SPORE_FIRE_DURATION_REDUCTION = BUILDER
            .comment("赤焰属性宿主的持续时间缩减比例。(0.5 = 时间减半)",
                    "Duration reduction multiplier for Fire hosts. (0.5 = Halved duration)",
                    "Default: 0.5")
            .defineInRange("spore_fire_duration_reduction", 0.5, 0.0, 1.0);
    BUILDER.comment(" ");

    SPORE_NATURE_DURATION_MULTIPLIER = BUILDER
            .comment("自然属性宿主的持续时间倍率。(1.0 = 不改变)",
                    "Duration multiplier for Nature hosts. (1.0 = no change)",
                    "Default: 1.0")
            .defineInRange("spore_nature_duration_multiplier", 1.0, 0.1, 100.0);
    BUILDER.comment(" ");

    SPORE_THUNDER_MULTIPLIER = BUILDER
            .comment("雷霆属性宿主的持续时间倍率。(2.0 = 时间翻倍)",
                    "Duration multiplier for Thunder hosts. (2.0 = Doubled duration)",
                    "Default: 2.0")
            .defineInRange("spore_thunder_multiplier", 2.0, 1.0, 5.0);
    BUILDER.comment(" ");

    SPORE_FROST_DURATION_MULTIPLIER = BUILDER
            .comment("冰霜属性宿主的持续时间倍率。(0.5 = 时间减半)",
                    "Duration multiplier for Frost hosts. (0.5 = Halved duration)",
                    "Default: 0.5")
            .defineInRange("spore_frost_duration_multiplier", 0.5, 0.1, 100.0);
    BUILDER.comment(" ");

    SPORE_ENTITY_BLACKLIST = BUILDER
            .comment("易燃孢子效果免疫黑名单（填入实体ID，例如：minecraft:creeper）。",
                    "Flammable Spore immunity blacklist (Entity IDs, e.g., minecraft:creeper).",
                    "Default: []")
            .defineListAllowEmpty("spore_entity_blacklist", List.of(), o -> o instanceof String);
    BUILDER.comment(" ");

    SPORE_DURABILITY_DAMAGE = BUILDER
            .comment("每秒对穿戴装备的耐久度侵蚀值。0 = 关闭此功能。",
                     "例如设为 1：每秒减少1点耐久。",
                     "层数决定侵蚀几件装备：1层→1件，2层→2件，5层→全部护甲+主手。",
                     "Durability damage per second to worn equipment. 0 = disable.",
                     "Stacks determine how many pieces are affected: 1→1, 2→2, 5→all armor+mainhand.",
                     "Default: 1")
            .defineInRange("spore_durability_damage", 1, 0, 100);
    BUILDER.pop();

    BUILDER.push("contagion_system");
    BUILDER.comment("传染系统 - Contagion System",
            "当生物身上的孢子层数达到阈值后，会周期性向周围生物传播孢子。",
            "When an entity's spore stacks reach the threshold, it periodically spreads spores to nearby entities.");
    BUILDER.comment(" ");

    SPORE_REACTION_THRESHOLD = BUILDER
            .comment("易燃孢子触发剧烈反应（传染扩散、毒火爆燃）所需的最小层数，设为0则关闭孢子元素反应。",
                    "Minimum Flammable Spore stacks required to trigger severe reactions (Contagion spread, Toxic Blast explosion). Set to 0 to disable all spore reactions.",
                    "Default: 3")
            .defineInRange("spore_reaction_threshold", 3, 0, 100);
    BUILDER.comment(" ");

    CONTAGION_BASE_RADIUS = BUILDER
            .comment("易燃孢子传染的基础半径（格）。",
                    "Base radius (blocks) for Flammable Spore contagion.",
                    "Default: 2.0")
            .defineInRange("contagion_base_radius", 2.0, 1.0, 16.0);
    BUILDER.comment(" ");

    CONTAGION_RADIUS_PER_STACK = BUILDER
            .comment("高层数时，每多一层增加的传染半径（格）。",
                    "Additional contagion radius (blocks) per extra stack at high levels.",
                    "Default: 1.0")
            .defineInRange("contagion_radius_per_stack", 1.0, 0.0, 5.0);
    BUILDER.comment(" ");

    CONTAGION_TRANSFER_BASE = BUILDER
            .comment("传染时，孢子层数减去该值即为转移层数（最小为1）。",
                     "例如设为 2：3层→1层，4层→2层，5层→3层。",
                     "Transfer stacks = max(1, source_stacks - this_value).",
                     "Example (value=2): 3 stacks→1, 4 stacks→2, 5 stacks→3.",
                     "Default: 2")
            .defineInRange("contagion_transfer_base", 2, 0, 10);
    BUILDER.comment(" ");

    CONTAGION_ONLY_HOSTILE = BUILDER
            .comment("是否只允许易燃孢子传染给敌对生物？如果为 true，玩家和被动生物将不会被环境传染。",
                    "Whether Flammable Spores should only spread to hostile entities. If true, players and passive mobs will not be infected by contagion.",
                    "Default: false")
            .define("contagion_only_hostile", false);
    BUILDER.comment(" ");

    CONTAGION_ALLOW_INFECTED_SPREAD = BUILDER
            .comment("被传染的目标在孢子层数达到阈值后是否也能触发传染机制？警告：在大量可被传染的生物（如村民）附近开启此选项可能会导致性能问题。",
                    "Can infected targets also trigger the contagion mechanism after their spore stacks reach the threshold?",
                    "Warning: Enabling this option near large groups of infectable entities (such as villagers) may cause performance issues.",
                    "Default: false")
            .define("contagion_allow_infected_spread", false);
    BUILDER.comment(" ");

    CONTAGION_ALLOW_REINFECTED = BUILDER
            .comment("当实体的孢子效果完全消失后，若再次获得孢子，是否允许重新触发传染（清除之前的感染标记）？",
                    "When an entity's spore effect completely disappears, if it gains spores again, is it allowed to re-trigger contagion (clearing previous infection marks)?",
                    "Default: true")
            .define("contagion_allow_reinfected", true);
    BUILDER.pop();

    BUILDER.push("wildfire_ejection");
    BUILDER.comment("自然反制 - Nature Counter",
            "自然属性生物被点燃时的一种反击机制：瞬间清除自身火焰并爆发出孢子云雾，击退周围敌人并施加孢子。",
            "A counter-attack for Nature entities when ignited: clears own fire, creates a spore cloud that knocks back enemies and applies spores.");
    BUILDER.comment(" ");

    WILDFIRE_TRIGGER_THRESHOLD = BUILDER
            .comment("触发自然反制（反击）所需的最小自然属性强化点数，设为0则关闭自然反制。",
                    "Minimum Nature points required to trigger Nature Counter (counter-attack). Set to 0 to disable.",
                    "Default: 20.0")
            .defineInRange("wildfire_trigger_threshold", 20.0, 0.0, 10000.0);
    BUILDER.comment(" ");

    WILDFIRE_COOLDOWN = BUILDER
            .comment("自然反制的冷却时间（Tick）。",
                    "Cooldown (Ticks) for Nature Counter.",
                    "Default: 200")
            .defineInRange("wildfire_cooldown", 200, 0, 60000);
    BUILDER.comment(" ");

    WILDFIRE_RADIUS = BUILDER
            .comment("反击爆炸的半径（格）。",
                    "Radius (blocks) of the counter-attack explosion.",
                    "Default: 3.0")
            .defineInRange("wildfire_radius", 3.0, 1.0, 16.0);
    BUILDER.comment(" ");

    WILDFIRE_KNOCKBACK = BUILDER
            .comment("反击造成的水平击退力度。",
                    "Horizontal knockback strength of the counter-attack.",
                    "Default: 1.5")
            .defineInRange("wildfire_knockback", 1.5, 0.0, 10.0);
    BUILDER.comment(" ");

    WILDFIRE_VERTICAL_KNOCKBACK = BUILDER
            .comment("反击造成的垂直击退力度。",
                    "Vertical knockback strength of the counter-attack.",
                    "Default: 0.5")
            .defineInRange("wildfire_vertical_knockback", 0.5, 0.0, 10.0);
    BUILDER.comment(" ");

    WILDFIRE_SPORE_AMOUNT = BUILDER
            .comment("反击时施加给敌人的孢子层数。",
                    "Number of Spore stacks applied to enemies during counter-attack.",
                    "Default: 2")
            .defineInRange("wildfire_spore_amount", 2, 0, 10);
    BUILDER.comment(" ");

    WILDFIRE_CLEAR_BURNING = BUILDER
            .comment("自然反制是否清除自身的燃烧和灼烧效果？如果为 false，则保留这些效果。",
                    "Whether Nature Counter clears burning and scorched effects from itself. If false, these effects are preserved.",
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
    BUILDER.comment(" ");

    BLAST_TRIGGER_THRESHOLD = BUILDER
            .comment("触发毒火爆燃（引爆易燃孢子）所需的最小赤焰属性强化点数，设为0则关闭毒火爆燃。",
                    "Minimum Fire points required to trigger Toxic Blast (detonate Flammable Spores). Set to 0 to disable Toxic Blast.",
                    "Default: 50.0")
            .defineInRange("blast_trigger_threshold", 50.0, 0.0, 10000.0);
    BUILDER.comment(" ");

     SPORE_ENVIRONMENTAL_BLAST_ENABLED = BUILDER
            .comment("Environmentally triggered Toxic Blast (magma, lava, fire, Nether).",
                     "生物在岩浆块/熔岩/火焰中受伤或进入下界时，直接触发毒火爆燃。",
                     "Default: true")
            .define("spore_environmental_blast_enabled", true);
    BUILDER.comment(" ");

    BLAST_WEAK_IGNITE_MULT = BUILDER
            .comment("当孢子层数不足时，弱效爆燃施加的灼烧伤害倍率。",
                    "该倍率直接乘以灼烧的每跳最终伤害。",
                    "例如：设为 1.5，则灼烧每跳伤害变为原来的 1.5 倍。",
                    "Scorch damage multiplier for weak blast (when spore stacks are below reaction threshold).",
                    "Directly multiplies the final Scorch damage per tick.",
                    "Example: 1.5 = 1.5x damage per tick.",
                    "Default: 1.5")
            .defineInRange("blast_weak_ignite_mult", 1.5, 1.0, 100.0);
    BUILDER.comment(" ");

    BLAST_BASE_DAMAGE = BUILDER
            .comment("达到反应阈值（默认3层）引爆时的基础爆炸伤害。",
                    "Base explosion damage when detonating at the reaction threshold (default 3).",
                    "Default: 5.0")
            .defineInRange("blast_base_damage", 5.0, 0.0, 1000.0);
    BUILDER.comment(" ");

    BLAST_DMG_STEP = BUILDER
            .comment("爆炸伤害提升一级所需的额外赤焰属性强化点数（基于触发阈值计算）。",
                    "即：赤焰强化点数超过 blast_trigger_threshold 后，每超出该步长值，爆炸伤害增加一级。",
                    "例如：阈值为50，步长为20，则70点强化提升1级，90点提升2级，以此类推。",
                    "Additional Fire points required to increase explosion damage tier (calculated above the trigger threshold).",
                    "Meaning: For every multiple of this step value beyond the blast_trigger_threshold, the explosion damage increases by one tier.",
                    "Example: threshold=50, step=20, then 70 points gives +1 tier, 90 points gives +2 tiers, etc.",
                    "Default: 25.0")
            .defineInRange("blast_dmg_step", 25.0, 1.0, 10000.0);
    BUILDER.comment(" ");

    BLAST_DMG_AMOUNT = BUILDER
            .comment("每一级提升增加的爆炸伤害点数。",
                    "Explosion damage added per tier.",
                    "Default: 2.0")
            .defineInRange("blast_dmg_amount", 2.0, 0.0, 1000.0);
    BUILDER.comment(" ");

    BLAST_GROWTH_DAMAGE = BUILDER
            .comment("超过反应阈值后，每多一层易燃孢子增加的爆炸伤害。",
                    "Bonus explosion damage per extra Flammable Spore stack above the threshold.",
                    "Default: 2.0")
            .defineInRange("blast_growth_damage", 2.0, 0.0, 1000.0);
    BUILDER.comment(" ");

    BLAST_BASE_RANGE = BUILDER
            .comment("基础爆炸半径（格）。",
                    "Base explosion radius (blocks).",
                    "Default: 1.5")
            .defineInRange("blast_base_range", 1.5, 0.5, 1000.0);
    BUILDER.comment(" ");

    BLAST_GROWTH_RANGE = BUILDER
            .comment("超过反应阈值后，每多一层易燃孢子增加的爆炸半径（格）。",
                    "Bonus explosion radius per extra Flammable Spore stack above the threshold.",
                    "Default: 1.0")
            .defineInRange("blast_growth_range", 1.0, 0.0, 5.0);
    BUILDER.comment(" ");

    BLAST_SCORCH_BASE = BUILDER
            .comment("弱效引燃（低于反应阈值）造成的灼烧持续时间（秒）。",
                    "Duration (seconds) of weak scorching applied by low stacks (below reaction threshold).",
                    "Default: 3.0")
            .defineInRange("blast_scorch_base", 3.0, 0.0, 6000.0);
    BUILDER.comment(" ");

    BLAST_BASE_SCORCH_TIME = BUILDER
            .comment("成功爆炸（达到反应阈值）后造成的灼烧持续时间（秒）。",
                    "Duration (seconds) of scorching applied by a successful explosion (at reaction threshold).",
                    "Default: 3.0")
            .defineInRange("blast_base_scorch_time", 3.0, 0.0, 6000.0);
    BUILDER.comment(" ");

    BLAST_GROWTH_SCORCH_TIME = BUILDER
            .comment("超过反应阈值后，每多一层易燃孢子增加的灼烧时间（秒）。",
                    "Bonus scorch time (seconds) per extra Flammable Spore stack above the threshold.",
                    "Default: 1.0")
            .defineInRange("blast_growth_scorch_time", 1.0, 0.0, 1000.0);
    BUILDER.comment(" ");

    BLAST_CHAIN_REACTION = BUILDER
            .comment("是否开启毒火爆燃的连锁反应机制？如果开启，当爆炸波及到身上有孢子的生物时，会立即诱发它们也发生爆炸（连环爆炸）。",
                    "Whether to enable the Chain Reaction mechanic for Toxic Blast. If enabled, detonating a spore-infected entity will recursively detonate other nearby infected entities immediately.",
                    "Default: true")
            .define("blast_chain_reaction", true);
    BUILDER.comment(" ");

    BLAST_MAX_BLAST_PROT_CAP = BUILDER
            .comment("“爆炸保护”附魔最多能抵消的爆燃伤害比例。(0.5 = 50%)",
                    "Maximum blast damage mitigation provided by 'Blast Protection' enchantment. (0.5 = 50%)",
                    "Default: 0.5")
            .defineInRange("blast_max_blast_prot_cap", 0.5, 0.0, 1.0);
    BUILDER.comment(" ");

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
    public static int wetnessShallowWaterCap;
    public static double wetnessFireReduction;
    public static int wetnessRainGainInterval;
    public static int wetnessDecayBaseTime;
    public static double wetnessExhaustionIncrease;
    public static int wetnessPotionAddLevel;
    public static int wetnessDryingThreshold;
    public static double wetnessSelfDryingDamagePenalty;
    public static int wetnessFireDryingTime;
    public static double wetnessHeatSearchRadius;
    public static boolean wetnessWaterAnimalImmune;
    public static boolean wetnessNetherDimensionImmune;
    public static List<? extends String> cachedWetnessBlacklist;
    public static int sporeMaxStacks;
    public static boolean sporeEnvironmentalBlastEnabled;
    public static int sporeReactionThreshold;
    public static double sporePoisonDamage;
    public static int sporeDamageInterval;
    public static double sporeFireVulnPerStack;
    public static int sporeDurationPerStack;
    public static double sporeThunderMultiplier;
    public static double sporeFireDurationReduction;
    public static double sporeNatureDurationMultiplier;
    public static double sporeFrostDurationMultiplier;
    public static List<? extends String> cachedSporeBlacklist;
    public static int sporeDurabilityDamage;
    public static int contagionTransferBase;
    public static double contagionBaseRadius;
    public static double contagionRadiusPerStack;
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
    public static int steamHighHeatMaxLevel;
    public static int steamLowHeatMaxLevel;
    public static double steamCloudRadius;
    public static double steamRadiusPerLevel;
    public static int steamCloudDuration;
    public static int steamDurationPerLevel;
    public static boolean steamClearAggro;
    public static double steamCloudHeightCeiling;
    public static int steamCondensationDelay;
    public static int steamCondensationDurationBase;
    public static int steamCondensationDurationPerLevel;

    public static double steamScaldingDamage;
    public static double steamScaldingMultiplierFire;
    public static double steamDamageScalePerLevel;
    public static double steamScaldingMultiplierNature;
    public static double steamScaldingMultiplierThunder;
    public static double steamScaldingMultiplierFrost;
    public static int steamImmunityThreshold;
    public static List<? extends String> cachedSteamBlacklist;
    public static int steamHighHeatTriggerThreshold;
    public static int steamLowHeatTriggerThreshold;
    public static int steamTriggerCooldown;
    public static double steamMaxFireProtCap;
    public static double steamMaxGeneralProtCap;
    public static int scorchedTriggerThreshold;
    public static double scorchedBaseChance;
    public static int scorchedChancePerPoint;
    public static int scorchedDuration;
    public static int scorchedCooldown;
    public static double scorchedDamageBase;
    public static int scorchedDamageScalingStep;
    public static int scorchedResistThreshold;
    public static double scorchedImmuneModifier;
    public static double scorchedFireProtReduction;
    public static double scorchedShockDamageRatio;
    public static double scorchedGenProtReduction;
    public static double scorchedFireDurationMultiplier;
    public static double scorchedNatureDurationMultiplier;
    public static double scorchedThunderDurationMultiplier;
    public static double scorchedFrostDurationMultiplier;
    public static double poisonScorchDurationMultiplier;
    public static double poisonScorchDamageMultiplier;
    public static List<? extends String> cachedScorchedBlacklist;
    public static int scorchedAuraFirePowerThreshold;
    public static double scorchedAuraRadius;
    public static int scorchedAuraDamageInterval;
    public static boolean scorchedAuraSteamEnabled;
    public static boolean scorchedAuraSporeDetonationEnabled;
    public static double scorchedFrostDmgMultiplier;
    public static double scorchedNatureDmgMultiplier;
    public static double scorchedFireDmgMultiplier;
    public static double scorchedThunderDmgMultiplier;

    public static void refreshCache() {
        wetnessMaxLevel = WETNESS_MAX_LEVEL.get();
        wetnessShallowWaterCap = WETNESS_SHALLOW_WATER_CAP.get();
        wetnessFireReduction = WETNESS_FIRE_REDUCTION.get();
        wetnessRainGainInterval = WETNESS_RAIN_GAIN_INTERVAL.get();
        wetnessDecayBaseTime = WETNESS_DECAY_BASE_TIME.get();
        wetnessExhaustionIncrease = WETNESS_EXHAUSTION_INCREASE.get();
        wetnessPotionAddLevel = WETNESS_POTION_ADD_LEVEL.get();
        wetnessDryingThreshold = WETNESS_DRYING_THRESHOLD.get();
        wetnessSelfDryingDamagePenalty = WETNESS_SELF_DRYING_DAMAGE_PENALTY.get();
        wetnessFireDryingTime = WETNESS_FIRE_DRYING_TIME.get();
        wetnessHeatSearchRadius = WETNESS_HEAT_SEARCH_RADIUS.get();
        wetnessWaterAnimalImmune = WETNESS_WATER_ANIMAL_IMMUNE.get();
        wetnessNetherDimensionImmune = WETNESS_NETHER_DIMENSION_IMMUNE.get();
        cachedWetnessBlacklist = WETNESS_ENTITY_BLACKLIST.get();
        sporeMaxStacks = SPORE_MAX_STACKS.get();
        sporeEnvironmentalBlastEnabled = SPORE_ENVIRONMENTAL_BLAST_ENABLED.get();
        sporeReactionThreshold = SPORE_REACTION_THRESHOLD.get();
        sporePoisonDamage = SPORE_POISON_DAMAGE.get();
        sporeDamageInterval = SPORE_DAMAGE_INTERVAL.get();
        sporeFireVulnPerStack = SPORE_FIRE_VULN_PER_STACK.get();
        sporeDurationPerStack = SPORE_DURATION_PER_STACK.get();
        sporeFireDurationReduction = SPORE_FIRE_DURATION_REDUCTION.get();
        sporeNatureDurationMultiplier = SPORE_NATURE_DURATION_MULTIPLIER.get();
        sporeThunderMultiplier = SPORE_THUNDER_MULTIPLIER.get();
        sporeFrostDurationMultiplier = SPORE_FROST_DURATION_MULTIPLIER.get();
        cachedSporeBlacklist = SPORE_ENTITY_BLACKLIST.get();
        sporeDurabilityDamage = SPORE_DURABILITY_DAMAGE.get();
        contagionTransferBase = CONTAGION_TRANSFER_BASE.get();
        contagionBaseRadius = CONTAGION_BASE_RADIUS.get();
        contagionRadiusPerStack = CONTAGION_RADIUS_PER_STACK.get();
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
        steamHighHeatMaxLevel = STEAM_HIGH_HEAT_MAX_LEVEL.get();
        steamLowHeatMaxLevel = STEAM_LOW_HEAT_MAX_LEVEL.get();
        steamHighHeatTriggerThreshold = STEAM_HIGH_HEAT_TRIGGER_THRESHOLD.get();
        steamLowHeatTriggerThreshold = STEAM_LOW_HEAT_TRIGGER_THRESHOLD.get();
        steamTriggerCooldown = STEAM_TRIGGER_COOLDOWN.get();
        steamCloudRadius = STEAM_CLOUD_RADIUS.get();
        steamRadiusPerLevel = STEAM_RADIUS_PER_LEVEL.get();
        steamCloudDuration = STEAM_CLOUD_DURATION.get();
        steamDurationPerLevel = STEAM_DURATION_PER_LEVEL.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        steamCloudHeightCeiling = STEAM_CLOUD_HEIGHT_CEILING.get();
        steamCondensationDelay = STEAM_CONDENSATION_DELAY.get();
        steamCondensationDurationBase = STEAM_CONDENSATION_DURATION_BASE.get();
        steamCondensationDurationPerLevel = STEAM_CONDENSATION_DURATION_PER_LEVEL.get();

        steamScaldingDamage = STEAM_SCALDING_DAMAGE.get();
        steamScaldingMultiplierFire = STEAM_SCALDING_MULTIPLIER_FIRE.get();
        steamDamageScalePerLevel = STEAM_DAMAGE_SCALE_PER_LEVEL.get();
        steamScaldingMultiplierNature = STEAM_SCALDING_MULTIPLIER_NATURE.get();
        steamScaldingMultiplierThunder = STEAM_SCALDING_MULTIPLIER_THUNDER.get();
        steamScaldingMultiplierFrost = STEAM_SCALDING_MULTIPLIER_FROST.get();
        steamImmunityThreshold = STEAM_IMMUNITY_THRESHOLD.get();
        cachedSteamBlacklist = STEAM_IMMUNITY_BLACKLIST.get();
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
        scorchedFireDurationMultiplier = SCORCHED_FIRE_DURATION_MULTIPLIER.get();
        scorchedNatureDurationMultiplier = SCORCHED_NATURE_DURATION_MULTIPLIER.get();
        scorchedThunderDurationMultiplier = SCORCHED_THUNDER_DURATION_MULTIPLIER.get();
        scorchedFrostDurationMultiplier = SCORCHED_FROST_DURATION_MULTIPLIER.get();
        poisonScorchDurationMultiplier = POISON_SCORCH_DURATION_MULTIPLIER.get();
        poisonScorchDamageMultiplier = POISON_SCORCH_DAMAGE_MULTIPLIER.get();
        cachedScorchedBlacklist = SCORCHED_ENTITY_BLACKLIST.get();
        scorchedAuraFirePowerThreshold = SCORCHED_AURA_FIRE_POWER_THRESHOLD.get();
        scorchedAuraRadius = SCORCHED_AURA_RADIUS.get();
        scorchedAuraDamageInterval = SCORCHED_AURA_DAMAGE_INTERVAL.get();
        scorchedAuraSteamEnabled = SCORCHED_AURA_STEAM_ENABLED.get();
        scorchedAuraSporeDetonationEnabled = SCORCHED_AURA_SPORE_DETONATION_ENABLED.get();
        scorchedFrostDmgMultiplier = SCORCHED_FROST_DMG_MULTIPLIER.get();
        scorchedNatureDmgMultiplier = SCORCHED_NATURE_DMG_MULTIPLIER.get();
        scorchedFireDmgMultiplier = SCORCHED_FIRE_DMG_MULTIPLIER.get();
        scorchedThunderDmgMultiplier = SCORCHED_THUNDER_DMG_MULTIPLIER.get();
    }

    @SuppressWarnings("deprecation")
    public static void register(String fileName) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, fileName);
    }
}