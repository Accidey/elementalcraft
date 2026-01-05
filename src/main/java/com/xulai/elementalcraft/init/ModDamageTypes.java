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
 * <p>
 * 中文说明：
 * 模组自定义伤害类型的注册持有类。
 * 在 Minecraft 1.20.1 及更高版本中，伤害类型 (DamageType) 是通过 JSON 数据文件定义的（数据驱动）。
 * 此类定义了用于在代码中引用这些数据文件的资源键 (ResourceKey)。
 * <p>
 * English Description:
 * Registry holder class for custom mod Damage Types.
 * In Minecraft 1.20.1 and later, DamageTypes are defined via JSON data files (data-driven).
 * This class defines the ResourceKeys used to reference these data files within the code.
 */
public class ModDamageTypes {

    /**
     * 蒸汽烫伤伤害类型的资源键。
     * 用于高温蒸汽云造成的伤害逻辑。具体的伤害属性（如是否绕过护甲、是否耗尽饱食度等）在对应的 JSON 文件中定义。
     * <p>
     * ResourceKey for the Steam Scalding damage type.
     * Used for damage logic caused by high-heat steam clouds. Specific damage properties (e.g., bypassing armor, exhausting hunger) are defined in the corresponding JSON file.
     */
    public static final ResourceKey<DamageType> STEAM_SCALDING = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "steam_scalding")
    );

    /**
     * 熔岩魔法伤害类型的资源键。
     * 用于灼烧 (Scorched) 机制造成的持续伤害。
     * 对应 data/elementalcraft/damage_type/lava_magic.json 文件。
     * <p>
     * ResourceKey for Lava Magic damage type.
     * Used for the periodic damage caused by the Scorched mechanic.
     * Corresponds to data/elementalcraft/damage_type/lava_magic.json file.
     */
    public static final ResourceKey<DamageType> LAVA_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "lava_magic")
    );

    /**
     * 辅助方法：根据资源键和当前世界创建一个新的伤害源实例。
     * <p>
     * Helper method: Creates a new DamageSource instance based on the ResourceKey and the current level.
     *
     * @param level 当前世界（用于访问注册表） / Current level (used to access registries)
     * @param key   伤害类型的资源键 / ResourceKey of the damage type
     * @return 对应的伤害源实例 / Corresponding DamageSource instance
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(key)
        );
    }
}