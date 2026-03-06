package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "elementalcraft")
public class TooltipEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty() || stack.isEmpty()) return;

        if (!stack.hasCustomHoverName()) {
            tooltip.set(0, stack.getHoverName());
        }

        ElementType element = ElementUtils.getDominantElement(stack);
        if (element == ElementType.NONE) return;

        Component originalName = tooltip.get(0);

        MutableComponent prefix = Component.literal("[")
                .append(Component.translatable("element." + element.getId()))
                .append("]")
                .withStyle(style -> style.withColor(element.getColor()).withBold(true));

        MutableComponent dot = Component.literal(" ●")
                .withStyle(style -> style.withColor(element.getColor()));

        MutableComponent finalName = Component.literal("")
                .append(prefix)
                .append(dot)
                .append(Component.literal(" "))
                .append(originalName);

        tooltip.set(0, finalName);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTooltipLower(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        for (ElementType type : ElementType.values()) {
            if (type == ElementType.NONE) continue;
            
            int level = ElementUtils.getEnhancementLevel(stack, type);
            if (level > 0) {
                int value = level * ElementalConfig.getStrengthPerLevel();

                MutableComponent line = Component.literal("")
                        .append(Component.translatable("tooltip.elementalcraft.enhancement"))
                        .append(" ")
                        .append(Component.translatable("element." + type.getId()))
                        .append(" ")
                        .append(Component.literal("+" + value))
                        .withStyle(style -> style.withColor(type.getColor()));

                tooltip.add(line);
            }
        }

        for (ElementType type : ElementType.values()) {
            if (type == ElementType.NONE) continue;
            
            int level = ElementUtils.getResistanceLevel(stack, type);
            if (level > 0) {
                int totalValue = level * ElementalConfig.getResistPerLevel();

                MutableComponent line = Component.literal("")
                        .append(Component.translatable("tooltip.elementalcraft.resistance"))
                        .append(" ")
                        .append(Component.translatable("element." + type.getId()))
                        .append(" ")
                        .append(Component.literal("+" + totalValue))
                        .withStyle(style -> style.withColor(type.getColor()));

                tooltip.add(line);
            }
        }
    }
}