// src/main/java/com/xulai/elementalcraft/config/ElementalVisualConfig.java
package com.xulai.elementalcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * ElementalVisualConfig
 * <p>
 * 中文说明：
 * 元素视觉特效配置文件类。
 * 专门用于控制模组中各类视觉特效的开启与关闭。
 * 即使是在服务端运行的逻辑（如远程箭矢拖尾），也可以通过此配置进行开关，从而节省服务器计算资源。
 * 目前主要控制赤焰属性的近战和远程特效。
 * <p>
 * English Description:
 * Configuration class for Elemental Visual Effects.
 * Dedicated to controlling the enabling and disabling of various visual effects in the mod.
 * Even logic running on the server (such as ranged arrow trails) can be toggled via this config, saving server computational resources.
 * Currently primarily controls Fire attribute melee and ranged effects.
 */
public class ElementalVisualConfig {

    public static final ForgeConfigSpec SPEC;

    // ======================== Fire Visuals / 赤焰特效 ========================
    
    // 近战特效开关（挥动轨迹 + 命中爆裂）
    // Melee Visuals Switch (Swing Trail + Impact Burst)
    public static final ForgeConfigSpec.BooleanValue FIRE_MELEE_ENABLED;

    // 远程特效开关（飞行拖尾 + 命中爆裂）
    // Ranged Visuals Switch (Flight Trail + Impact Burst)
    public static final ForgeConfigSpec.BooleanValue FIRE_RANGED_ENABLED;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Elemental Visual Effects Configuration", "元素视觉特效配置")
                .push("visual_effects");

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

        BUILDER.pop(); // pop fire_visuals
        BUILDER.pop(); // pop visual_effects

        SPEC = BUILDER.build();
    }

    // ======================== Static Cache / 静态缓存 ========================

    // 静态字段用于快速访问，避免频繁调用 ConfigValue.get()
    // Static fields for fast access, avoiding frequent ConfigValue.get() calls
    public static boolean fireMeleeEnabled = true;
    public static boolean fireRangedEnabled = true;

    /**
     * 注册配置文件。
     * <p>
     * Registers the configuration file.
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "elementalcraft-visuals.toml");
    }

    /**
     * 刷新配置缓存。
     * 必须在 ModConfigEvent 或 ConfigAutoSync 中调用。
     * <p>
     * Refreshes the configuration cache.
     * Must be called during ModConfigEvent or ConfigAutoSync.
     */
    public static void refreshCache() {
        fireMeleeEnabled = FIRE_MELEE_ENABLED.get();
        fireRangedEnabled = FIRE_RANGED_ENABLED.get();
    }
}