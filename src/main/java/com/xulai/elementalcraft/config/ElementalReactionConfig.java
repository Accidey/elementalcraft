// src/main/java/com/xulai/elementalcraft/config/ElementalReactionConfig.java
package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

/**
 * ElementalReactionConfig
 * <p>
 * 中文说明：
 * 元素反应系统的专用配置文件类。
 * 此类定义了模组中所有元素交互的数值参数，允许玩家或腐竹调整游戏平衡性。
 * 主要配置板块包括：潮湿机制、孢子感染、传染扩散、自然系反应（吸取/反击）、赤焰系反应（爆燃）以及蒸汽反应。
 * <p>
 * English Description:
 * Dedicated configuration class for the Elemental Reaction System.
 * This class defines all numerical parameters for elemental interactions, allowing players or server admins to adjust game balance.
 * Main configuration sections include: Wetness Mechanics, Spore Infection, Contagion Spread, Nature Reactions (Siphon/Counter), Fire Reactions (Blast), and Steam Reactions.
 */
public class ElementalReactionConfig {
    public static final ForgeConfigSpec SPEC;

    // ========================================================================
    // 1. 潮湿系统配置 (Wetness System)
    // 功能：控制生物如何获得潮湿状态、潮湿的消退速度、以及潮湿对火属性伤害的防御效果。
    // Function: Controls how entities gain wetness, decay rates, and fire damage resistance.
    // ========================================================================
    public static final ForgeConfigSpec.IntValue WETNESS_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue WETNESS_SHALLOW_WATER_CAP_RATIO;
    public static final ForgeConfigSpec.DoubleValue WETNESS_FIRE_REDUCTION;
    public static final ForgeConfigSpec.IntValue WETNESS_RAIN_GAIN_INTERVAL;
    public static final ForgeConfigSpec.IntValue WETNESS_DECAY_BASE_TIME;
    public static final ForgeConfigSpec.DoubleValue WETNESS_EXHAUSTION_INCREASE;
    public static final ForgeConfigSpec.IntValue WETNESS_POTION_ADD_LEVEL;
    public static final ForgeConfigSpec.IntValue WETNESS_DRYING_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue WETNESS_SELF_DRYING_DAMAGE_PENALTY;
    public static final ForgeConfigSpec.IntValue WETNESS_FIRE_DRYING_TIME;
    public static final ForgeConfigSpec.BooleanValue WETNESS_WATER_ANIMAL_IMMUNE;
    public static final ForgeConfigSpec.BooleanValue WETNESS_NETHER_DIMENSION_IMMUNE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WETNESS_ENTITY_BLACKLIST;

    // ========================================================================
    // 2. 孢子系统配置 (Spore System)
    // 功能：定义易燃孢子的伤害、减速、易伤倍率以及不同元素属性宿主的特殊加成。
    // Function: Defines spore damage, slowness, vulnerability multipliers, and special bonuses for different elemental hosts.
    // ========================================================================
    public static final ForgeConfigSpec.IntValue SPORE_MAX_STACKS;
    public static final ForgeConfigSpec.DoubleValue SPORE_POISON_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SPORE_SPEED_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SPORE_PHYS_RESIST;
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_VULN_PER_STACK;
    public static final ForgeConfigSpec.IntValue SPORE_DURATION_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue SPORE_THUNDER_MULTIPLIER;
    // Removed SPORE_FIRE_REDUCTION
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_DURATION_REDUCTION;

    // ========================================================================
    // 3. 传染系统配置 (Contagion System)
    // 功能：控制孢子如何在生物群中扩散，以及潮湿环境如何加速孢子的繁殖。
    // Function: Controls how spores spread among mobs and how wet environments accelerate spore reproduction.
    // ========================================================================
    public static final ForgeConfigSpec.IntValue CONTAGION_CHECK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_BASE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_RADIUS_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_INTENSITY_RATIO;
    public static final ForgeConfigSpec.IntValue CONTAGION_WETNESS_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue CONTAGION_CONSUMES_WETNESS;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_WETNESS_CONVERSION_RATIO;
    public static final ForgeConfigSpec.IntValue CONTAGION_WETNESS_MAX_BONUS;

    // ========================================================================
    // 4. 自然反应配置 (Nature Reaction)
    // 功能：配置自然属性的特殊能力，包括“动态寄生”（攻击挂孢子）、“寄生吸取”（吸潮湿回血）和“野火喷射”（受火反击）。
    // Function: Configures Nature attribute abilities: "Dynamic Parasitism" (Attack applies spores), "Parasitic Drain" (Drain wetness to heal), and "Wildfire Ejection" (Counter fire).
    // ========================================================================
    
    // 动态寄生 (Dynamic Parasitism)
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_BASE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_SCALING_STEP;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_SCALING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_WETNESS_BONUS;

    // 寄生吸取 (Parasitic Drain)
    public static final ForgeConfigSpec.IntValue NATURE_SIPHON_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NATURE_DRAIN_POWER_STEP;
    public static final ForgeConfigSpec.IntValue NATURE_DRAIN_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue NATURE_SIPHON_HEAL;
    public static final ForgeConfigSpec.IntValue NATURE_DRAIN_COOLDOWN;

    // 野火喷射 (Wildfire Ejection)
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.IntValue WILDFIRE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue WILDFIRE_SPORE_AMOUNT;

    // ========================================================================
    // 5. 赤焰反应配置 (Fire Reaction)
    // 功能：主要配置“毒火爆燃”机制，定义爆炸伤害、灼烧时长以及与孢子层数的成长关系。
    // Function: Mainly configures the "Toxic Blast" mechanic, defining explosion damage, scorch duration, and scaling with spore stacks.
    // ========================================================================
    public static final ForgeConfigSpec.DoubleValue BLAST_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue BLAST_WEAK_IGNITE_MULT;
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_DMG_STEP;
    public static final ForgeConfigSpec.DoubleValue BLAST_DMG_AMOUNT;
    
    // 动态成长 (Dynamic Growth)
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_RANGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_RANGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_SCORCH_BASE;
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_SCORCH_TIME;
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_SCORCH_TIME;
    
    // 防御上限 (Defense Caps)
    public static final ForgeConfigSpec.DoubleValue BLAST_MAX_BLAST_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue BLAST_MAX_GENERAL_PROT_CAP;

    // ========================================================================
    // 6. 蒸汽反应配置 (Steam Reaction)
    // 功能：定义冰/水与火相遇时产生的蒸汽云。包含云的大小、持续时间、致盲效果以及高温烫伤伤害。
    // Function: Defines steam clouds formed when Ice/Water meets Fire. Includes cloud size, duration, blindness, and scalding damage.
    // ========================================================================
    
    // 全局与限制 (Global & Limits)
    public static final ForgeConfigSpec.BooleanValue STEAM_REACTION_ENABLED;
    public static final ForgeConfigSpec.IntValue STEAM_HIGH_HEAT_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_LOW_HEAT_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_REDUCTION;

    // 蒸汽云属性 (Cloud Properties)
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_RADIUS;
    public static final ForgeConfigSpec.DoubleValue STEAM_RADIUS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_CLOUD_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_DURATION_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_BLINDNESS_DURATION;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_HEIGHT_CEILING;

    // 冷凝逻辑 (Condensation Logic)
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_STEP_FIRE;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_STEP_FROST;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DELAY;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DURATION_BASE;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DURATION_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_SPORE_GROWTH_RATE;

    // 烫伤伤害 (Scalding Damage)
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_SCALE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_WEAKNESS;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_SPORE;
    public static final ForgeConfigSpec.IntValue STEAM_IMMUNITY_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STEAM_IMMUNITY_BLACKLIST;

    // 触发逻辑 (Trigger Logic)
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_THRESHOLD_FIRE;
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_THRESHOLD_FROST;

    // 防御系统 (Defense System)
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_FLOOR_RATIO;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_FIRE_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_GENERAL_PROT_CAP;

    // ========================================================================
    // 7. 灼烧机制配置 (Scorched Mechanic)
    // 功能：定义一种独立于原版“着火”的特殊燃烧状态，无法被水轻易熄灭，造成持续伤害。
    // Function: Defines a special burning status separate from vanilla fire, harder to extinguish with water, dealing continuous damage.
    // ========================================================================
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
    public static final ForgeConfigSpec.DoubleValue SCORCHED_GEN_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_NATURE_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCORCHED_ENTITY_BLACKLIST;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Elemental Reaction System Configuration", "元素反应系统配置 - 调整所有元素交互的平衡性")
                .push("wetness_system");

        WETNESS_MAX_LEVEL = BUILDER
                .comment("Max Wetness Stacks", 
                        "潮湿状态最高能叠加多少层？")
                .defineInRange("wetness_max_level", 5, 1, 100);

        WETNESS_SHALLOW_WATER_CAP_RATIO = BUILDER
                .comment("Shallow Water Cap Ratio",
                        "在浅水中（仅脚部接触水）时，获得的潮湿层数会被限制在最大值的百分之多少？(0.6 = 60%)")
                .defineInRange("wetness_shallow_water_cap_ratio", 0.6, 0.0, 1.0);

        WETNESS_FIRE_REDUCTION = BUILDER
                .comment("Fire Damage Reduction per Stack",
                        "每一层潮湿可以抵挡百分之多少的火属性伤害？(0.1 = 10% 减伤)")
                .defineInRange("wetness_fire_reduction", 0.1, 0.0, 1.0);

        WETNESS_RAIN_GAIN_INTERVAL = BUILDER
                .comment("Rain Gain Interval (Seconds)",
                        "在雨中站立时，每隔多少秒增加一层潮湿？")
                .defineInRange("wetness_rain_gain_interval", 10, 1, 3600);

        WETNESS_DECAY_BASE_TIME = BUILDER
                .comment("Natural Decay Time (Seconds)",
                        "离开水源后，每一层潮湿自然变干需要多少秒？")
                .defineInRange("wetness_decay_base_time", 10, 1, 3600);

        WETNESS_EXHAUSTION_INCREASE = BUILDER
                .comment("Hunger Drain Multiplier",
                        "身体湿透时，饥饿感（饱食度消耗）会增加多少倍？")
                .defineInRange("wetness_exhaustion_increase", 0.05, 0.0, 10.0);

        WETNESS_POTION_ADD_LEVEL = BUILDER
                .comment("Splash Water Bottle Stacks",
                        "被喷溅水瓶砸中时，瞬间增加多少层潮湿？")
                .defineInRange("wetness_potion_add_level", 1, 1, 100);

        WETNESS_DRYING_THRESHOLD = BUILDER
                .comment("Fire Drying Threshold",
                        "受到火属性攻击时，对方需要多少赤焰强化点数才能瞬间蒸发你身上的1层潮湿？")
                .defineInRange("wetness_drying_threshold", 20, 1, 1000);

        WETNESS_SELF_DRYING_DAMAGE_PENALTY = BUILDER
                .comment("Self-Drying Damage Penalty",
                        "当赤焰属性生物自己身上湿了并试图用火蒸干时，造成的伤害会降低多少？(0.3 = 降低30%)")
                .defineInRange("wetness_self_drying_damage_penalty", 0.3, 0.0, 1.0);

        WETNESS_FIRE_DRYING_TIME = BUILDER
                .comment("Fire Standing Dry Time",
                        "直接站在火里烧多少秒可以瞬间清除所有潮湿效果？")
                .defineInRange("wetness_fire_drying_time", 2, 1, 600);

        BUILDER.push("immunity");
        WETNESS_WATER_ANIMAL_IMMUNE = BUILDER
                .comment("Water Animal Immunity",
                        "水生生物（如鱼、鱿鱼）是否完全免疫潮湿效果？")
                .define("water_animal_immune", true);

        WETNESS_NETHER_DIMENSION_IMMUNE = BUILDER
                .comment("Nether Dimension Immunity",
                        "下界维度的生物是否天生免疫潮湿效果？")
                .define("nether_dimension_immune", true);

        WETNESS_ENTITY_BLACKLIST = BUILDER
                .comment("Wetness Blacklist",
                        "潮湿效果免疫黑名单（填入实体ID，例如 minecraft:blaze）。")
                .defineListAllowEmpty("wetness_entity_blacklist", List.of(), o -> o instanceof String);
        BUILDER.pop();
        BUILDER.pop();

        // Spore System
        BUILDER.push("spore_system");
        SPORE_MAX_STACKS = BUILDER
                .comment("Max Spore Stacks",
                        "易燃孢子最高能叠加多少层？")
                .defineInRange("max_spore_stacks", 5, 1, 100);

        SPORE_POISON_DAMAGE = BUILDER
                .comment("Poison Damage Per Second",
                        "感染孢子后，每秒受到多少点无视护甲的固定伤害？")
                .defineInRange("spore_poison_damage", 0.5, 0.0, 20.0);

        SPORE_SPEED_REDUCTION = BUILDER
                .comment("Slowness Per Stack",
                        "每一层孢子会让移动速度和攻击速度降低多少？(0.1 = 10% 减速)")
                .defineInRange("spore_speed_reduction", 0.1, 0.0, 0.5);

        SPORE_PHYS_RESIST = BUILDER
                .comment("Physical Resistance Per Stack",
                        "孢子会在皮肤表面形成硬壳，每一层提供多少物理减伤？(0.05 = 5% 减伤)")
                .defineInRange("spore_phys_resist", 0.05, 0.0, 0.5);

        SPORE_FIRE_VULN_PER_STACK = BUILDER
                .comment("Fire Vulnerability Per Stack",
                        "每一层孢子会让宿主受到的火属性和蒸汽伤害增加多少？(0.1 = 10% 易伤)")
                .defineInRange("spore_fire_vuln_per_stack", 0.1, 0.0, 1.0);

        SPORE_DURATION_PER_STACK = BUILDER
                .comment("Duration Per Stack (Seconds)",
                        "每一层孢子能持续存在多少秒？(基础5秒 + 每层增加秒数)")
                .defineInRange("spore_duration_per_stack", 5, 1, 60);
        
        SPORE_THUNDER_MULTIPLIER = BUILDER
                .comment("Thunder Host Multiplier",
                        "雷霆属性的生物感染孢子时，获得的层数（持续时间）会翻几倍？(默认 2.0 倍)")
                .defineInRange("spore_thunder_multiplier", 2.0, 1.0, 5.0);
        
        SPORE_FIRE_DURATION_REDUCTION = BUILDER
                .comment("Fire Host Duration Reduction",
                        "赤焰属性的生物感染孢子时，持续时间只有正常的多少？(默认 0.5 = 50%)")
                .defineInRange("spore_fire_duration_reduction", 0.5, 0.0, 1.0);
        BUILDER.pop();

        // Contagion System
        BUILDER.push("contagion_system");
        CONTAGION_CHECK_INTERVAL = BUILDER
                .comment("Check Interval (Ticks)",
                        "系统多久检测一次孢子是否传染给周围生物？(20 Tick = 1秒)")
                .defineInRange("contagion_check_interval", 20, 1, 1200);

        CONTAGION_BASE_RADIUS = BUILDER
                .comment("Contagion Radius",
                        "孢子传染的基础范围是多少格？")
                .defineInRange("contagion_base_radius", 2.0, 1.0, 16.0);

        CONTAGION_RADIUS_PER_STACK = BUILDER
                .comment("Radius Increase Per Stack",
                        "当孢子层数很高时，每多一层，传染范围增加多少格？")
                .defineInRange("contagion_radius_per_stack", 1.0, 0.0, 5.0);

        CONTAGION_INTENSITY_RATIO = BUILDER
                .comment("Transfer Ratio",
                        "传染时，会将宿主身上多少比例的孢子层数复制给受害者？(0.2 = 20%)")
                .defineInRange("contagion_intensity_ratio", 0.2, 0.0, 1.0);

        CONTAGION_WETNESS_THRESHOLD = BUILDER
                .comment("Wetness Conversion Threshold",
                        "对方身上至少有多少层潮湿时，才会开始促进孢子繁殖？")
                .defineInRange("contagion_wetness_threshold", 1, 0, 100);

        CONTAGION_CONSUMES_WETNESS = BUILDER
                .comment("Consume Wetness",
                        "传染发生时，是否消耗对方身上的潮湿状态来生成更多孢子？")
                .define("contagion_consumes_wetness", true);

        CONTAGION_WETNESS_CONVERSION_RATIO = BUILDER
                .comment("Wetness to Spore Ratio",
                        "多余的每一层潮湿可以转化为多少层额外的孢子？")
                .defineInRange("contagion_wetness_conversion_ratio", 1.0, 0.0, 10.0);

        CONTAGION_WETNESS_MAX_BONUS = BUILDER
                .comment("Max Conversion Bonus",
                        "通过潮湿转化最多能额外增加多少层孢子？(防止瞬间秒杀)")
                .defineInRange("contagion_wetness_max_bonus", 5, 0, 50);

        BUILDER.pop();

        // Nature Reaction
        BUILDER.push("nature_reaction");
        
        // Dynamic Parasitism
        BUILDER.push("dynamic_parasitism");
        NATURE_PARASITE_BASE_THRESHOLD = BUILDER
                .comment("Parasitism Threshold",
                        "自然属性强化至少要达到多少，攻击时才能触发寄生（挂孢子）？")
                .defineInRange("base_threshold", 5.0, 0.0, 1000.0);
        
        NATURE_PARASITE_BASE_CHANCE = BUILDER
                .comment("Base Parasite Chance",
                        "触发寄生的基础概率是多少？(0.05 = 5%)")
                .defineInRange("base_chance", 0.05, 0.0, 1.0);

        NATURE_PARASITE_SCALING_STEP = BUILDER
                .comment("Chance Scaling Step",
                        "每增加多少点自然属性，寄生概率会提升一级？")
                .defineInRange("scaling_step", 20.0, 1.0, 1000.0);

        NATURE_PARASITE_SCALING_CHANCE = BUILDER
                .comment("Chance Increase Per Step",
                        "每提升一级，寄生概率增加多少？(0.05 = 5%)")
                .defineInRange("scaling_chance", 0.05, 0.0, 1.0);

        NATURE_PARASITE_WETNESS_BONUS = BUILDER
                .comment("Wetness Chance Bonus",
                        "如果你身上有潮湿效果，每一层潮湿让寄生概率额外增加多少？")
                .defineInRange("wetness_bonus", 0.05, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("parasitic_drain");
        NATURE_SIPHON_THRESHOLD = BUILDER
                .comment("Siphon Threshold",
                        "自然属性强化达到多少时，攻击可以触发“寄生吸取”（吸干潮湿并回血）？")
                .defineInRange("nature_drain_threshold", 20, 1, 1000);

        NATURE_DRAIN_POWER_STEP = BUILDER
                .comment("Power Per Drain Level",
                        "每拥有多少点自然属性，可以多吸取1层潮湿？")
                .defineInRange("nature_drain_power_step", 20.0, 1.0, 1000.0);

        NATURE_DRAIN_AMOUNT = BUILDER
                .comment("Base Drain Amount",
                        "每次攻击基础吸取多少层潮湿？")
                .defineInRange("nature_drain_amount_per_step", 1, 1, 10);

        NATURE_SIPHON_HEAL = BUILDER
                .comment("Heal Per Drain",
                        "每吸取1层潮湿，攻击者恢复多少点血量？(1.0 = 半颗心)")
                .defineInRange("nature_heal_amount", 1.0, 0.0, 100.0);

        NATURE_DRAIN_COOLDOWN = BUILDER
                .comment("Siphon Cooldown",
                        "寄生吸取触发后的冷却时间是多少Tick？")
                .defineInRange("nature_drain_cooldown", 200, 0, 6000);
        BUILDER.pop();

        BUILDER.push("wildfire_ejection");
        WILDFIRE_TRIGGER_THRESHOLD = BUILDER
                .comment("Wildfire Threshold",
                        "自然属性强化达到多少时，受到火伤会触发“野火喷射”反击？")
                .defineInRange("wildfire_trigger_threshold", 20.0, 0.0, 1000.0);

        WILDFIRE_COOLDOWN = BUILDER
                .comment("Wildfire Cooldown",
                        "野火喷射反击的冷却时间是多少Tick？")
                .defineInRange("wildfire_cooldown", 200, 0, 6000);

        WILDFIRE_RADIUS = BUILDER
                .comment("Wildfire Radius",
                        "反击爆炸的范围半径是多少格？")
                .defineInRange("wildfire_radius", 3.0, 1.0, 16.0);

        WILDFIRE_KNOCKBACK = BUILDER
                .comment("Knockback Strength",
                        "反击将敌人击退的力度有多大？")
                .defineInRange("wildfire_knockback", 1.5, 0.0, 10.0);

        WILDFIRE_SPORE_AMOUNT = BUILDER
                .comment("Counter Spore Amount",
                        "反击时强制给周围敌人施加多少层孢子？")
                .defineInRange("wildfire_spore_amount", 2, 0, 10);
        BUILDER.pop();
        BUILDER.pop();

        // Fire Reaction
        BUILDER.push("fire_reaction");
        BUILDER.push("toxic_blast");
        BLAST_TRIGGER_THRESHOLD = BUILDER
                .comment("Blast Threshold",
                        "赤焰属性强化需要达到多少，才能引爆孢子（毒火爆燃）？")
                .defineInRange("blast_trigger_threshold", 50.0, 0.0, 1000.0);

        BLAST_WEAK_IGNITE_MULT = BUILDER
                .comment("Weak Ignite Multiplier",
                        "如果孢子层数不足以爆炸（<3层），引燃伤害会是平时的几倍？")
                .defineInRange("blast_weak_ignite_mult", 1.5, 1.0, 10.0);

        BLAST_BASE_DAMAGE = BUILDER
                .comment("Base Explosion Damage",
                        "引爆3层孢子时的基础爆炸伤害是多少？")
                .defineInRange("blast_base_damage", 5.0, 0.0, 100.0);

        BLAST_DMG_STEP = BUILDER
                .comment("Damage Scaling Step",
                        "每增加多少点赤焰属性，爆炸伤害会提升一级？")
                .defineInRange("blast_dmg_step", 20.0, 1.0, 1000.0);

        BLAST_DMG_AMOUNT = BUILDER
                .comment("Damage Per Step",
                        "每提升一级，爆炸伤害增加多少点？")
                .defineInRange("blast_dmg_amount", 1.0, 0.0, 100.0);

        BLAST_GROWTH_DAMAGE = BUILDER
                .comment("Bonus Damage Per Extra Stack",
                        "如果孢子层数超过3层，每多1层增加多少爆炸伤害？")
                .defineInRange("blast_growth_damage", 1.0, 0.0, 10.0);

        BLAST_BASE_RANGE = BUILDER
                .comment("Base Explosion Range",
                        "基础爆炸范围半径是多少格？")
                .defineInRange("blast_base_range", 1.5, 0.5, 10.0);

        BLAST_GROWTH_RANGE = BUILDER
                .comment("Range Increase Per Stack",
                        "孢子层数超过3层后，每多1层爆炸范围增加多少格？")
                .defineInRange("blast_growth_range", 1.0, 0.0, 5.0);
        
        BLAST_SCORCH_BASE = BUILDER
                .comment("Weak Scorch Duration",
                        "弱效引燃（<3层）造成的灼烧持续多少秒？")
                .defineInRange("blast_scorch_base", 3.0, 0.0, 60.0);

        BLAST_BASE_SCORCH_TIME = BUILDER
                .comment("Explosion Scorch Duration",
                        "成功爆炸（3层）后，造成的灼烧持续多少秒？")
                .defineInRange("blast_base_scorch_time", 3.0, 0.0, 60.0);

        BLAST_GROWTH_SCORCH_TIME = BUILDER
                .comment("Scorch Time Bonus Per Stack",
                        "孢子层数超过3层后，每多1层灼烧时间增加多少秒？")
                .defineInRange("blast_growth_scorch_time", 1.0, 0.0, 10.0);

        // Defense Caps
        BLAST_MAX_BLAST_PROT_CAP = BUILDER
                .comment("Max Blast Protection Cap",
                        "“爆炸保护”附魔最多能抵消百分之多少的爆燃伤害？(0.5 = 50%)")
                .defineInRange("blast_max_blast_prot_cap", 0.5, 0.0, 1.0);

        BLAST_MAX_GENERAL_PROT_CAP = BUILDER
                .comment("Max General Protection Cap",
                        "普通“保护”附魔最多能抵消百分之多少的爆燃伤害？(0.25 = 25%)")
                .defineInRange("blast_max_general_prot_cap", 0.25, 0.0, 1.0);

        BUILDER.pop();
        BUILDER.pop();

        // Steam Reaction
        BUILDER.push("steam_reaction");

        // Global & Limits
        STEAM_REACTION_ENABLED = BUILDER
                .comment("Enable Steam Reaction",
                        "是否开启蒸汽反应机制（水火相遇产生蒸汽）？")
                .define("steam_reaction_enabled", true);

        STEAM_HIGH_HEAT_MAX_LEVEL = BUILDER
                .comment("Max High-Heat Level",
                        "高温蒸汽云（火攻水）最高能达到多少级？")
                .defineInRange("steam_high_heat_max_level", 5, 1, 100);

        STEAM_LOW_HEAT_MAX_LEVEL = BUILDER
                .comment("Max Low-Heat Level",
                        "低温蒸汽云（冰攻火）最高能达到多少级？")
                .defineInRange("steam_low_heat_max_level", 5, 1, 100);

        STEAM_MAX_REDUCTION = BUILDER
                .comment("Global Damage Cap",
                        "潮湿导致的伤害修正（增伤或减伤）最高不能超过百分之多少？(0.9 = 90%)")
                .defineInRange("steam_max_reduction", 0.9, 0.0, 1.0);

        // Cloud Properties
        BUILDER.push("cloud_properties");
        STEAM_CLOUD_RADIUS = BUILDER
                .comment("Base Cloud Radius",
                        "蒸汽云的基础半径是多少格？")
                .defineInRange("steam_cloud_radius", 2.0, 0.5, 10.0);

        STEAM_RADIUS_PER_LEVEL = BUILDER
                .comment("Radius Increase Per Level",
                        "每增加一级（潮湿层数），蒸汽云半径增加多少格？")
                .defineInRange("steam_radius_per_level", 0.5, 0.0, 5.0);

        STEAM_CLOUD_DURATION = BUILDER
                .comment("Base Duration (Ticks)",
                        "蒸汽云的基础存在时间是多少Tick？")
                .defineInRange("steam_cloud_duration", 100, 20, 1200);

        STEAM_DURATION_PER_LEVEL = BUILDER
                .comment("Duration Increase Per Level",
                        "每增加一级，蒸汽云存在时间增加多少Tick？")
                .defineInRange("steam_duration_per_level", 20, 0, 200);

        STEAM_BLINDNESS_DURATION = BUILDER
                .comment("Blindness Duration",
                        "走进蒸汽云时，失明效果持续多少Tick？")
                .defineInRange("steam_blindness_duration", 60, 20, 600);

        STEAM_CLEAR_AGGRO = BUILDER
                .comment("Clear Aggro",
                        "蒸汽云是否会遮蔽视线，强制清除里面怪物的仇恨目标？")
                .define("steam_clear_aggro", true);

        STEAM_CLOUD_HEIGHT_CEILING = BUILDER
                .comment("Cloud Effect Height",
                        "蒸汽云向上飘散的有效高度是多少格？（超过这个高度不烫伤）")
                .defineInRange("steam_cloud_height_ceiling", 3.0, 0.0, 16.0);
        BUILDER.pop();

        // Condensation Logic
        BUILDER.push("condensation_logic");
        STEAM_CONDENSATION_STEP_FROST= BUILDER
                .comment("Frost Power Step (Low-Heat)",
                        "产生低温蒸汽时，每多少点冰霜属性可以提升一级蒸汽等级？")
                .defineInRange("steam_condensation_step_frost", 20, 1, 1000);

        STEAM_CONDENSATION_STEP_FIRE = BUILDER
                .comment("Fire Power Step (High-Heat)",
                        "产生高温蒸汽时，每多少点赤焰属性可以提升一级蒸汽等级？")
                .defineInRange("steam_condensation_step_fire", 20, 1, 1000);

        STEAM_CONDENSATION_DELAY = BUILDER
                .comment("Wetness Gain Delay",
                        "在低温蒸汽里待多久（Tick）才会身上变湿（获得1层潮湿）？")
                .defineInRange("steam_condensation_delay", 100, 1, 2400);

        STEAM_CONDENSATION_DURATION_BASE = BUILDER
                .comment("Low-Heat Base Duration",
                        "低温蒸汽云的基础持续时间是多少Tick？")
                .defineInRange("steam_condensation_duration_base", 100, 20, 1200);

        STEAM_CONDENSATION_DURATION_PER_LEVEL = BUILDER
                .comment("Low-Heat Duration Increase",
                        "低温蒸汽每升一级，持续时间增加多少Tick？")
                .defineInRange("steam_condensation_duration_per_level", 20, 0, 200);

        STEAM_SPORE_GROWTH_RATE = BUILDER
                .comment("Spore Growth Rate",
                        "在低温蒸汽中，孢子每隔多少Tick会繁殖一次？")
                .defineInRange("steam_spore_growth_rate", 20, 1, 600);
        BUILDER.pop();

        // Scalding Damage
        BUILDER.push("scalding_damage");
        STEAM_SCALDING_DAMAGE = BUILDER
                .comment("Base Scalding Damage",
                        "高温蒸汽每秒造成多少点基础烫伤伤害？")
                .defineInRange("steam_scalding_damage", 1.0, 0.0, 100.0);

        STEAM_DAMAGE_SCALE_PER_LEVEL = BUILDER
                .comment("Damage Increase Per Level",
                        "蒸汽等级每升一级，烫伤伤害增加百分之多少？(0.2 = +20%)")
                .defineInRange("steam_damage_scale_per_level", 0.2, 0.0, 10.0);

        STEAM_SCALDING_MULTIPLIER_WEAKNESS = BUILDER
                .comment("Weakness Multiplier",
                        "冰霜/自然属性的生物在高温蒸汽里受到的伤害是平时的几倍？")
                .defineInRange("steam_scalding_multiplier_weakness", 1.5, 1.0, 10.0);

        STEAM_SCALDING_MULTIPLIER_SPORE = BUILDER
                .comment("Spore Host Multiplier",
                        "如果生物身上有孢子，受到的蒸汽烫伤伤害是平时的几倍？")
                .defineInRange("steam_scalding_multiplier_spore", 1.5, 1.0, 10.0);

        STEAM_IMMUNITY_THRESHOLD = BUILDER
                .comment("Scalding Immunity Threshold",
                        "赤焰抗性达到多少点时，可以完全免疫蒸汽烫伤？")
                .defineInRange("steam_immunity_threshold", 80, 0, 1000);

        STEAM_IMMUNITY_BLACKLIST = BUILDER
                .comment("Steam Immunity Blacklist",
                        "蒸汽烫伤免疫黑名单（填入实体ID）。")
                .defineListAllowEmpty("steam_immunity_blacklist", List.of(), o -> o instanceof String);
        BUILDER.pop();

        // Trigger Logic
        BUILDER.push("trigger_logic");
        STEAM_TRIGGER_THRESHOLD_FIRE = BUILDER
                .comment("Fire Trigger Threshold",
                        "攻击时赤焰属性强化至少多少点，才能把水/冰蒸发成高温蒸汽？")
                .defineInRange("fire_trigger_threshold", 20, 0, 1000);

        STEAM_TRIGGER_THRESHOLD_FROST = BUILDER
                .comment("Frost Trigger Threshold",
                        "攻击时冰霜属性强化至少多少点，才能把火冷却成低温蒸汽？")
                .defineInRange("frost_trigger_threshold", 20, 0, 1000);
        BUILDER.pop();

        // Defense System
        BUILDER.push("defense_system");
        STEAM_DAMAGE_FLOOR_RATIO = BUILDER
                .comment("Minimum Damage Floor",
                        "对于被克制的生物（冰/自然），无论抗性多高，蒸汽伤害至少保留原伤害的百分之多少？(0.5 = 50%)")
                .defineInRange("damage_floor_ratio", 0.5, 0.0, 1.0);

        STEAM_MAX_FIRE_PROT_CAP = BUILDER
                .comment("Max Fire Protection Cap",
                        "火焰保护附魔最多能抵消百分之多少的蒸汽伤害？(0.5 = 50%)")
                .defineInRange("max_fire_prot_cap", 0.5, 0.0, 1.0);

        STEAM_MAX_GENERAL_PROT_CAP = BUILDER
                .comment("Max General Protection Cap",
                        "普通保护附魔最多能抵消百分之多少的蒸汽伤害？(0.25 = 25%)")
                .defineInRange("max_general_prot_cap", 0.25, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.pop();

        // Scorched Mechanic
        BUILDER.comment("Scorched Mechanic Configuration", "灼烧机制配置 - 一种无法轻易熄灭的强力燃烧")
                .push("scorched_mechanic");

        SCORCHED_TRIGGER_THRESHOLD = BUILDER
                .comment("Trigger Threshold",
                        "赤焰属性强化至少多少点，攻击时才能触发“灼烧”效果？")
                .defineInRange("scorched_trigger_threshold", 20, 1, 1000);

        SCORCHED_BASE_CHANCE = BUILDER
                .comment("Base Trigger Chance",
                        "触发灼烧的基础概率是多少？(0.2 = 20%)")
                .defineInRange("scorched_base_chance", 0.2, 0.0, 1.0);

        SCORCHED_CHANCE_PER_POINT = BUILDER
                .comment("Chance Bonus Per Point",
                        "每多1点赤焰属性，触发概率增加多少？(0.001 = 0.1%)")
                .defineInRange("scorched_chance_per_point", 0.001, 0.0, 0.1);

        SCORCHED_DURATION = BUILDER
                .comment("Duration (Ticks)",
                        "灼烧状态持续多少Tick？(100 Tick = 5秒)")
                .defineInRange("scorched_duration", 100, 20, 1200);

        SCORCHED_COOLDOWN = BUILDER
                .comment("Cooldown (Ticks)",
                        "灼烧效果触发后，需要冷却多久才能再次触发？")
                .defineInRange("scorched_cooldown", 200, 0, 6000);

        SCORCHED_BURNING_LOCK_DURATION = BUILDER
                .comment("Visual Lock Duration",
                        "灼烧生效时，强制显示火焰特效多少秒？(防止被水瞬间浇灭视觉效果)")
                .defineInRange("scorched_burning_lock_duration", 3.0, 1.0, 60.0);

        SCORCHED_DAMAGE_BASE = BUILDER
                .comment("Base Damage",
                        "灼烧每秒造成多少点基础伤害？")
                .defineInRange("scorched_damage_base", 1.0, 0.1, 100.0);

        SCORCHED_DAMAGE_SCALING_STEP = BUILDER
                .comment("Damage Scaling Step",
                        "每多少点赤焰属性，灼烧伤害增加0.5点？")
                .defineInRange("scorched_damage_scaling_step", 20, 1, 1000);

        SCORCHED_RESIST_THRESHOLD = BUILDER
                .comment("Immunity Threshold",
                        "赤焰抗性达到多少点时，可以完全免疫灼烧伤害？")
                .defineInRange("scorched_resist_threshold", 80, 1, 1000);

        SCORCHED_IMMUNE_MODIFIER = BUILDER
                .comment("Immune Mob Damage Multiplier",
                        "天生免疫火的生物（如下界生物）受到灼烧时，伤害是正常的几倍？(0.5 = 50%)")
                .defineInRange("scorched_immune_modifier", 0.5, 0.0, 1.0);

        SCORCHED_FIRE_PROT_REDUCTION = BUILDER
                .comment("Fire Protection Reduction Cap",
                        "火焰保护附魔最多能抵消百分之多少的灼烧伤害？")
                .defineInRange("scorched_fire_prot_reduction", 0.5, 0.0, 1.0);

        SCORCHED_GEN_PROT_REDUCTION = BUILDER
                .comment("General Protection Reduction Cap",
                        "普通保护附魔最多能抵消百分之多少的灼烧伤害？")
                .defineInRange("scorched_gen_prot_reduction", 0.25, 0.0, 1.0);

        SCORCHED_NATURE_MULTIPLIER = BUILDER
                .comment("Nature Weakness Multiplier",
                        "自然属性生物受到的灼烧伤害是平时的几倍？")
                .defineInRange("scorched_nature_multiplier", 1.5, 1.0, 10.0);

        SCORCHED_ENTITY_BLACKLIST = BUILDER
                .comment("Scorched Blacklist",
                        "灼烧效果免疫黑名单。")
                .defineListAllowEmpty("scorched_entity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // -------------------------------------------------------------------------
    // 静态缓存变量 (Static Cache Variables)
    // 用于在游戏运行时快速访问配置值，避免频繁调用 ForgeConfigSpec 的 get() 方法。
    // Used for fast access to config values during runtime to avoid frequent calls to ForgeConfigSpec's get() method.
    // -------------------------------------------------------------------------
    
    // Wetness
    public static int wetnessMaxLevel;
    public static double wetnessShallowWaterCapRatio;
    public static double wetnessFireReduction;
    public static int wetnessRainGainInterval;
    public static int wetnessDecayBaseTime;
    public static double wetnessExhaustionIncrease;
    public static int wetnessPotionAddLevel;
    public static int wetnessDryingThreshold;
    public static double wetnessSelfDryingDamagePenalty;
    public static int wetnessFireDryingTime;
    public static boolean wetnessWaterAnimalImmune;
    public static boolean wetnessNetherDimensionImmune;
    public static List<? extends String> cachedWetnessBlacklist;

    // Spore System
    public static int sporeMaxStacks;
    public static double sporePoisonDamage;
    public static double sporeSpeedReduction;
    public static double sporePhysResist;
    public static double sporeFireVulnPerStack;
    public static int sporeDurationPerStack;
    public static double sporeThunderMultiplier;
    // Removed SPORE_FIRE_REDUCTION
    public static double sporeFireDurationReduction;

    // Contagion System
    public static int contagionCheckInterval;
    public static double contagionBaseRadius;
    public static double contagionRadiusPerStack;
    public static double contagionIntensityRatio;

    // New Contagion Configs
    public static int contagionWetnessThreshold;
    public static boolean contagionConsumesWetness;
    public static double contagionWetnessConversionRatio;
    public static int contagionWetnessMaxBonus;


    // Nature Reaction
    
    public static double natureParasiteBaseThreshold;
    public static double natureParasiteBaseChance;
    public static double natureParasiteScalingStep;
    public static double natureParasiteScalingChance;
    public static double natureParasiteWetnessBonus;
    
    public static int natureSiphonThreshold;
    public static double natureDrainPowerStep;
    public static int natureDrainAmount;
    public static double natureSiphonHeal;
    public static int natureDrainCooldown;
    
    public static double wildfireTriggerThreshold;
    public static int wildfireCooldown;
    public static double wildfireRadius;
    public static double wildfireKnockback;
    public static int wildfireSporeAmount;

    // Toxic Blast
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

    // Blast Defense Caps
    public static double blastMaxBlastProtCap;
    public static double blastMaxGeneralProtCap;

    // Steam Reaction
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
    public static double scorchedGenProtReduction;
    public static double scorchedNatureMultiplier;
    public static List<? extends String> cachedScorchedBlacklist;

    /**
     * 注册配置文件。
     * <p>
     * Registers the configuration file.
     */
    @SuppressWarnings("deprecation") // ModLoadingContext.get() is standard in 1.20.1
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
        // Wetness System
        wetnessMaxLevel = WETNESS_MAX_LEVEL.get();
        wetnessShallowWaterCapRatio = WETNESS_SHALLOW_WATER_CAP_RATIO.get();
        wetnessFireReduction = WETNESS_FIRE_REDUCTION.get();
        wetnessRainGainInterval = WETNESS_RAIN_GAIN_INTERVAL.get();
        wetnessDecayBaseTime = WETNESS_DECAY_BASE_TIME.get();
        wetnessExhaustionIncrease = WETNESS_EXHAUSTION_INCREASE.get();
        wetnessPotionAddLevel = WETNESS_POTION_ADD_LEVEL.get();
        wetnessDryingThreshold = WETNESS_DRYING_THRESHOLD.get();
        wetnessSelfDryingDamagePenalty = WETNESS_SELF_DRYING_DAMAGE_PENALTY.get();
        wetnessFireDryingTime = WETNESS_FIRE_DRYING_TIME.get();
        wetnessWaterAnimalImmune = WETNESS_WATER_ANIMAL_IMMUNE.get();
        wetnessNetherDimensionImmune = WETNESS_NETHER_DIMENSION_IMMUNE.get();
        cachedWetnessBlacklist = WETNESS_ENTITY_BLACKLIST.get();

        // Spore System
        sporeMaxStacks = SPORE_MAX_STACKS.get();
        sporePoisonDamage = SPORE_POISON_DAMAGE.get();
        sporeSpeedReduction = SPORE_SPEED_REDUCTION.get();
        sporePhysResist = SPORE_PHYS_RESIST.get();
        sporeFireVulnPerStack = SPORE_FIRE_VULN_PER_STACK.get();
        sporeDurationPerStack = SPORE_DURATION_PER_STACK.get();
        sporeThunderMultiplier = SPORE_THUNDER_MULTIPLIER.get();
        // Removed sporeFireReduction refresh
        sporeFireDurationReduction = SPORE_FIRE_DURATION_REDUCTION.get();

        // Contagion System
        contagionCheckInterval = CONTAGION_CHECK_INTERVAL.get();
        contagionBaseRadius = CONTAGION_BASE_RADIUS.get();
        contagionRadiusPerStack = CONTAGION_RADIUS_PER_STACK.get();
        contagionIntensityRatio = CONTAGION_INTENSITY_RATIO.get();

        contagionWetnessThreshold = CONTAGION_WETNESS_THRESHOLD.get();
        contagionConsumesWetness = CONTAGION_CONSUMES_WETNESS.get();
        contagionWetnessConversionRatio = CONTAGION_WETNESS_CONVERSION_RATIO.get();
        contagionWetnessMaxBonus = CONTAGION_WETNESS_MAX_BONUS.get();

        // Nature Reaction
        natureParasiteBaseThreshold = NATURE_PARASITE_BASE_THRESHOLD.get();
        natureParasiteBaseChance = NATURE_PARASITE_BASE_CHANCE.get();
        natureParasiteScalingStep = NATURE_PARASITE_SCALING_STEP.get();
        natureParasiteScalingChance = NATURE_PARASITE_SCALING_CHANCE.get();
        natureParasiteWetnessBonus = NATURE_PARASITE_WETNESS_BONUS.get();

        natureSiphonThreshold = NATURE_SIPHON_THRESHOLD.get();
        natureDrainPowerStep = NATURE_DRAIN_POWER_STEP.get();
        natureDrainAmount = NATURE_DRAIN_AMOUNT.get();
        natureSiphonHeal = NATURE_SIPHON_HEAL.get();
        natureDrainCooldown = NATURE_DRAIN_COOLDOWN.get();
        wildfireTriggerThreshold = WILDFIRE_TRIGGER_THRESHOLD.get();
        wildfireCooldown = WILDFIRE_COOLDOWN.get();
        wildfireRadius = WILDFIRE_RADIUS.get();
        wildfireKnockback = WILDFIRE_KNOCKBACK.get();
        wildfireSporeAmount = WILDFIRE_SPORE_AMOUNT.get();

        // Toxic Blast
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
        blastMaxBlastProtCap = BLAST_MAX_BLAST_PROT_CAP.get();
        blastMaxGeneralProtCap = BLAST_MAX_GENERAL_PROT_CAP.get();

        // Steam Reaction - Global
        steamReactionEnabled = STEAM_REACTION_ENABLED.get();
        steamHighHeatMaxLevel = STEAM_HIGH_HEAT_MAX_LEVEL.get();
        steamLowHeatMaxLevel = STEAM_LOW_HEAT_MAX_LEVEL.get();
        steamMaxReduction = STEAM_MAX_REDUCTION.get();

        // Steam Reaction - Cloud
        steamCloudRadius = STEAM_CLOUD_RADIUS.get();
        steamRadiusPerLevel = STEAM_RADIUS_PER_LEVEL.get();
        steamCloudDuration = STEAM_CLOUD_DURATION.get();
        steamDurationPerLevel = STEAM_DURATION_PER_LEVEL.get();
        steamBlindnessDuration = STEAM_BLINDNESS_DURATION.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        steamCloudHeightCeiling = STEAM_CLOUD_HEIGHT_CEILING.get();

        // Steam Reaction - Condensation
        steamCondensationStepFire = STEAM_CONDENSATION_STEP_FIRE.get();
        steamCondensationStepFrost = STEAM_CONDENSATION_STEP_FROST.get();
        steamCondensationDelay = STEAM_CONDENSATION_DELAY.get();
        steamCondensationDurationBase = STEAM_CONDENSATION_DURATION_BASE.get();
        steamCondensationDurationPerLevel = STEAM_CONDENSATION_DURATION_PER_LEVEL.get();
        steamSporeGrowthRate = STEAM_SPORE_GROWTH_RATE.get();

        // Steam Reaction - Scalding
        steamScaldingDamage = STEAM_SCALDING_DAMAGE.get();
        steamDamageScalePerLevel = STEAM_DAMAGE_SCALE_PER_LEVEL.get();
        steamScaldingMultiplierWeakness = STEAM_SCALDING_MULTIPLIER_WEAKNESS.get();
        steamScaldingMultiplierSpore = STEAM_SCALDING_MULTIPLIER_SPORE.get();
        steamImmunityThreshold = STEAM_IMMUNITY_THRESHOLD.get();
        cachedSteamBlacklist = STEAM_IMMUNITY_BLACKLIST.get();

        // Steam Reaction - Trigger & Defense
        steamTriggerThresholdFire = STEAM_TRIGGER_THRESHOLD_FIRE.get();
        steamTriggerThresholdFrost = STEAM_TRIGGER_THRESHOLD_FROST.get();
        steamDamageFloorRatio = STEAM_DAMAGE_FLOOR_RATIO.get();
        steamMaxFireProtCap = STEAM_MAX_FIRE_PROT_CAP.get();
        steamMaxGeneralProtCap = STEAM_MAX_GENERAL_PROT_CAP.get();

        // Scorched Mechanic
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
        scorchedGenProtReduction = SCORCHED_GEN_PROT_REDUCTION.get();
        scorchedNatureMultiplier = SCORCHED_NATURE_MULTIPLIER.get();
        cachedScorchedBlacklist = SCORCHED_ENTITY_BLACKLIST.get();
    }
}