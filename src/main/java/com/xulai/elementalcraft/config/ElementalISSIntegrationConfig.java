package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public final class ElementalISSIntegrationConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue MOB_LOW_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.IntValue MOB_AGGRESSIVE_CAST_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MOB_NORMAL_CAST_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue SCROLL_DROP_CHANCE;

    public static final ForgeConfigSpec.DoubleValue CASTER_MOB_CHANCE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CASTER_MOB_BLACKLIST;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Iron's Spellbooks 联动配置（仅安装ISS时加载，支持热重载）",
                        "Iron's Spellbooks Integration Configuration (Only loaded when ISS is installed, supports hot reload)")
                .push("iss_integration");
        
        BUILDER.comment(" ");        

        BUILDER.comment("施法生物概率（所有属性共用）",
                        "Caster Mob Chance (Shared across all elements)")
                .push("caster_mob_chance");

        BUILDER.comment(" ");

        CASTER_MOB_CHANCE = BUILDER
                .comment("属性生物成为施法者的概率。",
                         "",
                         "Chance for an elemental-attributed mob to become a caster.",
                         "",
                         "Default: 0.5 (50%) / 默认：0.5（50%）")
                .defineInRange("caster_mob_chance", 0.5, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Mob施法AI（持有法术卷轴的属性生物）",
                        "Mob Casting AI (elemental creatures with spell scrolls)")
                .push("mob_casting_ai");

        BUILDER.comment(" ");

        MOB_LOW_HEALTH_THRESHOLD = BUILDER
                .comment("生命值比例低于此值时，生物进入激进施法模式（施法更频繁）。",
                         "自然施法生物中，仅持有伤害法术的生物才会进入激进模式。",
                         "",
                         "Health ratio below which the mob enters aggressive casting mode (casts more frequently).",
                         "For Nature caster mobs, only mobs holding damage spells enter aggressive mode.",
                         "",
                         "Default: 0.5 (50%) / 默认：0.5（50%）")
                .defineInRange("mob_low_health_threshold", 0.5, 0.0, 1.0);

        BUILDER.comment(" ");

        MOB_AGGRESSIVE_CAST_COOLDOWN = BUILDER
                .comment("激进模式（低血量）下施法冷却时间（刻）。20刻 = 1秒。",
                         "",
                         "Cooldown (in ticks) between spell casts when mob is in aggressive mode (low health). 20 ticks = 1 second.",
                         "",
                         "Default: 100 (5 seconds) / 默认：100（5秒）")
                .defineInRange("mob_aggressive_cast_cooldown", 100, 1, 72000);

        BUILDER.comment(" ");

        MOB_NORMAL_CAST_COOLDOWN = BUILDER
                .comment("正常模式下施法冷却时间（刻）。20刻 = 1秒。",
                         "",
                         "Cooldown (in ticks) between spell casts in normal mode. 20 ticks = 1 second.",
                         "",
                         "Default: 200 (10 seconds) / 默认：200（10秒）")
                .defineInRange("mob_normal_cast_cooldown", 200, 1, 72000);

        BUILDER.pop();

        BUILDER.comment("卷轴掉落",
                        "Scroll Drop")
                .push("scroll_drop");

        BUILDER.comment(" ");

        SCROLL_DROP_CHANCE = BUILDER
                .comment("施法生物持有的法术卷轴掉落概率。1.0 = 100%，0.0 = 不掉落。",
                         "",
                         "Drop chance for the spell scroll held by caster mobs. 1.0 = 100%, 0.0 = never drops.",
                         "",
                         "Default: 1.0 (100%) / 默认：1.0（100%）")
                .defineInRange("scroll_drop_chance", 1.0, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("施法生物黑名单",
                        "Caster Mob Blacklist")
                .push("caster_blacklist");

        BUILDER.comment(" ");

        CASTER_MOB_BLACKLIST = BUILDER
                .comment("在此列表中的实体不能被选为施法生物。",
                         "格式：实体注册ID列表，例如 [\"minecraft:wither\", \"minecraft:warden\"]",
                         "也支持模组命名空间格式：[\"iceandfire\"] 会屏蔽该模组全部实体。",
                         "",
                         "Entities in this list cannot be selected as caster mobs.",
                         "Format: list of entity registry IDs, e.g., [\"minecraft:wither\", \"minecraft:warden\"]",
                         "Also supports mod namespace format: [\"iceandfire\"] to blacklist all entities from that mod.",
                         "",
                         "Default: [wither, warden, ender_dragon] / 默认：[凋零，坚守者，末影龙]")
                .defineListAllowEmpty("caster_mob_blacklist",
                        List.of("minecraft:wither", "minecraft:warden", "minecraft:ender_dragon"),
                        o -> o instanceof String);

        BUILDER.pop();

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static double mobLowHealthThreshold = 0.5;
    public static int mobAggressiveCastCooldown = 100;
    public static int mobNormalCastCooldown = 200;
    public static float scrollDropChance = 1.0F;

    public static double casterMobChance = 0.5;
    public static List<? extends String> cachedCasterBlacklist;

    public static void refreshCache() {
        mobLowHealthThreshold = MOB_LOW_HEALTH_THRESHOLD.get();
        mobAggressiveCastCooldown = MOB_AGGRESSIVE_CAST_COOLDOWN.get();
        mobNormalCastCooldown = MOB_NORMAL_CAST_COOLDOWN.get();
        scrollDropChance = SCROLL_DROP_CHANCE.get().floatValue();

        casterMobChance = CASTER_MOB_CHANCE.get();
        cachedCasterBlacklist = CASTER_MOB_BLACKLIST.get();
    }

    public static void register(String configPath) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, configPath);
    }

    private ElementalISSIntegrationConfig() {}
}
