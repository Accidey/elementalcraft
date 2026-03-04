// src/main/java/com/xulai/elementalcraft/config/ElementalThunderFrostReactionsConfig.java
package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * ElementalThunderFrostReactionsConfig
 * <p>
 * 中文说明：
 * 雷霆-冰霜反应系统专用配置文件类。
 * 定义了静电效果的触发规则、叠层限制、持续时间、周期性伤害、免疫阈值以及保底伤害次数等参数。
 * 静电效果是雷霆属性特有的持续伤害效果，可通过强化点数提升触发概率和伤害。
 * 当目标的雷霆抗性达到免疫阈值后，将完全免疫静电的叠加和伤害。
 * <p>
 * English Description:
 * Dedicated configuration class for the Thunder-Frost reaction system.
 * Defines parameters for the Static Shock effect, including trigger rules,
 * stack limits, duration, periodic damage, immunity threshold, and guaranteed hits per stack.
 * Static Shock is a damage-over-time effect unique to the Thunder attribute,
 * with its chance and damage scaling based on attribute strength.
 * When the target's Thunder resistance reaches the immunity threshold,
 * it becomes completely immune to Static Shock stacking and damage.
 */
public final class ElementalThunderFrostReactionsConfig {
    public static final ForgeConfigSpec SPEC;

    // ======================== Config Specs / 配置项定义 ========================

    // --- Trigger & Stack Rules / 触发与叠加规则 ---

    public static final ForgeConfigSpec.IntValue THUNDER_STRENGTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue STATIC_BASE_CHANCE;
    public static final ForgeConfigSpec.IntValue STATIC_SCALING_STEP;
    public static final ForgeConfigSpec.DoubleValue STATIC_SCALING_CHANCE;
    public static final ForgeConfigSpec.IntValue STATIC_MAX_STACKS_PER_ATTACK;
    public static final ForgeConfigSpec.IntValue STATIC_MAX_TOTAL_STACKS;
    public static final ForgeConfigSpec.IntValue STATIC_DURATION_PER_STACK_TICKS;   // in ticks / 以刻为单位

    // --- Immunity Rule / 免疫规则 ---

    public static final ForgeConfigSpec.IntValue STATIC_RESIST_IMMUNITY_THRESHOLD;

    // --- Periodic Damage (DOT) / 周期性伤害 ---

    public static final ForgeConfigSpec.IntValue STATIC_DAMAGE_INTERVAL_TICKS;       // in ticks / 以刻为单位
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_MIN;
    public static final ForgeConfigSpec.DoubleValue STATIC_DAMAGE_MAX;

    // --- Guaranteed Hits per Stack / 每层保底伤害次数 ---

    public static final ForgeConfigSpec.IntValue STATIC_HITS_PER_STACK;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        // ======================== Static Shock System / 静电系统 ========================
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

        BUILDER.pop(); // pop trigger_and_stack

        BUILDER.comment("Immunity Rule", "免疫规则")
                .push("immunity");

        STATIC_RESIST_IMMUNITY_THRESHOLD = BUILDER
                .comment("Thunder Resistance points required for an entity to become completely immune to Static Shock (both stacking and damage).",
                         "实体完全免疫静电（叠加和伤害）所需的雷霆抗性点数。",
                         "Default: 80")
                .defineInRange("static_resist_immunity_threshold", 80, 1, 10000);

        BUILDER.pop(); // pop immunity

        BUILDER.comment("Periodic Damage (Damage over Time)", "周期性伤害（持续伤害）")
                .push("periodic_damage");

        STATIC_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Interval (in ticks) between each damage tick while Static Shock is active. 20 ticks = 1 second.",
                         "静电生效期间，每次造成伤害的间隔（以刻为单位）。20刻 = 1秒。",
                         "Default: 100 (5 seconds)")
                .defineInRange("static_damage_interval_ticks", 100, 1, 72000);

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

        BUILDER.pop(); // pop periodic_damage

        BUILDER.comment("Guaranteed Hits per Stack", "每层保底伤害次数")
                .push("guaranteed_hits");

        STATIC_HITS_PER_STACK = BUILDER
                .comment("Number of damage ticks that must occur per stack of Static Shock during its duration. Total required hits = stacks × hits_per_stack.",
                         "每层静电在持续时间内必须触发的伤害次数。总所需次数 = 层数 × 每层次数。",
                         "Default: 1")
                .defineInRange("static_hits_per_stack", 1, 1, 100);

        BUILDER.pop(); // pop guaranteed_hits
        BUILDER.pop(); // pop static_shock

        SPEC = BUILDER.build();
    }

    // ======================== Static Cache Fields / 静态缓存字段 ========================

    public static int thunderStrengthThreshold;
    public static double staticBaseChance;
    public static int staticScalingStep;
    public static double staticScalingChance;
    public static int staticMaxStacksPerAttack;
    public static int staticMaxTotalStacks;
    public static int staticDurationPerStackTicks;
    public static int staticResistImmunityThreshold;
    public static int staticDamageIntervalTicks;
    public static double staticDamageMin;
    public static double staticDamageMax;
    public static int staticHitsPerStack;

    /**
     * Registers this configuration file with Forge.
     * <p>
     * 将此配置文件注册到 Forge。
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "elementalcraft-thunderfrost-reactions.toml");
    }

    /**
     * Refreshes the static cache by reading the latest values from the config spec.
     * Must be called during {@link net.minecraftforge.fml.event.config.ModConfigEvent} to keep cache in sync.
     * <p>
     * 从配置规范中读取最新值，刷新静态缓存。
     * 必须在 {@link net.minecraftforge.fml.event.config.ModConfigEvent} 事件中调用，以保持缓存同步。
     */
    public static void refreshCache() {
        thunderStrengthThreshold = THUNDER_STRENGTH_THRESHOLD.get();
        staticBaseChance = STATIC_BASE_CHANCE.get();
        staticScalingStep = STATIC_SCALING_STEP.get();
        staticScalingChance = STATIC_SCALING_CHANCE.get();
        staticMaxStacksPerAttack = STATIC_MAX_STACKS_PER_ATTACK.get();
        staticMaxTotalStacks = STATIC_MAX_TOTAL_STACKS.get();
        staticDurationPerStackTicks = STATIC_DURATION_PER_STACK_TICKS.get();
        staticResistImmunityThreshold = STATIC_RESIST_IMMUNITY_THRESHOLD.get();
        staticDamageIntervalTicks = STATIC_DAMAGE_INTERVAL_TICKS.get();
        staticDamageMin = STATIC_DAMAGE_MIN.get();
        staticDamageMax = STATIC_DAMAGE_MAX.get();
        staticHitsPerStack = STATIC_HITS_PER_STACK.get();
    }

    // Private constructor to prevent instantiation
    private ElementalThunderFrostReactionsConfig() {}
}