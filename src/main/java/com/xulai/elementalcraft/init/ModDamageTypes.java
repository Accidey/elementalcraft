// src/main/java/com/xulai/elementalcraft/init/ModDamageTypes.java
package com.xulai.elementalcraft.init;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * ModDamageTypes
 *
 * 中文说明：
 * 模组自定义伤害类型的注册中心。
 * 在 Minecraft 1.20.1+ 中，伤害类型 (DamageType) 是数据驱动的（通过 JSON 定义），
 * 这里我们需要定义 ResourceKey 来在代码中引用它们。
 *
 * English description:
 * Registry center for mod custom damage types.
 * In Minecraft 1.20.1+, DamageTypes are data-driven (defined via JSON).
 * Here we define ResourceKeys to reference them in code.
 */
public class ModDamageTypes {

    /**
     * Steam Scalding Damage Type Key
     * 蒸汽烫伤伤害类型键
     *
     * Used for High Heat steam cloud damage. Bypasses armor.
     * 用于高温蒸汽云造成的伤害。无视护甲。
     */
    public static final ResourceKey<DamageType> STEAM_SCALDING = ResourceKey.create(
            Registries.DAMAGE_TYPE, 
            new ResourceLocation(ElementalCraft.MODID, "steam_scalding")
    );

    /**
     * Helper method to get a DamageSource from a Level and Key.
     * 从 Level 和 Key 获取 DamageSource 的辅助方法。
     *
     * @param level The level (world) / 当前世界
     * @param key The damage type key / 伤害类型键
     * @return A new DamageSource instance / 新的伤害源实例
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(key)
        );
    }
}