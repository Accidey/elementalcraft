package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ElementalCraft.MODID);

    public static final RegistryObject<MobEffect> WETNESS =
            MOB_EFFECTS.register("wetness", WetnessMobEffect::new);

    public static final RegistryObject<MobEffect> SPORES =
            MOB_EFFECTS.register("flammable_spores", FlammableSporesEffect::new);

    public static final RegistryObject<MobEffect> STATIC_SHOCK =
            MOB_EFFECTS.register("static_shock", StaticShockEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    private ModMobEffects() {}
}