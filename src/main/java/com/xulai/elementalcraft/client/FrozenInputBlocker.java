package com.xulai.elementalcraft.client;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
public class FrozenInputBlocker {

    private static boolean isAllowedKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_ESCAPE
            || keyCode == GLFW.GLFW_KEY_F2
            || keyCode == GLFW.GLFW_KEY_F3
            || keyCode == GLFW.GLFW_KEY_F5;
    }

    private static boolean isAffected(Minecraft mc) {
        return mc.player != null
            && (mc.player.hasEffect(ModMobEffects.FREEZE.get())
             || mc.player.hasEffect(ModMobEffects.PARALYSIS.get()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        if (!isAffected(mc)) return;

        for (var key : mc.options.keyMappings) {
            if (isAllowedKey(key.getKey().getValue())) continue;
            key.setDown(false);
            while (key.consumeClick()) {}
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isAffected(mc)) return;

        for (var key : mc.options.keyMappings) {
            if (key.matchesMouse(event.getButton())) {
                key.setDown(false);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (isAffected(mc)) {
            event.setCanceled(true);
        }
    }
}
