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
    public static final ForgeConfigSpec.IntValue CONTAGION_WETNESS_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue CONTAGION_CONSUMES_WETNESS;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_WETNESS_CONVERSION_RATIO;
    public static final ForgeConfigSpec.IntValue CONTAGION_WETNESS_MAX_BONUS;
    public static final ForgeConfigSpec.BooleanValue CONTAGION_ONLY_HOSTILE;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_BASE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_SCALING_STEP;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_SCALING_CHANCE;
    public static final ForgeConfigSpec.IntValue NATURE_PARASITE_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_WETNESS_BONUS;
    public static final ForgeConfigSpec.IntValue NATURE_IMMUNITY_THRESHOLD;
    public static final ForgeConfigSpec.IntValue NATURE_SIPHON_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NATURE_DRAIN_POWER_STEP;
    public static final ForgeConfigSpec.IntValue NATURE_DRAIN_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue NATURE_SIPHON_HEAL;
    public static final ForgeConfigSpec.IntValue NATURE_DRAIN_COOLDOWN;
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
    public static final ForgeConfigSpec.IntValue STEAM_BLINDNESS_DURATION;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;
    public static final ForgeConfigSpec.IntValue STEAM_CHECK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCAN_RADIUS_MULTIPLIER;
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
    public static final ForgeConfigSpec.DoubleValue SCORCHED_BURNING_LOCK_DURATION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_DAMAGE_BASE;
    public static final ForgeConfigSpec.IntValue SCORCHED_DAMAGE_SCALING_STEP;
    public static final ForgeConfigSpec.IntValue SCORCHED_RESIST_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_IMMUNE_MODIFIER;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_FIRE_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_SHOCK_DAMAGE_RATIO;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_GEN_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_NATURE_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCORCHED_ENTITY_BLACKLIST;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        BUILDER.comment("Elemental Reaction System Configuration",
                "元素反应系统配置 - 调整赤焰和自然元素交互的平衡性。",
                "Elemental Reaction System Configuration - Adjust balances for fire and nature interactions.")
                .push("wetness_system");
        WETNESS_MAX_LEVEL = BUILDER
                .comment("潮湿状态的最高叠加层数。",
                        "The maximum stack amount for Wetness status.")
                .defineInRange("wetness_max_level", 5, 1, 1000);
        WETNESS_SHALLOW_WATER_CAP_RATIO = BUILDER
                .comment("在浅水中（仅脚部接触水）时，获得的潮湿层数上限比例。(0.6 = 最大值的60%)",
                        "The cap ratio for Wetness stacks when in shallow water (feet only). (0.6 = 60% of max)")
                .defineInRange("wetness_shallow_water_cap_ratio", 0.6, 0.0, 1.0);
        WETNESS_FIRE_REDUCTION = BUILDER
                .comment("每一层潮湿抵挡赤焰属性伤害的百分比。(0.1 = 10% 减伤)",
                        "Percentage of fire damage reduction provided by each stack of Wetness. (0.1 = 10% reduction)")
                .defineInRange("wetness_fire_reduction", 0.1, 0.0, 1.0);
        WETNESS_MAX_REDUCTION = BUILDER
                .comment("潮湿状态提供的最大赤焰属性伤害减免比例。(0.9 = 90%)",
                        "Maximum fire damage reduction provided by Wetness status. (0.9 = 90%)")
                .defineInRange("wetness_max_reduction", 0.9, 0.0, 1.0);
        WETNESS_RAIN_GAIN_INTERVAL = BUILDER
                .comment("在雨中站立时，增加一层潮湿所需的时间（秒）。",
                        "Time (in seconds) required to gain a Wetness stack while standing in rain.")
                .defineInRange("wetness_rain_gain_interval", 10, 1, 3600);
        WETNESS_DECAY_BASE_TIME = BUILDER
                .comment("离开水源后，每一层潮湿自然消退所需的基础时间（秒）。",
                        "Base time (in seconds) for each Wetness stack to decay naturally after leaving water.")
                .defineInRange("wetness_decay_base_time", 10, 1, 3600);
        WETNESS_EXHAUSTION_INCREASE = BUILDER
                .comment("潮湿状态下，饥饿感（饱食度消耗）的增加倍率。",
                        "Multiplier for hunger exhaustion when under Wetness status.")
                .defineInRange("wetness_exhaustion_increase", 0.05, 0.0, 10.0);
        WETNESS_POTION_ADD_LEVEL = BUILDER
                .comment("被喷溅水瓶击中时，瞬间增加的潮湿层数。",
                        "Number of Wetness stacks instantly added when hit by a Splash Water Bottle.")
                .defineInRange("wetness_potion_add_level", 1, 1, 100);
        WETNESS_DRYING_THRESHOLD = BUILDER
                .comment("瞬间蒸发1层潮湿所需的赤焰属性强化点数阈值。",
                        "Threshold of Fire Attribute points required to instantly evaporate 1 layer of Wetness.")
                .defineInRange("wetness_drying_threshold", 20, 1, 1000);
        WETNESS_SELF_DRYING_DAMAGE_PENALTY = BUILDER
                .comment("赤焰生物自我蒸干潮湿时，造成的伤害降低比例。(0.3 = 降低30%)",
                        "Damage reduction penalty when a Fire entity tries to dry itself. (0.3 = 30% reduction)")
                .defineInRange("wetness_self_drying_damage_penalty", 0.3, 0.0, 1.0);
        WETNESS_FIRE_DRYING_TIME = BUILDER
                .comment("站在火中烧多少秒可瞬间清除所有潮湿效果。",
                        "Seconds required to stand in fire to instantly clear all Wetness effects.")
                .defineInRange("wetness_fire_drying_time", 2, 1, 600);
        WETNESS_TICK_INTERVAL = BUILDER
                .comment("潮湿状态逻辑更新的间隔（Tick）。",
                        "Interval (Ticks) for updating Wetness logic (decay/accumulation).")
                .defineInRange("wetness_tick_interval", 20, 1, 1200);
        WETNESS_HEAT_SEARCH_RADIUS = BUILDER
                .comment("检测周围热源（熔岩/岩浆块）的半径范围（格）。注意：岩浆块的检测半径会-1格。",
                        "Radius (blocks) to search for nearby heat sources (Lava/Magma). Note: Magma Block detection radius is automatically reduced by 1.")
                .defineInRange("wetness_heat_search_radius", 2.0, 1.0, 16.0);
        BUILDER.push("immunity");
        WETNESS_WATER_ANIMAL_IMMUNE = BUILDER
                .comment("水生生物（如鱼、鱿鱼）是否完全免疫潮湿效果？",
                        "Are water animals (e.g., fish, squids) completely immune to Wetness?")
                .define("water_animal_immune", true);
        WETNESS_NETHER_DIMENSION_IMMUNE = BUILDER
                .comment("下界维度的生物是否天生免疫潮湿效果？",
                        "Are entities in the Nether dimension naturally immune to Wetness?")
                .define("nether_dimension_immune", true);
        WETNESS_ENTITY_BLACKLIST = BUILDER
                .comment("潮湿效果免疫黑名单（填入实体ID）。",
                        "Wetness immunity blacklist (Entity IDs).")
                .defineListAllowEmpty("wetness_entity_blacklist", List.of(), o -> o instanceof String);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("spore_system");
        SPORE_MAX_STACKS = BUILDER
                .comment("易燃孢子的最大叠加层数。",
                        "Maximum stack amount for Flammable Spores.")
                .defineInRange("max_spore_stacks", 5, 1, 1000);
        SPORE_REACTION_THRESHOLD = BUILDER
                .comment("易燃孢子触发剧烈反应（传染扩散、毒火爆燃）所需的最小层数。",
                        "Minimum Flammable Spore stacks required to trigger severe reactions (Contagion spread, Toxic Blast explosion).")
                .defineInRange("spore_reaction_threshold", 3, 1, 100);
        SPORE_POISON_DAMAGE = BUILDER
                .comment("感染易燃孢子后，每次造成的无视护甲伤害。",
                        "Armor-bypassing damage per second when infected with Flammable Spores.")
                .defineInRange("spore_poison_damage", 0.5, 0.0, 200.0);
        SPORE_DAMAGE_INTERVAL = BUILDER
                .comment("易燃孢子伤害触发的间隔（Tick）。默认100 Tick = 5秒。",
                        "Interval (Ticks) for Flammable Spores damage ticks. Default 100 Ticks = 5 seconds.")
                .defineInRange("spore_damage_interval", 100, 1, 12000);
        SPORE_SPEED_REDUCTION = BUILDER
                .comment("每一层易燃孢子效果造成的减速比例。(0.1 = 10%)",
                        "Percentage of slowness applied per Flammable Spore stack. (0.1 = 10%)")
                .defineInRange("spore_speed_reduction", 0.1, 0.0, 0.5);
        SPORE_PHYS_RESIST = BUILDER
                .comment("每一层易燃孢子提供的物理伤害减免比例。(0.05 = 5%)",
                        "Percentage of physical resistance provided per Flammable Spore stack. (0.05 = 5%)")
                .defineInRange("spore_phys_resist", 0.05, 0.0, 0.5);
        SPORE_FIRE_VULN_PER_STACK = BUILDER
                .comment("每一层孢子增加的赤焰属性和高温蒸汽易伤比例。(0.1 = 10%)",
                        "Percentage of Fire and High-Heat Steam vulnerability increased per Flammable Spore stack. (0.1 = 10%)")
                .defineInRange("spore_fire_vuln_per_stack", 0.1, 0.0, 1.0);
        SPORE_DURATION_PER_STACK = BUILDER
                .comment("每一层易燃孢子增加的基础持续时间（秒）。",
                        "Base duration (seconds) added per Flammable Spore stack.")
                .defineInRange("spore_duration_per_stack", 5, 1, 6000);
        SPORE_THUNDER_MULTIPLIER = BUILDER
                .comment("雷霆属性宿主的持续时间倍率。(2.0 = 时间翻倍)",
                        "Duration multiplier for Thunder attribute hosts. (2.0 = Doubled duration)")
                .defineInRange("spore_thunder_multiplier", 2.0, 1.0, 5.0);
        SPORE_FIRE_DURATION_REDUCTION = BUILDER
                .comment("赤焰属性宿主的持续时间缩减比例。(0.5 = 时间减半)",
                        "Duration reduction multiplier for Fire attribute hosts. (0.5 = Halved duration)")
                .defineInRange("spore_fire_duration_reduction", 0.5, 0.0, 1.0);
        SPORE_ENTITY_BLACKLIST = BUILDER
                .comment("易燃孢子效果免疫黑名单（填入实体ID，例如：minecraft:creeper）。",
                        "Flammable Spore immunity blacklist (Entity IDs, e.g., minecraft:creeper).")
                .defineListAllowEmpty("spore_entity_blacklist", List.of(), o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("contagion_system");
        CONTAGION_CHECK_INTERVAL = BUILDER
                .comment("感染易燃孢子后触发传染所需时间（Tick）。",
                        "Interval (Ticks) required to trigger contagion after being infected with Flammable Spores.")
                .defineInRange("contagion_check_interval", 20, 1, 12000);
        CONTAGION_BASE_RADIUS = BUILDER
                .comment("易燃孢子传染的基础半径（格）。",
                        "Base radius (blocks) for Flammable Spore contagion.")
                .defineInRange("contagion_base_radius", 2.0, 1.0, 16.0);
        CONTAGION_RADIUS_PER_STACK = BUILDER
                .comment("高层数时，每多一层增加的传染半径（格）。",
                        "Additional contagion radius (blocks) per extra stack at high levels.")
                .defineInRange("contagion_radius_per_stack", 1.0, 0.0, 5.0);
        CONTAGION_INTENSITY_RATIO = BUILDER
                .comment("传染时，传递给受害者的易燃孢子层数比例。(0.2 = 20%)",
                        "Ratio of Flammable Spore stacks transferred to the victim during contagion. (0.2 = 20%)")
                .defineInRange("contagion_intensity_ratio", 0.2, 0.0, 1.0);
        CONTAGION_WETNESS_THRESHOLD = BUILDER
                .comment("触发潮湿加速繁殖所需的最小潮湿层数。",
                        "Minimum Wetness stacks required to trigger accelerated spore reproduction.")
                .defineInRange("contagion_wetness_threshold", 1, 0, 1000);
        CONTAGION_CONSUMES_WETNESS = BUILDER
                .comment("传染发生时，是否消耗受害者的潮湿状态？",
                        "Whether to consume the victim's Wetness status when contagion occurs.")
                .define("contagion_consumes_wetness", true);
        CONTAGION_WETNESS_CONVERSION_RATIO = BUILDER
                .comment("被传染的目标每层潮湿效果可转化为多少层额外的易燃孢子。",
                        "Ratio of extra Flammable Spore stacks converted from each Wetness level of the infected target.")
                .defineInRange("contagion_wetness_conversion_ratio", 1.0, 0.0, 10.0);
        CONTAGION_WETNESS_MAX_BONUS = BUILDER
                .comment("通过潮湿转化能获得的最大额外易燃孢子层数。",
                        "Maximum extra Flammable Spore stacks obtainable through Wetness conversion.")
                .defineInRange("contagion_wetness_max_bonus", 5, 0, 500);
        CONTAGION_ONLY_HOSTILE = BUILDER
                .comment("是否只允许易燃孢子传染给敌对生物？如果为 true，玩家和被动生物将不会被环境传染。",
                        "Whether Flammable Spores should only spread to hostile entities. If true, players and passive mobs will not be infected by contagion.")
                .define("contagion_only_hostile", false);
        BUILDER.pop();

        BUILDER.push("nature_reaction");
        BUILDER.push("dynamic_parasitism");
        NATURE_PARASITE_BASE_THRESHOLD = BUILDER
                .comment("攻击触发易燃孢子效果所需的最小自然属性强化点数。",
                        "Minimum Nature attribute points required to trigger Flammable Spores effect on attack.")
                .defineInRange("base_threshold", 5.0, 0.0, 10000.0);
        NATURE_PARASITE_BASE_CHANCE = BUILDER
                .comment("触发易燃孢子效果的基础概率。(0.05 = 5%)",
                        "Base chance to trigger Flammable Spores effect. (0.05 = 5%)")
                .defineInRange("base_chance", 0.05, 0.0, 1.0);
        NATURE_PARASITE_SCALING_STEP = BUILDER
                .comment("易燃孢子概率成长的属性阶梯值。例如设为20时，自然属性强化点数达到20/40/60点都会触发概率提升。",
                        "The attribute step size for Flammable Spores chance scaling. E.g., if set to 20, chance increases at 20, 40, 60 points.")
                .defineInRange("scaling_step", 20.0, 1.0, 10000.0);
        NATURE_PARASITE_SCALING_CHANCE = BUILDER
                .comment("每个阶梯（等级）额外增加的易燃孢子触发概率。",
                        "Flammable Spores chance increase per tier.")
                .defineInRange("scaling_chance", 0.05, 0.0, 1.0);
        NATURE_PARASITE_AMOUNT = BUILDER
                .comment("每次触发效果时施加的易燃孢子层数。",
                        "Number of Flammable Spore stacks applied when effect is triggered.")
                .defineInRange("parasite_amount", 1, 1, 100);
        NATURE_PARASITE_WETNESS_BONUS = BUILDER
                .comment("自身每层潮湿提供的额外易燃孢子触发概率。",
                        "Extra Flammable Spores chance provided per stack of self-wetness.")
                .defineInRange("wetness_bonus", 0.05, 0.0, 1.0);
        NATURE_IMMUNITY_THRESHOLD = BUILDER
                .comment("完全免疫易燃孢子所需的自然抗性点数。",
                        "Nature Resistance points required to be completely immune to Flammable Spores.")
                .defineInRange("nature_immunity_threshold", 80, 0, 10000);
        BUILDER.pop();

        BUILDER.push("parasitic_drain");
        NATURE_SIPHON_THRESHOLD = BUILDER
                .comment("触发寄生吸取（吸取目标潮湿效果到自身，然后恢复自身血量）所需的最小自然属性强化点数。",
                        "Minimum Nature attribute points required to trigger Parasitic Drain (absorb target's Wetness to restore health).")
                .defineInRange("nature_drain_threshold", 20, 1, 10000);
        NATURE_DRAIN_POWER_STEP = BUILDER
                .comment("每增加一级吸取目标潮湿等级所需的自然属性强化点数。",
                        "Nature attribute points required to increase Wetness drain amount by one level.")
                .defineInRange("nature_drain_power_step", 20.0, 1.0, 10000.0);
        NATURE_DRAIN_AMOUNT = BUILDER
                .comment("吸取目标潮湿效果基础层数。",
                        "Base number of Wetness stacks drained from the target.")
                .defineInRange("nature_drain_amount_per_step", 1, 1, 1000);
        NATURE_SIPHON_HEAL = BUILDER
                .comment("每吸取一层潮湿自身恢复的血量。",
                        "Health points restored to self per Wetness stack drained.")
                .defineInRange("nature_heal_amount", 1.0, 0.0, 1000.0);
        NATURE_DRAIN_COOLDOWN = BUILDER
                .comment("寄生吸取的冷却时间（Tick）。",
                        "Cooldown (Ticks) for Parasitic Drain.")
                .defineInRange("nature_drain_cooldown", 200, 0, 60000);
        BUILDER.pop();

        BUILDER.push("wildfire_ejection");
        WILDFIRE_TRIGGER_THRESHOLD = BUILDER
                .comment("触发野火喷射（反击）所需的最小自然属性强化点数。",
                        "Minimum Nature attribute points required to trigger Wildfire Ejection (counter-attack).")
                .defineInRange("wildfire_trigger_threshold", 20.0, 0.0, 10000.0);
        WILDFIRE_COOLDOWN = BUILDER
                .comment("野火喷射的冷却时间（Tick）。",
                        "Cooldown (Ticks) for Wildfire Ejection.")
                .defineInRange("wildfire_cooldown", 200, 0, 60000);
        WILDFIRE_RADIUS = BUILDER
                .comment("反击爆炸的半径（格）。",
                        "Radius (blocks) of the counter-attack explosion.")
                .defineInRange("wildfire_radius", 3.0, 1.0, 16.0);
        WILDFIRE_KNOCKBACK = BUILDER
                .comment("反击造成的水平击退力度。",
                        "Horizontal knockback strength of the counter-attack.")
                .defineInRange("wildfire_knockback", 1.5, 0.0, 10.0);
        WILDFIRE_VERTICAL_KNOCKBACK = BUILDER
                .comment("反击造成的垂直击退力度。",
                        "Vertical knockback strength of the counter-attack.")
                .defineInRange("wildfire_vertical_knockback", 0.5, 0.0, 10.0);
        WILDFIRE_SPORE_AMOUNT = BUILDER
                .comment("反击时施加给敌人的孢子层数。",
                        "Number of Spore stacks applied to enemies during counter-attack.")
                .defineInRange("wildfire_spore_amount", 2, 0, 10);
        WILDFIRE_CLEAR_BURNING = BUILDER
                .comment("野火喷射是否清除目标身上的燃烧和灼烧效果？如果为 false，则保留这些效果。",
                        "Whether Wildfire Ejection clears burning and scorched effects from targets. If false, these effects are preserved.")
                .define("wildfire_clear_burning", false);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("fire_reaction");
        BUILDER.push("toxic_blast");
        BLAST_TRIGGER_THRESHOLD = BUILDER
                .comment("触发毒火爆燃（引爆易燃孢子）所需的最小赤焰属性强化点数。",
                        "Minimum Fire attribute points required to trigger Toxic Blast (detonate Flammable Spores).")
                .defineInRange("blast_trigger_threshold", 50.0, 0.0, 10000.0);
        BLAST_WEAK_IGNITE_MULT = BUILDER
                .comment("低层数（低于反应阈值）时的引燃伤害倍率。",
                        "Ignite damage multiplier for low stacks (below reaction threshold).")
                .defineInRange("blast_weak_ignite_mult", 1.5, 1.0, 100.0);
        BLAST_BASE_DAMAGE = BUILDER
                .comment("达到反应阈值（默认3层）引爆时的基础爆炸伤害。",
                        "Base explosion damage when detonating at the reaction threshold (default 3).")
                .defineInRange("blast_base_damage", 5.0, 0.0, 1000.0);
        BLAST_DMG_STEP = BUILDER
                .comment("爆炸伤害提升一级所需的赤焰属性强化点数。",
                        "Fire attribute points required to increase explosion damage tier.")
                .defineInRange("blast_dmg_step", 20.0, 1.0, 10000.0);
        BLAST_DMG_AMOUNT = BUILDER
                .comment("每一级提升增加的爆炸伤害点数。",
                        "Explosion damage added per tier.")
                .defineInRange("blast_dmg_amount", 1.0, 0.0, 1000.0);
        BLAST_GROWTH_DAMAGE = BUILDER
                .comment("超过反应阈值后，每多一层易燃孢子增加的爆炸伤害。",
                        "Bonus explosion damage per extra Flammable Spore stack above the threshold.")
                .defineInRange("blast_growth_damage", 1.0, 0.0, 1000.0);
        BLAST_BASE_RANGE = BUILDER
                .comment("基础爆炸半径（格）。",
                        "Base explosion radius (blocks).")
                .defineInRange("blast_base_range", 1.5, 0.5, 1000.0);
        BLAST_GROWTH_RANGE = BUILDER
                .comment("超过反应阈值后，每多一层易燃孢子增加的爆炸半径（格）。",
                        "Bonus explosion radius per extra Flammable Spore stack above the threshold.")
                .defineInRange("blast_growth_range", 1.0, 0.0, 5.0);
        BLAST_SCORCH_BASE = BUILDER
                .comment("弱效引燃（低于反应阈值）造成的灼烧持续时间（秒）。",
                        "Duration (seconds) of weak scorching applied by low stacks (below reaction threshold).")
                .defineInRange("blast_scorch_base", 3.0, 0.0, 6000.0);
        BLAST_BASE_SCORCH_TIME = BUILDER
                .comment("成功爆炸（达到反应阈值）后造成的灼烧持续时间（秒）。",
                        "Duration (seconds) of scorching applied by a successful explosion (at reaction threshold).")
                .defineInRange("blast_base_scorch_time", 3.0, 0.0, 6000.0);
        BLAST_GROWTH_SCORCH_TIME = BUILDER
                .comment("超过反应阈值后，每多一层易燃孢子增加的灼烧时间（秒）。",
                        "Bonus scorch time (seconds) per extra Flammable Spore stack above the threshold.")
                .defineInRange("blast_growth_scorch_time", 1.0, 0.0, 1000.0);
        BLAST_CHAIN_REACTION = BUILDER
                .comment("是否开启毒火爆燃的连锁反应机制？如果开启，当爆炸波及到身上有孢子的生物时，会立即诱发它们也发生爆炸（连环爆炸）。",
                        "Whether to enable the Chain Reaction mechanic for Toxic Blast. If enabled, detonating a spore-infected entity will recursively detonate other nearby infected entities immediately.")
                .define("blast_chain_reaction", true);
        BLAST_MAX_BLAST_PROT_CAP = BUILDER
                .comment("“爆炸保护”附魔最多能抵消的爆燃伤害比例。(0.5 = 50%)",
                        "Maximum blast damage mitigation provided by 'Blast Protection' enchantment. (0.5 = 50%)")
                .defineInRange("blast_max_blast_prot_cap", 0.5, 0.0, 1.0);
        BLAST_MAX_GENERAL_PROT_CAP = BUILDER
                .comment("普通“保护”附魔最多能抵消的爆燃伤害比例。(0.25 = 25%)",
                        "Maximum blast damage mitigation provided by general 'Protection' enchantment. (0.25 = 25%)")
                .defineInRange("blast_max_general_prot_cap", 0.25, 0.0, 1.0);
        ENCHANTMENT_CALCULATION_DENOMINATOR = BUILDER
                .comment("用于计算附魔保护比例的分母。",
                        "默认值 16.0 意味着需要4件装备都附魔保护IV（4 * 4 = 16）才能达到设定的最大减伤比例。",
                        "The denominator used for calculating enchantment protection ratio.",
                        "Default 16.0 means full protection (100% weight) requires 4 armor pieces with Protection IV (4 * 4 = 16).")
                .defineInRange("enchantment_calculation_denominator", 16.0, 1.0, 1000.0);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("steam_reaction");
        STEAM_REACTION_ENABLED = BUILDER
                .comment("是否开启蒸汽反应机制（水火相遇产生蒸汽）？",
                        "Whether to enable the Steam Reaction mechanism (Water meets Fire).")
                .define("steam_reaction_enabled", true);
        STEAM_HIGH_HEAT_MAX_LEVEL = BUILDER
                .comment("高温蒸汽云（火攻水）的最高等级。",
                        "Maximum level for High-Heat Steam clouds (Fire attacks Water).")
                .defineInRange("steam_high_heat_max_level", 5, 1, 10000);
        STEAM_LOW_HEAT_MAX_LEVEL = BUILDER
                .comment("低温蒸汽云（冰攻火）的最高等级。",
                        "Maximum level for Low-Heat Steam clouds (Frost attacks Fire).")
                .defineInRange("steam_low_heat_max_level", 5, 1, 10000);
        STEAM_MAX_REDUCTION = BUILDER
                .comment("全局伤害修正（增伤或减伤）的上限比例。(0.9 = 90%)",
                        "Global cap for damage modification (increase or reduction). (0.9 = 90%)")
                .defineInRange("steam_max_reduction", 0.9, 0.0, 1.0);
        BUILDER.push("cloud_properties");
        STEAM_CLOUD_RADIUS = BUILDER
                .comment("蒸汽云的基础半径（格）。",
                        "Base radius (blocks) for steam clouds.")
                .defineInRange("steam_cloud_radius", 2.0, 0.5, 1000.0);
        STEAM_RADIUS_PER_LEVEL = BUILDER
                .comment("每增加一级（潮湿层数），蒸汽云增加的半径（格）。",
                        "Additional radius (blocks) per steam level (wetness stack).")
                .defineInRange("steam_radius_per_level", 0.5, 0.0, 5.0);
        STEAM_CLOUD_DURATION = BUILDER
                .comment("蒸汽云的基础存在时间（Tick）。",
                        "Base duration (Ticks) for steam clouds.")
                .defineInRange("steam_cloud_duration", 100, 20, 12000);
        STEAM_DURATION_PER_LEVEL = BUILDER
                .comment("每增加一级，蒸汽云增加的存在时间（Tick）。",
                        "Additional duration (Ticks) per steam level.")
                .defineInRange("steam_duration_per_level", 20, 0, 2000);
        STEAM_BLINDNESS_DURATION = BUILDER
                .comment("走进蒸汽云时，失明效果的持续时间（Tick）。",
                        "Duration (Ticks) of Blindness effect when entering a steam cloud.")
                .defineInRange("steam_blindness_duration", 60, 20, 6000);
        STEAM_CLEAR_AGGRO = BUILDER
                .comment("蒸汽云是否会遮蔽视线，强制清除里面怪物的仇恨目标？",
                        "Whether steam clouds obscure vision and force mobs inside to lose aggro.")
                .define("steam_clear_aggro", true);
        STEAM_CHECK_INTERVAL = BUILDER
                .comment("实体检测周围蒸汽云的间隔（Tick）。",
                        "Interval (Ticks) for entities to scan for nearby steam clouds.")
                .defineInRange("steam_check_interval", 10, 1, 1200);
        STEAM_SCAN_RADIUS_MULTIPLIER = BUILDER
                .comment("检测蒸汽云时，搜索范围相对于云半径的倍率。",
                        "Multiplier for the search radius when looking for steam clouds.")
                .defineInRange("steam_scan_radius_multiplier", 3.0, 1.0, 10.0);
        STEAM_CLOUD_HEIGHT_CEILING = BUILDER
                .comment("蒸汽云向上飘散的有效高度（格）。超过这个高度不造成烫伤。",
                        "Effective height ceiling (blocks) for rising steam clouds. No scalding above this height.")
                .defineInRange("steam_cloud_height_ceiling", 3.0, 0.0, 16.0);
        BUILDER.pop();

        BUILDER.push("condensation_logic");
        STEAM_CONDENSATION_STEP_FROST = BUILDER
                .comment("产生低温蒸汽时，提升一级蒸汽等级所需的冰霜属性强化点数。",
                        "Frost attribute points required to increase Low-Heat Steam level by one.")
                .defineInRange("steam_condensation_step_frost", 20, 1, 10000);
        STEAM_CONDENSATION_STEP_FIRE = BUILDER
                .comment("产生高温蒸汽时，提升一级蒸汽等级所需的赤焰属性强化点数。",
                        "Fire attribute points required to increase High-Heat Steam level by one.")
                .defineInRange("steam_condensation_step_fire", 20, 1, 10000);
        STEAM_CONDENSATION_DELAY = BUILDER
                .comment("在低温蒸汽里待多久（Tick）才会获得潮湿效果？",
                        "Time (Ticks) required to stay in Low-Heat Steam to gain Wetness.")
                .defineInRange("steam_condensation_delay", 100, 1, 24000);
        STEAM_CONDENSATION_DURATION_BASE = BUILDER
                .comment("低温蒸汽云的基础持续时间（Tick）。",
                        "Base duration (Ticks) for Low-Heat Steam clouds.")
                .defineInRange("steam_condensation_duration_base", 100, 20, 12000);
        STEAM_CONDENSATION_DURATION_PER_LEVEL = BUILDER
                .comment("低温蒸汽每升一级增加的持续时间（Tick）。",
                        "Additional duration (Ticks) per level for Low-Heat Steam.")
                .defineInRange("steam_condensation_duration_per_level", 20, 0, 2000);
        STEAM_SPORE_GROWTH_RATE = BUILDER
                .comment("在低温蒸汽中，易燃孢子繁殖的间隔时间（Tick）。",
                        "Interval (Ticks) for Flammable Spore reproduction inside Low-Heat Steam.")
                .defineInRange("steam_spore_growth_rate", 20, 1, 6000);
        BUILDER.pop();

        BUILDER.push("scalding_damage");
        STEAM_SCALDING_DAMAGE = BUILDER
                .comment("高温蒸汽每秒造成的基础烫伤伤害。",
                        "Base scalding damage per second from High-Heat Steam.")
                .defineInRange("steam_scalding_damage", 1.0, 0.0, 10000.0);
        STEAM_DAMAGE_SCALE_PER_LEVEL = BUILDER
                .comment("蒸汽等级每升一级，烫伤伤害增加的比例。(0.2 = +20%)",
                        "Percentage increase in scalding damage per steam level. (0.2 = +20%)")
                .defineInRange("steam_damage_scale_per_level", 0.2, 0.0, 10.0);
        STEAM_SCALDING_MULTIPLIER_WEAKNESS = BUILDER
                .comment("冰霜/自然属性生物受到高温蒸汽伤害的倍率。",
                        "Damage multiplier for Frost/Nature attribute entities in High-Heat Steam.")
                .defineInRange("steam_scalding_multiplier_weakness", 1.5, 1.0, 1000.0);
        STEAM_SCALDING_MULTIPLIER_SPORE = BUILDER
                .comment("携带易燃孢子的生物受到蒸汽烫伤伤害的倍率。",
                        "Damage multiplier for Flammable Spore infected entities in steam.")
                .defineInRange("steam_scalding_multiplier_spore", 1.5, 1.0, 1000.0);
        STEAM_IMMUNITY_THRESHOLD = BUILDER
                .comment("完全免疫蒸汽烫伤所需的赤焰抗性点数。",
                        "Fire Resistance points required to be completely immune to steam scalding.")
                .defineInRange("steam_immunity_threshold", 80, 0, 1000);
        STEAM_IMMUNITY_BLACKLIST = BUILDER
                .comment("蒸汽烫伤免疫黑名单（填入实体ID）。",
                        "Steam scalding immunity blacklist (Entity IDs).")
                .defineListAllowEmpty("steam_immunity_blacklist", List.of(), o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("trigger_logic");
        STEAM_TRIGGER_THRESHOLD_FIRE = BUILDER
                .comment("攻击时，触发高温蒸汽所需的最小赤焰属性强化点数。",
                        "Minimum Fire attribute points required to trigger High-Heat Steam (evaporate water/ice).")
                .defineInRange("fire_trigger_threshold", 20, 0, 1000);
        STEAM_TRIGGER_THRESHOLD_FROST = BUILDER
                .comment("攻击时，触发低温蒸汽所需的最小冰霜属性强化点数。",
                        "Minimum Frost attribute points required to trigger Low-Heat Steam (cool down fire).")
                .defineInRange("frost_trigger_threshold", 20, 0, 1000);
        STEAM_TRIGGER_COOLDOWN = BUILDER
                .comment("触发蒸汽反应后的冷却时间（Tick）。",
                        "Cooldown (Ticks) applied to an entity after triggering a steam reaction.")
                .defineInRange("steam_trigger_cooldown", 200, 0, 6000);
        STEAM_MAX_FIRE_PROT_CAP = BUILDER
                .comment("火焰保护附魔最多能抵消的蒸汽伤害比例。(0.5 = 50%)",
                        "Maximum steam damage mitigation provided by 'Fire Protection' enchantment. (0.5 = 50%)")
                .defineInRange("max_fire_prot_cap", 0.5, 0.0, 1.0);
        STEAM_MAX_GENERAL_PROT_CAP = BUILDER
                .comment("普通保护附魔最多能抵消的蒸汽伤害比例。(0.25 = 25%)",
                        "Maximum steam damage mitigation provided by general 'Protection' enchantment. (0.25 = 25%)")
                .defineInRange("max_general_prot_cap", 0.25, 0.0, 1.0);
        STEAM_DAMAGE_FLOOR_RATIO = BUILDER
                .comment("冰霜/自然属性生物的蒸汽伤害保底比例。",
                        "无论附魔提供多少减伤，它们至少要承受原始伤害的这一比例。（0.1 = 10%）",
                        "Minimum damage floor ratio for vulnerable entities (Frost/Nature), regardless of resistance. (0.1 = 10%)")
                .defineInRange("damage_floor_ratio", 0.1, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("Scorched Mechanic Configuration",
                "灼烧机制配置 - 一种无法轻易熄灭的强力燃烧。",
                "Scorched Mechanic Configuration - A powerful burning effect that is hard to extinguish.")
                .push("scorched_mechanic");
        SCORCHED_TRIGGER_THRESHOLD = BUILDER
                .comment("攻击触发“灼烧”效果所需的最小赤焰属性强化点数。",
                        "Minimum Fire attribute points required to trigger 'Scorched' effect on attack.")
                .defineInRange("scorched_trigger_threshold", 20, 1, 10000);
        SCORCHED_BASE_CHANCE = BUILDER
                .comment("触发灼烧的基础概率。(0.2 = 20%)",
                        "Base chance to trigger Scorched effect. (0.2 = 20%)")
                .defineInRange("scorched_base_chance", 0.2, 0.0, 1.0);
        SCORCHED_CHANCE_PER_POINT = BUILDER
                .comment("每点赤焰属性增加的触发概率。(0.001 = 0.1%)",
                        "Chance increase per Fire attribute point. (0.001 = 0.1%)")
                .defineInRange("scorched_chance_per_point", 0.001, 0.0, 0.1);
        SCORCHED_DURATION = BUILDER
                .comment("灼烧状态的持续时间（Tick）。(100 Tick = 5秒)",
                        "Duration (Ticks) of the Scorched effect. (100 Ticks = 5 seconds)")
                .defineInRange("scorched_duration", 100, 20, 12000);
        SCORCHED_COOLDOWN = BUILDER
                .comment("灼烧效果触发后的冷却时间（Tick）。",
                        "Cooldown (Ticks) after triggering the Scorched effect.")
                .defineInRange("scorched_cooldown", 200, 0, 6000);
        SCORCHED_BURNING_LOCK_DURATION = BUILDER
                .comment("灼烧生效时，强制显示火焰特效的时间（秒）。",
                        "Duration (seconds) to force render fire visuals when Scorched is active.")
                .defineInRange("scorched_burning_lock_duration", 3.0, 1.0, 60.0);
        SCORCHED_DAMAGE_BASE = BUILDER
                .comment("灼烧每秒造成的基础伤害。",
                        "Base damage per second from Scorched effect.")
                .defineInRange("scorched_damage_base", 1.0, 0.1, 10000.0);
        SCORCHED_DAMAGE_SCALING_STEP = BUILDER
                .comment("灼烧伤害增加0.5点所需的赤焰属性增量。",
                        "Fire attribute increment required to increase Scorched damage by 0.5.")
                .defineInRange("scorched_damage_scaling_step", 20, 1, 10000);
        SCORCHED_RESIST_THRESHOLD = BUILDER
                .comment("完全免疫灼烧伤害所需的赤焰抗性点数。",
                        "Fire Resistance points required to be completely immune to Scorched damage.")
                .defineInRange("scorched_resist_threshold", 80, 1, 10000);
        SCORCHED_IMMUNE_MODIFIER = BUILDER
                .comment("天生免疫火的生物受到灼烧伤害的倍率。(0.5 = 50%)",
                        "Damage multiplier for naturally fire-immune entities when Scorched. (0.5 = 50%)")
                .defineInRange("scorched_immune_modifier", 0.5, 0.0, 1.0);
        SCORCHED_FIRE_PROT_REDUCTION = BUILDER
                .comment("火焰保护附魔最多能抵消的灼烧伤害比例。",
                        "Maximum Scorched damage mitigation provided by 'Fire Protection' enchantment.")
                .defineInRange("scorched_fire_prot_reduction", 0.5, 0.0, 1.0);
        SCORCHED_SHOCK_DAMAGE_RATIO = BUILDER
                .comment("热休克（灼烧遇水）时，剩余持续伤害瞬间结算的比例。(0.5 = 50%)",
                        "Ratio of remaining DOT damage dealt instantly during Thermal Shock. (0.5 = 50%)")
                .defineInRange("scorched_shock_damage_ratio", 0.5, 0.0, 10.0);
        SCORCHED_GEN_PROT_REDUCTION = BUILDER
                .comment("普通保护附魔最多能抵消的灼烧伤害比例。",
                        "Maximum Scorched damage mitigation provided by general 'Protection' enchantment.")
                .defineInRange("scorched_gen_prot_reduction", 0.25, 0.0, 1.0);
        SCORCHED_NATURE_MULTIPLIER = BUILDER
                .comment("自然属性生物受到灼烧伤害的倍率。",
                        "Damage multiplier for Nature attribute entities when Scorched.")
                .defineInRange("scorched_nature_multiplier", 1.5, 1.0, 10.0);
        SCORCHED_ENTITY_BLACKLIST = BUILDER
                .comment("灼烧效果免疫黑名单。",
                        "Scorched effect immunity blacklist.")
                .defineListAllowEmpty("scorched_entity_blacklist", List.of(), o -> o instanceof String);
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
    public static int contagionWetnessThreshold;
    public static boolean contagionConsumesWetness;
    public static double contagionWetnessConversionRatio;
    public static int contagionWetnessMaxBonus;
    public static boolean contagionOnlyHostile;
    public static double natureParasiteBaseThreshold;
    public static double natureParasiteBaseChance;
    public static double natureParasiteScalingStep;
    public static double natureParasiteScalingChance;
    public static int natureParasiteAmount;
    public static double natureParasiteWetnessBonus;
    public static int natureImmunityThreshold;
    public static int natureSiphonThreshold;
    public static double natureDrainPowerStep;
    public static int natureDrainAmount;
    public static double natureSiphonHeal;
    public static int natureDrainCooldown;
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
    public static int steamBlindnessDuration;
    public static boolean steamClearAggro;
    public static int steamCheckInterval;
    public static double steamScanRadiusMultiplier;
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
    public static double scorchedBurningLockDuration;
    public static double scorchedDamageBase;
    public static int scorchedDamageScalingStep;
    public static int scorchedResistThreshold;
    public static double scorchedImmuneModifier;
    public static double scorchedFireProtReduction;
    public static double scorchedShockDamageRatio;
    public static double scorchedGenProtReduction;
    public static double scorchedNatureMultiplier;
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
        contagionWetnessThreshold = CONTAGION_WETNESS_THRESHOLD.get();
        contagionConsumesWetness = CONTAGION_CONSUMES_WETNESS.get();
        contagionWetnessConversionRatio = CONTAGION_WETNESS_CONVERSION_RATIO.get();
        contagionWetnessMaxBonus = CONTAGION_WETNESS_MAX_BONUS.get();
        contagionOnlyHostile = CONTAGION_ONLY_HOSTILE.get();
        natureParasiteBaseThreshold = NATURE_PARASITE_BASE_THRESHOLD.get();
        natureParasiteBaseChance = NATURE_PARASITE_BASE_CHANCE.get();
        natureParasiteScalingStep = NATURE_PARASITE_SCALING_STEP.get();
        natureParasiteScalingChance = NATURE_PARASITE_SCALING_CHANCE.get();
        natureParasiteAmount = NATURE_PARASITE_AMOUNT.get();
        natureParasiteWetnessBonus = NATURE_PARASITE_WETNESS_BONUS.get();
        natureImmunityThreshold = NATURE_IMMUNITY_THRESHOLD.get();
        natureSiphonThreshold = NATURE_SIPHON_THRESHOLD.get();
        natureDrainPowerStep = NATURE_DRAIN_POWER_STEP.get();
        natureDrainAmount = NATURE_DRAIN_AMOUNT.get();
        natureSiphonHeal = NATURE_SIPHON_HEAL.get();
        natureDrainCooldown = NATURE_DRAIN_COOLDOWN.get();
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
        steamBlindnessDuration = STEAM_BLINDNESS_DURATION.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        steamCheckInterval = STEAM_CHECK_INTERVAL.get();
        steamScanRadiusMultiplier = STEAM_SCAN_RADIUS_MULTIPLIER.get();
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
        scorchedBurningLockDuration = SCORCHED_BURNING_LOCK_DURATION.get();
        scorchedDamageBase = SCORCHED_DAMAGE_BASE.get();
        scorchedDamageScalingStep = SCORCHED_DAMAGE_SCALING_STEP.get();
        scorchedResistThreshold = SCORCHED_RESIST_THRESHOLD.get();
        scorchedImmuneModifier = SCORCHED_IMMUNE_MODIFIER.get();
        scorchedFireProtReduction = SCORCHED_FIRE_PROT_REDUCTION.get();
        scorchedShockDamageRatio = SCORCHED_SHOCK_DAMAGE_RATIO.get();
        scorchedGenProtReduction = SCORCHED_GEN_PROT_REDUCTION.get();
        scorchedNatureMultiplier = SCORCHED_NATURE_MULTIPLIER.get();
        cachedScorchedBlacklist = SCORCHED_ENTITY_BLACKLIST.get();
    }

    @SuppressWarnings("deprecation")
    public static void register(String fileName) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, fileName);
    }
}