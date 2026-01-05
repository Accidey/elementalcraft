// src/main/java/com/xulai/elementalcraft/config/ElementalReactionConfig.java
package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

/**
 * ElementalReactionConfig (V1.5.2 - Removed Nature Damage Bonus)
 * <p>
 * 中文说明：
 * 元素反应系统的专用配置文件类。
 * 负责定义和管理所有与元素反应（如潮湿、蒸汽、自然吸取、灼烧、易燃孢子等）相关的可配置参数。
 * 包含 ForgeConfigSpec 的构建以及运行时缓存的刷新逻辑。
 * <p>
 * V1.5.2 更新：移除了自然属性对潮湿目标的伤害加成配置 (natureWetnessBonus)。
 */
public class ElementalReactionConfig {
    public static final ForgeConfigSpec SPEC;

    // ======================== Wetness System / 潮湿系统 ========================
    public static final ForgeConfigSpec.IntValue WETNESS_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue WETNESS_SHALLOW_WATER_CAP_RATIO;
    public static final ForgeConfigSpec.DoubleValue WETNESS_RESIST_MODIFIER;
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

    // ======================== Spore System / 孢子系统 (V1.5 Core) ========================
    public static final ForgeConfigSpec.IntValue SPORE_MAX_STACKS;
    public static final ForgeConfigSpec.DoubleValue SPORE_POISON_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SPORE_SPEED_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SPORE_PHYS_RESIST;
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_VULN_PER_STACK;
    public static final ForgeConfigSpec.IntValue SPORE_DURATION_PER_STACK;
    
    // V1.5.1 New Configs
    public static final ForgeConfigSpec.DoubleValue SPORE_THUNDER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SPORE_FIRE_DURATION_REDUCTION;

    // ======================== Contagion System / 传染系统 ========================
    public static final ForgeConfigSpec.IntValue CONTAGION_CHECK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_BASE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_RADIUS_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue CONTAGION_INTENSITY_RATIO;

    // ======================== Nature Reaction / 自然反应 ========================
    
    // --- Dynamic Parasitism / 动态寄生 ---
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_BASE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_SCALING_STEP;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_SCALING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue NATURE_PARASITE_WETNESS_BONUS;

    // --- Parasitic Drain / 寄生吸取 ---
    public static final ForgeConfigSpec.IntValue NATURE_SIPHON_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NATURE_DRAIN_POWER_STEP;
    public static final ForgeConfigSpec.IntValue NATURE_DRAIN_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue NATURE_SIPHON_HEAL;
    public static final ForgeConfigSpec.IntValue NATURE_DRAIN_COOLDOWN;

    // --- Wildfire Ejection / 野火喷射  ---
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.IntValue WILDFIRE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue WILDFIRE_SPORE_AMOUNT;

    // ======================== Fire Reaction / 赤焰反应 ========================
    // --- Toxic Blast / 毒火爆燃 ---
    public static final ForgeConfigSpec.DoubleValue BLAST_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue BLAST_WEAK_IGNITE_MULT;
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue BLAST_DMG_STEP;
    public static final ForgeConfigSpec.DoubleValue BLAST_DMG_AMOUNT;
    
    // Dynamic Growth Parameters (V1.5)
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_DAMAGE;      // +Damage per stack
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_RANGE;         // Base Radius
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_RANGE;       // +Radius per stack
    public static final ForgeConfigSpec.DoubleValue BLAST_SCORCH_BASE;        // Base Scorch Time (For Weak Ignite)
    public static final ForgeConfigSpec.DoubleValue BLAST_BASE_SCORCH_TIME;   // Base Scorch Time (For Blast)
    public static final ForgeConfigSpec.DoubleValue BLAST_SCORCH_PER_STACK;   // Legacy/Growth Time
    public static final ForgeConfigSpec.DoubleValue BLAST_GROWTH_SCORCH_TIME; // +Time per stack (New)

    // ======================== Steam Reaction / 蒸汽反应 ========================
    
    // --- Global Switch & Limits / 全局开关与限制 ---
    public static final ForgeConfigSpec.BooleanValue STEAM_REACTION_ENABLED;
    public static final ForgeConfigSpec.IntValue STEAM_HIGH_HEAT_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_LOW_HEAT_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_REDUCTION;

    // --- Cloud Properties / 蒸汽云属性 ---
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_RADIUS;
    public static final ForgeConfigSpec.DoubleValue STEAM_RADIUS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_CLOUD_DURATION;
    public static final ForgeConfigSpec.IntValue STEAM_DURATION_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_BLINDNESS_DURATION;
    public static final ForgeConfigSpec.BooleanValue STEAM_CLEAR_AGGRO;
    
    // Steam Cloud Height Ceiling / 蒸汽云高度限制
    public static final ForgeConfigSpec.DoubleValue STEAM_CLOUD_HEIGHT_CEILING;

    // --- Condensation Logic / 冷凝逻辑 (低温) ---
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_STEP_FIRE;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_STEP_FROST;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DELAY;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DURATION_BASE;
    public static final ForgeConfigSpec.IntValue STEAM_CONDENSATION_DURATION_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue STEAM_SPORE_GROWTH_RATE;

    // --- Scalding Damage / 烫伤伤害 ---
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_SCALE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_WEAKNESS;
    public static final ForgeConfigSpec.DoubleValue STEAM_SCALDING_MULTIPLIER_SPORE;
    public static final ForgeConfigSpec.IntValue STEAM_IMMUNITY_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STEAM_IMMUNITY_BLACKLIST;

    // --- Trigger Logic / 触发逻辑 (双轨制) ---
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_THRESHOLD_FIRE;
    public static final ForgeConfigSpec.IntValue STEAM_TRIGGER_THRESHOLD_FROST;

    // --- Defense System / 防御体系 (分层防御) ---
    public static final ForgeConfigSpec.DoubleValue STEAM_DAMAGE_FLOOR_RATIO;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_FIRE_PROT_CAP;
    public static final ForgeConfigSpec.DoubleValue STEAM_MAX_GENERAL_PROT_CAP;

    // ======================== Scorched Mechanic / 灼烧机制 ========================
    public static final ForgeConfigSpec.IntValue SCORCHED_TRIGGER_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_BASE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_CHANCE_PER_POINT;
    public static final ForgeConfigSpec.IntValue SCORCHED_DURATION;
    public static final ForgeConfigSpec.IntValue SCORCHED_COOLDOWN; // New
    public static final ForgeConfigSpec.DoubleValue SCORCHED_BURNING_LOCK_DURATION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_DAMAGE_BASE;
    public static final ForgeConfigSpec.IntValue SCORCHED_DAMAGE_SCALING_STEP;
    public static final ForgeConfigSpec.IntValue SCORCHED_RESIST_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_IMMUNE_MODIFIER; // New
    public static final ForgeConfigSpec.DoubleValue SCORCHED_FIRE_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_GEN_PROT_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SCORCHED_NATURE_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCORCHED_ENTITY_BLACKLIST; // New

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Elemental Reaction System Configuration", "元素反应系统配置")
                .push("wetness_system");

        WETNESS_MAX_LEVEL = BUILDER
                .comment("Maximum stack level of the Wetness effect.",
                        "潮湿效果的最大堆叠层数。")
                .defineInRange("wetness_max_level", 5, 1, 100);

        WETNESS_SHALLOW_WATER_CAP_RATIO = BUILDER
                .comment("Ratio of max wetness level allowed when in shallow water (not fully submerged).",
                        "当处于浅水（未完全淹没）时，获得的潮湿层数比例（0.6 = 60%）。",
                        "Formula: MaxLevel * Ratio")
                .defineInRange("wetness_shallow_water_cap_ratio", 0.6, 0.0, 1.0);

        WETNESS_RESIST_MODIFIER = BUILDER
                .comment("General resistance modifier provided per Wetness level.",
                        "雷霆和冰霜属性攻击潮湿目标时，每层潮湿提供的额外伤害倍率（0.1 = +10%）。")
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
                        "每层潮湿额外增加的饱食度消耗速度。")
                .defineInRange("wetness_exhaustion_increase", 0.05, 0.0, 10.0);

        WETNESS_POTION_ADD_LEVEL = BUILDER
                .comment("Number of Wetness levels added instantly by a Splash Water Bottle.",
                        "被喷溅水瓶击中时，增加的潮湿层数。")
                .defineInRange("wetness_potion_add_level", 1, 1, 100);

        WETNESS_DRYING_THRESHOLD = BUILDER
                .comment("Fire enhancement points required to remove ONE extra wetness level (Self-Drying).",
                        "攻击时移除 1 层自身潮湿效果所需的赤焰强化点数（自我干燥）。")
                .defineInRange("wetness_drying_threshold", 20, 1, 1000);

        WETNESS_SELF_DRYING_DAMAGE_PENALTY = BUILDER
                .comment("Damage penalty ratio when a wet Fire attacker dries themselves.",
                        "当赤焰属性攻击者处于潮湿状态时，触发自我干燥导致的伤害惩罚比例（0.3 = -30% 伤害）。")
                .defineInRange("wetness_self_drying_damage_penalty", 0.3, 0.0, 1.0);

        WETNESS_FIRE_DRYING_TIME = BUILDER
                .comment("Time (in seconds) to stand in fire to remove all wetness.",
                        "在火焰中停留多少秒后清除所有潮湿效果。")
                .defineInRange("wetness_fire_drying_time", 2, 1, 600);

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
        BUILDER.pop();

        // ======================== Spore System / 孢子系统 ========================
        BUILDER.push("spore_system");
        SPORE_MAX_STACKS = BUILDER
                .comment("Maximum stack level of the Flammable Spores effect (5 layers).",
                        "易燃孢子效果的最大堆叠层数（5层）。")
                .defineInRange("max_spore_stacks", 5, 1, 100);

        SPORE_POISON_DAMAGE = BUILDER
                .comment("Poison damage dealt per second per spore stack.",
                        "每层孢子每秒造成的中毒伤害（无视护甲）。")
                .defineInRange("spore_poison_damage", 0.5, 0.0, 20.0);

        SPORE_SPEED_REDUCTION = BUILDER
                .comment("Movement and attack speed reduction ratio per stack (0.1 = 10%).",
                        "每层孢子降低的移动速度和攻击速度比例（0.1 = 10%）。")
                .defineInRange("spore_speed_reduction", 0.1, 0.0, 0.5);

        SPORE_PHYS_RESIST = BUILDER
                .comment("Physical damage resistance provided per stack (0.05 = 5%).",
                        "每层孢子提供的物理伤害减免（0.05 = 5%）。")
                .defineInRange("spore_phys_resist", 0.05, 0.0, 0.5);

        SPORE_FIRE_VULN_PER_STACK = BUILDER
                .comment("Fire/Steam damage vulnerability increase per spore stack (0.1 = 10%).",
                        "每层孢子增加的火焰/蒸汽易伤比例（0.1 = 10%）。",
                        "Example: 5 stacks * 0.1 = 50% extra fire damage.")
                .defineInRange("spore_fire_vuln_per_stack", 0.1, 0.0, 1.0);

        SPORE_DURATION_PER_STACK = BUILDER
                .comment("Duration seconds per stack (Base 5s + 5s/stack).",
                        "每层孢子增加的持续时间（基础5秒 + 5秒/层）。")
                .defineInRange("spore_duration_per_stack", 5, 1, 60);
        
        // V1.5.1 New Configs
        SPORE_THUNDER_MULTIPLIER = BUILDER
                .comment("Effect multiplier for Thunder attribute hosts (Default 1.5).",
                         "雷霆属性宿主受到的孢子效果倍率（默认 1.5）。")
                .defineInRange("spore_thunder_multiplier", 1.5, 1.0, 5.0);
        
        SPORE_FIRE_REDUCTION = BUILDER
                .comment("Effect reduction ratio for Fire attribute hosts (Default 0.5).",
                         "赤焰属性宿主受到的孢子效果比例（默认 0.5 = 50%）。")
                .defineInRange("spore_fire_reduction", 0.5, 0.0, 1.0);
        
        SPORE_FIRE_DURATION_REDUCTION = BUILDER
                .comment("Duration reduction ratio for Fire attribute hosts (Default 0.5).",
                         "赤焰属性宿主受到的孢子持续时间比例（默认 0.5 = 50%）。")
                .defineInRange("spore_fire_duration_reduction", 0.5, 0.0, 1.0);
        BUILDER.pop();

        // ======================== Contagion System / 传染系统 ========================
        BUILDER.push("contagion_system");
        CONTAGION_CHECK_INTERVAL = BUILDER
                .comment("Tick interval for checking spore contagion spreading.",
                        "检测孢子扩散传播的时间间隔（Tick）。")
                .defineInRange("contagion_check_interval", 20, 1, 1200);

        CONTAGION_BASE_RADIUS = BUILDER
                .comment("Base radius for spore contagion.",
                        "孢子传染的基础半径范围。")
                .defineInRange("contagion_base_radius", 2.0, 1.0, 16.0);

        CONTAGION_RADIUS_PER_STACK = BUILDER
                .comment("Extra radius added per spore stack above threshold.",
                        "每层额外孢子增加的传染半径。")
                .defineInRange("contagion_radius_per_stack", 1.0, 0.0, 5.0);

        CONTAGION_INTENSITY_RATIO = BUILDER
                .comment("Ratio of spore stacks transferred to the victim (0.2 = 20%).",
                        "传染给受害者的孢子层数比例（向下取整）。")
                .defineInRange("contagion_intensity_ratio", 0.2, 0.0, 1.0);
        BUILDER.pop();

        // ======================== Nature Reaction / 自然反应 ========================
        BUILDER.push("nature_reaction");
        
        // --- Dynamic Parasitism / 动态寄生 ---
        BUILDER.push("dynamic_parasitism");
        NATURE_PARASITE_BASE_THRESHOLD = BUILDER
                .comment("Min nature power to trigger parasitism (Default: 5.0).",
                         "触发寄生的最低自然强化值（默认：5.0）。")
                .defineInRange("base_threshold", 5.0, 0.0, 1000.0);
        
        NATURE_PARASITE_BASE_CHANCE = BUILDER
                .comment("Base chance to apply spore (Default: 0.05 = 5%).",
                         "基础寄生概率（默认：0.05 = 5%）。")
                .defineInRange("base_chance", 0.05, 0.0, 1.0);

        NATURE_PARASITE_SCALING_STEP = BUILDER
                .comment("Nature power step for increased chance (Default: 20.0).",
                         "概率成长的自然属性步长（默认：20.0）。")
                .defineInRange("scaling_step", 20.0, 1.0, 1000.0);

        NATURE_PARASITE_SCALING_CHANCE = BUILDER
                .comment("Chance increase per step (Default: 0.05 = 5%).",
                         "每一步长增加的概率（默认：0.05 = 5%）。")
                .defineInRange("scaling_chance", 0.05, 0.0, 1.0);

        NATURE_PARASITE_WETNESS_BONUS = BUILDER
                .comment("Chance increase per wetness level (Default: 0.05 = 5%).",
                         "每层潮湿增加的概率（默认：0.05 = 5%）。")
                .defineInRange("wetness_bonus", 0.05, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("parasitic_drain");
        NATURE_SIPHON_THRESHOLD = BUILDER
                .comment("Nature enhancement points required to trigger Parasitic Drain.",
                        "触发寄生吸取所需的自然强化属性阈值。")
                .defineInRange("nature_drain_threshold", 20, 1, 1000);

        // [Added] Step Config
        NATURE_DRAIN_POWER_STEP = BUILDER
                .comment("Nature power required to drain 1 wetness level (Default: 20.0).",
                        "吸取1层潮湿所需的自然强化点数（默认：20.0）。")
                .defineInRange("nature_drain_power_step", 20.0, 1.0, 1000.0);

        NATURE_DRAIN_AMOUNT = BUILDER
                .comment("Number of wetness levels drained per trigger.",
                        "每次判定成功吸取的潮湿层数。")
                .defineInRange("nature_drain_amount_per_step", 1, 1, 10);

        NATURE_SIPHON_HEAL = BUILDER
                .comment("Health points restored per drained wetness level (1.0 = 0.5 hearts).",
                        "每吸取1层潮湿，攻击者恢复的血量（1.0 = 0.5颗心）。")
                .defineInRange("nature_heal_amount", 1.0, 0.0, 100.0);

        NATURE_DRAIN_COOLDOWN = BUILDER
                .comment("Cooldown ticks for Parasitic Drain (200 = 10s).",
                        "寄生吸取的内置冷却时间（Tick）。")
                .defineInRange("nature_drain_cooldown", 200, 0, 6000);
        BUILDER.pop();

        BUILDER.push("wildfire_ejection");
        WILDFIRE_TRIGGER_THRESHOLD = BUILDER
                .comment("Nature attribute threshold required to trigger Wildfire Ejection.",
                        "触发野火喷射所需的自然属性阈值。")
                .defineInRange("wildfire_trigger_threshold", 20.0, 0.0, 1000.0);

        WILDFIRE_COOLDOWN = BUILDER
                .comment("Cooldown ticks for Wildfire Ejection (200 = 10s).",
                        "野火喷射的冷却时间（Tick，200 = 10秒）。")
                .defineInRange("wildfire_cooldown", 200, 0, 6000);

        WILDFIRE_RADIUS = BUILDER
                .comment("Radius of the Wildfire Ejection blast (3.0 blocks).",
                        "野火喷射的反击爆发半径范围。")
                .defineInRange("wildfire_radius", 3.0, 1.0, 16.0);

        WILDFIRE_KNOCKBACK = BUILDER
                .comment("Knockback strength coefficient.",
                        "击退力度系数。")
                .defineInRange("wildfire_knockback", 1.5, 0.0, 10.0);

        WILDFIRE_SPORE_AMOUNT = BUILDER
                .comment("Number of Spore stacks applied to nearby enemies (2 stacks).",
                        "强制给周围敌人施加的孢子层数。")
                .defineInRange("wildfire_spore_amount", 2, 0, 10);
        BUILDER.pop();
        BUILDER.pop();

        // ======================== Fire Reaction / 赤焰反应 ========================
        BUILDER.push("fire_reaction");
        BUILDER.push("toxic_blast");
        BLAST_TRIGGER_THRESHOLD = BUILDER
                .comment("Flame attribute threshold required to trigger Toxic Blast (>50).",
                        "触发毒火爆燃所需的赤焰属性阈值（大于50）。")
                .defineInRange("blast_trigger_threshold", 50.0, 0.0, 1000.0);

        BLAST_WEAK_IGNITE_MULT = BUILDER
                .comment("Damage multiplier for weak ignite (<3 stacks) (1.5x damage).",
                        "弱效引燃（<3层）的灼烧伤害倍率（1.5倍）。")
                .defineInRange("blast_weak_ignite_mult", 1.5, 1.0, 10.0);

        BLAST_BASE_DAMAGE = BUILDER
                .comment("Base damage of the blast (at 3 stacks).",
                        "爆炸基础伤害（3层时）。")
                .defineInRange("blast_base_damage", 5.0, 0.0, 100.0);

        BLAST_DMG_STEP = BUILDER
                .comment("Attribute step to increase blast damage.",
                        "伤害成长的属性步长（每多少属性加一次伤）。")
                .defineInRange("blast_dmg_step", 20.0, 1.0, 1000.0);

        BLAST_DMG_AMOUNT = BUILDER
                .comment("Damage amount added per step.",
                        "每个步长增加的伤害数值。")
                .defineInRange("blast_dmg_amount", 1.0, 0.0, 100.0);

        // V1.5.0 Dynamic Growth Parameters
        BLAST_GROWTH_DAMAGE = BUILDER
                .comment("Damage increase per extra stack above 3.",
                        "超过3层后，每多1层增加的爆炸伤害。")
                .defineInRange("blast_growth_damage", 1.0, 0.0, 10.0);

        BLAST_BASE_RANGE = BUILDER
                .comment("Base radius of the blast (at 3 stacks).",
                        "爆炸基础半径（3层时）。")
                .defineInRange("blast_base_range", 1.5, 0.5, 10.0);

        BLAST_GROWTH_RANGE = BUILDER
                .comment("Radius increase per extra stack above 3.",
                        "超过3层后，每多1层增加的爆炸半径。")
                .defineInRange("blast_growth_range", 1.0, 0.0, 5.0);
        
        BLAST_SCORCH_BASE = BUILDER
                .comment("Base scorch duration for weak ignite (<3 stacks) (3.0s).",
                        "弱效引燃（<3层）的基础灼烧时长（3.0秒）。")
                .defineInRange("blast_scorch_base", 3.0, 0.0, 60.0);

        BLAST_BASE_SCORCH_TIME = BUILDER
                .comment("Base duration (seconds) of Scorch applied by blast (at 3 stacks).",
                        "爆燃造成的灼烧基础时长（秒，3层时）。")
                .defineInRange("blast_base_scorch_time", 3.0, 0.0, 60.0);

        BLAST_SCORCH_PER_STACK = BUILDER
                .comment("Legacy parameter - mapped to growth scorch time.",
                        "遗留参数 - 映射到时长成长。")
                .defineInRange("blast_scorch_per_stack", 0.5, 0.0, 10.0);

        BLAST_GROWTH_SCORCH_TIME = BUILDER
                .comment("Extra duration (seconds) added per extra stack above 3.",
                        "超过3层后，每多1层增加的灼烧时长（秒）。")
                .defineInRange("blast_growth_scorch_time", 1.0, 0.0, 10.0);
        BUILDER.pop();
        BUILDER.pop();

        // ======================== Steam Reaction / 蒸汽反应 ========================
        BUILDER.push("steam_reaction");

        // --- Global & Limits / 全局与限制 ---
        STEAM_REACTION_ENABLED = BUILDER
                .comment("Whether to enable the Steam Reaction mechanism.",
                        "是否开启蒸汽反应机制。")
                .define("steam_reaction_enabled", true);

        STEAM_HIGH_HEAT_MAX_LEVEL = BUILDER
                .comment("Maximum level cap for High-Heat Steam Clouds (Fire attack Frost/Wet).",
                        "高温蒸汽云的最大等级上限。")
                .defineInRange("steam_high_heat_max_level", 5, 1, 100);

        STEAM_LOW_HEAT_MAX_LEVEL = BUILDER
                .comment("Maximum level cap for Low-Heat Steam Clouds (Frost attack Fire).",
                        "低温蒸汽云的最大等级上限。")
                .defineInRange("steam_low_heat_max_level", 5, 1, 100);

        STEAM_MAX_REDUCTION = BUILDER
                .comment("Global Cap for damage modifications (Wetness effects).",
                        "潮湿相关伤害修正（增伤/减伤）的全局上限。")
                .defineInRange("steam_max_reduction", 0.9, 0.0, 1.0);

        // --- Cloud Properties / 蒸汽云属性 ---
        BUILDER.push("cloud_properties");
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
                        "进入蒸汽云雾时获得的失明效果持续时间（Tick）。")
                .defineInRange("steam_blindness_duration", 60, 20, 600);

        STEAM_CLEAR_AGGRO = BUILDER
                .comment("Whether steam clouds clear mob aggro.",
                        "是否清除位于蒸汽云内怪物的仇恨目标。")
                .define("steam_clear_aggro", true);

        STEAM_CLOUD_HEIGHT_CEILING = BUILDER
                .comment("The maximum height above the steam cloud center where entities are affected.",
                        "蒸汽云上方受影响的最大高度（超过此高度不受伤害）。")
                .defineInRange("steam_cloud_height_ceiling", 3.0, 0.0, 16.0);
        BUILDER.pop();

        // --- Condensation (Low-Heat) / 冷凝逻辑 (低温) ---
        BUILDER.push("condensation_logic");
        STEAM_CONDENSATION_STEP_FROST= BUILDER
                .comment("Fire Enhancement points required per level of Low-Heat Steam cloud (Frost attack Fire).",
                        "低温蒸汽云：每增加 1 个等级所需的目标冰霜强化点数步长。",
                        "Formula: Level = 1 + (TargetFirePower / Step)")
                .defineInRange("steam_condensation_step_frost", 20, 1, 1000);

        STEAM_CONDENSATION_STEP_FIRE = BUILDER
                .comment("Frost Enhancement points required per level of High-Heat Steam cloud (Fire attack Frost).",
                        "高温蒸汽云：每增加 1 个等级所需的目标赤焰强化点数步长。",
                        "Formula: Level = 1 + (TargetFrostPower / Step)")
                .defineInRange("steam_condensation_step_fire", 20, 1, 1000);

        STEAM_CONDENSATION_DELAY = BUILDER
                .comment("Time (in Ticks) an entity must stay inside Low-Heat Steam to gain Wetness.",
                        "实体在低温蒸汽中需停留多久（tick）才能获得 1 层潮湿效果。",
                        "默认 100 Tick = 5 秒。")
                .defineInRange("steam_condensation_delay", 100, 1, 2400);

        STEAM_CONDENSATION_DURATION_BASE = BUILDER
                .comment("Base duration of the Low-Heat steam cloud (in Ticks).",
                        "低温蒸汽云的基础持续时间（20 Tick = 1秒）。")
                .defineInRange("steam_condensation_duration_base", 100, 20, 1200);

        STEAM_CONDENSATION_DURATION_PER_LEVEL = BUILDER
                .comment("Extra duration added per Low-Heat steam level (in Ticks).",
                        "低温蒸汽云每级增加的持续时间（20 Tick = 1秒）。")
                .defineInRange("steam_condensation_duration_per_level", 20, 0, 200);

        STEAM_SPORE_GROWTH_RATE = BUILDER
                .comment("Ticks required to grow spore stacks in Low-Heat Steam.",
                        "低温蒸汽中孢子增殖的频率（Ticks）。")
                .defineInRange("steam_spore_growth_rate", 20, 1, 600);
        BUILDER.pop();

        // --- Scalding Damage / 烫伤伤害 ---
        BUILDER.push("scalding_damage");
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
                        "对冰霜或自然属性生物造成的烫伤伤害倍率（克制加成）。")
                .defineInRange("steam_scalding_multiplier_weakness", 1.5, 1.0, 10.0);

        STEAM_SCALDING_MULTIPLIER_SPORE = BUILDER
                .comment("Damage multiplier for entities with Flammable Spores in high-heat steam.",
                        "高温蒸汽对具有易燃孢子实体的烫伤伤害倍率（1.5 = 150%）。")
                .defineInRange("steam_scalding_multiplier_spore", 1.5, 1.0, 10.0);

        STEAM_IMMUNITY_THRESHOLD = BUILDER
                .comment("Fire Resistance points required to be immune to steam scalding.",
                        "免疫蒸汽烫伤所需的赤焰抗性点数阈值。")
                .defineInRange("steam_immunity_threshold", 80, 0, 1000);

        STEAM_IMMUNITY_BLACKLIST = BUILDER
                .comment("Entities in this list are immune to steam scalding.",
                        "蒸汽烫伤免疫黑名单（ID格式）。")
                .defineListAllowEmpty("steam_immunity_blacklist", List.of(), o -> o instanceof String);
        BUILDER.pop();

        // --- Trigger Logic / 触发逻辑 (双轨制) ---
        BUILDER.push("trigger_logic");
        STEAM_TRIGGER_THRESHOLD_FIRE = BUILDER
                .comment("Fire Power threshold required to trigger Steam Burst vs Frost/Wet.",
                        "赤焰属性攻击触发高温蒸汽云所需的强化值。")
                .defineInRange("fire_trigger_threshold", 20, 0, 1000);

        STEAM_TRIGGER_THRESHOLD_FROST = BUILDER
                .comment("Frost Power threshold required to trigger Steam Condensation vs Fire.",
                        "冰霜属性攻击触发低温蒸汽云所需的强化值。")
                .defineInRange("frost_trigger_threshold", 20, 0, 1000);
        BUILDER.pop();

        // --- Defense System / 防御体系 (分层防御) ---
        BUILDER.push("defense_system");
        STEAM_DAMAGE_FLOOR_RATIO = BUILDER
                .comment("Minimum damage floor ratio for vulnerable targets (Frost/Nature).",
                        "Final Damage >= Raw Damage * Floor Ratio.",
                        "高温蒸汽云对冰霜/自然属性生物时的最小保底伤害比例。",
                        "即：无论冰霜自然属性生物的赤焰抗性多高，高温蒸汽云至少会造成（原始伤害 x 此比例）的伤害。")
                .defineInRange("damage_floor_ratio", 0.5, 0.0, 1.0);

        STEAM_MAX_FIRE_PROT_CAP = BUILDER
                .comment("Maximum damage reduction cap allowed from Fire Protection enchantment.",
                        "火焰保护附魔对高温蒸汽云提供的最大伤害减免上限（0.5 = 最多减免 50%）。")
                .defineInRange("max_fire_prot_cap", 0.5, 0.0, 1.0);

        STEAM_MAX_GENERAL_PROT_CAP = BUILDER
                .comment("Maximum damage reduction cap allowed from general Protection enchantment.",
                        "普通保护附魔对高温蒸汽云提供的最大伤害减免上限（0.25 = 最多减免 25%）。")
                .defineInRange("max_general_prot_cap", 0.25, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.pop();

        // ======================== Scorched Mechanic / 灼烧机制 ========================
        BUILDER.comment("Scorched Mechanic Configuration", "灼烧机制配置")
                .push("scorched_mechanic");

        SCORCHED_TRIGGER_THRESHOLD = BUILDER
                .comment("Fire Attribute points required to trigger Scorched effect.",
                        "触发灼烧效果所需的攻击者赤焰属性点数阈值。")
                .defineInRange("scorched_trigger_threshold", 20, 1, 1000);

        SCORCHED_BASE_CHANCE = BUILDER
                .comment("Base probability (0.0 - 1.0) to trigger Scorched.",
                        "触发灼烧的基础概率（20% = 0.2）。")
                .defineInRange("scorched_base_chance", 0.2, 0.0, 1.0);

        SCORCHED_CHANCE_PER_POINT = BUILDER
                .comment("Extra probability added per point of Fire Enhancement.",
                        "每点赤焰强化属性额外增加的触发概率（例如 0.001 表示每 100 点增加 10%）。")
                .defineInRange("scorched_chance_per_point", 0.001, 0.0, 0.1);

        SCORCHED_DURATION = BUILDER
                .comment("Duration of the Scorched effect in Ticks (20 Ticks = 1 Second).",
                        "灼烧状态的持续时间（Tick）。默认 100 Tick = 5 秒。")
                .defineInRange("scorched_duration", 100, 20, 1200);

        SCORCHED_COOLDOWN = BUILDER
                .comment("Cooldown (in Ticks) before Scorched effect can be triggered again.",
                        "灼烧效果触发后的内置冷却时间（Tick）。默认 200 Tick = 10 秒。")
                .defineInRange("scorched_cooldown", 200, 0, 6000);

        SCORCHED_BURNING_LOCK_DURATION = BUILDER
                .comment("Duration (in Seconds) to force visual burning state when Scorched is active.",
                        "灼烧激活时，强制保持生物燃烧视觉状态的时长（秒）。用于防止雨水/浅水立刻熄灭火焰视觉。")
                .defineInRange("scorched_burning_lock_duration", 3.0, 1.0, 60.0);

        SCORCHED_DAMAGE_BASE = BUILDER
                .comment("Base damage per second for Scorched effect.",
                        "灼烧效果每秒造成的基础伤害点数。")
                .defineInRange("scorched_damage_base", 1.0, 0.1, 100.0);

        SCORCHED_DAMAGE_SCALING_STEP = BUILDER
                .comment("Fire Strength points required to increase Scorched damage by 0.5.",
                        "每多少点赤焰强化属性，增加 0.5 点灼烧伤害（固定步长）。")
                .defineInRange("scorched_damage_scaling_step", 20, 1, 1000);

        SCORCHED_RESIST_THRESHOLD = BUILDER
                .comment("Fire Resistance points required to fully negate Scorched damage.",
                        "完全免疫灼烧伤害所需的赤焰抗性阈值。")
                .defineInRange("scorched_resist_threshold", 80, 1, 1000);

        SCORCHED_IMMUNE_MODIFIER = BUILDER
                .comment("Damage multiplier for entities that are immune to fire/burning (e.g. Nether mobs).",
                        "对火焰/燃烧免疫的生物（如下界生物）受到灼烧伤害的系数。0.5 = 50% 伤害。")
                .defineInRange("scorched_immune_modifier", 0.5, 0.0, 1.0);

        SCORCHED_FIRE_PROT_REDUCTION = BUILDER
                .comment("Max damage reduction from Fire Protection enchantment (16 Levels = Max).",
                        "火焰保护附魔提供的最大伤害减免比例（16级达到上限，默认 0.5 = 50%）。")
                .defineInRange("scorched_fire_prot_reduction", 0.5, 0.0, 1.0);

        SCORCHED_GEN_PROT_REDUCTION = BUILDER
                .comment("Max damage reduction from Protection enchantment (16 Levels = Max).",
                        "普通保护附魔提供的最大伤害减免比例（16级达到上限，默认 0.25 = 25%）。")
                .defineInRange("scorched_gen_prot_reduction", 0.25, 0.0, 1.0);

        SCORCHED_NATURE_MULTIPLIER = BUILDER
                .comment("Damage multiplier for Nature attribute entities.",
                        "自然属性生物受到的灼烧伤害倍率（默认 1.5 = 150%）。")
                .defineInRange("scorched_nature_multiplier", 1.5, 1.0, 10.0);

        SCORCHED_ENTITY_BLACKLIST = BUILDER
                .comment("Entities in this list will NEVER be affected by Scorched.",
                        "灼烧效果黑名单（ID格式）。")
                .defineListAllowEmpty("scorched_entity_blacklist", List.of(), o -> o instanceof String);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // Static Cache / 静态缓存
    
    // Wetness
    public static int wetnessMaxLevel;
    public static double wetnessShallowWaterCapRatio;
    public static double wetnessResistModifier;
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
    // V1.5.1 New Multipliers
    public static double sporeThunderMultiplier;
    public static double sporeFireReduction;
    public static double sporeFireDurationReduction;

    // Contagion System
    public static int contagionCheckInterval;
    public static double contagionBaseRadius;
    public static double contagionRadiusPerStack;
    public static double contagionIntensityRatio;

    // Nature Reaction
    
    // Dynamic Parasitism Cache
    public static double natureParasiteBaseThreshold;
    public static double natureParasiteBaseChance;
    public static double natureParasiteScalingStep;
    public static double natureParasiteScalingChance;
    public static double natureParasiteWetnessBonus;
    
    public static int natureSiphonThreshold;
    public static double natureDrainPowerStep; // [New]
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
    
    // V1.5.0 Cleaned Variables
    public static double blastGrowthDamage;
    public static double blastBaseRange;
    public static double blastGrowthRange;
    public static double blastScorchBase;
    public static double blastBaseScorchTime;
    public static double blastScorchPerStack;
    public static double blastGrowthScorchTime;

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
        // Wetness System / 潮湿系统
        wetnessMaxLevel = WETNESS_MAX_LEVEL.get();
        wetnessShallowWaterCapRatio = WETNESS_SHALLOW_WATER_CAP_RATIO.get();
        wetnessResistModifier = WETNESS_RESIST_MODIFIER.get();
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

        // Spore System / 孢子系统
        sporeMaxStacks = SPORE_MAX_STACKS.get();
        sporePoisonDamage = SPORE_POISON_DAMAGE.get();
        sporeSpeedReduction = SPORE_SPEED_REDUCTION.get();
        sporePhysResist = SPORE_PHYS_RESIST.get();
        sporeFireVulnPerStack = SPORE_FIRE_VULN_PER_STACK.get();
        sporeDurationPerStack = SPORE_DURATION_PER_STACK.get();
        // V1.5.1
        sporeThunderMultiplier = SPORE_THUNDER_MULTIPLIER.get();
        sporeFireReduction = SPORE_FIRE_REDUCTION.get();
        sporeFireDurationReduction = SPORE_FIRE_DURATION_REDUCTION.get();

        // Contagion System / 传染系统
        contagionCheckInterval = CONTAGION_CHECK_INTERVAL.get();
        contagionBaseRadius = CONTAGION_BASE_RADIUS.get();
        contagionRadiusPerStack = CONTAGION_RADIUS_PER_STACK.get();
        contagionIntensityRatio = CONTAGION_INTENSITY_RATIO.get();

        // Nature Reaction / 自然反应
        
        natureParasiteBaseThreshold = NATURE_PARASITE_BASE_THRESHOLD.get();
        natureParasiteBaseChance = NATURE_PARASITE_BASE_CHANCE.get();
        natureParasiteScalingStep = NATURE_PARASITE_SCALING_STEP.get();
        natureParasiteScalingChance = NATURE_PARASITE_SCALING_CHANCE.get();
        natureParasiteWetnessBonus = NATURE_PARASITE_WETNESS_BONUS.get();

        natureSiphonThreshold = NATURE_SIPHON_THRESHOLD.get();
        natureDrainPowerStep = NATURE_DRAIN_POWER_STEP.get(); // [New]
        natureDrainAmount = NATURE_DRAIN_AMOUNT.get();
        natureSiphonHeal = NATURE_SIPHON_HEAL.get();
        natureDrainCooldown = NATURE_DRAIN_COOLDOWN.get();
        wildfireTriggerThreshold = WILDFIRE_TRIGGER_THRESHOLD.get();
        wildfireCooldown = WILDFIRE_COOLDOWN.get();
        wildfireRadius = WILDFIRE_RADIUS.get();
        wildfireKnockback = WILDFIRE_KNOCKBACK.get();
        wildfireSporeAmount = WILDFIRE_SPORE_AMOUNT.get();

        // Toxic Blast / 毒火爆燃
        blastTriggerThreshold = BLAST_TRIGGER_THRESHOLD.get();
        blastWeakIgniteMult = BLAST_WEAK_IGNITE_MULT.get();
        blastBaseDamage = BLAST_BASE_DAMAGE.get();
        blastDmgStep = BLAST_DMG_STEP.get();
        blastDmgAmount = BLAST_DMG_AMOUNT.get();
        
        // V1.5.0 Cleaned Assignments
        blastGrowthDamage = BLAST_GROWTH_DAMAGE.get();
        blastBaseRange = BLAST_BASE_RANGE.get();
        blastGrowthRange = BLAST_GROWTH_RANGE.get();
        blastScorchBase = BLAST_SCORCH_BASE.get(); 
        
        blastBaseScorchTime = BLAST_BASE_SCORCH_TIME.get();
        blastScorchPerStack = BLAST_SCORCH_PER_STACK.get();
        blastGrowthScorchTime = BLAST_GROWTH_SCORCH_TIME.get();

        // Steam Reaction - Global / 蒸汽反应 - 全局
        steamReactionEnabled = STEAM_REACTION_ENABLED.get();
        steamHighHeatMaxLevel = STEAM_HIGH_HEAT_MAX_LEVEL.get();
        steamLowHeatMaxLevel = STEAM_LOW_HEAT_MAX_LEVEL.get();
        steamMaxReduction = STEAM_MAX_REDUCTION.get();

        // Steam Reaction - Cloud / 蒸汽反应 - 云属性
        steamCloudRadius = STEAM_CLOUD_RADIUS.get();
        steamRadiusPerLevel = STEAM_RADIUS_PER_LEVEL.get();
        steamCloudDuration = STEAM_CLOUD_DURATION.get();
        steamDurationPerLevel = STEAM_DURATION_PER_LEVEL.get();
        steamBlindnessDuration = STEAM_BLINDNESS_DURATION.get();
        steamClearAggro = STEAM_CLEAR_AGGRO.get();
        steamCloudHeightCeiling = STEAM_CLOUD_HEIGHT_CEILING.get();

        // Steam Reaction - Condensation / 蒸汽反应 - 冷凝
        steamCondensationStepFire = STEAM_CONDENSATION_STEP_FIRE.get();
        steamCondensationStepFrost = STEAM_CONDENSATION_STEP_FROST.get();
        steamCondensationDelay = STEAM_CONDENSATION_DELAY.get();
        steamCondensationDurationBase = STEAM_CONDENSATION_DURATION_BASE.get();
        steamCondensationDurationPerLevel = STEAM_CONDENSATION_DURATION_PER_LEVEL.get();
        steamSporeGrowthRate = STEAM_SPORE_GROWTH_RATE.get();

        // Steam Reaction - Scalding / 蒸汽反应 - 烫伤
        steamScaldingDamage = STEAM_SCALDING_DAMAGE.get();
        steamDamageScalePerLevel = STEAM_DAMAGE_SCALE_PER_LEVEL.get();
        steamScaldingMultiplierWeakness = STEAM_SCALDING_MULTIPLIER_WEAKNESS.get();
        steamScaldingMultiplierSpore = STEAM_SCALDING_MULTIPLIER_SPORE.get();
        steamImmunityThreshold = STEAM_IMMUNITY_THRESHOLD.get();
        cachedSteamBlacklist = STEAM_IMMUNITY_BLACKLIST.get();

        // Steam Reaction - Trigger & Defense / 蒸汽反应 - 触发与防御
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