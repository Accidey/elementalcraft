package com.xulai.elementalcraft.sound;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, ElementalCraft.MODID);

    public static final RegistryObject<SoundEvent> ELECTRIC_ZAP =
            SOUND_EVENTS.register("electric_zap",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ElementalCraft.MODID, "electric_zap")));

    public static final RegistryObject<SoundEvent> SPORE_GAIN =
            SOUND_EVENTS.register("spore_gain",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ElementalCraft.MODID, "spore_gain")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private ModSounds() {}
}