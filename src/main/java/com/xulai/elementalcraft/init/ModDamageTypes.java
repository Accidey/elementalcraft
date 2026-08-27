package com.xulai.elementalcraft.init;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ModDamageTypes {

    public static final ResourceKey<DamageType> STEAM_SCALDING = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "steam_scalding")
    );
    public static final ResourceKey<DamageType> STEAM_SPORE_COMBUSTION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "steam_spore_combustion")
    );

    public static final ResourceKey<DamageType> LAVA_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "lava_magic")
    );

    public static final ResourceKey<DamageType> SPORES = ResourceKey.create(
           Registries.DAMAGE_TYPE,
           new ResourceLocation(ElementalCraft.MODID, "spores")
    );

    public static final ResourceKey<DamageType> STATIC_SHOCK = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "static_shock")
    );

    public static final ResourceKey<DamageType> FROSTBITE_THERMAL_SHOCK = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "frostbite_thermal_shock")
    );

    public static final ResourceKey<DamageType> FROSTBITE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "frostbite")
    );

    public static final ResourceKey<DamageType> TOXIC_BLAST = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ElementalCraft.MODID, "toxic_blast")
    );

    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(key)
        );
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key, Entity attacker) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(key),
                attacker
        );
    }
}
