package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ElementalVisualConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue FIRE_MELEE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue FIRE_RANGED_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NATURE_MELEE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue NATURE_RANGED_ENABLED;
    public static final ForgeConfigSpec.BooleanValue THUNDER_MELEE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue THUNDER_RANGED_ENABLED;

    public static final ForgeConfigSpec.BooleanValue GLOBAL_VISIBILITY_CHECK_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GLOBAL_VIEW_DISTANCE_MULTIPLIER;

    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_BASE_ANGLE_DEGREES;
    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_ANGLE_MULTIPLIER_BASE;
    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_ANGLE_MULTIPLIER_PER_TIER;
    public static final ForgeConfigSpec.IntValue FIRE_MELEE_PARTICLE_COUNT_BASE;
    public static final ForgeConfigSpec.IntValue FIRE_MELEE_PARTICLE_COUNT_OFFSET;
    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_WAVE_AMPLITUDE;
    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_WAVE_FREQUENCY;
    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_SOUL_FLAME_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_LAVA_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FIRE_MELEE_SOUL_CHANCE;
    public static final ForgeConfigSpec.BooleanValue FIRE_MELEE_ENABLE_SOUL_FLAME;
    public static final ForgeConfigSpec.BooleanValue FIRE_MELEE_ENABLE_LAVA;
    public static final ForgeConfigSpec.BooleanValue FIRE_MELEE_ENABLE_SOUL;

    public static final ForgeConfigSpec.DoubleValue FIRE_RANGED_CONE_MAX_RADIUS;
    public static final ForgeConfigSpec.DoubleValue FIRE_RANGED_BACK_OFFSET_START;
    public static final ForgeConfigSpec.DoubleValue FIRE_RANGED_ROTATION_SPEED;
    public static final ForgeConfigSpec.DoubleValue FIRE_RANGED_INNER_RADIUS_FACTOR;
    public static final ForgeConfigSpec.IntValue FIRE_RANGED_INNER_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue FIRE_RANGED_ACTIVATION_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue FIRE_RANGED_OUTER_REVERSE_ROTATION;
    public static final ForgeConfigSpec.BooleanValue FIRE_RANGED_INNER_REVERSE_ROTATION;
    public static final ForgeConfigSpec.IntValue FIRE_RANGED_OUTER_HELIX_COUNT_PER_TIER;
    public static final ForgeConfigSpec.IntValue FIRE_RANGED_INNER_HELIX_COUNT_PER_TIER;
    public static final ForgeConfigSpec.IntValue FIRE_RANGED_TRAIL_LAVA_PARTICLE_COUNT;
    public static final ForgeConfigSpec.IntValue FIRE_RANGED_TRAIL_SOUL_PARTICLE_COUNT;
    public static final ForgeConfigSpec.DoubleValue FIRE_RANGED_TRAIL_LAVA_SPREAD;
    public static final ForgeConfigSpec.DoubleValue FIRE_RANGED_TRAIL_SOUL_SPREAD;
    public static final ForgeConfigSpec.BooleanValue FIRE_RANGED_ENABLE_OUTER_HELIX;
    public static final ForgeConfigSpec.BooleanValue FIRE_RANGED_ENABLE_INNER_HELIX;
    public static final ForgeConfigSpec.BooleanValue FIRE_RANGED_ENABLE_TRAIL_PARTICLES;

    public static final ForgeConfigSpec.IntValue FIRE_IMPACT_FLAME_PARTICLE_COUNT_PER_TIER;
    public static final ForgeConfigSpec.DoubleValue FIRE_IMPACT_FLAME_SPREAD;
    public static final ForgeConfigSpec.BooleanValue FIRE_IMPACT_LAVA_ENABLED;
    public static final ForgeConfigSpec.IntValue FIRE_IMPACT_LAVA_PARTICLE_COUNT_PER_TIER;
    public static final ForgeConfigSpec.DoubleValue FIRE_IMPACT_LAVA_SPREAD;
    public static final ForgeConfigSpec.BooleanValue FIRE_IMPACT_SOUL_FLAME_ENABLED;
    public static final ForgeConfigSpec.IntValue FIRE_IMPACT_SOUL_FLAME_COUNT;
    public static final ForgeConfigSpec.DoubleValue FIRE_IMPACT_SOUL_FLAME_SPREAD;
    public static final ForgeConfigSpec.BooleanValue FIRE_IMPACT_CAMPFIRE_SMOKE_ENABLED;
    public static final ForgeConfigSpec.IntValue FIRE_IMPACT_SMOKE_COUNT;
    public static final ForgeConfigSpec.DoubleValue FIRE_IMPACT_SMOKE_SPREAD_XZ;
    public static final ForgeConfigSpec.DoubleValue FIRE_IMPACT_SMOKE_SPREAD_Y;

    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_BASE_ANGLE_DEGREES;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_ANGLE_MULTIPLIER_BASE;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_ANGLE_MULTIPLIER_PER_TIER;
    public static final ForgeConfigSpec.IntValue NATURE_MELEE_PARTICLE_COUNT_BASE;
    public static final ForgeConfigSpec.IntValue NATURE_MELEE_PARTICLE_COUNT_OFFSET;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_WAVE_AMPLITUDE;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_WAVE_FREQUENCY;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_COMPOSTER_SPEED_XZ;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_SPORE_BLOSSOM_CHANCE;
    public static final ForgeConfigSpec.BooleanValue NATURE_MELEE_CHERRY_LEAVES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_CHERRY_LEAVES_CHANCE;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_CHERRY_LEAVES_MIN_PROGRESS;
    public static final ForgeConfigSpec.BooleanValue NATURE_MELEE_WAX_ON_ENABLED;
    public static final ForgeConfigSpec.DoubleValue NATURE_MELEE_WAX_ON_MIN_PROGRESS;

    public static final ForgeConfigSpec.DoubleValue NATURE_RANGED_CONE_MAX_RADIUS;
    public static final ForgeConfigSpec.DoubleValue NATURE_RANGED_BACK_OFFSET_START;
    public static final ForgeConfigSpec.DoubleValue NATURE_RANGED_ROTATION_SPEED;
    public static final ForgeConfigSpec.IntValue NATURE_RANGED_OUTER_HELIX_COUNT_PER_TIER;
    public static final ForgeConfigSpec.IntValue NATURE_RANGED_ACTIVATION_INTERVAL;
    public static final ForgeConfigSpec.IntValue NATURE_RANGED_TAIL_HELIX_COUNT_PER_TIER;
    public static final ForgeConfigSpec.IntValue NATURE_RANGED_TAIL_DELAY_TICKS;
    public static final ForgeConfigSpec.DoubleValue NATURE_RANGED_TAIL_RADIUS_FACTOR;
    public static final ForgeConfigSpec.BooleanValue NATURE_RANGED_OUTER_REVERSE_ROTATION;
    public static final ForgeConfigSpec.BooleanValue NATURE_RANGED_TAIL_REVERSE_ROTATION;
    public static final ForgeConfigSpec.IntValue NATURE_RANGED_MAIN_PARTICLE_COUNT;
    public static final ForgeConfigSpec.IntValue NATURE_RANGED_TAIL_PARTICLE_COUNT;
    public static final ForgeConfigSpec.BooleanValue NATURE_RANGED_CENTER_PARTICLE_ENABLED;
    public static final ForgeConfigSpec.IntValue NATURE_RANGED_CENTER_PARTICLE_COUNT;

    public static final ForgeConfigSpec.IntValue NATURE_IMPACT_HAPPY_VILLAGER_COUNT_PER_TIER;
    public static final ForgeConfigSpec.DoubleValue NATURE_IMPACT_HAPPY_VILLAGER_SPREAD;
    public static final ForgeConfigSpec.DoubleValue NATURE_IMPACT_HAPPY_VILLAGER_SPEED;
    public static final ForgeConfigSpec.BooleanValue NATURE_IMPACT_SPORE_BLOSSOM_ENABLED;
    public static final ForgeConfigSpec.IntValue NATURE_IMPACT_SPORE_BLOSSOM_COUNT;
    public static final ForgeConfigSpec.DoubleValue NATURE_IMPACT_SPORE_BLOSSOM_SPREAD_XZ;
    public static final ForgeConfigSpec.DoubleValue NATURE_IMPACT_SPORE_BLOSSOM_SPREAD_Y;
    public static final ForgeConfigSpec.DoubleValue NATURE_IMPACT_SPORE_BLOSSOM_SPEED;
    public static final ForgeConfigSpec.BooleanValue NATURE_IMPACT_CHERRY_LEAVES_ENABLED;
    public static final ForgeConfigSpec.IntValue NATURE_IMPACT_CHERRY_LEAVES_COUNT;
    public static final ForgeConfigSpec.DoubleValue NATURE_IMPACT_CHERRY_LEAVES_SPREAD_XZ;
    public static final ForgeConfigSpec.DoubleValue NATURE_IMPACT_CHERRY_LEAVES_SPREAD_Y;
    public static final ForgeConfigSpec.DoubleValue NATURE_IMPACT_CHERRY_LEAVES_SPEED;

    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_BASE_ANGLE_DEGREES;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_ANGLE_MULTIPLIER_BASE;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_ANGLE_MULTIPLIER_PER_TIER;
    public static final ForgeConfigSpec.IntValue THUNDER_MELEE_PARTICLE_COUNT_BASE;
    public static final ForgeConfigSpec.IntValue THUNDER_MELEE_PARTICLE_COUNT_OFFSET;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_FORWARD_OFFSET_FACTOR;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_FALL_SPEED;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_GLOW_CHANCE_TIER2;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_REVERSE_PORTAL_CHANCE_TIER3;
    public static final ForgeConfigSpec.BooleanValue THUNDER_MELEE_ARC_LINE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MELEE_ARC_LINE_STEP_FACTOR;

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGED_CONE_MAX_RADIUS;
    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGED_BACK_OFFSET_START;
    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGED_ROTATION_SPEED;
    public static final ForgeConfigSpec.IntValue THUNDER_RANGED_HELIX_COUNT_PER_TIER;
    public static final ForgeConfigSpec.IntValue THUNDER_RANGED_ACTIVATION_INTERVAL;
    public static final ForgeConfigSpec.IntValue THUNDER_RANGED_MAIN_PARTICLE_COUNT;
    public static final ForgeConfigSpec.BooleanValue THUNDER_RANGED_TAIL_END_ROD_ENABLED;
    public static final ForgeConfigSpec.IntValue THUNDER_RANGED_TAIL_END_ROD_COUNT;
    public static final ForgeConfigSpec.BooleanValue THUNDER_RANGED_TAIL_REVERSE_PORTAL_ENABLED;
    public static final ForgeConfigSpec.IntValue THUNDER_RANGED_TAIL_REVERSE_PORTAL_GROUPS;
    public static final ForgeConfigSpec.IntValue THUNDER_RANGED_TAIL_REVERSE_PORTAL_COUNT;
    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGED_TAIL_REVERSE_PORTAL_SPREAD;
    public static final ForgeConfigSpec.BooleanValue THUNDER_RANGED_TAIL_DRAGON_BREATH_ENABLED;
    public static final ForgeConfigSpec.IntValue THUNDER_RANGED_TAIL_DRAGON_BREATH_GROUPS;
    public static final ForgeConfigSpec.IntValue THUNDER_RANGED_TAIL_DRAGON_BREATH_COUNT;
    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGED_TAIL_DRAGON_BREATH_SPREAD;

    public static final ForgeConfigSpec.IntValue THUNDER_IMPACT_GLOW_COUNT_PER_TIER;
    public static final ForgeConfigSpec.DoubleValue THUNDER_IMPACT_GLOW_SPREAD;
    public static final ForgeConfigSpec.DoubleValue THUNDER_IMPACT_GLOW_SPEED;
    public static final ForgeConfigSpec.IntValue THUNDER_IMPACT_END_ROD_COUNT_PER_TIER;
    public static final ForgeConfigSpec.DoubleValue THUNDER_IMPACT_END_ROD_SPREAD;
    public static final ForgeConfigSpec.DoubleValue THUNDER_IMPACT_END_ROD_SPEED;
    public static final ForgeConfigSpec.BooleanValue THUNDER_IMPACT_EXTRA_END_ROD_ENABLED;
    public static final ForgeConfigSpec.IntValue THUNDER_IMPACT_EXTRA_END_ROD_COUNT_PER_TIER;
    public static final ForgeConfigSpec.DoubleValue THUNDER_IMPACT_EXTRA_END_ROD_HORIZONTAL_SPREAD;
    public static final ForgeConfigSpec.BooleanValue THUNDER_IMPACT_EXTRA_END_ROD_VERTICAL_RANDOM;

    public static boolean fireMeleeEnabled = true;
    public static boolean fireRangedEnabled = true;
    public static boolean natureMeleeEnabled = true;
    public static boolean natureRangedEnabled = true;
    public static boolean thunderMeleeEnabled = true;
    public static boolean thunderRangedEnabled = true;

    public static boolean globalVisibilityCheckEnabled = true;
    public static double globalViewDistanceMultiplier = 1.0;

    public static double fireMeleeRadius = 2.2;
    public static double fireMeleeBaseAngleDegrees = 50.0;
    public static double fireMeleeAngleMultiplierBase = 0.3;
    public static double fireMeleeAngleMultiplierPerTier = 0.2;
    public static int fireMeleeParticleCountBase = 15;
    public static int fireMeleeParticleCountOffset = 3;
    public static double fireMeleeWaveAmplitude = 0.1;
    public static double fireMeleeWaveFrequency = 4.0;
    public static double fireMeleeSoulFlameChance = 0.4;
    public static double fireMeleeLavaChance = 0.3;
    public static double fireMeleeSoulChance = 0.25;
    public static boolean fireMeleeEnableSoulFlame = true;
    public static boolean fireMeleeEnableLava = true;
    public static boolean fireMeleeEnableSoul = true;

    public static double fireRangedConeMaxRadius = 2.4;
    public static double fireRangedBackOffsetStart = 0.3;
    public static double fireRangedRotationSpeed = 3.0;
    public static double fireRangedInnerRadiusFactor = 0.5;
    public static int fireRangedInnerDelayTicks = 2;
    public static int fireRangedActivationInterval = 1;
    public static int fireRangedOuterHelixCountPerTier = 2;
    public static int fireRangedInnerHelixCountPerTier = 2;
    public static int fireRangedTrailLavaParticleCount = 3;
    public static int fireRangedTrailSoulParticleCount = 2;
    public static double fireRangedTrailLavaSpread = 0.4;
    public static double fireRangedTrailSoulSpread = 0.5;
    public static boolean fireRangedEnableOuterHelix = true;
    public static boolean fireRangedEnableInnerHelix = true;
    public static boolean fireRangedEnableTrailParticles = true;
    public static boolean fireRangedOuterReverseRotation = true;
    public static boolean fireRangedInnerReverseRotation = false;

    public static int fireImpactFlameParticleCountPerTier = 5;
    public static double fireImpactFlameSpread = 0.3;
    public static boolean fireImpactLavaEnabled = true;
    public static int fireImpactLavaParticleCountPerTier = 4;
    public static double fireImpactLavaSpread = 0.5;
    public static boolean fireImpactSoulFlameEnabled = true;
    public static int fireImpactSoulFlameCount = 15;
    public static double fireImpactSoulFlameSpread = 0.4;
    public static boolean fireImpactCampfireSmokeEnabled = true;
    public static int fireImpactSmokeCount = 5;
    public static double fireImpactSmokeSpreadXZ = 0.2;
    public static double fireImpactSmokeSpreadY = 0.5;

    public static double natureMeleeRadius = 2.2;
    public static double natureMeleeBaseAngleDegrees = 50.0;
    public static double natureMeleeAngleMultiplierBase = 0.3;
    public static double natureMeleeAngleMultiplierPerTier = 0.2;
    public static int natureMeleeParticleCountBase = 15;
    public static int natureMeleeParticleCountOffset = 3;
    public static double natureMeleeWaveAmplitude = 0.1;
    public static double natureMeleeWaveFrequency = 4.0;
    public static double natureMeleeComposterSpeedXZ = 0.1;
    public static double natureMeleeSporeBlossomChance = 0.4;
    public static boolean natureMeleeCherryLeavesEnabled = true;
    public static double natureMeleeCherryLeavesChance = 0.5;
    public static double natureMeleeCherryLeavesMinProgress = 0.3;
    public static boolean natureMeleeWaxOnEnabled = true;
    public static double natureMeleeWaxOnMinProgress = 0.8;

    public static double natureRangedConeMaxRadius = 2.4;
    public static double natureRangedBackOffsetStart = 0.3;
    public static double natureRangedRotationSpeed = 3.0;
    public static int natureRangedOuterHelixCountPerTier = 2;
    public static int natureRangedActivationInterval = 1;
    public static int natureRangedTailHelixCountPerTier = 2;
    public static int natureRangedTailDelayTicks = 3;
    public static double natureRangedTailRadiusFactor = 0.5;
    public static boolean natureRangedOuterReverseRotation = true;
    public static boolean natureRangedTailReverseRotation = false;
    public static int natureRangedMainParticleCount = 2;
    public static int natureRangedTailParticleCount = 2;
    public static boolean natureRangedCenterParticleEnabled = true;
    public static int natureRangedCenterParticleCount = 1;

    public static int natureImpactHappyVillagerCountPerTier = 4;
    public static double natureImpactHappyVillagerSpread = 0.4;
    public static double natureImpactHappyVillagerSpeed = 0.1;
    public static boolean natureImpactSporeBlossomEnabled = true;
    public static int natureImpactSporeBlossomCount = 12;
    public static double natureImpactSporeBlossomSpreadXZ = 0.5;
    public static double natureImpactSporeBlossomSpreadY = 0.2;
    public static double natureImpactSporeBlossomSpeed = 0.01;
    public static boolean natureImpactCherryLeavesEnabled = true;
    public static int natureImpactCherryLeavesCount = 8;
    public static double natureImpactCherryLeavesSpreadXZ = 0.4;
    public static double natureImpactCherryLeavesSpreadY = 0.1;
    public static double natureImpactCherryLeavesSpeed = 0.05;

    public static double thunderMeleeRadius = 2.2;
    public static double thunderMeleeBaseAngleDegrees = 50.0;
    public static double thunderMeleeAngleMultiplierBase = 0.3;
    public static double thunderMeleeAngleMultiplierPerTier = 0.2;
    public static int thunderMeleeParticleCountBase = 15;
    public static int thunderMeleeParticleCountOffset = 3;
    public static double thunderMeleeForwardOffsetFactor = 0.5;
    public static double thunderMeleeFallSpeed = -0.01;
    public static double thunderMeleeGlowChanceTier2 = 0.4;
    public static double thunderMeleeReversePortalChanceTier3 = 0.3;
    public static boolean thunderMeleeArcLineEnabled = true;
    public static double thunderMeleeArcLineStepFactor = 4.0;

    public static double thunderRangedConeMaxRadius = 2.4;
    public static double thunderRangedBackOffsetStart = 0.3;
    public static double thunderRangedRotationSpeed = 6.0;
    public static int thunderRangedHelixCountPerTier = 2;
    public static int thunderRangedActivationInterval = 1;
    public static int thunderRangedMainParticleCount = 48;
    public static boolean thunderRangedTailEndRodEnabled = true;
    public static int thunderRangedTailEndRodCount = 3;
    public static boolean thunderRangedTailReversePortalEnabled = true;
    public static int thunderRangedTailReversePortalGroups = 3;
    public static int thunderRangedTailReversePortalCount = 5;
    public static double thunderRangedTailReversePortalSpread = 0.2;
    public static boolean thunderRangedTailDragonBreathEnabled = true;
    public static int thunderRangedTailDragonBreathGroups = 2;
    public static int thunderRangedTailDragonBreathCount = 1;
    public static double thunderRangedTailDragonBreathSpread = 0.3;

    public static int thunderImpactGlowCountPerTier = 8;
    public static double thunderImpactGlowSpread = 0.5;
    public static double thunderImpactGlowSpeed = 0.1;
    public static int thunderImpactEndRodCountPerTier = 4;
    public static double thunderImpactEndRodSpread = 0.3;
    public static double thunderImpactEndRodSpeed = 0.05;
    public static boolean thunderImpactExtraEndRodEnabled = true;
    public static int thunderImpactExtraEndRodCountPerTier = 4;
    public static double thunderImpactExtraEndRodHorizontalSpread = 1.2;
    public static boolean thunderImpactExtraEndRodVerticalRandom = true;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Elemental Visual Effects Configuration", "元素视觉特效配置")
                .push("visual_effects");

        BUILDER.comment("Global Performance & Visibility", "全局性能与可见性")
                .push("global");

        GLOBAL_VISIBILITY_CHECK_ENABLED = BUILDER
                .comment("Whether to check if the projectile is within players' view distance before spawning particles. Disabling may increase server load.",
                        "是否在生成粒子前检查投射物是否在玩家视距内。禁用可能增加服务器负载。",
                        "Default: true")
                .define("visibility_check_enabled", true);

        GLOBAL_VIEW_DISTANCE_MULTIPLIER = BUILDER
                .comment("Multiplier for the view distance threshold. Larger values make particles visible from farther away.",
                        "视距阈值的倍数。值越大，粒子在更远处可见。",
                        "Default: 1.0")
                .defineInRange("view_distance_multiplier", 1.0, 0.5, 3.0);

        BUILDER.pop();

        BUILDER.comment("Fire Attribute Visuals", "赤焰属性特效")
                .push("fire_visuals");

        FIRE_MELEE_ENABLED = BUILDER
                .comment("Whether to enable Fire attribute melee visual effects (Swing Trail & Impact).",
                        "是否开启赤焰属性的近战视觉特效（包含挥动轨迹和命中爆裂）。",
                        "Default: true")
                .define("fire_melee_enabled", true);

        FIRE_RANGED_ENABLED = BUILDER
                .comment("Whether to enable Fire attribute ranged visual effects (Projectile Trail & Impact).",
                        "是否开启赤焰属性的远程视觉特效（包含弹射物飞行拖尾和命中爆裂）。",
                        "Disable this can improve performance on servers with many entities.",
                        "在实体较多的服务器上关闭此项可以提高性能。",
                        "Default: true")
                .define("fire_ranged_enabled", true);

        BUILDER.comment("Fire Melee Swing Visuals", "赤焰近战挥动特效")
                .push("melee");

        FIRE_MELEE_RADIUS = BUILDER
                .comment("Radius of the melee swing arc (in blocks). Larger values make the arc wider.",
                        "近战挥动弧形的半径（方块距离）。数值越大，弧形范围越宽。",
                        "Default: 2.2")
                .defineInRange("radius", 2.2, 0.5, 5.0);

        FIRE_MELEE_BASE_ANGLE_DEGREES = BUILDER
                .comment("Base angle of the swing arc in degrees. This angle is multiplied by the angle multiplier.",
                        "挥动弧形的基准角度（度数）。该角度会与角度乘数相乘得到最终角度。",
                        "Default: 50.0")
                .defineInRange("base_angle_degrees", 50.0, 10.0, 180.0);

        FIRE_MELEE_ANGLE_MULTIPLIER_BASE = BUILDER
                .comment("Base multiplier for the swing angle. Final angle = base_angle * (base_multiplier + tier * per_tier).",
                        "挥动角度的基础乘数。最终角度 = 基准角度 × (基础乘数 + 等级 × 每等级增量)。",
                        "Default: 0.3")
                .defineInRange("angle_multiplier_base", 0.3, 0.1, 1.0);

        FIRE_MELEE_ANGLE_MULTIPLIER_PER_TIER = BUILDER
                .comment("Additional angle multiplier per visual tier. Higher tiers make the swing wider.",
                        "每个视觉等级增加的额外角度乘数。等级越高，挥动范围越宽。",
                        "Default: 0.2")
                .defineInRange("angle_multiplier_per_tier", 0.2, 0.0, 0.5);

        FIRE_MELEE_PARTICLE_COUNT_BASE = BUILDER
                .comment("Base number of particles along the swing arc. Actual count = base * angle_multiplier + offset.",
                        "挥动弧线上粒子的基础数量。实际数量 = 基数 × 角度乘数 + 偏移量。",
                        "Default: 15")
                .defineInRange("particle_count_base", 15, 5, 50);

        FIRE_MELEE_PARTICLE_COUNT_OFFSET = BUILDER
                .comment("Offset added to the particle count. Prevents zero particles at low multipliers.",
                        "粒子数量的偏移值，避免乘数过小时粒子数为零。",
                        "Default: 3")
                .defineInRange("particle_count_offset", 3, 0, 20);

        FIRE_MELEE_WAVE_AMPLITUDE = BUILDER
                .comment("Amplitude of the sine wave offset along the arc. Adds a wavy effect.",
                        "沿弧形轨迹的正弦波偏移幅度，增加飘动感。",
                        "Default: 0.1")
                .defineInRange("wave_amplitude", 0.1, 0.0, 0.5);

        FIRE_MELEE_WAVE_FREQUENCY = BUILDER
                .comment("Frequency of the sine wave offset (multiplied by PI). Higher values create more waves.",
                        "正弦波偏移的频率（乘以π后的倍数）。值越大，波动越密集。",
                        "Default: 4.0")
                .defineInRange("wave_frequency", 4.0, 0.0, 10.0);

        FIRE_MELEE_SOUL_FLAME_CHANCE = BUILDER
                .comment("Probability (0-1) of spawning a Soul Flame particle per position when tier >= 2.",
                        "当视觉等级 ≥ 2 时，每个粒子位置生成灵魂火焰粒子的概率（0-1）。",
                        "Default: 0.4")
                .defineInRange("soul_flame_chance", 0.4, 0.0, 1.0);

        FIRE_MELEE_LAVA_CHANCE = BUILDER
                .comment("Probability (0-1) of spawning a Lava particle per position when tier >= 3.",
                        "当视觉等级 ≥ 3 时，每个粒子位置生成熔岩粒子的概率（0-1）。",
                        "Default: 0.3")
                .defineInRange("lava_chance", 0.3, 0.0, 1.0);

        FIRE_MELEE_SOUL_CHANCE = BUILDER
                .comment("Probability (0-1) of spawning a Soul particle per position when tier >= 4.",
                        "当视觉等级 ≥ 4 时，每个粒子位置生成灵魂粒子的概率（0-1）。",
                        "Default: 0.25")
                .defineInRange("soul_chance", 0.25, 0.0, 1.0);

        FIRE_MELEE_ENABLE_SOUL_FLAME = BUILDER
                .comment("Whether to allow Soul Flame particles in melee swings (tier >= 2).",
                        "是否允许在近战挥动中生成灵魂火焰粒子（等级 ≥ 2）。",
                        "Default: true")
                .define("enable_soul_flame", true);

        FIRE_MELEE_ENABLE_LAVA = BUILDER
                .comment("Whether to allow Lava particles in melee swings (tier >= 3).",
                        "是否允许在近战挥动中生成熔岩粒子（等级 ≥ 3）。",
                        "Default: true")
                .define("enable_lava", true);

        FIRE_MELEE_ENABLE_SOUL = BUILDER
                .comment("Whether to allow Soul particles in melee swings (tier >= 4).",
                        "是否允许在近战挥动中生成灵魂粒子（等级 ≥ 4）。",
                        "Default: true")
                .define("enable_soul", true);

        BUILDER.pop();

        BUILDER.comment("Fire Ranged Projectile Trail Visuals", "赤焰远程投射物拖尾特效")
                .push("ranged");

        BUILDER.comment("Helix Geometry", "螺旋线几何参数")
                .push("geometry");

        FIRE_RANGED_OUTER_REVERSE_ROTATION = BUILDER
                .comment("Whether the outer helix rotates in reverse direction (clockwise).",
                        "外圈螺旋线是否反向旋转（顺时针）。",
                        "Default: true")
                .define("outer_reverse_rotation", true);

        FIRE_RANGED_INNER_REVERSE_ROTATION = BUILDER
                .comment("Whether the inner helix rotates in reverse direction.",
                        "内圈螺旋线是否反向旋转。",
                        "Default: false")
                .define("inner_reverse_rotation", false);

        FIRE_RANGED_CONE_MAX_RADIUS = BUILDER
                .comment("Maximum radius of the outer helix (in blocks).",
                        "外圈螺旋线的最大半径（方块）。",
                        "Default: 2.4")
                .defineInRange("cone_max_radius", 2.4, 0.5, 5.0);

        FIRE_RANGED_BACK_OFFSET_START = BUILDER
                .comment("Offset distance behind the projectile where the helix starts (in blocks).",
                        "螺旋线起点位于投射物后方的偏移距离（方块）。",
                        "Default: 0.3")
                .defineInRange("back_offset_start", 0.3, 0.0, 2.0);

        FIRE_RANGED_ROTATION_SPEED = BUILDER
                .comment("Rotation speed of the helix (radians per tick). Higher values spin faster.",
                        "螺旋线的旋转角速度（弧度/ tick）。值越大旋转越快。",
                        "Default: 3.0")
                .defineInRange("rotation_speed", 3.0, 0.5, 10.0);

        FIRE_RANGED_INNER_RADIUS_FACTOR = BUILDER
                .comment("Factor applied to outer radius to get inner helix radius (inner = outer * factor).",
                        "内圈螺旋线半径相对于外圈半径的因子（内圈半径 = 外圈半径 × 因子）。",
                        "Default: 0.5")
                .defineInRange("inner_radius_factor", 0.5, 0.1, 1.0);

        FIRE_RANGED_INNER_DELAY_TICKS = BUILDER
                .comment("Delay in ticks before the inner helix starts to appear.",
                        "内圈螺旋线开始出现的延迟时间（tick）。",
                        "Default: 2")
                .defineInRange("inner_delay_ticks", 2, 0, 20);

        FIRE_RANGED_ACTIVATION_INTERVAL = BUILDER
                .comment("How many ticks between activating additional helix strands.",
                        "激活额外螺旋线的间隔（tick）。",
                        "Default: 1")
                .defineInRange("activation_interval", 1, 1, 10);

        BUILDER.pop();

        BUILDER.comment("Particle Density", "粒子密度参数")
                .push("density");

        FIRE_RANGED_OUTER_HELIX_COUNT_PER_TIER = BUILDER
                .comment("Number of outer helix strands per visual tier. Total strands = tier * this value.",
                        "每个视觉等级的外圈螺旋线条数。总线条数 = 等级 × 此值。",
                        "Default: 2")
                .defineInRange("outer_helix_count_per_tier", 2, 1, 8);

        FIRE_RANGED_INNER_HELIX_COUNT_PER_TIER = BUILDER
                .comment("Number of inner helix strands per visual tier. Total strands = tier * this value.",
                        "每个视觉等级的内圈螺旋线条数。总线条数 = 等级 × 此值。",
                        "Default: 2")
                .defineInRange("inner_helix_count_per_tier", 2, 1, 8);

        FIRE_RANGED_TRAIL_LAVA_PARTICLE_COUNT = BUILDER
                .comment("Number of lava particles emitted at the tail each tick.",
                        "每 tick 在尾部生成的熔岩粒子数量。",
                        "Default: 3")
                .defineInRange("trail_lava_particle_count", 3, 0, 10);

        FIRE_RANGED_TRAIL_SOUL_PARTICLE_COUNT = BUILDER
                .comment("Number of soul particles emitted at the tail each tick.",
                        "每 tick 在尾部生成的灵魂粒子数量。",
                        "Default: 2")
                .defineInRange("trail_soul_particle_count", 2, 0, 10);

        FIRE_RANGED_TRAIL_LAVA_SPREAD = BUILDER
                .comment("Random spread range for lava particles (blocks).",
                        "熔岩粒子的随机偏移范围（方块）。",
                        "Default: 0.4")
                .defineInRange("trail_lava_spread", 0.4, 0.0, 1.0);

        FIRE_RANGED_TRAIL_SOUL_SPREAD = BUILDER
                .comment("Random spread range for soul particles (blocks).",
                        "灵魂粒子的随机偏移范围（方块）。",
                        "Default: 0.5")
                .defineInRange("trail_soul_spread", 0.5, 0.0, 1.0);

        FIRE_RANGED_ENABLE_OUTER_HELIX = BUILDER
                .comment("Whether to generate the outer helix (fire particles).",
                        "是否生成外圈螺旋线（火焰粒子）。",
                        "Default: true")
                .define("enable_outer_helix", true);

        FIRE_RANGED_ENABLE_INNER_HELIX = BUILDER
                .comment("Whether to generate the inner helix (soul fire particles).",
                        "是否生成内圈螺旋线（灵魂火焰粒子）。",
                        "Default: true")
                .define("enable_inner_helix", true);

        FIRE_RANGED_ENABLE_TRAIL_PARTICLES = BUILDER
                .comment("Whether to generate the tail scattered particles (lava and soul).",
                        "是否生成尾部散落粒子（熔岩和灵魂）。",
                        "Default: true")
                .define("enable_trail_particles", true);

        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("Fire Impact Explosion Visuals", "赤焰命中爆裂特效")
                .push("impact");

        FIRE_IMPACT_FLAME_PARTICLE_COUNT_PER_TIER = BUILDER
                .comment("Number of flame particles per visual tier. Total = tier * this value.",
                        "每个视觉等级的火焰粒子数量。总数 = 等级 × 此值。",
                        "Default: 5")
                .defineInRange("flame_particle_count_per_tier", 5, 1, 30);

        FIRE_IMPACT_FLAME_SPREAD = BUILDER
                .comment("Spread radius of flame particles (blocks).",
                        "火焰粒子的扩散半径（方块）。",
                        "Default: 0.3")
                .defineInRange("flame_spread", 0.3, 0.1, 1.0);

        FIRE_IMPACT_LAVA_ENABLED = BUILDER
                .comment("Whether to enable lava particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，是否在命中时生成熔岩粒子。",
                        "Default: true")
                .define("lava_enabled", true);

        FIRE_IMPACT_LAVA_PARTICLE_COUNT_PER_TIER = BUILDER
                .comment("Number of lava particles per visual tier when tier >= 3. Total = tier * this value.",
                        "当等级 ≥ 3 时，每个视觉等级的熔岩粒子数量。总数 = 等级 × 此值。",
                        "Default: 4")
                .defineInRange("lava_particle_count_per_tier", 4, 1, 20);

        FIRE_IMPACT_LAVA_SPREAD = BUILDER
                .comment("Spread radius of lava particles (blocks).",
                        "熔岩粒子的扩散半径（方块）。",
                        "Default: 0.5")
                .defineInRange("lava_spread", 0.5, 0.1, 1.5);

        FIRE_IMPACT_SOUL_FLAME_ENABLED = BUILDER
                .comment("Whether to enable soul flame particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，是否在命中时生成灵魂火焰粒子。",
                        "Default: true")
                .define("soul_flame_enabled", true);

        FIRE_IMPACT_SOUL_FLAME_COUNT = BUILDER
                .comment("Fixed number of soul flame particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，命中时生成的灵魂火焰粒子固定数量。",
                        "Default: 15")
                .defineInRange("soul_flame_count", 15, 0, 50);

        FIRE_IMPACT_SOUL_FLAME_SPREAD = BUILDER
                .comment("Spread radius of soul flame particles (blocks).",
                        "灵魂火焰粒子的扩散半径（方块）。",
                        "Default: 0.4")
                .defineInRange("soul_flame_spread", 0.4, 0.1, 1.5);

        FIRE_IMPACT_CAMPFIRE_SMOKE_ENABLED = BUILDER
                .comment("Whether to enable campfire smoke particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，是否在命中时生成篝火烟雾粒子。",
                        "Default: true")
                .define("campfire_smoke_enabled", true);

        FIRE_IMPACT_SMOKE_COUNT = BUILDER
                .comment("Fixed number of smoke particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，命中时生成的烟雾粒子固定数量。",
                        "Default: 5")
                .defineInRange("smoke_count", 5, 0, 20);

        FIRE_IMPACT_SMOKE_SPREAD_XZ = BUILDER
                .comment("Horizontal spread radius of smoke particles (blocks).",
                        "烟雾粒子的水平扩散半径（方块）。",
                        "Default: 0.2")
                .defineInRange("smoke_spread_xz", 0.2, 0.0, 1.0);

        FIRE_IMPACT_SMOKE_SPREAD_Y = BUILDER
                .comment("Vertical spread radius of smoke particles (blocks).",
                        "烟雾粒子的垂直扩散半径（方块）。",
                        "Default: 0.5")
                .defineInRange("smoke_spread_y", 0.5, 0.0, 1.5);

        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("Nature Attribute Visuals", "自然属性特效")
                .push("nature_visuals");

        NATURE_MELEE_ENABLED = BUILDER
                .comment("Whether to enable Nature attribute melee visual effects (Blossom Sever & Impact).",
                        "是否开启自然属性的近战视觉特效（包含落英斩和极速生长）。",
                        "Default: true")
                .define("nature_melee_enabled", true);

        NATURE_RANGED_ENABLED = BUILDER
                .comment("Whether to enable Nature attribute ranged visual effects (Flora Bolt).",
                        "是否开启自然属性的远程视觉特效（包含花神之箭双螺旋轨迹）。",
                        "Disable this can improve performance on servers with many entities.",
                        "在实体较多的服务器上关闭此项可以提高性能。",
                        "Default: true")
                .define("nature_ranged_enabled", true);

        BUILDER.comment("Nature Melee Swing Visuals", "自然近战挥动特效")
                .push("melee");

        NATURE_MELEE_RADIUS = BUILDER
                .comment("Radius of the melee swing arc (in blocks).",
                        "近战挥动弧形的半径（方块）。",
                        "Default: 2.2")
                .defineInRange("radius", 2.2, 0.5, 5.0);

        NATURE_MELEE_BASE_ANGLE_DEGREES = BUILDER
                .comment("Base angle of the swing arc in degrees.",
                        "挥动弧形的基准角度（度数）。",
                        "Default: 50.0")
                .defineInRange("base_angle_degrees", 50.0, 10.0, 180.0);

        NATURE_MELEE_ANGLE_MULTIPLIER_BASE = BUILDER
                .comment("Base multiplier for the swing angle. Final angle = base_angle * (base_multiplier + tier * per_tier).",
                        "挥动角度的基础乘数。",
                        "Default: 0.3")
                .defineInRange("angle_multiplier_base", 0.3, 0.1, 1.0);

        NATURE_MELEE_ANGLE_MULTIPLIER_PER_TIER = BUILDER
                .comment("Additional angle multiplier per visual tier.",
                        "每个视觉等级增加的额外角度乘数。",
                        "Default: 0.2")
                .defineInRange("angle_multiplier_per_tier", 0.2, 0.0, 0.5);

        NATURE_MELEE_PARTICLE_COUNT_BASE = BUILDER
                .comment("Base number of particles along the swing arc. Actual count = base * angle_multiplier + offset.",
                        "挥动弧线上粒子的基础数量。",
                        "Default: 15")
                .defineInRange("particle_count_base", 15, 5, 50);

        NATURE_MELEE_PARTICLE_COUNT_OFFSET = BUILDER
                .comment("Offset added to the particle count.",
                        "粒子数量的偏移值。",
                        "Default: 3")
                .defineInRange("particle_count_offset", 3, 0, 20);

        NATURE_MELEE_WAVE_AMPLITUDE = BUILDER
                .comment("Amplitude of the sine wave offset along the arc.",
                        "正弦波偏移幅度。",
                        "Default: 0.1")
                .defineInRange("wave_amplitude", 0.1, 0.0, 0.5);

        NATURE_MELEE_WAVE_FREQUENCY = BUILDER
                .comment("Frequency of the sine wave offset (multiplied by PI).",
                        "正弦波偏移的频率（乘以π）。",
                        "Default: 4.0")
                .defineInRange("wave_frequency", 4.0, 0.0, 10.0);

        NATURE_MELEE_COMPOSTER_SPEED_XZ = BUILDER
                .comment("Horizontal speed multiplier for composter particles (multiplied by look direction).",
                        "堆肥粒子的水平速度系数（乘以视线方向）。",
                        "Default: 0.1")
                .defineInRange("composter_speed_xz", 0.1, 0.0, 0.5);

        NATURE_MELEE_SPORE_BLOSSOM_CHANCE = BUILDER
                .comment("Probability (0-1) of spawning a spore blossom particle per position.",
                        "每个粒子位置生成孢子花粒子的概率（0-1）。",
                        "Default: 0.4")
                .defineInRange("spore_blossom_chance", 0.4, 0.0, 1.0);

        NATURE_MELEE_CHERRY_LEAVES_ENABLED = BUILDER
                .comment("Whether to allow cherry leaves particles (tier >= 3).",
                        "是否允许樱花叶粒子（等级 ≥ 3）。",
                        "Default: true")
                .define("cherry_leaves_enabled", true);

        NATURE_MELEE_CHERRY_LEAVES_CHANCE = BUILDER
                .comment("Probability (0-1) of spawning a cherry leaves particle when tier >= 3 and progress > min_progress.",
                        "樱花叶粒子生成概率（等级 ≥ 3 且进度大于最小进度）。",
                        "Default: 0.5")
                .defineInRange("cherry_leaves_chance", 0.5, 0.0, 1.0);

        NATURE_MELEE_CHERRY_LEAVES_MIN_PROGRESS = BUILDER
                .comment("Minimum progress (0-1) along the arc for cherry leaves particles to appear.",
                        "樱花叶粒子出现的最小进度（0-1）。",
                        "Default: 0.3")
                .defineInRange("cherry_leaves_min_progress", 0.3, 0.0, 1.0);

        NATURE_MELEE_WAX_ON_ENABLED = BUILDER
                .comment("Whether to allow wax on particles (tier >= 4).",
                        "是否允许打蜡粒子（等级 ≥ 4）。",
                        "Default: true")
                .define("wax_on_enabled", true);

        NATURE_MELEE_WAX_ON_MIN_PROGRESS = BUILDER
                .comment("Minimum progress (0-1) along the arc for wax on particles to appear.",
                        "打蜡粒子出现的最小进度（0-1）。",
                        "Default: 0.8")
                .defineInRange("wax_on_min_progress", 0.8, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Nature Ranged Projectile Trail Visuals", "自然远程投射物拖尾特效")
                .push("ranged");

        BUILDER.comment("Helix Geometry", "螺旋线几何参数")
                .push("geometry");

        NATURE_RANGED_CONE_MAX_RADIUS = BUILDER
                .comment("Maximum radius of the main helix (in blocks).",
                        "主螺旋线的最大半径（方块）。",
                        "Default: 2.4")
                .defineInRange("cone_max_radius", 2.4, 0.5, 5.0);

        NATURE_RANGED_BACK_OFFSET_START = BUILDER
                .comment("Offset distance behind the projectile where the helix starts (in blocks).",
                        "螺旋线起点位于投射物后方的偏移距离（方块）。",
                        "Default: 0.3")
                .defineInRange("back_offset_start", 0.3, 0.0, 2.0);

        NATURE_RANGED_ROTATION_SPEED = BUILDER
                .comment("Rotation speed of the helix (radians per tick).",
                        "螺旋线的旋转角速度（弧度/tick）。",
                        "Default: 3.0")
                .defineInRange("rotation_speed", 3.0, 0.5, 10.0);

        NATURE_RANGED_OUTER_HELIX_COUNT_PER_TIER = BUILDER
                .comment("Number of main helix strands per visual tier. Total strands = tier * this value.",
                        "每个视觉等级的主螺旋线条数。总线条数 = 等级 × 此值。",
                        "Default: 2")
                .defineInRange("outer_helix_count_per_tier", 2, 1, 8);

        NATURE_RANGED_ACTIVATION_INTERVAL = BUILDER
                .comment("How many ticks between activating additional helix strands.",
                        "激活额外螺旋线的间隔（tick）。",
                        "Default: 1")
                .defineInRange("activation_interval", 1, 1, 10);

        NATURE_RANGED_TAIL_HELIX_COUNT_PER_TIER = BUILDER
                .comment("Number of tail helix strands per visual tier.",
                        "每个视觉等级的尾部螺旋线条数。",
                        "Default: 2")
                .defineInRange("tail_helix_count_per_tier", 2, 1, 8);

        NATURE_RANGED_TAIL_DELAY_TICKS = BUILDER
                .comment("Delay in ticks before the tail helix starts to appear.",
                        "尾部螺旋线开始出现的延迟时间（tick）。",
                        "Default: 3")
                .defineInRange("tail_delay_ticks", 3, 0, 20);

        NATURE_RANGED_TAIL_RADIUS_FACTOR = BUILDER
                .comment("Factor applied to main radius to get tail helix radius (tail = main * factor).",
                        "尾部螺旋线半径相对于主半径的因子（尾部半径 = 主半径 × 因子）。",
                        "Default: 0.5")
                .defineInRange("tail_radius_factor", 0.5, 0.1, 1.0);

        NATURE_RANGED_OUTER_REVERSE_ROTATION = BUILDER
                .comment("Whether the main helix rotates in reverse direction (clockwise).",
                        "主螺旋线是否反向旋转（顺时针）。",
                        "Default: true")
                .define("outer_reverse_rotation", true);

        NATURE_RANGED_TAIL_REVERSE_ROTATION = BUILDER
                .comment("Whether the tail helix rotates in reverse direction.",
                        "尾部螺旋线是否反向旋转。",
                        "Default: false")
                .define("tail_reverse_rotation", false);

        BUILDER.pop();

        BUILDER.comment("Particle Density", "粒子密度参数")
                .push("density");

        NATURE_RANGED_MAIN_PARTICLE_COUNT = BUILDER
                .comment("Number of particles spawned per main helix point.",
                        "主螺旋线每个点生成的粒子数。",
                        "Default: 2")
                .defineInRange("main_particle_count", 2, 1, 10);

        NATURE_RANGED_TAIL_PARTICLE_COUNT = BUILDER
                .comment("Number of particles spawned per tail helix point.",
                        "尾部螺旋线每个点生成的粒子数。",
                        "Default: 2")
                .defineInRange("tail_particle_count", 2, 1, 10);

        NATURE_RANGED_CENTER_PARTICLE_ENABLED = BUILDER
                .comment("Whether to spawn an extra particle at the tail center.",
                        "是否在尾部中心生成额外粒子。",
                        "Default: true")
                .define("center_particle_enabled", true);

        NATURE_RANGED_CENTER_PARTICLE_COUNT = BUILDER
                .comment("Number of particles spawned at the tail center.",
                        "尾部中心生成的粒子数量。",
                        "Default: 1")
                .defineInRange("center_particle_count", 1, 0, 5);

        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("Nature Impact Explosion Visuals", "自然命中爆裂特效")
                .push("impact");

        NATURE_IMPACT_HAPPY_VILLAGER_COUNT_PER_TIER = BUILDER
                .comment("Number of happy villager particles per visual tier. Total = tier * this value.",
                        "每个视觉等级的 Happy Villager 粒子数量。总数 = 等级 × 此值。",
                        "Default: 4")
                .defineInRange("happy_villager_count_per_tier", 4, 1, 30);

        NATURE_IMPACT_HAPPY_VILLAGER_SPREAD = BUILDER
                .comment("Spread radius of happy villager particles (blocks).",
                        "Happy Villager 粒子的扩散半径（方块）。",
                        "Default: 0.4")
                .defineInRange("happy_villager_spread", 0.4, 0.1, 1.5);

        NATURE_IMPACT_HAPPY_VILLAGER_SPEED = BUILDER
                .comment("Speed of happy villager particles.",
                        "Happy Villager 粒子的速度。",
                        "Default: 0.1")
                .defineInRange("happy_villager_speed", 0.1, 0.0, 0.5);

        NATURE_IMPACT_SPORE_BLOSSOM_ENABLED = BUILDER
                .comment("Whether to spawn spore blossom particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，是否在命中时生成孢子花粒子。",
                        "Default: true")
                .define("spore_blossom_enabled", true);

        NATURE_IMPACT_SPORE_BLOSSOM_COUNT = BUILDER
                .comment("Number of spore blossom particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，孢子花粒子数量。",
                        "Default: 12")
                .defineInRange("spore_blossom_count", 12, 0, 50);

        NATURE_IMPACT_SPORE_BLOSSOM_SPREAD_XZ = BUILDER
                .comment("Horizontal spread radius of spore blossom particles (blocks).",
                        "孢子花粒子的水平扩散半径（方块）。",
                        "Default: 0.5")
                .defineInRange("spore_blossom_spread_xz", 0.5, 0.0, 1.5);

        NATURE_IMPACT_SPORE_BLOSSOM_SPREAD_Y = BUILDER
                .comment("Vertical spread radius of spore blossom particles (blocks).",
                        "孢子花粒子的垂直扩散半径（方块）。",
                        "Default: 0.2")
                .defineInRange("spore_blossom_spread_y", 0.2, 0.0, 1.0);

        NATURE_IMPACT_SPORE_BLOSSOM_SPEED = BUILDER
                .comment("Speed of spore blossom particles.",
                        "孢子花粒子的速度。",
                        "Default: 0.01")
                .defineInRange("spore_blossom_speed", 0.01, 0.0, 0.2);

        NATURE_IMPACT_CHERRY_LEAVES_ENABLED = BUILDER
                .comment("Whether to spawn cherry leaves particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，是否在命中时生成樱花叶粒子。",
                        "Default: true")
                .define("cherry_leaves_enabled", true);

        NATURE_IMPACT_CHERRY_LEAVES_COUNT = BUILDER
                .comment("Number of cherry leaves particles on impact when tier >= 3.",
                        "当等级 ≥ 3 时，樱花叶粒子数量。",
                        "Default: 8")
                .defineInRange("cherry_leaves_count", 8, 0, 50);

        NATURE_IMPACT_CHERRY_LEAVES_SPREAD_XZ = BUILDER
                .comment("Horizontal spread radius of cherry leaves particles (blocks).",
                        "樱花叶粒子的水平扩散半径（方块）。",
                        "Default: 0.4")
                .defineInRange("cherry_leaves_spread_xz", 0.4, 0.0, 1.5);

        NATURE_IMPACT_CHERRY_LEAVES_SPREAD_Y = BUILDER
                .comment("Vertical spread radius of cherry leaves particles (blocks).",
                        "樱花叶粒子的垂直扩散半径（方块）。",
                        "Default: 0.1")
                .defineInRange("cherry_leaves_spread_y", 0.1, 0.0, 1.0);

        NATURE_IMPACT_CHERRY_LEAVES_SPEED = BUILDER
                .comment("Speed of cherry leaves particles.",
                        "樱花叶粒子的速度。",
                        "Default: 0.05")
                .defineInRange("cherry_leaves_speed", 0.05, 0.0, 0.2);

        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("Thunder Attribute Visuals", "雷霆属性特效")
                .push("thunder_visuals");

        THUNDER_MELEE_ENABLED = BUILDER
                .comment("Whether to enable Thunder attribute melee visual effects (Arc Swing & Impact).",
                        "是否开启雷霆属性的近战视觉特效（包含电弧挥动和命中爆裂）。",
                        "Default: true")
                .define("thunder_melee_enabled", true);

        THUNDER_RANGED_ENABLED = BUILDER
                .comment("Whether to enable Thunder attribute ranged visual effects (Spiral Lightning Trail).",
                        "是否开启雷霆属性的远程视觉特效（包含螺旋闪电拖尾）。",
                        "Disable this can improve performance on servers with many entities.",
                        "在实体较多的服务器上关闭此项可以提高性能。",
                        "Default: true")
                .define("thunder_ranged_enabled", true);

        BUILDER.comment("Thunder Melee Swing Visuals", "雷霆近战挥动特效")
                .push("melee");

        THUNDER_MELEE_RADIUS = BUILDER
                .comment("Radius of the melee swing arc (in blocks).",
                        "近战挥动弧形的半径（方块）。",
                        "Default: 2.2")
                .defineInRange("radius", 2.2, 0.5, 5.0);

        THUNDER_MELEE_BASE_ANGLE_DEGREES = BUILDER
                .comment("Base angle of the swing arc in degrees.",
                        "挥动弧形的基准角度（度数）。",
                        "Default: 50.0")
                .defineInRange("base_angle_degrees", 50.0, 10.0, 180.0);

        THUNDER_MELEE_ANGLE_MULTIPLIER_BASE = BUILDER
                .comment("Base multiplier for the swing angle. Final angle = base_angle * (base_multiplier + tier * per_tier).",
                        "挥动角度的基础乘数。",
                        "Default: 0.3")
                .defineInRange("angle_multiplier_base", 0.3, 0.1, 1.0);

        THUNDER_MELEE_ANGLE_MULTIPLIER_PER_TIER = BUILDER
                .comment("Additional angle multiplier per visual tier.",
                        "每个视觉等级增加的额外角度乘数。",
                        "Default: 0.2")
                .defineInRange("angle_multiplier_per_tier", 0.2, 0.0, 0.5);

        THUNDER_MELEE_PARTICLE_COUNT_BASE = BUILDER
                .comment("Base number of particles along the swing arc. Actual count = base * angle_multiplier + offset.",
                        "挥动弧线上粒子的基础数量。",
                        "Default: 15")
                .defineInRange("particle_count_base", 15, 5, 50);

        THUNDER_MELEE_PARTICLE_COUNT_OFFSET = BUILDER
                .comment("Offset added to the particle count.",
                        "粒子数量的偏移值。",
                        "Default: 3")
                .defineInRange("particle_count_offset", 3, 0, 20);

        THUNDER_MELEE_FORWARD_OFFSET_FACTOR = BUILDER
                .comment("Forward offset factor for particle positions (multiplied by cos(angle) * radius).",
                        "粒子位置的前向偏移系数（乘以 cos(angle) * radius）。",
                        "Default: 0.5")
                .defineInRange("forward_offset_factor", 0.5, 0.2, 1.0);

        THUNDER_MELEE_FALL_SPEED = BUILDER
                .comment("Vertical fall speed of particles (negative = downward).",
                        "粒子的垂直下落速度（负值表示向下）。",
                        "Default: -0.01")
                .defineInRange("fall_speed", -0.01, -0.1, 0.1);

        THUNDER_MELEE_GLOW_CHANCE_TIER2 = BUILDER
                .comment("Probability (0-1) of spawning an extra GLOW particle per position when tier >= 2.",
                        "当等级 ≥ 2 时，每个粒子位置额外生成 GLOW 粒子的概率（0-1）。",
                        "Default: 0.4")
                .defineInRange("glow_chance_tier2", 0.4, 0.0, 1.0);

        THUNDER_MELEE_REVERSE_PORTAL_CHANCE_TIER3 = BUILDER
                .comment("Probability (0-1) of spawning a REVERSE_PORTAL particle per position when tier >= 3.",
                        "当等级 ≥ 3 时，每个粒子位置生成 REVERSE_PORTAL 粒子的概率（0-1）。",
                        "Default: 0.3")
                .defineInRange("reverse_portal_chance_tier3", 0.3, 0.0, 1.0);

        THUNDER_MELEE_ARC_LINE_ENABLED = BUILDER
                .comment("Whether to spawn arc line particles at the swing ends when tier >= 4.",
                        "当等级 ≥ 4 时，是否在挥动两端生成弧形连线粒子。",
                        "Default: true")
                .define("arc_line_enabled", true);

        THUNDER_MELEE_ARC_LINE_STEP_FACTOR = BUILDER
                .comment("Step density factor for arc line particles. Number of steps = distance * factor.",
                        "弧形连线粒子的步数密度因子。步数 = 距离 × 因子。",
                        "Default: 4.0")
                .defineInRange("arc_line_step_factor", 4.0, 1.0, 10.0);

        BUILDER.pop();

        BUILDER.comment("Thunder Ranged Projectile Trail Visuals", "雷霆远程投射物拖尾特效")
                .push("ranged");

        BUILDER.comment("Helix Geometry", "螺旋线几何参数")
                .push("geometry");

        THUNDER_RANGED_CONE_MAX_RADIUS = BUILDER
                .comment("Maximum radius of the helix (in blocks).",
                        "螺旋线的最大半径（方块）。",
                        "Default: 2.4")
                .defineInRange("cone_max_radius", 2.4, 0.5, 5.0);

        THUNDER_RANGED_BACK_OFFSET_START = BUILDER
                .comment("Offset distance behind the projectile where the helix starts (in blocks).",
                        "螺旋线起点位于投射物后方的偏移距离（方块）。",
                        "Default: 0.3")
                .defineInRange("back_offset_start", 0.3, 0.0, 2.0);

        THUNDER_RANGED_ROTATION_SPEED = BUILDER
                .comment("Rotation speed of the helix (radians per tick).",
                        "螺旋线的旋转角速度（弧度/tick）。",
                        "Default: 6.0")
                .defineInRange("rotation_speed", 6.0, 0.5, 15.0);

        THUNDER_RANGED_HELIX_COUNT_PER_TIER = BUILDER
                .comment("Number of helix strands per visual tier. Total strands = tier * this value.",
                        "每个视觉等级的螺旋线条数。总线条数 = 等级 × 此值。",
                        "Default: 2")
                .defineInRange("helix_count_per_tier", 2, 1, 8);

        THUNDER_RANGED_ACTIVATION_INTERVAL = BUILDER
                .comment("How many ticks between activating additional helix strands.",
                        "激活额外螺旋线的间隔（tick）。",
                        "Default: 1")
                .defineInRange("activation_interval", 1, 1, 10);

        BUILDER.pop();

        BUILDER.comment("Particle Density", "粒子密度参数")
                .push("density");

        THUNDER_RANGED_MAIN_PARTICLE_COUNT = BUILDER
                .comment("Number of particles spawned per helix point (Thunder Spark particle).",
                        "每个螺旋线点生成的粒子数量（雷电火花粒子）。",
                        "Default: 48")
                .defineInRange("main_particle_count", 48, 1, 100);

        BUILDER.pop();

        BUILDER.comment("Tail Particles", "尾部粒子")
                .push("tail");

        THUNDER_RANGED_TAIL_END_ROD_ENABLED = BUILDER
                .comment("Whether to spawn END_ROD particles at the tail when tier >= 2.",
                        "当等级 ≥ 2 时，是否在尾部生成 END_ROD 粒子。",
                        "Default: true")
                .define("tail_end_rod_enabled", true);

        THUNDER_RANGED_TAIL_END_ROD_COUNT = BUILDER
                .comment("Number of END_ROD particles spawned at the tail.",
                        "尾部生成的 END_ROD 粒子数量。",
                        "Default: 3")
                .defineInRange("tail_end_rod_count", 3, 0, 20);

        THUNDER_RANGED_TAIL_REVERSE_PORTAL_ENABLED = BUILDER
                .comment("Whether to spawn REVERSE_PORTAL particles at the tail when tier >= 3.",
                        "当等级 ≥ 3 时，是否在尾部生成 REVERSE_PORTAL 粒子。",
                        "Default: true")
                .define("tail_reverse_portal_enabled", true);

        THUNDER_RANGED_TAIL_REVERSE_PORTAL_GROUPS = BUILDER
                .comment("Number of groups of REVERSE_PORTAL particles.",
                        "REVERSE_PORTAL 粒子的组数。",
                        "Default: 3")
                .defineInRange("tail_reverse_portal_groups", 3, 0, 10);

        THUNDER_RANGED_TAIL_REVERSE_PORTAL_COUNT = BUILDER
                .comment("Number of particles per REVERSE_PORTAL group.",
                        "每组 REVERSE_PORTAL 粒子的数量。",
                        "Default: 5")
                .defineInRange("tail_reverse_portal_count", 5, 0, 20);

        THUNDER_RANGED_TAIL_REVERSE_PORTAL_SPREAD = BUILDER
                .comment("Random spread range for REVERSE_PORTAL particles (blocks).",
                        "REVERSE_PORTAL 粒子的随机偏移范围（方块）。",
                        "Default: 0.2")
                .defineInRange("tail_reverse_portal_spread", 0.2, 0.0, 1.0);

        THUNDER_RANGED_TAIL_DRAGON_BREATH_ENABLED = BUILDER
                .comment("Whether to spawn DRAGON_BREATH particles at the tail when tier >= 4.",
                        "当等级 ≥ 4 时，是否在尾部生成 DRAGON_BREATH 粒子。",
                        "Default: true")
                .define("tail_dragon_breath_enabled", true);

        THUNDER_RANGED_TAIL_DRAGON_BREATH_GROUPS = BUILDER
                .comment("Number of groups of DRAGON_BREATH particles.",
                        "DRAGON_BREATH 粒子的组数。",
                        "Default: 2")
                .defineInRange("tail_dragon_breath_groups", 2, 0, 10);

        THUNDER_RANGED_TAIL_DRAGON_BREATH_COUNT = BUILDER
                .comment("Number of particles per DRAGON_BREATH group.",
                        "每组 DRAGON_BREATH 粒子的数量。",
                        "Default: 1")
                .defineInRange("tail_dragon_breath_count", 1, 0, 10);

        THUNDER_RANGED_TAIL_DRAGON_BREATH_SPREAD = BUILDER
                .comment("Random spread range for DRAGON_BREATH particles (blocks).",
                        "DRAGON_BREATH 粒子的随机偏移范围（方块）。",
                        "Default: 0.3")
                .defineInRange("tail_dragon_breath_spread", 0.3, 0.0, 1.0);

        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("Thunder Impact Explosion Visuals", "雷霆命中爆裂特效")
                .push("impact");

        THUNDER_IMPACT_GLOW_COUNT_PER_TIER = BUILDER
                .comment("Number of GLOW particles per visual tier. Total = tier * this value.",
                        "每个视觉等级的 GLOW 粒子数量。总数 = 等级 × 此值。",
                        "Default: 8")
                .defineInRange("glow_count_per_tier", 8, 1, 50);

        THUNDER_IMPACT_GLOW_SPREAD = BUILDER
                .comment("Spread radius of GLOW particles (blocks).",
                        "GLOW 粒子的扩散半径（方块）。",
                        "Default: 0.5")
                .defineInRange("glow_spread", 0.5, 0.1, 1.5);

        THUNDER_IMPACT_GLOW_SPEED = BUILDER
                .comment("Speed of GLOW particles.",
                        "GLOW 粒子的速度。",
                        "Default: 0.1")
                .defineInRange("glow_speed", 0.1, 0.0, 0.5);

        THUNDER_IMPACT_END_ROD_COUNT_PER_TIER = BUILDER
                .comment("Number of END_ROD particles per visual tier. Total = tier * this value.",
                        "每个视觉等级的 END_ROD 粒子数量。总数 = 等级 × 此值。",
                        "Default: 4")
                .defineInRange("end_rod_count_per_tier", 4, 0, 50);

        THUNDER_IMPACT_END_ROD_SPREAD = BUILDER
                .comment("Spread radius of END_ROD particles (blocks).",
                        "END_ROD 粒子的扩散半径（方块）。",
                        "Default: 0.3")
                .defineInRange("end_rod_spread", 0.3, 0.0, 1.5);

        THUNDER_IMPACT_END_ROD_SPEED = BUILDER
                .comment("Speed of END_ROD particles.",
                        "END_ROD 粒子的速度。",
                        "Default: 0.05")
                .defineInRange("end_rod_speed", 0.05, 0.0, 0.5);

        THUNDER_IMPACT_EXTRA_END_ROD_ENABLED = BUILDER
                .comment("Whether to spawn extra END_ROD particles around the target when tier >= 2.",
                        "当等级 ≥ 2 时，是否在目标周围生成额外的 END_ROD 粒子。",
                        "Default: true")
                .define("extra_end_rod_enabled", true);

        THUNDER_IMPACT_EXTRA_END_ROD_COUNT_PER_TIER = BUILDER
                .comment("Number of extra END_ROD particles per visual tier. Total = tier * this value.",
                        "每等级的额外 END_ROD 粒子数量。总数 = 等级 × 此值。",
                        "Default: 4")
                .defineInRange("extra_end_rod_count_per_tier", 4, 0, 30);

        THUNDER_IMPACT_EXTRA_END_ROD_HORIZONTAL_SPREAD = BUILDER
                .comment("Horizontal spread range for extra END_ROD particles (blocks).",
                        "额外 END_ROD 粒子的水平扩散范围（方块）。",
                        "Default: 1.2")
                .defineInRange("extra_end_rod_horizontal_spread", 1.2, 0.0, 3.0);

        THUNDER_IMPACT_EXTRA_END_ROD_VERTICAL_RANDOM = BUILDER
                .comment("Whether to randomize vertical position of extra END_ROD particles (otherwise fixed at half height).",
                        "是否随机化额外 END_ROD 粒子的垂直位置（否则固定在半高）。",
                        "Default: true")
                .define("extra_end_rod_vertical_random", true);

        BUILDER.pop();
        BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register() {
        register("elementalcraft-visuals.toml");
    }

    public static void register(String configPath) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, configPath);
    }

    public static void refreshCache() {
        globalVisibilityCheckEnabled = GLOBAL_VISIBILITY_CHECK_ENABLED.get();
        globalViewDistanceMultiplier = GLOBAL_VIEW_DISTANCE_MULTIPLIER.get();

        fireMeleeEnabled = FIRE_MELEE_ENABLED.get();
        fireRangedEnabled = FIRE_RANGED_ENABLED.get();
        natureMeleeEnabled = NATURE_MELEE_ENABLED.get();
        natureRangedEnabled = NATURE_RANGED_ENABLED.get();
        thunderMeleeEnabled = THUNDER_MELEE_ENABLED.get();
        thunderRangedEnabled = THUNDER_RANGED_ENABLED.get();

        fireMeleeRadius = FIRE_MELEE_RADIUS.get();
        fireMeleeBaseAngleDegrees = FIRE_MELEE_BASE_ANGLE_DEGREES.get();
        fireMeleeAngleMultiplierBase = FIRE_MELEE_ANGLE_MULTIPLIER_BASE.get();
        fireMeleeAngleMultiplierPerTier = FIRE_MELEE_ANGLE_MULTIPLIER_PER_TIER.get();
        fireMeleeParticleCountBase = FIRE_MELEE_PARTICLE_COUNT_BASE.get();
        fireMeleeParticleCountOffset = FIRE_MELEE_PARTICLE_COUNT_OFFSET.get();
        fireMeleeWaveAmplitude = FIRE_MELEE_WAVE_AMPLITUDE.get();
        fireMeleeWaveFrequency = FIRE_MELEE_WAVE_FREQUENCY.get();
        fireMeleeSoulFlameChance = FIRE_MELEE_SOUL_FLAME_CHANCE.get();
        fireMeleeLavaChance = FIRE_MELEE_LAVA_CHANCE.get();
        fireMeleeSoulChance = FIRE_MELEE_SOUL_CHANCE.get();
        fireMeleeEnableSoulFlame = FIRE_MELEE_ENABLE_SOUL_FLAME.get();
        fireMeleeEnableLava = FIRE_MELEE_ENABLE_LAVA.get();
        fireMeleeEnableSoul = FIRE_MELEE_ENABLE_SOUL.get();

        fireRangedConeMaxRadius = FIRE_RANGED_CONE_MAX_RADIUS.get();
        fireRangedBackOffsetStart = FIRE_RANGED_BACK_OFFSET_START.get();
        fireRangedRotationSpeed = FIRE_RANGED_ROTATION_SPEED.get();
        fireRangedInnerRadiusFactor = FIRE_RANGED_INNER_RADIUS_FACTOR.get();
        fireRangedInnerDelayTicks = FIRE_RANGED_INNER_DELAY_TICKS.get();
        fireRangedActivationInterval = FIRE_RANGED_ACTIVATION_INTERVAL.get();
        fireRangedOuterHelixCountPerTier = FIRE_RANGED_OUTER_HELIX_COUNT_PER_TIER.get();
        fireRangedInnerHelixCountPerTier = FIRE_RANGED_INNER_HELIX_COUNT_PER_TIER.get();
        fireRangedTrailLavaParticleCount = FIRE_RANGED_TRAIL_LAVA_PARTICLE_COUNT.get();
        fireRangedTrailSoulParticleCount = FIRE_RANGED_TRAIL_SOUL_PARTICLE_COUNT.get();
        fireRangedTrailLavaSpread = FIRE_RANGED_TRAIL_LAVA_SPREAD.get();
        fireRangedTrailSoulSpread = FIRE_RANGED_TRAIL_SOUL_SPREAD.get();
        fireRangedEnableOuterHelix = FIRE_RANGED_ENABLE_OUTER_HELIX.get();
        fireRangedEnableInnerHelix = FIRE_RANGED_ENABLE_INNER_HELIX.get();
        fireRangedEnableTrailParticles = FIRE_RANGED_ENABLE_TRAIL_PARTICLES.get();
        fireRangedOuterReverseRotation = FIRE_RANGED_OUTER_REVERSE_ROTATION.get();
        fireRangedInnerReverseRotation = FIRE_RANGED_INNER_REVERSE_ROTATION.get();

        fireImpactFlameParticleCountPerTier = FIRE_IMPACT_FLAME_PARTICLE_COUNT_PER_TIER.get();
        fireImpactFlameSpread = FIRE_IMPACT_FLAME_SPREAD.get();
        fireImpactLavaEnabled = FIRE_IMPACT_LAVA_ENABLED.get();
        fireImpactLavaParticleCountPerTier = FIRE_IMPACT_LAVA_PARTICLE_COUNT_PER_TIER.get();
        fireImpactLavaSpread = FIRE_IMPACT_LAVA_SPREAD.get();
        fireImpactSoulFlameEnabled = FIRE_IMPACT_SOUL_FLAME_ENABLED.get();
        fireImpactSoulFlameCount = FIRE_IMPACT_SOUL_FLAME_COUNT.get();
        fireImpactSoulFlameSpread = FIRE_IMPACT_SOUL_FLAME_SPREAD.get();
        fireImpactCampfireSmokeEnabled = FIRE_IMPACT_CAMPFIRE_SMOKE_ENABLED.get();
        fireImpactSmokeCount = FIRE_IMPACT_SMOKE_COUNT.get();
        fireImpactSmokeSpreadXZ = FIRE_IMPACT_SMOKE_SPREAD_XZ.get();
        fireImpactSmokeSpreadY = FIRE_IMPACT_SMOKE_SPREAD_Y.get();

        natureMeleeRadius = NATURE_MELEE_RADIUS.get();
        natureMeleeBaseAngleDegrees = NATURE_MELEE_BASE_ANGLE_DEGREES.get();
        natureMeleeAngleMultiplierBase = NATURE_MELEE_ANGLE_MULTIPLIER_BASE.get();
        natureMeleeAngleMultiplierPerTier = NATURE_MELEE_ANGLE_MULTIPLIER_PER_TIER.get();
        natureMeleeParticleCountBase = NATURE_MELEE_PARTICLE_COUNT_BASE.get();
        natureMeleeParticleCountOffset = NATURE_MELEE_PARTICLE_COUNT_OFFSET.get();
        natureMeleeWaveAmplitude = NATURE_MELEE_WAVE_AMPLITUDE.get();
        natureMeleeWaveFrequency = NATURE_MELEE_WAVE_FREQUENCY.get();
        natureMeleeComposterSpeedXZ = NATURE_MELEE_COMPOSTER_SPEED_XZ.get();
        natureMeleeSporeBlossomChance = NATURE_MELEE_SPORE_BLOSSOM_CHANCE.get();
        natureMeleeCherryLeavesEnabled = NATURE_MELEE_CHERRY_LEAVES_ENABLED.get();
        natureMeleeCherryLeavesChance = NATURE_MELEE_CHERRY_LEAVES_CHANCE.get();
        natureMeleeCherryLeavesMinProgress = NATURE_MELEE_CHERRY_LEAVES_MIN_PROGRESS.get();
        natureMeleeWaxOnEnabled = NATURE_MELEE_WAX_ON_ENABLED.get();
        natureMeleeWaxOnMinProgress = NATURE_MELEE_WAX_ON_MIN_PROGRESS.get();

        natureRangedConeMaxRadius = NATURE_RANGED_CONE_MAX_RADIUS.get();
        natureRangedBackOffsetStart = NATURE_RANGED_BACK_OFFSET_START.get();
        natureRangedRotationSpeed = NATURE_RANGED_ROTATION_SPEED.get();
        natureRangedOuterHelixCountPerTier = NATURE_RANGED_OUTER_HELIX_COUNT_PER_TIER.get();
        natureRangedActivationInterval = NATURE_RANGED_ACTIVATION_INTERVAL.get();
        natureRangedTailHelixCountPerTier = NATURE_RANGED_TAIL_HELIX_COUNT_PER_TIER.get();
        natureRangedTailDelayTicks = NATURE_RANGED_TAIL_DELAY_TICKS.get();
        natureRangedTailRadiusFactor = NATURE_RANGED_TAIL_RADIUS_FACTOR.get();
        natureRangedOuterReverseRotation = NATURE_RANGED_OUTER_REVERSE_ROTATION.get();
        natureRangedTailReverseRotation = NATURE_RANGED_TAIL_REVERSE_ROTATION.get();
        natureRangedMainParticleCount = NATURE_RANGED_MAIN_PARTICLE_COUNT.get();
        natureRangedTailParticleCount = NATURE_RANGED_TAIL_PARTICLE_COUNT.get();
        natureRangedCenterParticleEnabled = NATURE_RANGED_CENTER_PARTICLE_ENABLED.get();
        natureRangedCenterParticleCount = NATURE_RANGED_CENTER_PARTICLE_COUNT.get();

        natureImpactHappyVillagerCountPerTier = NATURE_IMPACT_HAPPY_VILLAGER_COUNT_PER_TIER.get();
        natureImpactHappyVillagerSpread = NATURE_IMPACT_HAPPY_VILLAGER_SPREAD.get();
        natureImpactHappyVillagerSpeed = NATURE_IMPACT_HAPPY_VILLAGER_SPEED.get();
        natureImpactSporeBlossomEnabled = NATURE_IMPACT_SPORE_BLOSSOM_ENABLED.get();
        natureImpactSporeBlossomCount = NATURE_IMPACT_SPORE_BLOSSOM_COUNT.get();
        natureImpactSporeBlossomSpreadXZ = NATURE_IMPACT_SPORE_BLOSSOM_SPREAD_XZ.get();
        natureImpactSporeBlossomSpreadY = NATURE_IMPACT_SPORE_BLOSSOM_SPREAD_Y.get();
        natureImpactSporeBlossomSpeed = NATURE_IMPACT_SPORE_BLOSSOM_SPEED.get();
        natureImpactCherryLeavesEnabled = NATURE_IMPACT_CHERRY_LEAVES_ENABLED.get();
        natureImpactCherryLeavesCount = NATURE_IMPACT_CHERRY_LEAVES_COUNT.get();
        natureImpactCherryLeavesSpreadXZ = NATURE_IMPACT_CHERRY_LEAVES_SPREAD_XZ.get();
        natureImpactCherryLeavesSpreadY = NATURE_IMPACT_CHERRY_LEAVES_SPREAD_Y.get();
        natureImpactCherryLeavesSpeed = NATURE_IMPACT_CHERRY_LEAVES_SPEED.get();

        thunderMeleeRadius = THUNDER_MELEE_RADIUS.get();
        thunderMeleeBaseAngleDegrees = THUNDER_MELEE_BASE_ANGLE_DEGREES.get();
        thunderMeleeAngleMultiplierBase = THUNDER_MELEE_ANGLE_MULTIPLIER_BASE.get();
        thunderMeleeAngleMultiplierPerTier = THUNDER_MELEE_ANGLE_MULTIPLIER_PER_TIER.get();
        thunderMeleeParticleCountBase = THUNDER_MELEE_PARTICLE_COUNT_BASE.get();
        thunderMeleeParticleCountOffset = THUNDER_MELEE_PARTICLE_COUNT_OFFSET.get();
        thunderMeleeForwardOffsetFactor = THUNDER_MELEE_FORWARD_OFFSET_FACTOR.get();
        thunderMeleeFallSpeed = THUNDER_MELEE_FALL_SPEED.get();
        thunderMeleeGlowChanceTier2 = THUNDER_MELEE_GLOW_CHANCE_TIER2.get();
        thunderMeleeReversePortalChanceTier3 = THUNDER_MELEE_REVERSE_PORTAL_CHANCE_TIER3.get();
        thunderMeleeArcLineEnabled = THUNDER_MELEE_ARC_LINE_ENABLED.get();
        thunderMeleeArcLineStepFactor = THUNDER_MELEE_ARC_LINE_STEP_FACTOR.get();

        thunderRangedConeMaxRadius = THUNDER_RANGED_CONE_MAX_RADIUS.get();
        thunderRangedBackOffsetStart = THUNDER_RANGED_BACK_OFFSET_START.get();
        thunderRangedRotationSpeed = THUNDER_RANGED_ROTATION_SPEED.get();
        thunderRangedHelixCountPerTier = THUNDER_RANGED_HELIX_COUNT_PER_TIER.get();
        thunderRangedActivationInterval = THUNDER_RANGED_ACTIVATION_INTERVAL.get();
        thunderRangedMainParticleCount = THUNDER_RANGED_MAIN_PARTICLE_COUNT.get();
        thunderRangedTailEndRodEnabled = THUNDER_RANGED_TAIL_END_ROD_ENABLED.get();
        thunderRangedTailEndRodCount = THUNDER_RANGED_TAIL_END_ROD_COUNT.get();
        thunderRangedTailReversePortalEnabled = THUNDER_RANGED_TAIL_REVERSE_PORTAL_ENABLED.get();
        thunderRangedTailReversePortalGroups = THUNDER_RANGED_TAIL_REVERSE_PORTAL_GROUPS.get();
        thunderRangedTailReversePortalCount = THUNDER_RANGED_TAIL_REVERSE_PORTAL_COUNT.get();
        thunderRangedTailReversePortalSpread = THUNDER_RANGED_TAIL_REVERSE_PORTAL_SPREAD.get();
        thunderRangedTailDragonBreathEnabled = THUNDER_RANGED_TAIL_DRAGON_BREATH_ENABLED.get();
        thunderRangedTailDragonBreathGroups = THUNDER_RANGED_TAIL_DRAGON_BREATH_GROUPS.get();
        thunderRangedTailDragonBreathCount = THUNDER_RANGED_TAIL_DRAGON_BREATH_COUNT.get();
        thunderRangedTailDragonBreathSpread = THUNDER_RANGED_TAIL_DRAGON_BREATH_SPREAD.get();

        thunderImpactGlowCountPerTier = THUNDER_IMPACT_GLOW_COUNT_PER_TIER.get();
        thunderImpactGlowSpread = THUNDER_IMPACT_GLOW_SPREAD.get();
        thunderImpactGlowSpeed = THUNDER_IMPACT_GLOW_SPEED.get();
        thunderImpactEndRodCountPerTier = THUNDER_IMPACT_END_ROD_COUNT_PER_TIER.get();
        thunderImpactEndRodSpread = THUNDER_IMPACT_END_ROD_SPREAD.get();
        thunderImpactEndRodSpeed = THUNDER_IMPACT_END_ROD_SPEED.get();
        thunderImpactExtraEndRodEnabled = THUNDER_IMPACT_EXTRA_END_ROD_ENABLED.get();
        thunderImpactExtraEndRodCountPerTier = THUNDER_IMPACT_EXTRA_END_ROD_COUNT_PER_TIER.get();
        thunderImpactExtraEndRodHorizontalSpread = THUNDER_IMPACT_EXTRA_END_ROD_HORIZONTAL_SPREAD.get();
        thunderImpactExtraEndRodVerticalRandom = THUNDER_IMPACT_EXTRA_END_ROD_VERTICAL_RANDOM.get();
    }
}