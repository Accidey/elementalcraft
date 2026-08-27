package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PotionStackHandler {
    private static final Field ITEM_MAX_STACK_FIELD;

    static {
        Field field = null;
        for (String name : new String[]{"maxStackSize", "f_41370_"}) {
            try {
                field = Item.class.getDeclaredField(name);
                field.setAccessible(true);
                break;
            } catch (NoSuchFieldException ignored) {
            }
        }
        if (field == null) {
            ElementalCraft.LOGGER.error("Failed to find Item.maxStackSize field");
        }
        ITEM_MAX_STACK_FIELD = field;
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        if (!ElementalConfig.POTION_STACK_64.get()) return;
        makeStackable64(Items.POTION);
        makeStackable64(Items.SPLASH_POTION);
        makeStackable64(Items.LINGERING_POTION);
        ElementalCraft.LOGGER.info("[ElementalCraft] 原版药水堆叠上限已改为 64");
    }

    private static void makeStackable64(Item item) {
        if (ITEM_MAX_STACK_FIELD == null) return;
        try {
            ITEM_MAX_STACK_FIELD.setInt(item, 64);
        } catch (IllegalAccessException e) {
            ElementalCraft.LOGGER.error("Failed to set maxStackSize for {}", item, e);
        }
    }
}
