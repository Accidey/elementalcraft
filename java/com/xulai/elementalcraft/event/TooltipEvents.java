// src/main/java/com/xulai/elementalcraft/event/TooltipEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * TooltipEvents 类负责处理物品 Tooltip 的自定义显示逻辑。
 * 主要功能包括：
 * 1. 为拥有主导元素的物品添加【元素名】前缀（带颜色和粗体）。
 * 2. 显示装备的属性强化和抗性信息（整行跟随对应元素颜色）。
 * 3. 隐藏怪物隐形头盔的抗性提示。
 *
 * TooltipEvents class handles custom display logic for item tooltips.
 * Main features include:
 * 1. Adding a colored and bold [Element Name] prefix to items with a dominant element.
 * 2. Displaying elemental enhancement and resistance information for equipment (entire line follows the corresponding element color).
 * 3. Hiding resistance tooltips for monster invisible helmets.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class TooltipEvents {

    /**
     * 监听物品 Tooltip 事件（最高优先级），处理物品名称前缀添加。
     * 为拥有主导元素的物品添加【元素名】 ● 前缀，并保留原物品名的所有样式（史诗斜体、金色等）。
     *
     * Listens to item tooltip event (highest priority) to handle item name prefix addition.
     * Adds a [Element Name] ● prefix to items with a dominant element while preserving all original name styles (epic italic, gold, etc.).
     *
     * @param event 物品 Tooltip 事件 / Item tooltip event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty() || stack.isEmpty()) return;

        // 防止原版附魔将物品名称染成紫色，重置为物品稀有度默认颜色
        // Prevent vanilla enchantments from coloring the item name purple; reset to rarity default color
        if (!stack.hasCustomHoverName()) {
            tooltip.set(0, stack.getHoverName());
        }

        ElementType element = ElementUtils.getDominantElement(stack);
        if (element == ElementType.NONE) return;

        // 获取原始物品名称（保留所有原有样式） / Get original item name (preserve all existing styles)
        Component originalName = tooltip.get(0);

        // 构建【元素名】前缀（带元素颜色和粗体） / Build [Element Name] prefix (with element color and bold)
        MutableComponent prefix = Component.literal("【")
                .append(Component.translatable("element." + element.getId()))
                .append("】")
                .withStyle(style -> style.withColor(element.getColor()).withBold(true));

        // 添加白色分隔符 ● / Add white separator ●
        MutableComponent dot = Component.literal(" ●").withStyle(ChatFormatting.WHITE);

        // 拼接最终名称：前缀 + 分隔符 + 空格 + 原名称 / Concatenate final name: prefix + separator + space + original name
        MutableComponent finalName = Component.literal("")
                .append(prefix)
                .append(dot)
                .append(Component.literal(" "))
                .append(originalName);

        tooltip.set(0, finalName);
    }

    /**
     * 监听物品 Tooltip 事件（最高优先级），在名称下方添加属性强化和抗性信息。
     * 强化信息显示于玩家装备，抗性信息显示于怪物装备（隐藏隐形头盔提示）。
     *
     * Listens to item tooltip event (highest priority) to add elemental enhancement and resistance information below the name.
     * Enhancement info is shown on player equipment, resistance info on monster equipment (hides tooltip for invisible helmets).
     *
     * @param event 物品 Tooltip 事件 / Item tooltip event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTooltipLower(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        // ==================== 属性强化（玩家装备） / Elemental Enhancement (player equipment) ====================
        for (ElementType type : ElementType.values()) {
            if (type == ElementType.NONE) continue;
            int level = ElementUtils.getEnhancementLevel(stack, type);
            if (level > 0) {
                int value = level * ElementalConfig.getStrengthPerLevel();

                // 整行文字跟随对应元素颜色显示 / Entire line text follows the corresponding element color
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

        // ==================== 属性抗性（怪物装备） / Elemental Resistance (monster equipment) ====================
        for (ElementType type : ElementType.values()) {
            if (type == ElementType.NONE) continue;
            int level = ElementUtils.getResistanceLevel(stack, type);
            if (level > 0) {
                // 隐藏怪物隐形头盔的抗性提示（玩家捡到也不显示） / Hide resistance tooltip for monster invisible helmets (not shown even if player picks it up)
                if (stack.getItem() == Items.LEATHER_HELMET) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && "invisible_resist".equals(tag.getString("elementalcraft_marker"))) {
                        continue;
                    }
                }

                int totalValue = level * ElementalConfig.getResistPerLevel();

                // 整行文字跟随对应元素颜色显示 / Entire line text follows the corresponding element color
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