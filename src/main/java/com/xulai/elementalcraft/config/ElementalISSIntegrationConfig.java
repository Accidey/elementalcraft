package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ElementalISSIntegrationConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue POISON_CLOUD_FIRE_TIER_STEP;

    public static final ForgeConfigSpec.IntValue NATURE_ENHANCEMENT_PER_SPORE_STACK;

    public static final ForgeConfigSpec.IntValue THUNDER_ENHANCEMENT_PER_STATIC_STACK;

    public static final ForgeConfigSpec.DoubleValue MOB_LOW_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.IntValue MOB_AGGRESSIVE_CAST_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MOB_NORMAL_CAST_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MOB_WATER_BOTTLE_INTERVAL;
    public static final ForgeConfigSpec.IntValue MOB_MAX_MISS_COUNT;

    public static final ForgeConfigSpec.DoubleValue SCROLL_DROP_CHANCE;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Iron's Spellbooks Integration Configuration (Only loaded when ISS is installed, supports hot reload)",
                        "Iron's Spellbooks 联动配置（仅安装ISS时加载，支持热重载）")
                .push("iss_integration");

        BUILDER.push("poison_cloud_fire_reaction");
        BUILDER.comment("Poison Cloud + Fire Projectile Reaction (毒雾云 + 赤焰投射物反应)",
                        "当带有赤焰附魔的投射物碰到毒雾云时触发毒火爆燃。",
                        "When a fire_strike enchanted projectile collides with a PoisonCloud, triggers Toxic Blast.");

        POISON_CLOUD_FIRE_TIER_STEP = BUILDER
                .comment("Fire enhancement points per Toxic Blast tier. Set to 0 to disable.",
                        "e.g., 20 means: 0-20 = tier 1, 21-40 = tier 2, 41-60 = tier 3.",
                        "",
                        "每多少点赤焰属性强化增加1级毒火爆燃，设为0关闭此反应。",
                        "例如设为20：0-20点=1级，21-40点=2级，41-60点=3级。",
                        "",
                        "Default: 20")
                .defineInRange("poison_cloud_fire_tier_step", 20, 0, 1000);

        BUILDER.pop();

        BUILDER.push("root_spell_integration");
        BUILDER.comment("ROOT Spell + Flammable Spores Integration (ROOT法术 + 易燃孢子联动)",
                        "When ROOT spell hits a target, applies flammable spore stacks based on caster's nature enhancement.",
                        "When a fire-attribute attack hits a ROOT target and triggers Scorched, removes ROOT.",
                        "ROOT法术命中目标时，根据施法者自然属性强化施加易燃孢子层数。",
                        "赤焰属性攻击命中ROOT目标并触发灼烧时，解除ROOT效果。");

        NATURE_ENHANCEMENT_PER_SPORE_STACK = BUILDER
                .comment("Every N points of caster's nature enhancement applies 1 spore stack. Set to 0 to disable.",
                        "",
                        "每多少点施法者自然属性强化施加1层易燃孢子层数。设为0关闭。",
                        "",
                        "Default: 20")
                .defineInRange("nature_enhancement_per_spore_stack", 20, 0, 1000);

        BUILDER.pop();

        BUILDER.comment("Thunder Enhancement Static Stacks (Thunder enhancement points -> Static stacks for elemental reactions)",
                        "雷霆强化静电层数（雷霆属性强化点数 -> 静电层数用于触发元素反应）")
                .push("thunder_static_stacks");

        THUNDER_ENHANCEMENT_PER_STATIC_STACK = BUILDER
                .comment("Every N points of thunder enhancement applies 1 static stack. Set to 0 to disable.",
                        "",
                        "每多少点雷霆属性强化施加1层静电层数。设为0关闭。",
                        "",
                        "Default: 20 / 默认：20")
                .defineInRange("thunder_enhancement_per_static_stack", 20, 0, 1000);

        BUILDER.pop();

        BUILDER.comment("Mob Casting AI (Thunder/Nature caster mobs with spell scrolls)",
                        "Mob施法AI（持有雷霆/自然卷轴的施法生物）")
                .push("mob_casting_ai");

        MOB_LOW_HEALTH_THRESHOLD = BUILDER
                .comment("Health ratio below which the mob enters aggressive casting mode (casts more frequently).",
                        "For Nature caster mobs, only mobs holding damage spells enter aggressive mode.",
                        "",
                        "生命值比例低于此值时，生物进入激进施法模式（施法更频繁）。",
                        "自然施法生物中，仅持有伤害法术的生物才会进入激进模式。",
                        "",
                        "Default: 0.5 (50%) / 默认：0.5（50%）")
                .defineInRange("mob_low_health_threshold", 0.5, 0.0, 1.0);

        MOB_AGGRESSIVE_CAST_COOLDOWN = BUILDER
                .comment("Cooldown (in ticks) between spell casts when mob is in aggressive mode (low health). 20 ticks = 1 second.",
                        "",
                        "激进模式（低血量）下施法冷却时间（刻）。20刻 = 1秒。",
                        "",
                        "Default: 100 (5 seconds) / 默认：100（5秒）")
                .defineInRange("mob_aggressive_cast_cooldown", 100, 1, 72000);

        MOB_NORMAL_CAST_COOLDOWN = BUILDER
                .comment("Cooldown (in ticks) between spell casts in normal mode. 20 ticks = 1 second.",
                        "",
                        "正常模式下施法冷却时间（刻）。20刻 = 1秒。",
                        "",
                        "Default: 200 (10 seconds) / 默认：200（10秒）")
                .defineInRange("mob_normal_cast_cooldown", 200, 1, 72000);

        MOB_WATER_BOTTLE_INTERVAL = BUILDER
                .comment("Interval (in ticks) between splash water bottle throws at the target. 20 ticks = 1 second.",
                        "For Nature caster mobs, this value is reused as the pending cast delay (waiting for spell projectile to reach target).",
                        "",
                        "向目标投掷喷溅水瓶的间隔时间（刻）。20刻 = 1秒。",
                        "自然施法生物复用此值作为延迟施法判定时间（等待法术弹道到达目标）。",
                        "",
                        "Default: 40 (2 seconds) / 默认：40（2秒）")
                .defineInRange("mob_water_bottle_interval", 40, 1, 72000);

        MOB_MAX_MISS_COUNT = BUILDER
                .comment("When the spell misses the target, the mob will immediately recast on each miss.",
                        "This value controls how many consecutive misses are allowed before the mob enters a long cooldown.",
                        "For Thunder mobs: applies when target is already wet. For Nature mobs: applies after pending cast delay.",
                        "",
                        "法术未命中目标时，生物会在每次未命中后立即重新施法。",
                        "此值控制允许连续补刀的次数，达到上限后生物进入较长冷却。",
                        "雷霆生物：目标已潮湿时生效。自然生物：延迟施法判定后生效。",
                        "",
                        "Default: 2 / 默认：2")
                .defineInRange("mob_max_miss_count", 2, 1, 100);

        BUILDER.pop();

        BUILDER.comment("Scroll Drop", "卷轴掉落")
                .push("scroll_drop");

        SCROLL_DROP_CHANCE = BUILDER
                .comment("Drop chance for the spell scroll held by caster mobs (Thunder or Nature). 1.0 = 100%, 0.0 = never drops.",
                        "",
                        "施法生物持有的法术卷轴掉落概率（雷霆或自然）。1.0 = 100%，0.0 = 不掉落。",
                        "",
                        "Default: 1.0 (100%) / 默认：1.0（100%）")
                .defineInRange("scroll_drop_chance", 1.0, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static int poisonCloudFireTierStep = 20;

    public static int natureEnhancementPerSporeStack = 20;

    public static int thunderEnhancementPerStaticStack = 20;

    public static double mobLowHealthThreshold = 0.5;
    public static int mobAggressiveCastCooldown = 100;
    public static int mobNormalCastCooldown = 200;
    public static int mobWaterBottleInterval = 40;
    public static int mobMaxMissCount = 2;

    public static float scrollDropChance = 1.0F;

    public static void refreshCache() {
        poisonCloudFireTierStep = POISON_CLOUD_FIRE_TIER_STEP.get();

        natureEnhancementPerSporeStack = NATURE_ENHANCEMENT_PER_SPORE_STACK.get();

        thunderEnhancementPerStaticStack = THUNDER_ENHANCEMENT_PER_STATIC_STACK.get();

        mobLowHealthThreshold = MOB_LOW_HEALTH_THRESHOLD.get();
        mobAggressiveCastCooldown = MOB_AGGRESSIVE_CAST_COOLDOWN.get();
        mobNormalCastCooldown = MOB_NORMAL_CAST_COOLDOWN.get();
        mobWaterBottleInterval = MOB_WATER_BOTTLE_INTERVAL.get();
        mobMaxMissCount = MOB_MAX_MISS_COUNT.get();

        scrollDropChance = SCROLL_DROP_CHANCE.get().floatValue();
    }

    public static void register(String configPath) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, configPath);
    }

    private ElementalISSIntegrationConfig() {}
}
