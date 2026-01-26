// src/main/java/com/xulai/elementalcraft/config/ForcedItemConfig.java
package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

/**
 * ForcedItemConfig
 * <p>
 * 中文说明：
 * 强制物品属性配置文件类。
 * 该配置文件允许整合包制作者为特定物品（如武器或盔甲）强制指定特定的元素属性。
 * 支持热重载，方便在游戏中即时调整平衡性。
 * <p>
 * English Description:
 * Configuration class for Forced Item Attributes.
 * Allows modpack creators to enforce specific elemental attributes on specific items (e.g., weapons or armor).
 * Supports hot-reloading for instant balance adjustments in-game.
 */
public final class ForcedItemConfig {
    public static final ForgeConfigSpec SPEC;

    // 强制武器攻击属性配置
    // Forced Weapon Attack Attributes configuration
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FORCED_WEAPONS;

    // 强制盔甲强化与抗性属性配置
    // Forced Armor Enhancement + Resistance Attributes configuration
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FORCED_ARMOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Forced Item Attribute Configuration (Designed for modpacks, supports hot reload)",
                        "强制物品属性配置（专为整合包魔改设计，支持热重载）")
                .push("forced_items");

        // ======================== Forced Weapon Attributes / 强制武器属性 ========================
        builder.comment("Forced Weapon Attack Attributes", "强制武器攻击属性")
                .push("forced_weapons");

        FORCED_WEAPONS = builder
                .comment("",
                        "【Forced Weapon Attack Attributes】",
                        "【强制武器攻击属性】",
                        "Format: \"ItemID,AttributeID\"",
                        "格式：\"物品ID,属性ID\"",
                        "Attribute IDs: fire / frost / thunder / nature",
                        "属性ID：fire / frost / thunder / nature",
                        "Examples / 示例：",
                        "  \"minecraft:diamond_sword,fire\"        # All Diamond Swords force Fire Attack / 所有钻石剑强制赤焰攻击",
                        "  \"modid:legendary_sword,thunder\"       # Modded Legendary Sword forces Thunder Attack / 魔改神剑强制雷霆攻击",
                        "  \"minecraft:netherite_axe,frost\"       # Netherite Axe forces Frost Attack / 下界合金斧强制冰霜攻击")
                .defineListAllowEmpty("forced_weapons", List.of(), obj -> obj instanceof String);

        builder.pop();

        // ======================== Forced Armor Attributes / 强制装备属性 ========================
        builder.comment("Forced Armor Enhancement + Resistance Attributes", "强制装备强化+抗性属性")
                .push("forced_armor");

        FORCED_ARMOR = builder
                .comment("",
                        "【Forced Armor Enhancement + Resistance Attributes】",
                        "【强制装备强化+抗性属性】",
                        "Format: \"ItemID,EnhanceID,EnhancePoints,ResistID,ResistPoints\"",
                        "格式：\"物品ID,强化属性,强化点数,抗性属性,抗性点数\"",
                        "Points support fixed values or ranges (e.g., 100 or 50-200). Leave attribute ID empty for none.",
                        "点数支持固定值或范围（如 100 或 50-200），留空属性表示无。",
                        "Examples / 示例：",
                        "  \"minecraft:netherite_chestplate,fire,200,thunder,150\"   # Netherite Chest: Fire Enhance 200 + Thunder Resist 150 / 下界合金胸甲：火强化200 + 雷抗150",
                        "  \"modid:dragon_scale_helmet,nature,300,,200\"            # Dragon Helm: Nature Enhance 300 + No specific Resist Element (200 pts) / 龙鳞头盔：自然强化300 + 无属性抗性200",
                        "  \"minecraft:diamond_leggings,,150,frost,100\"            # Diamond Legs: Random Enhance (150 pts) + Frost Resist 100 / 钻石护腿：无指定强化（150点随机） + 冰抗100")
                .defineListAllowEmpty("forced_armor", List.of(), obj -> obj instanceof String);

        builder.pop();
        builder.pop();

        SPEC = builder.build();
    }

    /**
     * 将此配置文件注册到模组加载上下文。
     * <p>
     * Registers this configuration file to the mod loading context.
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "elementalcraft-forced-items.toml");
    }
}